package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionTargetVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE;

/**
 * Completion and navigation for options in Symfony form type attributes.
 *
 * <pre>
 * #[AsFormType(options: ['label' => 'Account'])]
 * class AccountData {}
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class AsFormTypeAttributeOptionGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE).withParent(StringLiteralExpression.class),
            new AsFormTypeOptionCompletionContributor()
        );
    }

    private static class AsFormTypeOptionCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(PsiElement psiElement) {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            ArrayCreationExpression options = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.getParent());
            if (options == null
                || !(options.getParent() instanceof ParameterList parameterList)
                || !(parameterList.getParent() instanceof PhpAttribute attribute)) {
                return null;
            }

            if (!AS_FORM_TYPE_ATTRIBUTE.equals(attribute.getFQN())
                || PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "options") != options) {
                return null;
            }

            if (!(attribute.getParent().getParent() instanceof PhpClass phpClass)) {
                return null;
            }

            return new AsFormTypeOptionCompletionProvider(psiElement, phpClass);
        }
    }

    private static class AsFormTypeOptionCompletionProvider extends GotoCompletionProvider {
        private final PhpClass myPhpClass;

        AsFormTypeOptionCompletionProvider(PsiElement element, PhpClass phpClass) {
            super(element);
            myPhpClass = phpClass;
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            List<LookupElement> elements = new ArrayList<>();
            visitOptions(new FormOptionLookupVisitor(elements));

            Map<String, LookupElement> distinct = new LinkedHashMap<>();
            for (LookupElement element : elements) {
                distinct.putIfAbsent(element.getLookupString(), element);
            }
            return new ArrayList<>(distinct.values());
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            if (!(element.getParent() instanceof StringLiteralExpression literal) || StringUtils.isBlank(literal.getContents())) {
                return Collections.emptyList();
            }

            Set<PsiElement> targets = new LinkedHashSet<>();
            visitOptions(new FormOptionTargetVisitor(literal.getContents(), targets));
            return targets;
        }

        private void visitOptions(FormOptionVisitor visitor) {
            FormOptionVisitor allowedOptions = (target, option, formClass, kind) -> {
                if (!"data_class".equals(option)) {
                    visitor.visit(target, option, formClass, kind);
                }
            };

            FormOptionsUtil.visitFormOptions(getProject(), myPhpClass.getFQN(), allowedOptions);
        }
    }
}
