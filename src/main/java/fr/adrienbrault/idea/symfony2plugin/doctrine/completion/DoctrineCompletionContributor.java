package fr.adrienbrault.idea.symfony2plugin.doctrine.completion;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.ClassConstantLookupElementAbstract;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "before PhpCompletionContributor") // upstream order="first"
public class DoctrineCompletionContributor extends CompletionContributor {

    public DoctrineCompletionContributor() {

        // getRepository(FOO) -> getRepository(FOO::class)
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(ConstantReference.class), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                MethodReference methodReference = PhpElementsUtil.findMethodReferenceOnClassConstant(psiElement);
                if (methodReference == null) {
                    return;
                }

                if (!(
                    PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Common\\Persistence\\ObjectManager", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Common\\Persistence\\ManagerRegistry", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Persistence\\ObjectManager", "getRepository") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Doctrine\\Persistence\\ManagerRegistry", "getRepository") ||

                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Doctrine\\ORM\\EntityManagerInterface", "getReference") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Doctrine\\ORM\\EntityManagerInterface", "getClassMetadata") ||
                        PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Doctrine\\ORM\\EntityManagerInterface", "getPartialReference")
                )) {
                    return;
                }

                Collection<DoctrineModel> modelClasses = EntityHelper.getModelClasses(psiElement.getProject());

                for (DoctrineModel doctrineModel : modelClasses) {
                    PhpClass phpClass = doctrineModel.getPhpClass();
                    if (phpClass.isAbstract() || phpClass.isInterface()) {
                        continue;
                    }

                    LookupElement elementBuilder = new Foo(phpClass);

                    completionResultSet.addElement(
                        PrioritizedLookupElement.withPriority(elementBuilder, 100)
                    );
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    private static class Foo extends ClassConstantLookupElementAbstract {

        public Foo(PhpClass phpClass) {
            super(phpClass);
        }

        @Override
        public void renderElement(LookupElementPresentation presentation) {
            super.renderElement(presentation);
            presentation.setIcon(Symfony2Icons.DOCTRINE);
        }
    }
}
