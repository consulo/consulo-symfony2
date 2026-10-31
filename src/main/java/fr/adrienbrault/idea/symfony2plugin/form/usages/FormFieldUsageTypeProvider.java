package fr.adrienbrault.idea.symfony2plugin.form.usages;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.application.util.CachedValueProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.psi.util.LanguageCachedValueUtil;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.localize.LocalizeValue;
import consulo.usage.PsiElementUsageTarget;
import consulo.usage.UsageTarget;
import consulo.usage.UsageType;
import consulo.usage.UsageTypeProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import consulo.language.psi.PsiReference;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class FormFieldUsageTypeProvider implements UsageTypeProvider {
    private static final UsageType FORM_FIELD = new UsageType(LocalizeValue.localizeTODO("Symfony form field"));

    @Override
    public @Nullable UsageType getUsageType(PsiElement element) {
        return null;
    }

    @Override
    public @Nullable UsageType getUsageType(PsiElement element, UsageTarget[] targets) {
        return ReadAction.compute(() -> {
            if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
                return null;
            }

            StringLiteralExpression literal = PsiTreeUtil.getParentOfType(element, StringLiteralExpression.class, false);
            if (literal == null) {
                return null;
            }

            List<FormFieldNameReference> fieldReferences = new ArrayList<>();
            for (PsiReference reference : literal.getReferences()) {
                if (reference instanceof FormFieldNameReference fieldReference) {
                    fieldReferences.add(fieldReference);
                }
            }

            for (UsageTarget target : targets) {
                if (!(target instanceof PsiElementUsageTarget psiElementUsageTarget) || !(psiElementUsageTarget.getElement() instanceof PhpNamedElement member)) {
                    continue;
                }

                if (fieldReferences.stream().anyMatch(reference -> reference.isReferenceTo(member))) {
                    return FORM_FIELD;
                }

                if (!FormFieldUsageTargetUtilKt.isSupportedFormTarget(member)) {
                    continue;
                }

                Set<StringLiteralExpression> literals = LanguageCachedValueUtil.getCachedValue(member, () -> {
                    Set<StringLiteralExpression> found = new HashSet<>();
                    FormFieldReferencesSearchExecutorKt.processFormFieldUsages(member, it -> {
                        found.add(it);
                        return true;
                    });
                    return CachedValueProvider.Result.create(found, PsiModificationTracker.MODIFICATION_COUNT);
                });

                if (literals.contains(literal)) {
                    return FORM_FIELD;
                }
            }

            return null;
        });
    }
}
