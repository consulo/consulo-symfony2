package fr.adrienbrault.idea.symfony2plugin;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.WriteAction;
import consulo.language.psi.PsiElement;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.SkipDefaultValuesSerializationFilters;
import consulo.util.xml.serializer.XmlSerializer;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.module.extension.SymfonyMutableModuleExtension;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Symfony configuration of a module. The state is owned by {@link SymfonyModuleExtension} and serialized into the
 * module extension element; an empty element means the defaults declared here.
 * <p>
 * Instances returned by the {@code getInstance} methods are read-only views: changes must go through
 * {@link #modify(Module, Consumer)} (or the module extension configuration panel) so they are committed into the module.
 */
public class Settings {

    // Default Symfony
    public static final Collection<String> DEFAULT_ROUTES = Arrays.asList(
        "app/cache/dev/appDevUrlGenerator.php",
        "var/cache/dev/appDevUrlGenerator.php",
        "var/cache/dev/appDevDebugProjectContainerUrlGenerator.php",
        "var/cache/dev/srcDevDebugProjectContainerUrlGenerator.php",
        "var/cache/dev/url_generating_routes.php", // Symfony >= 4
        "var/cache/dev/UrlGenerator.php" // Symfony >= 4
    );

    public static final String DEFAULT_TRANSLATION_PATH = "var/cache/dev/translations";

    public static final String DEFAULT_WEB_DIRECTORY = "public";
    public static final String DEFAULT_APP_DIRECTORY = "app";

    public String pathToTranslation = DEFAULT_TRANSLATION_PATH;
    public String directoryToWeb = DEFAULT_WEB_DIRECTORY;
    public String directoryToApp = DEFAULT_APP_DIRECTORY;
    public String serviceJsNameStrategy = null;

    public boolean objectSignatureTypeProvider = false;

    public boolean codeFoldingPhpRoute = true;
    public boolean codeFoldingPhpModel = true;
    public boolean codeFoldingPhpTemplate = true;
    public boolean codeFoldingTwigRoute = true;
    public boolean codeFoldingTwigTemplate = true;
    public boolean codeFoldingTwigConstant = true;
    public boolean featureTwigIcon = true;
    public boolean featurePhpClassIcon = true;
    public boolean featureTypeProvider = true;

    public boolean featurePropertyInjection = true;

    public boolean twigBundleNamespaceSupport = false;

    public boolean profilerLocalEnabled = false;
    public String profilerLocalUrl = "http://127.0.0.1:8000";
    public String profilerCsvPath;

    public boolean profilerHttpEnabled = false;
    public String profilerHttpUrl = "http://127.0.0.1:8000";

    /**
     * Use service id attribute as class name for service generator
     */
    public boolean serviceClassAsIdAttribute = true;

    @Nullable
    public List<TwigNamespaceSetting> twigNamespaces = new ArrayList<>();

    @Nullable
    public List<ContainerFile> containerFiles = new ArrayList<>();

    @Nullable
    public List<RoutingFile> routingFiles = new ArrayList<>();

    @Nullable
    public List<MethodParameterSetting> methodParameterSettings = new ArrayList<>();

    @Nullable
    public List<MethodSignatureSetting> methodSignatureSettings = new ArrayList<>();

    /**
     * Settings of the first module (in module manager order) with the Symfony module extension,
     * or a fresh default instance if Symfony is not enabled in the project.
     */
    public static Settings getInstance(Project project) {
        SymfonyModuleExtension extension = findProjectExtension(project);
        return extension != null ? extension.getSettings() : new Settings();
    }

    /**
     * Settings of the module, if it has the Symfony module extension; otherwise the project ones
     */
    public static Settings getInstance(@Nullable Module module) {
        if (module == null) {
            return new Settings();
        }

        SymfonyModuleExtension extension = module.getExtension(SymfonyModuleExtension.class);
        if (extension != null) {
            return extension.getSettings();
        }

        return getInstance(module.getProject());
    }

    /**
     * Settings of the module containing the element; elements without a module (libraries, light files)
     * or inside a module without Symfony fall back to the project settings
     */
    @RequiredReadAction
    public static Settings getInstance(PsiElement psiElement) {
        SymfonyModuleExtension extension = findExtension(psiElement);
        return extension != null ? extension.getSettings() : getInstance(psiElement.getProject());
    }

    /**
     * Settings for the element if Symfony is enabled for it (see {@link Symfony2ProjectComponent#isEnabled(PsiElement)}), otherwise null.
     * Needs only one module lookup, so it is suitable for hot paths like type providers.
     */
    @RequiredReadAction
    public static @Nullable Settings getEnabledInstance(PsiElement psiElement) {
        SymfonyModuleExtension projectExtension = findProjectExtension(psiElement.getProject());
        if (projectExtension == null) {
            return null;
        }

        Module module = findModule(psiElement);
        if (module == null) {
            return projectExtension.getSettings();
        }

        SymfonyModuleExtension extension = module.getExtension(SymfonyModuleExtension.class);
        return extension != null ? extension.getSettings() : null;
    }

    @RequiredReadAction
    public static @Nullable SymfonyModuleExtension findExtension(PsiElement psiElement) {
        Module module = findModule(psiElement);
        return module != null ? module.getExtension(SymfonyModuleExtension.class) : null;
    }

    @RequiredReadAction
    public static @Nullable Module findModule(PsiElement psiElement) {
        if (!psiElement.isValid()) {
            return null;
        }

        return psiElement.getModule();
    }

    /**
     * First module (in module manager order) with an enabled Symfony module extension
     */
    public static @Nullable SymfonyModuleExtension findProjectExtension(Project project) {
        if (project.isDisposed() || project.isDefault()) {
            return null;
        }

        Collection<SymfonyModuleExtension> extensions = ModuleExtensionHelper.getInstance(project).getModuleExtensions(SymfonyModuleExtension.class);
        if (extensions.isEmpty()) {
            return null;
        }

        if (extensions.size() == 1) {
            return extensions.iterator().next();
        }

        // stable order
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            SymfonyModuleExtension extension = module.getExtension(SymfonyModuleExtension.class);
            if (extension != null) {
                return extension;
            }
        }

        return null;
    }

    /**
     * Changes the Symfony settings of the first Symfony module of the project; no-op if Symfony is not enabled
     */
    @RequiredUIAccess
    public static void modify(Project project, Consumer<Settings> modifier) {
        SymfonyModuleExtension extension = findProjectExtension(project);
        if (extension != null) {
            modify(extension.getModule(), modifier);
        }
    }

    /**
     * Changes the Symfony settings of the module through a committed {@link ModifiableRootModel}, so the change is persisted
     */
    @RequiredUIAccess
    public static void modify(Module module, Consumer<Settings> modifier) {
        if (module.isDisposed()) {
            return;
        }

        ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
        SymfonyMutableModuleExtension extension = model.getExtensionWithoutCheck(SymfonyMutableModuleExtension.class);
        if (extension == null || !extension.isEnabled()) {
            model.dispose();
            return;
        }

        modifier.accept(extension.getSettings());

        if (model.isChanged()) {
            WriteAction.run(model::commit);
        }
        else {
            model.dispose();
        }
    }

    public Element serialize(String elementName) {
        Element element = new Element(elementName);
        serializeInto(element);
        return element;
    }

    /**
     * Writes all non default values as children of the element
     */
    public void serializeInto(Element element) {
        XmlSerializer.serializeInto(this, element, new SkipDefaultValuesSerializationFilters());
    }

    public static Settings deserialize(@Nullable Element element) {
        Settings settings = new Settings();
        if (element != null) {
            XmlSerializer.deserializeInto(settings, element);
        }
        return settings;
    }

    /**
     * Deep copy, nested setting beans are not shared
     */
    public Settings copy() {
        return deserialize(serialize("settings"));
    }

    public boolean isSame(Settings settings) {
        return JDOMUtil.areElementsEqual(serialize("settings"), settings.serialize("settings"));
    }
}
