package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @deprecated No longer used; the extension point "fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderFactory" has been removed.
 */
@Deprecated
public interface CompiledServiceBuilderFactory {

    Builder create();

    interface Builder {
        void build(CompiledServiceBuilderArguments args);
        boolean isModified(Project project);
    }
}
