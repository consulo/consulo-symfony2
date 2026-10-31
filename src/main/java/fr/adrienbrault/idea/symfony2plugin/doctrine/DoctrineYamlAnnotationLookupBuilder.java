package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineYamlAnnotationLookupBuilder implements CompletionProvider {

    private final String annotationClassName;
    private final String[] staticField;

    public DoctrineYamlAnnotationLookupBuilder(String annotationClassName, String... staticFields) {
        this.annotationClassName = annotationClassName;
        this.staticField = staticFields;
    }

    public static Set<String> getAnnotations(Project project, String className) {

        HashSet<String> map = new HashSet<>();

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
            return map;
        }

        for(Field field: phpClass.getFields()) {
            if(!field.isConstant()) {
                map.add(field.getName());
            }
        }

        return map;
    }

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {

        PsiElement position = parameters.getPosition();
        if(!Symfony2ProjectComponent.isEnabled(position)) {
            return;
        }

        for(String s: getAnnotations(position.getProject(), annotationClassName)) {
            completionResultSet.addElement(LookupElementBuilder.create(s).withIcon(Symfony2Icons.DOCTRINE));
        }

        for(String s: staticField) {
            completionResultSet.addElement(LookupElementBuilder.create(s).withIcon(Symfony2Icons.DOCTRINE));
        }

    }

}
