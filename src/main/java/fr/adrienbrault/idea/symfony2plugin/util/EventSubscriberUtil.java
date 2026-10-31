package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil;
import fr.adrienbrault.idea.symfony2plugin.config.dic.EventDispatcherSubscribedEvent;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagFactory;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.ServiceTagVisitorInterface;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.DispatcherEvent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.EventAnnotationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventSubscriberUtil {

    public static void visitNamedTag(Project project, String tagName, ServiceTagVisitorInterface visitor) {

        for (String service : ServiceUtil.getTaggedServices(project, tagName)) {
            for (PsiElement psiElement : ServiceIndexUtil.findServiceDefinitions(project, service)) {
                Collection<ServiceTagInterface> serviceTagVisitorArguments = ServiceTagFactory.create(service, psiElement);

                if(serviceTagVisitorArguments == null) {
                    continue;
                }

                for (ServiceTagInterface tagVisitorArgument : serviceTagVisitorArguments) {
                    if(tagName.equals(tagVisitorArgument.getName())) {
                        visitor.visit(tagVisitorArgument);
                    }
                }
            }
        }
    }

    /**
     * @TODO: implement collection with prio
     */
    public static Collection<String> getTaggedEventMethodParameter(Project project, String eventName) {

        // Static list
        if(ServiceUtil.TAGS.containsKey(eventName)) {
            return Collections.singletonList(ServiceUtil.TAGS.get(eventName));
        }

        // @Event annotation
        DispatcherEvent dispatcherEvent = ContainerUtil.getFirstItem(FileBasedIndex.getInstance().getValues(
            EventAnnotationStubIndex.KEY,
            eventName,
            GlobalSearchScope.allScope(project))
        );

        if(dispatcherEvent != null && StringUtils.isNotBlank(dispatcherEvent.getInstance())) {
            return normalizeFqns(Collections.singletonList(dispatcherEvent.getInstance()));
        }

        // Extract from directly from EventSubscriberInterface
        for (EventDispatcherSubscribedEvent event : EventDispatcherSubscriberUtil.getSubscribedEvent(project, eventName)) {
            String methodName = event.getMethodName();
            if(methodName == null) {
                continue;
            }

            Method method = PhpElementsUtil.getClassMethod(project, event.getFqnClassName(), methodName);
            if(method != null) {
                Collection<String> methodParameterClassHints = PhpElementsUtil.getMethodParameterTypeHints(method);
                if(!methodParameterClassHints.isEmpty()) {
                    return methodParameterClassHints;
                }
            }
        }

        // Visit all services tagged with "kernel.event_listener"
        for (String service : ServiceUtil.getTaggedServices(project, "kernel.event_listener")) {
            for (PsiElement psiElement : ServiceIndexUtil.findServiceDefinitions(project, service)) {
                Collection<ServiceTagInterface> serviceTagVisitorArguments = ServiceTagFactory.create(service, psiElement);

                if(serviceTagVisitorArguments == null) {
                    continue;
                }

                for (ServiceTagInterface tag : serviceTagVisitorArguments) {

                    if(!eventName.equals(tag.getAttribute("event"))) {
                        continue;
                    }

                    String methodName = tag.getAttribute("method");
                    if(StringUtils.isBlank(methodName)) {
                        continue;
                    }

                    // @TODO: collect
                    PhpClass phpClass = ServiceUtil.getServiceClass(project, tag.getServiceId());
                    if(phpClass != null) {
                        Method method = phpClass.findMethodByName(methodName);
                        if(method != null) {
                            Collection<String> methodParameterTypeHints = PhpElementsUtil.getMethodParameterTypeHints(method);
                            if(!methodParameterTypeHints.isEmpty()) {
                                return methodParameterTypeHints;
                            }
                        }


                    }
                }
            }
        }

        return Collections.emptyList();
    }

    private static Collection<String> normalizeFqns(Collection<String> values) {
        return values.stream()
            .filter(StringUtils::isNotBlank)
            .map(value -> value.startsWith("\\") ? value : "\\" + value)
            .collect(Collectors.toList());
    }
}
