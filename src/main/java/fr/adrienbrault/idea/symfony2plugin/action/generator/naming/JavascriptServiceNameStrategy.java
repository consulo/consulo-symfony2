package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import com.google.gson.JsonObject;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.project.Project;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class JavascriptServiceNameStrategy implements ServiceNameStrategyInterface {

    @Override
    public @Nullable String getServiceName(ServiceNameStrategyParameter parameter) {

        String serviceJsNameStrategy = Settings.getInstance(parameter.getProject()).serviceJsNameStrategy;
        if(serviceJsNameStrategy == null || StringUtils.isBlank(serviceJsNameStrategy)) {
            return null;
        }

        try {
            Object eval = run(parameter.getProject(), parameter.getClassName(), serviceJsNameStrategy);
            if(!(eval instanceof String)) {
                return null;
            }
            return StringUtils.isNotBlank((String) eval) ? (String) eval : null;
        } catch (ScriptException e) {
            Symfony2ProjectComponent.getLogger().error(String.format("ScriptException: '%s' - Script: '%s'", e.getMessage(), serviceJsNameStrategy));
        }

        return null;
    }

    public static @Nullable Object run(Project project, String className, String serviceJsNameStrategy) throws ScriptException {

        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("className", className);
        jsonObject.addProperty("projectName", project.getName());
        jsonObject.addProperty("projectBasePath", project.getBasePath());
        jsonObject.addProperty("defaultNaming", new DefaultServiceNameStrategy().getServiceName(new ServiceNameStrategyParameter(project, className)));

        PhpClass aClass = PhpElementsUtil.getClass(project, className);
        if(aClass != null) {
            String relativePath = VirtualFileUtil.getRelativePath(aClass.getContainingFile().getVirtualFile(), ProjectUtil.getProjectDir(aClass), '/');
            if(relativePath != null) {
                jsonObject.addProperty("relativePath", relativePath);
            }
            jsonObject.addProperty("absolutePath", aClass.getContainingFile().getVirtualFile().toString());
        }

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        if(engine == null) {
            return null;
        }

        return engine.eval("var __p = eval(" + jsonObject.toString() + "); result = function(args) { " + serviceJsNameStrategy + " }(__p)");
    }

}
