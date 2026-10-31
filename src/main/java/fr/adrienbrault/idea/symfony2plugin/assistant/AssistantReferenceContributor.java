package fr.adrienbrault.idea.symfony2plugin.assistant;

import consulo.language.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AssistantReferenceContributor {
    boolean supportData();
    String getAlias();
    boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config);
}
