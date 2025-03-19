package org.rri.ideals.server.executecommand;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public final class WorkspaceExecuteCommandService {

  private enum Commands {
    RELOAD_PROJECTS("ideals.reloadProjects");

    private final String commandId;

    Commands(String commandId) {
      this.commandId = commandId;
    };

    public static Commands fromCommandId(String commandId) {
      return Arrays.stream(Commands.values())
          .filter(command -> command.commandId.equalsIgnoreCase(commandId))
          .findFirst()
          .orElse(null);
    }

    public String commandId() {
      return this.commandId;
    }
  };

  public static WorkspaceExecuteCommandService getInstance() {
    return ApplicationManager.getApplication().getService(WorkspaceExecuteCommandService.class);
  }

  public CompletableFuture<Object> executeCommand(String commandId, List<Object> arguments, @NotNull Project project) {
    if (commandId == null) {
      throw new IllegalArgumentException("The workspace/executeCommand has empty command");
    }

    final var command = Commands.fromCommandId(commandId);
    if (command == null) {
      throw new UnsupportedOperationException(String.format("The command '%s' is not supported", commandId));
    }

    return switch (command) {
      case RELOAD_PROJECTS -> CompletableFuture.supplyAsync(() -> new ReloadProjectsExecuteCommand().execute(project), AppExecutorUtil.getAppExecutorService());
    };
  }

  public static List<String> getCommands() {
    return Arrays.stream(Commands.values()).map(Commands::commandId).toList();
  }
}
