package org.demiurg906.kotlin.plugin

import org.demiurg906.kotlin.plugin.runners.AbstractBoxTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractBoxTest> {
                model("versions")
            }
        }
    }
}
