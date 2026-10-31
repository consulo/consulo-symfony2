package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
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
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelSerializable;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.DoctrineModelExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DoctrineMetadataFileStubIndex extends FileBasedIndexExtension<String, DoctrineModelSerializable> {

    public static final ID<String, DoctrineModelSerializable> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.doctrine_metadata");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final DoctrineModelExternalizer EXTERNALIZER = DoctrineModelExternalizer.INSTANCE;

    private static final int MAX_FILE_BYTE_SIZE = 1048576;

    private static class MyStringStringFileContentDataIndexer implements DataIndexer<String, DoctrineModelSerializable, FileContent> {
        @Override
        public Map<String, DoctrineModelSerializable> map(FileContent fileContent) {

            Map<String, DoctrineModelSerializable> map = new HashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !StubIndexValidationUtil.isValidForIndex(
                fileContent,
                psiFile,
                MAX_FILE_BYTE_SIZE,
                false,
                Set.of("xml", "yml", "yaml", "php")
            )) {
                return map;
            }

            Collection<DoctrineClassMetadata> classRepositoryPair = DoctrineUtil.getClassRepositoryPair(psiFile);
            if(classRepositoryPair == null || classRepositoryPair.isEmpty()) {
                return map;
            }

            for (DoctrineClassMetadata metadata : classRepositoryPair) {
                String className = metadata.className();
                if(className.isEmpty()) {
                    continue;
                }

                map.put(className, new DoctrineModel(className, metadata.repositoryClass(), metadata.tableName()));
            }

            return map;
        }
    }

    @Override
    public ID<String, DoctrineModelSerializable> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, DoctrineModelSerializable, FileContent> getIndexer() {
        return new MyStringStringFileContentDataIndexer();
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<DoctrineModelSerializable> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return FileInputFilter.XML_YAML_PHP;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 6;
    }

}
