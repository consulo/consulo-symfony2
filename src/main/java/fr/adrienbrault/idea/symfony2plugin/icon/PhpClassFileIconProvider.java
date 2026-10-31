package fr.adrienbrault.idea.symfony2plugin.icon;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpModifier;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineClassMetadata;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Decorates Symfony-related PHP class file icons while keeping PHP's own class icon as the base.
 */
@ExtensionImpl(order = "after PhpIconDescriptorUpdater")
public class PhpClassFileIconProvider implements IconDescriptorUpdater {

    @RequiredReadAction
    @Override
    public void updateIcon(IconDescriptor iconDescriptor, PsiElement element, int flags) {
        if (!(element instanceof PsiFile psiFile) || psiFile.getVirtualFile() == null) {
            return;
        }

        Image icon = getIcon(psiFile.getVirtualFile(), flags, psiFile.getProject());
        if (icon != null) {
            iconDescriptor.setMainIcon(icon);
        }
    }

    @RequiredReadAction
    public @Nullable Image getIcon(VirtualFile virtualFile, int flags, @Nullable Project project) {
        if (!isSupportedRequest(virtualFile, project)) {
            return null;
        }

        Project currentProject = project;
        if (currentProject == null) {
            return null;
        }

        if (!(PsiManager.getInstance(currentProject).findFile(virtualFile) instanceof PhpFile psiFile)) {
            return null;
        }

        PhpClass phpClass = extractClass(psiFile);
        if (phpClass == null) {
            return null;
        }

        Image badgeIcon = getBadgeIcon(psiFile, phpClass);
        if (badgeIcon == null) {
            return null;
        }

        Image icon = PhpClassFileIconProviderKt.createDecoratedFileIcon(IconDescriptorUpdaters.getIcon(phpClass, 0), badgeIcon);
        // TODO consulo-port: IconManager.createLayeredIcon(psiFile, icon, ElementBase.transformFlags(psiFile, flags)) has no counterpart; layers come from other IconDescriptorUpdaters
        return icon;
    }

    private boolean isSupportedRequest(VirtualFile virtualFile, @Nullable Project project) {
        return project != null &&
            Symfony2ProjectComponent.isEnabled(project) &&
            Settings.getInstance(project).featurePhpClassIcon &&
            !DumbService.getInstance(project).isDumb() &&
            FileTypeRegistry.getInstance().isFileOfType(virtualFile, PhpFileType.INSTANCE);
    }

    private @Nullable PhpClass extractClass(PhpFile file) {
        List<PhpClass> classes = PhpPsiUtil.findAllClasses(file);
        if (classes.size() != 1) {
            return null;
        }

        PhpClass phpClass = classes.get(0);
        // Keep PHP core file icon behavior: only decorate files whose name matches the class name.
        return StringUtil.containsIgnoreCase(file.getName(), phpClass.getName()) ? phpClass : null;
    }

    private @Nullable Image getBadgeIcon(PhpFile file, PhpClass phpClass) {
        if (isSymfonyCommand(phpClass)) {
            return Symfony2Icons.SYMFONY_COMMAND_FILE;
        }
        else if (isSymfonyFormType(phpClass)) {
            return Symfony2Icons.FORM_TYPE_FILE;
        }
        else if (isSymfonyController(phpClass)) {
            return Symfony2Icons.CONTROLLER_FILE;
        }
        else if (isDoctrineEntity(file, phpClass)) {
            return Symfony2Icons.DOCTRINE_ENTITY_FILE;
        }
        else if (isDoctrineRepository(phpClass)) {
            return Symfony2Icons.DOCTRINE_REPOSITORY_FILE;
        }
        else {
            return null;
        }
    }

    private boolean isSymfonyCommand(PhpClass phpClass) {
        return !phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isEmpty();
    }

    private boolean isSymfonyFormType(PhpClass phpClass) {
        return PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\AbstractType") ||
            FormUtil.isFormType(phpClass);
    }

    private boolean isSymfonyController(PhpClass phpClass) {
        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller") ||
            hasRouteAttribute(phpClass)) {
            return true;
        }

        for (Method method : phpClass.getOwnMethods()) {
            if (hasRouteAttribute(method)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRouteAttribute(PhpClass phpClass) {
        for (String annotation : RouteHelper.ROUTE_ANNOTATIONS) {
            if (!phpClass.getAttributes(annotation).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRouteAttribute(Method method) {
        if (method.getAccess() != PhpModifier.Access.PUBLIC || method.isStatic()) {
            return false;
        }

        for (String annotation : RouteHelper.ROUTE_ANNOTATIONS) {
            if (!method.getAttributes(annotation).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean isDoctrineEntity(PhpFile file, PhpClass phpClass) {
        String className = StringUtil.trimLeading(phpClass.getFQN(), '\\');
        for (DoctrineClassMetadata metadata : DoctrineUtil.getClassRepositoryPair(file)) {
            if (className.equals(metadata.className())) {
                return true;
            }
        }

        return false;
    }

    private boolean isDoctrineRepository(PhpClass phpClass) {
        return !DoctrineMetadataUtil.findMetadataForRepositoryClass(phpClass).isEmpty() ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\ORM\\EntityRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Persistence\\ObjectRepository");
    }
}
