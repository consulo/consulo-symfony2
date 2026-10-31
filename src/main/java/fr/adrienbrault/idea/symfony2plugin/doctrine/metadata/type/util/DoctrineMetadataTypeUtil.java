package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.type.util;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.Pair;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineManagerEnum;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMetadataTypeUtil {

    public static final String DBAL_TYPE = "Doctrine\\DBAL\\Types\\Type";
    public static final String COUCHDB_TYPE = "Doctrine\\ODM\\CouchDB\\Types\\Type";
    public static final String MONGODB_TYPE = "Doctrine\\ODM\\MongoDB\\Types\\Type";

    public static Collection<String> getTypeClassesByScopeWithAllFallback(PsiElement psiElement) {
        DoctrineManagerEnum manager = DoctrineMetadataUtil.findManagerByScope(psiElement);
        if(manager != null) {
            return DoctrineMetadataTypeUtil.getTypeClassByManager(manager);
        }

        return getAllTypes();
    }

    private static Collection<String> getTypeClassByManager(DoctrineManagerEnum managerEnum) {
        if(managerEnum == DoctrineManagerEnum.ORM) {
            return Collections.singleton(DBAL_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.COUCHDB) {
            return Collections.singleton(COUCHDB_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.MONGODB) {
            return Collections.singleton(MONGODB_TYPE);
        } else if(managerEnum == DoctrineManagerEnum.ODM || managerEnum == DoctrineManagerEnum.DOCUMENT) {
            return Arrays.asList(MONGODB_TYPE, COUCHDB_TYPE);
        }

        return Collections.emptyList();
    }

    private static Collection<String> getAllTypes() {
        return Arrays.asList(
            DBAL_TYPE,
            COUCHDB_TYPE,
            MONGODB_TYPE
        );
    }

    public static void visitType(Project project, Collection<String> typeInterfaces, Processor<Pair<PhpClass, String>> processor) {
        for (String typeInterface : typeInterfaces) {
            for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, typeInterface)) {
                String name = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");

                // non string value;
                // Doctrine resolves also on class name
                if(name == null) {
                    String className = phpClass.getName();
                    if(className.endsWith("Type")) {
                        name = className.substring(0, className.length() - 4).toLowerCase();
                    }
                }

                if(name == null) {
                    continue;
                }

                processor.process(Pair.create(phpClass, name));
            }
        }
    }
}
