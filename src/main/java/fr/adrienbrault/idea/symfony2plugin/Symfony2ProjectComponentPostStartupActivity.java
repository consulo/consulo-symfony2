package fr.adrienbrault.idea.symfony2plugin;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of the nested {@link Symfony2ProjectComponent.PostStartupActivity} (upstream: postStartupActivity in plugin.xml).
 * Consulo cannot register nested classes via @ExtensionImpl.
 */
@ExtensionImpl
public class Symfony2ProjectComponentPostStartupActivity extends Symfony2ProjectComponent.PostStartupActivity {
}
