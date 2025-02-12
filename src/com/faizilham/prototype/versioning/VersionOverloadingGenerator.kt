package com.faizilham.prototype.versioning

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
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds

typealias VersionNumber = String

class VersionOverloadingGenerator(context: IrPluginContext) : IrElementVisitor<Unit, MutableList<IrFunction>?> {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns

    private val deprecationBuilder = DeprecationBuilder(context)

    companion object {
        val VersionAnnotation = FqName("com.faizilham.prototype.versioning.Version")
        val VersionOverloadsAnnotation = FqName("com.faizilham.prototype.versioning.VersionOverloads")
        val VERSION_OVERLOAD_WRAPPER by IrDeclarationOriginImpl
    }

    override fun visitElement(element: IrElement, data: MutableList<IrFunction>?) {
        when (element) {
            is IrClass -> {
                val addedWrappers = mutableListOf<IrFunction>()
                element.acceptChildren(this, addedWrappers)
                element.addAll(addedWrappers)

            }
            is IrFile -> {
                val addedWrappers = mutableListOf<IrFunction>()
                element.acceptChildren(this, addedWrappers)
                element.addChildren(addedWrappers)
            }
            is IrDeclaration,
            is IrModuleFragment -> element.acceptChildren(this, null)
            else -> {}
        }
    }

    override fun visitFunction(func: IrFunction, data: MutableList<IrFunction>?) {
        if (data == null) return
        val overloadCounts = getPreviousVersionsParamCounts(func) ?: return

        for (paramCount in overloadCounts) {
            val wrapper = generateWrapper(func, paramCount) ?: continue
            data.add(wrapper)
        }
    }

    private fun getPreviousVersionsParamCounts(func: IrFunction) : List<Int>? {
        // Generate previous versions parameter count (since the latest version is equivalent to the original IrFunction)
        // Assumptions (should enforced by checker):
        // 1. Version annotations are only added at optional parameters
        // 2. Optional parameters with versions are in the tail positions, non-annotated parameters are in the head
        // 3. Version annotations are in the increasing order
        // 4. TODO: exception, trailing lambdas are allowed to be non-optional

        if (!func.hasAnnotation(VersionOverloadsAnnotation)) return null

        val versionCounts = mutableListOf<Int>()
        var latestVersion = ""
        var paramsSize = 0

        func.valueParameters.forEach {
            val versionNumber = it.getVersionNumber()

            if (versionNumber > latestVersion) {
                versionCounts.add(paramsSize)
                latestVersion = versionNumber
            }

            paramsSize++
        }

        if (versionCounts.isEmpty()) return null

        return versionCounts
    }

    private fun IrValueParameter.getVersionNumber() : VersionNumber {
        val annotation = getAnnotation(VersionAnnotation) ?: return ""
        val versionNumber = (annotation.valueArguments[0] as? IrConst)?.value as? VersionNumber ?: ""
        return versionNumber
    }

    private fun generateWrapper(target: IrFunction, newParamCounts: Int): IrFunction? {
        val wrapperIrFunction = irFactory.generateWrapperHeader(target, newParamCounts) ?: return null

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

        for (i in 0..<newParamCounts) {
            val valueParameter = wrapperIrFunction.valueParameters[i]
            call.putValueArgument(i, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueParameter.symbol))
        }

        for (i in newParamCounts..<target.valueParameters.size) {
            call.putValueArgument(i, null)
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

    private fun IrFactory.generateWrapperHeader(oldFunction: IrFunction, newParamCounts: Int): IrFunction? {
        val res = when (oldFunction) {
            is IrConstructor -> {
                buildConstructor {
                    setSourceRange(oldFunction)
                    origin = VERSION_OVERLOAD_WRAPPER
                    name = oldFunction.name
                    visibility = oldFunction.visibility
                    returnType = oldFunction.returnType
                    isInline = oldFunction.isInline
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
            }
        }

        res.parent = oldFunction.parent
        res.addDeprecatedAnnotation()
        res.copyAnnotationsFrom(oldFunction)
        res.copyTypeParametersFrom(oldFunction)
        res.dispatchReceiverParameter = oldFunction.dispatchReceiverParameter?.copyTo(res)
        res.extensionReceiverParameter = oldFunction.extensionReceiverParameter?.copyTo(res)
        res.valueParameters += res.generateNewValueParameters(oldFunction, newParamCounts)
        return res
    }

    private fun IrFunction.addDeprecatedAnnotation() {
        val annotation = deprecationBuilder.buildAnnotationCall(DeprecationLevel.HIDDEN) ?: return
        annotations += annotation
    }

    private fun IrFunction.generateNewValueParameters(
        oldFunction: IrFunction,
        newParamCounts: Int
    ): List<IrValueParameter> {
        val result = mutableListOf<IrValueParameter>()

        for (i in 0..<newParamCounts) {
            val oldValueParameter = oldFunction.valueParameters[i]
            if (oldValueParameter.defaultValue != null) {
                result.add(
                    oldValueParameter.copyTo(
                        this,
                        defaultValue = null,
                        isCrossinline = oldValueParameter.isCrossinline,
                        isNoinline = oldValueParameter.isNoinline
                    )
                )
            } else if (oldValueParameter.defaultValue == null) {
                result.add(oldValueParameter.copyTo(this))
            }
        }

        return result
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