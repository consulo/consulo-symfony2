package fr.adrienbrault.idea.symfony2plugin.vite;

import com.intellij.lang.javascript.psi.JSArgumentList;
import consulo.javascript.ecmascript.psi.ES6ExportDefaultAssignment;
import com.intellij.lang.javascript.psi.JSBlockStatement;
import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSConditionalExpression;
import com.intellij.lang.javascript.psi.JSExpression;
import com.intellij.lang.javascript.psi.JSFile;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.JSFunctionExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression;
import com.intellij.lang.javascript.psi.JSParameter;
import com.intellij.lang.javascript.psi.JSParenthesizedExpression;
import com.intellij.lang.javascript.psi.JSProperty;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSReturnStatement;
import com.intellij.lang.javascript.psi.JSSpreadExpression;
import com.intellij.lang.javascript.psi.JSVariable;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Parses vite.config.js / vite.config.ts files and extracts entry points
 * from input, build.rollupOptions.input and build.rolldownOptions.input.
 *
 * ```javascript
 * const entries = { app: './assets/app.js' };
 * export default defineConfig({ input: { ...entries } });
 * ```
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class ViteConfigParser {
    public static final ViteConfigParser INSTANCE = new ViteConfigParser();

    private ViteConfigParser() {
    }

    public List<ViteEntry> parseEntries(PsiFile file) {
        if (!(file instanceof JSFile jsFile)) {
            return Collections.emptyList();
        }
        return new LocalConfig(jsFile).parseEntries();
    }

    private static class LocalConfig {
        private final JSFile myFile;
        private final Collection<JSVariable> myVariables;
        private final Set<JSExpression> myActiveExpressions = new HashSet<>();
        private final Set<JSObjectLiteralExpression> myActiveObjects = new HashSet<>();

        LocalConfig(JSFile file) {
            myFile = file;
            myVariables = PsiTreeUtil.findChildrenOfType(file, JSVariable.class);
        }

        List<ViteEntry> parseEntries() {
            List<ViteEntry> entries = new ArrayList<>();
            for (ES6ExportDefaultAssignment export : PsiTreeUtil.findChildrenOfType(myFile, ES6ExportDefaultAssignment.class)) {
                for (JSObjectLiteralExpression config : objects(export.getExpression(), 0, true)) {
                    Map<String, JSProperty> properties = properties(config, 0);
                    JSProperty buildProperty = properties.get("build");
                    List<JSObjectLiteralExpression> builds = objects(buildProperty != null ? buildProperty.getValue() : null, 0);

                    List<JSProperty> nestedInputs = new ArrayList<>();
                    for (JSObjectLiteralExpression build : builds) {
                        Map<String, JSProperty> buildProperties = properties(build, 0);
                        JSProperty options = buildProperties.get("rolldownOptions");
                        if (options == null) {
                            options = buildProperties.get("rollupOptions");
                        }

                        for (JSObjectLiteralExpression optionsObject : objects(options != null ? options.getValue() : null, 0)) {
                            JSProperty input = properties(optionsObject, 0).get("input");
                            if (input != null) {
                                nestedInputs.add(input);
                            }
                        }
                    }

                    List<JSProperty> inputs = nestedInputs;
                    if (inputs.isEmpty()) {
                        inputs = new ArrayList<>();
                        JSProperty input = properties.get("input");
                        if (input != null) {
                            inputs.add(input);
                        }
                    }

                    for (JSProperty input : inputs) {
                        for (JSObjectLiteralExpression obj : objects(input.getValue(), 0)) {
                            for (Map.Entry<String, JSProperty> entry : properties(obj, 0).entrySet()) {
                                JSProperty property = entry.getValue();
                                String path = property.getValue() instanceof JSLiteralExpression literal ? getStringValue(literal) : null;
                                entries.add(new ViteEntry(entry.getKey(), path, myFile.getVirtualFile(), property));
                            }
                        }
                    }
                }
            }

            // distinctBy { it.psiElement }
            Set<PsiElement> seen = new HashSet<>();
            List<ViteEntry> result = new ArrayList<>();
            for (ViteEntry entry : entries) {
                if (seen.add(entry.getPsiElement())) {
                    result.add(entry);
                }
            }

            return result;
        }

        private List<JSObjectLiteralExpression> objects(@Nullable JSExpression expression, int depth) {
            return objects(expression, depth, false);
        }

        // Only follow local syntax: this parser also runs inside a file-based indexer.
        private List<JSObjectLiteralExpression> objects(@Nullable JSExpression expression, int depth, boolean config) {
            if (expression == null || depth > 32 || !myActiveExpressions.add(expression)) {
                return Collections.emptyList();
            }

            try {
                if (expression instanceof JSObjectLiteralExpression objectLiteral) {
                    return Collections.singletonList(objectLiteral);
                }
                else if (expression instanceof JSParenthesizedExpression parenthesized) {
                    return objects(parenthesized.getInnerExpression(), depth + 1, config);
                }
                else if (expression instanceof JSReferenceExpression reference) {
                    return objects(localInitializer(reference), depth + 1, config);
                }
                else if (expression instanceof JSConditionalExpression conditional) {
                    List<JSObjectLiteralExpression> results = new ArrayList<>(objects(conditional.getThen(), depth + 1, config));
                    results.addAll(objects(conditional.getElse(), depth + 1, config));
                    return results;
                }
                else if (expression instanceof JSCallExpression call) {
                    JSReferenceExpression method = call.getMethodExpression() instanceof JSReferenceExpression ref ? ref : null;
                    if (config && method != null && method.getQualifier() == null && "defineConfig".equals(method.getReferencedName())) {
                        JSArgumentList argumentList = call.getArgumentList();
                        JSExpression[] arguments = argumentList != null ? argumentList.getArguments() : JSExpression.EMPTY_ARRAY;
                        return objects(arguments.length == 1 ? arguments[0] : null, depth + 1, true);
                    }
                    return Collections.emptyList();
                }
                else if (expression instanceof JSFunctionExpression function) {
                    if (!config) {
                        return Collections.emptyList();
                    }

                    JSBlockStatement block = function.getBlock();

                    if (block == null) {
                        JSExpression last = null;
                        for (PsiElement child : function.getChildren()) {
                            if (child instanceof JSExpression childExpression) {
                                last = childExpression;
                            }
                        }
                        return objects(last, depth + 1, true);
                    }

                    List<JSObjectLiteralExpression> results = new ArrayList<>();
                    block.accept(new PsiRecursiveElementWalkingVisitor() {
                        @Override
                        public void visitElement(PsiElement element) {
                            if (element instanceof JSFunction) {
                                return;
                            }
                            if (element instanceof JSReturnStatement returnStatement) {
                                results.addAll(objects(returnStatement.getExpression(), depth + 1, true));
                                return;
                            }
                            super.visitElement(element);
                        }
                    });
                    return results;
                }

                return Collections.emptyList();
            }
            finally {
                myActiveExpressions.remove(expression);
            }
        }

        private Map<String, JSProperty> properties(JSObjectLiteralExpression obj, int depth) {
            if (depth > 32 || !myActiveObjects.add(obj)) {
                return Collections.emptyMap();
            }

            Map<String, JSProperty> result = new LinkedHashMap<>();
            for (PsiElement child : obj.getChildren()) {
                if (child instanceof JSProperty property) {
                    String name = property.getName();
                    if (name != null) {
                        result.put(name, property);
                    }
                }
                else if (child instanceof JSSpreadExpression spread) {
                    for (JSObjectLiteralExpression spreadObject : objects(spread.getExpression(), depth + 1)) {
                        result.putAll(properties(spreadObject, depth + 1));
                    }
                }
            }

            myActiveObjects.remove(obj);
            return result;
        }

        private @Nullable JSExpression localInitializer(JSReferenceExpression ref) {
            if (ref.getQualifier() != null) {
                return null;
            }

            String name = ref.getReferencedName();
            if (name == null) {
                return null;
            }

            PsiElement scope = ref.getParent();
            while (scope != null) {
                if (scope instanceof JSBlockStatement || scope instanceof JSFunction || scope instanceof JSFile) {
                    for (JSVariable variable : myVariables) {
                        if (Objects.equals(variable.getName(), name) && Objects.equals(enclosingScope(variable), scope)) {
                            return variable.getInitializer();
                        }
                    }

                    if (scope instanceof JSFunction function) {
                        for (JSParameter parameter : function.getParameterVariables()) {
                            if (Objects.equals(parameter.getName(), name)) {
                                return null;
                            }
                        }
                    }
                }
                scope = scope.getParent();
            }

            return null;
        }

        private @Nullable PsiElement enclosingScope(PsiElement element) {
            PsiElement parent = element.getParent();
            while (parent != null && !(parent instanceof JSBlockStatement) && !(parent instanceof JSFunction) && !(parent instanceof JSFile)) {
                parent = parent.getParent();
            }
            return parent;
        }

        private static @Nullable String getStringValue(JSLiteralExpression literal) {
            return literal.getStringValue();
        }
    }
}
