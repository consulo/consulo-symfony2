package fr.adrienbrault.idea.symfony2plugin.assetMapper.dict;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum MappingFileEnum {
    IMPORTMAP, INSTALLED;

    public static final Companion Companion = new Companion();

    public static MappingFileEnum fromString(String text) {
        if (text.equalsIgnoreCase("importmap.php")) {
            return IMPORTMAP;
        }

        if (text.equalsIgnoreCase("installed.php")) {
            return INSTALLED;
        }

        throw new RuntimeException("invalid filename");
    }

    public static final class Companion {
        private Companion() {
        }

        public MappingFileEnum fromString(String text) {
            return MappingFileEnum.fromString(text);
        }
    }
}
