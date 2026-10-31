/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.adrienbrault.idea.symfony2plugin.tests.routing;

import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyCodeInsightTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Navigation and completion of a route name declared by a {@code #[Route]} attribute with a named argument, through
 * a reference contributed to a PHP string.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
public class RouteNavigationTest extends SymfonyCodeInsightTestCase {
    @BeforeEach
    public void setUpRoutes() throws Exception {
        myFixture.addFileToProject("vendor/symfony/classes.php", """
            <?php
            namespace Symfony\\Component\\Routing\\Attribute {
                #[\\Attribute]
                class Route {
                    public function __construct(string $path = null, string $name = null) {}
                }
            }
            namespace Symfony\\Bundle\\FrameworkBundle\\Controller {
                abstract class AbstractController {
                    protected function generateUrl(string $route, array $parameters = []): string {}
                    protected function redirectToRoute(string $route, array $parameters = []) {}
                }
            }
            namespace Symfony\\Component\\Routing\\Generator {
                interface UrlGeneratorInterface {
                    public function generate(string $name, array $parameters = []): string;
                }
            }
            """);
        myFixture.addFileToProject("src/Controller/BlogController.php", """
            <?php
            namespace App\\Controller;

            use Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController;
            use Symfony\\Component\\Routing\\Attribute\\Route;

            class BlogController extends AbstractController {
                #[Route('/blog', name: 'blog_index')]
                public function index() {}
            }
            """);
    }

    @Test
    public void generateUrlInController() throws Exception {
        assertNavigationMatch("Usage.php", """
                <?php
                class Usage extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController {
                    public function foo() {
                        $this->generateUrl('blog<caret>_index');
                    }
                }
                """,
            element -> element instanceof Method method && "index".equals(method.getName()), "BlogController::index");
    }

    @Test
    public void urlGeneratorGenerate() throws Exception {
        assertNavigationMatch("Usage.php", """
                <?php
                /** @var $f \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */
                $f->generate('blog<caret>_index');
                """,
            element -> element instanceof Method method && "index".equals(method.getName()), "BlogController::index");
    }

    @Test
    public void generateUrlCompletion() throws Exception {
        assertCompletionContains("Usage.php", """
            <?php
            /** @var $f \\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface */
            $f->generate('<caret>');
            """, "blog_index");
    }
}
