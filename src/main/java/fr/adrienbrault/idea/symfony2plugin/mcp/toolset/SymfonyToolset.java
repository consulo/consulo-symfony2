package fr.adrienbrault.idea.symfony2plugin.mcp.toolset;

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.mcp.tool.McpParameter;
import consulo.mcp.tool.McpToolCallResult;
import consulo.mcp.tool.McpToolException;
import consulo.mcpServer.McpToolActions;
import consulo.mcpServer.McpToolCallContext;
import consulo.mcpServer.McpToolRegistrar;
import consulo.mcpServer.McpToolset;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceBuilder;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityFieldsCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.ServiceDefinitionCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyCommandCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestDetailsCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyServiceLocatorCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigComponentCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateUsageCollector;
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigTemplateVariablesCollector;
import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndexInterface;
import fr.adrienbrault.idea.symfony2plugin.profiler.factory.ProfilerFactoryUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Groups Symfony MCP tools so they are exposed to the MCP server as a single toolset.
 */
public class SymfonyToolset implements McpToolset {

    @Override
    public void registerTools(McpToolRegistrar registrar) {
        registrar.tool("list_symfony_routes_url_controllers")
            .description("""
                Lists Symfony routes with URL mappings, controller mappings, file paths, and Twig template usages.

                Use this tool to find routes by route name, controller, partial route path, partial request URL, or a full absolute URL.
                Absolute URLs are normalized. 'https://www.de.test:8664/foo' is matched as '/foo'.

                This tool does not provide a complete list of all controllers, since controllers may exist without a route mapping.

                Returns CSV format with columns: name,controller,path,filePath,lineNumber,templates
                - name: Route name
                - controller: Full controller class::method reference
                - path: URL path pattern (contains parameters {id})
                - filePath: Relative path to controller file from project root
                - lineNumber: Controller method line number in the file; empty if unresolved
                - templates: Semicolon-separated list of Twig templates used by this controller references via render*() or php attributes

                Example output:
                name,controller,path,filePath,lineNumber,templates
                app_user_list,App\\Controller\\UserController::list,/users,src/Controller/UserController.php,42,user/list.html.twig;user/_pagination.html.twig
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("routeName", "Optional route-name filter (partial matching, case-insensitive). Examples: 'admin_dashboard', 'my_car_foo_stuff', 'App\\Controller\\ClassLikeRoute'").optional())
            .param(McpParameter.string("controller", "Optional controller filter (partial matching, case-insensitive). Valid examples: 'FooController', 'FooController::method', 'App\\Controller\\FooController::method'").optional())
            .param(McpParameter.string("urlPath", "Optional route-path, request-URL, or absolute URL filter (partial matching). Absolute URLs are normalized to the path. Examples: '/edit/{id}', '/edit/12', '/admin/users', 'https://www.de.test:8664/foo'").optional())
            .param(McpParameter.string("fileGlob", "Optional Ant-style glob on the controller file path relative to the project root. Example: 'src/Controller/**/*Admin*.php'").optional())
            .handler(this::list_symfony_routes_url_controllers);

        registrar.tool("list_symfony_commands")
            .description("""
                Lists all Symfony console commands available in the project as CSV.

                Returns CSV format with columns: name,className,filePath,options,arguments
                - name: Command name (e.g., cache:clear)
                - className: FQN of implementing class
                - filePath: Relative path from project root
                - options: JSON array of command options with name, shortcut, description, defaultValue
                - arguments: JSON array of command arguments with name, description, defaultValue

                Example output:
                name,className,filePath,options,arguments
                cache:clear,\\App\\Command\\CacheClearCommand,src/Command/CacheClearCommand.php,,
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("fileGlob", "Optional Ant-style glob on the command class file path relative to the project root. Example: 'src/Command/**/*.php'").optional())
            .handler(this::list_symfony_commands);

        registrar.tool("list_doctrine_entities")
            .description("""
                Lists all Doctrine ORM entities in the project as CSV.

                Returns CSV format with columns: className,filePath
                - className: FQN of the entity class
                - filePath: Relative path from project root

                Example output:
                className,filePath
                App\\Entity\\User,src/Entity/User.php
                """)
            .readOnly()
            .requiresProject()
            .handler(this::list_doctrine_entities);

        registrar.tool("list_doctrine_entity_fields")
            .description("""
                Lists all fields of a Doctrine entity as CSV.

                Parameters:
                - className: FQN of the entity class (e.g., "App\\Entity\\User" or "\\App\\Entity\\User")

                Returns CSV format with columns: name,column,type,relation,relationType,enumType,propertyType
                - name: Field/property name
                - column: Database column name
                - type: Doctrine type (string, integer, text, etc.)
                - relation: Related entity class if this is a relation field
                - relationType: Type of relation (OneToOne, OneToMany, ManyToOne, ManyToMany)
                - enumType: FQN of the enum class for enum fields (PHP 8.1+)
                - propertyType: Pipe-separated PHP property types (e.g. "string", "\\App\\Status|null")

                Example output:
                name,column,type,relation,relationType,enumType,propertyType
                id,id,integer,,,,int
                username,username,string,,,string
                status,status,string,,,\\App\\Enum\\Status,\\App\\Enum\\Status
                orders,orders,,App\\Entity\\Order,OneToMany,,
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("className", "Doctrine entity FQN. Valid examples: 'App\\Entity\\User', '\\App\\Entity\\User'"))
            .handler(this::list_doctrine_entity_fields);

        registrar.tool("list_twig_extensions")
            .description("""
                Lists available Twig template extensions for code generation and template assistance.

                Use this to discover:
                - Filters for value transformation ({{ value|filter }})
                - Functions for template logic ({{ function() }})
                - Tests for conditionals ({% if var is test %})
                - Tags for control structures ({% tag %})

                Supports filtering by name (partial match) and type. Use to generate accurate Twig code,
                validate template syntax, or suggest available extensions to developers.

                Returns CSV: extension_type,name,className,methodName,parameters
                Example: filter,upper,\\Twig\\Extension\\CoreExtension,upper,"value,encoding"
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("search", "Partial name search (case-insensitive). Examples: 'date', 'url', 'format'").optional())
            .param(McpParameter.bool("includeFilters", "Include filters ({{ value|filter }}). Default: true").defaultValue(true))
            .param(McpParameter.bool("includeFunctions", "Include functions ({{ func() }}). Default: true").defaultValue(true))
            .param(McpParameter.bool("includeTests", "Include tests ({% if value is test %}). Default: true").defaultValue(true))
            .param(McpParameter.bool("includeTags", "Include tags ({% tag %}). Default: true").defaultValue(true))
            .handler(this::list_twig_extensions);

        registrar.tool("list_symfony_profiler")
            .description("""
                Lists recent Symfony Profiler requests as CSV for tracing request handling, controller resolution, and rendered Twig views; pass a returned hash to get_symfony_profiler_details for collector details.

                - hash: Profiler token/hash
                - profiledAt: Profile timestamp (ISO-8601)
                - method: HTTP method
                - url: Request URL
                - statusCode: HTTP status code
                - profilerUrl: URL to the profiler page
                - controller: Controller handling the request
                - route: Matched route name
                - entryView: Symfony profiler "Entry View"
                - renderTemplate: Templates indexed from render*()/@Template/#[Template]
                - renderedTemplates: First 3 Twig templates from the profiler "Rendered Templates" panel, in profiler order
                - formTypes: Form types used (pipe-separated if multiple)
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("url", "Optional URL partial match, case-insensitive. Example: '/admin'").optional())
            .param(McpParameter.string("hash", "Optional profiler token partial match, case-insensitive. Example: '18e6b8'").optional())
            .param(McpParameter.string("controller", "Optional controller partial match, case-insensitive. Valid examples: 'UserController', 'UserController::showAction', 'App\\Controller\\UserController::showAction'").optional())
            .param(McpParameter.string("route", "Optional route partial match, case-insensitive. Example: 'user_show'").optional())
            .param(McpParameter.integer("limit", "Optional max rows. Default: 25.").defaultValue(25))
            .handler(this::list_symfony_profiler);

        registrar.tool("get_symfony_profiler_details")
            .description("""
                Shows one Symfony Profiler request. Use list_symfony_profiler first to find its hash or pass `latest`.

                Omit collector for a compact overview and the complete list of available collectors.

                Collector-specific renderers:

                - request: Sanitized HTTP request, response, session, route, and controller data
                - logger: Prioritized log sections with newest messages first
                - events: Dispatched event lists with priorities and called listeners
                - time: Performance summary and complete Stopwatch event timeline with start/end timings
                - twig: Twig render metrics, unique templates, and the complete rendering call tree
                - db: Doctrine query groups and timings

                Any other collector stored in the profile is available through a bounded, sanitized raw fallback.
                Collector details use bounded plain-text pages; use the page parameter to request the next page.
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("hash", "Profiler token/hash from list_symfony_profiler, or 'latest' newest known. Examples: '18e6b8', 'latest'"))
            .param(McpParameter.string("collector", "Optional exact case-sensitive collector name.").optional())
            .param(McpParameter.integer("page", "1-based page number. Defaults to 1.").defaultValue(1))
            .handler(this::get_symfony_profiler_details);

        registrar.tool("list_symfony_forms")
            .description("""
                Lists all Symfony form types in the project as CSV.

                Returns CSV format with columns: name,className,filePath
                - name: Form type name/alias
                - className: FQN of the form type class
                - filePath: Relative path from project root

                Example output:
                name,className,filePath
                user,App\\Form\\UserType,src/Form/UserType.php
                """)
            .readOnly()
            .requiresProject()
            .handler(this::list_symfony_forms);

        registrar.tool("list_symfony_form_options")
            .description("""
                Lists all options for a Symfony form type as CSV.

                Returns CSV format with columns: name,type,source
                - name: Option name
                - type: Option type (DEFAULT, REQUIRED, DEFINED)
                - source: Source class that defines the option

                Example output:
                name,type,source
                label,DEFAULT,Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType
                required,DEFAULT,Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType
                data,DEFAULT,Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("formType", "Form type name or FQN. Valid examples: 'text', 'App\\Form\\UserType', 'Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType'"))
            .handler(this::list_symfony_form_options);

        registrar.tool("locate_symfony_service")
            .description("""
                Locate Symfony service definitions by service ID or class name.

                Returns grouped source snippets with file paths, line numbers and surrounding definition.
                Autowired/resource-prototype services may resolve to the prototype definition block.

                Example:
                ## app.service.my_service
                File: config/services.yaml
                15: app.service.my_service:
                16:     class: App\\Service\\MyService
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("identifier", "Service ID or fully qualified class name. Examples: 'app.service.my_service', '\\App\\MyService'"))
            .handler(this::locate_symfony_service);

        registrar.tool("generate_symfony_service_definition")
            .description("""
                Generate Symfony service definitions in YAML, XML, Fluent PHP or PHP array format for one or more classes.
                Inspects each class constructor to guess service dependencies from parameter types for explicit wiring.

                Fluent PHP example:
                ```php
                $services->set(\\App\\EmailService::class)
                    ->args([
                        service('mailer')
                    ]);
                ```

                PHP array example (eg in `App::config()` or `return`):
                ```php
                [
                    \\App\\EmailService::class => [
                        'arguments' => [
                            service('mailer')
                        ],
                    ],
                ];
                ```
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("className", "Fully qualified class name for the service, or a comma-separated list of class names. Valid examples: '\\App\\Service\\EmailService', '\\App\\Service\\EmailService,\\App\\Service\\SmsService'"))
            .param(McpParameter.string("format", "Output format: 'yaml' (default), 'xml', 'fluent' or 'phparray'").defaultValue("yaml"))
            .param(McpParameter.bool("useClassNameAsId", "If true, uses class name as service ID (default); if false, generates a short ID").defaultValue(true))
            .handler(this::generate_symfony_service_definition);

        registrar.tool("list_twig_template_usages")
            .description("""
                Lists usages of Twig templates. Accepts a partial template name and can optionally constrain resolved template files with a project-relative Ant-style glob.

                CSV columns (values semicolon-separated): template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path
                - template: logical template name (e.g. "home/index.html.twig")
                - controller: PHP methods via render()/renderView()/@Template/#[Template]
                - twig_include: {% include %} / {{ include() }}
                - twig_embed: {% embed %}
                - twig_extends: {% extends %}
                - twig_import: {% import %} / {% from ... import %}
                - twig_use: {% use %}
                - twig_form_theme: {% form_theme %}
                - twig_component: {{ component('X') }} / {% component X %} / <twig:X>
                - route_name: Symfony route name(s) for controller action(s) in the controller column
                - route_path: Symfony route path(s) for controller action(s) in the controller column

                Example:
                template,controller,twig_include,twig_embed,twig_extends,twig_import,twig_use,twig_form_theme,twig_component,route_name,route_path
                partials/nav.html.twig,App\\Controller\\BaseController::index,templates/layouts/base.html.twig,,,,,,,app_base_index,/
                layouts/base.html.twig,,,,,templates/pages/home.html.twig;templates/pages/about.html.twig,,,,
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("template", "Optional partial template name. Example: 'home/index.html.twig'").optional())
            .param(McpParameter.string("fileGlob", "Optional Ant-style glob on resolved template file paths relative to the project root. Examples: 'templates/home/index.html.twig', 'templates/admin/**/*.html.twig'").optional())
            .handler(this::list_twig_template_usages);

        registrar.tool("list_twig_components")
            .description("""
                Lists Symfony UX Twig components and composition metadata.

                Supports partial component-name search (case-insensitive).

                Returns CSV format with columns:
                component_name,template_relative_path,component_tag,twig_component_syntax,component_print_block_syntax,twig_tag_composition_syntax,props,template_blocks

                - component_name: canonical component name (e.g. Alert, Admin:Card)
                - template_relative_path: component template path relative to project root
                - component_tag: HTML syntax (e.g. <twig:Alert></twig:Alert>)
                - twig_component_syntax: function syntax (e.g. {{ component('Alert') }})
                - component_print_block_syntax: block print calls for available blocks (e.g. {{ block('footer') }})
                - twig_tag_composition_syntax: Twig tag composition snippet ({% component 'Alert' %}...{% endcomponent %})
                - props: component props exposed by PHP class/template props tag
                - template_blocks: block names provided by component template
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("search", "Optional partial component-name filter (case-insensitive). Examples: 'Alert', 'Admin:Card'").optional())
            .handler(this::list_twig_components);

        registrar.tool("list_twig_template_variables")
            .description("""
                Lists all variables available in a Twig template with their PHP types and first-level accessible properties.
                Accepts comma-separated templates or project-relative path glob.

                CSV columns:
                - variable: Twig variable name
                - type: PHP FQN(s) joined with "|" (may include "[]" suffix for arrays)
                - properties: comma-separated first-level Twig-accessible names (get/is/has shortcut methods + public fields)

                template: home/index.html.twig => (file: templates/home/index.html.twig)
                variable,type,properties
                user,\\App\\Entity\\User,"id,email,name"
                """)
            .readOnly()
            .requiresProject()
            .param(McpParameter.string("template", "Optional logical template name or comma-separated list of logical template names. Example: 'home/index.html.twig' or 'home/index.html.twig,admin/dashboard.html.twig'").optional())
            .param(McpParameter.string("fileGlob", "Optional Ant-style glob on template file paths relative to the project root. Example: 'templates/home/index.html.twig' or 'templates/admin/**/*.html.twig'").optional())
            .handler(this::list_twig_template_variables);
    }

    private CompletableFuture<McpToolCallResult> list_symfony_routes_url_controllers(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String routeName = context.findString("routeName");
        String controller = context.findString("controller");
        String urlPath = context.findString("urlPath");
        String fileGlob = context.findString("fileGlob");

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyRouteCollector(project).collect(routeName, controller, urlPath, fileGlob)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_symfony_commands(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String fileGlob = context.findString("fileGlob");

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyCommandCollector(project).collect(fileGlob)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_doctrine_entities(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new DoctrineEntityCollector(project).collect()
        ));
    }

    private CompletableFuture<McpToolCallResult> list_doctrine_entity_fields(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String className = context.getString("className");

        if (className.isBlank()) {
            throw new McpToolException("className parameter is required.");
        }

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new DoctrineEntityFieldsCollector(project).collect(className)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_twig_extensions(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String search = context.findString("search");
        boolean includeFilters = context.getBoolean("includeFilters");
        boolean includeFunctions = context.getBoolean("includeFunctions");
        boolean includeTests = context.getBoolean("includeTests");
        boolean includeTags = context.getBoolean("includeTags");

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new TwigExtensionCollector(project).collect(search, includeFilters, includeFunctions, includeTests, includeTags)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_symfony_profiler(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String url = context.findString("url");
        String hash = context.findString("hash");
        String controller = context.findString("controller");
        String route = context.findString("route");
        int limit = context.getInt("limit");

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyProfilerRequestsCollector(project).collect(url, hash, controller, route, limit)
        ));
    }

    private CompletableFuture<McpToolCallResult> get_symfony_profiler_details(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String hash = context.getString("hash");
        String collector = context.findString("collector");
        int page = context.getInt("page");

        // Raw profile loading and PHP deserialization are blocking work and must not hold an IDE read lock.
        return Coroutine.first(ReadLock.<Object, ProfilerIndexInterface>apply(ignored -> ProfilerFactoryUtil.createIndex(project)))
            .then(CodeExecution.<ProfilerIndexInterface, McpToolCallResult>apply(profilerIndex -> {
                if (profilerIndex == null) {
                    throw new McpToolException("No profiler index available. Make sure the Symfony profiler is enabled and accessible.");
                }

                return McpToolCallResult.text(new SymfonyProfilerRequestDetailsCollector(profilerIndex).collect(hash, collector, page));
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), null)
            .toFuture();
    }

    private CompletableFuture<McpToolCallResult> list_symfony_forms(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyFormTypeCollector(project).collect()
        ));
    }

    private CompletableFuture<McpToolCallResult> list_symfony_form_options(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String formType = context.getString("formType");

        if (formType.isBlank()) {
            throw new McpToolException("formType parameter is required.");
        }

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyFormTypeOptionsCollector(project).collect(formType)
        ));
    }

    private CompletableFuture<McpToolCallResult> locate_symfony_service(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String identifier = context.getString("identifier");

        if (identifier.isBlank()) {
            throw new McpToolException("identifier parameter is required.");
        }

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new SymfonyServiceLocatorCollector(project).collect(identifier)
        ));
    }

    private CompletableFuture<McpToolCallResult> generate_symfony_service_definition(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String className = context.getString("className");
        String format = context.getString("format");
        boolean useClassNameAsId = context.getBoolean("useClassNameAsId");

        if (StringUtils.isBlank(className)) {
            throw new McpToolException("className parameter is required.");
        }

        ServiceBuilder.OutputType outputType = switch (format.toLowerCase(Locale.ROOT)) {
            case "xml" -> ServiceBuilder.OutputType.XML;
            case "yaml", "" -> ServiceBuilder.OutputType.Yaml;
            case "fluent" -> ServiceBuilder.OutputType.Fluent;
            case "phparray" -> ServiceBuilder.OutputType.PhpArray;
            default -> throw new McpToolException("Invalid format: '" + format + "'. Valid values are: 'yaml', 'xml', 'fluent' or 'phparray'");
        };

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new ServiceDefinitionCollector(project).collect(className, outputType, useClassNameAsId)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_twig_template_usages(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String template = context.findString("template");
        String fileGlob = context.findString("fileGlob");

        String normalizedTemplate = template != null && !template.trim().isBlank() ? template.trim() : null;
        String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;

        if (normalizedTemplate == null && normalizedFileGlob == null) {
            throw new McpToolException("At least one of 'template' or 'fileGlob' must be provided.");
        }

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new TwigTemplateUsageCollector(project).collect(normalizedTemplate, normalizedFileGlob)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_twig_components(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String search = context.findString("search");

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new TwigComponentCollector(project).collect(search)
        ));
    }

    private CompletableFuture<McpToolCallResult> list_twig_template_variables(McpToolCallContext context) {
        Project project = requireSymfonyProject(context);
        String template = context.findString("template");
        String fileGlob = context.findString("fileGlob");

        String normalizedTemplate = template != null && !template.trim().isBlank() ? template.trim() : null;
        String normalizedFileGlob = fileGlob != null && !fileGlob.trim().isBlank() ? fileGlob.trim() : null;

        if (normalizedTemplate == null && normalizedFileGlob == null) {
            throw new McpToolException("At least one of 'template' or 'fileGlob' must be provided.");
        }

        return McpToolActions.readAction(project, () -> McpToolCallResult.text(
            new TwigTemplateVariablesCollector(project).collect(normalizedTemplate, normalizedFileGlob)
        ));
    }

    private Project requireSymfonyProject(McpToolCallContext context) {
        Project project = context.getProject();

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            throw new McpToolException("Symfony is not enabled for this project. Enable the Symfony module extension in Project Structure > module > Symfony.");
        }

        return project;
    }
}
