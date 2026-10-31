package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

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
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.isInterpolatedString;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class TwigTranslationKeyInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Missing translation key";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyTranslationKeyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigTranslationKeyInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing translation key");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Translation");
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

    private static final class MyTranslationKeyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private @Nullable ElementPattern<PsiElement> myTranslationKeyPattern;

        private MyTranslationKeyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (!getTranslationKeyPattern().accepts(psiElement)) {
                super.visitElement(psiElement);
                return;
            }

            String text = psiElement.getText();
            if (StringUtils.isBlank(text) || isInterpolatedString(text)) {
                super.visitElement(psiElement);
                return;
            }

            // get domain on file scope or method parameter
            String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

            if (TranslationUtil.hasTranslationKey(psiElement.getProject(), text, domainName)) {
                super.visitElement(psiElement);
                return;
            }

            myHolder.registerProblem(
                psiElement,
                MESSAGE,
                new TranslationKeyIntentionAndQuickFixAction(text, domainName),
                new TranslationKeyGuessTypoQuickFix(text, domainName)
            );

            super.visitElement(psiElement);
        }

        private ElementPattern<PsiElement> getTranslationKeyPattern() {
            ElementPattern<PsiElement> pattern = myTranslationKeyPattern != null ? myTranslationKeyPattern : TwigPattern.getTranslationKeyPattern("trans", "transchoice");
            myTranslationKeyPattern = pattern;
            return pattern;
        }
    }
}
