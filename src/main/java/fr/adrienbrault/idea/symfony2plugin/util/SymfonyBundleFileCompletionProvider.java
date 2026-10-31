package fr.adrienbrault.idea.symfony2plugin.util;

import consulo.content.ContentIterator;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.dict.BundleFile;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ResourceFileInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundleFileLookupElement;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyBundleFileCompletionProvider implements CompletionProvider {

    private final String[] paths;

    public SymfonyBundleFileCompletionProvider(String... paths) {
        this.paths = paths;
    }

    @Override
    public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {

        if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
            return;
        }

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(completionParameters.getPosition().getProject());
        List<BundleFile> bundleFiles = new ArrayList<>();

        for(SymfonyBundle symfonyBundle : symfonyBundleUtil.getBundles()) {
            for(String path: this.paths) {
                visitPath(completionParameters, bundleFiles, symfonyBundle, path);
            }
        }

        for(BundleFile bundleFile : bundleFiles) {
            completionResultSet.addElement(new SymfonyBundleFileLookupElement(bundleFile, ResourceFileInsertHandler.getInstance()));
        }

    }

    private void visitPath(CompletionParameters completionParameters, List<BundleFile> bundleFiles, SymfonyBundle symfonyBundle, String path) {

        VirtualFile virtualFile = symfonyBundle.getRelative(path);
        if(virtualFile == null) {
            return;
        }

        final BundleContentIterator bundleContentIterator = new BundleContentIterator(symfonyBundle, bundleFiles, completionParameters.getPosition().getProject());
        VirtualFileUtil.visitChildrenRecursively(virtualFile, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(VirtualFile virtualFile) {
                bundleContentIterator.processFile(virtualFile);
                return super.visitFile(virtualFile);
            }
        });

    }

    private static class BundleContentIterator implements ContentIterator{

        private final SymfonyBundle symfonyBundle;
        private final List<BundleFile> bundleFiles;
        private final Project project;

        public BundleContentIterator(SymfonyBundle symfonyBundle, List<BundleFile> bundleFiles, Project project) {
            this.symfonyBundle = symfonyBundle;
            this.bundleFiles = bundleFiles;
            this.project = project;
        }

        @Override
        public boolean processFile(VirtualFile virtualFile) {
            if(!virtualFile.isDirectory()) {
                bundleFiles.add(new BundleFile(this.symfonyBundle, virtualFile, project));
            }

            return true;
        }
    }
}
