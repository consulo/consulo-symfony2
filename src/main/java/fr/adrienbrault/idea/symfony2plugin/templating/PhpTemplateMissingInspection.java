package fr.adrienbrault.idea.symfony2plugin.templating;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateCreateByNameLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateGuessTypoQuickFix;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpTemplateMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof StringLiteralExpression stringLiteralExpression) {
                    inspectTemplate(holder, stringLiteralExpression);
                }
                super.visitElement(element);
            }
        };
    }

    private void inspectTemplate(ProblemsHolder holder, StringLiteralExpression psiElement) {
        String templateName = getTemplateNameIfMissing(psiElement);
        if (templateName == null) {
            return;
        }

        holder.registerProblem(
            psiElement,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            new TemplateCreateByNameLocalQuickFix(templateName),
            new TemplateGuessTypoQuickFix(templateName)
        );
    }

    private @Nullable String getTemplateNameIfMissing(StringLiteralExpression psiElement) {
        ParameterBag currentParameterIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if (currentParameterIndex == null || currentParameterIndex.getIndex() != 0) {
            return null;
        }

        if (!(psiElement.getParent() instanceof ParameterList parameterList)) {
            return null;
        }

        if (!(parameterList.getParent() instanceof MethodReference methodReference)) {
            return null;
        }

        if (!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES)) {
            return null;
        }

        String templateName = PhpElementsUtil.getFirstArgumentStringValue(methodReference);
        if (templateName == null || templateName.isBlank()) {
            return null;
        }

        if (!TwigUtil.getTemplateFiles(psiElement.getProject(), templateName).isEmpty()) {
            return null;
        }

        return templateName;
    }

    @Override
    public String getShortName() {
        return "PhpTemplateMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Template Missing");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Twig");
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
