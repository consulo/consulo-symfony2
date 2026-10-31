package fr.adrienbrault.idea.symfony2plugin.extension;


/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorProviderDict {
    public static class TranslationDomain {
        private final String domain;
        private final boolean weak;

        public TranslationDomain(String domain) {
            this.domain = domain;
            this.weak = false;
        }

        public TranslationDomain(String domain, boolean weak) {
            this.domain = domain;
            this.weak = weak;
        }

        public String getDomain() {
            return domain;
        }

        public boolean isWeak() {
            return weak;
        }
    }

    public static class TranslationKey {
        private final String domain;
        private final boolean weak;

        public TranslationKey(String domain) {
            this.domain = domain;
            this.weak = false;
        }

        public TranslationKey(String domain, boolean weak) {
            this.domain = domain;
            this.weak = weak;
        }

        public String getDomain() {
            return domain;
        }

        public boolean isWeak() {
            return weak;
        }
    }
}
