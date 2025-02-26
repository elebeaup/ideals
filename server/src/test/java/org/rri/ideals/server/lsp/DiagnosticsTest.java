package org.rri.ideals.server.lsp;

import org.eclipse.lsp4j.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.rri.ideals.server.LspPath;
import org.rri.ideals.server.util.MiscUtil;

import java.nio.file.Files;
import java.util.List;

public class DiagnosticsTest extends LspServerTestBase {

  @Override
  protected String getProjectRelativePath() {
    return "lsp/project1";
  }

  @Test
  public void didOpen() {
    final var filePath = LspPath.fromLocalPath(getProjectPath().resolve("src/Test.java"));

    //noinspection CodeBlock2Expr
    final var didOpenTextDocumentParams = MiscUtil.with(new DidOpenTextDocumentParams(), params -> {
      params.setTextDocument(MiscUtil.with(new TextDocumentItem(), item -> {
        item.setUri(filePath.toLspUri());

        item.setText(MiscUtil.makeThrowsUnchecked(() -> Files.readString(filePath.toPath())));
        item.setVersion(1);
      }));
    });

    server().getTextDocumentService().didOpen(didOpenTextDocumentParams);

    final var diagnosticsParams = client().waitAndGetDiagnosticsPublished();

    Assert.assertEquals(filePath, LspPath.fromLspUri(diagnosticsParams.getUri()));
    Assert.assertEquals(1, diagnosticsParams.getDiagnostics().size());


    final var diagnostic = diagnosticsParams.getDiagnostics().get(0);
    Assert.assertEquals("';' expected", diagnostic.getMessage());
    Assert.assertEquals(new Range(new Position(3, 13), new Position(3, 14)), diagnostic.getRange());
  }

  @Test
  public void didChange() {
    final var filePath = LspPath.fromLocalPath(getProjectPath().resolve("src/Test.java"));

    sendOpen(filePath);

    final var params = new DidChangeTextDocumentParams();
    params.setTextDocument(MiscUtil.with(new VersionedTextDocumentIdentifier(), item -> {
      item.setUri(filePath.toLspUri());
      item.setVersion(2);
    }));

    params.setContentChanges(List.of(
            new TextDocumentContentChangeEvent(
                    new Range(new Position(3, 13), new Position(3, 14)),
                    ";+  // expression expected"
            )
    ));

    server().getTextDocumentService().didChange(params);

    final var diagnosticsParams = client().waitAndGetDiagnosticsPublished();

    Assert.assertEquals(filePath, LspPath.fromLspUri(diagnosticsParams.getUri()));
    Assert.assertEquals(2, diagnosticsParams.getDiagnostics().size());

    final var diagnostic1 = diagnosticsParams.getDiagnostics().get(0);
    Assert.assertEquals("Not a statement", diagnostic1.getMessage());
    Assert.assertEquals(new Range(new Position(3, 14), new Position(3, 15)), diagnostic1.getRange());

    final var diagnostic2 = diagnosticsParams.getDiagnostics().get(1);
    Assert.assertEquals("Expression expected", diagnostic2.getMessage());
    Assert.assertEquals(new Range(new Position(3, 15), new Position(3, 16)), diagnostic2.getRange());
  }

  private void sendOpen(@NotNull LspPath filePath) {
    //noinspection CodeBlock2Expr
    final var didOpenTextDocumentParams = MiscUtil.with(new DidOpenTextDocumentParams(), params -> {
      params.setTextDocument(MiscUtil.with(new TextDocumentItem(), item -> {
        item.setUri(filePath.toLspUri());

        item.setText(MiscUtil.makeThrowsUnchecked(() -> Files.readString(filePath.toPath())));
        item.setVersion(1);
      }));
    });

    server().getTextDocumentService().didOpen(didOpenTextDocumentParams);
  }


}
