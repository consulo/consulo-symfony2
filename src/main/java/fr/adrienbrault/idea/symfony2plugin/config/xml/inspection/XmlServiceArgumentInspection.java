package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.quickfix.AddServiceXmlArgumentLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlServiceArgumentInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "XmlServiceArgumentInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Symfony: XML Arguments");
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

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private final Supplier<ContainerCollectionResolver.LazyServiceCollector> myServiceCollector;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof XmlTag) {
                visitService((XmlTag) element);
            }

            super.visitElement(element);
        }

        private void visitService(XmlTag xmlTag) {
            if (!ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag)) {
                return;
            }

            List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, false, myServiceCollector.get());
            if (args.isEmpty()) {
                return;
            }

            PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(xmlTag, PlatformPatterns.psiElement(XmlTokenType.XML_NAME));
            if (childrenOfType == null) {
                return;
            }

            myHolder.registerProblem(
                childrenOfType,
                "Missing argument",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new AddServiceXmlArgumentLocalQuickFix(args)
            );
        }
    }
}
