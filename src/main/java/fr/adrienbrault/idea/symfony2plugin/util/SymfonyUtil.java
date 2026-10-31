package fr.adrienbrault.idea.symfony2plugin.util;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.VersionComparatorUtil;

import java.util.HashSet;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyUtil {

    private static final Key<CachedValue<Set<String>>> CACHE = new Key<>("SYMFONY_VERSION_CACHE");

    private static boolean compare(Project project, String version, Comparator comparator) {
        Set<String> cache = CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(getVersions(project), PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)),
            false
        );

        for (String s : cache) {
            if(comparator.accepts(s)) {
                return true;
            }
        }

        return false;
    }

    public static Set<String> getVersions(Project project) {
        Set<String> versions = new HashSet<>();

        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, "Symfony\\Component\\HttpKernel\\Kernel")) {
            Field versionField = phpClass.findFieldByName("VERSION", true);
            if(versionField == null) {
                continue;
            }

            PsiElement defaultValue = versionField.getDefaultValue();
            if(!(defaultValue instanceof StringLiteralExpression)) {
                continue;
            }

            String contents = ((StringLiteralExpression) defaultValue).getContents();
            if(isBlank(contents)) {
                continue;
            }

            // 3.2.0-DEV, 3.2.0-RC1
            contents = contents.toLowerCase().replaceAll("(.*)-([\\w]+)$", "$1");
            versions.add(contents);
        }

        return versions;
    }

    public static boolean isVersionGreaterThenEquals(Project project, String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) >= 0);
    }

    public static boolean isVersionGreaterThen(Project project, String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) > 0);
    }

    public static boolean isVersionLessThenEquals(Project project, String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) <= 0);
    }

    public static boolean isVersionLessThen(Project project, String version) {
        return compare(project, version, contents -> VersionComparatorUtil.compare(contents, version) < 0);
    }

    private interface Comparator {
        boolean accepts(String contents);
    }
}
