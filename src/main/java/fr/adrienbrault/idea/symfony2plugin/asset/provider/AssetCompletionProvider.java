package fr.adrienbrault.idea.symfony2plugin.asset.provider;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.util.ProcessingContext;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetLookupElement;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AssetCompletionProvider implements CompletionProvider {
    final private AssetDirectoryReader assetParser;

    final private boolean includeCustom;

    public AssetCompletionProvider(AssetDirectoryReader assetParser, boolean includeCustom) {
        this.assetParser = assetParser;
        this.includeCustom = includeCustom;
    }

    public AssetCompletionProvider(AssetDirectoryReader assetParser) {
        this(assetParser, false);
    }

    @Override
    public void addCompletions(CompletionParameters parameters, ProcessingContext context, final CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();

        if(!Symfony2ProjectComponent.isEnabled(parameters.getPosition())) {
            return;
        }

        for (AssetFile assetFile : assetParser.getAssetFiles(project)) {
            resultSet.addElement(new AssetLookupElement(assetFile, project));
        }

        if(includeCustom) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            for (String s : twigPathServiceParser.getNamedAssets().keySet()) {
                resultSet.addElement(LookupElementBuilder.create("@" + s).withIcon(PlatformIconGroup.nodesFolder()).withTypeText("Custom Assets", true));
            }
        }
    }
}
