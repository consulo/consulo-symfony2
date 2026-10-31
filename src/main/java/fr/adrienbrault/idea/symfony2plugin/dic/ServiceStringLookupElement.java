package fr.adrienbrault.idea.symfony2plugin.dic;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceStringLookupElement extends LookupElement {

    private final ContainerService containerService;

    private boolean boldText = false;

    public ServiceStringLookupElement(ContainerService containerService) {
        this.containerService = containerService;
    }

    public ServiceStringLookupElement(ContainerService containerService, boolean boldText) {
        this(containerService);
        this.boldText = boldText;
    }

    @Override
    public String getLookupString() {
        return containerService.getName();
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);

        String className = getClassName(containerService);
        if(className != null) {
            presentation.setTypeText(StringUtils.strip(className, "\\"));
        }

        // private or non container services
        if(className == null || containerService.isWeak()) {
            presentation.setIcon(Symfony2Icons.SERVICE_OPACITY);
        } else {
            presentation.setIcon(Symfony2Icons.SERVICE);
        }

        if(this.containerService.isPrivate()) {
            presentation.setIcon(Symfony2Icons.SERVICE_PRIVATE_OPACITY);
        }

        presentation.setItemTextBold(this.boldText);
        if(this.boldText) {
            presentation.setTypeGrayed(false);
            presentation.setItemTextUnderlined(true);
        }

        presentation.setStrikeout(containerService.isDeprecated());
    }

    @Nullable
    private String getClassName(ContainerService containerService) {
        return ContainerUtil.find(containerService.getClassNames(), Objects::nonNull);
    }
}
