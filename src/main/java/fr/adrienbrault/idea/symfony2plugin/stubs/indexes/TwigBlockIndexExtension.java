package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigCompositeElement;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigBlockIndexExtension extends FileBasedIndexExtension<String, Set<String>> {

    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_block_names");

    private final KeyDescriptor<String> KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private static final StringSetDataExternalizer DATA_EXTERNALIZER = new StringSetDataExternalizer();

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return fileContent -> {
            Map<String, Set<String>> blocks = new HashMap<>();

            PsiFile psiFile = fileContent.getPsiFile();
            if(psiFile instanceof TwigFile) {
                for (TwigBlock twigBlock : TwigUtil.getBlocksInFile((TwigFile) psiFile)) {
                    String blockName = twigBlock.getName();
                    if (blockName.length() > 255) {
                        continue;
                    }

                    // we only index file scope
                    // {% embed 'foo.html.twig' %}{% block foo %}{% endembed %}
                    PsiElement embedStatement = PsiElementUtils.getParentOfType(twigBlock.getTarget(), TwigElementTypes.EMBED_STATEMENT);
                    if(embedStatement == null) {
                        blocks.putIfAbsent("block", new HashSet<>());
                        blocks.get("block").add(blockName);
                    }
                }

                ElementPattern<PsiElement> twigTagUseNamePattern = TwigPattern.getTwigTagUseNamePattern();
                // TwigExtendsTag is a TwigCompositeElement
                for(PsiElement psiElement : PsiTreeUtil.getChildrenOfTypeAsList(psiFile, TwigCompositeElement.class)) {
                    if(psiElement instanceof TwigCompositeElement) {
                        // {% use 'foo.html.twig' %}
                        if(psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
                            psiElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                                @Override
                                public void visitElement(PsiElement element) {
                                    if(twigTagUseNamePattern.accepts(element) && PsiElementUtils.getParentOfType(element, TwigElementTypes.EMBED_STATEMENT) == null) {
                                        String templateName = TwigUtil.normalizeTemplateName(PsiElementUtils.trimQuote(element.getText()));
                                        if(templateName.length() < 255 && StringUtils.isNotBlank(templateName)) {
                                            blocks.putIfAbsent("use", new HashSet<>());
                                            blocks.get("use").add(templateName);
                                        }
                                    }

                                    super.visitElement(element);
                                }
                            });
                        }
                    }
                }
            }

            return blocks;
        };
    }

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return KEY_DESCRIPTOR;
    }

    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return DATA_EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
