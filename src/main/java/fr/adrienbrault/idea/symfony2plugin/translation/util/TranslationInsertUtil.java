package fr.adrienbrault.idea.symfony2plugin.translation.util;

import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlElementFactory;
import consulo.xml.language.psi.XmlFile;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jspecify.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationInsertUtil {
    @Nullable
    public static PsiElement invokeTranslation(final PsiFile psiFile, final String keyName, final String translation) {
        if(psiFile instanceof YAMLFile) {
            return invokeTranslation((YAMLFile) psiFile, keyName, translation);
        } else if(psiFile instanceof XmlFile && TranslationUtil.isSupportedXlfFile(psiFile)) {
            return invokeTranslation((XmlFile) psiFile, keyName, translation);
        }

        return null;
    }

    private static PsiElement invokeTranslation(final YAMLFile yamlFile, final String keyName, final String translation) {
        String[] split = keyName.split("\\.");
        PsiElement psiElement = YamlHelper.insertKeyIntoFile(yamlFile, "'" + translation + "'", split);
        if(psiElement == null) {
            return null;
        }

        // resolve target to get value
        YAMLKeyValue target = YAMLUtil.getQualifiedKeyInFile(yamlFile, split);
        if(target != null && target.getValue() != null) {
            return target.getValue();
        } else if(target != null) {
            return target;
        }

        return yamlFile;
    }

    @Nullable
    public static XmlTag invokeTranslation(final XmlFile xmlFile, final String keyName, final String translation) {
        XmlTag rootTag = xmlFile.getRootTag();
        if(rootTag == null) {
            return null;
        }

        String version = rootTag.getAttributeValue("version");
        if(version == null) {
            return null;
        }

        XmlTag file = rootTag.findFirstSubTag("file");
        if(file == null) {
            return null;
        }

        // version="1.2"
        if(version.equalsIgnoreCase("1.2")) {
            Function<XmlTag, XmlTag> func12 = body -> {
                XmlElementFactory instance = XmlElementFactory.getInstance(xmlFile.getProject());

                XmlTag source = instance.createTagFromText("<source/>");
                source.getValue().setText(keyName);

                XmlTag target = instance.createTagFromText("<target/>");
                target.getValue().setText(translation);

                XmlTag transUnit = instance.createTagFromText("<trans-unit/>");
                transUnit.setAttribute("id", String.valueOf(getIdForNewXlfUnit(body, "trans-unit")));

                transUnit.addSubTag(source, false);
                transUnit.addSubTag(target, false);

                return body.addSubTag(transUnit, false);
            };

            XmlTag body = file.findFirstSubTag("body");
            if(body != null) {
                return func12.apply(body);
            }
        } else if(version.equalsIgnoreCase("2.0")) {
            Function<XmlTag, XmlTag> func20 = body -> {
                XmlElementFactory instance = XmlElementFactory.getInstance(xmlFile.getProject());

                XmlTag source = instance.createTagFromText("<source/>");
                source.getValue().setText(keyName);

                XmlTag target = instance.createTagFromText("<target/>");
                target.getValue().setText(translation);

                XmlTag transUnit = instance.createTagFromText("<unit/>");
                transUnit.setAttribute("id", String.valueOf(String.valueOf(getIdForNewXlfUnit(body, "unit"))));

                XmlTag segment = transUnit.addSubTag(instance.createTagFromText("<segment/>"), false);

                segment.addSubTag(source, false);
                segment.addSubTag(target, false);

                return body.addSubTag(transUnit, false);
            };

            // version="2.0"
            XmlTag group = file.findFirstSubTag("group");
            // version="2.0" shortcut
            return func20.apply(Objects.requireNonNullElse(group, file));
        }

        return null;
    }

    private static int getIdForNewXlfUnit(XmlTag body, String subTag) {
        int lastId = 0;

        for (XmlTag transUnit : body.findSubTags(subTag)) {
            String id = transUnit.getAttributeValue("id");
            if(id == null) {
                continue;
            }

            int integer;
            try {
                integer = Integer.parseInt(id);
            } catch (NumberFormatException e) {
                continue;
            }

            // next safe id
            if(integer >= lastId) {
                lastId = integer + 1;
            }
        }

        return lastId;
    }

    /**
     * Remove TODO; moved to core
     */
    @Deprecated
    public static String findEol(PsiElement psiElement) {
        return findLineSeparator(psiElement);
    }

    public static String findLineSeparator(PsiElement psiElement) {
        for(PsiElement child: YamlHelper.getChildrenFix(psiElement)) {
            if(PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(child)) {
                return child.getText();
            }
        }

        PsiElement[] indentPsiElements = PsiTreeUtil.collectElements(psiElement.getContainingFile(), element ->
            PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(element)
        );

        if(indentPsiElements.length > 0) {
            return indentPsiElements[0].getText();
        }

        return "\n";
    }
}
