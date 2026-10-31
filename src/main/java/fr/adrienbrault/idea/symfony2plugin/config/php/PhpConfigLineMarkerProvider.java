package fr.adrienbrault.idea.symfony2plugin.config.php;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
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
import fr.adrienbrault.idea.symfony2plugin.config.ConfigLineMarkerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Provides Symfony config line markers for PHP config files.
 *
 * Markers are added for:
 * - root config keys: {@code 'framework' => [...]}
 * - conditional root config keys: {@code 'when@prod' => ['framework' => [...]]}
 * - import resource values: {@code 'imports' => [['resource' => 'legacy_config.php']]}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.config.ConfigLineMarkerProvider
 */
@ExtensionImpl
public class PhpConfigLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> result) {
        if (psiElements.isEmpty()) {
            return;
        }

        if (!Symfony2ProjectComponent.isEnabled(psiElements.getFirst())) {
            return;
        }

        Project project = psiElements.getFirst().getProject();
        ConfigLineMarkerUtil.LazyConfigTreeSignatures lazySignatures = null;

        for (PsiElement psiElement : psiElements) {
            if (!(psiElement instanceof StringLiteralExpression stringLiteral)) {
                continue;
            }

            if (PhpConfigUtil.isRootConfigKey(stringLiteral) || PhpConfigUtil.isConditionalConfigKey(stringLiteral)) {
                String key = stringLiteral.getContents();
                if (StringUtils.isBlank(key) || key.startsWith("when@")) {
                    continue;
                }

                if (lazySignatures == null) {
                    lazySignatures = new ConfigLineMarkerUtil.LazyConfigTreeSignatures(project);
                }

                PsiElement leafTarget = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteral);
                if (leafTarget == null) {
                    continue;
                }

                LineMarkerInfo<PsiElement> marker = ConfigLineMarkerUtil.createConfigNavigationMarker(project, leafTarget, lazySignatures, key);
                if (marker != null) {
                    result.add(marker);
                }

            } else if (PhpConfigUtil.isImportResourceValue(stringLiteral)) {
                String resourcePath = stringLiteral.getContents();

                if (!FileResourceUtil.hasFileResourceTargets(project, stringLiteral.getContainingFile(), resourcePath)) {
                    continue;
                }

                PsiElement leafTarget = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(stringLiteral);
                if (leafTarget == null) {
                    continue;
                }

                result.add(NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                    .setTargets(NotNullLazyValue.createValue(() -> FileResourceUtil.getFileResourceTargets(project, stringLiteral.getContainingFile(), resourcePath)))
                    .setTooltipText("Navigate to resource")
                    .createLineMarkerInfo(leafTarget));
            }
        }
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
