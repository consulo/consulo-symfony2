package fr.adrienbrault.idea.symfony2plugin.external.toolbox.provider;

import consulo.annotation.component.ExtensionImpl;

import consulo.ui.image.Image;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import de.espend.idea.php.toolbox.completion.dict.PhpToolboxCompletionContributorParameter;
import de.espend.idea.php.toolbox.extension.PhpToolboxProviderAbstract;
import de.espend.idea.php.toolbox.navigation.dict.PhpToolboxDeclarationHandlerParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderParameter;
import de.espend.idea.php.toolbox.provider.presentation.ProviderPresentation;
import de.espend.idea.php.toolbox.type.PhpToolboxTypeProviderArguments;
import de.espend.idea.php.toolbox.type.PhpToolboxTypeProviderInterface;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceStringLookupElement;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class ServiceToolboxProvider extends PhpToolboxProviderAbstract implements PhpToolboxTypeProviderInterface {

    @Override
    public Collection<LookupElement> getLookupElements(PhpToolboxCompletionContributorParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        List<LookupElement> results = new ArrayList<>();

        boolean aPrivate = parameter.getParameterBag().getParameterBool("private", false);
        boolean aPublic = parameter.getParameterBag().getParameterBool("public", true);

        for (ContainerService service : ContainerCollectionResolver.getServices(parameter.getProject()).values()) {
            if(service.isPrivate() == aPrivate) {
                results.add(new ServiceStringLookupElement(service));
                continue;
            }

            if(service.isPrivate() != aPublic) {
                results.add(new ServiceStringLookupElement(service));
            }
        }

        return results;
    }

    @Override
    public Collection<PsiElement> getPsiTargets(PhpToolboxDeclarationHandlerParameter parameter) {
        if(!Symfony2ProjectComponent.isEnabled(parameter.getProject())) {
            return Collections.emptyList();
        }

        final PhpClass serviceClass = ServiceUtil.getServiceClass(parameter.getProject(), parameter.getContents());
        if(serviceClass == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>() {{
            add(serviceClass);
        }};
    }

    @Override
    public String getName() {
        return "symfony.services";
    }

    @Nullable
    @Override
    public ProviderPresentation getPresentation() {
        return new ProviderPresentation() {
            @Nullable
            @Override
            public Image getIcon() {
                return Symfony2Icons.SERVICE;
            }

            @Nullable
            @Override
            public String getDescription() {
                return "Public services";
            }

            public ProviderParameter @Nullable [] getParameter() {
                return new ProviderParameter[] {
                    new ProviderParameter("private", ProviderParameter.TYPE.BOOLEAN),
                    new ProviderParameter("public", ProviderParameter.TYPE.BOOLEAN),
                };
            }
        };
    }

    @Nullable
    @Override
    public Collection<PhpNamedElement> resolveParameter(PhpToolboxTypeProviderArguments arguments) {
        PhpClass serviceClass = ServiceUtil.getServiceClass(arguments.getProject(), arguments.getParameter());
        if(serviceClass == null) {
            return null;
        }

        return new ArrayList<>() {{
            add(serviceClass);
        }};
    }
}
