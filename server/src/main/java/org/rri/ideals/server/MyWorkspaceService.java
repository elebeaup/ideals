package org.rri.ideals.server;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.dependency.DependencyCommands;
import org.rri.ideals.server.symbol.WorkspaceSymbolService;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MyWorkspaceService implements WorkspaceService {
  private static final Logger LOG = Logger.getInstance(MyWorkspaceService.class);
  
  @NotNull
  private final LspSession session;
  private DependencyCommands dependencyCommands;

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

  private @NotNull WorkspaceSymbolService workspaceSymbol() {
    return session.getProject().getService(WorkspaceSymbolService.class);
  }

  @SuppressWarnings("deprecation")
  @Override
  public CompletableFuture<Either<List<? extends SymbolInformation>, List<? extends WorkspaceSymbol>>> symbol(WorkspaceSymbolParams params) {
    return workspaceSymbol().runSearch(params.getQuery());
  }
  
  @Override
  public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
    String command = params.getCommand();
    List<Object> arguments = params.getArguments() != null ? params.getArguments() : Collections.emptyList();
    
    LOG.info("Executing command: " + command);
    
    try {
      switch (command) {
        case "repo-prompt.analyzeDependencies":
          return getDependencyCommands().analyzeDependencies(arguments);
        case "repo-prompt.visualizeDependencies":
          return getDependencyCommands().visualizeDependencies(arguments);
        case "repo-prompt.analyzeImpact":
          return getDependencyCommands().analyzeImpact(arguments);
        case "repo-prompt.getProjectStructure":
          return getDependencyCommands().getProjectStructure(arguments);
        case "java.project.getClasspathInfo":
        case "java.project.listSourcePaths":
        case "java.project.getPackageData":
          // Generic placeholder for standard Java commands
          return CompletableFuture.completedFuture(Collections.emptyMap());
        case "python.getDependencyGraph":
        case "python.analyzeModuleDependencies":
        case "python.projectDependencies":
          // For Python commands, return an appropriate error message
          return CompletableFuture.completedFuture("Python dependency analysis is not supported");
        case "_commands":
          // Return a list of supported commands
          return CompletableFuture.completedFuture(List.of(
              "repo-prompt.analyzeDependencies",
              "repo-prompt.visualizeDependencies", 
              "repo-prompt.analyzeImpact",
              "repo-prompt.getProjectStructure"
          ));
        default:
          LOG.warn("Unsupported command: " + command);
          return CompletableFuture.completedFuture("Unsupported command: " + command);
      }
    } catch (Exception e) {
      LOG.error("Error executing command: " + command, e);
      return CompletableFuture.completedFuture("Error executing command: " + e.getMessage());
    }
  }
  
  private DependencyCommands getDependencyCommands() {
    if (dependencyCommands == null) {
      dependencyCommands = new DependencyCommands(session.getProject());
    }
    return dependencyCommands;
  }
}