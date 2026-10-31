package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlServiceArgumentInspection extends LocalInspectionTool {
    public static final String[] INVALID_KEYS = new String[]{
        "parent", "factory_class", "factory_service",
        "factory_method", "abstract", "factory",
        "resource", "exclude", "alias"
    };

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "YamlServiceArgumentInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: Yaml Arguments");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Service");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myProblemsHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;

        private MyPsiElementVisitor(ProblemsHolder problemsHolder) {
            myProblemsHolder = problemsHolder;
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector() {
            if (myLazyServiceCollector == null) {
                myLazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(myProblemsHolder.getProject());
            }
            return myLazyServiceCollector;
        }

        @Override
        public void visitElement(PsiElement element) {
            PsiElement parent = element.getParent();
            YAMLKeyValue servicesKey = parent instanceof YAMLMapping && parent.getParent() instanceof YAMLKeyValue keyValue ? keyValue : null;
            if (element instanceof YAMLKeyValue yamlKeyValue && servicesKey != null && "services".equals(servicesKey.getKeyText())) {
                // we don't support parent services for now
                if (!yamlKeyValue.getKeyText().equalsIgnoreCase("_defaults") && isValidService(yamlKeyValue)) {
                    ServiceActionUtil.ServiceYamlContainer container = ServiceActionUtil.ServiceYamlContainer.create(yamlKeyValue);
                    if (container != null) {
                        List<String> yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(
                            myProblemsHolder.getProject(),
                            container,
                            false,
                            getLazyServiceCollector()
                        );

                        if (!yamlMissingArgumentTypes.isEmpty()) {
                            myProblemsHolder.registerProblem(yamlKeyValue.getFirstChild(), "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new YamlArgumentQuickfix());
                        }
                    }
                }
            }

            super.visitElement(element);
        }

        private boolean isValidService(YAMLKeyValue serviceKey) {
            Set<String> keySet = YamlHelper.getKeySet(serviceKey);
            if (keySet == null) {
                return true;
            }

            for (String invalidKey : INVALID_KEYS) {
                if (keySet.contains(invalidKey)) {
                    return false;
                }
            }

            // check autowire scope
            Boolean serviceAutowire = YamlHelper.getYamlKeyValueAsBoolean(serviceKey, "autowire");
            if (serviceAutowire != null) {
                // use service scope for autowire
                return !serviceAutowire;
            }

            // find file scope defaults: defaults: [autowire: true]
            YAMLMapping parentMapping = serviceKey.getParentMapping();
            YAMLKeyValue defaults = parentMapping != null ? YamlHelper.getYamlKeyValue(parentMapping, "_defaults") : null;
            if (defaults == null) {
                return true;
            }
            return !Boolean.TRUE.equals(YamlHelper.getYamlKeyValueAsBoolean(defaults, "autowire"));
        }
    }

    private static class YamlArgumentQuickfix implements LocalQuickFix {
        @Override
        public LocalizeValue getName() {
            return LocalizeValue.localizeTODO("Symfony: Yaml Argument");
        }

        @Override
        public void applyFix(Project project, ProblemDescriptor problemDescriptor) {
            PsiElement serviceKeyValue = problemDescriptor.getPsiElement().getParent();

            if (serviceKeyValue instanceof YAMLKeyValue yamlKeyValue) {
                ServiceActionUtil.fixServiceArgument(yamlKeyValue);
            }
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }
    }
}
