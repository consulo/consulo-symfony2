package fr.adrienbrault.idea.symfony2plugin.stimulus;

import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Completion for Stimulus controllers in HTML and Twig.
 * <p>
 * Supports:
 * - HTML: &lt;div data-controller="|"&gt; &lt;/div&gt;
 * - Twig: {{ stimulus_controller('|') }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class StimulusControllerCompletionRegistrar {

    /**
     * HTML: &lt;div data-controller="|"&gt; &lt;/div&gt;
     * <p>
     * Registered through {@link StimulusHtmlDataControllerCompletionRegistrarImpl} (nested classes cannot carry {@code @ExtensionImpl}).
     */
    public static class HtmlDataControllerCompletionRegistrar implements GotoCompletionRegistrar {
        @Override
        public void register(GotoCompletionRegistrarParameter registrar) {
            registrar.register(htmlDataControllerPattern(), psiElement -> {
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return null;
                }
                return new DataController(psiElement);
            });
        }
    }

    /**
     * Twig: {{ stimulus_controller('|') }}
     * <p>
     * Registered through {@link StimulusTwigCompletionRegistrarImpl} (nested classes cannot carry {@code @ExtensionImpl}).
     */
    public static class StimulusTwigCompletionRegistrar implements GotoCompletionRegistrar {
        @Override
        public void register(GotoCompletionRegistrarParameter registrar) {
            registrar.register(stimulusControllerPattern(), psiElement -> {
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return null;
                }
                return new StimulusTwigFunction(psiElement);
            });
        }
    }

    /**
     * HTML attribute value completion for data-controller
     */
    public static final class DataController extends GotoCompletionProvider {
        public DataController(PsiElement element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            if (!Symfony2ProjectComponent.isEnabled(getProject())) {
                return Collections.emptyList();
            }

            List<LookupElement> lookupElements = new ArrayList<>();
            for (StimulusController controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                lookupElements.add(StimulusControllerCompletion.createLookupElement(controller, false));
            }
            return lookupElements;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String controllerName = element.getText().trim();
            if (controllerName.isEmpty()) {
                return Collections.emptyList();
            }

            return getNavigationTargets(getProject(), controllerName);
        }
    }

    /**
     * Twig function completion for stimulus_controller()
     */
    public static final class StimulusTwigFunction extends GotoCompletionProvider {
        public StimulusTwigFunction(PsiElement element) {
            super(element);
        }

        @Override
        public void getLookupElements(GotoCompletionProviderLookupArguments arguments) {
            if (!Symfony2ProjectComponent.isEnabled(getProject())) {
                return;
            }

            List<LookupElement> lookupElements = new ArrayList<>();
            for (StimulusController controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                lookupElements.add(StimulusControllerCompletion.createLookupElement(controller, true));
            }

            CompletionResultSet completionResultSet = TwigUtil.withCompletionPrefix(arguments.getParameters(), arguments.getResultSet());
            completionResultSet.addAllElements(lookupElements);
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String controllerName = element.getText().trim();
            if (controllerName.isEmpty()) {
                return Collections.emptyList();
            }

            return getNavigationTargets(getProject(), resolveControllerName(controllerName));
        }

        private String resolveControllerName(String controllerName) {
            for (StimulusController controller : StimulusControllerCompletion.getAllControllers(getProject()).values()) {
                if (controllerName.equals(controller.getTwigName()) || controllerName.equals(controller.getNormalizedName())) {
                    String normalizedName = controller.getNormalizedName();
                    return normalizedName != null ? normalizedName : controllerName;
                }
            }
            return controllerName;
        }
    }

    private static PsiElementPattern.Capture<PsiElement> htmlDataControllerPattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(
                XmlPatterns.xmlAttributeValue()
                    .withParent(XmlPatterns.xmlAttribute("data-controller"))
            )
            .inFile(XmlPatterns.psiFile(XmlFile.class));
    }

    private static PsiElementPattern.Capture<PsiElement> stimulusControllerPattern() {
        return PlatformPatterns
            .psiElement(TwigTokenTypes.STRING_TEXT)
            .afterLeafSkipping(
                PlatformPatterns.or(
                    PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                    PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                    PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                ),
                PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("stimulus_controller")
            )
            .withLanguage(TwigLanguage.INSTANCE);
    }

    private static Collection<PsiElement> getNavigationTargets(Project project, String normalizedKey) {
        Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
            StimulusControllerStubIndex.KEY,
            normalizedKey,
            GlobalSearchScope.allScope(project)
        );

        return new ArrayList<>(PsiElementUtils.convertVirtualFilesToPsiFiles(project, containingFiles));
    }
}
