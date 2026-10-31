package fr.adrienbrault.idea.symfony2plugin.templating.documentation;

import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.BinaryExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.DumbService;
import consulo.virtualFileSystem.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * TODO consulo-port: platform.backend.documentation API missing (DocumentationTargetProvider, DocumentationTarget; could become
 *  LanguageDocumentationProvider.getCustomDocumentationElement + generateDoc). Was "implements DocumentationTargetProvider"
 *  registered as platform.backend.documentation.targetProvider; left unregistered. documentationTargets(PsiFile, int) is kept
 *  as a plain method returning the concrete target type (was List&lt;DocumentationTarget&gt;).
 */
public class TwigTemplateDocumentationTargetProvider {
    public List<TwigTemplateDocumentationTarget> documentationTargets(PsiFile file, int offset) {
        TwigTemplateDocumentationContext context = resolveContext(file, offset);
        if (context == null) {
            return Collections.emptyList();
        }

        List<VirtualFile> files = new ArrayList<>(TwigUtil.getTemplateFiles(file.getProject(), context.getName()));
        files.sort(Comparator.comparing(VirtualFile::getPath));

        List<TwigTemplateDocumentationTarget> targets = new ArrayList<>();
        for (VirtualFile virtualFile : files) {
            targets.add(new TwigTemplateDocumentationTarget(context, virtualFile));
        }

        return targets;
    }

    public @Nullable TwigTemplateDocumentationContext resolveContext(PsiFile file, int offset) {
        if (!file.isValid() || !Symfony2ProjectComponent.isEnabled(file.getProject()) || DumbService.isDumb(file.getProject())) {
            return null;
        }

        TwigTemplateDocumentationContext context;
        if (file instanceof PhpFile) {
            PsiElement leaf = file.findElementAt(offset);
            context = leaf != null ? php(leaf) : null;
        }
        else {
            PsiFile twig = file.getViewProvider().getPsi(TwigLanguage.INSTANCE);
            if (twig == null) {
                return null;
            }

            PsiElement leaf = twig.findElementAt(offset);
            context = leaf != null ? twig(leaf) : null;
        }

        if (context == null) {
            return null;
        }

        if (context.getName().isBlank() || TwigUtil.getTemplateFiles(file.getProject(), context.getName()).isEmpty()) {
            return null;
        }

        return context;
    }

    /**
     * Static arguments of TEMPLATE_SIGNATURES and existing Template references, for example:
     * <pre>
     * $this->render('blog.html.twig');
     * $this->renderView(view: 'blog.html.twig', parameters: []);
     * $twig->load('blog.html.twig');
     * $email->htmlTemplate('blog.html.twig');
     * #[Template('blog.html.twig')]
     * #[Template(template: 'blog.html.twig')]
     * </pre>
     * Also supports the corresponding `@Template("blog.html.twig")` annotation.
     */
    private @Nullable TwigTemplateDocumentationContext php(PsiElement leaf) {
        StringLiteralExpression literal;
        if (leaf instanceof StringLiteralExpression stringLiteralExpression) {
            literal = stringLiteralExpression;
        }
        else if (leaf.getParent() instanceof StringLiteralExpression stringLiteralExpression) {
            literal = stringLiteralExpression;
        }
        else {
            return null;
        }

        // Exclude partial names: render('blog.html.twig' . $suffix), render("blog/$name.html.twig").
        if (literal.getParent() instanceof BinaryExpression || PsiTreeUtil.findChildOfType(literal, Variable.class) != null) {
            return null;
        }

        // Existing references also cover Template attributes and annotations.
        if (MethodMatcher.getMatchedSignatureWithDepth(literal, SymfonyPhpReferenceContributor.TEMPLATE_SIGNATURES) == null &&
            !hasTemplateReference(literal)) {
            return null;
        }

        return new TwigTemplateDocumentationContext(literal.getContents(), literal);
    }

    private static boolean hasTemplateReference(PsiElement element) {
        for (PsiReference reference : element.getReferences()) {
            if (reference instanceof TemplateReference) {
                return true;
            }
        }

        return false;
    }

    /**
     * Static template strings, including their quote boundaries:
     * <pre>
     * {% extends 'layout.html.twig' %}
     * {% include 'blog.html.twig' %}
     * {% include ['blog.html.twig', 'fallback.html.twig'] %}
     * {% include condition ? 'blog.html.twig' : 'fallback.html.twig' %}
     * {% embed 'blog.html.twig' %}{% endembed %}
     * {% import 'forms.html.twig' as forms %}
     * {% from 'forms.html.twig' import field %}
     * {% form_theme form 'fields.html.twig' %}
     * {{ include('blog.html.twig') }}
     * {{ source('blog.html.twig') }}
     * {{ block('content', 'blog.html.twig') }}
     * </pre>
     * Excludes dynamic names: `'blog/' ~ name ~ '.html.twig'`, `"blog/#{name}.html.twig"`.
     */
    private @Nullable TwigTemplateDocumentationContext twig(PsiElement leaf) {
        ASTNode node = leaf.getNode();
        IElementType elementType = node != null ? node.getElementType() : null;

        PsiElement anchor = null;
        if (elementType == TwigTokenTypes.STRING_TEXT) {
            anchor = leaf;
        }
        else if (elementType == TwigTokenTypes.SINGLE_QUOTE || elementType == TwigTokenTypes.DOUBLE_QUOTE) {
            for (PsiElement sibling : new PsiElement[]{leaf.getNextSibling(), leaf.getPrevSibling()}) {
                if (sibling == null) {
                    continue;
                }

                ASTNode siblingNode = sibling.getNode();
                if (siblingNode != null && siblingNode.getElementType() == TwigTokenTypes.STRING_TEXT) {
                    anchor = sibling;
                    break;
                }
            }
        }

        if (anchor == null) {
            return null;
        }

        if (TwigTemplateDocumentationTargetProviderKt.templateUsageType(anchor) == null) {
            return null;
        }

        return new TwigTemplateDocumentationContext(anchor.getText(), anchor);
    }
}
