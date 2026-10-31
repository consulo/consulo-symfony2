package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.xml.language.psi.XmlAttributeValue;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.pattern.AnnotationPattern;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlDuplicateServiceKeyInspectionKt;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class DuplicateLocalRouteInspection extends LocalInspectionTool {
    private static final String MESSAGE = "Symfony: Duplicate route name";

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "DuplicateLocalRouteInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Duplicate Route");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Route");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        private MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if (element instanceof YAMLKeyValue yamlKeyValue && element.getLanguage() == YAMLLanguage.INSTANCE) {
                visitYaml(yamlKeyValue);
            }
            else if (element instanceof StringLiteralExpression stringLiteralExpression && element.getLanguage() == PhpLanguage.INSTANCE) {
                visitPhp(stringLiteralExpression);
            }
            else if (element instanceof XmlAttributeValue xmlAttributeValue) {
                XmlDuplicateServiceKeyInspectionKt.visitRoot(xmlAttributeValue, holder, "routes", "route", "id", MESSAGE);
            }

            super.visitElement(element);
        }

        private void visitYaml(YAMLKeyValue yamlKeyValue) {
            PsiElement yamlMapping = yamlKeyValue.getParent();
            if (YamlHelper.isRoutingFile(yamlKeyValue.getContainingFile()) && yamlMapping instanceof YAMLMapping mapping && mapping.getParent() instanceof YAMLDocument) {
                String keyText1 = null;

                int found = 0;
                for (YAMLKeyValue keyValue : mapping.getKeyValues()) {
                    String keyText = keyValue.getKeyText();

                    // lazy
                    if (keyText1 == null) {
                        keyText1 = yamlKeyValue.getKeyText();
                    }

                    if (keyText1.equals(keyText)) {
                        found++;
                    }

                    if (found == 2) {
                        PsiElement keyElement = yamlKeyValue.getKey();
                        assert keyElement != null;
                        holder.registerProblem(keyElement, "Symfony: Duplicate route name", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

                        break;
                    }
                }
            }
        }

        private void visitPhp(StringLiteralExpression element) {
            if (PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Annotation\\Route", "name") || PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Attribute\\Route", "name")) {
                PhpAttribute parentOfType = PsiTreeUtil.getParentOfType(element, PhpAttribute.class);
                PsiElement owner = parentOfType.getOwner();
                if (owner instanceof Method method && method.getAccess().isPublic() && method.getContainingClass() != null) {
                    int found = 0;
                    String contents = element.getContents();

                    for (Method ownMethod : method.getContainingClass().getOwnMethods()) {
                        Collection<PhpAttribute> attributes = new ArrayList<>(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Annotation\\Route"));
                        attributes.addAll(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Attribute\\Route"));

                        for (PhpAttribute attribute : attributes) {
                            String name = PhpElementsUtil.getAttributeArgumentStringByName(attribute, "name");
                            if (contents.equals(name)) {
                                found++;
                            }

                            if (found == 2) {
                                holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                                return;
                            }
                        }
                    }
                }
            }

            if (AnnotationPattern.getPropertyIdentifierValue("name").accepts(element)) {
                PhpDocTag phpDocTag = PsiTreeUtil.getParentOfType(element, PhpDocTag.class);
                if (phpDocTag != null) {
                    PhpClass phpClass = AnnotationUtil.getAnnotationReference(phpDocTag);
                    if (phpClass != null && RouteHelper.isRouteClassAnnotation(phpClass.getFQN())) {
                        PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(element, PhpDocComment.class);
                        PsiElement nextPsiSibling = phpDocComment.getNextPsiSibling();
                        if (nextPsiSibling instanceof Method method && method.getAccess().isPublic() && method.getContainingClass() != null) {
                            int found = 0;
                            String contents = element.getContents();

                            for (Method ownMethod : method.getContainingClass().getOwnMethods()) {
                                PhpDocCommentAnnotation phpClassContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(ownMethod.getDocComment());
                                if (phpClassContainer != null) {
                                    PhpDocTagAnnotation firstPhpDocBlock = phpClassContainer.getFirstPhpDocBlock(RouteHelper.ROUTE_ANNOTATIONS);
                                    if (firstPhpDocBlock != null) {
                                        String name = firstPhpDocBlock.getPropertyValue("name");
                                        if (contents.equals(name)) {
                                            found++;
                                        }

                                        if (found == 2) {
                                            holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
