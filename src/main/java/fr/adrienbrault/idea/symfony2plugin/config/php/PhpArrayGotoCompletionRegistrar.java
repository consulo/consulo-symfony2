package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.completion.DecoratedServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterLookupPercentElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Registers completion/navigation for PHP array service config values.
 */
@ExtensionImpl
public class PhpArrayGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            psiElement -> {
                if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                    return null;
                }

                PhpArrayServiceUtil.ServiceConfigPath keyPath = PhpArrayServiceUtil.getKeyPath(stringLiteralExpression);
                if (keyPath == null || !keyPath.isDecoratesOrParent()) {
                    return null;
                }

                return new DecoratesParentContributor(stringLiteralExpression);
            }
        );

        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE),
            psiElement -> {
                if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                    return null;
                }

                PhpArrayServiceUtil.ServiceConfigPath keyPath = PhpArrayServiceUtil.getKeyPath(stringLiteralExpression);
                if (keyPath == null || !keyPath.isArgument()) {
                    return null;
                }

                String contents = stringLiteralExpression.getContents();
                if (!contents.isBlank() && !contents.startsWith("%")) {
                    return null;
                }

                return new ParameterArgumentContributor(stringLiteralExpression);
            }
        );
    }

    private static class DecoratesParentContributor extends DecoratedServiceCompletionProvider {
        private DecoratesParentContributor(StringLiteralExpression element) {
            super(element);
        }

        @Nullable
        @Override
        public String findClassForElement(PsiElement psiElement) {
            return PhpArrayServiceUtil.getCurrentServiceClass(psiElement);
        }

        @Nullable
        @Override
        public String findIdForElement(PsiElement psiElement) {
            return PhpArrayServiceUtil.getServiceId(psiElement);
        }
    }

    private static class ParameterArgumentContributor extends GotoCompletionProvider {
        private ParameterArgumentContributor(StringLiteralExpression element) {
            super(element);
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            List<LookupElement> results = new ArrayList<>();

            for (ContainerParameter containerParameter : ContainerCollectionResolver.getParameters(getProject()).values()) {
                results.add(new ParameterLookupPercentElement(containerParameter));
            }

            return results;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String parameterName = GotoCompletionUtil.getStringLiteralValue(element);
            if (parameterName == null || !YamlHelper.isValidParameterName(parameterName)) {
                return Collections.emptyList();
            }

            return ServiceUtil.getServiceClassTargets(element.getProject(), parameterName);
        }
    }
}
