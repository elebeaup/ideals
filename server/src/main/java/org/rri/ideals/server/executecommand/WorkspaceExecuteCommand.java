package org.rri.ideals.server.executecommand;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface WorkspaceExecuteCommand<T> {

  T execute(@NotNull Project project, Object... arguments);
}
