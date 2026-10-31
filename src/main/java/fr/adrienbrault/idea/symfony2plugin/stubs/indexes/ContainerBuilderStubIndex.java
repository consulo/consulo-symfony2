package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ObjectUtil;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpInstruction;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpTypeDeclaration;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ContainerBuilderCall;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ContainerBuilderCallExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerBuilderStubIndex
 */
@ExtensionImpl
public class ContainerBuilderStubIndex extends FileBasedIndexExtension<String, ContainerBuilderCall> {

    public static final ID<String, ContainerBuilderCall> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.container_builder");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private final static ContainerBuilderCallExternalizer EXTERNALIZER = ContainerBuilderCallExternalizer.INSTANCE;

    private static final int MAX_FILE_BYTE_SIZE = 2621440;

    private static final Set<String> SET = new HashSet<>() {{
        add("Symfony\\Component\\DependencyInjection\\Container");
        add("Symfony\\Component\\DependencyInjection\\ContainerBuilder");
        add("Symfony\\Component\\DependencyInjection\\TaggedContainerInterface");
    }};

    private static final Set<String> METHODS = new HashSet<>() {{
        add("findTaggedServiceIds");
        add("setDefinition");
        add("setParameter");
        add("setAlias");
        add("register");
    }};

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, ContainerBuilderCall, FileContent> getIndexer() {

        return inputData -> {

            Map<String, ContainerBuilderCall> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!(psiFile instanceof PhpFile) ||
                !Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) ||
                !StubIndexValidationUtil.isValidForIndex(inputData, psiFile, MAX_FILE_BYTE_SIZE, true, null)
                ){

                return map;
            }

            PhpPsiUtil.findAllClasses((PhpFile) psiFile).stream()
                .flatMap(clazz -> Arrays.stream(clazz.getOwnMethods()))
                .forEach(method -> processMethod(method, map));
            return map;
        };
    }

    @Override
    public ID<String, ContainerBuilderCall> getName() {
        return KEY;
    }


    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    public DataExternalizer<ContainerBuilderCall> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, virtualFile) -> virtualFile.getFileType() == PhpFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 3;
    }

    private void processMethod(Method method, Map<String, ContainerBuilderCall> map) {
        Map<String, String> useImports = AnnotationBackportUtil.getUseImportMap(method);
        Set<CharSequence> containerParameters = Arrays.stream(method.getParameters())
            .filter(parameter -> isContainerParam(parameter, useImports))
            .map(Parameter::getNameCS)
            .collect(Collectors.toSet());
        if (containerParameters.isEmpty()) return;
        MyInstructionProcessor processor = new MyInstructionProcessor(map, containerParameters);
        for (PhpInstruction instruction : method.getControlFlow().getInstructions()) {
            instruction.process(processor);
        }
    }

    private static boolean isContainerParam(Parameter parameter, Map<String, String> useImports) {
        PhpTypeDeclaration typeDeclaration = parameter.getTypeDeclaration();
        if (typeDeclaration == null) {
            return false;
        }

        String parameterType = StringUtils.stripStart(typeDeclaration.getText(), "\\?");
        if (SET.contains(parameterType)) {
            return true;
        }

        String importedType = useImports.get(parameterType);
        return importedType != null && SET.contains(StringUtils.stripStart(importedType, "\\"));
    }

    private static class MyInstructionProcessor extends PhpInstructionProcessor {
        private final Map<String, ContainerBuilderCall> map;
        private final Set<CharSequence> containerParameters;

        MyInstructionProcessor(Map<String, ContainerBuilderCall> map,
                               Set<CharSequence> containerParameters) {
            this.map = map;
            this.containerParameters = containerParameters;
        }
        
        @Override
        public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
            if (instruction.getAccess().isWrite() || instruction.getAccess().isWriteRef() ||
                    !containerParameters.contains(instruction.getVariableName())) return true;
            
            MethodReference methodReference = 
                    ObjectUtil.tryCast(instruction.getAnchor().getParent(), MethodReference.class);
            if (methodReference == null || !METHODS.contains(methodReference.getName())) return true;
            
            String value = PhpElementsUtil.getFirstArgumentStringValue(methodReference);
            if (value == null) return true;
            
            String methodName = methodReference.getName();
            map.computeIfAbsent(methodName, name -> new ContainerBuilderCall());
            map.get(methodName).addParameter(value);
            
            return true;
        }
    }
}
