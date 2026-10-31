package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlServiceSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlServiceInstanceInspection extends LocalInspectionTool {
    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    @Override
    public String getShortName() {
        return "XmlServiceInstanceInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("XML: Wrong Service Instance");
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

    private static void annotateServiceInstance(PsiElement psiElement, ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        // search for parent service definition
        XmlTag currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if (currentXmlTag == null) {
            return;
        }

        XmlTag parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag.class);
        if (parentXmlTag == null) {
            return;
        }

        String name = parentXmlTag.getName();
        if ("service".equals(name)) {
            // service/argument[id]
            String serviceDefName = XmlHelper.getClassFromServiceDefinition(parentXmlTag);
            if (serviceDefName != null) {
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceDefName, lazyServiceCollector);

                // check type hint on constructor
                if (phpClass != null) {
                    Method constructor = phpClass.getConstructor();
                    if (constructor != null) {
                        String serviceName = ((XmlAttributeValue) psiElement).getValue();
                        if (StringUtils.isNotBlank(serviceName)) {
                            attachMethodInstances(psiElement, serviceName, constructor, XmlHelper.getArgumentIndex(currentXmlTag, constructor), holder, lazyServiceCollector);
                        }
                    }
                }
            }
        }
        else if ("call".equals(name)) {

            // service/call/argument[id]

            XmlAttribute methodAttribute = parentXmlTag.getAttribute("method");
            if (methodAttribute != null) {
                String methodName = methodAttribute.getValue();
                XmlTag serviceTag = parentXmlTag.getParentTag();

                // get service class
                if (serviceTag != null && "service".equals(serviceTag.getName())) {
                    String serviceDefName = XmlHelper.getClassFromServiceDefinition(serviceTag);
                    if (serviceDefName != null) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceDefName, lazyServiceCollector);

                        // finally check method type hint
                        if (phpClass != null) {
                            Method method = phpClass.findMethodByName(methodName);
                            if (method != null) {
                                String serviceName = ((XmlAttributeValue) psiElement).getValue();
                                if (StringUtils.isNotBlank(serviceName)) {
                                    attachMethodInstances(psiElement, serviceName, method, XmlHelper.getArgumentIndex(currentXmlTag, method), holder, lazyServiceCollector);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void attachMethodInstances(PsiElement target, String serviceName, Method method, int parameterIndex, ProblemsHolder holder, ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        Parameter[] constructorParameter = method.getParameters();
        if (parameterIndex >= constructorParameter.length) {
            return;
        }

        String className = constructorParameter[parameterIndex].getDeclaredType().toString();
        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), className);
        if (expectedClass == null) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceName, lazyServiceCollector);
        if (serviceParameterClass != null && !PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.registerProblem(
                target,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new XmlServiceSuggestIntentionAction(expectedClass.getFQN(), target)
            );
        }
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder myHolder;
        private ContainerCollectionResolver.@Nullable LazyServiceCollector myLazyServiceCollector;
        private @Nullable ElementPattern<?> myArgumentServiceIdPattern;

        MyPsiElementVisitor(ProblemsHolder holder) {
            myHolder = holder;
        }

        @Override
        public void visitElement(PsiElement psiElement) {
            if (getArgumentServiceIdPattern().accepts(psiElement)) {
                ContainerCollectionResolver.LazyServiceCollector collector = myLazyServiceCollector != null
                    ? myLazyServiceCollector
                    : new ContainerCollectionResolver.LazyServiceCollector(myHolder.getProject());
                myLazyServiceCollector = collector;

                annotateServiceInstance(psiElement, myHolder, collector);
            }

            super.visitElement(psiElement);
        }

        private ElementPattern<?> getArgumentServiceIdPattern() {
            ElementPattern<?> pattern = myArgumentServiceIdPattern != null ? myArgumentServiceIdPattern : XmlHelper.getArgumentServiceIdPattern();
            myArgumentServiceIdPattern = pattern;
            return pattern;
        }
    }
}
