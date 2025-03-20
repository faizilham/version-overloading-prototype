package org.demiurg906.kotlin.plugin.runners

import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.configureFirParser

open class AbstractDiagnosticTest : BaseTestRunner() {
    override fun TestConfigurationBuilder.configuration() {
        commonFirWithPluginFrontendConfiguration()
        configureFirParser(FirParser.Psi)
    }
}
