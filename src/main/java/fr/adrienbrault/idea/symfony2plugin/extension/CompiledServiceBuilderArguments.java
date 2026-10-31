package fr.adrienbrault.idea.symfony2plugin.extension;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @deprecated No longer used; {@link CompiledServiceBuilderFactory} extension support has been removed.
 */
@Deprecated
public class CompiledServiceBuilderArguments {

    public record StreamEntry(InputStream stream, VirtualFile virtualFile) {}

    final private Collection<StreamEntry> streams = new ArrayList<>();

    private final Project project;

    public CompiledServiceBuilderArguments(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public Collection<StreamEntry> getStreams() {
        return streams;
    }

    public void addStream(InputStream inputStream, VirtualFile virtualFile) {
        streams.add(new StreamEntry(inputStream, virtualFile));
    }
}
