package fr.adrienbrault.idea.symfony2plugin.completion;

import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.document.Document;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.Objects;

/**
 * Insert handler for Doctrine ORM lifecycle callback attributes that also adds
 * the #[HasLifecycleCallbacks] attribute to the class if not already present.
 *
 * This handler delegates to PhpDoctrineAttributeInsertHandler for the initial
 * attribute insertion, then ensures the class-level HasLifecycleCallbacks attribute
 * is present (required for lifecycle callbacks to work in Doctrine).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record PhpDoctrineLifecycleAttributeInsertHandler(
    String attributeFqn,
    String shortClassName,
    PhpDoctrineAttributeInsertHandler delegate
) implements InsertHandler<LookupElement> {

    private static final String DOCTRINE_LIFECYCLE_CLASS = "\\Doctrine\\ORM\\Mapping\\HasLifecycleCallbacks";

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        // IMPORTANT: Find the target field BEFORE modifying the document
        int startOffset = context.getStartOffset();
        PsiFile file = context.getFile();
        PsiElement originalElement = file.findElementAt(startOffset);
        if (originalElement == null) {
            return;
        }

        PhpNamedElement validAttributeScope = PhpAttributeScopeValidator.getValidAttributeScope(originalElement);

        delegate.handleInsert(context, item);

        // Determine the target context (field, method, or class) dynamically using the shared scope validator
        if (!(validAttributeScope instanceof Field) && !(validAttributeScope instanceof PhpClass) && !(validAttributeScope instanceof Method)) {
            return;
        }

        PhpClass phpClass = validAttributeScope instanceof PhpClass
            ? (PhpClass) validAttributeScope
            : Objects.requireNonNull(PsiTreeUtil.getParentOfType(validAttributeScope, PhpClass.class));

        if (!phpClass.getAttributes(DOCTRINE_LIFECYCLE_CLASS).isEmpty()) {
            return;
        }

        // Determine which alias to use (or null for direct class import)
        String importedAlias = PhpDoctrineAttributeInsertHandler.findAliasInEntitiesScope(phpClass);

        String attributeText = DOCTRINE_LIFECYCLE_CLASS.substring(DOCTRINE_LIFECYCLE_CLASS.lastIndexOf("\\") + 1);

        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(validAttributeScope);
        if (scopeForUseOperator != null) {
            if (importedAlias != null) {
                PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, DOCTRINE_LIFECYCLE_CLASS, importedAlias);
                attributeText = importedAlias + "\\" + attributeText;
            } else {
                String s = PhpElementsUtil.insertUseIfNecessary(scopeForUseOperator, DOCTRINE_LIFECYCLE_CLASS);
                if (s != null) {
                    attributeText = s;
                }
            }
        }

        addAttributeToMethod(file.getProject(), phpClass, "#[" + attributeText  + "]");
    }

    private void addAttributeToMethod(Project project, PhpClass phpClass, String attributeText) {
        // Insert attribute text directly before the method
        PsiFile file = phpClass.getContainingFile();
        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        PsiDocumentManager psiDocManager = PsiDocumentManager.getInstance(project);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        int methodStartOffset = phpClass.getTextRange().getStartOffset();
        String fullAttributeText = attributeText + "\n";

        document.insertString(methodStartOffset, fullAttributeText);
        psiDocManager.commitDocument(document);
        psiDocManager.doPostponedOperationsAndUnblockDocument(document);

        // Reformat the added attribute with proper indentation
        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset);
    }
}
