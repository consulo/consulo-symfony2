package fr.adrienbrault.idea.symfony2plugin.config.utils;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLFileType;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigUtil {

    private static final Key<CachedValue<TreeSignatureCache>> TREE_SIGNATURE_CACHE = new Key<>("TREE_SIGNATURE_CACHE");

    /**
     * Search for all "Symfony\Component\Config\Definition\Builder\TreeBuilder::root" elements
     */
    public static Map<String, Collection<String>> getTreeSignatures(Project project) {
        return getTreeSignatureCache(project).treeSignatures();
    }

    /**
     * Resolve config root keys to their PSI targets using the shared project cache as a prefilter.
     */
    public static Collection<PsiElement> getTreeSignatureTargets(Project project, String key) {
        Collection<TreeTargetHint> treeTargetHints = getTreeSignatureCache(project).treeTargetHints().get(key);
        if (treeTargetHints == null || treeTargetHints.isEmpty()) {
            return Collections.emptyList();
        }

        return resolveTreeSignatureTargets(project, key, treeTargetHints);
    }

    /**
     * Resolve config root keys to PSI targets and narrow them to the given configuration classes.
     */
    public static Collection<PsiElement> getTreeSignatureTargets(Project project, String key, Collection<String> classes) {
        Collection<TreeTargetHint> treeTargetHints = getTreeSignatureCache(project).treeTargetHints().get(key);
        if (treeTargetHints == null || treeTargetHints.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> classFilter = new HashSet<>(classes);
        List<TreeTargetHint> filteredHints = treeTargetHints.stream()
            .filter(treeTargetHint -> classFilter.contains(treeTargetHint.fqn()))
            .toList();

        if (filteredHints.isEmpty()) {
            return Collections.emptyList();
        }

        return resolveTreeSignatureTargets(project, key, filteredHints);
    }

    private static TreeSignatureCache getTreeSignatureCache(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            TREE_SIGNATURE_CACHE,
            () -> CachedValueProvider.Result.create(buildTreeSignatureCache(project), PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)),
            false
        );
    }

    private static TreeSignatureCache buildTreeSignatureCache(Project project) {
        Map<String, Collection<String>> signatures = new HashMap<>();
        Map<String, Collection<TreeTargetHint>> targetHints = new HashMap<>();

        visitTreeSignatures(PhpIndexUtil.getAllSubclasses(project, "Symfony\\Component\\Config\\Definition\\ConfigurationInterface"), treeVisitor -> {
            signatures.computeIfAbsent(treeVisitor.contents, _ignored -> new HashSet<>()).add(treeVisitor.phpClass.getFQN());
            // Cache string-based hints only, then resolve PSI from the narrowed class scope on demand.
            targetHints.computeIfAbsent(treeVisitor.contents, _ignored -> new ArrayList<>()).add(new TreeTargetHint(treeVisitor.phpClass.getFQN(), treeVisitor.kind));
        });

        return new TreeSignatureCache(signatures, targetHints);
    }

    private static void visitTreeSignatures(Collection<PhpClass> classes, Consumer<TreeVisitor> consumer) {
        for (PhpClass phpClass : classes) {
            Method method = phpClass.findOwnMethodByName("getConfigTreeBuilder");
            if(method == null) {
                continue;
            }

            method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    // Scan the method body once and apply cheap name checks before expensive type resolution.
                    if (element instanceof MethodReference methodReference) {
                        visitTreeBuilderRootCall(phpClass, methodReference, consumer);
                    } else if (element instanceof NewExpression newExpression) {
                        visitTreeBuilderInstantiation(phpClass, newExpression, consumer);
                    }

                    super.visitElement(element);
                }
            });
        }
    }

    /**
     * Symfony < 4.1: (new TreeBuilder())->root('foobar')
     */
    private static void visitTreeBuilderRootCall(PhpClass phpClass, MethodReference methodReference, Consumer<TreeVisitor> consumer) {
        if (!"root".equals(methodReference.getName())) {
            return;
        }

        if (!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder", "root")) {
            return;
        }

        visitTreeSignature(phpClass, methodReference, consumer);
    }

    /**
     * Symfony >= 4.1: new TreeBuilder('foobar')
     */
    private static void visitTreeBuilderInstantiation(PhpClass phpClass, NewExpression newExpression, Consumer<TreeVisitor> consumer) {
        ClassReference classReference = newExpression.getClassReference();
        if (classReference == null || !"TreeBuilder".equals(classReference.getName())) {
            return;
        }

        if (!PhpElementsUtil.isNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder")) {
            return;
        }

        visitTreeSignature(phpClass, newExpression, consumer);
    }

    private static void visitTreeSignature(PhpClass phpClass, ParameterListOwner ownerParameters, Consumer<TreeVisitor> consumer) {
        PsiElement[] parameters = ownerParameters.getParameters();
        if (parameters.length == 0) {
            return;
        }

        String contents = PhpElementsUtil.getStringValue(parameters[0]);
        if(contents == null || StringUtils.isBlank(contents)) {
            return;
        }

        consumer.accept(new TreeVisitor(phpClass, parameters[0], contents, ownerParameters instanceof MethodReference ? TreeBuilderKind.ROOT_METHOD : TreeBuilderKind.CONSTRUCTOR));
    }

    private static Collection<PsiElement> resolveTreeSignatureTargets(Project project, String key, Collection<TreeTargetHint> treeTargetHints) {
        Map<String, EnumSet<TreeBuilderKind>> kindsByClass = new HashMap<>();
        for (TreeTargetHint treeTargetHint : treeTargetHints) {
            kindsByClass.computeIfAbsent(treeTargetHint.fqn(), _ignored -> EnumSet.noneOf(TreeBuilderKind.class)).add(treeTargetHint.kind());
        }

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (Map.Entry<String, EnumSet<TreeBuilderKind>> entry : kindsByClass.entrySet()) {
            PhpClass phpClass = PhpElementsUtil.getClass(project, entry.getKey());
            if (phpClass == null) {
                continue;
            }

            Method method = phpClass.findOwnMethodByName("getConfigTreeBuilder");
            if (method == null) {
                continue;
            }

            resolveTreeSignatureTargets(phpClass, method, key, entry.getValue(), psiElements);
        }

        return psiElements;
    }

    private static void resolveTreeSignatureTargets(PhpClass phpClass, Method method, String key, Set<TreeBuilderKind> kinds, Collection<PsiElement> psiElements) {
        method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (kinds.contains(TreeBuilderKind.ROOT_METHOD) && element instanceof MethodReference methodReference) {
                    addMatchingTreeTarget(key, methodReference, psiElements);
                } else if (kinds.contains(TreeBuilderKind.CONSTRUCTOR) && element instanceof NewExpression newExpression) {
                    addMatchingTreeTarget(key, newExpression, psiElements);
                }

                super.visitElement(element);
            }
        });
    }

    private static void addMatchingTreeTarget(String key, MethodReference methodReference, Collection<PsiElement> psiElements) {
        if (!"root".equals(methodReference.getName())) {
            return;
        }

        if (!PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder", "root")) {
            return;
        }

        addMatchingTreeTarget(key, (ParameterListOwner) methodReference, psiElements);
    }

    private static void addMatchingTreeTarget(String key, NewExpression newExpression, Collection<PsiElement> psiElements) {
        ClassReference classReference = newExpression.getClassReference();
        if (classReference == null || !"TreeBuilder".equals(classReference.getName())) {
            return;
        }

        if (!PhpElementsUtil.isNewExpressionPhpClassWithInstance(newExpression, "Symfony\\Component\\Config\\Definition\\Builder\\TreeBuilder")) {
            return;
        }

        addMatchingTreeTarget(key, (ParameterListOwner) newExpression, psiElements);
    }

    private static void addMatchingTreeTarget(String key, ParameterListOwner ownerParameters, Collection<PsiElement> psiElements) {
        PsiElement[] parameters = ownerParameters.getParameters();
        if (parameters.length == 0) {
            return;
        }

        String contents = PhpElementsUtil.getStringValue(parameters[0]);
        if (contents == null || !key.equalsIgnoreCase(contents)) {
            return;
        }

        psiElements.add(parameters[0]);
    }

    private record TreeVisitor(PhpClass phpClass, PsiElement psiElement, String contents, TreeBuilderKind kind) {}

    private record TreeTargetHint(String fqn, TreeBuilderKind kind) {}

    private record TreeSignatureCache(Map<String, Collection<String>> treeSignatures, Map<String, Collection<TreeTargetHint>> treeTargetHints) {}

    private enum TreeBuilderKind {
        ROOT_METHOD,
        CONSTRUCTOR
    }

    /**
     * app/config[..].yml
     * app/config[..].yaml
     * config/packages/twig.yml
     * config/packages/twig.yaml
     */
    public static Collection<VirtualFile> getConfigurations(Project project, String packageName) {
        Collection<String[]> paths = Arrays.asList(
            new String[] {"config", "packages", packageName +".yml"},
            new String[] {"config", "packages", packageName +".yaml"},
            new String[] {"config", "packages", packageName, "config.yaml"},
            new String[] {"config", "packages", packageName, "config.yml"}
        );

        Collection<VirtualFile> virtualFiles = new HashSet<>();

        for (String[] path : paths) {
            VirtualFile configFile = VirtualFileUtil.findRelativeFile(ProjectUtil.getProjectDir(project), path);
            if(configFile != null) {
                virtualFiles.add(configFile);
            }
        }

        // note
        for (VirtualFile virtualFile : FilesystemUtil.getAppDirectories(project)) {
            VirtualFile configDir = VirtualFileUtil.findRelativeFile(virtualFile, "config");
            if(configDir != null) {
                for (VirtualFile configFile : configDir.getChildren()) {
                    // app/config/config*yml
                    if(configFile.getFileType() == YAMLFileType.YML && configFile.getName().startsWith("config")) {
                        virtualFiles.add(configFile);
                    }
                }
            }
        }

        return virtualFiles;
    }
}
