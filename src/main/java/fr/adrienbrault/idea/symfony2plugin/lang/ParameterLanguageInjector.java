package fr.adrienbrault.idea.symfony2plugin.lang;

import com.jetbrains.php.lang.psi.elements.AssignmentExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class ParameterLanguageInjector implements MultiHostInjector {

    private static final MethodMatcher.CallToSignature[] CSS_SELECTOR_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filter"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "children"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\CssSelector\\CssSelectorConverter", "toXPath"),
    };

    private static final MethodMatcher.CallToSignature[] XPATH_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filterXPath"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "evaluate"),
    };

    private static final MethodMatcher.CallToSignature[] JSON_SIGNATURES = {
            //new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "__construct"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "fromJsonString"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "setJson"),
    };

    private static final MethodMatcher.CallToSignature[] DQL_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\EntityManager", "createQuery"),
            new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query", "setDQL"),
    };

    private final MethodLanguageInjection[] LANGUAGE_INJECTIONS = {
        new MethodLanguageInjection(LANGUAGE_ID_CSS, "@media all { ", " {} }", CSS_SELECTOR_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_XPATH, null, null, XPATH_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_JSON, null, null, JSON_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_DQL, null, null, DQL_SIGNATURES),
    };

    public static final String LANGUAGE_ID_CSS = "CSS";
    public static final String LANGUAGE_ID_XPATH = "XPath";
    public static final String LANGUAGE_ID_JSON = "JSON";
    public static final String LANGUAGE_ID_DQL = "DQL";

    private static final String DQL_VARIABLE_NAME = "dql";

    public ParameterLanguageInjector() {
    }

    @Override
    public Class<? extends PsiElement> getElementClass() {
        // consulo-port: consulo-php has no StringLiteralExpressionImpl, the injector map also matches interfaces
        return StringLiteralExpression.class;
    }

    @Override
    public void injectLanguages(MultiHostRegistrar registrar, PsiElement element) {
        if (!(element instanceof StringLiteralExpression) || !((PsiLanguageInjectionHost) element).isValidHost()) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        final StringLiteralExpression expr = (StringLiteralExpression) element;

        PsiElement parent = expr.getParent();

        final boolean isParameter = parent instanceof ParameterList && expr.getPrevPsiSibling() == null; // 1st parameter
        final boolean isAssignment = parent instanceof AssignmentExpression;

        if (!isParameter && !isAssignment) {
            return;
        }

        if (isParameter)  {
            parent = parent.getParent();
        }

        for (MethodLanguageInjection languageInjection : LANGUAGE_INJECTIONS) {
            // $crawler->filter('...')
            // $em->createQuery('...')
            // JsonResponse::fromJsonString('...')
            if (parent instanceof MethodReference) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) parent, languageInjection.getSignatures())) {
                    injectLanguage(registrar, expr, languageInjection);
                    return;
                }
            }
            // $dql = "...";
            else if (parent instanceof AssignmentExpression) {
                Language language = languageInjection.getLanguage();
                if (language != null && LANGUAGE_ID_DQL.equals(language.getID())) {
                    PhpPsiElement variable = ((AssignmentExpression) parent).getVariable();
                    if (variable instanceof Variable) {
                        if (DQL_VARIABLE_NAME.equals(variable.getName())) {
                            injectLanguage(registrar, expr, languageInjection);
                            return;
                        }
                    }
                }
            }
        }

    }

    private void injectLanguage(MultiHostRegistrar registrar, StringLiteralExpression element, MethodLanguageInjection languageInjection) {
        Language language = languageInjection.getLanguage();
        if (language == null) {
            return;
        }

        registrar.startInjecting(language)
            .addPlace(languageInjection.getPrefix(), languageInjection.getSuffix(), element, element.getValueRange())
            .doneInjecting();
    }

    private static class MethodLanguageInjection {
        private final String language;
        private final String prefix;
        private final String suffix;
        private final MethodMatcher.CallToSignature[] signatures;

        MethodLanguageInjection(String languageId, String prefix, String suffix, MethodMatcher.CallToSignature[] signatures) {
            this.language = languageId;
            this.prefix = prefix;
            this.suffix = suffix;
            this.signatures = signatures;
        }

        public @Nullable Language getLanguage() {
            return Language.findLanguageByID(this.language);
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public MethodMatcher.CallToSignature[] getSignatures() {
            return signatures;
        }
    }
}
