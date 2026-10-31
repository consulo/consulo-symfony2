package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.index.io.DataIndexer;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.ID;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import consulo.xml.language.psi.XmlTag;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigComponentUsageStubIndex extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_component_usages");
    private static final KeyDescriptor<String> KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private static final DataExternalizer<Set<String>> DATA_EXTERNALIZER = new StringSetDataExternalizer();

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Set<String>> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if (!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !(psiFile instanceof TwigFile twigFile)) {
                return map;
            }

            for (Usage usage : getComponentUsages(twigFile)) {
                String normalized = normalizeComponentName(usage.componentName());
                if (normalized == null) {
                    continue;
                }

                map.putIfAbsent(normalized, new HashSet<>());
                map.get(normalized).add(usage.type().name().toLowerCase(Locale.ROOT));
            }

            return map;
        };
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public static Collection<PsiElement> getComponentUsages(TwigFile twigFile, Collection<String> componentNames) {
        Set<String> normalizedComponentNames = new HashSet<>();
        for (String componentName : componentNames) {
            String normalized = normalizeComponentName(componentName);
            if (normalized != null) {
                normalizedComponentNames.add(normalized);
            }
        }

        if (normalizedComponentNames.isEmpty()) {
            return List.of();
        }

        Set<PsiElement> targets = new LinkedHashSet<>();

        for (Usage usage : getComponentUsages(twigFile)) {
            String normalized = normalizeComponentName(usage.componentName());
            if (normalized == null || !normalizedComponentNames.contains(normalized)) {
                continue;
            }

            targets.add(usage.target());
        }

        return targets;
    }

    public static String normalizeComponentName(String name) {
        String trimmed = name.trim();
        if (trimmed.isBlank()) {
            return null;
        }

        return trimmed;
    }

    private static Collection<Usage> getComponentUsages(TwigFile twigFile) {
        Set<Usage> usages = new LinkedHashSet<>();

        ElementPattern<PsiElement> componentPattern = TwigPattern.getComponentPattern();
        twigFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element.getNode() != null && element.getNode().getElementType() == TwigTokenTypes.STRING_TEXT && componentPattern.accepts(element)) {
                    String componentName = PsiElementUtils.trimQuote(element.getText());
                    if (StringUtils.isNotBlank(componentName)) {
                        usages.add(new Usage(componentName, element, UsageType.FUNCTION));
                    }
                } else if (element.getNode() != null && element.getNode().getElementType() == TwigElementTypes.TAG) {
                    Usage usage = getTagComponentUsage(element);
                    if (usage != null) {
                        usages.add(usage);
                    }
                }

                super.visitElement(element);
            }
        });

        for (PsiFile psiFile : twigFile.getViewProvider().getAllFiles()) {
            for (XmlTag xmlTag : PsiTreeUtil.findChildrenOfType(psiFile, XmlTag.class)) {
                String componentName = getComponentName(xmlTag);
                if (componentName == null) {
                    continue;
                }

                usages.add(new Usage(componentName, xmlTag, UsageType.TAG));
            }
        }

        return usages;
    }

    @Nullable
    private static Usage getTagComponentUsage(PsiElement tag) {
        PsiElement tagStart = tag.getFirstChild();
        if (tagStart == null) {
            return null;
        }

        PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(tagStart, TwigTokenTypes.TAG_NAME);
        if (tagName == null || !"component".equals(tagName.getText())) {
            return null;
        }

        PsiElement componentElement = PsiTreeUtil.nextVisibleLeaf(tagName);
        if (componentElement == null) {
            return null;
        }

        String componentName = PsiElementUtils.trimQuote(componentElement.getText());
        if (StringUtils.isBlank(componentName)) {
            return null;
        }

        return new Usage(componentName, componentElement, UsageType.TAG);
    }

    @Nullable
    private static String getComponentName(XmlTag tag) {
        String name = tag.getName();
        if (StringUtils.isBlank(name)) {
            return null;
        }

        if (name.startsWith("twig:")) {
            String componentName = name.substring(5);
            return "block".equalsIgnoreCase(componentName) ? null : componentName;
        }

        if ("twig".equals(tag.getNamespacePrefix())) {
            return "block".equalsIgnoreCase(name) ? null : name;
        }

        return null;
    }

    private enum UsageType {
        TAG,
        FUNCTION
    }

    private record Usage(String componentName, PsiElement target, UsageType type) {
    }
}
