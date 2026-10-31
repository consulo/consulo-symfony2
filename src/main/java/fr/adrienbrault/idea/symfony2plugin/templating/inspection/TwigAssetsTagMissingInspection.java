package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.jetbrains.twig.TwigLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * {% javascripts
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * {% stylesheets
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigAssetsTagMissingInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigAssetsTagMissingInspection";
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

        private @Nullable ElementPattern<PsiElement> myStylesheetsPattern;
        private @Nullable ElementPattern<PsiElement> myJavaScriptsPattern;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        private ElementPattern<PsiElement> getStylesheetsPattern() {
            if (myStylesheetsPattern == null) {
                myStylesheetsPattern = TwigPattern.getAutocompletableAssetTag("stylesheets");
            }
            return myStylesheetsPattern;
        }

        private ElementPattern<PsiElement> getJavaScriptsPattern() {
            if (myJavaScriptsPattern == null) {
                myJavaScriptsPattern = TwigPattern.getAutocompletableAssetTag("javascripts");
            }
            return myJavaScriptsPattern;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (getStylesheetsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                String templateName = element.getText();
                if (!isKnownAssetFileOrFolder(element, templateName, TwigUtil.CSS_FILES_EXTENSIONS)) {
                    myHolder.registerProblem(element, "Missing asset");
                }
            }
            else if (getJavaScriptsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                String templateName = element.getText();
                if (!isKnownAssetFileOrFolder(element, templateName, TwigUtil.JS_FILES_EXTENSIONS)) {
                    myHolder.registerProblem(element, "Missing asset");
                }
            }

            super.visitElement(element);
        }
    }

    // consulo-port: was a private top-level Kotlin function of TwigAssetsTagMissingInspection.kt
    private static boolean isKnownAssetFileOrFolder(PsiElement element, String templateName, String... fileTypes) {
        // custom assets
        if (templateName.startsWith("@") && templateName.length() > 1) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(element.getProject(), TwigNamedAssetsServiceParser.class);
            Set<String> strings = twigPathServiceParser.getNamedAssets().keySet();
            if (strings.contains(templateName.substring(1))) {
                return true;
            }
        }

        return !TwigUtil.resolveAssetsFiles(element.getProject(), templateName, fileTypes).isEmpty();
    }
}
