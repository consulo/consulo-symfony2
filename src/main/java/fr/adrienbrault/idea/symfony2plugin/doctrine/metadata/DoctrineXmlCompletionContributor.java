package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DoctrineXmlCompletionContributor extends CompletionContributor {

    public DoctrineXmlCompletionContributor() {

        // <entity name="Class\Name"/>
        // <document name="Class\Name"/>
        // <embeddable name="Class\Name"/>
        extend(CompletionType.BASIC, XmlPatterns.psiElement().withParent(PlatformPatterns.or(
            DoctrineMetadataPattern.getXmlModelClass(),
            DoctrineMetadataPattern.getXmlTargetEntityClass(),
            DoctrineMetadataPattern.getXmlTargetDocumentClass(),
            DoctrineMetadataPattern.getEmbeddableNameClassPattern(),
            DoctrineMetadataPattern.getEmbeddedClassPattern(),
            DoctrineMetadataPattern.getFieldEnumType()
        )), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters parameters, ProcessingContext processingContext, CompletionResultSet resultSet) {
                PsiElement psiElement = parameters.getOriginalPosition();
                if (psiElement == null) {
                    return;
                }

                Project project = psiElement.getProject();
                if (!Symfony2ProjectComponent.isEnabled(project)) {
                    return;
                }

                PhpClassCompletionProvider.addClassCompletion(parameters, resultSet, psiElement, false);
            }
        });

        // <entity repository-class="Class\Name"/>
        // <document repository-class="Class\Name"/>
        extend(CompletionType.BASIC, XmlPatterns.psiElement().withParent(PlatformPatterns.or(DoctrineMetadataPattern.getXmlRepositoryClass())),
            new CompletionProvider() {
                @Override
                public void addCompletions(CompletionParameters parameters, ProcessingContext processingContext, CompletionResultSet resultSet) {
                    Project project = parameters.getPosition().getProject();
                    if (!Symfony2ProjectComponent.isEnabled(project)) {
                        return;
                    }

                    // @TODO: filter on doctrine manager
                    resultSet.addAllElements(
                        DoctrineMetadataUtil.getObjectRepositoryLookupElements(project)
                    );
                }
            });
   }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
}
