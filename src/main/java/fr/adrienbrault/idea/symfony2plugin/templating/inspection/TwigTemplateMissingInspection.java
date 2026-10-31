package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * {% include 'f<caret>.html.twig' %}
 * {{ include('f<caret>.html.twig') }}
 * {% embed 'f<caret>.html.twig' %}
 * ... and so on
 *
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigTemplateMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigTemplateMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing Template");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Template");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return TwigLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (!(element instanceof LeafPsiElement) || element.getNode().getElementType() != TwigTokenTypes.STRING_TEXT) {
                super.visitElement(element);
                return;
            }

            if (TwigUtil.isStaticTemplateUsage(element)) {
                inspectTemplate(element, myHolder);
            }

            super.visitElement(element);
        }
    }

    // consulo-port: was a private top-level Kotlin function of TwigTemplateMissingInspection.kt
    private static void inspectTemplate(PsiElement element, ProblemsHolder holder) {
        String templateName = element.getText();
        if (templateName.isBlank()) {
            return;
        }

        Collection<VirtualFile> psiElements = TwigUtil.getTemplateFiles(element.getProject(), templateName);
        if (!psiElements.isEmpty()) {
            return;
        }

        holder.registerProblem(
            element,
            "Twig: Missing Template",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            new TemplateCreateByNameLocalQuickFix(templateName),
            new TemplateGuessTypoQuickFix(templateName)
        );
    }
}
