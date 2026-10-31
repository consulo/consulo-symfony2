package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after StaticVariableCollector")
public class GlobalExtensionVariableCollector implements TwigFileVariableCollector {
    private static final Key<CachedValue<Map<String, PsiVariable>>> TWIG_EXTENSION_GLOBAL_VARIABLES = new Key<>("TWIG_EXTENSION_GLOBAL_VARIABLES");

    @Override
    public void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {
        variables.putAll(getGlobals(parameter.getProject()));
    }

    private static Map<String, PsiVariable> getGlobals(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TWIG_EXTENSION_GLOBAL_VARIABLES,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableMap(getGlobalsInner(project)),
                PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
            ),
            false
        );
    }

    private static Map<String, PsiVariable> getGlobalsInner(Project project) {
        Map<String, PsiVariable> variables = new HashMap<>();

        for(PhpClass phpClass : TwigUtil.getTwigExtensionClasses(project)) {
            if(!PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile())) {
                Method method = phpClass.findMethodByName("getGlobals");
                if(method != null) {
                    Collection<PhpReturn> phpReturns = PsiTreeUtil.findChildrenOfType(method, PhpReturn.class);
                    for(PhpReturn phpReturn: phpReturns) {
                        PhpPsiElement returnPsiElement = phpReturn.getFirstPsiChild();
                        if(returnPsiElement instanceof ArrayCreationExpression) {
                            variables.putAll(PhpMethodVariableResolveUtil.getTypesOnArrayHash((ArrayCreationExpression) returnPsiElement));
                        }
                    }
                }
            }
        }

        return variables;
    }
}
