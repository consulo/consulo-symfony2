package fr.adrienbrault.idea.symfony2plugin.twig.action;

/**
 * Top-level functions of the former {@code TwigTranslationGeneratorAction.kt}.
 */
public final class TwigTranslationGeneratorActionKt {
    private TwigTranslationGeneratorActionKt() {
    }

    public static String createTranslationSnippet(String translationKey, String defaultDomain, String domain) {
        if (!domain.equals(defaultDomain)) {
            return "{{ '" + translationKey + "'|trans({}, '" + domain + "') }}";
        }
        else {
            return "{{ '" + translationKey + "'|trans }}";
        }
    }
}
