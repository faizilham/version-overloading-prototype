package org.demiurg906.kotlin.plugin

import org.demiurg906.kotlin.plugin.runners.AbstractVersionOverloadTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
//            testClass<AbstractDiagnosticTest> {
//                model("diagnostics")
//            }
//
//            testClass<AbstractBoxTest> {
//                model("box")
//            }

            testClass<AbstractVersionOverloadTest> {
                model("versions")
            }
        }
    }
}
