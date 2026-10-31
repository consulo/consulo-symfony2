package fr.adrienbrault.idea.symfony2plugin.translation;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.ParameterListOwner;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationKeyGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class PhpTranslationKeyInspection extends LocalInspectionTool {
    public static final String MESSAGE = TwigTranslationKeyInspection.MESSAGE;

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    inspectTranslationKey(holder, stringLiteralExpression);
                }
                super.visitElement(element);
            }
        };
    }

    private void inspectTranslationKey(ProblemsHolder holder, StringLiteralExpression psiElement) {
        ParameterListOwner methodReferenceOrNewExpression = TranslationUtil.getTranslationFunctionContext(psiElement);
        if (methodReferenceOrNewExpression == null) {
            return;
        }

        if (!PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
            return;
        }

        if (!TranslationUtil.isTranslationReference(methodReferenceOrNewExpression)) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        PsiElement domainElement = parameterList.getParameter("domain", TranslationUtil.getDomainParameter(methodReferenceOrNewExpression));
        if (domainElement == null) {
            // no domain found; fallback to default domain
            annotateTranslationKey(psiElement, "messages", holder);
        }
        else {
            // resolve string in parameter
            String domain = PhpElementsUtil.getStringValue(domainElement);
            if (domain != null) {
                annotateTranslationKey(psiElement, domain, holder);
            }
        }
    }

    private void annotateTranslationKey(StringLiteralExpression psiElement, String domainName, ProblemsHolder holder) {
        String keyName = psiElement.getContents();

        // should not annotate "foo$bar"
        // @TODO: regular expression to only notice translation keys and not possible text values
        if (StringUtils.isBlank(keyName) || keyName.contains("$")) {
            return;
        }

        // dont annotate non goto available keys
        if (TranslationUtil.hasTranslationKey(psiElement.getProject(), keyName, domainName)) {
            return;
        }

        holder.registerProblem(
            psiElement,
            MESSAGE,
            new TranslationKeyIntentionAndQuickFixAction(keyName, domainName),
            new TranslationKeyGuessTypoQuickFix(keyName, domainName)
        );
    }

    @Override
    public String getShortName() {
        return "PhpTranslationKeyInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("PHP: Missing translation key");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Translation");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }
}
