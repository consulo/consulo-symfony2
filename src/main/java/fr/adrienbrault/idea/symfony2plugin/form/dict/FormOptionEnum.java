package fr.adrienbrault.idea.symfony2plugin.form.dict;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public enum FormOptionEnum {
    UNKNOWN, DEFAULT, REQUIRED, DEFINED;

    public static FormOptionEnum getEnum(String s) {

        return switch (s) {
            case "setDefault" -> DEFAULT;
            case "setRequired" -> REQUIRED;
            case "setDefined", "setOptional" -> DEFINED;
            default -> UNKNOWN;
        };

    }
}