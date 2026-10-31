package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.dataholder.Key;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after GlobalExtensionVariableCollector")
public class ControllerDocVariableCollector implements TwigFileVariableCollector {

    public static final String DOC_PATTERN  = "\\{#[\\s]+@[C|c]ontroller[\\s]+([\\w\\\\\\[\\]:]+)[\\s]+#}";
    public static final String DOC_PATTERN_COMPLETION  = "\\{#[\\s]+@[C|c]ontroller[\\s]+.*#}";
    private static final Key<CachedValue<List<String>>> CONTROLLER_DOC_BLOCKS_CACHE = new Key<>("TWIG_CONTROLLER_DOC_BLOCKS");

    @Override
    public void collectPsiVariables(TwigFileVariableCollectorParameter parameter, Map<String, PsiVariable> variables) {
        PsiFile psiFile = parameter.getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return;
        }

        List<String> controllerNames = findFileControllerDocBlocks((TwigFile) psiFile);
        if(controllerNames.isEmpty()) {
            return;
        }

        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(psiFile);
        ControllerIndex controllerIndex = new ControllerIndex(parameter.getProject());

        for(String controllerName: controllerNames) {
            for(Method method : controllerIndex.resolveShortcutName(controllerName)) {
                variables.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(method, templateNames));
            }
        }
    }

    private static List<String> findFileControllerDocBlocks(TwigFile twigFile) {
        return LanguageCachedValueUtil.getCachedValue(
            twigFile,
            CONTROLLER_DOC_BLOCKS_CACHE,
            () -> CachedValueProvider.Result.create(
                Collections.unmodifiableList(findFileControllerDocBlocksInner(twigFile)),
                twigFile
            )
        );
    }

    private static List<String> findFileControllerDocBlocksInner(TwigFile twigFile) {

        Pattern pattern = Pattern.compile(DOC_PATTERN);

        ArrayList<String> controller = new ArrayList<>();

        for(PsiComment psiComment: PsiTreeUtil.getChildrenOfTypeAsList(twigFile, PsiComment.class)) {
            Matcher matcher = pattern.matcher(psiComment.getText());
            if (matcher.find()) {
                controller.add(matcher.group(1));
            }
        }

        return controller;
    }
}
