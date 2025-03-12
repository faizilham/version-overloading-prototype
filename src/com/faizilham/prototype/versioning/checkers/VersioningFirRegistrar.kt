package com.faizilham.prototype.versioning.checkers

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class VersioningFirRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::VersioningCheckerFirExtension
    }
}