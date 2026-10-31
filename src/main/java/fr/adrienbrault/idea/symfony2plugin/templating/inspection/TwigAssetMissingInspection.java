package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.LeafPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jspecify.annotations.Nullable;

/**
 * asset('<caret>')
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigAssetMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigAssetMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing Asset");
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
        return TwigLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private @Nullable ElementPattern<PsiElement> myAssetPattern;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        private ElementPattern<PsiElement> getAssetPattern() {
            if (myAssetPattern == null) {
                myAssetPattern = TwigPattern.getAutocompletableAssetPattern();
            }
            return myAssetPattern;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (!(element instanceof LeafPsiElement) || element.getNode().getElementType() != TwigTokenTypes.STRING_TEXT) {
                super.visitElement(element);
                return;
            }

            if (getAssetPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                inspectAsset(element);
            }

            super.visitElement(element);
        }

        private void inspectAsset(PsiElement element) {
            String asset = element.getText();

            if (asset.isBlank() || !TwigUtil.resolveAssetsFiles(element.getProject(), asset).isEmpty()) {
                return;
            }

            myHolder.registerProblem(element, "Missing asset");
        }
    }
}
