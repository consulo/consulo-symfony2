package fr.adrienbrault.idea.symfony2plugin.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.editor.completion.lookup.InsertHandler;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.project.Project;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import consulo.util.lang.Pair;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(order = "after PhpCompletionContributor") // upstream order="last"
public class PhpIncompleteCompletionContributor extends CompletionContributor {
    public PhpIncompleteCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider() {
            @Override
            public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                PsiElement parent;
                PsiElement prevSibling = completionParameters.getPosition().getPrevSibling();
                if (prevSibling != null && prevSibling.getNode().getElementType() == PhpTokenTypes.ARROW) {
                    // $foo->
                    parent = prevSibling.getParent();
                } else {
                    // $foo->ad
                    parent = psiElement.getParent();
                }

                if (parent instanceof FieldReference fieldReference) {
                    PhpExpression classReference = fieldReference.getClassReference();
                    if (classReference != null) {
                        Project project = psiElement.getProject();
                        if (PhpIndex.getInstance(project).completeType(project, classReference.getType(), new HashSet<>()).getTypes().stream().anyMatch(s -> s.contains("\\Symfony\\Component\\Form\\FormBuilderInterface"))) {
                            PhpClass phpClass = FormOptionsUtil.getFormPhpClassFromContext(psiElement);
                            if (phpClass == null) {
                                return;
                            }

                            Set<String> alreadyKnownFields = new HashSet<>();
                            String formTypeClassFromScope = FormUtil.getFormTypeClassFromScope(parent);
                            if (formTypeClassFromScope != null) {
                                PhpClass clazz = PhpElementsUtil.getClassInterface(project, formTypeClassFromScope);
                                if (clazz != null) {
                                    FormFieldResolver.visitFormReferencesFields(clazz, twigTypeContainer -> alreadyKnownFields.add(twigTypeContainer.getStringElement()));
                                }
                            }

                            FormUnderscoreMethodReference.visitPropertyPath(
                                phpClass,
                                pair -> completionResultSet.addElement(new MyLookupElement(
                                    pair.getFirst(),
                                    "add",
                                    pair.getSecond(),
                                    phpClass.getName(),
                                    alreadyKnownFields.contains(pair.getFirst())
                                ))
                            );
                        }
                    }
                }
            }
        });
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }

    private class MyInsertHandler implements InsertHandler<consulo.language.editor.completion.lookup.LookupElement> {
        public void handleInsert(InsertionContext context, consulo.language.editor.completion.lookup.LookupElement item) {
            context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
            context.commitDocument();

            if (!(item instanceof MyLookupElement anonymousFunctionMyLookupElement)) {
                return;
            }

            PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
            if (elementAt == null) {
                return;
            }

            Pair<String, Map<String, String>> guessedFormFieldParameters = FormUtil.getGuessedFormFieldParameters(
                PhpIndex.getInstance(context.getProject()),
                context.getProject(), ((MyLookupElement) item).getKey(),
                ((MyLookupElement) item).getPhpNamedElement()
            );

            String content = "";

            String typeClass = guessedFormFieldParameters.getFirst();
            Map<String, String> options = guessedFormFieldParameters.getSecond();

            content += "add('%s'".formatted(anonymousFunctionMyLookupElement.getKey());

            if (typeClass != null) {
                typeClass = PhpElementsUtil.insertUseIfNecessary(elementAt, typeClass);
                content += ", " + typeClass + "::class";
            }

            Set<Map.Entry<String, String>> entries = options.entrySet();
            if (!entries.isEmpty()) {
                content += ", [";

                List<String> opts = new ArrayList<>();
                for (Map.Entry<String, String> entry : entries) {
                    if (entry.getKey().equals("class")) {
                        String classUse = PhpElementsUtil.insertUseIfNecessary(elementAt, entry.getValue());
                        opts.add("'%s' => %s::class".formatted(entry.getKey(), classUse));
                    } else {
                        opts.add("'%s' => %s".formatted(entry.getKey(), entry.getValue()));
                    }
                }

                content += StringUtils.join(opts, ", ");

                content += "]";
            }

            content += ");\n";

            Project project = context.getProject();
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(context.getDocument());

            int caretModel = context.getEditor().getCaretModel().getOffset();

            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), content);
            CodeStyleManager.getInstance(project).reformatText(context.getFile(), caretModel, caretModel + content.length());
        }
    }

    private class MyLookupElement extends consulo.language.editor.completion.lookup.LookupElement {
        private final String key;
        private final String lookupElement;

        private final PhpNamedElement phpNamedElement;
        private final String typeText;
        private final boolean exists;

        public MyLookupElement(String key, String lookupElement, PhpNamedElement phpNamedElement, String typeText, boolean exists) {
            this.key = key;
            this.lookupElement = lookupElement;
            this.phpNamedElement = phpNamedElement;
            this.typeText = typeText;
            this.exists = exists;
        }

        @Override
        public void renderElement(LookupElementPresentation presentation) {
            super.renderElement(presentation);

            if (this.exists) {
                presentation.setTypeText(typeText, Symfony2Icons.SYMFONY_AI_OPACITY);
                presentation.setTypeGrayed(true);
            } else {
                presentation.setTypeText(typeText, Symfony2Icons.SYMFONY_AI);
            }

            presentation.setIcon(IconDescriptorUpdaters.getIcon(phpNamedElement, 0));
            presentation.setTypeIconRightAligned(true);
        }

        @Override
        public String getLookupString() {
            return lookupElement + "('" + getKey() + "')";
        }

        @Override
        public void handleInsert(InsertionContext context) {
            new MyInsertHandler().handleInsert(context, this);
        }

        public String getKey() {
            return key;
        }

        public PhpNamedElement getPhpNamedElement() {
            return phpNamedElement;
        }
    }
}
