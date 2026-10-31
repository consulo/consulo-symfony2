package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.jetbrains.php.codeInsight.PhpCodeInsightUtil;
import com.jetbrains.php.lang.psi.PhpGroupUseElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.refactoring.PhpAliasImporter;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationUseImporter {

    public static void insertUse(InsertionContext context, String fqnAnnotation) {
        PsiElement element = PsiUtilCore.getElementAtOffset(context.getFile(), context.getStartOffset());
        PhpPsiElement scopeForUseOperator = PhpCodeInsightUtil.findScopeForUseOperator(element);

        if(null == scopeForUseOperator) {
            return;
        }

        // PhpCodeInsightUtil.canImport:
        // copied from PhpReferenceInsertHandler; throws an error on PhpContractUtil because of "fully qualified names only"
        // but that is catch on phpstorm side already; looks fixed now so use fqn

        if(!fqnAnnotation.startsWith("\\")) {
            fqnAnnotation = "\\" + fqnAnnotation;
        }

        // this looks suitable! :)
        if(PhpCodeInsightUtil.findImportedName(scopeForUseOperator, fqnAnnotation, PhpGroupUseElement.PhpUseKeyword.CLASS) == null) {
            Project project = context.getProject();

            PsiDocumentManager.getInstance(project).commitDocument(context.getDocument());
            PhpAliasImporter.insertUseStatement(fqnAnnotation, scopeForUseOperator);
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(context.getDocument());
        }
    }
}
