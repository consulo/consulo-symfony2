package fr.adrienbrault.idea.symfony2plugin.routing;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteLookupElement extends LookupElement {

    final private Route route;
    private boolean isWeak = false;
    private InsertHandler<RouteLookupElement> insertHandler;
    private boolean escape = false;

    public RouteLookupElement(Route route) {
        this.route = route;
    }

    public RouteLookupElement(Route route, boolean isWeak) {
        this(route);
        this.isWeak = isWeak;
    }

    @Override
    public String getLookupString() {
        if (escape) {
            return route.getName().replace("\\", "\\\\");
        }

        return route.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(route.getController());
        presentation.setTypeGrayed(true);
        presentation.setIcon(!this.isWeak ? Symfony2Icons.ROUTE : Symfony2Icons.ROUTE_WEAK);

        formatTail(presentation);
    }

    /**
     * "(GET|POST, var1, var2)"
     */
    private void formatTail(LookupElementPresentation presentation) {
        List<String> tails = new ArrayList<>();

        Collection<String> methods = route.getMethods();
        if(!methods.isEmpty()) {
            tails.add(StringUtils.join(ContainerUtil.map(methods, String::toUpperCase), "|"));
        }

        Set<String> variables = this.route.getVariables();
        if(!variables.isEmpty()) {
            tails.addAll(variables);
        }

        if(!tails.isEmpty()) {
            presentation.setTailText("(" + StringUtils.join(tails, ", ") + ")", true);
        }
    }

    @Override
    public void handleInsert(InsertionContext context) {
        if(insertHandler != null) {
            insertHandler.handleInsert(context, this);
        }
        super.handleInsert(context);
    }

    public void withInsertHandler(InsertHandler<RouteLookupElement> insertHandler) {
        this.insertHandler = insertHandler;
    }

    public void withEscape() {
        this.escape = true;
    }

    public Route getRoute() {
        return route;
    }

}
