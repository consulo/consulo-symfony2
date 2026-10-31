package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resolve "find*" and attach the entity from the getRepository method
 *
 * "$om->getRepository('\Foo\Bar')->find('foobar')->get<caret>Id()"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ObjectRepositoryResultTypeProvider implements PhpTypeProvider4 {
    private static final MethodMatcher.CallToSignature[] FIND_SIGNATURES = new MethodMatcher.CallToSignature[]{
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "find"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findOneBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findAll"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Common\\Persistence\\ObjectRepository", "findBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "find"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findOneBy"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findAll"),
        new MethodMatcher.CallToSignature("\\Doctrine\\Persistence\\ObjectRepository", "findBy"),
    };

    private static final Set<String> MANAGED_FIND_METHOD = new LinkedHashSet<>(Arrays.asList("find", "findOneBy", "findAll", "findBy"));

    private static final char OBJECT_REPOSITORY_RESULT_TRIM_KEY = 'Ƅ';

    @Override
    public char getKey() {
        return 'Œ';
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

        // we can get the repository name from the signature calls
        // #M#?#M#?#M#C\Foo\Bar\Controller\BarController.get?doctrine.getRepository?EntityBundle:User.find
        String repositorySignature = methodReference.getSignature();

        int lastRepositoryName = repositorySignature.lastIndexOf(ObjectRepositoryTypeProvider.TRIM_KEY);
        if (lastRepositoryName == -1) {
            return null;
        }

        repositorySignature = repositorySignature.substring(lastRepositoryName);
        int nextMethodCall = repositorySignature.indexOf('.' + methodRefName);
        if (nextMethodCall == -1) {
            return null;
        }

        repositorySignature = repositorySignature.substring(1, nextMethodCall);
        return new PhpType().add("#" + getKey() + refSignature + OBJECT_REPOSITORY_RESULT_TRIM_KEY + repositorySignature);
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        int endIndex = s.lastIndexOf(OBJECT_REPOSITORY_RESULT_TRIM_KEY);
        if (endIndex == -1) {
            return null;
        }

        String originalSignature = s.substring(0, endIndex);
        String parameter = PhpTypeProviderUtil.getResolvedParameter(
            PhpIndex.getInstance(project),
            s.substring(endIndex + 1)
        );
        if (parameter == null) {
            return null;
        }

        PhpClass phpClass = EntityHelper.resolveShortcutName(project, parameter);
        if (phpClass == null) {
            return null;
        }

        PhpIndex phpIndex = PhpIndex.getInstance(project);
        Collection<PhpNamedElement> typeSignature = getTypeSignatureMagic(phpIndex, originalSignature);

        // ->getRepository(SecondaryMarket::class)->findAll() => "findAll", but only if its a instance of this method;
        // so non Doctrine method are already filtered
        Set<String> resolveMethods = new LinkedHashSet<>();
        for (Method method : getObjectRepositoryCall(typeSignature)) {
            resolveMethods.add(method.getName());
        }

        if (resolveMethods.isEmpty()) {
            return null;
        }

        PhpType phpType = new PhpType();
        Set<String> types = new LinkedHashSet<>();
        for (String methodName : resolveMethods) {
            types.add("findAll".equals(methodName) || methodName.startsWith("findBy") ? phpClass.getFQN() + "[]" : phpClass.getFQN());
        }
        types.forEach(phpType::add);

        return phpType;
    }

    @Override
    public @Nullable Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        return null;
    }

    private Collection<Method> getObjectRepositoryCall(Collection<PhpNamedElement> phpNamedElements) {
        Set<PhpNamedElement> filtered = new LinkedHashSet<>();
        for (PhpNamedElement phpNamedElement : phpNamedElements) {
            if (phpNamedElement instanceof Method method && PhpElementsUtil.isMethodInstanceOf(method, FIND_SIGNATURES)) {
                filtered.add(phpNamedElement);
            }
        }

        Collection<Method> methods = new ArrayList<>();
        for (PhpNamedElement phpNamedElement : filtered) {
            if (phpNamedElement instanceof Method method) {
                methods.add(method);
            }
        }

        return methods;
    }

    /**
     * We can have multiple types inside a TypeProvider; split them on "|" so that we dont get empty types
     *
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository|
     * #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.getRepository
     */
    private Collection<PhpNamedElement> getTypeSignatureMagic(PhpIndex phpIndex, String signature) {
        // magic method resolving; we need to have the ObjectRepository method which does not exists for magic methods, so strip it
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findByName => findBy
        // #M#x#M#C\FooBar.get?doctrine.odm.mongodb.document_manager.findOneBy => findOne
        Set<PhpNamedElement> elements = new LinkedHashSet<>();
        String[] signatures = signature.split("\\|");
        for (String signaturePart : signatures) {
            String part = signaturePart;
            int separator = part.lastIndexOf('.');
            if (separator > 0) {
                // method already exists in repository use it
                for (PhpNamedElement phpNamedElement : phpIndex.getBySignature(part, null, 0)) {
                    if (phpNamedElement instanceof Method method) {
                        if (MANAGED_FIND_METHOD.contains(method.getName())) {
                            continue;
                        }

                        // we got into the repository itself so stop here; was not overwritten by repository class
                        PhpClass containingClass = method.getContainingClass();
                        if (
                            PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Persistence\\ObjectRepository") ||
                            PhpElementsUtil.isEqualClassName(containingClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository")
                        ) {
                            continue;
                        }

                        return Collections.emptyList();
                    }
                }

                // strip field name from the method name "findOneByName" => "findOneBy"
                String methodName = part.substring(separator + 1);
                if (methodName.startsWith("findOneBy")) {
                    part = part.substring(0, separator + 1) + "findOneBy";
                }
                else if (methodName.startsWith("findBy")) {
                    part = part.substring(0, separator + 1) + "findBy";
                }
            }

            elements.addAll(phpIndex.getBySignature(part, null, 0));
        }

        return elements;
    }
}
