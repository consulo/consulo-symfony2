/**
 * Twig code fences in Markdown, loaded only with the Markdown plugin
 */
module fr.adrienbrault.idea.symfony2plugin.markdown {
    requires consulo.language.api;
    requires consulo.language.editor.api;
    requires consulo.language.impl;
    requires consulo.markdown;
    requires consulo.twig;

    exports fr.adrienbrault.idea.symfony2plugin.markdown;
}
