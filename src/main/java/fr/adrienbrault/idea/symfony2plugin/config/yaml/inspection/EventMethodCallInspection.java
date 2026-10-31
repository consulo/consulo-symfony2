package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpAttributesOwner;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpReturn;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.util.lang.lazy.LazyValue;
import consulo.xml.language.XMLLanguage;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.EventSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class EventMethodCallInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "EventMethodCall";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Create Method");
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
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;

        private final Supplier<ContainerCollectionResolver.LazyServiceCollector> myServiceCollector;

        private final Supplier<ElementPattern<PsiElement>> myYamlTagsMethodPattern = LazyValue.notNull(() -> StandardPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("tags"),
            YamlElementPatternHelper.getSingleLineScalarKey("method")
        ));

        private final Supplier<ElementPattern<PsiElement>> myYamlTextPattern = LazyValue.notNull(() -> PlatformPatterns.psiElement(YAMLTokenTypes.TEXT));

        private final Supplier<ElementPattern<PsiElement>> myYamlDoubleQuotedScalarPattern = LazyValue.notNull(() -> PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING));

        private final Supplier<ElementPattern<PsiElement>> myYamlInsideCallsPattern = LazyValue.notNull(() -> YamlElementPatternHelper.getInsideKeyValue("calls"));

        private final Supplier<ElementPattern<PsiElement>> myXmlTagMethodPattern = LazyValue.notNull(() -> XmlHelper.getTagAttributePattern("tag", "method")
            .inside(XmlHelper.getInsideTagPattern("services"))
            .inFile(XmlHelper.getXmlFilePattern()));

        private final Supplier<ElementPattern<PsiElement>> myXmlCallMethodPattern = LazyValue.notNull(() -> XmlHelper.getTagAttributePattern("call", "method")
            .inside(XmlHelper.getInsideTagPattern("services"))
            .inFile(XmlHelper.getXmlFilePattern()));

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
            myServiceCollector = LazyValue.notNull(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
        }

        @Override
        public void visitElement(PsiElement element) {
            Language language = element.getLanguage();

            if (language == YAMLLanguage.INSTANCE) {
                visitYmlElement(element);
            }
            else if (language == XMLLanguage.INSTANCE) {
                visitXmlElement(element);
            }
            else if (language == PhpLanguage.INSTANCE && element instanceof StringLiteralExpression) {
                visitPhpElement((StringLiteralExpression) element, myHolder);
            }

            super.visitElement(element);
        }

        private void visitXmlElement(PsiElement element) {
            boolean isSupportedTag = myXmlTagMethodPattern.get().accepts(element) || myXmlCallMethodPattern.get().accepts(element);

            if (isSupportedTag) {
                // attach to text child only
                PsiElement[] psiElements = element.getChildren();
                if (psiElements.length < 2) {
                    return;
                }

                String serviceClass = XmlHelper.getServiceDefinitionClass(element);
                if (serviceClass == null || serviceClass.isBlank()) {
                    return;
                }

                registerMethodProblem(psiElements[1], serviceClass);
            }
        }

        private void visitYamlMethodTagKey(PsiElement psiElement) {
            String methodName = PsiElementUtils.trimQuote(psiElement.getText());
            if (methodName.isBlank()) {
                return;
            }

            String classValue = YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement);
            if (classValue == null) {
                return;
            }

            registerMethodProblem(psiElement, classValue);
        }

        private void visitYmlElement(PsiElement psiElement) {
            if (myYamlTagsMethodPattern.get().accepts(psiElement)) {
                visitYamlMethodTagKey(psiElement);
            }

            if (myYamlTextPattern.get().accepts(psiElement) || myYamlDoubleQuotedScalarPattern.get().accepts(psiElement)) {
                visitYamlMethod(psiElement);
            }
        }

        private void visitYamlMethod(PsiElement psiElement) {
            if (myYamlInsideCallsPattern.get().accepts(psiElement)) {
                PsiElement parent = psiElement.getParent();
                if (parent instanceof YAMLScalar) {
                    YamlHelper.visitServiceCall((YAMLScalar) parent, service ->
                        registerMethodProblem(psiElement, YamlHelper.trimSpecialSyntaxServiceName(service))
                    );
                }
            }
        }

        private void registerMethodProblem(PsiElement psiElement, String classKeyValue) {
            EventMethodCallInspection.registerMethodProblem(
                psiElement,
                myHolder,
                ServiceUtil.getResolvedClassDefinition(
                    psiElement.getProject(),
                    classKeyValue,
                    myServiceCollector.get()
                )
            );
        }
    }

    private static void registerMethodProblem(PsiElement psiElement, ProblemsHolder holder, @Nullable PhpClass phpClass) {
        if (phpClass == null) {
            return;
        }

        PhpClass resolvedClass = phpClass;

        String methodName = PsiElementUtils.trimQuote(psiElement.getText());
        if (resolvedClass.findMethodByName(methodName) != null) {
            return;
        }

        holder.registerProblem(
            psiElement,
            "Missing Method",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            new CreateMethodQuickFix(resolvedClass, methodName, new MyCreateMethodQuickFix())
        );
    }

    private static class MyCreateMethodQuickFix implements CreateMethodQuickFix.InsertStringInterface {
        @Override
        public StringBuilder getStringBuilder(ProblemDescriptor problemDescriptor, PhpClass phpClass, String functionName) {
            String eventType = getEventTypeHint(problemDescriptor, phpClass);
            String parameter = eventType != null ? eventType + " $event" : "";

            return new StringBuilder("public function " + functionName + "(" + parameter + ")\n {\n}\n\n");
        }

        private @Nullable String getEventTypeHint(ProblemDescriptor problemDescriptor, PhpClass phpClass) {
            String eventName = EventDispatcherSubscriberUtil.getEventNameFromScope(problemDescriptor.getPsiElement());
            if (eventName == null) {
                return null;
            }

            Collection<String> taggedEventMethodParameter = EventSubscriberUtil.getTaggedEventMethodParameter(problemDescriptor.getPsiElement().getProject(), eventName);
            if (taggedEventMethodParameter.isEmpty()) {
                return null;
            }

            return taggedEventMethodParameter.stream()
                .map(it -> {
                    String imported = importIfNecessary(phpClass, it);
                    return imported != null ? imported : "null";
                })
                .collect(Collectors.joining("|"));
        }

        private @Nullable String importIfNecessary(PhpClass phpClass, String fqn) {
            String qualifiedName = AnnotationBackportUtil.getQualifiedName(phpClass, fqn);
            if (qualifiedName != null && !qualifiedName.equals(StringUtils.stripStart(fqn, "\\"))) {
                // class already imported
                return qualifiedName;
            }

            return PhpElementsUtil.insertUseIfNecessary(phpClass, fqn);
        }
    }

    /**
     * getSubscribedEvents method quick fix check
     *
     * return array(
     *   ConsoleEvents::COMMAND => array('onCommanda', 255),
     *   ConsoleEvents::TERMINATE => array('onTerminate', -255),
     * );
     *
     */
    private static void visitPhpElement(StringLiteralExpression element, ProblemsHolder holder) {
        PsiElement parent = element.getParent();
        if (parent != null && parent.getNode() != null && parent.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE &&
            PsiTreeUtil.getParentOfType(parent, PhpReturn.class) != null
        ) {
            Method method = PsiTreeUtil.getParentOfType(parent, Method.class);
            PhpClass containingClass = method != null ? method.getContainingClass() : null;
            if (method != null && "getSubscribedEvents".equals(method.getName()) &&
                containingClass != null &&
                PhpElementsUtil.isInstanceOf(
                    containingClass,
                    "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface"
                )
            ) {
                String contents = element.getContents();
                if (!contents.isBlank() && containingClass.findMethodByName(contents) == null) {
                    registerMethodProblem(element, holder, containingClass);
                }
            }
        }

        if (parent instanceof ParameterList && PhpElementsUtil.isAttributeNamedArgumentString(
            element,
            "\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener",
            "method"
        )) {
            PhpAttribute phpAttribute = PsiTreeUtil.getParentOfType(parent, PhpAttribute.class);
            PhpAttributesOwner owner = phpAttribute != null ? phpAttribute.getOwner() : null;
            if (owner instanceof PhpClass) {
                String contents = element.getContents();
                if (!contents.isBlank() && ((PhpClass) owner).findMethodByName(contents) == null) {
                    registerMethodProblem(element, holder, (PhpClass) owner);
                }
            }
        }
    }
}
