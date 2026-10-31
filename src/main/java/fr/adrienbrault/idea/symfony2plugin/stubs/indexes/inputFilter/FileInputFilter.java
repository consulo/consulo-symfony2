package fr.adrienbrault.idea.symfony2plugin.stubs.indexes.inputFilter;

import com.jetbrains.php.lang.PhpFileType;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.xml.language.XmlFileType;
import org.jetbrains.yaml.YAMLFileType;
import org.jspecify.annotations.Nullable;

/**
 * Files which are allowed for cusomng on index
 *
 * Its a performance issue indexing too many files
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileInputFilter {
    public static final FileBasedIndex.InputFilter XML_YAML_PHP = new XmlYamlAndPhpFileInputFilter();
    public static final FileBasedIndex.InputFilter XML_YAML = new XmlAndYamlFileInputFilter();

    private static class XmlAndYamlFileInputFilter implements FileBasedIndex.InputFilter {
        @Override
        public boolean acceptInput(@Nullable Project project, VirtualFile file) {
            FileType fileType = file.getFileType();

            // "xsd" and other file types are inside file tye, filter by extension
            if (fileType == XmlFileType.INSTANCE) {
                String extension = file.getExtension();
                return "xml".equalsIgnoreCase(extension);
            }

            return fileType == YAMLFileType.YML;
        }
    }

    private static class XmlYamlAndPhpFileInputFilter implements FileBasedIndex.InputFilter {
        @Override
        public boolean acceptInput(@Nullable Project project, VirtualFile file) {
            FileType fileType = file.getFileType();

            // "xsd" and other file types are inside file tye, filter by extension
            if (fileType == XmlFileType.INSTANCE) {
                String extension = file.getExtension();
                return "xml".equalsIgnoreCase(extension);
            }

            return fileType == YAMLFileType.YML || fileType == PhpFileType.INSTANCE;
        }
    }
}
