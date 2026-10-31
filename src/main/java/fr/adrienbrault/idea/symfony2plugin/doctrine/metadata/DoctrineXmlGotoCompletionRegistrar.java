package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DoctrineXmlGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(XmlPatterns.psiElement().withParent(PlatformPatterns.or(
            DoctrineMetadataPattern.getXmlModelClass(),
            DoctrineMetadataPattern.getXmlRepositoryClass(),
            DoctrineMetadataPattern.getXmlTargetDocumentClass(),
            DoctrineMetadataPattern.getXmlTargetEntityClass(),
            DoctrineMetadataPattern.getEmbeddableNameClassPattern(),
            DoctrineMetadataPattern.getEmbeddedClassPattern(),
            DoctrineMetadataPattern.getFieldEnumType()
        )), ClassGotoCompletionProvider::new);
    }

    private static class ClassGotoCompletionProvider extends GotoCompletionProvider {

        private ClassGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            PsiElement parent = element.getParent();
            if(!(parent instanceof XmlAttributeValue)) {
                return Collections.emptyList();
            }

            String value = ((XmlAttributeValue) parent).getValue();
            if(StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }

            return new ArrayList<>(DoctrineMetadataUtil.getClassInsideScope(element, value));
        }
    }
}
