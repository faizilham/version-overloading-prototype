package com.faizilham.prototype.versioning

import com.faizilham.prototype.versioning.checkers.VersioningFirRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

class VersionOverloadingPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(VersionOverloadingExtension())
        FirExtensionRegistrarAdapter.registerExtension(VersioningFirRegistrar())
    }
}
