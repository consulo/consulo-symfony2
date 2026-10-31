package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;
import consulo.xml.language.psi.XmlToken;
import consulo.xml.language.psi.XmlTokenType;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.twig.TwigFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.util.lang.lazy.LazyValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ExtensionImpl
public class TwigHtmlLineMarkerProvider implements LineMarkerProvider {
    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> results) {
        if (psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.getFirst())) {
            return;
        }

        PsiFile file = psiElements.getFirst().getContainingFile();
        if (!isSupportedFile(file)) {
            return;
        }

        Map<String, Boolean> componentBlockExistsCache = new HashMap<>();

        for (PsiElement psiElement : psiElements) {
            if (!(psiElement instanceof XmlToken xmlToken) || xmlToken.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
                continue;
            }

            if (!(xmlToken.getParent() instanceof XmlAttributeValue xmlAttributeValue)) {
                continue;
            }

            LineMarkerInfo<?> blockOverride = attachTwigComponentBlockOverride(xmlAttributeValue, xmlToken, componentBlockExistsCache);
            if (blockOverride != null) {
                results.add(blockOverride);
            }
        }
    }

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    public Language getLanguage() {
        return HTMLLanguage.INSTANCE;
    }

    public static boolean isSupportedFile(@Nullable PsiFile file) {
        return file != null && file.getName().endsWith(".twig");
    }

    @Nullable
    protected LineMarkerInfo<?> attachTwigComponentBlockOverride(XmlAttributeValue xmlAttributeValue, PsiElement lineMarkerTarget, Map<String, Boolean> componentBlockExistsCache) {
        if (!(xmlAttributeValue.getParent() instanceof XmlAttribute xmlAttribute) || !"name".equals(xmlAttribute.getName())) {
            return null;
        }

        if (!(xmlAttribute.getParent() instanceof XmlTag xmlTag) || !TwigHtmlCompletionUtil.isTwigBlockTag(xmlTag)) {
            return null;
        }

        if (!isTwigBackedXmlTag(xmlTag)) {
            return null;
        }

        String blockName = xmlAttributeValue.getValue();
        if (blockName.isBlank()) {
            return null;
        }

        XmlTag componentTag = findParentComponentTag(xmlTag);
        if (componentTag == null) {
            return null;
        }

        String name = componentTag.getName();
        String rawComponentName = name.startsWith("twig:") ? name.substring(5) : name;

        String componentName = resolveComponentName(xmlAttributeValue.getProject(), rawComponentName);
        if (componentName == null) {
            return null;
        }

        Project project = xmlAttributeValue.getProject();
        String cacheKey = componentName + "\0" + blockName;
        if (!componentBlockExistsCache.computeIfAbsent(cacheKey, ignored -> hasComponentBlock(project, componentName, blockName))) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES)
            .setTargets(LazyValue.notNull(new TwigComponentBlockTargetsLazyValue(project, componentName, blockName)))
            .setTooltipText("Navigate to block");

        return builder.createLineMarkerInfo(lineMarkerTarget);
    }

    @Nullable
    protected LineMarkerInfo<?> attachTwigComponentBlockOverride(XmlAttributeValue xmlAttributeValue, PsiElement lineMarkerTarget) {
        return attachTwigComponentBlockOverride(xmlAttributeValue, lineMarkerTarget, new HashMap<>());
    }

    @Nullable
    protected String resolveComponentName(Project project, String rawComponentName) {
        return UxUtil.resolveTwigComponentName(project, rawComponentName);
    }

    protected boolean hasComponentBlock(Project project, String componentName, String blockName) {
        List<VirtualFile> virtualFiles = new ArrayList<>();
        for (PsiFile templateFile : UxUtil.getComponentTemplates(project, componentName)) {
            VirtualFile virtualFile = templateFile.getVirtualFile();
            if (virtualFile != null) {
                virtualFiles.add(virtualFile);
            }
        }

        if (virtualFiles.isEmpty()) {
            return false;
        }

        Map<VirtualFile, Collection<String>> blockNamesForFiles = TwigUtil.getBlockNamesForFiles(project, virtualFiles);
        for (Collection<String> blockNames : blockNamesForFiles.values()) {
            if (blockNames.contains(blockName)) {
                return true;
            }
        }

        return false;
    }

    protected boolean isTwigBackedXmlTag(XmlTag xmlTag) {
        PsiElement twigElement = TwigUtil.getElementOnTwigViewProvider(xmlTag);
        return twigElement != null && twigElement.getContainingFile() instanceof TwigFile;
    }

    @Nullable
    private static XmlTag findParentComponentTag(XmlTag tag) {
        XmlTag parentTag = tag.getParentTag();
        while (parentTag != null) {
            if (!TwigHtmlCompletionUtil.isTwigBlockTag(parentTag)) {
                String name = parentTag.getName();
                if (name.startsWith("twig:") || "twig".equals(parentTag.getNamespacePrefix())) {
                    return parentTag;
                }
            }

            parentTag = parentTag.getParentTag();
        }

        return null;
    }

    private record TwigComponentBlockTargetsLazyValue(Project project, String componentName, String blockName) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return TwigBlockUtil.getComponentBlockTargets(project, componentName, blockName);
        }
    }
}
