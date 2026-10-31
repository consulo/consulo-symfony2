package fr.adrienbrault.idea.symfony2plugin.form;

import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.FormDataClassStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    @RequiredReadAction
    public LineMarkerInfo<?> getLineMarkerInfo(PsiElement psiElement) {
        return null;
    }

    @Override
    @RequiredReadAction
    public void collectSlowLineMarkers(List<PsiElement> psiElements, Collection<LineMarkerInfo> lineMarkerInfos) {
        if(psiElements.isEmpty() || !Symfony2ProjectComponent.isEnabled(psiElements.getFirst())) {
            return;
        }

        Project project = psiElements.getFirst().getProject();
        var classNamePattern = PhpElementsUtil.getClassNamePattern();
        for (PsiElement psiElement : psiElements) {
            if (classNamePattern.accepts(psiElement)) {
                attachFormDataClass(lineMarkerInfos, psiElement);
                attachPhpClassToFormDataClass(project, lineMarkerInfos, psiElement);
            }
        }
    }

    private void attachPhpClassToFormDataClass(Project project, Collection<LineMarkerInfo> lineMarkerInfos, PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        String fqn = ((PhpClass) phpClassContext).getFQN();

        Set<String> classes = FileBasedIndex.getInstance().getValues(FormDataClassStubIndex.KEY, "\\" + StringUtils.stripStart(fqn, "\\"), GlobalSearchScope.allScope(project))
            .stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

        Collection<PhpClass> phpClasses = new HashSet<>();
        for (String clazz: classes) {
            if (PhpElementsUtil.isEqualClassName(clazz, fqn)) {
                continue;
            }
            phpClasses.addAll(PhpElementsUtil.getClassesInterface(project, clazz));
        }

        if (!phpClasses.isEmpty()) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER)
                .setTargets(phpClasses)
                .setTooltipText("Navigate to form");

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }

    private void attachFormDataClass(Collection<LineMarkerInfo> lineMarkerInfos, PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if (phpClassContext == null) {
            return;
        }

        boolean isFormTypeInstance = FormUtil.isFormType((PhpClass) phpClassContext)
            || PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Form\\FormExtensionInterface");

        if (!isFormTypeInstance) {
            return;
        }

        PhpClass formDataClass = FormOptionsUtil.getFormPhpClassFromContext(leaf);
        if (formDataClass != null && !PhpElementsUtil.isEqualClassName(formDataClass.getFQN(), ((PhpClass) phpClassContext).getFQN())) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTargets(formDataClass)
                .setTooltipText("Navigate to data class");

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }

    @Override
    public Language getLanguage() {
        return PhpLanguage.INSTANCE;
    }
}
