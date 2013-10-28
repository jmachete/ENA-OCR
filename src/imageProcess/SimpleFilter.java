
package imageProcess;

import java.io.File;

/**
 *
 * @author Joao Machete
 */
public class SimpleFilter extends javax.swing.filechooser.FileFilter {

    private String m_description;
    private String m_extension;
    private String[] extensions;

    public SimpleFilter(String extension, String description) {
        m_description = description;
        m_extension = extension.toLowerCase();
        extensions = m_extension.split(";");
    }

    @Override
    public String getDescription() {
        return m_description;
    }

    public String getExtension() {
        return m_extension;
    }

    @Override
    public boolean accept(File f) {
        if (f == null) {
            return false;
        }
        if (f.isDirectory()) {
            return true;
        }
        if (m_extension.equals("*")) {
            return true;
        }

        String lowerCaseFileName = f.getName().toLowerCase();

        for (String ext : extensions) {
            if (lowerCaseFileName.endsWith("." + ext)) {
                return true;
            }
        }

        return false;
    }
}
