package fr.adrienbrault.idea.symfony2plugin.tests.routing.dic;

import org.junit.jupiter.api.Test;

import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.dic.ServiceRouteContainer;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

import java.util.Collection;

public class ServiceRouteContainerTest extends SymfonyLightCodeInsightFixtureTestCase {
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/routing/fixtures";
    }

    @Test

    public void testGetServiceNamesForServiceControllerRoutes() {
        configureServiceControllerRouteFixtures();

        assertContainsElements(ServiceRouteContainer.getServiceNames(getProject()), "foo.bar_controller");
    }

    @Test

    public void testGetMethodMatchesForServiceControllerClassMethod() {
        configureServiceControllerRouteFixtures();

        Method method = PhpElementsUtil.getClassMethod(getProject(), "Service\\Controller\\FooController", "indexAction");
        assertNotNull(method);

        Collection<Route> routes = ServiceRouteContainer.getMethodMatchesForRouteController(method);
        assertNotNull(routes.stream().filter(route -> "xml_route_as_service".equals(route.getName())).findFirst().orElse(null));
    }

    private void configureServiceControllerRouteFixtures() {
        myFixture.copyFileToProject("GetRoutesOnControllerAction.php");
        myFixture.copyFileToProject("GetRoutesOnControllerAction.routing.xml");
        myFixture.copyFileToProject("GetRoutesOnControllerAction.services.xml");
    }
}
