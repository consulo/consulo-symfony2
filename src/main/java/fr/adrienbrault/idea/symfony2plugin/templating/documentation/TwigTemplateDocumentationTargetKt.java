package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import consulo.application.progress.ProgressManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateInclude.TYPE;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Top-level declarations of the former TwigTemplateDocumentationTarget.kt
 */
public final class TwigTemplateDocumentationTargetKt {
    public static final String TEMPLATE_FIND_USAGES_LINK = "symfony-template-find-usages";

    private TwigTemplateDocumentationTargetKt() {
    }

    public static TwigTemplateDocumentationData collectTwigTemplateDocumentation(TwigTemplateDocumentationContext context) {
        Project project = context.getAnchor().getProject();
        FileBasedIndex index = FileBasedIndex.getInstance();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        Collection<VirtualFile> targets = TwigUtil.getTemplateFiles(project, context.getName());

        Set<String> names = new LinkedHashSet<>();
        names.add(context.getName());
        for (VirtualFile target : targets) {
            names.addAll(TwigUtil.getTemplateNamesForFile(project, target));
        }

        // The PHP usage index retains the spelling; the Twig indices normalize their keys.
        for (String name : new ArrayList<>(names)) {
            names.add(TwigUtil.normalizeTemplateName(name));
            if (name.startsWith("@") && !name.startsWith("@!")) {
                names.add("@!" + name.substring(1));
            }
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String name : names) {
            normalized.add(TwigUtil.normalizeTemplateName(name));
        }

        Set<String> phpFiles = new LinkedHashSet<>();
        Set<String> callers = new LinkedHashSet<>();

        for (String name : names) {
            ProgressManager.checkCanceled();
            index.processValues(PhpTwigTemplateUsageStubIndex.KEY, name, null, (file, usage) -> {
                ProgressManager.checkCanceled();
                phpFiles.add(file.getUrl());
                for (String usageScope : usage.getScopes()) {
                    callers.add(presentScope(usageScope));
                }
                return true;
            }, scope);
        }

        Map<String, Set<String>> usages = new LinkedHashMap<>();

        for (String name : normalized) {
            ProgressManager.checkCanceled();
            index.processValues(TwigIncludeStubIndex.KEY, name, null, (file, include) -> {
                for (TYPE includeType : include.getTypes()) {
                    String type = switch (includeType) {
                        case INCLUDE -> "include";
                        case INCLUDE_FUNCTION -> "include()";
                        case SOURCE_FUNCTION -> "source()";
                        case EMBED -> "embed";
                        case IMPORT -> "import";
                        case FROM -> "from";
                        case FORM_THEME -> "form_theme";
                        case BLOCK_FUNCTION -> "block";
                    };

                    usages.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(file.getUrl());
                }
                return true;
            }, scope);

            for (VirtualFile file : index.getContainingFiles(TwigExtendsStubIndex.KEY, name, scope)) {
                usages.computeIfAbsent("extends", k -> new LinkedHashSet<>()).add(file.getUrl());
            }
        }

        Map<String, Set<String>> parents = new LinkedHashMap<>();
        for (VirtualFile file : targets) {
            ProgressManager.checkCanceled();
            parents.put(file.getUrl(), new LinkedHashSet<>(index.getFileData(TwigExtendsStubIndex.KEY, file, project).keySet()));
        }

        Map<String, String> paths = new LinkedHashMap<>();
        for (VirtualFile file : targets) {
            String relativePath = VfsExUtil.getRelativeProjectPath(project, file);
            paths.put(file.getUrl(), relativePath != null ? relativePath : file.getPresentableUrl());
        }

        Map<String, Set<String>> twigUsages = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : usages.entrySet()) {
            twigUsages.put(entry.getKey(), new LinkedHashSet<>(entry.getValue()));
        }

        return new TwigTemplateDocumentationData(
            context.getName(),
            paths,
            parents,
            new LinkedHashSet<>(phpFiles),
            new LinkedHashSet<>(callers),
            twigUsages
        );
    }

    private static String presentScope(String scope) {
        int start = 0;
        while (start < scope.length() && scope.charAt(start) == '\\') {
            start++;
        }

        String name = scope.substring(start);
        int separator = name.lastIndexOf('.');

        return separator < 0 ? name : name.substring(0, separator) + "::" + name.substring(separator + 1);
    }
}
