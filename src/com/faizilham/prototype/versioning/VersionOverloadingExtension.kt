package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class VersionOverloadingExtension : FirExtensionRegistrar(), IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.acceptChildren(VersionOverloadingGenerator(pluginContext), null)
    }

    override fun ExtensionRegistrarContext.configurePlugin() {
        +::CheckerExtension
    }

    class CheckerExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
        override val declarationCheckers = object : DeclarationCheckers() {
            override val functionCheckers = setOf(VersionOverloadingChecker)
        }
    }
}