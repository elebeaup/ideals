package org.rri.ideals.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.executecommand.WorkspaceExecuteCommandService;
import org.rri.ideals.server.symbol.WorkspaceSymbolService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyWorkspaceService implements WorkspaceService {
  @NotNull
  private final LspSession session;

  public MyWorkspaceService(@NotNull LspSession session) {
    this.session = session;
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {

  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {

  }

  @Override
  public void didRenameFiles(RenameFilesParams params) {
    // Refresh file system to avoid false positives in diagnostics (see #38)
    //
    // TODO
    //  it would probably be better to move this into didOpen
    //  because the order of and delays between calls didClose/didOpen/didRenameFiles
    //  during file rename seems client-specific
    //  so VFS refresh may happen too late and thus have no effect
    ApplicationManager.getApplication().invokeAndWait(() -> VirtualFileManager.getInstance().syncRefresh());
  }

  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    return WorkspaceExecuteCommandService.getInstance()
        .executeCommand(params.getCommand(), params.getArguments(), session.getProject());
  }

  private @NotNull WorkspaceSymbolService workspaceSymbol() {
    return session.getProject().getService(WorkspaceSymbolService.class);
  }

  @SuppressWarnings("deprecation")
  @Override
  public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
    return workspaceSymbol().runSearch(params.getQuery());
  }
}
