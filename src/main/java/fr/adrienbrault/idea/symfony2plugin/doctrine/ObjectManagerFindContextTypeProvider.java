package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpReference;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelInterface;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * $repository->find()->get<caret>Id();
 * $this->repository->find()->ge<caret>tId();
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ObjectManagerFindContextTypeProvider implements PhpTypeProvider4 {
    private static final char OBJECT_MANAGER_CONTEXT_TRIM_KEY = 'Ƃ';

    @Override
    public char getKey() {
        return 'ų';
    }

    // Index-safe only: no PhpIndex here.
    @Override
    public @Nullable PhpType getType(PsiElement e) {
        if (!(e instanceof MethodReference methodReference)) {
            return null;
        }

        Settings settings = Settings.getEnabledInstance(e);
        if (settings == null || !settings.featureTypeProvider) {
            return null;
        }

        String refSignature = methodReference.getSignature();
        if (StringUtil.isEmpty(refSignature)) {
            return null;
        }

        String methodRefName = methodReference.getName();
        if (
            methodRefName == null ||
            (!"find".equals(methodRefName) && !"findAll".equals(methodRefName) && !methodRefName.startsWith("findOneBy") && !methodRefName.startsWith("findBy"))
        ) {
            return null;
        }

        String signature = null;
        PhpPsiElement firstPsiChild = methodReference.getFirstPsiChild();
        // reduce supported scope here; by checking via "instanceof"
        if (firstPsiChild instanceof Variable variable) {
            signature = variable.getSignature();
        }
        else if (firstPsiChild instanceof PhpReference phpReference) {
            signature = phpReference.getSignature();
        }

        if (StringUtils.isBlank(signature)) {
            return null;
        }

        return new PhpType().add("#" + getKey() + signature + OBJECT_MANAGER_CONTEXT_TRIM_KEY + methodRefName);
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        // Kotlin split + dropLastWhile { it.isEmpty() }: same as java split which drops trailing empty strings
        String[] split = s.substring(2).split(String.valueOf(OBJECT_MANAGER_CONTEXT_TRIM_KEY));
        if (split.length < 2) {
            return null;
        }

        String signature = split[0];
        String methodName = split[1];
        Set<PhpClass> repositoryClasses = new LinkedHashSet<>();

        // collect the instances of the type before our call; which we extract on "getType()"
        for (PhpNamedElement phpNamedElement : PhpIndex.getInstance(project).getBySignature(signature)) {
            // #C\Foo\BarRepository
            if (phpNamedElement instanceof PhpClass phpClass) {
                repositoryClasses.add(phpClass);
                continue;
            }

            // resolve the the previous type
            for (String type : phpNamedElement.getType().filterPrimitives().getTypes()) {
                repositoryClasses.addAll(PhpIndex.getInstance(project).getAnyByFQN(type));
            }
        }

        // only all repository class
        Set<PhpClass> repositories = new LinkedHashSet<>();
        for (PhpClass phpClass : repositoryClasses) {
            if (PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository") ||
                PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Persistence\\ObjectRepository")) {
                repositories.add(phpClass);
            }
        }

        if (repositories.isEmpty()) {
            return null;
        }

        // based on repositoryClass find the Entity which as defined it via metadata
        Set<String> types = new LinkedHashSet<>();
        for (PhpClass phpClass : repositories) {
            Collection<DoctrineModelInterface> models = DoctrineMetadataUtil.findMetadataModelForRepositoryClass(project, phpClass.getPresentableFQN());
            for (DoctrineModelInterface doctrineModel : models) {
                for (PhpClass entityClass : PhpElementsUtil.getClassesInterface(project, doctrineModel.getClassName())) {
                    types.add(
                        "findAll".equals(methodName) || methodName.startsWith("findBy")
                            ? entityClass.getFQN() + "[]"
                            : entityClass.getFQN()
                    );
                }
            }
        }

        if (types.isEmpty()) {
            return null;
        }

        PhpType phpType = new PhpType();
        types.forEach(phpType::add);
        return phpType;
    }

    @Override
    public @Nullable Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        return null;
    }
}
