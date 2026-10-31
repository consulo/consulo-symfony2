package fr.adrienbrault.idea.symfony2plugin.config;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.NotNullLazyValue;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLValue;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ConfigLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> result) {
        if(psiElements.isEmpty()) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if (!Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        ConfigLineMarkerUtil.LazyConfigTreeSignatures function = null;
        var rootConfigKeyPattern = YamlElementPatternHelper.getRootConfigKeyPattern();

        var resourcePattern = YamlElementPatternHelper.getSingleLineScalarKey("resource");

        for (PsiElement psiElement : psiElements) {
            if(psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && rootConfigKeyPattern.accepts(psiElement)) {
                if(function == null) {
                    function = new ConfigLineMarkerUtil.LazyConfigTreeSignatures(project);
                }
                visitRootElements(project, result, psiElement, function);
            } else if (resourcePattern.accepts(psiElement)) {
                visitResourceValue(project, result, psiElement);
            }
        }
    }

    private void visitRootElements(Project project, Collection<LineMarkerInfo> result, PsiElement psiElement, ConfigLineMarkerUtil.LazyConfigTreeSignatures function) {
        PsiElement parent = psiElement.getParent();
        if(!(parent instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) parent).getKeyText();

        // nested condition:
        // when@prod:
        //    framework:
        //        router: ~
        if (keyText.startsWith("when@")) {
            YAMLValue value = ((YAMLKeyValue) parent).getValue();

            for (YAMLKeyValue yamlKeyValue : PsiTreeUtil.getChildrenOfTypeAsList(value, YAMLKeyValue.class)) {
                String keyText1 = yamlKeyValue.getKeyText();
                if (StringUtils.isNotBlank(keyText1)) {
                    PsiElement key = yamlKeyValue.getKey();
                    if (key != null) {
                        visitConfigKey(project, result, key, function, keyText1);
                    }
                }
            }

            return;
        }

        visitConfigKey(project, result, psiElement, function, keyText);
    }

    private void visitConfigKey(Project project, Collection<LineMarkerInfo> result, PsiElement psiElement, ConfigLineMarkerUtil.LazyConfigTreeSignatures function, String keyText) {
        LineMarkerInfo<PsiElement> marker = ConfigLineMarkerUtil.createConfigNavigationMarker(project, psiElement, function, keyText);
        if (marker != null) {
            result.add(marker);
        }
    }

    private void visitResourceValue(Project project, Collection<LineMarkerInfo> result, PsiElement psiElement) {
        String resourcePath = PsiElementUtils.trimQuote(psiElement.getText());

        if (!FileResourceUtil.hasFileResourceTargets(project, psiElement.getContainingFile(), resourcePath)) {
            return;
        }

        result.add(NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(NotNullLazyValue.createValue(() -> FileResourceUtil.getFileResourceTargets(project, psiElement.getContainingFile(), resourcePath)))
            .setTooltipText("Navigate to resource")
            .createLineMarkerInfo(psiElement));
    }

    @Override
    public Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }
}
