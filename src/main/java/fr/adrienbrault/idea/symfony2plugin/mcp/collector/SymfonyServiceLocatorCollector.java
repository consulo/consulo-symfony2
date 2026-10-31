package fr.adrienbrault.idea.symfony2plugin.mcp.collector;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SymfonyServiceLocatorCollector {
    private final Project myProject;

    public SymfonyServiceLocatorCollector(Project project) {
        myProject = project;
    }

    public String collect(String identifier) {
        StringBuilder sb = new StringBuilder();
        List<String> sections = buildServiceSections(identifier);

        if (sections.isEmpty()) {
            sb.append("No service found for: ").append(identifier);
            return sb.toString();
        }

        for (int index = 0; index < sections.size(); index++) {
            sb.append(sections.get(index));
            if (index < sections.size() - 1) {
                sb.append('\n');
                sb.append("---").append('\n');
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private List<String> buildServiceSections(String identifier) {
        Map<String, ContainerService> services = ContainerCollectionResolver.getServices(myProject);
        List<String> sections = new ArrayList<>();
        Set<String> visitedServiceNames = new LinkedHashSet<>();
        boolean isClassName = identifier.contains("\\");

        if (isClassName) {
            String strippedIdentifier = StringUtils.stripStart(identifier, "\\");
            String normalizedIdentifier = "\\" + strippedIdentifier;
            ContainerCollectionResolver.ServiceCollector serviceCollector = ContainerCollectionResolver.ServiceCollector.create(myProject);

            for (String serviceName : serviceCollector.convertClassNameToServices(normalizedIdentifier)) {
                if (!visitedServiceNames.add(serviceName)) {
                    continue;
                }

                ContainerService containerService = findContainerService(services, serviceName);
                String className = containerService != null && containerService.getClassName() != null ? containerService.getClassName() : normalizedIdentifier;
                String section = buildSection(serviceName, className, ServiceIndexUtil.findServiceDefinitionBlocks(myProject, serviceName), containerService);
                if (section != null) {
                    sections.add(section);
                }
            }

            String directServiceName = strippedIdentifier;
            List<PsiElement> directElements = ServiceIndexUtil.findServiceDefinitionBlocks(myProject, directServiceName);
            if (!directElements.isEmpty() && visitedServiceNames.add(directServiceName)) {
                ContainerService containerService = findContainerService(services, directServiceName);
                String className = containerService != null && containerService.getClassName() != null ? containerService.getClassName() : normalizedIdentifier;
                String section = buildSection(directServiceName, className, directElements, containerService);
                if (section != null) {
                    sections.add(section);
                }
            }
        }
        else {
            ContainerService containerService = findContainerService(services, identifier);
            String className = containerService != null && containerService.getClassName() != null ? containerService.getClassName() : "";
            String section = buildSection(identifier, className, ServiceIndexUtil.findServiceDefinitionBlocks(myProject, identifier), containerService);
            if (section != null) {
                sections.add(section);
            }
        }

        return sections;
    }

    private @Nullable ContainerService findContainerService(
        Map<String, ContainerService> services,
        String serviceName
    ) {
        String lower = serviceName.toLowerCase(Locale.ROOT);
        ContainerService containerService = services.get(lower);
        if (containerService != null) {
            return containerService;
        }

        containerService = services.get(StringUtils.stripStart(serviceName, "\\").toLowerCase(Locale.ROOT));
        if (containerService != null) {
            return containerService;
        }

        return services.get("\\" + StringUtils.stripStart(serviceName, "\\").toLowerCase(Locale.ROOT));
    }

    private @Nullable String buildSection(
        String serviceName,
        String className,
        List<PsiElement> elements,
        @Nullable ContainerService containerService
    ) {
        List<String> fileBlocks = new ArrayList<>();
        for (PsiElement element : elements) {
            String block = buildFileBlock(element);
            if (StringUtils.isNotBlank(block)) {
                fileBlocks.add(block);
            }
        }
        Set<String> resourceBlocks = new LinkedHashSet<>();

        if (fileBlocks.isEmpty() && containerService != null) {
            for (String resourceServiceId : containerService.getResourceServiceIds()) {
                for (PsiElement resourceElement : ServiceIndexUtil.findServiceDefinitionBlocks(myProject, resourceServiceId)) {
                    String block = buildFileBlock(resourceElement);
                    if (StringUtils.isNotBlank(block)) {
                        resourceBlocks.add(block);
                    }
                }
            }
        }

        if (fileBlocks.isEmpty() && resourceBlocks.isEmpty() && containerService == null) {
            return null;
        }

        List<String> resourceBlockList = new ArrayList<>(resourceBlocks);
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(serviceName).append('\n');
        sb.append('\n');

        if (!fileBlocks.isEmpty()) {
            for (int index = 0; index < fileBlocks.size(); index++) {
                sb.append(fileBlocks.get(index));
                if (index < fileBlocks.size() - 1) {
                    sb.append('\n');
                    sb.append('\n');
                }
            }
        }
        else if (!resourceBlockList.isEmpty()) {
            sb.append("[AUTOWIRED] Auto-registered via resource/prototype definition.").append('\n');
            sb.append('\n');
            for (int index = 0; index < resourceBlockList.size(); index++) {
                sb.append(resourceBlockList.get(index));
                if (index < resourceBlockList.size() - 1) {
                    sb.append('\n');
                    sb.append('\n');
                }
            }
        }
        else if (containerService != null && (containerService.isAutowireEnabled() || containerService.hasResourcePrototypeMetadata())) {
            sb.append("[AUTOWIRED] Auto-registered; no explicit source block was resolved.").append('\n');
        }
        else {
            return null;
        }

        return sb.toString();
    }

    private String buildFileBlock(PsiElement element) {
        StringBuilder sb = new StringBuilder();
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null) {
            return sb.toString();
        }
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null) {
            return sb.toString();
        }
        String relativePath = McpPathUtil.INSTANCE.getRelativeProjectPath(myProject, virtualFile);
        if (StringUtils.isBlank(relativePath)) {
            return sb.toString();
        }

        Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
        if (document == null) {
            return sb.toString();
        }
        TextRange blockRange = resolveBlockTextRange(element);
        int startLine = document.getLineNumber(blockRange.getStartOffset());
        int endLine = document.getLineNumber(Math.max(blockRange.getEndOffset(), blockRange.getStartOffset() + 1) - 1);
        int lineNumberWidth = String.valueOf(endLine + 1).length();

        sb.append("File: ").append(relativePath).append('\n');

        for (int lineIndex = startLine; lineIndex <= endLine; lineIndex++) {
            int lineStartOffset = document.getLineStartOffset(lineIndex);
            int lineEndOffset = document.getLineEndOffset(lineIndex);
            String lineText = document.getText(new TextRange(lineStartOffset, lineEndOffset));
            if (lineText.length() > 120) {
                lineText = lineText.substring(0, 120) + "...";
            }

            String lineNumber = StringUtils.leftPad(String.valueOf(lineIndex + 1), lineNumberWidth);
            sb.append(lineNumber).append(": ").append(lineText).append('\n');
        }

        return sb.toString();
    }

    private TextRange resolveBlockTextRange(PsiElement element) {
        if (element instanceof YAMLKeyValue) {
            return element.getTextRange();
        }
        else if (element instanceof XmlTag) {
            return element.getTextRange();
        }
        else {
            return element.getTextRange();
        }
    }
}
