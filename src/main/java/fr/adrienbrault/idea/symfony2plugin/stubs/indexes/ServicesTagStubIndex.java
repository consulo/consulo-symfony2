package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.xml.language.psi.XmlFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServicesTagStubIndex extends FileBasedIndexExtension<String, Set<String>> {

    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_tags");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {

        return inputData -> {

            Map<String, Set<String>> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if (!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                map.putAll(FormUtil.getTags((YAMLFile) psiFile));
            }

            if(psiFile instanceof XmlFile) {
                map.putAll(FormUtil.getTags((XmlFile) psiFile));
            }

            return map;
        };
    }

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<Set<String>> getValueExternalizer() {
        return new StringSetDataExternalizer();
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

}
