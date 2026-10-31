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
import consulo.language.editor.FileModificationService;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * The files of the tests are always writable - the service of the IDE, which asks the version control to make a file
 * writable, is in its implementation.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
@ServiceImpl
@Singleton
public class HeadlessFileModificationService extends FileModificationService {
    @Override
    public boolean preparePsiElementsForWrite(Collection<? extends PsiElement> elements) {
        return true;
    }

    @Override
    public boolean prepareFileForWrite(@Nullable PsiFile psiFile) {
        return true;
    }

    @Override
    public boolean prepareVirtualFilesForWrite(Project project, Collection<VirtualFile> files) {
        return true;
    }
}
