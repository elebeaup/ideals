package org.rri.ideals.server.codeactions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rri.ideals.server.LspLightBasePlatformTestCase5;
import org.rri.ideals.server.LspPath;
import org.rri.ideals.server.TestUtil;
import org.rri.ideals.server.commands.ExecutorContext;

import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CodeActionServiceTest extends LspLightBasePlatformTestCase5 {

  private CodeActionService codeActionService;

  @BeforeEach
  void setUp() {
    codeActionService = getProject().getService(CodeActionService.class);
  }

  @Test
  void testGetCodeActions() throws ExecutionException, InterruptedException {
    final var text = """
        class A {
          public static void main() {
            int a = "";
            
            System.out.println();
          }
        }
        """;

    final var expectedIntentions = Stream.of(
        "Convert to atomic",
        "Split into declaration and assignment"
    ).sorted().toList();

    final var expectedQuickFixes = Stream.of(
        "Change variable 'a' type to 'String'"
    ).toList();

    final var file = myFixture.configureByText("test.java", text);
    final var orExpressionRange = TestUtil.newRange(2, 8, 2, 8);
    moveCaretToPosition(orExpressionRange.getStart());
    final var executorContext = new ExecutorContext(file, myFixture.getEditor(), new TestUtil.DumbCancelChecker());
    final var codeActionsBeforeDiagnostic = codeActionService.getCodeActions(orExpressionRange, executorContext);

    assertTrue(codeActionsBeforeDiagnostic.stream().allMatch(it -> it.getKind().equals(CodeActionKind.Refactor)));
    assertEquals(expectedIntentions, codeActionsBeforeDiagnostic.stream().map(CodeAction::getTitle).sorted().toList());

    myFixture.doHighlighting();

    final var quickFixes = codeActionService.getCodeActions(orExpressionRange, executorContext);
    quickFixes.removeAll(codeActionsBeforeDiagnostic);

    assertTrue(quickFixes.stream().allMatch(it -> it.getKind().equals(CodeActionKind.QuickFix)));
    assertEquals(expectedQuickFixes, quickFixes.stream().map(CodeAction::getTitle).sorted().toList());
  }

  @Test
  void testQuickFixFoundAndApplied() throws ExecutionException, InterruptedException {
    final var before = """
        class A {
           final int x = "a";
        }
        """;

    final var after = """
        class A {
           final java.lang.String x = "a";
        }
        """;

    final var actionTitle = "Change field 'x' type to 'String'";

    final var file = myFixture.configureByText("test.java", before);
    final var xVariableRange = TestUtil.newRange(1, 13, 1, 13);
    var path = LspPath.fromVirtualFile(file.getVirtualFile());
    moveCaretToPosition(xVariableRange.getStart());
    final var executorContext = new ExecutorContext(file, myFixture.getEditor(), null);

    myFixture.doHighlighting();

    final var codeAction = codeActionService.getCodeActions(xVariableRange, executorContext).stream()
        .filter(it -> it.getTitle().equals(actionTitle))
        .findFirst()
        .orElseThrow(() -> new AssertionError("action not found"));

    final var edit = codeActionService.applyCodeAction((ActionData) codeAction.getData(), actionTitle, executorContext);
    assertEquals(after, TestUtil.applyEdits(file.getText(), edit.getChanges().get(path.toLspUri())));

    // checking the quick fix doesn't actually change the file
    ApplicationManager.getApplication().invokeAndWait(() -> {
      final var reloaded = PsiManager.getInstance(getProject()).findFile(file.getVirtualFile());
      assertNotNull(reloaded);
      assertEquals(before, reloaded.getText());
      final var reloadedDoc = PsiDocumentManager.getInstance(getProject()).getDocument(reloaded);
      assertNotNull(reloadedDoc);
      assertEquals(before, reloadedDoc.getText());
    });
  }
}
