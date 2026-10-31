package fr.adrienbrault.idea.symfony2plugin.form.completion;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ConstantReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.PrioritizedLookupElement;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "before PhpCompletionContributor") // upstream order="first"
public class FormCompletionContributor extends CompletionContributor {

    public FormCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(ConstantReference.class), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                // $builder->add('field', Type::class), $factory->create(Type::class), $this->createForm(type: Type::class)
                ConstantReference argument = (ConstantReference) completionParameters.getPosition().getParent();
                if (!(argument.getParent() instanceof ParameterList parameters)
                    || !(parameters.getParent() instanceof MethodReference method)
                    || FormUtil.getFormTypeArgument(method) != argument) {
                    return;
                }

                for (FormTypeClass formType : FormUtil.getFormTypeClasses(psiElement.getProject()).values()) {
                    PhpClass phpClass = formType.getPhpClass(psiElement.getProject());
                    if (phpClass == null || phpClass.isAbstract() || phpClass.isInterface() || phpClass.isTrait() || phpClass.isEnum()) {
                        continue;
                    }

                    LookupElement elementBuilder = new FormClassConstantsLookupElement(phpClass, formType.getType());

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
}
