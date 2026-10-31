package fr.adrienbrault.idea.symfony2plugin.composer;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.php.composer.newProject.ComposerProjectTemplate;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

/**
 * "Symfony Skeleton" of the Composer new project wizard: {@code composer create-project symfony/skeleton}, the minimal
 * Symfony application for microservices, console applications or APIs
 */
@ExtensionImpl
public class SymfonySkeletonComposerProjectTemplate implements ComposerProjectTemplate {
    @Override
    public String getId() {
        return "symfony-skeleton";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony Skeleton");
    }

    @Override
    public Image getIcon() {
        return Symfony2Icons.SYMFONY;
    }

    @Override
    public String getPackageName() {
        return "symfony/skeleton";
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("A minimal Symfony application (symfony/skeleton) for microservices, console applications or APIs");
    }
}
