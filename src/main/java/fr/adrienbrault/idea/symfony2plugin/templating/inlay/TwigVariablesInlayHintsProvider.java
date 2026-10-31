package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.language.editor.inlay.DeclarativeInlayTreeSink;
import consulo.language.editor.inlay.HintColorKind;
import consulo.language.editor.inlay.HintFontSize;
import consulo.language.editor.inlay.HintFormat;
import consulo.language.editor.inlay.HintMarginPadding;
import consulo.language.editor.inlay.InlayActionData;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPointerManager;
import consulo.localize.LocalizeValue;
import consulo.project.DumbService;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Block-level inlay hint for Twig templates rendered by a controller.
 * Clicking opens a tree popup for code insertion and navigation.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigVariablesInlayHintsProvider implements DeclarativeInlayHintsProvider {
    public static final String PROVIDER_ID = "symfony.twig.root.variables";

    private static final HintFormat FORMAT = new HintFormat(HintColorKind.Default, HintFontSize.ABitSmallerThanInEditor, HintMarginPadding.MarginAndSmallerPadding);

    @Override
    public @Nullable DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor) {
        if (!(file instanceof TwigFile) || DumbService.isDumb(file.getProject()) || !Symfony2ProjectComponent.isEnabled(file)) {
            return null;
        }
        return new TwigVariablesInlayCollector();
    }

    @Override
    public Language getLanguage() {
        return TwigLanguage.INSTANCE;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Twig Root Variables");
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Shows the variables a controller passes to a Twig template, above the template");
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.empty();
    }

    @Override
    public InlayGroup getGroup() {
        return InlayGroup.OTHER_GROUP;
    }

    private static class TwigVariablesInlayCollector implements DeclarativeInlayHintsCollector.OwnBypassCollector {
        @Override
        @RequiredReadAction
        public void collectHintsForFile(PsiFile file, DeclarativeInlayTreeSink sink) {
            if (!(file instanceof TwigFile twigFile)) {
                return;
            }

            if (TwigUtil.findTwigFileController(twigFile).isEmpty() && TwigUtil.getTwigFileMethodUsageOnIndex(twigFile).isEmpty()) {
                return;
            }

            Map<String, PsiVariable> variables = TwigTypeResolveUtil.collectScopeVariables(twigFile);
            if (variables.isEmpty()) {
                return;
            }

            InlayActionData actionData = new InlayActionData(
                new InlayActionPayload.PsiPointerInlayActionPayload(SmartPointerManager.createPointer(twigFile)),
                TwigVariablesInlayActionHandler.HANDLER_ID
            );
            sink.addPresentation(new DeclarativeInlayPosition.AboveLineIndentedPosition(0), FORMAT, builder ->
                builder.clickHandlerScope(actionData, scope -> {
                    scope.icon(Symfony2Icons.SYMFONY);
                    scope.text(" Variables (" + variables.size() + ")");
                })
            );
        }
    }
}
