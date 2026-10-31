package fr.adrienbrault.idea.symfony2plugin.stimulus;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StimulusController;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class for Stimulus controller completion.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class StimulusControllerCompletion {
    public static final StimulusControllerCompletion INSTANCE = new StimulusControllerCompletion();

    private StimulusControllerCompletion() {
    }

    public static Map<String, StimulusController> getAllControllers(Project project) {
        Map<String, StimulusController> controllers = new LinkedHashMap<>();
        Collection<String> keys = FileBasedIndex.getInstance().getAllKeys(StimulusControllerStubIndex.KEY, project);

        for (String key : keys) {
            for (StimulusController controller : FileBasedIndex.getInstance()
                .getValues(StimulusControllerStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                controllers.put(key, controller);
            }
        }

        return controllers;
    }

    public static LookupElement createLookupElement(StimulusController controller, boolean forTwig) {
        String name = forTwig ? controller.getTwigName() : controller.getNormalizedName();
        String typeText = forTwig && controller.hasOriginalName() ? controller.getNormalizedName() : "Stimulus";

        return LookupElementBuilder
            .create(name)
            .withTypeText(typeText)
            .withIcon(Symfony2Icons.SYMFONY)
            .withBoldness(true);
    }
}
