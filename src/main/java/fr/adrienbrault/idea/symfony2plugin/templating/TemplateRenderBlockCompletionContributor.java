package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * Provides completion and navigation for block names in AbstractController::renderBlock() and renderBlockView() calls.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateRenderBlockCompletionContributor {

    /**
     * $this->renderBlock('foo.html.twig', 'the_block', $context);
     */
    public static final MethodMatcher.CallToSignature[] CALL_TO_SIGNATURES = {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "renderBlock"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "renderBlockView")
    };

    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (psiElement == null || !(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression) || !PhpElementsUtil.getParameterInsideMethodReferencePattern().accepts(psiElement)) {
                return null;
            }

            String contents = stringLiteralExpression.getContents();
            if (StringUtils.isBlank(contents)) {
                return null;
            }

            int currentIndex = PsiElementUtils.getParameterIndexValue(stringLiteralExpression);
            if (currentIndex != 1) {
                return null;
            }

            MethodReference parentOfType = PsiTreeUtil.getParentOfType(stringLiteralExpression, MethodReference.class);
            if (!PhpElementsUtil.isMethodReferenceInstanceOf(parentOfType, CALL_TO_SIGNATURES)) {
                return null;
            }

            String templateName = PhpElementsUtil.getMethodReferenceStringValueParameter(parentOfType, 0);
            if (StringUtils.isBlank(templateName)) {
                return null;
            }

            Set<PsiElement> targets = new HashSet<>();

            Project project = psiElement.getProject();

            for (VirtualFile templateFile : TwigUtil.getTemplateFiles(project, templateName)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(templateFile);
                if (psiFile == null) {
                    continue;
                }

                targets.addAll(TwigBlockUtil.getBlockOverwriteTargets(psiFile, contents, true));
            }

            if (targets.size() <= 1) {
                return targets.toArray(new PsiElement[0]);
            }

            VirtualFile projectDir = ProjectUtil.getProjectDir(psiElement);
            return targets.stream()
                .map(element -> new TwigBlockFakePsiNavigationItem(projectDir, element))
                .toArray(PsiElement[]::new);
        }
    }
}
