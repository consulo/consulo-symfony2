package fr.adrienbrault.idea.symfony2plugin.templating;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.pattern.ElementPattern;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.navigation.GotoRelatedItem;
import consulo.project.Project;
import consulo.component.util.Iconable;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlTag;
import consulo.language.psi.stub.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.ui.image.Image;
import consulo.util.lang.lazy.LazyValue;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormFieldDataHolder;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormViewDataHolder;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileImplementsLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileOverwritesLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.FileLineMarkerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import com.jetbrains.twig.icons.TwigIcons;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigLineMarkerProvider implements LineMarkerProvider {
    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> results) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        Map<VirtualFile, FileImplementsLazyLoader> implementsMap = new HashMap<>();
        FileOverwritesLazyLoader fileOverwritesLazyLoader = null;

        var blockTagPattern = TwigPattern.getBlockTagPattern();
        var blockFunctionPattern = TwigPattern.getPrintBlockOrTagFunctionPattern("block");
        var formFunctionPattern = TwigPattern.getLeafFunctionPattern("form_start", "form", "form_end", "form_rest");

        for(PsiElement psiElement: psiElements) {
            // controller
            if(psiElement instanceof TwigFile twigFile) {
                attachController(twigFile, results);
                attachUxComponentTargets(twigFile, results);

                // find foreign file references tags like:
                // include, embed, source, from, import, ...
                LineMarkerInfo<?> lineIncludes = attachIncludes(twigFile);
                if(lineIncludes != null) {
                    results.add(lineIncludes);
                }

                LineMarkerInfo<?> extending = attachExtends(twigFile);
                if(extending != null) {
                    results.add(extending);
                }

                // eg bundle overwrites
                LineMarkerInfo<?> overwrites = attachOverwrites(twigFile);
                if(overwrites != null) {
                    results.add(overwrites);
                }
            } else if (blockTagPattern.accepts(psiElement) || blockFunctionPattern.accepts(psiElement)) {
                // blocks: {% block 'foobar' %}, {{ block('foobar') }}

                VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                if(!implementsMap.containsKey(virtualFile)) {
                    implementsMap.put(virtualFile, new FileImplementsLazyLoader(psiElement.getProject(), virtualFile));
                }

                LineMarkerInfo<?> lineImpl = attachBlockImplements(psiElement, implementsMap.get(virtualFile));
                if(lineImpl != null) {
                    results.add(lineImpl);
                }

                if(fileOverwritesLazyLoader == null) {
                    fileOverwritesLazyLoader = new FileOverwritesLazyLoader(psiElements.get(0).getProject());
                }

                LineMarkerInfo<?> lineOverwrites = attachBlockOverwrites(psiElement, fileOverwritesLazyLoader);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }
            } else if(formFunctionPattern.accepts(psiElement)) {
                PsiElement parent = psiElement.getParent();

                LineMarkerInfo<?> lineOverwrites = attachFormType(parent);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }
            }
        }
    }

    private void attachController(TwigFile twigFile, Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        Set<Function> methods = new HashSet<>();


        methods.addAll(TwigUtil.findTwigFileController(twigFile));
        methods.addAll(TwigUtil.getTwigFileMethodUsageOnIndex(twigFile));

        if(methods.isEmpty()) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(methods).
            setTooltipText("Navigate to controller");

        result.add(FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile));
    }

    private void attachUxComponentTargets(TwigFile twigFile, Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        Collection<PhpClass> uxComponentsClasses = UxUtil.getComponentClassesForTemplateFile(twigFile.getProject(), twigFile);

        // Fast pre-check before creating lazy targets:
        // if neither direct class mapping nor indexed usages exist we skip the file-level marker.
        if (uxComponentsClasses.isEmpty() && !UxUtil.hasComponentUsages(twigFile)) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
            .setTargets(LazyValue.notNull(new UxComponentTemplateTargets(twigFile)))
            .setTooltipText("Navigate to UX Component");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); UxComponentTargetsPsiElementListCellRenderer is kept but unused
            // .setCellRenderer(UxComponentTargetsPsiElementListCellRenderer::new);

        result.add(FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile));
    }

    @Nullable
    private LineMarkerInfo<?> attachIncludes(TwigFile twigFile) {
        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(twigFile);

        boolean found = false;
        for(String templateName: templateNames) {
            Project project = twigFile.getProject();

            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigIncludeStubIndex.KEY, templateName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
            );

            // stop on first target, we load them lazily afterwards
            if(!containingFiles.isEmpty()) {
                found = true;
                break;
            }
        }

        if(!found) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(LazyValue.notNull(new MyTemplateIncludeLazyValue(twigFile, templateNames)))
            .setTooltipText("Navigate to includes");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); MyFileReferencePsiElementListCellRenderer is kept but unused
            // .setCellRenderer(MyFileReferencePsiElementListCellRenderer::new);

        return FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile);
    }

    @Nullable
    private LineMarkerInfo<?> attachExtends(TwigFile twigFile) {
        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(twigFile);

        boolean found = false;
        for(String templateName: templateNames) {
            Project project = twigFile.getProject();

            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigExtendsStubIndex.KEY, templateName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
            );

            // stop on first target, we load them lazily afterwards
            if(!containingFiles.isEmpty()) {
                found = true;
                break;
            }
        }

        if(!found) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(LazyValue.notNull(new TemplateExtendsLazyTargets(twigFile.getProject(), twigFile.getVirtualFile())))
            .setTooltipText("Navigate to extends");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); MyFileReferencePsiElementListCellRenderer is kept but unused
            // .setCellRenderer(MyFileReferencePsiElementListCellRenderer::new);

        return FileLineMarkerUtil.createLineMarkerInfo(builder, twigFile);
    }

    @Nullable
    private LineMarkerInfo<?> attachOverwrites(TwigFile twigFile) {
        Collection<PsiFile> targets = new ArrayList<>();

        for (String templateName: TwigUtil.getTemplateNamesForFile(twigFile)) {
            for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(twigFile.getProject(), templateName)) {
                if(!psiFile.getVirtualFile().equals(twigFile.getVirtualFile()) && !targets.contains(psiFile)) {
                    targets.add(psiFile);
                }
            }
        }

        if(targets.isEmpty()) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        for(PsiElement blockTag: targets) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(
                blockTag,
                TwigUtil.getPresentableTemplateName(blockTag, true)
            ).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_OVERWRITE));
        }

        return getRelatedPopover("Overwrites", "Overwrite", twigFile, gotoRelatedItems, Symfony2Icons.TWIG_LINE_OVERWRITE, "Go to the overwritten template");
    }

    private LineMarkerInfo<?> getRelatedPopover(String singleItemTitle, String singleItemTooltipPrefix, PsiElement lineMarkerTarget, List<GotoRelatedItem> gotoRelatedItems, Image icon, String accessibleName) {

        // single item has no popup
        String title = singleItemTitle;
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = String.format(singleItemTooltipPrefix, customName);
            }
        }

        return FileLineMarkerUtil.createLineMarkerInfo(
            lineMarkerTarget,
            icon,
            title,
            new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems),
            () -> accessibleName
        );
    }

    @Nullable
    private LineMarkerInfo<?> attachBlockImplements(PsiElement psiElement, FileImplementsLazyLoader implementsLazyLoader) {
        if(!TwigBlockUtil.hasBlockImplementations(psiElement, implementsLazyLoader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(LazyValue.notNull(new BlockImplementationLazyValue(psiElement)))
            .setTooltipText("Implementations");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); MyBlockListCellRenderer is kept but unused
            // .setCellRenderer(MyBlockListCellRenderer::new);

        return builder.createLineMarkerInfo(psiElement);
    }

    private record BlockImplementationLazyValue(PsiElement psiElement) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return TwigBlockUtil.getBlockImplementationTargets(psiElement);
        }
    }

    /**
     * Provides lazy targets for given template scope
     */
    private record BlockOverwriteLazyValue(PsiElement psiElement) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return TwigBlockUtil.getBlockOverwriteTargets(psiElement);
        }
    }

    @Nullable
    private LineMarkerInfo<?> attachBlockOverwrites(PsiElement psiElement, FileOverwritesLazyLoader loader) {
        if(!TwigBlockUtil.hasBlockOverwrites(psiElement, loader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES)
            .setTargets(LazyValue.notNull(new BlockOverwriteLazyValue(psiElement)))
            .setTooltipText("Overwrites");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); MyBlockListCellRenderer is kept but unused
            // .setCellRenderer(MyBlockListCellRenderer::new);

        return builder.createLineMarkerInfo(psiElement);
    }

    @Nullable
    private LineMarkerInfo<?> attachFormType(PsiElement psiElement) {
        // form(theform);
        PsiElement twigFunctionParameterIdentifierPsi = TwigUtil.getTwigFunctionParameterIdentifierPsi(psiElement);
        if (twigFunctionParameterIdentifierPsi == null) {
            return null;
        }

        Collection<TwigTypeContainer> twigTypeContainers = TwigTypeResolveUtil.resolveTwigMethodName(twigFunctionParameterIdentifierPsi, TwigTypeResolveUtil.formatPsiTypeNameWithCurrent(twigFunctionParameterIdentifierPsi));

        Set<String> formTypes = new HashSet<>();
        Set<String> fieldOwners = new HashSet<>();
        for (TwigTypeContainer twigTypeContainer : twigTypeContainers) {
            FormViewDataHolder formViewDataHolder = twigTypeContainer.getFormViewDataHolder();
            if (formViewDataHolder != null) {
                formTypes.addAll(formViewDataHolder.formTypeFqns());
            }

            FormFieldDataHolder formFieldDataHolder = twigTypeContainer.getFormFieldDataHolder();
            if (formFieldDataHolder != null) {
                if (twigTypeContainer.getStringElement() == null && formFieldDataHolder.fieldTypeFqn() != null) {
                    formTypes.add(formFieldDataHolder.fieldTypeFqn());
                } else {
                    fieldOwners.add(formFieldDataHolder.ownerFormTypeFqn());
                }
            }
        }

        // Use field owners only when the form view itself has no type metadata.
        if (formTypes.isEmpty()) {
            formTypes.addAll(fieldOwners);
        }

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String formType : formTypes) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), formType);
            if (phpClass != null && FormUtil.isFormType(phpClass)) {
                phpClasses.add(phpClass);
            }
        }

        if (phpClasses.isEmpty()) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER)
            .setTargets(phpClasses)
            .setTooltipText("Navigate to Form");
            // TODO consulo-port: NavigationGutterIconBuilder has no setCellRenderer (closest: setPresentationProvider); MyBlockListCellRenderer is kept but unused
            // .setCellRenderer(MyBlockListCellRenderer::new);

        return builder.createLineMarkerInfo(psiElement.getFirstChild());
    }

    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    public Language getLanguage() {
        return TwigLanguage.INSTANCE;
    }

    private static class MyFileReferencePsiElementListCellRenderer extends PsiElementListCellRenderer<PsiElement> {
        @Override
        public String getElementText(PsiElement psiElement) {
            String symbolPresentableText = SymbolPresentationUtil.getSymbolPresentableText(psiElement);
            return StringUtils.abbreviate(symbolPresentableText, 50);
        }

        @Nullable
        @Override
        protected String getContainerText(PsiElement psiElement, String s) {
            // relative path else fallback to default name extraction
            PsiFile containingFile = psiElement.getContainingFile();
            String relativePath = VirtualFileUtil.getRelativePath(containingFile.getVirtualFile(), ProjectUtil.getProjectDir(psiElement), '/');
            return relativePath != null ? relativePath : SymbolPresentationUtil.getSymbolContainerText(psiElement);
        }

        @Override
        protected int getIconFlags() {
            return Iconable.ICON_FLAG_VISIBILITY;
        }

        @Override
        protected Image getIcon(PsiElement psiElement) {
            if(psiElement.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                return PhpIcons.IMPLEMENTED;
            } else if(psiElement.getNode().getElementType() == TwigElementTypes.EMBED_TAG) {
                return PhpIcons.OVERRIDEN;
            }

            return TwigIcons.TwigFileIcon;
        }
    }

    public static class UxComponentTargetsPsiElementListCellRenderer extends PsiElementListCellRenderer<PsiElement> {
        private ElementPattern<PsiElement> componentPattern;
        private ElementPattern<PsiElement> argumentAfterTagPattern;

        @Override
        public String getElementText(PsiElement psiElement) {
            if (psiElement instanceof PhpClass) {
                return StringUtils.abbreviate(SymbolPresentationUtil.getSymbolPresentableText(psiElement), 50);
            }

            String componentName = getComponentName(psiElement);
            if (componentName != null) {
                return componentName;
            }

            return StringUtils.abbreviate(SymbolPresentationUtil.getSymbolPresentableText(psiElement), 50);
        }

        @Nullable
        @Override
        protected String getContainerText(PsiElement psiElement, String s) {
            if (!(psiElement instanceof PhpClass) && getComponentName(psiElement) != null) {
                PsiFile containingFile = psiElement.getContainingFile();
                if (containingFile != null) {
                    String relativePath = VirtualFileUtil.getRelativePath(containingFile.getVirtualFile(), ProjectUtil.getProjectDir(psiElement), '/');
                    if (relativePath != null) {
                        return relativePath;
                    }
                }
            }

            return SymbolPresentationUtil.getSymbolContainerText(psiElement);
        }

        @Override
        protected int getIconFlags() {
            return Iconable.ICON_FLAG_VISIBILITY;
        }

        @Nullable
        private String getComponentName(PsiElement psiElement) {
            if (psiElement instanceof XmlTag xmlTag) {
                String name = xmlTag.getName();
                if (name.startsWith("twig:")) {
                    return name.substring(5);
                }

                if ("twig".equals(xmlTag.getNamespacePrefix())) {
                    return name;
                }
            }

            if (getComponentPattern().accepts(psiElement) || getArgumentAfterTagPattern().accepts(psiElement)) {
                String componentName = PsiElementUtils.trimQuote(psiElement.getText());
                return StringUtils.isBlank(componentName) ? null : componentName;
            }

            PsiElement previousVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(psiElement);
            if (previousVisibleLeaf != null && previousVisibleLeaf.getNode() != null && previousVisibleLeaf.getNode().getElementType() == TwigTokenTypes.TAG_NAME &&
                "component".equals(previousVisibleLeaf.getText()) &&
                psiElement.getParent() != null && psiElement.getParent().getNode() != null &&
                psiElement.getParent().getNode().getElementType() == TwigElementTypes.TAG) {
                String componentName = PsiElementUtils.trimQuote(psiElement.getText());
                return StringUtils.isBlank(componentName) ? null : componentName;
            }

            return null;
        }

        private ElementPattern<PsiElement> getComponentPattern() {
            return componentPattern != null ? componentPattern : (componentPattern = TwigPattern.getComponentPattern());
        }

        private ElementPattern<PsiElement> getArgumentAfterTagPattern() {
            return argumentAfterTagPattern != null ? argumentAfterTagPattern : (argumentAfterTagPattern = TwigPattern.getArgumentAfterTagNamePattern("component"));
        }
    }

    private record MyTemplateIncludeLazyValue(TwigFile twigFile, Collection<String> templateNames) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            Collection<VirtualFile> twigFiles = new ArrayList<>();

            Project project = twigFile.getProject();

            for (String templateName : this.templateNames) {
                // collect files which contains given template name for inclusion
                twigFiles.addAll(FileBasedIndex.getInstance().getContainingFiles(
                    TwigIncludeStubIndex.KEY,
                    templateName,
                    GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE))
                );
            }

            Collection<PsiElement> targets = new ArrayList<>();

            for (VirtualFile virtualFile : twigFiles) {
                // resolve virtual file
                PsiFile myTwigFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (!(myTwigFile instanceof TwigFile)) {
                    continue;
                }

                Collection<PsiElement> fileTargets = new ArrayList<>();

                TwigUtil.visitTemplateIncludes((TwigFile) myTwigFile, templateInclude -> {
                        if (this.templateNames.contains(templateInclude.getTemplateName()) || this.templateNames.contains(TwigUtil.normalizeTemplateName(templateInclude.getTemplateName()))) {
                            fileTargets.add(templateInclude.getPsiElement());
                        }
                    }
                );

                // navigate to include pattern; else fallback to file scope
                if (!fileTargets.isEmpty()) {
                    targets.addAll(fileTargets);
                } else {
                    targets.add(myTwigFile);
                }
            }

            return targets;
        }
    }

    private static class MyBlockListCellRenderer extends PsiElementListCellRenderer<PsiElement> {
        @Override
        public String getElementText(PsiElement psiElement) {
            return StringUtils.abbreviate(
                SymbolPresentationUtil.getSymbolPresentableText(psiElement),
                50
            );
        }

        @Override
        protected String getContainerText(PsiElement psiElement, String s) {
            return TwigUtil.getPresentableTemplateName(psiElement, true);
        }

        @Override
        protected int getIconFlags() {
            return Iconable.ICON_FLAG_VISIBILITY;
        }

        @Override
        protected Image getIcon(PsiElement psiElement) {
            return TwigIcons.TwigFileIcon;
        }
    }

    /**
     * Find "extends" which are targeting the given template file
     */
    private record TemplateExtendsLazyTargets(Project project, VirtualFile virtualFile) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            return PsiElementUtils.convertVirtualFilesToPsiFiles(project, TwigUtil.getTemplatesExtendingFile(project, virtualFile));
        }
    }

    private record UxComponentTemplateTargets(TwigFile twigFile) implements Supplier<Collection<? extends PsiElement>> {
        @Override
        public Collection<? extends PsiElement> get() {
            Project project = twigFile.getProject();
            VirtualFile currentFile = twigFile.getVirtualFile();

            // Collect direct UX component class targets first.
            Set<PsiElement> targets = new LinkedHashSet<>(UxUtil.getComponentClassesForTemplateFile(project, twigFile));
            if (currentFile == null) {
                return targets;
            }

            Set<String> normalizedComponentNames = new HashSet<>();
            for (String componentName : UxUtil.getTemplateComponentNames(twigFile)) {
                String normalized = TwigComponentUsageStubIndex.normalizeComponentName(componentName);
                if (normalized != null) {
                    normalizedComponentNames.add(normalized);
                }
            }

            if (normalizedComponentNames.isEmpty()) {
                return targets;
            }

            Set<VirtualFile> files = new HashSet<>();
            for (String normalizedComponentName : normalizedComponentNames) {
                // TwigComponentUsageStubIndex stores component usages in Twig files:
                // {{ component('Foo') }} and <twig:Foo>.
                files.addAll(FileBasedIndex.getInstance().getContainingFiles(
                    TwigComponentUsageStubIndex.KEY,
                    normalizedComponentName,
                    GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
                ));
            }

            files.remove(currentFile);

            for (VirtualFile file : files) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (!(psiFile instanceof TwigFile twigFile)) {
                    continue;
                }

                Collection<PsiElement> fileTargets = TwigComponentUsageStubIndex.getComponentUsages(twigFile, normalizedComponentNames);
                if (!fileTargets.isEmpty()) {
                    targets.addAll(fileTargets);
                } else {
                    targets.add(twigFile);
                }
            }

            return targets;
        }
    }

}
