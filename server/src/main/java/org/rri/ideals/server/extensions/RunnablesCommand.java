package org.rri.ideals.server.extensions;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.BaseRunConfigurationAction;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.JavaCommandLine;
import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.LineMarkerActionWrapper;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.target.TargetProgressIndicator;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.commands.ExecutorContext;
import org.rri.ideals.server.commands.LspCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.rri.ideals.server.util.MiscUtil.distinctByKey;

public class RunnablesCommand extends LspCommand<List<Runnable>> {

  private static final Logger LOG = Logger.getInstance(RunnablesCommand.class);

  @Override
  protected @NotNull Supplier<@NotNull String> getMessageSupplier() {
    return () -> "experimental/runnables call";
  }

  @Override
  protected boolean isCancellable() {
    return false;
  }

  @Override
  protected List<Runnable> execute(@NotNull ExecutorContext ctx) {
    final var project = ctx.getEditor().getProject();

    return DaemonCodeAnalyzerImpl.getLineMarkers(ctx.getEditor().getDocument(), project)
        .stream()
        .flatMap(lineMarkerInfo -> {
          var gutter = lineMarkerInfo.createGutterRenderer();

          if (gutter.getPopupMenuActions() != null && lineMarkerInfo.getElement() != null) {
            return Arrays.stream(((DefaultActionGroup) gutter.getPopupMenuActions()).getChildActionsOrStubs()).filter(anAction -> "Run context configuration".equals(anAction.getTemplateText())).filter(anAction -> anAction instanceof LineMarkerActionWrapper).map(anAction -> {
              var lineMarkerActionWrapper = (LineMarkerActionWrapper) anAction;
              var dataContext = SimpleDataContext.builder().add(CommonDataKeys.PROJECT, project)
                  .add(CommonDataKeys.PSI_ELEMENT, lineMarkerInfo.getElement())
                  .add(PsiLocation.DATA_KEY, new PsiLocation<>(lineMarkerInfo.getElement()))
                  .build();
              var executor = ((ExecutorAction) lineMarkerActionWrapper.getDelegate()).getExecutor();
              var configurationContext = ConfigurationContext.getFromContext(dataContext, "");
              var contexts = configurationContext.getConfigurationsFromContext();
              if (contexts != null && !contexts.isEmpty()) {
                var min = Collections.min(contexts, ConfigurationFromContext.COMPARATOR);
                var runnerAndConfigurationSettings = min.getConfigurationSettings();
                var actionName = executor.getActionName() + " '" + BaseRunConfigurationAction.suggestRunActionName(runnerAndConfigurationSettings.getConfiguration()) + "'";

                try {
                  var environment = ExecutionEnvironmentBuilder.create(executor, runnerAndConfigurationSettings).build();
                  var currentState = environment.getState();
                  var targetEnvironment = environment.getPreparedTargetEnvironment(currentState, TargetProgressIndicator.EMPTY);
                  var commandLine = ((JavaCommandLine) currentState).getJavaParameters().toCommandLine();
                  return new Runnable(actionName, new Runnable.Arguments(
                      commandLine.getWorkingDirectory().toString(),
                      commandLine.getExePath(),
                      commandLine.getParametersList().getList()
                  ));
                } catch (ExecutionException e) {
                  LOG.error("Unable to create an execution environment", e);
                }
              }

              return null;
            }).filter(Objects::nonNull);
          }

          return Stream.of();
        })
                .filter(distinctByKey(Runnable::label))
        .toList();
  }
}
