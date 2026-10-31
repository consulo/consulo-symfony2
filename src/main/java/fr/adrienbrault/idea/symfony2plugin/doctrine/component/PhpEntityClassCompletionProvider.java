package fr.adrienbrault.idea.symfony2plugin.doctrine.component;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpClassLookupElement;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.util.PhpContractUtil;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassReferenceInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpEntityClassCompletionProvider implements CompletionProvider {

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Map<String, String> entityNamespaces = ServiceXmlParserFactory.getInstance(project, EntityNamesServiceParser.class).getEntityNameMap();

        // copied from PhpCompletionUtil::addClassesInNamespace looks the official way to find classes in namespaces
        // its a really performance nightmare

        Collection<String> names = phpIndex.getAllClassNames(new CamelHumpMatcher(resultSet.getPrefixMatcher().getPrefix()));
        for (String name : names) {
            Collection<PhpClass> classes = phpIndex.getClassesByName(name);

            for(Map.Entry<String, String> entry: entityNamespaces.entrySet()) {
                String namespaceFqn = PhpLangUtil.toFQN(entry.getValue());
                Collection<PhpClass> filtered = filterByNamespace(classes, namespaceFqn);
                for (PhpClass phpClass : filtered) {
                    resultSet.addElement(new PhpClassLookupElement(phpClass, true, PhpClassReferenceInsertHandler.getInstance()));
                }
            }
        }
    }

    /**
     * Replacment for "PhpCompletionUtil.filterByNamespace(classes, namespaceFqn)"
     */
    public static Collection<PhpClass> filterByNamespace(Collection<PhpClass> classes, String namespaceFQN) {
        PhpContractUtil.assertFqn(namespaceFQN);
        // consulo-port: StreamEx is not available, plain java streams
        return classes.stream().filter((aClass) -> PhpLangUtil.equalsClassNames(namespaceFQN, StringUtil.trimEnd(aClass.getNamespaceName(), "\\"))).collect(Collectors.toCollection(HashSet::new));
    }
}
