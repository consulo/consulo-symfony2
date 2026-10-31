package fr.adrienbrault.idea.symfony2plugin.dic.intention;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpExpression;
import com.jetbrains.php.lang.psi.elements.FieldReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.component.util.Iconable;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.inspection.IntentionAndQuickFixAction;
import consulo.language.editor.intention.HighPriorityAction;
import consulo.language.editor.intention.IntentionMetaData;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import fr.adrienbrault.idea.symfony2plugin.completion.ServicePropertyInsertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import icons.SymfonyIcons;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ExtensionImpl
@IntentionMetaData(ignoreId = "symfony.PhpPropertyArgumentIntention", fileExtensions = "php", categories = "Symfony")
public class PhpPropertyArgumentIntention extends IntentionAndQuickFixAction implements Iconable, HighPriorityAction {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Symfony: Add Property Service");
    }

    @Override
    public boolean isAvailable(Project project, @Nullable Editor editor, PsiFile file) {
        if (editor == null) {
            return false;
        }

        // consulo-port: plugin.xml <language>PHP</language>
        PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
        if (elementAt == null || !elementAt.getLanguage().isKindOf(PhpLanguage.INSTANCE)) {
            return false;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference != null) {
            String name = fieldReference.getName();
            if (name != null && name.length() > 2) {
                PhpExpression classReference1 = fieldReference.getClassReference();
                if (classReference1 != null && "this".equals(classReference1.getName())) {
                    PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
                    if (phpClassScope != null) {
                        if (phpClassScope.findFieldByName(name, false) == null) {
                            return ServiceUtil.isPhpClassAService(phpClassScope);
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void applyFix(Project project, PsiFile file, @Nullable Editor editor) {
        if (editor == null) {
            return;
        }

        FieldReference fieldReference = getElement(editor, file);
        if (fieldReference == null) {
            return;
        }

        String name = fieldReference.getName();
        if (name == null) {
            return;
        }

        String methodName = null;
        PsiElement parent = fieldReference.getParent();
        if (parent instanceof MethodReference methodReference) {
            String name1 = methodReference.getName();
            if (StringUtils.isNotBlank(name1)) {
                methodName = name1;
            }
        }

        List<String> injectionService = ServicePropertyInsertUtil.getInjectionService(project, name, methodName)
            .stream()
            .map(it -> StringUtils.stripStart(it, "\\"))
            .collect(Collectors.toList());

        if (injectionService.size() == 1) {
            buildProperty(project, fieldReference, injectionService.get(0));
            return;
        }

        List<String> phpClasses = injectionService
            .stream()
            .map(it -> PhpIndex.getInstance(project).getAnyByFQN(it).iterator().next())
            .distinct()
            .map(PhpClass::getFQN)
            .collect(Collectors.toList());

        Map<String, Image> icons = new HashMap<>();
        for (String phpClass : phpClasses) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, phpClass);
            if (classInterface != null) {
                icons.put(phpClass, IconDescriptorUpdaters.getIcon(classInterface, 0));
            }
        }

        // consulo-port: upstream renders through PhpGotoTargetRendererProvider.PhpNamedElementPsiElementListCellRenderer (Swing)
        ListPopup popup = JBPopupFactory.getInstance().createListPopup(
            new BaseListPopupStep<>("Symfony: Property Service Suggestions", phpClasses) {
                @Override
                public String getTextFor(String value) {
                    return StringUtils.stripStart(value, "\\");
                }

                @Override
                public @Nullable Image getIconFor(String value) {
                    return icons.get(value);
                }

                @Override
                public @Nullable PopupStep onChosen(String selectedValue, boolean finalChoice) {
                    return doFinalStep(() -> buildProperty(project, fieldReference, selectedValue));
                }
            }
        );

        editor.showPopupInBestPositionFor(popup);
    }

    @Override
    public @Nullable Image getIcon(int flags) {
        return SymfonyIcons.Symfony;
    }

    private static void buildProperty(Project project, FieldReference fieldReference, String classFqn) {
        try {
            PhpClass phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass.class);
            if (phpClassScope == null || !ServiceUtil.isPhpClassAService(phpClassScope)) {
                return;
            }

            WriteCommandAction.writeCommandAction(project)
                .withName("Symfony: Add Property Service")
                .run(() -> ServicePropertyInsertUtil.appendPropertyInjection(phpClassScope, fieldReference.getName(), classFqn));
        }
        catch (Throwable ignored) {
        }
    }

    private static @Nullable FieldReference getElement(Editor editor, PsiFile file) {
        CaretModel caretModel = editor.getCaretModel();

        int position = caretModel.getOffset();
        PsiElement elementAt = file.findElementAt(position);
        if (elementAt == null) {
            return null;
        }

        PsiElement parent = elementAt.getParent();
        return parent instanceof FieldReference fieldReference ? fieldReference : null;
    }
}
