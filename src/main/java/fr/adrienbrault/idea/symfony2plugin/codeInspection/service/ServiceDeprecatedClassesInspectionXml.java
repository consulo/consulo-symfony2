package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.language.XMLLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * consulo-port: upstream nested class ServiceDeprecatedClassesInspection$ServiceDeprecatedClassesInspectionXml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceDeprecatedClassesInspectionXml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyXmlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ServiceDeprecatedClassesInspectionXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Deprecated Class in XML");
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
        return XMLLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyXmlPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private final Supplier<ContainerCollectionResolver.LazyServiceCollector> myServiceCollector;
        private final Supplier<ElementPattern<?>> myArgumentServiceIdPattern;
        private final Supplier<ElementPattern<?>> myServiceClassAttributePattern;

        private MyXmlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            myArgumentServiceIdPattern = LazyValue.notNull(() -> XmlHelper.getArgumentServiceIdPattern());
            myServiceClassAttributePattern = LazyValue.notNull(() -> XmlHelper.getServiceClassAttributeWithIdPattern());
        }

        @Override
        public void visitElement(PsiElement element) {
            visitXmlElement(element);
            super.visitElement(element);
        }

        private void visitXmlElement(PsiElement element) {
            boolean serviceArgumentAccepted = myArgumentServiceIdPattern.get().accepts(element);

            if (serviceArgumentAccepted || myServiceClassAttributePattern.get().accepts(element)) {
                String text = PsiElementUtils.trimQuote(element.getText());
                PsiElement[] psiElements = element.getChildren();

                // we need to attach to child because else strike out equal and quote char
                if (StringUtils.isNotBlank(text) && psiElements.length > 2) {
                    ServiceDeprecatedClassesInspection.ProblemRegistrar.attachDeprecatedProblem(psiElements[1], text, myHolder, myServiceCollector);

                    // check service arguments for "deprecated" defs
                    if (serviceArgumentAccepted) {
                        ServiceDeprecatedClassesInspection.ProblemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, myHolder, myServiceCollector);
                    }
                }
            }
        }
    }
}
