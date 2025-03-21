package org.rri.ideals.server.executecommand;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CompileProjectExecuteCommand implements WorkspaceExecuteCommand<Void> {

  @Override
  public Void execute(@NotNull Project project, Object... arguments) {
    final var am = ActionManager.getInstance();
    final var context = SimpleDataContext.getProjectContext(project);
    final var event = AnActionEvent.createEvent(context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);

    ApplicationManager.getApplication().invokeAndWait(() -> {
      ActionUtil.performActionDumbAwareWithCallbacks(am.getAction("CompileDirty"), event);
    });
    return null;
  }
}
