package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlAttribute;
import consulo.xml.language.psi.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.codeInspection.InspectionUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jspecify.annotations.Nullable;

/**
 * consulo-port: the registered nested inspections ControllerMethodInspection$ControllerMethodInspectionYaml / $ControllerMethodInspectionXml
 * moved to the top-level classes {@link ControllerMethodInspectionYaml} and {@link ControllerMethodInspectionXml}
 * (nested classes cannot carry @ExtensionImpl).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerMethodInspection {
    static final class YamlLazyRouteName implements InspectionUtil.LazyControllerNameResolve {
        private final PsiElement psiElement;

        YamlLazyRouteName(PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @Override
        public @Nullable String getRouteName() {
            YAMLKeyValue defaultKeyValue = PsiTreeUtil.getParentOfType(psiElement.getParent(), YAMLKeyValue.class);
            if (defaultKeyValue == null) {
                return null;
            }

            YAMLKeyValue def = PsiTreeUtil.getParentOfType(defaultKeyValue, YAMLKeyValue.class);
            if (def == null) {
                return null;
            }

            return YamlHelper.getYamlKeyName(def);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return this == o || (o instanceof YamlLazyRouteName that && psiElement.equals(that.psiElement));
        }

        @Override
        public int hashCode() {
            return psiElement.hashCode();
        }
    }

    static final class XmlLazyRouteName implements InspectionUtil.LazyControllerNameResolve {
        private final PsiElement psiElement;

        XmlLazyRouteName(PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @Override
        public @Nullable String getRouteName() {
            XmlTag defaultTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if (defaultTag != null) {
                XmlTag routeTag = PsiTreeUtil.getParentOfType(defaultTag, XmlTag.class);
                if (routeTag != null) {
                    XmlAttribute id = routeTag.getAttribute("id");
                    if (id != null) {
                        return id.getValue();
                    }
                }
            }

            return null;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            return this == o || (o instanceof XmlLazyRouteName that && psiElement.equals(that.psiElement));
        }

        @Override
        public int hashCode() {
            return psiElement.hashCode();
        }
    }
}
