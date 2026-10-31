package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
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
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionTargetVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE;

/**
 * Completion and navigation for options in Symfony form field attributes.
 *
 * <pre>
 * #[FormField(EmailType::class, options: ['required' => true])]
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class FormAttributeOptionGotoCompletionRegistrar implements GotoCompletionRegistrar {
    private static final String DEFAULT_FORM_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType";

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE).withParent(StringLiteralExpression.class),
            new FormFieldOptionCompletionContributor()
        );
    }

    private static class FormFieldOptionCompletionContributor implements GotoCompletionContributor {
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

            if (!FORM_FIELD_ATTRIBUTE.equals(attribute.getFQN())
                || PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 1, "options") != options) {
                return null;
            }

            PsiElement type = PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "type");
            String formType = type != null ? FormUtil.getFormTypeFqnOnParameter(type) : null;
            return new FormAttributeOptionCompletionProvider(psiElement, formType != null ? formType : DEFAULT_FORM_TYPE);
        }
    }

    private static class FormAttributeOptionCompletionProvider extends GotoCompletionProvider {
        private final String myFormType;

        FormAttributeOptionCompletionProvider(PsiElement element, String formType) {
            super(element);
            myFormType = formType;
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            List<LookupElement> elements = new ArrayList<>();
            FormOptionsUtil.visitFormOptions(getProject(), myFormType, new FormOptionLookupVisitor(elements));
            return elements;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            if (!(element.getParent() instanceof StringLiteralExpression literal) || StringUtils.isBlank(literal.getContents())) {
                return Collections.emptyList();
            }

            List<PsiElement> targets = new ArrayList<>();
            FormOptionsUtil.visitFormOptions(getProject(), myFormType, new FormOptionTargetVisitor(literal.getContents(), targets));
            return targets;
        }
    }
}
