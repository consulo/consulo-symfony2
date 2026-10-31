package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpClassCompletionProvider implements CompletionProvider {

    private final boolean withInterface;
    private boolean withLeadingBackslash = false;

    public PhpClassCompletionProvider() {
        this(false);
    }

    public PhpClassCompletionProvider(boolean withInterface) {
        this.withInterface = withInterface;
    }

    public PhpClassCompletionProvider withTrimLeadBackslash(boolean withLeadingBackslash) {
        this.withLeadingBackslash = withLeadingBackslash;
        return this;
    }

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, final CompletionResultSet resultSet) {

        PsiElement psiElement = parameters.getOriginalPosition();
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        addClassCompletion(parameters, resultSet, psiElement, withInterface, withLeadingBackslash);

    }
    public static void addClassCompletion(CompletionParameters parameters, final CompletionResultSet resultSet, PsiElement psiElement, boolean withInterface) {
        addClassCompletion(parameters, resultSet, psiElement, withInterface, false);
    }

    public static void addClassCompletion(CompletionParameters parameters, final CompletionResultSet resultSet, PsiElement psiElement, boolean withInterface, boolean withLeadBackslash) {

        // Foo\|Bar
        // Foo|\Bar
        PhpElementsUtil.visitNamespaceClassForCompletion(psiElement, parameters.getOffset(), (phpClass, presentableFQN, prefix) ->
            resultSet.addElement(LookupElementBuilder.create(prefix + presentableFQN).withIcon(IconDescriptorUpdaters.getIcon(phpClass, 0)))
        );

        PhpIndex phpIndex = PhpIndex.getInstance(psiElement.getProject());
        for (String className : phpIndex.getAllClassNames(resultSet.getPrefixMatcher())) {

            for(PhpClass phpClass: phpIndex.getClassesByName(className)) {
                resultSet.addElement(
                    wrapInsertHandler(new MyPhpLookupElement(phpClass), withLeadBackslash)
                );
            }

            if(withInterface) {
                for(PhpClass phpClass: phpIndex.getInterfacesByName(className)) {
                    resultSet.addElement(
                        wrapInsertHandler(new MyPhpLookupElement(phpClass), withLeadBackslash)
                    );
                }
            }

        }

    }

    private static MyPhpLookupElement wrapInsertHandler(MyPhpLookupElement lookupElement, boolean withLeadBackslash) {

        if(withLeadBackslash) {
            return lookupElement.withInsertHandler(PhpReferenceInsertHandler.getInstance());
        }

        return lookupElement.withInsertHandler(PhpReferenceTrimBackslashInsertHandler.getInstance());
    }

    private static class MyPhpLookupElement extends PhpLookupElement {

        public MyPhpLookupElement(PhpNamedElement namedElement) {
            super(namedElement);
        }

        public MyPhpLookupElement withInsertHandler(InsertHandler insertHandler) {
            this.handler = insertHandler;
            return this;
        }

    }

}
