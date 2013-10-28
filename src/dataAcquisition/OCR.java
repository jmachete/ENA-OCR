
package dataAcquisition;

import imageProcess.ImageIOHelper;
import java.io.*;
import java.util.*;
import javax.imageio.IIOImage;

public class OCR {
    private final String LANG_OPTION = "-l";
    private final String EOL = System.getProperty("line.separator");

    private String tessPath;

    final static String OUTPUT_FILE_NAME = "TessOutput";
    final static String FILE_EXTENSION = ".txt";
    
    /** Creates a new instance of OCR */
    public OCR(String tessPath) {
        this.tessPath = tessPath;
    }

    /**
     *
     * @param imageList
     * @param index
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    public String recognizeText(final List<IIOImage> imageList, final int index, final String lang) throws Exception {
        List<File> tempImageFiles = ImageIOHelper.createImageFiles(imageList, index);
        return recognizeText(tempImageFiles, lang);
    }
    /**
     *
     * @param imageFile
     * @param index
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    public String recognizeText(final File imageFile, final int index, final String lang) throws Exception {
        List<File> tempImageFiles = ImageIOHelper.createImageFiles(imageFile, index);
        return recognizeText(tempImageFiles, lang);
    }

    /**
     * 
     * @param tempImageFiles
     * @param lang
     * @return
     * @throws java.lang.Exception
     */
    public String recognizeText(final List<File> tempImageFiles, final String lang) throws Exception {
        File tempTessOutputFile = File.createTempFile(OUTPUT_FILE_NAME, FILE_EXTENSION);
        String outputFileName = tempTessOutputFile.getPath().substring(0, tempTessOutputFile.getPath().length() - FILE_EXTENSION.length()); // chop the .txt extension
        StringBuffer strB = new StringBuffer();
        
        List<String> cmd = new ArrayList<String>();
        cmd.add(tessPath + "/tesseract");
        cmd.add(""); // placeholder for inputfile
        cmd.add(outputFileName);
        cmd.add(LANG_OPTION);
        cmd.add(lang);

        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File(System.getProperty("user.home")));
            
        for (File tempImageFile : tempImageFiles) {
//            ProcessBuilder pb = new ProcessBuilder(tessPath + "/tesseract", tempImageFile.getPath(), outputFileName, LANG_OPTION, lang);
            
            cmd.set(1, tempImageFile.getPath());
            pb.command(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
//            Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
            
            int w = process.waitFor();
            System.out.println("Exit value = " + w);
            
            // delete temp working files
            tempImageFile.delete();
            
            if (w == 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(tempTessOutputFile), "UTF-8"));
                
                String str;
                
                while ((str = in.readLine()) != null) {
                    strB.append(str).append(EOL);
                }
                in.close();
            } else {
                String msg;
                switch (w) {
                    case 1:
                        msg = "Errors accessing files.";
                        break;
                    case 29:
                        msg = "Cannot recognize the image or its selected region.";
                        break;
                    case 31:
                        msg = "Unsupported image format.";
                        break;                        
                    default:
                        msg = "Errors occurred.";
                }
                for (File image : tempImageFiles) {
                    image.delete();
                }
                tempTessOutputFile.delete();
                throw new RuntimeException(msg);
            }
            
        }
        tempTessOutputFile.delete();
        return strB.toString();
    }

}