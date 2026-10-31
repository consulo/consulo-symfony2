package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class TwigRouteUsageStubIndex extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_route_usages");

    private static final KeyDescriptor<String> KEY_DESCRIPTOR = new EnumeratorStringDescriptor();
    private static final DataExternalizer<Set<String>> DATA_EXTERNALIZER = new StringSetDataExternalizer();

    @Override
    public ID<String, Set<String>> getName() {
        return KEY;
    }

    // Index-safe only: no PhpIndex/type resolution here.
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Set<String>> map = new HashMap<>();
            PsiFile psiFile = inputData.getPsiFile();
            if (!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject()) || !(psiFile instanceof TwigFile twigFile)) {
                return map;
            }

            for (Usage usage : TwigRouteUsageStubIndexKt.collectTwigRouteUsages(twigFile)) {
                map.putIfAbsent(usage.getRouteName(), new HashSet<>());
                map.get(usage.getRouteName()).add(usage.getKind().name().toLowerCase(Locale.ROOT));
            }

            return map;
        };
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
        return 1;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return (project, file) -> file.getFileType() == TwigFileType.INSTANCE;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    public enum UsageKind {
        PATH,
        URL,
        COMPARE,
        SAME_AS,
        IN_ARRAY,
    }

    public static final class Usage {
        private final String myRouteName;
        private final PsiElement myTarget;
        private final UsageKind myKind;

        public Usage(String routeName, PsiElement target, UsageKind kind) {
            myRouteName = routeName;
            myTarget = target;
            myKind = kind;
        }

        public String getRouteName() {
            return myRouteName;
        }

        public PsiElement getTarget() {
            return myTarget;
        }

        public UsageKind getKind() {
            return myKind;
        }

        public String component1() {
            return myRouteName;
        }

        public PsiElement component2() {
            return myTarget;
        }

        public UsageKind component3() {
            return myKind;
        }

        public Usage copy(String routeName, PsiElement target, UsageKind kind) {
            return new Usage(routeName, target, kind);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Usage usage)) {
                return false;
            }
            return myRouteName.equals(usage.myRouteName) && myTarget.equals(usage.myTarget) && myKind == usage.myKind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myRouteName, myTarget, myKind);
        }

        @Override
        public String toString() {
            return "Usage(routeName=" + myRouteName + ", target=" + myTarget + ", kind=" + myKind + ")";
        }
    }
}
