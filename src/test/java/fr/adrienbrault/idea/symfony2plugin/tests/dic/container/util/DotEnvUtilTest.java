package fr.adrienbrault.idea.symfony2plugin.tests.dic.container.util;

import org.junit.jupiter.api.Test;

import consulo.language.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.DotEnvUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DotEnvUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("env.env");
        myFixture.copyFileToProject("docker-compose.yml");
        myFixture.copyFileToProject("Dockerfile");
        myFixture.copyFileToProject(".env");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/container/util/fixtures";
    }

    @Test

    public void testGetEnvironmentVariables() {
        assertContainsElements(DotEnvUtil.getEnvironmentVariables(getProject()), "foobar", "DEBUG_WEB", "DEBUG_SERVICES", "DOCKERFILE_FOO", "DEBUG_WEB_2", "DEBUG_SERVICES_2", "ADMIN_USER_DOCKERFILE");
    }

    @Test

    public void testGetEnvironmentVariableTargets() {
        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargets(getProject(), "foobar")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );
    }

    @Test

    public void testGetEnvironmentVariablesWithValuesDoubleQuoted() {
        Map<String, String> vars = DotEnvUtil.getEnvironmentVariablesWithValues(getProject());
        assertEquals("postgresql://app:secret@127.0.0.1:5432/mydb?serverVersion=16", vars.get("DATABASE_URL"));
    }

    @Test

    public void testGetEnvironmentVariablesWithValuesPlain() {
        Map<String, String> vars = DotEnvUtil.getEnvironmentVariablesWithValues(getProject());
        assertEquals("plain_value", vars.get("PLAIN_VAR"));
    }

    @Test

    public void testGetEnvironmentVariablesWithValuesSingleQuoted() {
        Map<String, String> vars = DotEnvUtil.getEnvironmentVariablesWithValues(getProject());
        assertEquals("single", vars.get("SINGLE_QUOTED"));
    }

    @Test

    public void testGetEnvironmentVariablesWithValuesInlineCommentStripped() {
        Map<String, String> vars = DotEnvUtil.getEnvironmentVariablesWithValues(getProject());
        assertEquals("value", vars.get("COMMENTED"));
    }

    @Test

    public void testGetEnvironmentVariablesWithValuesWhitespaceAroundEquals() {
        Map<String, String> vars = DotEnvUtil.getEnvironmentVariablesWithValues(getProject());
        assertEquals("whitespace_value", vars.get("WHITESPACE_KEY"));
    }

    @Test

    public void testGetEnvironmentVariableTargetsForParameter() {
        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargetsForParameter(getProject(), "%env(int:foobar)%")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );

        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargetsForParameter(getProject(), "%env(foobar)%")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );

        assertEquals(1, DotEnvUtil.getEnvironmentVariableTargetsForParameter(getProject(), "%env(int:json:foo_foo:foo-foo:foobar)%")
            .stream()
            .filter(psiElement -> psiElement instanceof PsiFile && "env.env".equals(((PsiFile) psiElement).getName()))
            .count()
        );
    }
}
