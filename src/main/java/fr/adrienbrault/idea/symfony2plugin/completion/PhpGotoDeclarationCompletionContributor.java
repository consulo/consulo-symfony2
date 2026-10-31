package fr.adrienbrault.idea.symfony2plugin.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import de.espend.idea.php.annotation.pattern.AnnotationPattern;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import icons.SymfonyIcons;
import consulo.util.lang.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpGotoDeclarationCompletionContributor {
    // Consulo port: nested "Completion" contributor moved to top-level PhpGotoDeclarationCompletionContributorCompletion
    // (Consulo cannot register nested extension classes)

    /**
     * Registered via top-level subclass {@link PhpGotoDeclarationCompletionContributorGotoDeclaration}
     */
    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (psiElement == null) {
                return null;
            }

            Project project = psiElement.getProject();
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return new PsiElement[0];
            }

            PsiElement context = psiElement.getParent();
            Collection<PsiElement> psiElements = new ArrayList<>();
            if (context instanceof StringLiteralExpression) {
                String contents = ((StringLiteralExpression) context).getContents();
                if (StringUtils.isNotBlank(contents)) {
                    // $httpClient->request('', '', ['<caret>' => '']);
                    if (isHttpClientOptionArgument(psiElement)) {
                        consumeHttpClientOptions(project, pair -> {
                            if (pair.getFirst().equals(contents)) {
                                psiElements.add(pair.getSecond().getFirst());
                            }
                        });
                    }

                    // #[Route('/test/te<caret>st/test')]
                    if (AnnotationPattern.getDefaultPropertyValue().accepts(psiElement) || AnnotationPattern.getAttributesDefaultPattern().accepts(context)) {
                       psiElements.addAll(partialRouteNavigation(project, contents, psiElement, offset));
                    }
                }
            }

            return psiElements.toArray(new PsiElement[0]);
        }

        /**
         * "#[Route('/test/te<caret>st/test')] => '/test/"
         */
        private static Collection<PsiElement> partialRouteNavigation(Project project, String contents, PsiElement psiElement, int offset) {
            int calulatedOffset = offset - psiElement.getTextRange().getStartOffset();
            if (calulatedOffset < 0) {
                calulatedOffset = 0;
            }

            // find the nearest full path: "foo/fo<caret>/foo" => "/foo/"
            int lastSlash = contents.indexOf("/", calulatedOffset);
            if (lastSlash < 0) {
                lastSlash = contents.length();
            }

            String partialUrl = contents.substring(0, lastSlash);
            if (partialUrl.equalsIgnoreCase(contents) || partialUrl.length() < 3) {
                return Collections.emptyList();
            }

            String partialUrlWithSlash = "/" + StringUtils.strip(contents.substring(0, lastSlash), "/") + "/";

            Set<PsiElement> targets = new HashSet<>();

            for (Route route : RouteHelper.getAllRoutes(project).values()) {
                String path = route.getPath();
                if (path == null) {
                    continue;
                }

                if (StringUtils.strip(contents, "/").equalsIgnoreCase(StringUtils.strip(path, "/"))) {
                    continue;
                }

                String routePathWithSlash = "/" + StringUtils.strip(path, "/") .toLowerCase() + "/";
                if (routePathWithSlash.startsWith(partialUrlWithSlash.toLowerCase())) {
                    targets.addAll(RouteHelper.getRouteDefinitionTargets(project, route.getName()));
                }
            }

            return targets;
        }
    }

    // Consulo port: package-private (was private) for PhpGotoDeclarationCompletionContributorCompletion
    static class HttpClientCompletionProvider implements CompletionProvider {
        @Override
        public void addCompletions(CompletionParameters completionParameters, ProcessingContext processingContext, CompletionResultSet completionResultSet) {
            PsiElement psiElement = completionParameters.getOriginalPosition();
            if (psiElement == null) {
                return;
            }

            Project project = psiElement.getProject();
            if (!Symfony2ProjectComponent.isEnabled(project) || !(psiElement.getContext() instanceof StringLiteralExpression)) {
                return;
            }

            if (!isHttpClientOptionArgument(psiElement)) {
                return;
            }

            consumeHttpClientOptions(project, pair -> {
                LookupElementBuilder element = LookupElementBuilder.create(pair.getFirst())
                    .withIcon(SymfonyIcons.Symfony);

                PsiElement value = pair.getSecond().getSecond();
                if (value instanceof PhpTypedElement) {
                    String s = ((PhpTypedElement) value).getType().toString();
                    if (StringUtils.isNotBlank(s)) {
                        element = element.withTypeText(s, true);
                    }
                }

                completionResultSet.addElement(element);
            });
        }
    }

    private static void consumeHttpClientOptions(Project project, Consumer<Pair<String, Pair<PsiElement, PsiElement>>> consumer) {
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, "\\Symfony\\Contracts\\HttpClient\\HttpClientInterface")) {
            Field optionsDefaults = phpClass.findFieldByName("OPTIONS_DEFAULTS", true);
            if (optionsDefaults != null && optionsDefaults.isConstant()) {
                PsiElement defaultValue = optionsDefaults.getDefaultValue();
                if (defaultValue instanceof ArrayCreationExpression) {
                    for (Map.Entry<String, Pair<PsiElement, PsiElement>> entry : PhpElementsUtil.getArrayKeyValueMapWithKeyAndValueElement((ArrayCreationExpression) defaultValue).entrySet()) {
                        consumer.accept(new Pair<>(entry.getKey(), entry.getValue()));
                    }
                }
            }
        }
    }

    private static boolean isHttpClientOptionArgument(PsiElement psiElement) {
        ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.getParent());
        if (arrayCreationExpression == null) {
            return false;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(arrayCreationExpression, 2)
            .withSignature("\\Symfony\\Contracts\\HttpClient\\HttpClientInterface", "request")
            .match();

        if (methodMatchParameter == null) {
            methodMatchParameter = new MethodMatcher.StringParameterMatcher(arrayCreationExpression, 0)
                .withSignature("\\Symfony\\Contracts\\HttpClient\\HttpClientInterface", "withOptions")
                .match();
        }

        return methodMatchParameter != null;
    }
}
