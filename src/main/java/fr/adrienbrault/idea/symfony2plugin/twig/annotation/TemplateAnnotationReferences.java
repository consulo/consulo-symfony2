package fr.adrienbrault.idea.symfony2plugin.twig.annotation;

import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TemplateAnnotationReferences implements PhpAnnotationReferenceProvider {
    @Override
    public PsiReference @Nullable [] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {

        if(!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !(annotationPropertyParameter.getElement() instanceof StringLiteralExpression) || !PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
            return new PsiReference[0];
        }

        if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT) {
            // @Foo("template.html.twig")
            return new PsiReference[]{ new TemplateReference((StringLiteralExpression) annotationPropertyParameter.getElement()) };
        } else if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.PROPERTY_VALUE || "template".equals(annotationPropertyParameter.getPropertyName())) {
            // @Foo(template="template.html.twig") — skip PHP 8 attributes; handled by SymfonyPhpReferenceContributor generic named-arg provider
            if (PsiTreeUtil.getParentOfType(annotationPropertyParameter.getElement(), PhpAttribute.class) != null) {
                return new PsiReference[0];
            }
            return new PsiReference[]{ new TemplateReference((StringLiteralExpression) annotationPropertyParameter.getElement()) };
        }

        return new PsiReference[0];
    }

}
