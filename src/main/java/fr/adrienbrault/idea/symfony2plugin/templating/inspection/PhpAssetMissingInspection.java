package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.MethodReference;
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
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

/**
 * PHP version of "Twig" "asset" function
 *
 * "Package*::getUrl"
 * "Package*::getVersion"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpAssetMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new AssetPackageElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "PhpAssetMissingInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Missing Asset");
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

    private static class AssetPackageElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private AssetPackageElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof StringLiteralExpression stringLiteralExpression) {
                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(stringLiteralExpression);
                String methodName = methodReference != null ? methodReference.getName() : null;
                if (("getUrl".equals(methodName) || "getVersion".equals(methodName)) &&
                    (PhpElementsUtil.isMethodReferenceInstanceOf(
                        methodReference,
                        "\\Symfony\\Component\\Asset\\Packages",
                        methodName
                    ) ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(
                            methodReference,
                            "\\Symfony\\Component\\Asset\\PackageInterface",
                            methodName
                        ))
                ) {
                    inspectAsset(stringLiteralExpression);
                }
            }
            super.visitElement(element);
        }

        private void inspectAsset(StringLiteralExpression element) {
            String asset = element.getContents();
            if (asset.isBlank() || !TwigUtil.resolveAssetsFiles(element.getProject(), asset).isEmpty()) {
                return;
            }

            myHolder.registerProblem(element, "Symfony: Missing asset");
        }
    }
}
