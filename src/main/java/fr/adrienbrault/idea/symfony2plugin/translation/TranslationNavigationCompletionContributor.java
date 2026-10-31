package fr.adrienbrault.idea.symfony2plugin.translation;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.ParameterListOwner;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationNavigationCompletionContributor {
    /**
     * Registered via {@link TranslationNavigationCompletionContributorCompletion}; Consulo cannot register nested classes via @ExtensionImpl.
     */
    public static class Completion extends CompletionContributor {
        public Completion() {
            // $this->translator->trans('<caret>', [], '<caret>');
            // new TranslatableMessage('<caret>', [], '<caret>');
            // t('<caret>', [], '<caret>');
            extend(CompletionType.BASIC, PlatformPatterns.or(
                PhpElementsUtil.getParameterInsideMethodReferencePattern(),
                PhpElementsUtil.getParameterInsideNewExpressionPattern(),
                PhpElementsUtil.getParameterInsideFunctionReferencePattern()
            ), new CompletionProvider() {
                @Override
                public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
                    PsiElement psiElement = parameters.getOriginalPosition();
                    if (psiElement == null || !(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                        return;
                    }

                    Project project = parameters.getPosition().getProject();

                    visit(
                        stringLiteralExpression,
                        s -> result.addAllElements(TranslationUtil.getTranslationDomainLookupElements(project)),
                        (domain, s) -> result.addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(project, domain))
                    );
                }
            });
        }

        @Override
        public Language getLanguage() {
            return PhpLanguage.INSTANCE;
        }
    }

    /**
     * Registered via {@link TranslationNavigationCompletionContributorGotoDeclaration}; Consulo cannot register nested classes via @ExtensionImpl.
     */
    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement sourceElement, int offset, Editor editor) {
            if (sourceElement != null && sourceElement.getParent() instanceof StringLiteralExpression stringLiteralExpression && (
                PhpElementsUtil.getParameterInsideMethodReferencePattern().accepts(sourceElement)
                    || PhpElementsUtil.getParameterInsideNewExpressionPattern().accepts(sourceElement)
                    || PhpElementsUtil.getParameterInsideFunctionReferencePattern().accepts(sourceElement)
            )) {
                Collection<PsiElement> psiElements = new ArrayList<>();

                Project project = sourceElement.getProject();

                visit(
                    stringLiteralExpression,
                    s -> psiElements.addAll(TranslationUtil.getDomainPsiFiles(project, s.getContents())),
                    (domain, s) -> psiElements.addAll(Arrays.asList(TranslationUtil.getTranslationPsiElements(project, s.getContents(), domain)))
                );

                return psiElements.toArray(new PsiElement[0]);
            }

            return null;
        }
    }

    public static void visit(StringLiteralExpression psiElement, TranslationNavigationCompletionContributor.Domain domain, TranslationNavigationCompletionContributor.Key key) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList parameterList)) {
            return;
        }

        if (!(parameterList.getContext() instanceof ParameterListOwner parameterListOwner)) {
            return;
        };

        if (!TranslationUtil.isTranslationReference(parameterListOwner)) {
            return;
        }

        int domainParameter = TranslationUtil.getDomainParameter(parameterListOwner);

        if (PsiElementUtils.isCurrentParameter(psiElement, "domain", domainParameter)) {
            domain.accept(psiElement);
            return;
        }

        if (PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
            PsiElement domainPsi = parameterList.getParameter("domain", domainParameter);

            String domainName = "messages";
            if (domainPsi != null) {
                String stringValue = PhpElementsUtil.getStringValue(domainPsi);
                if (stringValue != null) {
                    domainName = stringValue;
                }
            }

            key.accept(domainName, psiElement);
        }
    }

    private interface Domain {
        void accept(StringLiteralExpression stringLiteralExpression);
    }

    private interface Key {
        void accept(String domain, StringLiteralExpression stringLiteralExpression);
    }
}
