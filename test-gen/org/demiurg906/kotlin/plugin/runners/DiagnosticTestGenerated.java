

package org.demiurg906.kotlin.plugin.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.demiurg906.kotlin.plugin.GenerateTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("testData/checkers")
@TestDataPath("$PROJECT_ROOT")
public class DiagnosticTestGenerated extends AbstractDiagnosticTest {
  @Test
  public void testAllFilesPresentInCheckers() {
    KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("testData/checkers"), Pattern.compile("^(.+)\\.kt$"), null, true);
  }

  @Test
  @TestMetadata("checker.kt")
  public void testChecker() {
    runTest("testData/checkers/checker.kt");
  }
}
