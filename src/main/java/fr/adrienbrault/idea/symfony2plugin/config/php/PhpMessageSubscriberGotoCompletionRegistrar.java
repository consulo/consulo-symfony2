package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PatternCondition;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * \Symfony\Component\Messenger\Handler\MessageSubscriberInterface::getHandledMessages
 *
 * yield FirstMessage::class => ['method' => '<caret>']
 *
 * return array(
 *   FirstMessage::class => '<caret>',
 * )
 *
 * @author Stefano Arlandini <sarlandini@alice.it>
 */
@ExtensionImpl
public class PhpMessageSubscriberGotoCompletionRegistrar implements GotoCompletionRegistrar {
    private static final PsiElementPattern.Capture<PsiElement> REGISTRAR_PATTERN = PlatformPatterns.psiElement()
        .withLanguage(PhpLanguage.INSTANCE)
        .withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.psiElement()
                    .withElementType(PhpElementTypes.ARRAY_VALUE)
                    .withParent(
                        PlatformPatterns.or(
                            PlatformPatterns.psiElement(ArrayHashElement.class)
                                .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class).withParent(PhpReturn.class))
                                .with(new PatternCondition<>("Key Type") {
                                    @Override
                                    public boolean accepts(ArrayHashElement arrayHashElement, ProcessingContext context) {
                                        PhpPsiElement keyElement = arrayHashElement.getKey();

                                        return keyElement instanceof StringLiteralExpression || keyElement instanceof ClassConstantReference;
                                    }
                                }),
                            PlatformPatterns.psiElement(ArrayHashElement.class)
                                .with(new PatternCondition<>("Key Text") {
                                    @Override
                                    public boolean accepts(ArrayHashElement arrayHashElement, ProcessingContext context) {
                                        PhpPsiElement keyElement = arrayHashElement.getKey();

                                        if (!(keyElement instanceof StringLiteralExpression)) {
                                            return false;
                                        }

                                        return ((StringLiteralExpression) keyElement).getContents().equals("method");
                                    }
                                })
                                .withParent(
                                    PlatformPatterns.psiElement(ArrayCreationExpression.class).withParent(
                                        PlatformPatterns.psiElement(PhpYield.class)
                                            .with(new PatternCondition<>("Yield Key Type") {
                                                @Override
                                                public boolean accepts(PhpYield phpYield, ProcessingContext context) {
                                                    PsiElement keyElement = phpYield.getArgument();

                                                    return keyElement instanceof StringLiteralExpression || keyElement instanceof ClassConstantReference;
                                                }
                                            })
                                    )
                                )
                        )
                    )
            )
        );

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(REGISTRAR_PATTERN, psiElement -> {
            Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class, true, Function.class);

            if (method == null) {
                return null;
            }

            if (!PhpElementsUtil.isMethodInstanceOf(method, "\\Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface", "getHandledMessages")) {
                return null;
            }

            return new PhpClassPublicMethodProvider(method.getContainingClass());
        });
    }

    private static class PhpClassPublicMethodProvider extends GotoCompletionProvider {
        private final PhpClass phpClass;

        public PhpClassPublicMethodProvider(PhpClass phpClass) {
            super(phpClass);

            this.phpClass = phpClass;
        }

        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> elements = new ArrayList<>();

            for (Method method : phpClass.getMethods()) {
                if (!method.getAccess().isPublic()) {
                    continue;
                }

                String methodName = method.getName();

                if (methodName.equals("getHandledMessages") || methodName.startsWith("__")) {
                    continue;
                }

                elements.add(LookupElementBuilder.createWithIcon(method));
            }

            return elements;
        }

        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            PsiElement parentElement = element.getParent();

            if (!(parentElement instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String contents = ((StringLiteralExpression) parentElement).getContents();

            if (StringUtils.isBlank(contents)) {
                return Collections.emptyList();
            }

            Method method = phpClass.findMethodByName(contents);

            if (method != null) {
                return Collections.singletonList(method);
            }

            return Collections.emptyList();
        }
    }
}
