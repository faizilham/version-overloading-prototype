package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import kotlin.reflect.full.instanceParameter

class VersionOverloadingPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val extension = VersionOverloadingExtension()
        FirExtensionRegistrarAdapter.registerExtension(extension)
        IrGenerationExtension.registerExtension(extension)
    }
}
