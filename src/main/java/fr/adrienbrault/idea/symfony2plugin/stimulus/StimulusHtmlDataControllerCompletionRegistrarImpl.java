package fr.adrienbrault.idea.symfony2plugin.stimulus;

import consulo.annotation.component.ExtensionImpl;

/**
 * Registration of the nested {@link StimulusControllerCompletionRegistrar.HtmlDataControllerCompletionRegistrar}
 * (Consulo cannot register nested classes via {@code @ExtensionImpl}).
 */
@ExtensionImpl
public class StimulusHtmlDataControllerCompletionRegistrarImpl extends StimulusControllerCompletionRegistrar.HtmlDataControllerCompletionRegistrar {
}
