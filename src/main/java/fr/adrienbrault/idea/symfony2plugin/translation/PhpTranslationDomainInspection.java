package fr.adrienbrault.idea.symfony2plugin.translation;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.ParameterListOwner;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationDomainGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationDomainInspection;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class PhpTranslationDomainInspection extends LocalInspectionTool {
    public static final String MESSAGE = TwigTranslationDomainInspection.MESSAGE;

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    inspectTranslationDomain(holder, stringLiteralExpression);
                }
                super.visitElement(element);
            }
        };
    }

    private void inspectTranslationDomain(ProblemsHolder holder, StringLiteralExpression psiElement) {
        ParameterListOwner methodReferenceOrNewExpression = TranslationUtil.getTranslationFunctionContext(psiElement);
        if (methodReferenceOrNewExpression == null) {
            return;
        }

        ASTNode previousNonWhitespaceSibling = FormatterUtil.getPreviousNonWhitespaceSibling(psiElement.getNode());

        if (previousNonWhitespaceSibling != null && previousNonWhitespaceSibling.getElementType() == PhpTokenTypes.opCOLON) {
            ASTNode previousNonWhitespaceSibling1 = FormatterUtil.getPreviousNonWhitespaceSibling(previousNonWhitespaceSibling);
            if (previousNonWhitespaceSibling1 != null && previousNonWhitespaceSibling1.getElementType() == PhpTokenTypes.IDENTIFIER) {
                String text = previousNonWhitespaceSibling1.getText();
                boolean isSupportedAttributeInsideContext = "domain".equals(text) &&
                    TranslationUtil.isTranslationReference(methodReferenceOrNewExpression);

                if (isSupportedAttributeInsideContext) {
                    annotateTranslationDomain(psiElement, holder);
                }
            }

            return;
        }

        int domainParameter = TranslationUtil.getDomainParameter(methodReferenceOrNewExpression);

        if (domainParameter >= 0) {
            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
            if (currentIndex != null && currentIndex.getIndex() == domainParameter) {
                annotateTranslationDomain(psiElement, holder);
            }
        }
    }

    private void annotateTranslationDomain(StringLiteralExpression psiElement, ProblemsHolder holder) {
        String contents = psiElement.getContents();
        if (StringUtils.isBlank(contents) || TranslationUtil.hasDomain(psiElement.getProject(), contents)) {
            return;
        }

        holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new TranslationDomainGuessTypoQuickFix(contents));
    }

    @Override
    public String getShortName() {
        return "PhpTranslationDomainInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("PHP: Missing translation domain");
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
