package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: was the nested class ContainerConstantInspection$ContainerConstantXmlInspection
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ContainerConstantXmlInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyXmlPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "ContainerConstantXml";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Constant not found in XML definition");
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
        private @Nullable ElementPattern<PsiElement> myXmlConstantPattern;

        MyXmlPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            ContainerConstantInspection.visitXmlElement(element, myHolder, getXmlConstantPattern());
            super.visitElement(element);
        }

        private ElementPattern<PsiElement> getXmlConstantPattern() {
            ElementPattern<PsiElement> pattern = myXmlConstantPattern != null ? myXmlConstantPattern : XmlHelper.getArgumentValueWithTypePattern("constant");
            myXmlConstantPattern = pattern;
            return pattern;
        }
    }
}
