package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor;

import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.lang.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "return ['key' => 'value1', 'key2' => 'value1']"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationArrayReturnVisitor {

    public static void visitPhpReturn(PhpFile phpFile, Consumer<Pair<String, PsiElement>> arrayKeyVisitor) {

        PhpInstructionProcessor processor = new PhpInstructionProcessor() {
            @Override
            public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                PsiElement argument = instruction.getArgument();
                if (argument instanceof ArrayCreationExpression arrayCreationExpression) {
                    collectConfigKeys(arrayCreationExpression, arrayKeyVisitor);
                }

                return super.processReturnInstruction(instruction);
            }
        };

        // toplevel "returns"
        PhpControlFlowUtil.processFlow(phpFile.getControlFlow(), processor);

        // toplevel "return" with namespace in file (for imports)
        for (PhpNamedElement value : phpFile.getTopLevelDefs().values()) {
            if (value instanceof PhpNamespace phpNamespace) {
                PhpControlFlowUtil.processFlow(phpNamespace.getControlFlow(), processor);
            }
        }
    }

    public static void collectConfigKeys(ArrayCreationExpression creationExpression, Consumer<Pair<String, PsiElement>> arrayKeyVisitor) {
        collectConfigKeys(creationExpression, arrayKeyVisitor, new ArrayList<>());
    }

    public static void collectConfigKeys(ArrayCreationExpression creationExpression, Consumer<Pair<String, PsiElement>> arrayKeyVisitor, List<String> context) {
        for(ArrayHashElement hashElement: PsiTreeUtil.getChildrenOfTypeAsList(creationExpression, ArrayHashElement.class)) {
            PsiElement arrayKey = hashElement.getKey();
            PsiElement arrayValue = hashElement.getValue();

            if(arrayKey instanceof StringLiteralExpression) {
                List<String> myContext = new ArrayList<>(context);
                myContext.add(((StringLiteralExpression) arrayKey).getContents());
                String keyName = StringUtils.join(myContext, ".");

                if(arrayValue instanceof ArrayCreationExpression) {
                    collectConfigKeys((ArrayCreationExpression) arrayValue, arrayKeyVisitor, myContext);
                } else {
                    arrayKeyVisitor.accept(new Pair<>(keyName, arrayKey));
                }
            }
        }
    }
}