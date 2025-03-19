package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.Constants.CopyMethodName
import com.faizilham.prototype.versioning.Constants.IntroducedAtFqName
import com.faizilham.prototype.versioning.Constants.VERSION_OVERLOAD_WRAPPER
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
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
        when (element) {
            is IrClass -> {
                val visitorContext = VisitorContext(isDataClass = element.isData)
                element.acceptChildren(this, visitorContext)
                element.addChildren(visitorContext.generatedFunctions)
            }
            is IrFile -> {
                val visitorContext = VisitorContext(isDataClass = false)
                element.acceptChildren(this, visitorContext)
                element.addChildren(visitorContext.generatedFunctions)
            }
            is IrDeclaration,
            is IrModuleFragment -> element.acceptChildren(this, null)
            else -> {}
        }
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

    private fun generateVersions(func: IrFunction, data: VisitorContext, versionParamIndexes: SortedMap<Version?, MutableList<Int>>?) {
        if (versionParamIndexes == null || versionParamIndexes.size < 2) return

        var lastIncludedParameters = MutableList<Boolean>(func.valueParameters.size) { true }

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

    class VisitorContext(
        val isDataClass: Boolean,
        val generatedFunctions: MutableList<IrFunction> = mutableListOf(),
        var primaryConstructorVersions: SortedMap<Version?, MutableList<Int>>? = null
    )

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
        } catch (_: Exception) {
            null
        }
    }

    private fun generateWrapper(target: IrFunction, includedParams: List<Boolean>): IrFunction? {
        val wrapperIrFunction = irFactory.generateWrapperHeader(target, includedParams) ?: return null

        val call = when (target) {
            is IrConstructor ->
                IrDelegatingConstructorCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.unitType, target.symbol
                )
            is IrSimpleFunction ->
                IrCallImpl.fromSymbolOwner(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)
        }

        for (arg in wrapperIrFunction.allTypeParameters) {
            call.putTypeArgument(arg.index, arg.defaultType)
        }

        call.dispatchReceiver = wrapperIrFunction.dispatchReceiverParameter?.let { dispatchReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                dispatchReceiver.symbol
            )
        }

        call.extensionReceiver = wrapperIrFunction.extensionReceiverParameter?.let { extensionReceiver ->
            IrGetValueImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                extensionReceiver.symbol
            )
        }

        var lastWrapperIndex = 0

        target.valueParameters.forEachIndexed { targetIndex, targetParam ->
            if (!includedParams[targetIndex]) {
                call.putValueArgument(targetIndex, null)
                return@forEachIndexed
            }

            val wrapperParam = wrapperIrFunction.valueParameters[lastWrapperIndex]
            call.putValueArgument(targetIndex, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, wrapperParam.symbol))
            lastWrapperIndex += 1
        }

        wrapperIrFunction.body = when (target) {
            is IrConstructor -> {
                irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(call))
            }
            is IrSimpleFunction -> {
                irFactory.createExpressionBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, call)
            }
        }

        return wrapperIrFunction
    }

    private fun IrFactory.generateWrapperHeader(oldFunction: IrFunction, includedParams: List<Boolean>): IrFunction? {
        val res = when (oldFunction) {
            is IrConstructor -> {
                buildConstructor {
                    setSourceRange(oldFunction)
                    origin = VERSION_OVERLOAD_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
                    containerSource = oldFunction.containerSource
                }
            }
            is IrSimpleFunction -> buildFun {
                setSourceRange(oldFunction)
                origin = VERSION_OVERLOAD_WRAPPER
                name = oldFunction.name
                visibility = oldFunction.visibility
                modality = oldFunction.modality
                returnType = oldFunction.returnType
                isInline = oldFunction.isInline
                isSuspend = oldFunction.isSuspend
                containerSource = oldFunction.containerSource
            }
        }

        res.parent = oldFunction.parent
        res.addDeprecatedAnnotation(DeprecationLevel.HIDDEN)
        res.copyAnnotationsFrom(oldFunction)
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters = res.generateNewValueParameters(oldFunction, includedParams)

        return res
    }

    private fun IrFunction.generateNewValueParameters(oldFunction: IrFunction, includedParams: List<Boolean>): List<IrValueParameter> {
        val result = mutableListOf<IrValueParameter>()
        val containerClass = this.parent as? IrClass

        // FIXME: copy default value? older version of fun$default is not available for older kotlin function if it is not copied
        if (containerClass != null) {
            oldFunction.valueParameters.forEachIndexed { i, param ->
                if (includedParams[i]) {
                    val newParam = param.copyTo(this).transform(GetValueTransformer(containerClass, this), null)
                    result.add(newParam)
                }
            }
        } else {
            oldFunction.valueParameters.forEachIndexed { i, param ->
                if (includedParams[i]) {
                    val newParam = param.copyTo(this)
                    result.add(newParam)
                }
            }
        }

        return result
    }

    private fun IrFunction.addDeprecatedAnnotation(level: DeprecationLevel) {
        val annotation = deprecationBuilder.buildAnnotationCall(level) ?: return
        annotations += annotation
    }
}

private class GetValueTransformer(val irClass: IrClass, val irFunction: IrFunction) : IrElementTransformerVoid() {
    override fun visitGetValue(expression: IrGetValue): IrGetValue {
        return (super.visitGetValue(expression) as IrGetValue).remapSymbolParent(
            classRemapper = { irClass },
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