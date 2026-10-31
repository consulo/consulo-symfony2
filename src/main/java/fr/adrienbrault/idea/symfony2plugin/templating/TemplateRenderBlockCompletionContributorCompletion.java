package fr.adrienbrault.idea.symfony2plugin.templating;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import com.jetbrains.twig.icons.TwigIcons;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;

import static fr.adrienbrault.idea.symfony2plugin.templating.TemplateRenderBlockCompletionContributor.CALL_TO_SIGNATURES;

/**
 * Completion part of {@link TemplateRenderBlockCompletionContributor}: block names in
 * AbstractController::renderBlock() and renderBlockView() calls.
 *
 * consulo-port: was the nested class TemplateRenderBlockCompletionContributor$Completion; a nested class cannot carry @ExtensionImpl
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TemplateRenderBlockCompletionContributorCompletion extends CompletionContributor {

    public TemplateRenderBlockCompletionContributorCompletion() {
        extend(CompletionType.BASIC, PhpElementsUtil.getParameterInsideMethodReferencePattern(), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet result) {
                PsiElement psiElement = parameters.getOriginalPosition();
                if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                    return;
                }

                int currentIndex = PsiElementUtils.getParameterIndexValue(stringLiteralExpression);
                if (currentIndex != 1) {
                    return;
                }

                MethodReference parentOfType = PsiTreeUtil.getParentOfType(stringLiteralExpression, MethodReference.class);
                if (!PhpElementsUtil.isMethodReferenceInstanceOf(parentOfType, CALL_TO_SIGNATURES)) {
                    return;
                }

                String templateName = PhpElementsUtil.getMethodReferenceStringValueParameter(parentOfType, 0);
                if (StringUtils.isBlank(templateName)) {
                    return;
                }

                Project project = parentOfType.getProject();

                Set<VirtualFile> parentFiles = new HashSet<>();
                for (VirtualFile templateFile : TwigUtil.getTemplateFiles(project, templateName)) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(templateFile);
                    if (psiFile != null) {
                        parentFiles.addAll(TwigFileUtil.collectParentFiles(true, psiFile));
                    }
                }

                Set<String> blockNames = new HashSet<>();
                TwigUtil.getBlockNamesForFiles(project, parentFiles).values()
                    .forEach(blockNames::addAll);

                for (String blockName : blockNames) {
                    result.addElement(LookupElementBuilder.create(blockName).withIcon(TwigIcons.TwigFileIcon));
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
