package com.faizilham.prototype.versioning

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class VersionOverloadingPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(VersionOverloadingExtension())

        f(1)
        f(1) { it }
        f(1, 2) { it }
    }
}

fun f(x: Int, y: Int = 0, z: Int = 0, g: (Int) -> Int = { it }): Int = g(x + y + z)
