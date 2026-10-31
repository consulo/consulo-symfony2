/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.adrienbrault.idea.symfony2plugin.tests;

import consulo.annotation.component.ServiceImpl;
import consulo.language.editor.completion.CamelHumpMatcher;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProcess;
import consulo.language.editor.completion.CompletionResult;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionSorter;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementWeigher;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The completion service of the tests: the one of the IDE is in its implementation, with the lookup and the progress of
 * the editor. Here the contributors fill a plain result set, the variants are matched against the prefix and nothing is
 * sorted.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
@ServiceImpl
@Singleton
public class HeadlessCompletionService extends CompletionService {
    private static final CompletionSorter SORTER = new CompletionSorter() {
        @Override
        public CompletionSorter weighBefore(String beforeId, LookupElementWeigher... weighers) {
            return this;
        }

        @Override
        public CompletionSorter weighAfter(String afterId, LookupElementWeigher... weighers) {
            return this;
        }

        @Override
        public CompletionSorter weigh(LookupElementWeigher weigher) {
            return this;
        }
    };

    @Override
    public void setAdvertisementText(@Nullable String text) {
    }

    @Override
    protected CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer,
                                                  CompletionContributor contributor, PrefixMatcher matcher) {
        return new ResultSet(parameters, consumer, contributor, matcher);
    }

    /**
     * Like the IDE's: the identifier before the caret in the element at the caret
     */
    @Override
    protected String suggestPrefix(CompletionParameters parameters) {
        PsiElement position = parameters.getPosition();
        String text = position.getText().substring(0, parameters.getOffset() - position.getTextRange().getStartOffset());
        if (text.isEmpty() || Character.isWhitespace(text.charAt(text.length() - 1))) {
            return "";
        }
        text = text.trim();
        int i = text.length() - 1;
        while (i >= 0 && Character.isJavaIdentifierPart(text.charAt(i))) {
            i--;
        }
        return text.substring(i + 1);
    }

    @Override
    protected PrefixMatcher createMatcher(String prefix, boolean typoTolerant) {
        return new CamelHumpMatcher(prefix, true, typoTolerant);
    }

    @Override
    public @Nullable CompletionProcess getCurrentCompletion() {
        return null;
    }

    @Override
    public CompletionSorter defaultSorter(CompletionParameters parameters, PrefixMatcher matcher) {
        return SORTER;
    }

    @Override
    public CompletionSorter emptySorter() {
        return SORTER;
    }

    private static class ResultSet extends CompletionResultSet {
        private final CompletionParameters myParameters;

        ResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer, CompletionContributor contributor, PrefixMatcher matcher) {
            super(matcher, consumer, contributor);
            myParameters = parameters;
        }

        @Override
        public void addElement(LookupElement element) {
            CompletionResult result = CompletionResult.wrap(element, getPrefixMatcher(), SORTER);
            if (result != null) {
                passResult(result);
            }
        }

        @Override
        public CompletionResultSet withPrefixMatcher(PrefixMatcher matcher) {
            return new ResultSet(myParameters, getConsumer(), myContributor, matcher);
        }

        @Override
        public CompletionResultSet withPrefixMatcher(String prefix) {
            return withPrefixMatcher(getPrefixMatcher().cloneWithPrefix(prefix));
        }

        @Override
        public CompletionResultSet withRelevanceSorter(CompletionSorter sorter) {
            return this;
        }

        @Override
        public void addLookupAdvertisement(String text) {
        }

        @Override
        public CompletionResultSet caseInsensitive() {
            return withPrefixMatcher(new CamelHumpMatcher(getPrefixMatcher().getPrefix(), false));
        }

        @Override
        public void restartCompletionOnPrefixChange(ElementPattern<String> prefixCondition) {
        }

        @Override
        public void restartCompletionWhenNothingMatches() {
        }
    }
}
