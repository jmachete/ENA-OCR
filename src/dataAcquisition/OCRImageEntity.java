
package dataAcquisition;

import imageProcess.ImageIOHelper;
import java.io.File;
import java.util.List;
import javax.imageio.IIOImage;

public class OCRImageEntity {

    private List<IIOImage> originalImages;
    private File originalImageFile;
    private int index;

    public OCRImageEntity(List<IIOImage> originalImages, int index) {
        this.originalImages = originalImages;
        this.index = index;
    }

    public OCRImageEntity(File originalImageFile, int index) {
        this.originalImageFile = originalImageFile;
        this.index = index;
    }

    /**
     * @return the originalImages
     */
    public List<IIOImage> getOriginalImages() {
        return originalImages;
    }

    /**
     * @return the originalImageFile
     */
    public File getOriginalImageFile() {
        return originalImageFile;
    }

    /**
     * @return the ClonedImageFiles
     */
    public List<File> getClonedImageFiles() throws Exception {
        if (originalImages != null) {
            return ImageIOHelper.createImageFiles(originalImages, index);
        } else {
            return ImageIOHelper.createImageFiles(originalImageFile, index);
        }
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
