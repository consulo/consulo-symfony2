package fr.adrienbrault.idea.symfony2plugin.composer;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.php.composer.newProject.ComposerProjectTemplate;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

import java.util.List;

/**
 * "Symfony Web Application" of the Composer new project wizard - what {@code symfony new --webapp} does:
 * {@code composer create-project symfony/skeleton} and then {@code composer require webapp}, the pack with Twig,
 * Doctrine, forms, security and the other packages of a traditional web application
 */
@ExtensionImpl
public class SymfonyWebAppComposerProjectTemplate implements ComposerProjectTemplate {
    @Override
    public String getId() {
        return "symfony-webapp";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony Web Application");
    }

    @Override
    public Image getIcon() {
        return Symfony2Icons.SYMFONY;
    }

    @Override
    public String getPackageName() {
        return "symfony/skeleton";
    }

    /**
     * {@code webapp} is the Flex alias of {@code symfony/webapp-pack}
     */
    @Override
    public List<String> getRequiredPackages() {
        return List.of("webapp");
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("A Symfony web application (symfony/skeleton with the webapp pack) with Twig, Doctrine, forms and security");
    }
}
