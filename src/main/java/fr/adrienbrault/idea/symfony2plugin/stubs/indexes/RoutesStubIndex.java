package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.xml.language.psi.XmlFile;
import consulo.index.io.DataIndexer;
import consulo.index.io.ID;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.KeyDescriptor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.StubIndexedRoute;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StubIndexedRouteExternalizer;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter.FileInputFilter;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.visitor.AnnotationRouteElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ExtensionImpl
public class RoutesStubIndex extends FileBasedIndexExtension<String, StubIndexedRoute> {

    public static final ID<String, StubIndexedRoute> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.routes_object");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final StubIndexedRouteExternalizer EXTERNALIZER = StubIndexedRouteExternalizer.INSTANCE;

    @Override
    public ID<String, StubIndexedRoute> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public DataIndexer<String, StubIndexedRoute, FileContent> getIndexer() {
        return inputData -> {
            Map<String, StubIndexedRoute> map = new HashMap<>();

            PsiFile psiFile = inputData.getPsiFile();
            if(!Symfony2ProjectComponent.isEnabledForIndex(psiFile.getProject())) {
                return map;
            }

            if(psiFile instanceof YAMLFile) {
                if(!isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
                if(yamlDocument == null) {
                    return map;
                }

                for(StubIndexedRoute indexedRoutes: RouteHelper.getYamlRouteDefinitions(yamlDocument)) {
                    String name = indexedRoutes.getName();
                    if (name.length() < 255) {
                        map.put(name, indexedRoutes);
                    }
                }

                return map;
            } else if(psiFile instanceof XmlFile) {
                for(StubIndexedRoute indexedRoutes: RouteHelper.getXmlRouteDefinitions((XmlFile) psiFile)) {
                    String name = indexedRoutes.getName();
                    if (name.length() < 255) {
                        map.put(name, indexedRoutes);
                    }
                }
            } else if(psiFile instanceof PhpFile) {
                // @Route(), #[Route], RoutingConfigurator fluent routes
                if(!isValidForIndex(inputData, psiFile)) {
                    return map;
                }

                AnnotationRouteElementVisitor visitor = new AnnotationRouteElementVisitor(map);
                for (PhpClass phpClass : PhpPsiUtil.findAllClasses((PhpFile) psiFile)) {
                    visitor.visitFile(phpClass);
                }

                for (StubIndexedRoute indexedRoutes : RouteHelper.getPhpRouteDefinitions((PhpFile) psiFile)) {
                    String name = indexedRoutes.getName();
                    if (name.length() < 255) {
                        map.put(name, indexedRoutes);
                    }
                }
            }

            return map;
        };

    }

    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public DataExternalizer<StubIndexedRoute> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return FileInputFilter.XML_YAML_PHP;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public int getVersion() {
        return 6;
    }

    private static boolean isValidForIndex(FileContent inputData, PsiFile psiFile) {

        String fileName = psiFile.getName();
        if(fileName.startsWith(".") || fileName.endsWith("Test")) {
            return false;
        }

        VirtualFile baseDir = ProjectUtil.getProjectDir(psiFile.getProject());
        if(baseDir == null) {
            return false;
        }

        // is Test file in path name
        String relativePath = VirtualFileUtil.getRelativePath(inputData.getFile(), baseDir, '/');
        if(relativePath != null && (relativePath.contains("/Test/") || relativePath.contains("/Fixtures/"))) {
            return false;
        }

        return true;
    }
}


