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
import consulo.language.Language;
import consulo.language.scratch.RootType;
import consulo.language.scratch.ScratchFileService;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.PerFileMappings;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * The scratch files of the tests: there are none - the service of the IDE is in its implementation, the JSON schema
 * support asks it about every file.
 *
 * @author VISTALL
 * @since 2026-10-31
 */
@ServiceImpl
@Singleton
public class HeadlessScratchFileService extends ScratchFileService {
    private static final PerFileMappings<Language> NO_MAPPINGS = new PerFileMappings<>() {
        @Override
        public void setMapping(@Nullable VirtualFile file, @Nullable Language value) {
        }

        @Override
        public @Nullable Language getMapping(@Nullable VirtualFile file) {
            return null;
        }
    };

    @Override
    public String getRootPath(RootType rootId) {
        throw new UnsupportedOperationException("the tests have no scratch files");
    }

    @Override
    public @Nullable RootType getRootType(@Nullable VirtualFile file) {
        return null;
    }

    @Override
    public VirtualFile findFile(RootType rootType, String pathName, Option option) throws IOException {
        throw new UnsupportedOperationException("the tests have no scratch files");
    }

    @Override
    public PerFileMappings<Language> getScratchesMapping() {
        return NO_MAPPINGS;
    }
}
