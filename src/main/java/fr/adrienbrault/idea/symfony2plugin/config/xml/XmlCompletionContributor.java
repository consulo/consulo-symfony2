package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionType;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import consulo.project.Project;
import consulo.xml.language.XMLLanguage;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.pattern.XmlPatterns;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterLookupPercentElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleFileCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class XmlCompletionContributor extends CompletionContributor {

    public XmlCompletionContributor() {
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-service").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getAutowiringTypePattern(), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("parent").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());

        // <argument type="service" id="<caret>" />
        // <factory service="<caret>" />
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(PlatformPatterns.or(
            XmlHelper.getArgumentServiceIdPattern(),
            XmlHelper.getFactoryServiceCompletionPattern(),
            XmlHelper.getArgumentServiceIdForArgumentPattern()
        )), new ServiceCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "alias").inside(XmlHelper.getInsideTagPattern("services")), new FormAliasParametersCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getArgumentValuePattern(), new ArgumentParameterCompletionProvider());

        extend(
            CompletionType.BASIC,
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            new SymfonyBundleFileCompletionProvider("Resources/config", "config", "Controller", "src/Controller")
        );

        extend(
            CompletionType.BASIC,
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            new YamlCompletionContributor.DirectoryScopeCompletionProvider()
        );
    }

    @Override
    public Language getLanguage() {
        return XMLLanguage.INSTANCE;
    }

    private static class FormAliasParametersCompletionProvider implements CompletionProvider {
        @Override
        public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, final CompletionResultSet completionResultSet) {
            PsiElement psiElement = completionParameters.getOriginalPosition();
            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(xmlTag == null) {
                return;
            }

            XmlTag xmlTagService = PsiTreeUtil.getParentOfType(xmlTag, XmlTag.class);
            if(xmlTagService != null) {
                XmlAttribute xmlAttribute = xmlTagService.getAttribute("class");
                if(xmlAttribute != null) {
                    String value = xmlAttribute.getValue();
                    if(StringUtils.isNotBlank(value)) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), value);
                        if(phpClass != null) {
                            FormUtil.attachFormAliasesCompletions(phpClass, completionResultSet);
                        }
                    }
                }
            }

        }
    }

    /**
     * <argument>%</argument>
     * <argument></argument>
     */
    private static class ArgumentParameterCompletionProvider implements CompletionProvider {

        @Override
        public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {
            PsiElement psiElement = completionParameters.getOriginalPosition();
            if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            Project project = psiElement.getProject();
            for( Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(project).entrySet()) {
                completionResultSet.addElement(
                    new ParameterLookupPercentElement(entry.getValue())
                );
            }

            // %env('foobar')%
            for (String s : DotEnvUtil.getEnvironmentVariables(project)) {
                completionResultSet.addElement(new ParameterLookupPercentElement(new ContainerParameter("env(" + s +")", false)));
                completionResultSet.addElement(new ParameterLookupPercentElement(new ContainerParameter("env(resolve:" + s +")", false)));
            }
        }
    }
}

