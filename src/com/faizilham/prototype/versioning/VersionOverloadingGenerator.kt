package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.valueArguments
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

typealias VersionNumber = Int

class VersionOverloadingGenerator(private val pluginContext: IrPluginContext) : IrElementVisitorVoid {
    companion object {
        val VersionAnnotation = FqName("com.faizilham.prototype.versioning.Version")
    }

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        var lastVersionNumber: Int? = null
        val versions = mutableMapOf<VersionNumber, MutableList<IrValueParameter>>()
        val baseParameters = mutableListOf<IrValueParameter>()
        declaration.valueParameters.reversed().forEach {
            val annotation = it.getAnnotation(VersionAnnotation)

            if (annotation == null) {
                baseParameters.add(it)
                return@forEach
            }

            val versionNumber = (annotation.valueArguments[0] as? IrConst<*>)?.value as? VersionNumber ?: return@forEach

            versions.getOrPut(versionNumber) { mutableListOf() }.add(it)
        }
    }
}
