package fr.adrienbrault.idea.symfony2plugin.stimulus;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of the nested {@link StimulusControllerCompletionRegistrar.StimulusTwigCompletionRegistrar}
 * (Consulo cannot register nested classes via {@code @ExtensionImpl}).
 */
@ExtensionImpl
public class StimulusTwigCompletionRegistrarImpl extends StimulusControllerCompletionRegistrar.StimulusTwigCompletionRegistrar {
}
