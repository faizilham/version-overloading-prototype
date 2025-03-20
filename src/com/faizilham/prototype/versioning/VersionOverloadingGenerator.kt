package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.CopyMethodName
import com.faizilham.prototype.versioning.Constants.IntroducedAtFqName
import com.faizilham.prototype.versioning.Constants.VERSION_OVERLOAD_WRAPPER
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.builders.declarations.buildConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.setSourceRange
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.name.StandardClassIds
import java.lang.Runtime.Version
import java.util.*

// Generate hidden overloads for each previous versions of a function to maintain binary compatibility
// Assumptions (enforced by checker):
// 1. Version annotations are only added at optional parameters
// 2. The version number conforms to the java.lang.Runtime.Version format
// 3. Optional parameters with version annotations are in the tail positions or before a trailing lambda,
//    and non-annotated parameters are in the head
// 4. Version annotations are either in increasing order, or must be provided by name

class VersionOverloadingGenerator(context: IrPluginContext) : IrVisitor<Unit, VersionOverloadingGenerator.VisitorContext?>() {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns
    private val deprecationBuilder = DeprecationBuilder(context)

    override fun visitElement(element: IrElement, data: VisitorContext?) {
        if (element !is IrDeclarationContainer) {
            return element.acceptChildren(this, data)
        }

        val visitorContext = VisitorContext(isDataClass = element is IrClass && element.isData)
        element.acceptChildren(this, visitorContext)
        element.declarations.addAll(visitorContext.generatedFunctions)
    }

    override fun visitFunction(declaration: IrFunction, data: VisitorContext?) {
        if (data == null) return

        val versionParamIndexes =
            if (data.isDataClass && declaration.name == CopyMethodName) {
                data.primaryConstructorVersions
            } else {
                getSortedVersionParameterIndexes(declaration)
            }

        generateVersions(declaration, data, versionParamIndexes)

        if (data.isDataClass && declaration is IrConstructor && declaration.isPrimary) {
            data.primaryConstructorVersions = versionParamIndexes
        }
    }

    class VisitorContext(
        val isDataClass: Boolean,
        val generatedFunctions: MutableList<IrFunction> = mutableListOf(),
        var primaryConstructorVersions: SortedMap<Version?, MutableList<Int>>? = null
    )

    private fun generateVersions(func: IrFunction, data: VisitorContext, versionParamIndexes: SortedMap<Version?, MutableList<Int>>?) {
        if (versionParamIndexes == null || versionParamIndexes.size < 2) return

        var lastIncludedParameters = BooleanArray(func.valueParameters.size) { true }

        versionParamIndexes.asIterable().forEachIndexed { i, (_, paramIndexes) ->
            if (i > 0) {
                val wrapper = generateWrapper(func, lastIncludedParameters) ?: return@forEachIndexed
                data.generatedFunctions.add(wrapper)
            }

            paramIndexes.forEach {
                lastIncludedParameters[it] = false
            }
        }
    }

    private fun getSortedVersionParameterIndexes(func: IrFunction): SortedMap<Version?, MutableList<Int>> {
        val versionIndexes = sortedMapOf<Version?, MutableList<Int>> (nullsLast(compareByDescending { it }))

        func.valueParameters.forEachIndexed { i, param ->
            val versionNumber = param.getVersionNumber()

            if (versionIndexes.containsKey(versionNumber)) {
                versionIndexes[versionNumber]!!.add(i)
            } else {
                versionIndexes[versionNumber] = mutableListOf(i)
            }
        }

        return versionIndexes
    }

    private fun IrValueParameter.getVersionNumber() : Version? {
        if (defaultValue == null) return null
        val annotation = getAnnotation(IntroducedAtFqName) ?: return null
        val versionString = (annotation.valueArguments[0] as? IrConst)?.value as? String ?: return null

        return try {
            Version.parse(versionString)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun generateWrapper(original: IrFunction, includedParams: BooleanArray): IrFunction? {
        val wrapperIrFunction = irFactory.generateWrapperHeader(original, includedParams) ?: return null

        val call = when (original) {
            is IrConstructor ->
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, irBuiltIns.unitType, original.symbol
                )
            is IrSimpleFunction ->
                IrCallImpl.fromSymbolOwner(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, original.returnType, original.symbol)
        }

        for (arg in wrapperIrFunction.allTypeParameters) {
            call.putTypeArgument(arg.index, arg.defaultType)
        }

        call.dispatchReceiver = wrapperIrFunction.dispatchReceiverParameter?.let { dispatchReceiver ->
            IrGetValueImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                dispatchReceiver.symbol
            )
        }

        call.extensionReceiver = wrapperIrFunction.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetValueImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                extensionReceiver.symbol
            )
        }

        var lastWrapperIndex = 0
        for (originalIndex in original.valueParameters.indices) {
            if (!includedParams[originalIndex]) {
                call.putValueArgument(originalIndex, null)
                continue
            }

            val wrapperParam = wrapperIrFunction.valueParameters[lastWrapperIndex]
            call.putValueArgument(originalIndex, IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, wrapperParam.symbol))
            lastWrapperIndex += 1
        }

        wrapperIrFunction.body = when (original) {
            is IrConstructor -> {
                irFactory.createBlockBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, listOf(call))
            }
            is IrSimpleFunction -> {
                irFactory.createExpressionBody(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, call)
            }
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateWrapperHeader(original: IrFunction, includedParams: BooleanArray): IrFunction? {
        val res = when (original) {
            is IrConstructor -> {
                buildConstructor {
                    setSourceRange(original)
                    origin = VERSION_OVERLOAD_WRAPPER
                    name = original.name
                    visibility = original.visibility
                    returnType = original.returnType
                    isInline = original.isInline
                    containerSource = original.containerSource
                }
            }
            is IrSimpleFunction -> buildFun {
                setSourceRange(original)
                origin = VERSION_OVERLOAD_WRAPPER
                name = original.name
                visibility = original.visibility
                modality = original.modality
                returnType = original.returnType
                isInline = original.isInline
                isSuspend = original.isSuspend
                containerSource = original.containerSource
            }
        }

        res.parent = original.parent
        res.addDeprecatedAnnotation(DeprecationLevel.HIDDEN)
        res.copyAnnotationsFrom(original)
        res.copyTypeParametersFrom(original)
        res.dispatchReceiverParameter = original.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = original.extensionReceiverParameter?.copyTo(res)
        res.valueParameters = res.generateNewValueParameters(original, includedParams)

        return res
    }

    private fun IrFunction.generateNewValueParameters(original: IrFunction, includedParams: BooleanArray): List<IrValueParameter> {
        val result = mutableListOf<IrValueParameter>()

        original.valueParameters.forEachIndexed { i, param ->
            if (includedParams[i]) {
                // NOTE: default value is copied because kotlin binaries calls the generated $default function
                val newParam = param.copyTo(this).transform(GetValueTransformer(this), null)
                result.add(newParam)
            }
        }

        return result
    }

    private fun IrFunction.addDeprecatedAnnotation(level: DeprecationLevel) {
        val annotation = deprecationBuilder.buildAnnotationCall(level) ?: return
        annotations += annotation
    }
}

private class GetValueTransformer(val irFunction: IrFunction) : IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrGetValue {
        return (super.visitGetValue(expression) as IrGetValue).remapSymbolParent(
            classRemapper = { irFunction.parent as? IrClass ?: it },
            functionRemapper = { irFunction }
        )
    }
}

private class DeprecationBuilder(private val context: IrPluginContext) {
    private val classSymbol = context.referenceClass(StandardClassIds.Annotations.Deprecated)
    private val deprecationLevelClass = context.referenceClass(StandardClassIds.DeprecationLevel)?.owner

    fun buildAnnotationCall(level: DeprecationLevel) : IrConstructorCall? {
        if (classSymbol == null || deprecationLevelClass == null) return null

        val levelSymbol = deprecationLevelClass.declarations
            .filterIsInstance<IrEnumEntry>()
            .single { it.name.toString() == level.name }.symbol

        return IrConstructorCallImpl.fromSymbolOwner(
            SYNTHETIC_OFFSET,
            SYNTHETIC_OFFSET,
            classSymbol.defaultType,
            classSymbol.constructors.first()
        ).apply {
            putValueArgument(
                0,
                IrConstImpl.string(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, context.irBuiltIns.stringType, "Deprecated")
            )

            putValueArgument(
                2,
                IrGetEnumValueImpl(
                    SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                    deprecationLevelClass.defaultType, levelSymbol
                )
            )
        }
    }
}