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
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpCallInstruction;
import com.jetbrains.php.lang.documentation.phpdoc.PhpDocUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpConstantNameIndex;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.TemplateUsageExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import consulo.util.lang.Trinity; // TODO consulo-port: Trinity comes from the Kotlin stdlib (shared with PhpMethodVariableResolveUtil); replace together once a Java tuple type is chosen
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpTwigTemplateUsageStubIndex extends FileBasedIndexExtension<String, TemplateUsage> {

    public static final ID<String, TemplateUsage> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_php_usage");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final int MAX_FILE_BYTE_SIZE = 2097152;
    private static final TemplateUsageExternalizer EXTERNALIZER = TemplateUsageExternalizer.INSTANCE;

    @Override
    public ID<String, TemplateUsage> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, TemplateUsage, FileContent> getIndexer() {
        return inputData -> {
            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return Collections.emptyMap();
            }

            if(!(inputData.getPsiFile() instanceof PhpFile) && isValidForIndex(inputData)) {
                return Collections.emptyMap();
            }

            Map<String, Set<String>> items = new HashMap<>();
            Consumer<Trinity<String, PhpNamedElement, FunctionReference>> consumer = triple -> {
                String templateName = triple.getFirst();

                if (templateName.length() > 255) {
                    return;
                }

                if(!items.containsKey(templateName)) {
                    items.put(templateName, new HashSet<>());
                }

                items.get(templateName).add(StringUtils.stripStart(triple.getSecond().getFQN(), "\\"));
            };

            for (PhpNamedElement topLevelElement : ((PhpFile) psiFile).getTopLevelDefs().values()) {
                if (topLevelElement instanceof PhpClass clazz) {
                    for (Method method : clazz.getOwnMethods()) {
                        PhpMethodVariableResolveUtil.TemplateRenderVisitor.processMethodAttributes(method, consumer);
                        PhpDocComment docComment = method.getDocComment();
                        if (docComment != null) {
                            Map<String, String> fileImports = AnnotationBackportUtil.getUseImportMap(docComment);
                            PhpDocUtil.processTagElementsByPredicate(
                                docComment,
                                docTag -> PhpMethodVariableResolveUtil.TemplateRenderVisitor.processDocTag(docTag, fileImports, consumer),
                                phpDocTag -> true
                            );
                        }
                        processMethodReferences(consumer, method);
                    }
                }
                if (topLevelElement instanceof Function function) {
                    processMethodReferences(consumer, function);
                }
            }

            Map<String, TemplateUsage> map = new HashMap<>();

            items.forEach(
                (key, value) -> map.put(key, new TemplateUsage(key, value))
            );

            return map;
        };
    }

    private static void processMethodReferences(Consumer<Trinity<String, PhpNamedElement, FunctionReference>> consumer, Function function) {
        PhpControlFlowUtil.processFlow(function.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processPhpCallInstruction(PhpCallInstruction instruction) {
                if (instruction.getFunctionReference() instanceof MethodReference methodReference) {
                    PhpMethodVariableResolveUtil.TemplateRenderVisitor.processMethodReference(methodReference, consumer);
                }
                return super.processPhpCallInstruction(instruction);
            }
        });
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<TemplateUsage> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return PhpConstantNameIndex.PHP_INPUT_FILTER;
    }


    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 5;
    }

    private static boolean isValidForIndex(FileContent inputData) {
        return inputData.getFile().getLength() < MAX_FILE_BYTE_SIZE;
    }
}
