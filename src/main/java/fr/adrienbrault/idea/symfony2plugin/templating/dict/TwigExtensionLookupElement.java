package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.PhpPresentationUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionLookupElement extends LookupElement {
    final private TwigExtension twigExtension;

    final private String name;

    final private Project project;

    public TwigExtensionLookupElement(Project project, String name, TwigExtension twigExtension) {
        this.project = project;
        this.name = name;
        this.twigExtension = twigExtension;
    }

    @Override
    public String getLookupString() {
        return name;
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if(twigExtension.getTwigExtensionType() == TwigExtensionParser.TwigExtensionType.SIMPLE_FUNCTION) {
            TwigExtensionInsertHandler.getInstance().handleInsert(context, this, twigExtension);
        }

        super.handleInsert(context);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setIcon(TwigExtensionParser.getIcon(this.twigExtension.getTwigExtensionType()));

        buildTailText(presentation);

        presentation.setItemText(name);
        presentation.setTypeText(StringUtils.camelize(this.twigExtension.getType().toLowerCase()));
        presentation.setTypeGrayed(true);
    }

    private void buildTailText(LookupElementPresentation presentation) {
        if(this.twigExtension.getTwigExtensionType() == TwigExtensionParser.TwigExtensionType.SIMPLE_TEST) {
            return;
        }

        String signature = this.twigExtension.getSignature();
        if(signature == null) {
            return;
        }

        Collection<? extends PhpNamedElement> phpNamedElements = PhpIndex.getInstance(this.project).getBySignature(signature);
        if(phpNamedElements.isEmpty()) {
            return;
        }

        PhpNamedElement function = phpNamedElements.iterator().next();
        if(function instanceof Function) {
            List<Parameter> parameters = new LinkedList<>(Arrays.asList(((Function) function).getParameters()));

            if(this.twigExtension.getOption("needs_context") != null && !parameters.isEmpty()) {
                parameters.remove(0);
            }

            if(this.twigExtension.getOption("needs_environment") != null && !parameters.isEmpty()) {
                parameters.remove(0);
            }

            presentation.setTailText(PhpPresentationUtil.formatParameters(null, parameters.toArray(new Parameter[0])).toString(), true);
        }
    }
}