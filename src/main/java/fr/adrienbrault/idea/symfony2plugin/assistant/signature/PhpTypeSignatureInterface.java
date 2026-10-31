package fr.adrienbrault.idea.symfony2plugin.assistant.signature;

import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface PhpTypeSignatureInterface {

    @Nullable
    Collection<? extends PhpNamedElement> getByParameter(Project project, String parameter);

    String getName();
}
