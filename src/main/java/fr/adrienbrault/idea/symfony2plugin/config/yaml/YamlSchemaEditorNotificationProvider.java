package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.document.Document;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.language.editor.WriteCommandAction;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import fr.adrienbrault.idea.symfony2plugin.SymfonyProjectState;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import icons.SymfonyIcons;
import jakarta.inject.Inject;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shows a notification banner at the top of YAML service configuration files
 * suggesting to add the new Symfony 7.4 JSON schema hint for better autocompletion.

 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlSchemaEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
    private static final String SCHEMA_COMMENT_PREFIX = "# yaml-language-server: $schema=";
    private static final String SCHEMA_COMMENT_MARKER = "yaml-language-server";
    private static final String SCHEMA_FILENAME = "services.schema.json";

    private final Project myProject;

    @Inject
    public YamlSchemaEditorNotificationProvider(Project project) {
        myProject = project;
    }

    @Override
    public String getId() {
        return "symfony.yaml.schema";
    }

    @RequiredReadAction
    @Override
    public @Nullable EditorNotificationBuilder buildNotification(VirtualFile file, FileEditor fileEditor, Supplier<EditorNotificationBuilder> builderFactory) {
        Project project = myProject;

        // Only process if Symfony plugin is enabled
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return null;
        }

        // Check if user has dismissed this notification
        if (SymfonyProjectState.getInstance(project).dismissYamlSchemaNotification) {
            return null;
        }

        // Only process YAML files
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof YAMLFile)) {
            return null;
        }

        // Check if this looks like a Symfony service configuration file
        if (!isServiceConfigurationFile((YAMLFile) psiFile)) {
            return null;
        }

        // Check if schema hint is already present by searching for PsiComment nodes
        if (hasYamlLanguageServerComment(psiFile)) {
            return null;
        }

        // Only show notification if schema file exists in the project
        if (!hasSchemaPath(project)) {
            return null;
        }

        // Create the notification panel
        return createNotificationPanel(project, psiFile, builderFactory.get());
    }

    /**
     * Checks if the file has a yaml-language-server comment anywhere in the file
     */
    private boolean hasYamlLanguageServerComment(PsiFile psiFile) {
        // Use PsiTreeUtil to find all PsiComment elements at any level
        Collection<PsiComment> comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment.class);
        for (PsiComment comment : comments) {
            String commentText = comment.getText();
            if (commentText != null && commentText.contains(SCHEMA_COMMENT_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given YAML file is a Symfony service configuration file
     * by checking for "services" or "parameters" root keys in the content
     */
    private boolean isServiceConfigurationFile(YAMLFile yamlFile) {
        VirtualFile virtualFile = yamlFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        String name = virtualFile.getName();

        // docker-compose.yml is also having "services" in root: ignore it
        if (name.contains("-compose")) {
            return false;
        }

        // Content validation: check for "services" or "parameters" or "imports" root keys
        List<YAMLDocument> documents = yamlFile.getDocuments();
        if (documents.isEmpty()) {
            return false;
        }

        YAMLDocument first = documents.getFirst();
        if (first != null && first.getTopLevelValue() instanceof YAMLMapping mapping) {
            for (YAMLKeyValue keyValue : mapping.getKeyValues()) {
                String keyText = keyValue.getKeyText();
                if ("services".equals(keyText) || "parameters".equals(keyText) || "imports".equals(keyText)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates the notification panel with action buttons
     */
    private EditorNotificationBuilder createNotificationPanel(Project project, PsiFile psiFile, EditorNotificationBuilder panel) {
        panel.asInfo();
        panel.withIcon(SymfonyIcons.Symfony);

        panel.withText(LocalizeValue.localizeTODO("Add YAML schema hint for better Symfony service autocompletion?"));

        // "Add Schema" button
        panel.withAction(LocalizeValue.localizeTODO("Add Schema Hint"), e -> {
            String schemaPath = findSchemaPath(project, psiFile);
            if (schemaPath != null) {
                addSchemaHint(psiFile, schemaPath);
                // Update the editors to remove the notification
                EditorNotifications.getInstance(project).updateAllNotifications();
            }
        });

        // "Ignore" button
        panel.withAction(LocalizeValue.localizeTODO("Don't Show Again"), e -> {
            SymfonyProjectState.getInstance(project).dismissYamlSchemaNotification = true;
            // Update the editors to remove the notification
            EditorNotifications.getInstance(project).updateAllNotifications();
        });

        return panel;
    }

    private boolean hasSchemaPath(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            () -> {
                Collection<VirtualFile> schemaFiles = FilenameIndex.getVirtualFilesByName(
                    project,
                    SCHEMA_FILENAME,
                    GlobalSearchScope.allScope(project)
                );

                return CachedValueProvider.Result.create(!schemaFiles.isEmpty(), VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
            }
        );
    }

    /**
     * Finds the schema file and returns the relative path from the current file.
     * Prioritizes vendor directory files.
     */
    private @Nullable String findSchemaPath(Project project, PsiFile currentVirtualFile) {
        VirtualFile virtualFile = currentVirtualFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Collection<VirtualFile> schemaFiles = FilenameIndex.getVirtualFilesByName(
            project,
            SCHEMA_FILENAME,
            GlobalSearchScope.allScope(project)
        );

        if (schemaFiles.isEmpty()) {
            return null;
        }

        // Filter and sort schema files: only use files in Loader/schema/, prioritize vendor directory
        List<VirtualFile> sortedFiles = schemaFiles.stream()
            .filter(file -> file.getPath().contains("/Loader/schema/"))
            .sorted(Comparator.comparing((VirtualFile file) -> {
                String path = file.getPath();
                // Vendor files get priority (lower number = higher priority)
                if (path.contains("/vendor/symfony/dependency-injection/")) {
                    return 0;
                } else if (path.contains("/vendor/")) {
                    return 1;
                }
                return 2;
            }))
            .toList();

        if (sortedFiles.isEmpty()) {
            return null;
        }

        for (VirtualFile sortedFile : sortedFiles) {
            String s = calculateRelativePath(virtualFile, sortedFile);
            if (s != null) {
                return s;
            }
        }

        return null;
    }


    /**
     * Calculates the relative path from the current file to the schema file
     */
    private @Nullable String calculateRelativePath(VirtualFile from, VirtualFile to) {
        try {
            Path fromPath = Paths.get(from.getParent().getPath());
            Path toPath = Paths.get(to.getPath());
            Path relativePath = fromPath.relativize(toPath);
            return relativePath.toString().replace('\\', '/');
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Adds the schema hint comment to the top of the file
     */
    private void addSchemaHint(PsiFile psiFile, String schemaPath) {
        WriteCommandAction.runWriteCommandAction(psiFile.getProject(), () -> {
            Document document =
                PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);

            if (document != null) {
                String schemaComment = SCHEMA_COMMENT_PREFIX + schemaPath;
                String newContent = schemaComment + "\n" + document.getText();
                document.setText(newContent);
            }
        });
    }
}
