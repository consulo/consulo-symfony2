package fr.adrienbrault.idea.symfony2plugin.completion;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionConfidence;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.ThreeState;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl(id = "asTrue") // upstream order="first": plain first is rejected for plugins; no PHP confidence to precede
public class PhpParameterStringCompletionConfidence extends CompletionConfidence {

    @Override
    public ThreeState shouldSkipAutopopup(PsiElement contextElement, PsiFile psiFile, int offset) {
        if (!Symfony2ProjectComponent.isEnabled(contextElement)) {
            return ThreeState.UNSURE;
        }

        if(!(psiFile instanceof PhpFile)) {
            return ThreeState.UNSURE;
        }

        PsiElement context = contextElement.getContext();
        if(!(context instanceof StringLiteralExpression)) {
            return ThreeState.UNSURE;
        }

        // $test == "";
        if(context.getParent() instanceof BinaryExpression) {
            return ThreeState.NO;
        }

        // $this->container->get("");
        PsiElement stringContext = context.getContext();
        if(stringContext instanceof ParameterList) {
            return ThreeState.NO;
        }

        // $this->method(... array('foo'); array('bar' => 'foo') ...);
        ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(context);
        if(arrayCreationExpression != null && arrayCreationExpression.getContext() instanceof ParameterList) {
            return ThreeState.NO;
        }

        // $array['value']
        if(PlatformPatterns.psiElement().withSuperParent(2, ArrayIndex.class).accepts(contextElement)) {
            return ThreeState.NO;
        }

        return ThreeState.UNSURE;
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
