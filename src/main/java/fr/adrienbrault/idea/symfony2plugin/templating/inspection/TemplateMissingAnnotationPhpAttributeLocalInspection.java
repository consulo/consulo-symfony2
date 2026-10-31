package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpAttributesList;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TemplateMissingAnnotationPhpAttributeLocalInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof PhpDocTag phpDocTag) {
                    annotate(phpDocTag, holder);
                }
                else if (element instanceof PhpAttribute phpAttribute) {
                    String fqn = phpAttribute.getFQN();
                    if (fqn != null && PhpElementsUtil.isEqualClassName(fqn, TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
                        annotate(phpAttribute, holder);
                    }
                }

                super.visitElement(element);
            }
        };
    }

    @Override
    public String getShortName() {
        return "TemplateMissingAnnotationPhpAttributeLocalInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing Template");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Asset");
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

    private void annotate(PhpAttribute phpAttribute, ProblemsHolder holder) {
        Set<String> templateNames = new LinkedHashSet<>();
        boolean isEmptyTemplateAndGuess = phpAttribute.getArguments().isEmpty();
        if (isEmptyTemplateAndGuess) {
            Method method = phpAttribute.getParent() instanceof PhpAttributesList attributesList
                && attributesList.getParent() instanceof Method parentMethod ? parentMethod : null;
            if (method != null) {
                Collections.addAll(templateNames, TwigUtil.getControllerMethodShortcut(method));
            }
        }
        else {
            String templateName = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(phpAttribute, "template");
            if (templateName == null) {
                return;
            }
            templateNames.add(templateName);
        }

        attachProblemForMissingTemplatesWithSuggestions(phpAttribute, holder, templateNames, isEmptyTemplateAndGuess);
    }

    private void annotate(PhpDocTag phpDocTag, ProblemsHolder holder) {
        PhpDocTagAnnotation phpDocAnnotationContainer = AnnotationUtil.getPhpDocAnnotationContainer(phpDocTag);
        if (phpDocAnnotationContainer == null || !PhpElementsUtil.isEqualClassName(phpDocAnnotationContainer.getPhpClass(), TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
            return;
        }

        if (phpDocTag.getFirstPsiChild() == null) {
            return;
        }

        Set<String> templateNames = new LinkedHashSet<>();
        boolean isEmptyTemplateAndGuess = false;

        String matcher = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
        if (matcher != null) {
            templateNames.add(matcher);
        }
        else {
            isEmptyTemplateAndGuess = true;

            // find template name on last method
            PhpDocComment docComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
            if (docComment == null) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if (method == null) {
                return;
            }

            Collections.addAll(templateNames, TwigUtil.getControllerMethodShortcut(method));
        }

        attachProblemForMissingTemplatesWithSuggestions(phpDocTag, holder, templateNames, isEmptyTemplateAndGuess);
    }

    private void attachProblemForMissingTemplatesWithSuggestions(
        PsiElement target,
        ProblemsHolder holder,
        Set<String> templateNames,
        boolean isEmptyTemplateAndGuess
    ) {
        if (templateNames.isEmpty()) {
            return;
        }

        for (String templateName : templateNames) {
            if (!TwigUtil.getTemplateFiles(holder.getProject(), templateName).isEmpty()) {
                return;
            }
        }

        // find html target, as this this our first priority for end users condition
        // or fallback on first item
        String[] templates = templateNames.stream()
            .filter(it -> it.regionMatches(true, it.length() - ".html.twig".length(), ".html.twig", 0, ".html.twig".length()))
            .toArray(String[]::new);

        List<LocalQuickFix> quickFixes = new ArrayList<>();
        quickFixes.add(new TemplateCreateByNameLocalQuickFix(templates));

        if (!isEmptyTemplateAndGuess && templates.length > 0) {
            // use first as underscore is higher priority and common way by framework bundle
            quickFixes.add(new TemplateGuessTypoQuickFix(templates[0]));
        }

        holder.registerProblem(target, "Twig: Missing Template", quickFixes.toArray(LocalQuickFix[]::new));
    }
}
