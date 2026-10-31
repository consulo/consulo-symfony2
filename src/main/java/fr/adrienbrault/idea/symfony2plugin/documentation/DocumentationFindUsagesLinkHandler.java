package fr.adrienbrault.idea.symfony2plugin.documentation;

/**
 * Opens Find Usages from documentation links.
 *
 * TODO consulo-port: platform.backend.documentation API missing (DocumentationLinkHandler, DocumentationTarget,
 *  LinkResolveResult; could become LanguageDocumentationProvider.getCustomDocumentationElement + generateDoc).
 *  The class is left unregistered (was <platform.backend.documentation.linkHandler>) and has no members, because
 *  every upstream member depends on the missing API. Upstream (Kotlin) implementation for reference:
 *
 * <pre>
 * class DocumentationFindUsagesLinkHandler : DocumentationLinkHandler {
 *     override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
 *         val contextProvider = when (url) {
 *             ROUTE_FIND_USAGES_LINK if target is RouteDocumentationTarget -> routeContextProvider(target)
 *             TEMPLATE_FIND_USAGES_LINK if target is TwigTemplateDocumentationTarget -> twigTemplateContextProvider(target)
 *             else -> return null
 *         }
 *
 *         invokeFindUsages(contextProvider)
 *         return LinkResolveResult.resolvedTarget(target)
 *     }
 *
 *     private fun routeContextProvider(target: RouteDocumentationTarget): () -> DataContext? {
 *         val pointer = target.createPointer()
 *         return { pointer.dereference()?.findUsagesContext() }
 *     }
 *
 *     private fun twigTemplateContextProvider(target: TwigTemplateDocumentationTarget): () -> DataContext? {
 *         val pointer = target.createPointer()
 *         return { pointer.dereference()?.findUsagesContext() }
 *     }
 *
 *     private fun invokeFindUsages(contextProvider: () -> DataContext?) {
 *         ReadAction.nonBlocking<DataContext?> { contextProvider() }
 *             .finishOnUiThread(ModalityState.nonModal()) { context ->
 *                 val project = context?.getData(CommonDataKeys.PROJECT) ?: return@finishOnUiThread
 *                 if (project.isDisposed) {
 *                     return@finishOnUiThread
 *                 }
 *
 *                 val actionContext = SimpleDataContext.builder()
 *                     .setParent(context)
 *                     .add(CommonDataKeys.EDITOR, FileEditorManager.getInstance(project).selectedTextEditor)
 *                     .add(PlatformDataKeys.CONTEXT_COMPONENT, WindowManager.getInstance().getIdeFrame(project)?.component)
 *                     .build()
 *                 val action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES)
 *                 val event = AnActionEvent.createEvent(action, actionContext, null, "SymfonyDocumentation", ActionUiKind.POPUP, null)
 *                 ActionUtil.performAction(action, event)
 *             }
 *             .submit(AppExecutorUtil.getAppExecutorService())
 *     }
 * }
 * </pre>
 */
public class DocumentationFindUsagesLinkHandler {
}
