package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import com.jetbrains.twig.TwigFileType;
import consulo.application.Application;
import consulo.component.util.pointer.Pointer;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigationService;
import consulo.navigation.TargetPresentation;
import consulo.project.Project;
import consulo.usage.PsiElementUsageTargetFactory;
import consulo.usage.UsageTarget;
import consulo.usage.UsageView;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.jspecify.annotations.Nullable;

/**
 * TODO consulo-port: platform.backend.documentation API missing (DocumentationTarget, DocumentationResult; could become
 *  LanguageDocumentationProvider.getCustomDocumentationElement + generateDoc). Was "implements DocumentationTarget";
 *  the former overrides are kept as plain methods, computeDocumentation() (DocumentationResult.asyncDocumentation +
 *  readAction { renderTwigTemplateDocumentation(collectTwigTemplateDocumentation(current)) }) is dropped.
 */
public class TwigTemplateDocumentationTarget {
    private final TwigTemplateDocumentationContext myContext;
    private final VirtualFile myTemplateFile;

    public TwigTemplateDocumentationTarget(TwigTemplateDocumentationContext context, VirtualFile templateFile) {
        myContext = context;
        myTemplateFile = templateFile;
    }

    public Pointer<TwigTemplateDocumentationTarget> createPointer() {
        SmartPsiElementPointer<PsiElement> pointer = SmartPointerManager.createPointer(myContext.getAnchor());
        VirtualFile selected = myTemplateFile;

        return () -> {
            PsiElement anchor = pointer.getElement();
            if (anchor == null) {
                return null;
            }

            TwigTemplateDocumentationContext restored = new TwigTemplateDocumentationTargetProvider().resolveContext(anchor.getContainingFile(), anchor.getTextOffset());
            if (restored == null) {
                return null;
            }

            if (!TwigUtil.getTemplateFiles(anchor.getProject(), restored.getName()).contains(selected)) {
                return null;
            }

            return new TwigTemplateDocumentationTarget(restored, selected);
        };
    }

    private @Nullable TwigTemplateDocumentationContext current() {
        PsiElement anchor = myContext.getAnchor();
        if (!anchor.isValid()) {
            return null;
        }

        return new TwigTemplateDocumentationTargetProvider().resolveContext(anchor.getContainingFile(), anchor.getTextOffset());
    }

    public @Nullable Navigatable getNavigatable() {
        TwigTemplateDocumentationContext current = current();
        if (current == null) {
            return null;
        }

        VirtualFile file = null;
        int matches = 0;
        for (VirtualFile candidate : TwigUtil.getTemplateFiles(current.getAnchor().getProject(), current.getName())) {
            if (candidate.equals(myTemplateFile)) {
                file = candidate;
                matches++;
            }
        }

        if (file == null || matches != 1) {
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(current.getAnchor().getProject()).findFile(file);
        if (psiFile == null) {
            return null;
        }

        return psiFile.getNavigationElement() instanceof Navigatable navigatable ? navigatable : null;
    }

    public TargetPresentation computePresentation() {
        String locationText = VfsExUtil.getRelativeProjectPath(myContext.getAnchor().getProject(), myTemplateFile);

        // TODO consulo-port: TargetPresentation.builder(..) -> NavigationService.presentationBuilder(..)
        return Application.get().getInstance(NavigationService.class)
            .presentationBuilder(LocalizeValue.of(myContext.getName()))
            .withIcon(TwigFileType.INSTANCE.getIcon())
            .withLocationText(LocalizeValue.of(locationText != null ? locationText : myTemplateFile.getPresentableUrl()))
            .build();
    }

    // Hints stay cheap; full index/PSI collection runs asynchronously under a read action.
    public @Nullable String computeDocumentationHint() {
        TwigTemplateDocumentationContext current = current();
        if (current == null) {
            return null;
        }

        return StringUtil.escapeXmlEntities(current.getName());
    }

    public @Nullable DataContext findUsagesContext() {
        TwigTemplateDocumentationContext current = current();
        if (current == null) {
            return null;
        }

        if (!myTemplateFile.isValid() || !TwigUtil.getTemplateFiles(current.getAnchor().getProject(), current.getName()).contains(myTemplateFile)) {
            return null;
        }

        PsiFile template = PsiManager.getInstance(current.getAnchor().getProject()).findFile(myTemplateFile);
        if (template == null) {
            return null;
        }

        return DataContext.builder()
            .add(Project.KEY, template.getProject())
            .add(PsiElement.KEY, template)
            .add(PsiFile.KEY, template)
            // upstream: PsiElement2UsageTargetAdapter(template, true); the highlightUsages flag has no counterpart
            .add(UsageView.USAGE_TARGETS_KEY, new UsageTarget[]{PsiElementUsageTargetFactory.getInstance().create(template)})
            .build();
    }
}
