package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.CharFilter;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.textCompletion.TextCompletionProvider;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Class name field with completion of PHP class names; the consumer is notified (delayed) on typing
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ClassCompletionPanelWrapper {
    private final Consumer<String> myConsumer;

    private final EditorBox myField;

    private @Nullable Future<?> myUpdateRequest;
    private boolean myDisposed = false;

    @RequiredUIAccess
    public ClassCompletionPanelWrapper(Project project, Disposable parentDisposable, Consumer<String> consumer) {
        myConsumer = consumer;

        myField = project.getApplication().getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .completion(new PhpClassNameCompletionProvider(project))
            .build();

        UIAccess uiAccess = UIAccess.current();
        myField.addValueListener(event -> {
            String text = getClassName();
            if (StringUtil.isEmpty(text) || StringUtil.endsWith(text, "\\")) {
                return;
            }

            addUpdateRequest(uiAccess, 250, () -> myConsumer.accept(getClassName()));
        });

        Disposer.register(parentDisposable, () -> {
            myDisposed = true;
            cancelUpdateRequest();
        });
    }

    public Component getComponent() {
        return myField;
    }

    public String getClassName() {
        return StringUtil.notNullize(myField.getValue());
    }

    @RequiredUIAccess
    public void setClassName(String clazz) {
        myField.setValue(clazz, false);
        myConsumer.accept(getClassName());
    }

    private void addUpdateRequest(UIAccess uiAccess, int delay, @RequiredUIAccess Runnable runnable) {
        if (myDisposed) {
            return;
        }

        cancelUpdateRequest();
        myUpdateRequest = AppExecutorUtil.getAppScheduledExecutorService().schedule(
            () -> uiAccess.give(() -> {
                if (!myDisposed) {
                    runnable.run();
                }
            }),
            delay,
            TimeUnit.MILLISECONDS
        );
    }

    private void cancelUpdateRequest() {
        Future<?> updateRequest = myUpdateRequest;
        if (updateRequest != null) {
            updateRequest.cancel(false);
            myUpdateRequest = null;
        }
    }

    /**
     * Completes class names; the inserted value is the FQN without the leading backslash
     * (PhpCompletionUtil.installClassCompletion is bound to the Swing EditorTextField)
     */
    private static class PhpClassNameCompletionProvider implements TextCompletionProvider {
        private final Project myProject;

        private PhpClassNameCompletionProvider(Project project) {
            myProject = project;
        }

        @Override
        public @Nullable String getAdvertisement() {
            return null;
        }

        @Override
        public @Nullable String getPrefix(String text, int offset) {
            // the short name is matched, the typed namespace filters the classes
            String typed = text.substring(0, offset);
            return typed.substring(typed.lastIndexOf('\\') + 1);
        }

        @Override
        public CompletionResultSet applyPrefixMatcher(CompletionResultSet result, String prefix) {
            return result.withPrefixMatcher(new PlainPrefixMatcher(prefix)).caseInsensitive();
        }

        @Override
        public CharFilter.@Nullable Result acceptChar(char c) {
            return null;
        }

        @RequiredReadAction
        @Override
        public void fillCompletionVariants(CompletionParameters parameters, String prefix, CompletionResultSet result) {
            String text = parameters.getEditor().getDocument().getText();
            int offset = Math.min(parameters.getOffset(), text.length());

            String typed = text.substring(0, offset);
            int namespaceEnd = typed.lastIndexOf('\\');
            String namespace = namespaceEnd < 0 ? null : PhpLangUtil.toFQN(typed.substring(0, namespaceEnd + 1));

            PhpIndex phpIndex = PhpIndex.getInstance(myProject);
            for (String name : phpIndex.getAllClassNames(result.getPrefixMatcher())) {
                for (PhpClass phpClass : phpIndex.getClassesByName(name)) {
                    String classNamespace = PhpLangUtil.getParentNamespaceFQN(phpClass.getFQN());
                    if (namespace != null && !PhpLangUtil.equalsNamespaceNames(classNamespace, namespace)) {
                        continue;
                    }

                    String fqn = PhpLangUtil.toPresentableFQN(phpClass.getFQN());
                    result.addElement(
                        LookupElementBuilder.create(phpClass, phpClass.getName())
                            .withLookupString(fqn)
                            .withTypeText(classNamespace, true)
                            .withInsertHandler((context, item) -> context.getDocument().replaceString(0, context.getTailOffset(), fqn))
                    );
                }
            }
        }
    }
}
