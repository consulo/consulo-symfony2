package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.NotNullLazyValue;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpSerializerLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> lineMarkerInfos) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        var classNamePattern = PhpElementsUtil.getClassNamePattern();

        for(PsiElement psiElement : psiElements) {
            if (psiElement.getNode().getElementType() == PhpTokenTypes.IDENTIFIER && classNamePattern.accepts(psiElement)) {
                attachSerializerActions(lineMarkerInfos, psiElement);
            }
        }
    }

    private void attachSerializerActions(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        String fqn = ((PhpClass) phpClassContext).getFQN();

        Project project = leaf.getProject();
        if (SerializerUtil.hasClassTargetForSerializer(project, fqn)) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTooltipText("Navigate to Serializer")
                .setTargets(NotNullLazyValue.createValue(() -> SerializerUtil.getClassTargetForSerializer(project, fqn)));

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
