package fr.adrienbrault.idea.symfony2plugin.templating.variable.collector;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.ast.ASTNode;
import consulo.util.lang.Pair;
import consulo.language.psi.PsiElement;
import consulo.language.impl.ast.TreeUtil;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after IncludeVariableCollector")
public class MarcoScopeVariableCollector implements TwigFileVariableCollector {
    @Override
    public void collect(TwigFileVariableCollectorParameter parameter, Map<String, Set<String>> variables) {
        ASTNode macroStatement = TreeUtil.findParent(parameter.getElement().getNode(), TwigElementTypes.MACRO_STATEMENT);
        if(macroStatement == null) {
            return;
        }

        PsiElement psiElement = macroStatement.getPsi();
        if(psiElement == null) {
            return;
        }

        PsiElement marcoTag = psiElement.getFirstChild();
        if(marcoTag == null) {
            return;
        }

        Pair<String, String> pair = TwigUtil.getTwigMacroNameAndParameter(marcoTag);
        if(pair == null || pair.getSecond() == null) {
            return;
        }

        // strip braces "(foobar, foo)"
        String args = StringUtils.stripStart(pair.getSecond(), "( ");
        args = StringUtils.stripEnd(args, ") ");

        for (String s : args.split("\\s*,\\s*")) {
            variables.put(s, Collections.emptySet());
        }
    }
}
