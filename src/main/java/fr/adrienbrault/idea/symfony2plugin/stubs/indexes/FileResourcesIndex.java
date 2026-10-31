package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.FileResourceExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.FileResourceVisitorUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class FileResourcesIndex extends FileBasedIndexExtension<String, FileResource> {

    private static final int MAX_FILE_BYTE_SIZE = 1048576;
    private static final FileResourceExternalizer EXTERNALIZER = FileResourceExternalizer.INSTANCE;

    public static final ID<String, FileResource> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.file_resources");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    public ID<String, FileResource> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, FileResource, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !isValidForIndex(inputData, psiFile)) {
                return Collections.emptyMap();
            }

            final Map<String, FileResource> items = new HashMap<>();

            FileResourceVisitorUtil.visitFile(psiFile, consumer -> {
                String resource = consumer.getResource();
                if (resource.length() < 255) {
                    items.put(resource, consumer.createFileResource());
                }
            });

            return items;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<FileResource> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return FileInputFilter.XML_YAML;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 4;
    }

    public static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();

        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        String extension = inputData.getFile().getExtension();
        if(extension == null || !(extension.equalsIgnoreCase("xml") || extension.equalsIgnoreCase("yml")|| extension.equalsIgnoreCase("yaml"))) {
            return false;
        }

        if(inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
            return false;
        }

        return true;
    }
}



