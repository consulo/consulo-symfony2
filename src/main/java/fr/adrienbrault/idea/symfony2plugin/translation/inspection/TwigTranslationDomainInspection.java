package fr.adrienbrault.idea.symfony2plugin.translation.inspection;

import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public final class TwigTranslationDomainInspection extends LocalInspectionTool {
    public static final String MESSAGE = "Symfony: Missing translation domain";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyTranslationDomainPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TwigTranslationDomain";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Twig: Missing translation domain");
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

    /**
     * 'foo'|trans({}, 'foobar')
     * 'foo'|transchoice({}, null, 'foobar')
     */
    private static final class MyTranslationDomainPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private @Nullable ElementPattern<PsiElement> myTransDomainPattern;

        private MyTranslationDomainPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (!getTransDomainPattern().accepts(psiElement)) {
                return;
            }

            // @TODO: move to pattern, dont allow nested filters: eg "'form.tab.profile'|trans|desc('Interchange')"
            PsiElement[] psiElementTrans = new PsiElement[1];
            PsiElementUtils.getPrevSiblingOnCallback(psiElement, psiElement1 -> {
                if (psiElement1.getNode().getElementType() == TwigTokenTypes.FILTER) {
                    return false;
                }
                else {
                    if (PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")).accepts(psiElement1)) {
                        psiElementTrans[0] = psiElement1;
                    }

                    return true;
                }
            });

            PsiElement translationElement = psiElementTrans[0];
            if (translationElement != null && TwigUtil.getTwigMethodString(translationElement) != null) {
                String text = psiElement.getText();
                if (StringUtils.isNotBlank(text) && !TranslationUtil.hasDomain(psiElement.getProject(), text)) {
                    myHolder.registerProblem(
                        psiElement,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        new TranslationDomainGuessTypoQuickFix(text)
                    );
                }
            }

            super.visitElement(psiElement);
        }

        private ElementPattern<PsiElement> getTransDomainPattern() {
            ElementPattern<PsiElement> pattern = myTransDomainPattern != null ? myTransDomainPattern : TwigPattern.getTransDomainPattern();
            myTransDomainPattern = pattern;
            return pattern;
        }
    }
}
