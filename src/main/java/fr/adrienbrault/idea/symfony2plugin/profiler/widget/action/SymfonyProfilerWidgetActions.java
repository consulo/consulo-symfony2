package fr.adrienbrault.idea.symfony2plugin.profiler.widget.action;

import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.application.ApplicationManager;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerRequestInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.ProfilerTwigComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.utils.ProfilerUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import com.jetbrains.twig.icons.TwigIcons;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyProfilerWidgetActions {

    public static class TemplateAction extends AnAction {
        private final String templateName;
        private final Project project;

        public TemplateAction(Project project, String templateName) {
            super(LocalizeValue.of(templateName), LocalizeValue.localizeTODO("Open Template"), createDeferredTemplateIcon(project, templateName));
            this.templateName = templateName;
            this.project = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            Collection<PsiFile> psiFiles = TwigUtil.getTemplatePsiElements(project, templateName);

            // @TODO: multiple targets?
            if(!psiFiles.isEmpty()) {
                IdeHelper.navigateToPsiElement(psiFiles.iterator().next());
            }
        }

        private static Image createDeferredTemplateIcon(Project project, String templateName) {
            return IconDeferrer.getInstance().defer(
                TwigIcons.TwigFileIcon,
                new TemplateIconRequest(project, templateName),
                request -> {
                    Image icon = ApplicationManager.getApplication().runReadAction((Supplier<Image>) () ->
                        getTemplateFileIcon(request.project(), request.templateName())
                    );
                    return icon != null ? icon : TwigIcons.TwigFileIcon;
                }
            );
        }

        @Nullable
        public static Image getTemplateFileIcon(Project project, String templateName) {
            for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(project, templateName)) {
                return IconDescriptorUpdaters.getIcon(psiFile, 0);
            }

            return null;
        }

        private record TemplateIconRequest(Project project, String templateName) {
        }
    }

    public static class FormTypeAction extends AnAction {
        private final String formType;
        private final Project project;

        public FormTypeAction(Project project, String formType) {
            super(LocalizeValue.of(formType), LocalizeValue.localizeTODO("Open FormType"), Symfony2Icons.FORM_TYPE);
            this.formType = formType;
            this.project = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            PhpClass psiFiles = FormUtil.getFormTypeToClass(project, formType);
            if (psiFiles != null) {
                IdeHelper.navigateToPsiElement(psiFiles);
            }
        }
    }

    public static class TwigComponentAction extends AnAction {
        private final Project project;

        private final ProfilerTwigComponent component;

        public TwigComponentAction(Project project, ProfilerTwigComponent component) {
            super(LocalizeValue.of(getText(component)), LocalizeValue.localizeTODO("Open Twig Component"), TwigIcons.TwigFileIcon);
            this.project = project;
            this.component = component;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (!component.isAnonymous() && component.className() != null) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, component.className());
                if (phpClass != null) {
                    IdeHelper.navigateToPsiElement(phpClass);
                    return;
                }
            }

            PsiFile template = getTemplateFile();
            if (template != null) {
                IdeHelper.navigateToPsiElement(template);
            }
        }

        @Nullable
        private PsiFile getTemplateFile() {
            if (component.template() != null) {
                Collection<PsiFile> psiFiles = TwigUtil.getTemplatePsiElements(project, component.template());
                if(!psiFiles.isEmpty()) {
                    return psiFiles.iterator().next();
                }
            }

            return null;
        }

        private static String getText(ProfilerTwigComponent component) {
            if (component.isAnonymous()) {
                return String.format("%s (anonymous)", component.name());
            }

            return component.name();
        }
    }

    public static class RouteAction extends AnAction {

        private final String routeName;
        private final Project project;

        public RouteAction(Project project, @Nullable String text) {
            super(LocalizeValue.ofNullable(text), LocalizeValue.localizeTODO("Open Route"), Symfony2Icons.ROUTE);
            this.routeName = text;
            this.project = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            PsiElement[] targets = RouteHelper.getMethods(project, this.routeName);
            if(targets.length > 0) {
                IdeHelper.navigateToPsiElement(targets[0]);
            }
        }

    }

    public static class MethodAction extends AnAction {

        private final String methodShortcut;
        private final Project project;

        public MethodAction(Project project, @Nullable String text) {
            super(LocalizeValue.ofNullable(text), LocalizeValue.localizeTODO("Open Method"), com.jetbrains.php.PhpIcons.METHOD);
            this.methodShortcut = text;
            this.project = project;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            PsiElement[] method = RouteHelper.getMethodsOnControllerShortcut(project, this.methodShortcut);
            if(method.length > 0) {
                IdeHelper.navigateToPsiElement(method[0]);
            }
        }
    }

    public static class UrlAction extends AnAction {

        private final ProfilerIndexInterface profilerIndex;

        private final ProfilerRequestInterface profilerRequest;

        private @Nullable String panel;

        public UrlAction(ProfilerIndexInterface profilerIndex, ProfilerRequestInterface profilerRequest) {
            super(LocalizeValue.of(String.format("(%s) %s", profilerRequest.getHash(), profilerRequest.getUrl())), LocalizeValue.localizeTODO("Open Url"), Symfony2Icons.PROFILER_LINK);
            this.profilerIndex = profilerIndex;
            this.profilerRequest = profilerRequest;

            Presentation presentation = getTemplatePresentation();
            presentation.setText(LocalizeValue.ofNullable(ProfilerUtil.formatProfilerRow(profilerRequest)));
        }

        public UrlAction(ProfilerIndexInterface profilerIndex, ProfilerRequestInterface profilerRequest, String panel) {
            this(profilerIndex, profilerRequest);
            this.panel = panel;
        }

        public UrlAction withIcon(Image icon) {
            Presentation presentation = getTemplatePresentation();
            presentation.setIcon(icon);
            return this;
        }

        public UrlAction withText(String text) {
            Presentation presentation = getTemplatePresentation();
            presentation.setText(LocalizeValue.of(text));

            return this;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent event) {
            String urlForRequest = profilerIndex.getUrlForRequest(profilerRequest);
            if(urlForRequest == null) {
                return;
            }

            if (this.panel != null) {
                urlForRequest += "?panel=" + this.panel;
            }

            IdeHelper.openUrl(urlForRequest);
        }
    }
}
