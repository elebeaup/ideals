package org.rri.ideals.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.rules.TestNameExtension;
import org.eclipse.lsp4j.Position;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

public abstract class LspLightBasePlatformTestCase5 {

  @RegisterExtension
  private final TestNameExtension testNameRule = new TestNameExtension();
  @RegisterExtension
  private final BasePlatefomExtension basePlatefomExtension = new BasePlatefomExtension();

  protected CodeInsightTestFixture myFixture;

  protected void moveCaretToPosition(final Position position) {
    ApplicationManager.getApplication().invokeAndWait(() -> {
      myFixture.getEditor().getCaretModel().moveToLogicalPosition(new LogicalPosition(position.getLine(), position.getCharacter()));
    });
  }

  protected Project getProject() {
    return myFixture.getProject();
  }

  public class BasePlatefomExtension extends BasePlatformTestCase implements
      BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
      setUp();
      LspLightBasePlatformTestCase5.this.myFixture = myFixture;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
      tearDown();
    }

    @Override
    protected boolean isIconRequired() {
      return true;
    }
  }
}
