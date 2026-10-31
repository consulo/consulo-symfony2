package fr.adrienbrault.idea.symfony2plugin.navigation.controller;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import com.jetbrains.twig.icons.TwigIcons;

import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after ModelsControllerRelatedGotoCollector")
public class TemplatesControllerRelatedGotoCollector implements ControllerActionGotoRelatedCollector {
    @Override
    public void collectGotoRelatedItems(ControllerActionGotoRelatedCollectorParameter parameter) {
        HashSet<String> uniqueTemplates = new HashSet<>();

        PhpMethodVariableResolveUtil.visitRenderTemplateFunctions(parameter.getMethod(), triple -> {
            uniqueTemplates.add(triple.getFirst());
        });

        for (String uniqueTemplate : uniqueTemplates) {
            for (PsiFile templateTarget : TwigUtil.getTemplatePsiElements(parameter.getProject(), uniqueTemplate)) {
                parameter.add(
                    new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, uniqueTemplate)
                        .withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER)
                );
            }
        }
    }
}
