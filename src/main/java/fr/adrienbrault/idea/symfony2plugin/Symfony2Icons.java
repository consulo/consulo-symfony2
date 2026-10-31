package fr.adrienbrault.idea.symfony2plugin;

import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.icon.SymfonyIconGroup;

import java.util.Calendar;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class Symfony2Icons {

    public static final Image SYMFONY;
    public static final Image SYMFONY_LINE_MARKER;

    static {
        if(Calendar.getInstance().get(Calendar.MONTH) == Calendar.DECEMBER && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) > 7) {
            SYMFONY = SymfonyIconGroup.symfony_dec();
            SYMFONY_LINE_MARKER = SymfonyIconGroup.symfony_line_marker_dec();
        } else if(Calendar.getInstance().get(Calendar.MONTH) == Calendar.APRIL && Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == 7) {
            SYMFONY = SymfonyIconGroup.symfony_birth();
            SYMFONY_LINE_MARKER = SymfonyIconGroup.symfony_line_marker_birth();
        } else {
            SYMFONY = SymfonyIconGroup.symfony();
            SYMFONY_LINE_MARKER = SymfonyIconGroup.symfony_line_marker();
        }
    }

    public static final Image SYMFONY_TOOL_WINDOW = SymfonyIconGroup.symfony_tool_window();
    public static final Image DOCTRINE = SymfonyIconGroup.doctrine();
    public static final Image DOCTRINE_LINE_MARKER = SymfonyIconGroup.doctrine_line_marker();
    public static final Image DOCTRINE_ENTITY_FILE = SymfonyIconGroup.doctrine_entity_file();
    public static final Image DOCTRINE_REPOSITORY_FILE = SymfonyIconGroup.doctrine_repository_file();
    public static final Image DOCTRINE_WEAK = SymfonyIconGroup.doctrine_weak();
    public static final Image NO_SQL = SymfonyIconGroup.no_sql();
    public static final Image ROUTE = SymfonyIconGroup.route();
    public static final Image ROUTE_WEAK = SymfonyIconGroup.route_weak();
    public static final Image SERVICE = SymfonyIconGroup.service();
    public static final Image SERVICE_OPACITY = SymfonyIconGroup.service_opacity();
    public static final Image FORM_TYPE = SymfonyIconGroup.form_type();
    public static final Image FORM_TYPE_FILE = SymfonyIconGroup.form_type_file();
    public static final Image CONTROLLER_FILE = SymfonyIconGroup.controller_file();
    public static final Image FORM_TYPE_WEAK = SymfonyIconGroup.form_type_weak();
    public static final Image TRANSLATION = SymfonyIconGroup.translation();
    public static final Image TRANSLATION_WEAK = SymfonyIconGroup.translation_weak();
    public static final Image PARAMETER = SymfonyIconGroup.parameter();
    public static final Image FORM_OPTION = SymfonyIconGroup.form_option();
    public static final Image FORM_EXTENSION = SymfonyIconGroup.form_extension();
    public static final Image FORM_EXTENSION_WEAK = SymfonyIconGroup.form_extension_weak();
    public static final Image SERVICE_TAG = SymfonyIconGroup.service_tag();
    public static final Image SERVICE_TAG_WEAK = SymfonyIconGroup.service_tag_weak();
    public static final Image EVENT = SymfonyIconGroup.event();
    public static final Image BUNDLE = SymfonyIconGroup.bundle();

    public static final Image SERVICE_PRIVATE_OPACITY = SymfonyIconGroup.service_private_opacity();

    public static final Image SERVICE_LINE_MARKER = SymfonyIconGroup.service_line_marker();
    public static final Image FORM_TYPE_LINE_MARKER = SymfonyIconGroup.form_type_line_marker();
    public static final Image ROUTE_LINE_MARKER = SymfonyIconGroup.route_line_marker();
    public static final Image TWIG_CONTROLLER_LINE_MARKER = SymfonyIconGroup.twig_controller_line_marker();
    public static final Image TWIG_LINE_MARKER = SymfonyIconGroup.twig_line_marker();
    public static final Image TWIG_LINE_OVERWRITE = SymfonyIconGroup.overwrite();

    public static final Image TWIG_CONTROLLER_FILE = SymfonyIconGroup.twig_controller_file();
    public static final Image TWIG_EXTENDS_FILE = SymfonyIconGroup.twig_extends_file();
    public static final Image TWIG_IMPLEMENTS_FILE = SymfonyIconGroup.twig_implements_file();

    public static final Image PARAMETER_OPACITY = SymfonyIconGroup.parameter_opacity();

    public static final Image MAIL = SymfonyIconGroup.mail();
    public static final Image PROFILER_LINK = SymfonyIconGroup.profiler_link();

    public static final Image CONFIG_PROTOTYPE = SymfonyIconGroup.config_prototype();
    public static final Image CONFIG_VALUE = SymfonyIconGroup.config_value();
    public static final Image CONFIG_VALUE_SHORTCUT = SymfonyIconGroup.config_value_shortcut();

    public static final Image TWIG_BLOCK_OVERWRITE = SymfonyIconGroup.twig_block_overwrite();

    public static final Image SYMFONY_AI = SymfonyIconGroup.symfony_ai();
    public static final Image SYMFONY_AI_OPACITY = SymfonyIconGroup.symfony_ai_opacity();
    public static final Image SYMFONY_ATTRIBUTE = SymfonyIconGroup.symfony_attribute();
    public static final Image SYMFONY_COMMAND_FILE = SymfonyIconGroup.symfony_command_file();
}
