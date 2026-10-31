package fr.adrienbrault.idea.symfony2plugin.translation;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of {@link ConstraintMessageGotoCompletionRegistrar.ConstraintMessageAnnotationCompletionProvider}
 * (was annotation.xml PhpAnnotationCompletionProvider); Consulo cannot register nested classes via @ExtensionImpl.
 */
@ExtensionImpl
public class ConstraintMessageAnnotationCompletionProviderImpl extends ConstraintMessageGotoCompletionRegistrar.ConstraintMessageAnnotationCompletionProvider {
}
