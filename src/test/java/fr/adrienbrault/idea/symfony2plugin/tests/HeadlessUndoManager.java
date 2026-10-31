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

import consulo.document.DocumentReference;
import consulo.localize.LocalizeValue;
import consulo.undoRedo.UndoManager;
import consulo.undoRedo.UndoableAction;
import consulo.util.lang.Couple;
import org.jspecify.annotations.Nullable;

/**
 * The undo of the tests: there is none - the undo managers of the IDE are in its implementation, and changing a
 * document reports to them.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
abstract class HeadlessUndoManager implements UndoManager {
    @Override
    public void undoableActionPerformed(UndoableAction action) {
    }

    @Override
    public void nonundoableActionPerformed(DocumentReference ref, boolean isGlobal) {
    }

    @Override
    public boolean isUndoInProgress() {
        return false;
    }

    @Override
    public boolean isRedoInProgress() {
        return false;
    }

    @Override
    public void undo(@Nullable Object editor) {
    }

    @Override
    public void redo(@Nullable Object editor) {
    }

    @Override
    public boolean isUndoAvailable(@Nullable Object editor) {
        return false;
    }

    @Override
    public boolean isRedoAvailable(@Nullable Object editor) {
        return false;
    }

    @Override
    public Couple<LocalizeValue> getUndoActionNameAndDescription(Object editor) {
        return Couple.of(LocalizeValue.empty(), LocalizeValue.empty());
    }

    @Override
    public Couple<LocalizeValue> getRedoActionNameAndDescription(Object editor) {
        return Couple.of(LocalizeValue.empty(), LocalizeValue.empty());
    }

    @Override
    public void invalidateActionsFor(DocumentReference ref) {
    }

    @Override
    public boolean canMergeGroup(Object groupId, Object lastGroupId) {
        return false;
    }
}
