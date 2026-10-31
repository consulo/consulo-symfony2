package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.codeEditor.Editor;
import consulo.codeEditor.event.EditorMouseEvent;
import consulo.language.editor.inlay.InlayActionHandler;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.psi.PsiElement;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import fr.adrienbrault.idea.symfony2plugin.templating.inlay.ui.TwigVariablesTreePopup;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Map;

/**
 * Opens the variables popup of the {@link TwigVariablesInlayHintsProvider} hint; the variables are collected again, they may have
 * changed since the hint was shown
 */
@ExtensionImpl
public class TwigVariablesInlayActionHandler implements InlayActionHandler {
    public static final String HANDLER_ID = "symfony.twig.root.variables";

    @Override
    public String getHandlerId() {
        return HANDLER_ID;
    }

    @Override
    public boolean isPlainClickEnabled() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void handleClick(EditorMouseEvent e, InlayActionPayload payload) {
        if (!(payload instanceof InlayActionPayload.PsiPointerInlayActionPayload pointerPayload)) {
            return;
        }

        Editor editor = e.getEditor();
        ReadAction.nonBlocking(() -> {
                PsiElement element = pointerPayload.getPointer().getElement();
                return element instanceof TwigFile twigFile ? Map.entry(twigFile, TwigTypeResolveUtil.collectScopeVariables(twigFile)) : null;
            })
            .finishOnUiThread(app -> ModalityState.nonModal(), result -> {
                if (result != null && !result.getValue().isEmpty() && !editor.isDisposed()) {
                    TwigVariablesTreePopup.show(result.getKey(), editor, result.getValue(), null);
                }
            })
            .submit(AppExecutorUtil.getAppExecutorService());
    }
}
