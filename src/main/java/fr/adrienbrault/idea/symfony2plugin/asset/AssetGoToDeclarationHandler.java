package fr.adrienbrault.idea.symfony2plugin.asset;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class AssetGoToDeclarationHandler implements GotoDeclarationHandler {

    @Override
    public PsiElement @Nullable [] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        String[] fileExtensionFilterIfValidTag = findValidAssetFilter(psiElement);
        if (fileExtensionFilterIfValidTag == null) {
            return null;
        }

        Collection<PsiElement> psiElements = new HashSet<>();
        for (VirtualFile virtualFile : TwigUtil.resolveAssetsFiles(psiElement.getProject(), psiElement.getText(), fileExtensionFilterIfValidTag)) {
            PsiElement target;
            if(virtualFile.isDirectory()) {
                target = PsiManager.getInstance(psiElement.getProject()).findDirectory(virtualFile);
            } else {
                target = PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile);
            }

            if(target != null) {
                psiElements.add(target);
            }
        }

        return psiElements.toArray(new PsiElement[0]);
    }

    private String[] findValidAssetFilter(PsiElement psiElement) {

        // tag filter
        String[] fileExtensionFilterIfValidTag = getFileExtensionFilterIfValidTag(psiElement);
        if (fileExtensionFilterIfValidTag != null) {
            return fileExtensionFilterIfValidTag;
        }

        // asset / absolute_url dont have pre filter
        if(TwigPattern.getPrintBlockOrTagFunctionPattern("asset", "absolute_url").accepts(psiElement)) {
            return ArrayUtils.addAll(TwigUtil.CSS_FILES_EXTENSIONS, TwigUtil.JS_FILES_EXTENSIONS);
        }

        return null;
    }

    private String @Nullable [] getFileExtensionFilterIfValidTag(PsiElement psiElement) {
        for (String tag: new String[] {"stylesheets", "javascripts"}) {
            if (!TwigPattern.getAutocompletableAssetTag(tag).accepts(psiElement)) {
                continue;
            }

            return switch (tag) {
                case "stylesheets" -> TwigUtil.CSS_FILES_EXTENSIONS;
                case "javascripts" -> TwigUtil.JS_FILES_EXTENSIONS;
                default -> null;
            };
        }

        return null;
    }

    @Override
    public LocalizeValue getActionText(DataContext dataContext) {
        return LocalizeValue.empty();
    }
}
