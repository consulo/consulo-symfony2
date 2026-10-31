package fr.adrienbrault.idea.symfony2plugin.config;

import consulo.application.util.NotNullLazyValue;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.utils.ConfigUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ConfigLineMarkerUtil {
    private ConfigLineMarkerUtil() {
    }

    @Nullable
    public static LineMarkerInfo<PsiElement> createConfigNavigationMarker(Project project, PsiElement psiElement, LazyConfigTreeSignatures signatures, String key) {
        Map<String, Collection<String>> treeSignatures = signatures.value();
        if (!treeSignatures.containsKey(key)) {
            return null;
        }

        return NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(NotNullLazyValue.createValue(new ConfigTargetSupplier(project, treeSignatures.get(key), key)))
            .setTooltipText("Navigate to configuration")
            .createLineMarkerInfo(psiElement);
    }

    public static final class LazyConfigTreeSignatures {
        private final Project project;

        private Map<String, Collection<String>> treeSignatures;

        public LazyConfigTreeSignatures(Project project) {
            this.project = project;
        }

        public Map<String, Collection<String>> value() {
            return Objects.requireNonNullElseGet(
                this.treeSignatures,
                () -> this.treeSignatures = ConfigUtil.getTreeSignatures(project)
            );
        }
    }

    private record ConfigTargetSupplier(Project project, Collection<String> configuration, String root) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return ConfigUtil.getTreeSignatureTargets(project, root, configuration);
        }
    }
}
