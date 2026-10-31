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
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.HashMap;
import java.util.Map;

@ExtensionImpl
public class ContainerParameterStubIndex extends FileBasedIndexExtension<String, String> {

    public static final ID<String, String> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.parameter2");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();

    @Override
    public ID<String, String> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, String, FileContent> getIndexer() {
        return inputData -> {
            Map<String, String> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                attachTHashMapNullable(YamlHelper.getLocalParameterMap(psiFile), map);
            } else if(psiFile instanceof XmlFile) {
                attachTHashMapNullable(XmlHelper.getFileParameterMap((XmlFile) psiFile), map);
            }

            return map;
        };
    }

    /**
     * workaround for nullable keys #238
     */
    private void attachTHashMapNullable(Map<String, String> source, Map<String, String> tHashMap) {
        for(Map.Entry<String, String> entry: source.entrySet()) {

            // dont index null values
            String value = entry.getValue();
            if(value == null) {
                value = "";
            }

            tHashMap.put(entry.getKey(), value);
        }
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<String> getValueExternalizer() {
        return EnumeratorStringDescriptor.INSTANCE;
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
        return 3;
    }
}



