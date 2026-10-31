package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.xml.language.psi.XmlDocument;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorIntegerDescriptor;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ContainerIdUsagesStubIndex extends FileBasedIndexExtension<String, Integer> {

    public static final ID<String, Integer> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.service_id_usage");

    @Override
    public ID<String, Integer> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Integer, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Integer> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(!ServicesDefinitionStubIndex.isValidForIndex(inputData, psiFile)) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                map.putAll(getIdUsages((YAMLFile) psiFile));
            } else if(psiFile instanceof XmlFile) {
                map.putAll(getIdUsages((XmlFile) psiFile));
            }

            return map;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public DataExternalizer<Integer> getValueExternalizer() {
        return EnumeratorIntegerDescriptor.INSTANCE;
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
        return 1;
    }

    private static Map<String, Integer> getIdUsages(YAMLFile yamlFile) {
        Map<String, Integer> services = new HashMap<>();

        for(YAMLKeyValue yamlKeyValue: YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            String keyName = yamlKeyValue.getKeyText();
            if(StringUtils.isBlank(keyName)) {
                continue;
            }

            YAMLKeyValue arguments = YamlHelper.getYamlKeyValue(yamlKeyValue, "arguments");
            if(arguments != null) {
                YAMLValue value = arguments.getValue();
                if(value instanceof YAMLSequence) {
                    for (String id : YamlHelper.getYamlArrayValuesAsList((YAMLSequence) value)) {
                        String idClean = YamlHelper.trimSpecialSyntaxServiceName(id);
                        if(StringUtils.isNotBlank(idClean)) {
                            services.putIfAbsent(idClean, 0);
                            services.put(idClean, services.get(idClean) + 1);
                        }
                    }
                }
            }

            // calls:
            //  - [foo, [@bar, @bar]]
            YAMLKeyValue calls = YamlHelper.getYamlKeyValue(yamlKeyValue, "calls");
            if(calls != null) {

                for (YAMLPsiElement yamlPsiElement : calls.getYAMLElements()) {
                    if(yamlPsiElement instanceof YAMLSequence) {
                        for (YAMLSequenceItem yamlSequenceItem : ((YAMLSequence) yamlPsiElement).getItems()) {
                            YAMLValue value = yamlSequenceItem.getValue();
                            if(value instanceof YAMLSequence) {
                                List<YAMLSequenceItem> callItem = ((YAMLSequence) value).getItems();
                                if(callItem.size() > 1) {
                                    // [foo, [@bar, @bar2]]

                                    // get arguments: [@bar, @bar2]
                                    YAMLValue methodArguments = callItem.get(1).getValue();
                                    if(methodArguments instanceof YAMLSequence) {
                                        // visit arguments in array: [@bar, @bar2]

                                        for (YAMLSequenceItem methodArgument : ((YAMLSequence) methodArguments).getItems()) {
                                            YAMLValue value2 = methodArgument.getValue();

                                            // @bar
                                            if(value2 instanceof YAMLScalar) {
                                                String textValue = ((YAMLScalar) value2).getTextValue();

                                                String idClean = YamlHelper.trimSpecialSyntaxServiceName(textValue);
                                                if(StringUtils.isNotBlank(idClean)) {
                                                    services.putIfAbsent(idClean, 0);
                                                    services.put(idClean, services.get(idClean) + 1);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return services;
    }

    private static Map<String, Integer> getIdUsages(XmlFile psiFile) {
        PsiElement firstChild = psiFile.getFirstChild();
        if(!(firstChild instanceof XmlDocument)) {
            return Collections.emptyMap();
        }

        XmlTag rootTag = ((XmlDocument) firstChild).getRootTag();
        if(rootTag == null || !"container".equals(rootTag.getName())) {
            return Collections.emptyMap();
        }

        Map<String, Integer> services = new HashMap<>();

        for (XmlTag xmlTag : rootTag.findSubTags("services")) {
            for (XmlTag service : xmlTag.findSubTags("service")) {
                for (XmlTag argument : service.findSubTags("argument")) {
                    if("service".equalsIgnoreCase(argument.getAttributeValue("type"))) {
                        String id = argument.getAttributeValue("id");
                        if(id != null && StringUtils.isNotBlank(id)) {
                            services.putIfAbsent(id, 0);
                            services.put(id, services.get(id) + 1);
                        }
                    }
                }

                // <call method="setFoo2">
                //   <argument type="service" id="xml_setter_foobar_3" />
                //   <argument type="service" id="xml_setter_foobar_1" />
                // </call>
                for (XmlTag calls : service.findSubTags("call")) {
                    for (XmlTag argument : calls.findSubTags("argument")) {
                        if(!"service".equalsIgnoreCase(argument.getAttributeValue("type"))) {
                            continue;
                        }

                        String id = argument.getAttributeValue("id");
                        if(StringUtils.isBlank(id)) {
                            continue;
                        }

                        services.putIfAbsent(id, 0);
                        services.put(id, services.get(id) + 1);
                    }
                }
            }
        }

        return services;
    }
}