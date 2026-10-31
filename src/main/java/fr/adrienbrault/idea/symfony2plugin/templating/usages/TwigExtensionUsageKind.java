package fr.adrienbrault.idea.symfony2plugin.templating.usages;

/**
 * Twig extension symbol kind like `form_start` in `{{ form_start() }}` or `trans` in `{{ value|trans }}`.
 */
public enum TwigExtensionUsageKind {
    /**
     * Twig function like `form_start` in `{{ form_start() }}`.
     */
    FUNCTION,

    /**
     * Twig filter like `trans` in `{{ value|trans }}`.
     */
    FILTER,
}
