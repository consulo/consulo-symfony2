package fr.adrienbrault.idea.symfony2plugin.dic;

import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inlay.HintInfo;
import consulo.language.editor.inlay.InlayInfo;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlText;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceArgumentParameterHintsProvider implements InlayParameterHintsProvider {
    @Override
    public List<InlayInfo> getParameterHints(PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return Collections.emptyList();
        }

        List<InlayInfo> inlays = new ArrayList<>();

        Match typeHint = getTypeHint(psiElement);
        if(typeHint != null) {
            inlays.add(new InlayInfo(typeHint.parameter, typeHint.targetOffset));
        }

        return inlays;
    }

    @Nullable
    @Override
    public HintInfo getHintInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    public Set<String> getDefaultBlackList() {
        return Collections.emptySet();
    }

    @Override
    public String getInlayPresentation(String inlayText) {
        // remove ":"
        return inlayText;
    }

    @Override
    public LocalizeValue getPreviewFileText() {
        return LocalizeValue.empty();
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }
    
    @Nullable
    private Match getTypeHint(PsiElement psiElement) {
        if(psiElement instanceof YAMLScalar) {
            // arguments: [@foobar]
            ServiceTypeHint serviceTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(
                (YAMLScalar) psiElement,
                new ContainerCollectionResolver.LazyServiceCollector(psiElement.getProject())
            );

            if(serviceTypeHint != null) {
                String s = attachMethodInstances(serviceTypeHint.getMethod(), serviceTypeHint.getIndex());
                if(s != null) {
                    return new Match(s, psiElement.getTextRange().getEndOffset());
                }
            }

            // call: [setFoo, [@getParamater]]
            final Match[] match = {null};
            YamlHelper.visitServiceCallArgumentMethodIndex((YAMLScalar) psiElement, parameter ->
                match[0] = new Match(createTypeHintFromParameter(psiElement.getProject(), parameter), psiElement.getTextRange().getEndOffset())
            );

            return match[0];
        } else if (psiElement instanceof XmlAttributeValue) {
            // <service><argument type="service" id="a<caret>a"></service>
            PsiElement xmlAttribute = psiElement.getParent();
            if(xmlAttribute instanceof XmlAttribute && "id".equalsIgnoreCase(((XmlAttribute) xmlAttribute).getName())) {
                PsiElement argumentTag = xmlAttribute.getParent();

                if (argumentTag instanceof XmlTag) {
                    if ("service".equalsIgnoreCase(((XmlTag) argumentTag).getName())) {
                        // <service type="alias" id="a<caret>a"/>
                        String alias = ((XmlTag) argumentTag).getAttributeValue("alias");
                        if (alias != null && StringUtils.isNotBlank(alias)) {
                            PhpClass serviceClass = ServiceUtil.getServiceClass(psiElement.getProject(), alias);
                            if (serviceClass != null) {
                                return new Match(serviceClass.getName(), argumentTag.getTextRange().getEndOffset());
                            }
                        }
                    } else if ("argument".equalsIgnoreCase(((XmlTag) argumentTag).getName())) {
                        // <service><argument type="service" id="a<caret>a"></service>
                        PsiElement serviceTag = argumentTag.getParent();
                        if (serviceTag instanceof XmlTag && "service".equals(((XmlTag) serviceTag).getName())) {
                            Pair<String, Method> parameterHint = findMethodParameterHint((XmlTag) argumentTag);
                            if (parameterHint != null) {
                                return new Match(parameterHint.getFirst(), argumentTag.getTextRange().getEndOffset());
                            }
                        }

                        // <call method="setMailer">
                        //   <argument type="service" id="ma<caret>iler" />
                        // </call>
                        final Match[] match = {null};
                        XmlHelper.visitServiceCallArgumentMethodIndex((XmlAttributeValue) psiElement, parameter ->
                            match[0] = new Match(createTypeHintFromParameter(psiElement.getProject(), parameter), argumentTag.getTextRange().getEndOffset())
                        );

                        return match[0];
                    }
                }
            }
        } else if(psiElement instanceof XmlText) {
            // <service><argument>%a<caret>a%</argument></service>
            PsiElement argumentTag = psiElement.getParent();

            // match only: <argument>%a<caret>a%</argument>
            // ignore: <argument>\n<caret><argument></argument></argument>
            if(argumentTag instanceof XmlTag && "argument".equals(((XmlTag) argumentTag).getName()) && ((XmlTag) argumentTag).getSubTags().length == 0) {
                PsiElement serviceTag = argumentTag.getParent();
                if(serviceTag instanceof XmlTag && "service".equals(((XmlTag) serviceTag).getName())) {
                    Pair<String, Method> parameterHint = findMethodParameterHint((XmlTag) argumentTag);
                    if(parameterHint != null) {
                        return new Match(parameterHint.getFirst(), argumentTag.getTextRange().getEndOffset());
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private Pair<String, Method> getParamater(Project project, String aClass, java.util.function.Function<Void, Integer> function) {
        if(StringUtils.isNotBlank(aClass)) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(project, aClass);
            if(phpClass != null) {
                Method constructor = phpClass.getConstructor();
                if(constructor != null) {
                    Integer argumentIndex = function.apply(null);
                    if(argumentIndex >= 0) {
                        String s = attachMethodInstances(constructor, argumentIndex);
                        if(s == null) {
                            return null;
                        }

                        return Pair.create(s, constructor);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private Pair<String, Method> findMethodParameterHint(XmlTag argumentTag) {
        PsiElement serviceTag = argumentTag.getParent();
        if("service".equalsIgnoreCase(((XmlTag) serviceTag).getName())) {
            String aClass = XmlHelper.getClassFromServiceDefinition((XmlTag) serviceTag);
            if(aClass != null && StringUtils.isNotBlank(aClass)) {
                return getParamater(argumentTag.getProject(), aClass, aVoid -> XmlHelper.getArgumentIndex(argumentTag));
            }
        }

        return null;
    }

    @Nullable
    private String attachMethodInstances(Function function, int parameterIndex) {
        Parameter[] constructorParameter = function.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return null;
        }

        Parameter parameter = constructorParameter[parameterIndex];

        return createTypeHintFromParameter(function.getProject(), parameter);
    }

    private String createTypeHintFromParameter(Project project, Parameter parameter) {
        String className = parameter.getDeclaredType().toString();
        if(PhpType.isNotExtendablePrimitiveType(className)) {
            return parameter.getName();
        }

        int i = className.lastIndexOf("\\");
        if(i > 0) {
            return className.substring(i + 1);
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(project, className);
        if(expectedClass != null) {
            return expectedClass.getName();
        }

        return parameter.getName();
    }

    private record Match(String parameter, int targetOffset) {}
}
