package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.DumbService;
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
 * Upstream was registered as codeInsight.inlayProvider (language Twig); left unregistered.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@SuppressWarnings("UnstableApiUsage")
public class TwigVariablesInlayHintsProvider { // TODO consulo-port: InlayHintsProvider missing; rewrite as DeclarativeInlayHintsProvider

    // TODO consulo-port: SettingsKey / NoSettings missing; upstream:
    // private static final SettingsKey<NoSettings> KEY = new SettingsKey<>("symfony.twig.root.variables");

    public boolean isVisibleInSettings() {
        return true;
    }

    // TODO consulo-port: SettingsKey / NoSettings missing; upstream:
    // public SettingsKey<NoSettings> getKey() {
    //     return KEY;
    // }

    public String getName() {
        return "Twig Root Variables";
    }

    public @Nullable String getPreviewText() {
        return null;
    }

    // TODO consulo-port: ImmediateConfigurable / NoSettings missing; upstream:
    // public ImmediateConfigurable createConfigurable(NoSettings settings) {
    //     return changeListener -> new JPanel();
    // }
    //
    // public NoSettings createSettings() {
    //     return new NoSettings();
    // }

    // TODO consulo-port: InlayHintsCollector / NoSettings / InlayHintsSink missing; upstream signature:
    // public InlayHintsCollector getCollectorFor(PsiFile file, Editor editor, NoSettings settings, InlayHintsSink sink)
    public TwigVariablesInlayCollector getCollectorFor(PsiFile file, Editor editor) {
        return new TwigVariablesInlayCollector(editor);
    }

    public boolean isLanguageSupported(Language language) {
        return language.isKindOf(TwigLanguage.INSTANCE);
    }

    private static class TwigVariablesInlayCollector { // TODO consulo-port: was "extends FactoryInlayHintsCollector" (missing)
        private final Editor myEditor;

        TwigVariablesInlayCollector(Editor editor) {
            myEditor = editor;
        }

        // TODO consulo-port: InlayHintsSink missing; upstream signature: collect(PsiElement element, Editor editor, InlayHintsSink sink)
        public boolean collect(PsiElement element, Editor editor) {
            if (!(element instanceof TwigFile twigFile)) {
                return true;
            }

            if (DumbService.isDumb(element.getProject())) {
                return false;
            }

            if (!Symfony2ProjectComponent.isEnabled(element)) {
                return false;
            }

            if (TwigUtil.findTwigFileController(twigFile).isEmpty() && TwigUtil.getTwigFileMethodUsageOnIndex(twigFile).isEmpty()) {
                return false;
            }

            Map<String, PsiVariable> variables = TwigTypeResolveUtil.collectScopeVariables(element);
            if (variables.isEmpty()) {
                return false;
            }

            // TODO consulo-port: PresentationFactory / SequencePresentation / InlayHintsSink missing; upstream:
            // PresentationFactory factory = getFactory();
            // InlayPresentation icon = factory.inset(factory.scaledIcon(Symfony2Icons.SYMFONY, 0.77f), 0, 0, 1, 0);
            // InlayPresentation label = factory.smallText(" Variables (" + variables.size() + ")");
            // InlayPresentation combined = new SequencePresentation(List.of(icon, label));
            // InlayPresentation clickable = factory.referenceOnHover(combined, (event, translated) ->
            //     TwigVariablesTreePopup.show(twigFile, editor, variables, event)
            // );
            // InlayPresentation presentation = factory.inset(factory.offsetFromTopForSmallText(clickable), 0, 4, 1, 4);
            //
            // sink.addBlockElement(0, false, true, 0, presentation);
            return false;
        }
    }
}
