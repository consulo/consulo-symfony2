package fr.adrienbrault.idea.symfony2plugin.codeInspection.service;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * consulo-port: upstream nested class TaggedExtendsInterfaceClassInspection$TaggedExtendsInterfaceClassInspectionXml
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TaggedExtendsInterfaceClassInspectionXml extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyXmlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "TaggedExtendsInterfaceClassXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Missing Tag extends/interface statement in XML");
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
        private final Supplier<ElementPattern<?>> myXmlServiceClassAttrPattern;
        private final Supplier<ElementPattern<?>> myXmlServiceIdAttrPattern;

        private MyXmlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            myXmlServiceClassAttrPattern = LazyValue.notNull(() -> XmlHelper.getServiceClassAttributeWithIdPattern());
            myXmlServiceIdAttrPattern = LazyValue.notNull(() -> XmlHelper.getServiceIdAttributePattern());
        }

        @Override
        public void visitElement(PsiElement element) {
            visitXmlElement(element);
            super.visitElement(element);
        }

        private void visitXmlElement(PsiElement element) {
            String className = TaggedExtendsInterfaceClassInspection.getClassNameFromServiceDefinition(element, myXmlServiceClassAttrPattern.get(), myXmlServiceIdAttrPattern.get());
            if (className == null) {
                return;
            }

            XmlTag serviceTag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
            if (serviceTag == null) {
                return;
            }

            // attach problems to string value only
            PsiElement[] psiElements = element.getChildren();
            if (psiElements.length > 2) {
                TaggedExtendsInterfaceClassInspection.registerTaggedProblems(psiElements[1], FormUtil.getTags(serviceTag), className, myHolder, myServiceCollector);
            }
        }
    }
}
