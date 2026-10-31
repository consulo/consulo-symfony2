package fr.adrienbrault.idea.symfony2plugin.navigation;

import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.elements.TwigBlockStatement;
import com.jetbrains.twig.elements.TwigBlockTag;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import com.jetbrains.twig.icons.TwigIcons;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigGotoRelatedProvider extends GotoRelatedProvider {
    @Override
    public List<? extends GotoRelatedItem> getItems(PsiElement psiElement2) {
        Language language = psiElement2.getLanguage();
        if (language != TwigLanguage.INSTANCE && language != HTMLLanguage.INSTANCE) {
            return Collections.emptyList();
        }

        Project project = psiElement2.getProject();

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return Collections.emptyList();
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();

        PsiElement psiElement = TwigUtil.getElementOnTwigViewProvider(psiElement2);
        if (psiElement == null) {
            return Collections.emptyList();
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof TwigFile) {
            // extends
            VirtualFile twigVirtualFile = psiFile.getVirtualFile();

            Set<String> templates = twigVirtualFile != null
                ? new HashSet<>(FileBasedIndex.getInstance().getFileData(TwigExtendsStubIndex.KEY, twigVirtualFile, project).keySet())
                : new HashSet<>();

            Set<VirtualFile> virtualFiles = new HashSet<>();
            for (String template : templates) {
                for (PsiFile templatePsiElement : TwigUtil.getTemplatePsiElements(project, template)) {
                    VirtualFile virtualFile = templatePsiElement.getVirtualFile();
                    if (!virtualFiles.contains(virtualFile)) {
                        virtualFiles.add(virtualFile);
                        gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templatePsiElement, "extends").withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    }
                }
            }

            // twig blocks up
            @Nullable TwigBlockStatement parentOfType = PsiTreeUtil.getParentOfType(psiElement, TwigBlockStatement.class);
            if (parentOfType != null) {
                TwigBlockTag twigBlockTag = PsiTreeUtil.findChildOfType(parentOfType, TwigBlockTag.class);
                if (twigBlockTag != null) {
                    PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(twigBlockTag, TwigPattern.getBlockTagPattern());
                    if (childrenOfType != null) {
                        String blockName = twigBlockTag.getName();

                        gotoRelatedItems.addAll(TwigBlockUtil.getBlockOverwriteTargets(childrenOfType).stream().map((Function<PsiElement, GotoRelatedItem>) psiElement1 ->
                            new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement1, blockName).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE)
                        ).toList());

                        gotoRelatedItems.addAll(TwigBlockUtil.getBlockImplementationTargets(childrenOfType).stream().map((Function<PsiElement, GotoRelatedItem>) psiElement1 ->
                            new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement1, blockName).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE)
                        ).toList());
                    }
                }
            }

            // "include" and other file tags
            TwigUtil.TemplateIncludePatterns patterns = new TwigUtil.TemplateIncludePatterns();

            TwigTagWithFileReference twigTagWithFileReference = PsiTreeUtil.getParentOfType(psiElement, TwigTagWithFileReference.class);
            if (twigTagWithFileReference != null) {
                visitFileReferenceElement(project, gotoRelatedItems, twigTagWithFileReference, patterns);
            }

            // "embed" tag
            PsiElement parentOfType1 = PsiElementUtils.getParentOfType(psiElement, TwigElementTypes.EMBED_STATEMENT);
            if (parentOfType1 != null) {
                PsiElement embedTag = PsiElementUtils.getChildrenOfType(parentOfType1, PlatformPatterns.psiElement().withElementType(TwigElementTypes.EMBED_TAG));
                if (embedTag != null) {
                    visitFileReferenceElement(project, gotoRelatedItems, embedTag, patterns);
                }
            }
        }

        return gotoRelatedItems;
    }

    private void visitFileReferenceElement(Project project, List<GotoRelatedItem> gotoRelatedItems, PsiElement psiElement, TwigUtil.TemplateIncludePatterns patterns) {
        TwigUtil.visitTemplateIncludes(psiElement, templateInclude -> {
            for (PsiFile templatePsiElement : TwigUtil.getTemplatePsiElements(project, templateInclude.getTemplateName())) {
                gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templatePsiElement, templateInclude.getType().toString().toLowerCase()).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE));
            }
        }, patterns);
    }
}
