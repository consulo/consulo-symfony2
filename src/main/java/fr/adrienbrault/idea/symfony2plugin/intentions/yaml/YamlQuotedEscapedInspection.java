package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiErrorElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jspecify.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class YamlQuotedEscapedInspection extends LocalInspectionTool {
    private static final Pattern UNESCAPED_BACKSLASH_PATTERN = Pattern.compile(".*[^\\\\]\\\\[^\\\\0abtnvfre \"/N_LPxuU].*");

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                IElementType elementType = element.getNode().getElementType();
                if (elementType == YAMLTokenTypes.SCALAR_DSTRING) {
                    if (SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                        // "Foo\Foo" -> "Foo\\Foo"
                        String text = StringUtils.strip(element.getText(), "\"");

                        // dont check to long strings
                        // ascii chars that need to be escape; some @see Symfony\Component\Yaml\Unescaper
                        if (text.length() < 255 && UNESCAPED_BACKSLASH_PATTERN.matcher(text).matches()) {
                            holder.registerProblem(element, "Not escaping a backslash in a double-quoted string is deprecated", ProblemHighlightType.WEAK_WARNING);
                        }
                    }
                }
                else if (elementType == YAMLTokenTypes.TEXT) {
                    if (SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                        // @foo -> "@foo"
                        boolean hasPreviousErrorElement = parentIsErrorAndHasPreviousElement(element);
                        String text = hasPreviousErrorElement
                            ? element.getParent().getPrevSibling().getText()
                            : element.getText();

                        if (text.length() > 1 || hasPreviousErrorElement && !text.isEmpty()) {
                            char startChar = text.charAt(0);
                            switch (startChar) {
                                case '@', '`', '|', '>' -> holder.registerProblem(element, "Deprecated usage of '" + startChar + "' at the beginning of unquoted string", ProblemHighlightType.WEAK_WARNING);

                                case '%' -> {
                                    // deprecated in => "3.1"; but as most user will need to migrate in 2.8 let them know it already
                                    holder.registerProblem(element, "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1", ProblemHighlightType.WEAK_WARNING);
                                }

                                default -> {
                                }
                            }
                        }
                    }
                }
                super.visitElement(element);
            }
        };
    }

    private boolean parentIsErrorAndHasPreviousElement(PsiElement element) {
        return element.getParent() instanceof PsiErrorElement && element.getParent().getPrevSibling() != null;
    }

    @Override
    public String getShortName() {
        return "YamlQuotedEscapedInspection";
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Quoted issues");
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return LocalizeValue.localizeTODO("Yaml");
    }

    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.localizeTODO("Symfony"), getGroupDisplayName()};
    }

    @Override
    public @Nullable Language getLanguage() {
        return YAMLLanguage.INSTANCE;
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WARNING;
    }
}
