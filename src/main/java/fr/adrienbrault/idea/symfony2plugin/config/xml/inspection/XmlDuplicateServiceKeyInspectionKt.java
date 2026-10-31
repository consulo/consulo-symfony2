package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlAttributeValue;
import consulo.xml.language.psi.XmlTag;

import java.util.Objects;

/**
 * Top-level functions of upstream XmlDuplicateServiceKeyInspection.kt
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class XmlDuplicateServiceKeyInspectionKt {
    private XmlDuplicateServiceKeyInspectionKt() {
    }

    public static void visitRoot(
        XmlAttributeValue xmlAttributeValue,
        ProblemsHolder holder,
        String root,
        String child,
        String tagName,
        String message
    ) {
        PsiElement xmlAttributeParent = xmlAttributeValue.getParent();
        if (!(xmlAttributeParent instanceof XmlAttribute)) {
            return;
        }

        XmlAttribute xmlAttribute = (XmlAttribute) xmlAttributeParent;
        if (!tagName.equals(xmlAttribute.getName())) {
            return;
        }

        XmlTag xmlTag = xmlAttribute.getParent();
        if (xmlTag == null) {
            return;
        }

        PsiElement rootContext = xmlTag.getParent();
        if (!(rootContext instanceof XmlTag)) {
            return;
        }

        XmlTag rootContextXmlTag = (XmlTag) rootContext;
        if (!child.equals(xmlTag.getName()) || !root.equals(rootContextXmlTag.getName())) {
            return;
        }

        String value = null;
        boolean valueComputed = false;
        int count = 0;
        for (XmlTag subTag : rootContextXmlTag.findSubTags(child)) {
            if (!valueComputed) {
                value = xmlAttributeValue.getValue();
                valueComputed = true;
            }

            if (Objects.equals(subTag.getAttributeValue(tagName), value)) {
                count++;
                if (count == 2) {
                    break;
                }
            }
        }

        boolean hasDuplicate = count == 2;

        if (hasDuplicate) {
            holder.registerProblem(xmlAttributeValue, message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
