package org.rri.ideals.server.hover;

import com.intellij.lang.documentation.impl.TargetsKt;
import com.intellij.openapi.application.ReadAction;
import com.intellij.platform.backend.documentation.impl.DocumentationRequest;
import com.intellij.platform.backend.documentation.impl.ImplKt;
import io.github.furstenheim.CopyDown;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.jetbrains.annotations.NotNull;
import org.rri.ideals.server.commands.ExecutorContext;
import org.rri.ideals.server.commands.LspCommand;

import java.util.Optional;
import java.util.function.Supplier;

public class HoverCommand extends LspCommand<Hover> {
    @Override
    protected @NotNull Supplier<@NotNull String> getMessageSupplier() {
        return () -> "Hover call";
    }

    @Override
    protected boolean isCancellable() {
        return false;
    }

    @Override
    protected boolean isRunInEdt() {
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    protected Hover execute(@NotNull ExecutorContext ctx) {
        return ReadAction.compute(() -> TargetsKt.documentationTargets(ctx.getPsiFile(), ctx.getEditor().getCaretModel().getOffset()).stream()
                .findFirst()
                .flatMap(target -> {
                    //noinspection OverrideOnly
                    final var request = new DocumentationRequest(target.createPointer(), target.computePresentation());
                    return Optional.ofNullable(ImplKt.computeDocumentationBlocking(request.getTargetPointer())).map(res -> {
                        final var htmlToMarkdownConverter = new CopyDown();
                        final var markdown = htmlToMarkdownConverter.convert(res.getHtml());
                        return new Hover(new MarkupContent(MarkupKind.MARKDOWN, markdown));
                    });
                }).orElse(null));
    }
}
