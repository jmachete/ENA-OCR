
package enaocr;

import imageProcess.Utilities;
import dataAcquisition.OCRImageEntity;
import dataAcquisition.OCR;
import dataAcquisition.XmlJDOM;
import imageProcess.ImageIconScalable;
import imageProcess.ImageIOHelper;
import imageProcess.JImageLabel;
import imageProcess.ImageDropTargetListener;
import imageProcess.SimpleFilter;
import java.awt.Color;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.*;
import javax.imageio.*;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import org.jdesktop.application.Task;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.persistence.RollbackException;
import javax.swing.table.DefaultTableModel;
import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.PropertyStateEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.beans.PropertyChangeEvent;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.jdom.JDOMException;
import java.lang.reflect.Method;
import javax.swing.JOptionPane;
import java.util.Arrays;


/**
 *
 * @author Joao Machete
 */
/**
 * The application's main frame.
 */
public class EnaOCRView extends FrameView {

    protected final Preferences prefs = Preferences.userRoot().node("/enaocr");
    static final boolean MAC_OS_X = System.getProperty("os.name").startsWith("Mac");
    static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");
    private JFileChooser fileChooser, chooser, fc;
    private String currentDirectory, inputFilesDirectory, textRecognized;
    private String tessPath, inputPath, selectedInputMethod;
    protected String curLangCode;
    private String [] langCodes, langs, zones, zoneDetailed;
    public static final String APP_NAME = "EnaOCR";
    private final String UTF8 = "UTF-8";
    private final String TIFF = "tiff";
    private Properties prop;
    private File imageFile, folder;
    private File[] listOfFiles;
    private ArrayList<File> listOfImagesFilesToProcess;
    private ArrayList<String> zoneResult;
    private javax.swing.filechooser.FileFilter[] fileFilters, outputFilters;
    private java.util.List<ImageIconScalable> imageList;
    private ImageIconScalable imageIcon;
    private int imageIndex, imageTotal;
    private int originalW, originalH, originalRectangleWidth, originalRectangleHeight, originalRectangleX, originalRectangleY;
    private int counterJComboBoxZones =0;
    private int indexImage = -1;
    private int filterIndex;
    private int value, counter;
    protected ResourceBundle myResources, bundle;
    private boolean reset, change;
    private Frame frame = new Frame();
    private java.util.List<IIOImage> iioImageList;
    private Font font;
    private final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    private DefaultTableModel modelo, resultModel, detaildTableModel;
    private MyTableModel typeOfDocumentModel;
    protected static String selectedLang = "en";
    private float scaleX, scaleY;
    private final float ZOOM_FACTOR = 1.25f;
    private String [][] matrix;
    private boolean DEBUG = false;
    private JComboBox comboBox;
    private XmlJDOM xmlJDOM;
    private float counterZoom=0;
    private Color defaultColor = new Color(212, 208, 200);
    private boolean imageOpen = false;
    private OCRTask task;
    private Container c;
    private TableRowSorter<TableModel> sorter;
    private int progress = 0;
    private int scaleProgressbar = 0;
    private ImageIcon ii;

    
    public EnaOCRView(SingleFrameApplication app) {
        super(app);
        File baseDir = Utilities.getBaseDir(this);
        if (WINDOWS) {
            tessPath = new File(baseDir, "tesseract").getPath();
        } else {
            tessPath = prefs.get("TesseractDirectory", new File(baseDir, "tesseract").getPath());
        }
        inputPath = prefs.get("InputFileDirectory", new File("inputDirectory").getPath());

        prop = new Properties();

        try {
            File tessdataDir = new File(tessPath, "tessdata");
            if (!tessdataDir.exists()) {
                String TESSDATA_PREFIX = System.getenv("TESSDATA_PREFIX");
                if (TESSDATA_PREFIX == null && !WINDOWS) { // if TESSDATA_PREFIX env var not set
                    TESSDATA_PREFIX = "/usr/local/share/"; // default path of tessdata on Linux after make install
                    tessdataDir = new File(TESSDATA_PREFIX, "tessdata");
                    if (!tessdataDir.exists()) {
                        TESSDATA_PREFIX = "/usr/share/tesseract-ocr/"; // other possible path of tessdata
                        tessdataDir = new File(TESSDATA_PREFIX, "tessdata");
                    }
                } else {
                    tessdataDir = new File(TESSDATA_PREFIX, "tessdata");
                }
            }

            langCodes = tessdataDir.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".inttemp");
                }
            });

            File xmlFile = new File(baseDir, "data/ISO639-3.xml");
            prop.loadFromXML(new FileInputStream(xmlFile));
        } catch (IOException ioe) {
            JOptionPane.showMessageDialog(null, "Missing ISO639-3.xml file. Cannot find it in " + new File(baseDir, "data").getPath() + " directory.", APP_NAME, JOptionPane.ERROR_MESSAGE);
            ioe.printStackTrace();
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            if (langCodes == null) {
                langs = new String[0];
            } else {
                langs = new String[langCodes.length];
            }
            for (int i = 0; i < langs.length; i++) {
                langCodes[i] = langCodes[i].replace(".inttemp", "");
                langs[i] = prop.getProperty(langCodes[i], langCodes[i]);
            }
        }

        selectedInputMethod = prefs.get("inputMethod", "Telex");

        try {
            UIManager.setLookAndFeel(prefs.get("lookAndFeel", UIManager.getSystemLookAndFeelClassName()));
        } catch (Exception e) {
            e.printStackTrace();
            // keep default LAF
        }
        
        initComponents();

        new DropTarget(this.jImageLabel, new ImageDropTargetListener(this));

        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
	messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
	messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        }); 
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });

        // tracking table selection
        masterTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    firePropertyChange("recordSelected", !isRecordSelected(), isRecordSelected());
                }
            });
        detailTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    firePropertyChange("detailRecordSelected", !isDetailRecordSelected(), isDetailRecordSelected());
                }
            }); 

        // tracking changes to save
        bindingGroup.addBindingListener(new AbstractBindingListener() {
            @Override
            public void targetChanged(Binding binding, PropertyStateEvent event) {
                // save action observes saveNeeded property
                setSaveNeeded(true);
            }
        });

        // have a transaction started
        entityManager.getTransaction().begin();
  

        currentDirectory = prefs.get("currentDirectory", null);
//        currentDirectory = prefs.get("currentDirectory", System.getProperty("user.home"));
        fileChooser = new JFileChooser(currentDirectory);
        fileChooser.setDialogTitle("Abrir uma Imagem");

        chooser = new JFileChooser(inputFilesDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(inputPath));
        chooser.setAcceptAllFileFilterUsed(true);
        chooser.setApproveButtonText("Ok");
        chooser.setDialogTitle("Localize o Directório de leitura");

        String filename = File.separator+"xml";
        fc = new JFileChooser(new File(filename));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(true);
        fc.setApproveButtonText("Ok");
        fc.setDialogTitle("Gravar Resultados");
//        fc.addChoosableFileFilter(new MyFilterXML());
//        fc.setAcceptAllFileFilterUsed(false);

        
        javax.swing.filechooser.FileFilter tiffFilter = new SimpleFilter("tif;tiff", "TIFF");
        javax.swing.filechooser.FileFilter jpegFilter = new SimpleFilter("jpg;jpeg", "JPEG");
        javax.swing.filechooser.FileFilter gifFilter = new SimpleFilter("gif", "GIF");
        javax.swing.filechooser.FileFilter pngFilter = new SimpleFilter("png", "PNG");
        javax.swing.filechooser.FileFilter bmpFilter = new SimpleFilter("bmp", "Bitmap");
        javax.swing.filechooser.FileFilter allImageFilter = new SimpleFilter("tif;tiff;jpg;jpeg;gif;png;bmp", "All Image Files");


        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.addChoosableFileFilter(allImageFilter);
        fileChooser.addChoosableFileFilter(tiffFilter);
        fileChooser.addChoosableFileFilter(jpegFilter);
        fileChooser.addChoosableFileFilter(gifFilter);
        fileChooser.addChoosableFileFilter(pngFilter);
        fileChooser.addChoosableFileFilter(bmpFilter);


        fileChooser.setFileFilter(fileChooser.getChoosableFileFilters()[prefs.getInt("filterIndex", 0)]);
//        fc.setFileFilter(fc.getChoosableFileFilters()[prefs.getInt("filterIndex", 0)]);

        filterIndex = prefs.getInt("filterIndex", 0);
        fileFilters = fileChooser.getChoosableFileFilters();
//        outputFilters = fc.getChoosableFileFilters();
//        fc.setFileFilter(outputFilters[filterIndex]);

        font = new Font(
                prefs.get("fontName", MAC_OS_X ? "Lucida Grande" : "Tahoma"),
                prefs.getInt("fontStyle", Font.PLAIN),
                prefs.getInt("fontSize", 12));
//        jTextArea1.setFont(font);

        this.jScrollPane1.setSize(
                snap(prefs.getInt("frameWidth", 500), 300, screen.width),
                snap(prefs.getInt("frameHeight", 360), 150, screen.height));
        this.jScrollPane1.setLocation(
                snap(
                prefs.getInt("frameX", (screen.width - this.jScrollPane1.getWidth()) / 2),
                screen.x, screen.x + screen.width - this.jScrollPane1.getWidth()),
                snap(
                prefs.getInt("frameY", screen.y + (screen.height - this.jScrollPane1.getHeight()) / 3),
                screen.y, screen.y + screen.height - this.jScrollPane1.getHeight()));
        if (langCodes == null) {
            JOptionPane.showMessageDialog(frame, "Tesseract não encontrado. Por favor especifique o destino no menu opções.", APP_NAME, JOptionPane.INFORMATION_MESSAGE);
        }
        
        this.jButtonActualSize.setEnabled(false);
        this.jButtonFitImage.setEnabled(false);
        this.jButtonZoomIn.setEnabled(false);
        this.jButtonZoomOut.setEnabled(false);

    }

    public class MyFilterXML extends javax.swing.filechooser.FileFilter {
        public boolean accept(File file) {
            String filename = file.getName();
            return filename.endsWith(".xml");
        }
        public String getDescription() {
            return "*.xml";
        }
    }
    public class MyFilterTXT extends javax.swing.filechooser.FileFilter {
        public boolean accept(File file) {
            String filename = file.getName();
            return filename.endsWith(".txt");
        }
        public String getDescription() {
            return "*.txt";
        }
    }

    public boolean isSaveNeeded() {
        return saveNeeded;
    }

    private void setSaveNeeded(boolean saveNeeded) {
        if (saveNeeded != this.saveNeeded) {
            this.saveNeeded = saveNeeded;
            firePropertyChange("saveNeeded", !saveNeeded, saveNeeded);
        }
    }

    public boolean isRecordSelected() {
        return masterTable.getSelectedRow() != -1;
    }
    
    public boolean isDetailRecordSelected() {
        return detailTable.getSelectedRow() != -1;
    }

    @Action
    public void newRecord() {
        dataBase.Zonetype Z = new dataBase.Zonetype();
        entityManager.persist(Z);
        list.add(Z);
        int row = list.size()-1;
        masterTable.setRowSelectionInterval(row, row);
        masterTable.scrollRectToVisible(masterTable.getCellRect(row, 0, true));
        setSaveNeeded(true);

    }

    @Action(enabledProperty = "recordSelected")
    public void deleteRecord() {
        int[] selected = masterTable.getSelectedRows();
        List<dataBase.Zonetype> toRemove = new ArrayList<dataBase.Zonetype>(selected.length);
        for (int idx=0; idx<selected.length; idx++) {
            dataBase.Zonetype Z = list.get(masterTable.convertRowIndexToModel(selected[idx]));
            toRemove.add(Z);
            entityManager.remove(Z);
        }
        list.removeAll(toRemove);
        setSaveNeeded(true);
    }
    
    @Action(enabledProperty = "recordSelected")
    public void newDetailRecord() {

        int index = masterTable.getSelectedRow();
        dataBase.Zonetype Z = list.get(masterTable.convertRowIndexToModel(index));
        Collection<dataBase.ZoneDefinitions> zs = Z.getZoneDefinitionsList();
        if (zs == null) {
            zs = new LinkedList<dataBase.ZoneDefinitions>();
            Z.setZoneDefinitionsList((List)zs);
        }
        dataBase.ZoneDefinitions z = new dataBase.ZoneDefinitions();
        entityManager.persist(z);
        z.setZoneDefinitionsId(Z);
        zs.add(z);
        masterTable.clearSelection();
        masterTable.setRowSelectionInterval(index, index);
        int row = zs.size()-1;
        detailTable.setRowSelectionInterval(row, row);
        detailTable.scrollRectToVisible(detailTable.getCellRect(row, 0, true));
        setSaveNeeded(true);
        newDetailButton.setEnabled(false);
    }

    @Action(enabledProperty = "detailRecordSelected")
    public void deleteDetailRecord() {
        int index = masterTable.getSelectedRow();
        dataBase.Zonetype Z = list.get(masterTable.convertRowIndexToModel(index));
        Collection<dataBase.ZoneDefinitions> zs = Z.getZoneDefinitionsList();
        int[] selected = detailTable.getSelectedRows();
        List<dataBase.ZoneDefinitions> toRemove = new ArrayList<dataBase.ZoneDefinitions>(selected.length);
        for (int idx=0; idx<selected.length; idx++) {
            selected[idx] = detailTable.convertRowIndexToModel(selected[idx]);
            int count = 0;
            Iterator<dataBase.ZoneDefinitions> iter = zs.iterator();
            while (count++ < selected[idx]) iter.next();
            dataBase.ZoneDefinitions z = iter.next();
            toRemove.add(z);
            entityManager.remove(z);
        }
        zs.removeAll(toRemove);
        masterTable.clearSelection();
        masterTable.setRowSelectionInterval(index, index);
        setSaveNeeded(true);
    }

    @Action(enabledProperty = "saveNeeded")
    public Task save() {
        
        saveButton.setBackground(defaultColor);
        newDetailButton.setEnabled(true);
        return new SaveTask(getApplication());
    }

    private class SaveTask extends Task {
        SaveTask(org.jdesktop.application.Application app) {
            super(app);
        }
        @Override protected Void doInBackground() {
            try {
                entityManager.getTransaction().commit();
                entityManager.getTransaction().begin();
            } catch (RollbackException rex) {
                rex.printStackTrace();
                entityManager.getTransaction().begin();
                List<dataBase.Zonetype> merged = new ArrayList<dataBase.Zonetype>(list.size());
                for (dataBase.Zonetype Z : list) {
                    merged.add(entityManager.merge(Z));
                }
                list.clear();
                list.addAll(merged);
            }
            return null;
        }
        @Override protected void finished() {
            setSaveNeeded(false);
        }
    }

    private class OCRTask extends SwingWorker<Void, Void> {
        /*
         * Main task. Executed in background thread.
         */

        @Override
        public Void doInBackground() {
            Random random = new Random();
            int progress = 0;
            //Initialize progress property.
            setProgress(0);
            while (progress < 100) {
                //Sleep for up to one second.
                try {
                    Thread.sleep(random.nextInt(1000));
                } catch (InterruptedException ignore) {}
                //Make random progress.
                progress += random.nextInt(10);
                setProgress(Math.min(progress, 100));
            }
            return null;
        }

        /*
         * Executed in event dispatching thread
         */
        @Override
        public void done() {
            Toolkit.getDefaultToolkit().beep();
//            setCursor(true); //turn on the wait cursor
            System.out.println("Progress bar DONE()");
        }

    }

    /**
     * An example action method showing how to create asynchronous tasks
     * (running on background) and how to show their progress. Note the
     * artificial 'Thread.sleep' calls making the task long enough to see the
     * progress visualization - remove the sleeps for real application.
     */
    @Action
    public Task refresh() {
       return new RefreshTask(getApplication());
    }

    private class RefreshTask extends Task {
        RefreshTask(org.jdesktop.application.Application app) {
            super(app);
        }
        @SuppressWarnings("unchecked")
        @Override protected Void doInBackground() {
            try {
                setProgress(0, 0, 4);
                setMessage("A Rever as actuais alterações ...");
                setProgress(1, 0, 4);
                entityManager.getTransaction().rollback();
                Thread.sleep(1000L); // remove for real app
                setProgress(2, 0, 4);

                setMessage("A Iniciar uma nova operação ...");
                entityManager.getTransaction().begin();
                Thread.sleep(500L); // remove for real app
                setProgress(3, 0, 4);

                setMessage("A procurar novos dados ...");
                java.util.Collection data = query.getResultList();
                for (Object entity : data) {
                    entityManager.refresh(entity);
                }
                Thread.sleep(1300L); // remove for real app
                setProgress(4, 0, 4);

                Thread.sleep(150L); // remove for real app
                list.clear();
                list.addAll(data);
            } catch(InterruptedException ignore) { }
            return null;
        }
        @Override protected void finished() {
            setMessage("Fim.");
            setSaveNeeded(false);
        }
    }

    @Action
    public void showAboutBox() {
        if (aboutBox == null) {
            JFrame mainFrame = EnaOCRApp.getApplication().getMainFrame();
            aboutBox = new EnaOCRAboutBox(mainFrame);
            aboutBox.setLocationRelativeTo(mainFrame);
        }
        EnaOCRApp.getApplication().show(aboutBox);
    }

    @Action
    public void showXMLOptions() {
        if (xmlBox == null) {
            JFrame mainFrame = EnaOCRApp.getApplication().getMainFrame();
            xmlBox = new EnaOCRAboutBox(mainFrame);
            xmlBox.setLocationRelativeTo(mainFrame);
        }
        EnaOCRApp.getApplication().show(xmlBox);
    }

    public void openFile(File selectedFile) {

        iioImageList = ImageIOHelper.getIIOImageList(selectedFile);
        imageList = ImageIOHelper.getImageList(iioImageList);
        
        if (imageList == null) {
            JOptionPane.showMessageDialog(null, "Não é possivel abrir a Imagem", APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }

        imageTotal = imageList.size();
        imageIndex = 0;

        displayImage();

        originalW = imageIcon.getIconWidth();
        originalH = imageIcon.getIconHeight();

        this.frame.setTitle(selectedFile.getName() + " - " + APP_NAME);
         statusMessageLabel.setText(null);
        progressBar.setVisible(false);

        this.jButtonActualSize.setEnabled(true);
        this.jButtonFitImage.setEnabled(true);
        this.jButtonZoomIn.setEnabled(true);
        this.jButtonZoomOut.setEnabled(true);

    }

    void displayImage() {
//        if (imageList != null) {
//            this.jLabelCurIndex.setText("Page " + (imageIndex + 1) + " of " + imageTotal);   
            imageIcon = imageList.get(imageIndex);
            jImageLabel.setIcon(imageIcon);
            jImageLabel.revalidate();
//        }
    }

    void doChange(final boolean isZoomIn) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (ImageIconScalable tempImageIcon : imageList) {
                    int width = tempImageIcon.getIconWidth();
                    int height = tempImageIcon.getIconHeight();
                    int widthRectangle = (int)((JImageLabel) jImageLabel).getRect().getWidth();
                    int heightRectangle = (int)((JImageLabel) jImageLabel).getRect().getHeight();

                    if (isZoomIn) {
                        tempImageIcon.setScaledSize((int) (width * ZOOM_FACTOR), (int) (height * ZOOM_FACTOR));

                        //get the X and Y position of the rectangle
                        int x = (int) (((JImageLabel) jImageLabel).getRect().getX() * ZOOM_FACTOR);
                        int y = (int) (((JImageLabel) jImageLabel).getRect().getY() * ZOOM_FACTOR);

                        ((JImageLabel) jImageLabel).getRect().setSize(
                                (int) (widthRectangle * ZOOM_FACTOR),
                                (int) (heightRectangle * ZOOM_FACTOR));
                        ((JImageLabel) jImageLabel).getRect().setLocation(x, y);


                    } else {
                        tempImageIcon.setScaledSize((int) (width / ZOOM_FACTOR), (int) (height / ZOOM_FACTOR));

                        //get the X and Y position of the rectangle
                        int x = (int) (((JImageLabel) jImageLabel).getRect().getX() / ZOOM_FACTOR);
                        int y = (int) (((JImageLabel) jImageLabel).getRect().getY() / ZOOM_FACTOR);

                        ((JImageLabel) jImageLabel).getRect().setSize(
                                (int) (widthRectangle / ZOOM_FACTOR),
                                (int) (heightRectangle / ZOOM_FACTOR));
                        ((JImageLabel) jImageLabel).getRect().setLocation(x, y);

                    }
                }
                imageIcon = imageList.get(imageIndex);
                jImageLabel.revalidate();
                jScrollPane1.repaint();

                if (isZoomIn) {
                    scaleX /= ZOOM_FACTOR;
                    scaleY /= ZOOM_FACTOR;
                } else {
                    scaleX *= ZOOM_FACTOR;
                    scaleY *= ZOOM_FACTOR;
                }
            }
        });
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        mainPanel = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelInicio = new javax.swing.JPanel();
        jLabelTitle = new javax.swing.JLabel();
        jLabelSubTitle = new javax.swing.JLabel();
        jLabelDefinirZonas = new javax.swing.JLabel();
        jLabelProcessarFicheiros = new javax.swing.JLabel();
        jLabelExportarResultados = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTextPane1 = new javax.swing.JTextPane();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTextPane2 = new javax.swing.JTextPane();
        jScrollPane6 = new javax.swing.JScrollPane();
        jTextPane3 = new javax.swing.JTextPane();
        jLabel8 = new javax.swing.JLabel();
        jPanelDefinirZonas = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jPanelZone = new javax.swing.JPanel();
        masterScrollPane = new javax.swing.JScrollPane();
        masterTable = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldZoneTypeName = new javax.swing.JTextField();
        newButton = new javax.swing.JButton();
        deleteButton = new javax.swing.JButton();
        detailScrollPane = new javax.swing.JScrollPane();
        detailTable = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldZoneName = new javax.swing.JTextField();
        newDetailButton = new javax.swing.JButton();
        deleteDetailButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        jTextFieldX = new javax.swing.JTextField();
        jTextFieldY = new javax.swing.JTextField();
        jTextFieldWidth = new javax.swing.JTextField();
        jTextFieldHeight = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jPanelTestImage = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jToolBar2 = new javax.swing.JToolBar();
        jButtonOpen = new javax.swing.JButton();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        jButtonFitImage = new javax.swing.JButton();
        jButtonActualSize = new javax.swing.JButton();
        jSeparator6 = new javax.swing.JToolBar.Separator();
        jButtonZoomIn = new javax.swing.JButton();
        jButtonZoomOut = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JToolBar.Separator();
        jScrollPane1 = new javax.swing.JScrollPane();
        jImageLabel = new JImageLabel();
        jPanelProcessarFicheiros = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        jComboBoxSelectedZone = new javax.swing.JComboBox();
        jButtonInputFolder = new javax.swing.JButton();
        jTextFieldInputFolder = new javax.swing.JTextField();
        colorTextField(false);
        jButtonProcessFiles = new javax.swing.JButton();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel5 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableFileToProcess = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTableResults = new javax.swing.JTable();
        jPanelExportar = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        jLabel9 = new javax.swing.JLabel();
        jRadioButtonExportText = new javax.swing.JRadioButton();
        jTextFieldCustomText = new javax.swing.JTextField();
        jRadioButtonExportXML = new javax.swing.JRadioButton();
        jButtonRunExporter = new javax.swing.JButton();
        jButtonSaveExporter = new javax.swing.JButton();
        jSplitPane3 = new javax.swing.JSplitPane();
        jPanelTextArea = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jPanelInformation = new javax.swing.JPanel();
        jToolBar4 = new javax.swing.JToolBar();
        jComboBoxChangeTypeDoc = new javax.swing.JComboBox();
        jToolBar5 = new javax.swing.JToolBar();
        jButtonChangeXML = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTableTypeOfDocument = new javax.swing.JTable();
        menuBar = new javax.swing.JMenuBar();
        javax.swing.JMenu fileMenu = new javax.swing.JMenu();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();
        javax.swing.JMenuItem exitMenuItem = new javax.swing.JMenuItem();
        jMenuDefinitions = new javax.swing.JMenu();
        jMenuItemTessPath = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMenuProcessLanguage = new javax.swing.JMenu();
        ButtonGroup group = new ButtonGroup();
        jRadioButtonMenuItemPt = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemEn = new javax.swing.JRadioButtonMenuItem();
        javax.swing.JMenu helpMenu = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        javax.swing.JMenuItem aboutMenuItem = new javax.swing.JMenuItem();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar(0,100);
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(enaocr.EnaOCRApp.class).getContext().getResourceMap(EnaOCRView.class);
        entityManager = java.beans.Beans.isDesignTime() ? null : javax.persistence.Persistence.createEntityManagerFactory(resourceMap.getString("entityManager.persistenceUnit")).createEntityManager(); // NOI18N
        query = java.beans.Beans.isDesignTime() ? null : entityManager.createQuery(resourceMap.getString("query.query")); // NOI18N
        list = java.beans.Beans.isDesignTime() ? java.util.Collections.emptyList() : org.jdesktop.observablecollections.ObservableCollections.observableList(query.getResultList());
        buttonGroup1 = new javax.swing.ButtonGroup();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        jTabbedPane1.setToolTipText(resourceMap.getString("jTabbedPane1.toolTipText")); // NOI18N
        jTabbedPane1.setName("jTabbedPane1"); // NOI18N

        jPanelInicio.setBackground(resourceMap.getColor("jPanelInicio.background")); // NOI18N
        jPanelInicio.setName("jPanelInicio"); // NOI18N

        jLabelTitle.setFont(resourceMap.getFont("jLabelTitle.font")); // NOI18N
        jLabelTitle.setForeground(resourceMap.getColor("jLabelTitle.foreground")); // NOI18N
        jLabelTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelTitle.setText(resourceMap.getString("jLabelTitle.text")); // NOI18N
        jLabelTitle.setName("jLabelTitle"); // NOI18N

        jLabelSubTitle.setBackground(resourceMap.getColor("jLabelSubTitle.background")); // NOI18N
        jLabelSubTitle.setFont(resourceMap.getFont("jLabelSubTitle.font")); // NOI18N
        jLabelSubTitle.setForeground(resourceMap.getColor("jLabelSubTitle.foreground")); // NOI18N
        jLabelSubTitle.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabelSubTitle.setText(resourceMap.getString("jLabelSubTitle.text")); // NOI18N
        jLabelSubTitle.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabelSubTitle.setName("jLabelSubTitle"); // NOI18N
        jLabelSubTitle.setOpaque(true);

        jLabelDefinirZonas.setBackground(resourceMap.getColor("jLabelDefinirZonas.background")); // NOI18N
        jLabelDefinirZonas.setFont(resourceMap.getFont("jLabelDefinirZonas.font")); // NOI18N
        jLabelDefinirZonas.setForeground(resourceMap.getColor("jLabelDefinirZonas.foreground")); // NOI18N
        jLabelDefinirZonas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelDefinirZonas.setText(resourceMap.getString("jLabelDefinirZonas.text")); // NOI18N
        jLabelDefinirZonas.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabelDefinirZonas.setName("jLabelDefinirZonas"); // NOI18N
        jLabelDefinirZonas.setOpaque(true);

        jLabelProcessarFicheiros.setBackground(resourceMap.getColor("jLabelProcessarFicheiros.background")); // NOI18N
        jLabelProcessarFicheiros.setFont(resourceMap.getFont("jLabelProcessarFicheiros.font")); // NOI18N
        jLabelProcessarFicheiros.setForeground(resourceMap.getColor("jLabelProcessarFicheiros.foreground")); // NOI18N
        jLabelProcessarFicheiros.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelProcessarFicheiros.setText(resourceMap.getString("jLabelProcessarFicheiros.text")); // NOI18N
        jLabelProcessarFicheiros.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabelProcessarFicheiros.setName("jLabelProcessarFicheiros"); // NOI18N
        jLabelProcessarFicheiros.setOpaque(true);

        jLabelExportarResultados.setBackground(resourceMap.getColor("jLabelExportarResultados.background")); // NOI18N
        jLabelExportarResultados.setFont(resourceMap.getFont("jLabelExportarResultados.font")); // NOI18N
        jLabelExportarResultados.setForeground(resourceMap.getColor("jLabelExportarResultados.foreground")); // NOI18N
        jLabelExportarResultados.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelExportarResultados.setText(resourceMap.getString("jLabelExportarResultados.text")); // NOI18N
        jLabelExportarResultados.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabelExportarResultados.setName("jLabelExportarResultados"); // NOI18N
        jLabelExportarResultados.setOpaque(true);

        jButton1.setBackground(resourceMap.getColor("jButton1.background")); // NOI18N
        jButton1.setIcon(resourceMap.getIcon("jButton1.icon")); // NOI18N
        jButton1.setText(resourceMap.getString("jButton1.text")); // NOI18N
        jButton1.setMaximumSize(new java.awt.Dimension(100, 78));
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setPreferredSize(new java.awt.Dimension(100, 78));
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jButton2.setBackground(resourceMap.getColor("jButton2.background")); // NOI18N
        jButton2.setIcon(resourceMap.getIcon("jButton2.icon")); // NOI18N
        jButton2.setMaximumSize(new java.awt.Dimension(100, 78));
        jButton2.setName("jButton2"); // NOI18N
        jButton2.setPreferredSize(new java.awt.Dimension(100, 78));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setBackground(resourceMap.getColor("jButton3.background")); // NOI18N
        jButton3.setIcon(resourceMap.getIcon("jButton3.icon")); // NOI18N
        jButton3.setMaximumSize(new java.awt.Dimension(100, 78));
        jButton3.setName("jButton3"); // NOI18N
        jButton3.setPreferredSize(new java.awt.Dimension(100, 78));
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jScrollPane4.setName("jScrollPane4"); // NOI18N

        jTextPane1.setBackground(resourceMap.getColor("jTextPane1.background")); // NOI18N
        jTextPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextPane1.setEditable(false);
        jTextPane1.setFont(resourceMap.getFont("jTextPane1.font")); // NOI18N
        jTextPane1.setText(resourceMap.getString("jTextPane1.text")); // NOI18N
        jTextPane1.setCaretColor(resourceMap.getColor("jTextPane1.caretColor")); // NOI18N
        jTextPane1.setMargin(new java.awt.Insets(10, 10, 10, 10));
        jTextPane1.setName("jTextPane1"); // NOI18N
        jTextPane1.setSelectionColor(resourceMap.getColor("jTextPane1.selectionColor")); // NOI18N
        jScrollPane4.setViewportView(jTextPane1);

        jScrollPane5.setName("jScrollPane5"); // NOI18N

        jTextPane2.setBackground(resourceMap.getColor("jTextPane2.background")); // NOI18N
        jTextPane2.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jTextPane2.setEditable(false);
        jTextPane2.setFont(resourceMap.getFont("jTextPane2.font")); // NOI18N
        jTextPane2.setText(resourceMap.getString("jTextPane2.text")); // NOI18N
        jTextPane2.setName("jTextPane2"); // NOI18N
        jTextPane2.setSelectionColor(resourceMap.getColor("jTextPane2.selectionColor")); // NOI18N
        jScrollPane5.setViewportView(jTextPane2);

        jScrollPane6.setBackground(resourceMap.getColor("jScrollPane6.background")); // NOI18N
        jScrollPane6.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPane6.setName("jScrollPane6"); // NOI18N
        jScrollPane6.setOpaque(false);

        jTextPane3.setBackground(resourceMap.getColor("jTextPane3.background")); // NOI18N
        jTextPane3.setEditable(false);
        jTextPane3.setFont(resourceMap.getFont("jTextPane3.font")); // NOI18N
        jTextPane3.setText(resourceMap.getString("jTextPane3.text")); // NOI18N
        jTextPane3.setName("jTextPane3"); // NOI18N
        jTextPane3.setSelectionColor(resourceMap.getColor("jTextPane3.selectionColor")); // NOI18N
        jScrollPane6.setViewportView(jTextPane3);

        jLabel8.setIcon(resourceMap.getIcon("jLabel8.icon")); // NOI18N
        jLabel8.setText(resourceMap.getString("jLabel8.text")); // NOI18N
        jLabel8.setToolTipText(resourceMap.getString("jLabel8.toolTipText")); // NOI18N
        jLabel8.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel8.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel8.setName("jLabel8"); // NOI18N
        jLabel8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel8MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanelInicioLayout = new javax.swing.GroupLayout(jPanelInicio);
        jPanelInicio.setLayout(jPanelInicioLayout);
        jPanelInicioLayout.setHorizontalGroup(
            jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelInicioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabelSubTitle, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 587, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelInicioLayout.createSequentialGroup()
                        .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addComponent(jLabelExportarResultados, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addComponent(jLabelProcessarFicheiros, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)
                            .addComponent(jLabelDefinirZonas, javax.swing.GroupLayout.DEFAULT_SIZE, 442, Short.MAX_VALUE)))
                    .addGroup(jPanelInicioLayout.createSequentialGroup()
                        .addComponent(jLabelTitle, javax.swing.GroupLayout.DEFAULT_SIZE, 539, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanelInicioLayout.setVerticalGroup(
            jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelInicioLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelSubTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                    .addGroup(jPanelInicioLayout.createSequentialGroup()
                        .addComponent(jLabelDefinirZonas)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                    .addGroup(jPanelInicioLayout.createSequentialGroup()
                        .addComponent(jLabelProcessarFicheiros, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelInicioLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelInicioLayout.createSequentialGroup()
                        .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, 109, Short.MAX_VALUE)
                        .addGap(10, 10, 10))
                    .addGroup(jPanelInicioLayout.createSequentialGroup()
                        .addComponent(jLabelExportarResultados)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.DEFAULT_SIZE, 87, Short.MAX_VALUE)
                        .addContainerGap())))
        );

        jTabbedPane1.addTab(resourceMap.getString("jPanelInicio.TabConstraints.tabTitle"), jPanelInicio); // NOI18N

        jPanelDefinirZonas.setName("jPanelDefinirZonas"); // NOI18N
        jPanelDefinirZonas.setLayout(new java.awt.BorderLayout());

        jSplitPane1.setDividerSize(9);
        jSplitPane1.setToolTipText(resourceMap.getString("jSplitPane1.toolTipText")); // NOI18N
        jSplitPane1.setMaximumSize(new java.awt.Dimension(300, 300));
        jSplitPane1.setName("jSplitPane1"); // NOI18N
        jSplitPane1.setOneTouchExpandable(true);

        jPanel1.setMaximumSize(new java.awt.Dimension(4000, 4000));
        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(50, 50));

        jPanelZone.setEnabled(false);
        jPanelZone.setName("jPanelZone"); // NOI18N

        masterScrollPane.setEnabled(false);
        masterScrollPane.setName("masterScrollPane"); // NOI18N

        masterTable.setAutoCreateRowSorter(true);
        masterTable.setColumnSelectionAllowed(true);
        masterTable.setEnabled(false);
        masterTable.setName("masterTable"); // NOI18N
        masterTable.getTableHeader().setReorderingAllowed(false);

        org.jdesktop.swingbinding.JTableBinding jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, list, masterTable);
        org.jdesktop.swingbinding.JTableBinding.ColumnBinding columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${zoneTypeName}"));
        columnBinding.setColumnName("Zone Type Name");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();
        counterJComboBoxZones = list.size();
        masterScrollPane.setViewportView(masterTable);
        masterTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        masterTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("masterTable.columnModel.title0")); // NOI18N

        jLabel1.setText(resourceMap.getString("jLabel1.text")); // NOI18N
        jLabel1.setName("jLabel1"); // NOI18N

        jTextFieldZoneTypeName.setEnabled(false);
        jTextFieldZoneTypeName.setMinimumSize(new java.awt.Dimension(16, 30));
        jTextFieldZoneTypeName.setName("jTextFieldZoneTypeName"); // NOI18N
        jTextFieldZoneTypeName.setPreferredSize(new java.awt.Dimension(8, 20));

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.zoneTypeName}"), jTextFieldZoneTypeName, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(enaocr.EnaOCRApp.class).getContext().getActionMap(EnaOCRView.class, this);
        newButton.setAction(actionMap.get("newRecord")); // NOI18N
        newButton.setIcon(resourceMap.getIcon("newButton.icon")); // NOI18N
        newButton.setText(resourceMap.getString("newButton.text")); // NOI18N
        newButton.setToolTipText(resourceMap.getString("newButton.toolTipText")); // NOI18N
        newButton.setEnabled(false);
        newButton.setName("newButton"); // NOI18N
        newButton.setPreferredSize(new java.awt.Dimension(81, 23));
        newButton.setEnabled(false);

        deleteButton.setAction(actionMap.get("deleteRecord")); // NOI18N
        deleteButton.setIcon(resourceMap.getIcon("deleteButton.icon")); // NOI18N
        deleteButton.setText(resourceMap.getString("deleteButton.text")); // NOI18N
        deleteButton.setToolTipText(resourceMap.getString("deleteButton.toolTipText")); // NOI18N
        deleteButton.setEnabled(false);
        deleteButton.setName("deleteButton"); // NOI18N
        deleteButton.setPreferredSize(new java.awt.Dimension(81, 23));

        detailScrollPane.setEnabled(false);
        detailScrollPane.setName("detailScrollPane"); // NOI18N

        detailTable.setColumnSelectionAllowed(true);
        detailTable.setEnabled(false);
        detailTable.setName("detailTable"); // NOI18N
        detailTable.getTableHeader().setReorderingAllowed(false);

        org.jdesktop.beansbinding.ELProperty eLProperty = org.jdesktop.beansbinding.ELProperty.create("${selectedElement.zoneDefinitionsList}");
        jTableBinding = org.jdesktop.swingbinding.SwingBindings.createJTableBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, masterTable, eLProperty, detailTable);
        columnBinding = jTableBinding.addColumnBinding(org.jdesktop.beansbinding.ELProperty.create("${zoneDefName}"));
        columnBinding.setColumnName("Zone Def Name");
        columnBinding.setColumnClass(String.class);
        columnBinding.setEditable(false);
        jTableBinding.setSourceUnreadableValue(java.util.Collections.emptyList());
        bindingGroup.addBinding(jTableBinding);
        jTableBinding.bind();
        detailTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                detailTableMouseClicked(evt);
            }
        });
        detailScrollPane.setViewportView(detailTable);
        detailTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        detailTable.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("detailTable.columnModel.title0")); // NOI18N

        jLabel2.setText(resourceMap.getString("jLabel2.text")); // NOI18N
        jLabel2.setName("jLabel2"); // NOI18N

        jTextFieldZoneName.setEnabled(false);
        jTextFieldZoneName.setMinimumSize(new java.awt.Dimension(16, 30));
        jTextFieldZoneName.setName("jTextFieldZoneName"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.zoneDefName}"), jTextFieldZoneName, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        newDetailButton.setAction(actionMap.get("newDetailRecord")); // NOI18N
        newDetailButton.setIcon(resourceMap.getIcon("newDetailButton.icon")); // NOI18N
        newDetailButton.setText(resourceMap.getString("newDetailButton.text")); // NOI18N
        newDetailButton.setToolTipText(resourceMap.getString("newDetailButton.toolTipText")); // NOI18N
        newDetailButton.setEnabled(false);
        newDetailButton.setName("newDetailButton"); // NOI18N
        newDetailButton.setPreferredSize(new java.awt.Dimension(81, 23));

        deleteDetailButton.setAction(actionMap.get("deleteDetailRecord")); // NOI18N
        deleteDetailButton.setIcon(resourceMap.getIcon("deleteDetailButton.icon")); // NOI18N
        deleteDetailButton.setText(resourceMap.getString("deleteDetailButton.text")); // NOI18N
        deleteDetailButton.setToolTipText(resourceMap.getString("deleteDetailButton.toolTipText")); // NOI18N
        deleteDetailButton.setEnabled(false);
        deleteDetailButton.setName("deleteDetailButton"); // NOI18N
        deleteDetailButton.setPreferredSize(new java.awt.Dimension(81, 23));

        saveButton.setAction(actionMap.get("save")); // NOI18N
        saveButton.setIcon(resourceMap.getIcon("saveButton.icon")); // NOI18N
        saveButton.setText(resourceMap.getString("saveButton.text")); // NOI18N
        saveButton.setToolTipText(resourceMap.getString("saveButton.toolTipText")); // NOI18N
        saveButton.setName("saveButton"); // NOI18N
        saveButton.setPreferredSize(new java.awt.Dimension(81, 23));

        jTextFieldX.setEnabled(false);
        jTextFieldX.setName("jTextFieldX"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.upperleftx}"), jTextFieldX, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jTextFieldY.setEnabled(false);
        jTextFieldY.setName("jTextFieldY"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.upperlefty}"), jTextFieldY, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jTextFieldWidth.setEnabled(false);
        jTextFieldWidth.setName("jTextFieldWidth"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.zoneWith}"), jTextFieldWidth, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jTextFieldHeight.setEnabled(false);
        jTextFieldHeight.setName("jTextFieldHeight"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, detailTable, org.jdesktop.beansbinding.ELProperty.create("${selectedElement.zoneHeight}"), jTextFieldHeight, org.jdesktop.beansbinding.BeanProperty.create("text"));
        bindingGroup.addBinding(binding);

        jLabel4.setText(resourceMap.getString("jLabel4.text")); // NOI18N
        jLabel4.setName("jLabel4"); // NOI18N

        jLabel5.setText(resourceMap.getString("jLabel5.text")); // NOI18N
        jLabel5.setName("jLabel5"); // NOI18N

        javax.swing.GroupLayout jPanelZoneLayout = new javax.swing.GroupLayout(jPanelZone);
        jPanelZone.setLayout(jPanelZoneLayout);
        jPanelZoneLayout.setHorizontalGroup(
            jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelZoneLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanelZoneLayout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldZoneName, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelZoneLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldZoneTypeName, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(10, 10, 10)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(newButton, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE)
                    .addComponent(newDetailButton, javax.swing.GroupLayout.DEFAULT_SIZE, 99, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deleteDetailButton, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)
                    .addComponent(deleteButton, javax.swing.GroupLayout.DEFAULT_SIZE, 97, Short.MAX_VALUE)))
            .addComponent(detailScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
            .addGroup(jPanelZoneLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelZoneLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED))
                    .addGroup(jPanelZoneLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextFieldWidth, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldX, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 47, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldY, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
            .addComponent(masterScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 332, Short.MAX_VALUE)
        );

        jPanelZoneLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTextFieldHeight, jTextFieldWidth, jTextFieldX, jTextFieldY});

        jPanelZoneLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jTextFieldZoneName, jTextFieldZoneTypeName});

        jPanelZoneLayout.setVerticalGroup(
            jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelZoneLayout.createSequentialGroup()
                .addComponent(masterScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldZoneTypeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(deleteButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(5, 5, 5)
                .addComponent(detailScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 125, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(newDetailButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deleteDetailButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldZoneName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanelZoneLayout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldX, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldY, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(4, 4, 4)
                        .addGroup(jPanelZoneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextFieldHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)))
                    .addGroup(jPanelZoneLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                        .addComponent(saveButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanelZoneLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteButton, newButton});

        jPanelZoneLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {deleteDetailButton, newDetailButton});

        jPanelZoneLayout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jTextFieldZoneName, jTextFieldZoneTypeName});

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanelZone, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanelZone, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanelTestImage.setName("jPanelTestImage"); // NOI18N
        jPanelTestImage.setLayout(new java.awt.BorderLayout());

        jLabel3.setFont(resourceMap.getFont("jLabel3.font")); // NOI18N
        jLabel3.setForeground(resourceMap.getColor("jLabel3.foreground")); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText(resourceMap.getString("jLabel3.text")); // NOI18N
        jLabel3.setName("jLabel3"); // NOI18N
        jPanelTestImage.add(jLabel3, java.awt.BorderLayout.PAGE_START);

        jToolBar2.setFloatable(false);
        jToolBar2.setOrientation(1);
        jToolBar2.setRollover(true);
        jToolBar2.setName("jToolBar2"); // NOI18N
        jToolBar2.setPreferredSize(new java.awt.Dimension(31, 51));

        jButtonOpen.setBackground(resourceMap.getColor("jButtonOpen.background")); // NOI18N
        jButtonOpen.setIcon(resourceMap.getIcon("jButtonOpen.icon")); // NOI18N
        jButtonOpen.setText(resourceMap.getString("jButtonOpen.text")); // NOI18N
        jButtonOpen.setToolTipText(resourceMap.getString("jButtonOpen.toolTipText")); // NOI18N
        jButtonOpen.setFocusable(false);
        jButtonOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonOpen.setName("jButtonOpen"); // NOI18N
        jButtonOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonOpenActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonOpen);

        jSeparator8.setName("jSeparator8"); // NOI18N
        jToolBar2.add(jSeparator8);

        jButtonFitImage.setAction(actionMap.get("jButtonFitWidthActionPerformed")); // NOI18N
        jButtonFitImage.setIcon(resourceMap.getIcon("jButtonFitImage.icon")); // NOI18N
        jButtonFitImage.setText(resourceMap.getString("jButtonFitImage.text")); // NOI18N
        jButtonFitImage.setToolTipText(resourceMap.getString("jButtonFitImage.toolTipText")); // NOI18N
        jButtonFitImage.setFocusable(false);
        jButtonFitImage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonFitImage.setName("jButtonFitImage"); // NOI18N
        jButtonFitImage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFitImageActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonFitImage);

        jButtonActualSize.setIcon(resourceMap.getIcon("jButtonActualSize.icon")); // NOI18N
        jButtonActualSize.setText(resourceMap.getString("jButtonActualSize.text")); // NOI18N
        jButtonActualSize.setFocusable(false);
        jButtonActualSize.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonActualSize.setName("jButtonActualSize"); // NOI18N
        jButtonActualSize.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonActualSize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonActualSizeActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonActualSize);

        jSeparator6.setName("jSeparator6"); // NOI18N
        jToolBar2.add(jSeparator6);

        jButtonZoomIn.setIcon(resourceMap.getIcon("jButtonZoomIn.icon")); // NOI18N
        jButtonZoomIn.setText(resourceMap.getString("jButtonZoomIn.text")); // NOI18N
        jButtonZoomIn.setToolTipText(resourceMap.getString("jButtonZoomIn.toolTipText")); // NOI18N
        jButtonZoomIn.setFocusable(false);
        jButtonZoomIn.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonZoomIn.setName("jButtonZoomIn"); // NOI18N
        jButtonZoomIn.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomInActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonZoomIn);

        jButtonZoomOut.setIcon(resourceMap.getIcon("jButtonZoomOut.icon")); // NOI18N
        jButtonZoomOut.setText(resourceMap.getString("jButtonZoomOut.text")); // NOI18N
        jButtonZoomOut.setToolTipText(resourceMap.getString("jButtonZoomOut.toolTipText")); // NOI18N
        jButtonZoomOut.setFocusable(false);
        jButtonZoomOut.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonZoomOut.setName("jButtonZoomOut"); // NOI18N
        jButtonZoomOut.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonZoomOutActionPerformed(evt);
            }
        });
        jToolBar2.add(jButtonZoomOut);

        jSeparator4.setName("jSeparator4"); // NOI18N
        jToolBar2.add(jSeparator4);

        jPanelTestImage.add(jToolBar2, java.awt.BorderLayout.LINE_START);

        jScrollPane1.setEnabled(false);
        jScrollPane1.setName("jScrollPane1"); // NOI18N

        jImageLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jImageLabel.setText(resourceMap.getString("jImageLabel.text")); // NOI18N
        jImageLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jImageLabel.setEnabled(false);
        jImageLabel.setName("jImageLabel"); // NOI18N
        jImageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jImageLabelMouseReleased(evt);
            }
        });
        jScrollPane1.setViewportView(jImageLabel);

        jPanelTestImage.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanelTestImage);

        jPanelDefinirZonas.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(resourceMap.getString("jPanelDefinirZonas.TabConstraints.tabTitle"), jPanelDefinirZonas); // NOI18N

        jPanelProcessarFicheiros.setName("jPanelProcessarFicheiros"); // NOI18N
        jPanelProcessarFicheiros.setLayout(new java.awt.BorderLayout());

        jToolBar1.setFloatable(false);
        jToolBar1.setName("jToolBar1"); // NOI18N

        jComboBoxSelectedZone.setRenderer (new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus){
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof dataBase.Zonetype) {dataBase.Zonetype mec = (dataBase.Zonetype) value; setText(mec.getZoneTypeName());}
                return this;}});
    jComboBoxSelectedZone.setName("jComboBoxSelectedZone"); // NOI18N
    jComboBoxSelectedZone.setPreferredSize(new java.awt.Dimension(140, 22));

    org.jdesktop.swingbinding.JComboBoxBinding jComboBoxBinding = org.jdesktop.swingbinding.SwingBindings.createJComboBoxBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, list, jComboBoxSelectedZone);
    jComboBoxBinding.setSourceNullValue(null);
    jComboBoxBinding.setSourceUnreadableValue(null);
    bindingGroup.addBinding(jComboBoxBinding);

    jComboBoxSelectedZone.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jComboBoxSelectedZoneActionPerformed(evt);
        }
    });
    jToolBar1.add(jComboBoxSelectedZone);

    jButtonInputFolder.setIcon(resourceMap.getIcon("jButtonInputFolder.icon")); // NOI18N
    jButtonInputFolder.setText(resourceMap.getString("jButtonInputFolder.text")); // NOI18N
    jButtonInputFolder.setToolTipText(resourceMap.getString("jButtonInputFolder.toolTipText")); // NOI18N
    jButtonInputFolder.setFocusable(false);
    jButtonInputFolder.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jButtonInputFolder.setMaximumSize(new java.awt.Dimension(140, 27));
    jButtonInputFolder.setName("jButtonInputFolder"); // NOI18N
    jButtonInputFolder.setPreferredSize(new java.awt.Dimension(115, 27));
    jButtonInputFolder.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButtonInputFolder.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonInputFolderActionPerformed(evt);
        }
    });
    jToolBar1.add(jButtonInputFolder);

    jTextFieldInputFolder.setEditable(false);
    jTextFieldInputFolder.setText(resourceMap.getString("jTextFieldInputFolder.text")); // NOI18N
    jTextFieldInputFolder.setName("jTextFieldInputFolder"); // NOI18N
    jToolBar1.add(jTextFieldInputFolder);

    jButtonProcessFiles.setIcon(resourceMap.getIcon("jButtonProcessFiles.icon")); // NOI18N
    jButtonProcessFiles.setText(resourceMap.getString("jButtonProcessFiles.text")); // NOI18N
    jButtonProcessFiles.setEnabled(false);
    jButtonProcessFiles.setFocusable(false);
    jButtonProcessFiles.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jButtonProcessFiles.setName("jButtonProcessFiles"); // NOI18N
    jButtonProcessFiles.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButtonProcessFiles.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonProcessFilesActionPerformed(evt);
        }
    });
    jToolBar1.add(jButtonProcessFiles);

    jPanelProcessarFicheiros.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

    jSplitPane2.setDividerLocation(250);
    jSplitPane2.setDividerSize(9);
    jSplitPane2.setToolTipText(resourceMap.getString("jSplitPane2.toolTipText")); // NOI18N
    jSplitPane2.setAutoscrolls(true);
    jSplitPane2.setMaximumSize(new java.awt.Dimension(300, 300));
    jSplitPane2.setName("jSplitPane2"); // NOI18N
    jSplitPane2.setOneTouchExpandable(true);
    jSplitPane2.setPreferredSize(new java.awt.Dimension(183, 70));

    jPanel5.setName("jPanel5"); // NOI18N
    jPanel5.setPreferredSize(new java.awt.Dimension(50, 50));
    jPanel5.setLayout(new java.awt.BorderLayout());

    jLabel6.setFont(resourceMap.getFont("jLabel6.font")); // NOI18N
    jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel6.setText(resourceMap.getString("jLabel6.text")); // NOI18N
    jLabel6.setName("jLabel6"); // NOI18N
    jPanel5.add(jLabel6, java.awt.BorderLayout.PAGE_START);

    jScrollPane2.setName("jScrollPane2"); // NOI18N

    jTableFileToProcess.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Ficheiro", "Tamanho"
        }
    ) {
        Class[] types = new Class [] {
            java.lang.String.class, java.lang.String.class
        };
        boolean[] canEdit = new boolean [] {
            false, false
        };

        public Class getColumnClass(int columnIndex) {
            return types [columnIndex];
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return canEdit [columnIndex];
        }
    });
    jTableFileToProcess.setName("jTableFileToProcess"); // NOI18N
    jTableFileToProcess.getTableHeader().setReorderingAllowed(false);
    jScrollPane2.setViewportView(jTableFileToProcess);
    jTableFileToProcess.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("jTableFileToProcess.columnModel.title0")); // NOI18N
    jTableFileToProcess.getColumnModel().getColumn(1).setHeaderValue(resourceMap.getString("jTableFileToProcess.columnModel.title1")); // NOI18N

    jPanel5.add(jScrollPane2, java.awt.BorderLayout.CENTER);

    jSplitPane2.setLeftComponent(jPanel5);

    jPanel6.setName("jPanel6"); // NOI18N
    jPanel6.setPreferredSize(new java.awt.Dimension(50, 50));
    jPanel6.setLayout(new java.awt.BorderLayout());

    jLabel7.setFont(resourceMap.getFont("jLabel7.font")); // NOI18N
    jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    jLabel7.setText(resourceMap.getString("jLabel7.text")); // NOI18N
    jLabel7.setName("jLabel7"); // NOI18N
    jPanel6.add(jLabel7, java.awt.BorderLayout.PAGE_START);

    jScrollPane3.setName("jScrollPane3"); // NOI18N

    jTableResults.setModel(new javax.swing.table.DefaultTableModel(
        new Object [][] {

        },
        new String [] {
            "Title 1", "Title 2"
        }
    ));
    jTableResults.setColumnSelectionAllowed(true);
    jTableResults.setName("jTableResults"); // NOI18N
    jTableResults.getTableHeader().setReorderingAllowed(false);
    jScrollPane3.setViewportView(jTableResults);
    jTableResults.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
    jTableResults.getColumnModel().getColumn(0).setHeaderValue(resourceMap.getString("jTableResults.columnModel.title0")); // NOI18N

    jPanel6.add(jScrollPane3, java.awt.BorderLayout.CENTER);

    jSplitPane2.setRightComponent(jPanel6);

    jPanelProcessarFicheiros.add(jSplitPane2, java.awt.BorderLayout.CENTER);

    jTabbedPane1.addTab(resourceMap.getString("jPanelProcessarFicheiros.TabConstraints.tabTitle"), jPanelProcessarFicheiros); // NOI18N

    jPanelExportar.setEnabled(false);
    jPanelExportar.setName("jPanelExportar"); // NOI18N
    jPanelExportar.setLayout(new java.awt.BorderLayout());

    jToolBar3.setFloatable(false);
    jToolBar3.setRollover(true);
    jToolBar3.setEnabled(false);
    jToolBar3.setName("jToolBar3"); // NOI18N
    jToolBar3.setPreferredSize(new java.awt.Dimension(63, 27));

    jLabel9.setFont(resourceMap.getFont("jLabel9.font")); // NOI18N
    jLabel9.setText(resourceMap.getString("jLabel9.text")); // NOI18N
    jLabel9.setName("jLabel9"); // NOI18N
    jToolBar3.add(jLabel9);

    buttonGroup1.add(jRadioButtonExportText);
    jRadioButtonExportText.setText(resourceMap.getString("jRadioButtonExportText.text")); // NOI18N
    jRadioButtonExportText.setEnabled(false);
    jRadioButtonExportText.setFocusable(false);
    jRadioButtonExportText.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jRadioButtonExportText.setName("jRadioButtonExportText"); // NOI18N
    jRadioButtonExportText.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jRadioButtonExportText.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jRadioButtonExportTextActionPerformed(evt);
        }
    });
    jToolBar3.add(jRadioButtonExportText);

    jTextFieldCustomText.setText(resourceMap.getString("jTextFieldCustomText.text")); // NOI18N
    jTextFieldCustomText.setEnabled(false);
    jTextFieldCustomText.setMaximumSize(new java.awt.Dimension(29, 30));
    jTextFieldCustomText.setName("jTextFieldCustomText"); // NOI18N
    jTextFieldCustomText.setPreferredSize(new java.awt.Dimension(29, 20));
    jTextFieldCustomText.addKeyListener(new java.awt.event.KeyAdapter() {
        public void keyTyped(java.awt.event.KeyEvent evt) {
            jTextFieldCustomTextKeyTyped(evt);
        }
    });
    jToolBar3.add(jTextFieldCustomText);
    jTextFieldCustomText.setVisible(false);

    buttonGroup1.add(jRadioButtonExportXML);
    jRadioButtonExportXML.setSelected(true);
    jRadioButtonExportXML.setText(resourceMap.getString("jRadioButtonExportXML.text")); // NOI18N
    jRadioButtonExportXML.setEnabled(false);
    jRadioButtonExportXML.setFocusable(false);
    jRadioButtonExportXML.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jRadioButtonExportXML.setName("jRadioButtonExportXML"); // NOI18N
    jRadioButtonExportXML.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jRadioButtonExportXML.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jRadioButtonExportXMLActionPerformed(evt);
        }
    });
    jToolBar3.add(jRadioButtonExportXML);

    jButtonRunExporter.setIcon(resourceMap.getIcon("jButtonRunExporter.icon")); // NOI18N
    jButtonRunExporter.setText(resourceMap.getString("jButtonRunExporter.text")); // NOI18N
    jButtonRunExporter.setEnabled(false);
    jButtonRunExporter.setFocusable(false);
    jButtonRunExporter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jButtonRunExporter.setName("jButtonRunExporter"); // NOI18N
    jButtonRunExporter.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButtonRunExporter.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonRunExporterActionPerformed(evt);
        }
    });
    jToolBar3.add(jButtonRunExporter);

    jButtonSaveExporter.setIcon(resourceMap.getIcon("jButtonSaveExporter.icon")); // NOI18N
    jButtonSaveExporter.setText(resourceMap.getString("jButtonSaveExporter.text")); // NOI18N
    jButtonSaveExporter.setEnabled(false);
    jButtonSaveExporter.setFocusable(false);
    jButtonSaveExporter.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jButtonSaveExporter.setName("jButtonSaveExporter"); // NOI18N
    jButtonSaveExporter.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButtonSaveExporter.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonSaveExporterActionPerformed(evt);
        }
    });
    jToolBar3.add(jButtonSaveExporter);

    jPanelExportar.add(jToolBar3, java.awt.BorderLayout.PAGE_START);

    jSplitPane3.setDividerLocation(300);
    jSplitPane3.setDividerSize(9);
    jSplitPane3.setMinimumSize(new java.awt.Dimension(235, 26));
    jSplitPane3.setName("jSplitPane3"); // NOI18N
    jSplitPane3.setOneTouchExpandable(true);
    jSplitPane3.setPreferredSize(new java.awt.Dimension(583, 335));

    jPanelTextArea.setMinimumSize(new java.awt.Dimension(124, 24));
    jPanelTextArea.setName("jPanelTextArea"); // NOI18N
    jPanelTextArea.setPreferredSize(new java.awt.Dimension(150, 233));
    jPanelTextArea.setLayout(new java.awt.BorderLayout());

    jScrollPane7.setName("jScrollPane7"); // NOI18N

    jTextArea1.setEditable(false);
    jTextArea1.setLineWrap(true);
    jTextArea1.setWrapStyleWord(true);
    jTextArea1.setName("jTextArea1"); // NOI18N
    jScrollPane7.setViewportView(jTextArea1);

    jPanelTextArea.add(jScrollPane7, java.awt.BorderLayout.CENTER);

    jSplitPane3.setLeftComponent(jPanelTextArea);

    jPanelInformation.setEnabled(false);
    jPanelInformation.setName("jPanelInformation"); // NOI18N
    jPanelInformation.setLayout(new java.awt.BorderLayout());

    jToolBar4.setFloatable(false);
    jToolBar4.setRollover(true);
    jToolBar4.setEnabled(false);
    jToolBar4.setName("jToolBar4"); // NOI18N

    jComboBoxChangeTypeDoc.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    String[] bill = { "Factura Simples", "Factura Composta" };
    jComboBoxChangeTypeDoc = new javax.swing.JComboBox(bill);
    jComboBoxChangeTypeDoc.setEnabled(false);
    jComboBoxChangeTypeDoc.setName("jComboBoxChangeTypeDoc"); // NOI18N
    jComboBoxChangeTypeDoc.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jComboBoxChangeTypeDocActionPerformed(evt);
        }
    });
    jToolBar4.add(jComboBoxChangeTypeDoc);
    jComboBoxChangeTypeDoc.setVisible(false);

    jPanelInformation.add(jToolBar4, java.awt.BorderLayout.PAGE_START);

    jToolBar5.setFloatable(false);
    jToolBar5.setRollover(true);
    jToolBar5.setEnabled(false);
    jToolBar5.setName("jToolBar5"); // NOI18N

    jButtonChangeXML.setIcon(resourceMap.getIcon("jButtonChangeXML.icon")); // NOI18N
    jButtonChangeXML.setText(resourceMap.getString("jButtonChangeXML.text")); // NOI18N
    jButtonChangeXML.setDefaultCapable(false);
    jButtonChangeXML.setEnabled(false);
    jButtonChangeXML.setFocusable(false);
    jButtonChangeXML.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    jButtonChangeXML.setName("jButtonChangeXML"); // NOI18N
    jButtonChangeXML.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jButtonChangeXML.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jButtonChangeXMLActionPerformed(evt);
        }
    });
    jToolBar5.add(jButtonChangeXML);

    jPanelInformation.add(jToolBar5, java.awt.BorderLayout.PAGE_END);

    jScrollPane8.setEnabled(false);
    jScrollPane8.setName("jScrollPane8"); // NOI18N
    jScrollPane8.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            jScrollPane8MouseClicked(evt);
        }
    });
    jScrollPane8.addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusGained(java.awt.event.FocusEvent evt) {
            jScrollPane8FocusGained(evt);
        }
    });

    jTableTypeOfDocument.setModel(new MyTableModel());
    jTableTypeOfDocument.setEnabled(false);
    jTableTypeOfDocument.setName("jTableTypeOfDocument"); // NOI18N
    jTableTypeOfDocument.getTableHeader().setReorderingAllowed(false);
    jTableTypeOfDocument.setColumnSelectionAllowed(true);
    jTableTypeOfDocument.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            jTableTypeOfDocumentMouseClicked(evt);
        }
    });
    jTableTypeOfDocument.addFocusListener(new java.awt.event.FocusAdapter() {
        public void focusGained(java.awt.event.FocusEvent evt) {
            jTableTypeOfDocumentFocusGained(evt);
        }
    });
    jScrollPane8.setViewportView(jTableTypeOfDocument);
    jTableTypeOfDocument.setFillsViewportHeight(true);
    //Fiddle with the Sport column's cell editors/renderers.
    setUpColumn(jTableTypeOfDocument, jTableTypeOfDocument.getColumnModel().getColumn(1));

    jPanelInformation.add(jScrollPane8, java.awt.BorderLayout.CENTER);
    jScrollPane8.setVisible(false);

    jSplitPane3.setRightComponent(jPanelInformation);

    jPanelExportar.add(jSplitPane3, java.awt.BorderLayout.CENTER);

    jTabbedPane1.addTab(resourceMap.getString("jPanelExportar.TabConstraints.tabTitle"), jPanelExportar); // NOI18N

    mainPanel.add(jTabbedPane1, java.awt.BorderLayout.CENTER);

    menuBar.setName("menuBar"); // NOI18N

    fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
    fileMenu.setName("fileMenu"); // NOI18N

    jSeparator1.setName("jSeparator1"); // NOI18N
    fileMenu.add(jSeparator1);

    jSeparator2.setName("jSeparator2"); // NOI18N
    fileMenu.add(jSeparator2);

    exitMenuItem.setAction(actionMap.get("quit")); // NOI18N
    exitMenuItem.setIcon(resourceMap.getIcon("exitMenuItem.icon")); // NOI18N
    exitMenuItem.setName("exitMenuItem"); // NOI18N
    fileMenu.add(exitMenuItem);

    menuBar.add(fileMenu);

    jMenuDefinitions.setText(resourceMap.getString("jMenuDefinitions.text")); // NOI18N
    jMenuDefinitions.setName("jMenuDefinitions"); // NOI18N

    jMenuItemTessPath.setIcon(resourceMap.getIcon("jMenuItemTessPath.icon")); // NOI18N
    jMenuItemTessPath.setText(resourceMap.getString("jMenuItemTessPath.text")); // NOI18N
    jMenuItemTessPath.setName("jMenuItemTessPath"); // NOI18N
    jMenuItemTessPath.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jMenuItemTessPathActionPerformed(evt);
        }
    });
    jMenuDefinitions.add(jMenuItemTessPath);

    jSeparator3.setName("jSeparator3"); // NOI18N
    jMenuDefinitions.add(jSeparator3);

    jMenuProcessLanguage.setIcon(resourceMap.getIcon("jMenuProcessLanguage.icon")); // NOI18N
    jMenuProcessLanguage.setText(resourceMap.getString("jMenuProcessLanguage.text")); // NOI18N
    jMenuProcessLanguage.setName("jMenuProcessLanguage"); // NOI18N

    group.add(jRadioButtonMenuItemPt);
    jRadioButtonMenuItemPt.setSelected(true);
    jRadioButtonMenuItemPt.setText(resourceMap.getString("jRadioButtonMenuItemPt.text")); // NOI18N
    jRadioButtonMenuItemPt.setActionCommand(resourceMap.getString("jRadioButtonMenuItemPt.actionCommand")); // NOI18N
    jRadioButtonMenuItemPt.setName("jRadioButtonMenuItemPt"); // NOI18N
    jRadioButtonMenuItemPt.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jRadioButtonMenuItemPtActionPerformed(evt);
        }
    });
    jMenuProcessLanguage.add(jRadioButtonMenuItemPt);

    group.add(jRadioButtonMenuItemEn);
    jRadioButtonMenuItemEn.setSelected(true);
    jRadioButtonMenuItemEn.setText(resourceMap.getString("jRadioButtonMenuItemEn.text")); // NOI18N
    jRadioButtonMenuItemEn.setActionCommand(resourceMap.getString("jRadioButtonMenuItemEn.actionCommand")); // NOI18N
    jRadioButtonMenuItemEn.setName("jRadioButtonMenuItemEn"); // NOI18N
    jRadioButtonMenuItemEn.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jRadioButtonMenuItemEnActionPerformed(evt);
        }
    });
    jMenuProcessLanguage.add(jRadioButtonMenuItemEn);

    jMenuDefinitions.add(jMenuProcessLanguage);

    menuBar.add(jMenuDefinitions);

    helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
    helpMenu.setName("helpMenu"); // NOI18N

    jMenuItemHelp.setIcon(resourceMap.getIcon("jMenuItemHelp.icon")); // NOI18N
    jMenuItemHelp.setText(resourceMap.getString("jMenuItemHelp.text")); // NOI18N
    jMenuItemHelp.setToolTipText(resourceMap.getString("jMenuItemHelp.toolTipText")); // NOI18N
    jMenuItemHelp.setName("jMenuItemHelp"); // NOI18N
    jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            jMenuItemHelpActionPerformed(evt);
        }
    });
    helpMenu.add(jMenuItemHelp);

    aboutMenuItem.setAction(actionMap.get("showAboutBox")); // NOI18N
    aboutMenuItem.setIcon(resourceMap.getIcon("aboutMenuItem.icon")); // NOI18N
    aboutMenuItem.setText(resourceMap.getString("aboutMenuItem.text")); // NOI18N
    aboutMenuItem.setToolTipText(resourceMap.getString("aboutMenuItem.toolTipText")); // NOI18N
    aboutMenuItem.setName("aboutMenuItem"); // NOI18N
    helpMenu.add(aboutMenuItem);

    menuBar.add(helpMenu);

    statusPanel.setName("statusPanel"); // NOI18N

    statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

    statusMessageLabel.setName("statusMessageLabel"); // NOI18N

    statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    progressBar.setName("progressBar"); // NOI18N
    progressBar.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            progressBarPropertyChange(evt);
        }
    });

    javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
    statusPanel.setLayout(statusPanelLayout);
    statusPanelLayout.setHorizontalGroup(
        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE)
        .addGroup(statusPanelLayout.createSequentialGroup()
            .addContainerGap()
            .addComponent(statusMessageLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 592, Short.MAX_VALUE)
            .addComponent(statusAnimationLabel)
            .addContainerGap())
        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, statusPanelLayout.createSequentialGroup()
            .addContainerGap(327, Short.MAX_VALUE)
            .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 275, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
    );
    statusPanelLayout.setVerticalGroup(
        statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(statusPanelLayout.createSequentialGroup()
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(statusMessageLabel)
                .addComponent(statusAnimationLabel)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(3, 3, 3))
    );

    setComponent(mainPanel);
    setMenuBar(menuBar);
    setStatusBar(statusPanel);

    bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonOpenActionPerformed

        
        int returnVal = fileChooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentDirectory = fileChooser.getCurrentDirectory().getPath();
            openFile(fileChooser.getSelectedFile());
            scaleX = scaleY = 1f;
            //put enable all the buttons
            imageOpen=true;
            jButtonOpen.setBackground(defaultColor);
            jScrollPane1.setEnabled(true);
            jImageLabel.setEnabled(true);
            jPanelZone.setEnabled(true);
            masterScrollPane.setEnabled(true);
            masterTable.setEnabled(true);
            jTextFieldZoneTypeName.setEnabled(true);
            newButton.setEnabled(true);
            deleteButton.setEnabled(true);
            detailScrollPane.setEnabled(true);
            detailTable.setEnabled(true);
            jTextFieldZoneName.setEnabled(true);
            newDetailButton.setEnabled(true);
            deleteDetailButton.setEnabled(true);
            jTextFieldX.setEnabled(true);
            jTextFieldY.setEnabled(true);
            jTextFieldWidth.setEnabled(true);
            jTextFieldHeight.setEnabled(true);
        }
        else{
            JOptionPane.showMessageDialog(frame,
            "Selecione um ficheiro",
            "Atenção",
            JOptionPane.WARNING_MESSAGE);
        }
        for (int i = 0; i < fileFilters.length; i++) {
                if (fileFilters[i] == fileChooser.getFileFilter()) {
                    filterIndex = i;
                    break;
                }
            }
    }//GEN-LAST:event_jButtonOpenActionPerformed

    private void jButtonFitImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFitImageActionPerformed
        this.jButtonFitImage.setEnabled(false);
        this.jButtonActualSize.setEnabled(true);

        scaleX = (float) imageIcon.getIconWidth() / (float) this.jScrollPane1.getWidth();
        scaleY = (float) imageIcon.getIconHeight() / (float) this.jScrollPane1.getHeight();
        fitImageChange(this.jScrollPane1.getWidth(), this.jScrollPane1.getHeight(), false);
        reset = true;
    }//GEN-LAST:event_jButtonFitImageActionPerformed

    private void jButtonZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomInActionPerformed
        this.jButtonActualSize.setEnabled(true);
        this.jButtonFitImage.setEnabled(true);

        doChange(true);
        reset = false;
    }//GEN-LAST:event_jButtonZoomInActionPerformed

    private void jButtonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonZoomOutActionPerformed
        this.jButtonActualSize.setEnabled(true);
        this.jButtonFitImage.setEnabled(true);

        doChange(false);
        reset = false;
    }//GEN-LAST:event_jButtonZoomOutActionPerformed

    private void jMenuItemTessPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTessPathActionPerformed
        chooser = new JFileChooser(tessPath);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setCurrentDirectory(new File(tessPath));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setApproveButtonText("Ok");
        chooser.setDialogTitle("Localize o Directório Tesseract");
        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (!tessPath.equals(chooser.getSelectedFile().getAbsolutePath())) {
                tessPath = chooser.getSelectedFile().getAbsolutePath();
                JOptionPane.showMessageDialog(frame, "Por favor, reinicie a aplicação para que as alterações tenham efeito.", APP_NAME, JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }//GEN-LAST:event_jMenuItemTessPathActionPerformed

    private void detailTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_detailTableMouseClicked

          //show the selectionLabel for witch zone
        int x = Integer.parseInt(jTextFieldX.getText());
//        x = (int) (x / scaleX);
//        x = list.get(masterTable.getSelectedRow()).getZoneDefinitionsList().get(detailTable.getSelectedRow()).getUpperleftx();
        int y = Integer.parseInt(jTextFieldY.getText());
//        y = (int) (y / scaleY);
//        y = list.get(masterTable.getSelectedRow()).getZoneDefinitionsList().get(detailTable.getSelectedRow()).getUpperlefty();
        int width = Integer.parseInt(jTextFieldWidth.getText());
//        width = (int) (width / scaleX);
//        width = list.get(masterTable.getSelectedRow()).getZoneDefinitionsList().get(detailTable.getSelectedRow()).getZoneWith();
        int height = Integer.parseInt(jTextFieldHeight.getText());
//        height = (int) (height / scaleY);
//        height = list.get(masterTable.getSelectedRow()).getZoneDefinitionsList().get(detailTable.getSelectedRow()).getZoneHeight();
        originalRectangleX = x;
        originalRectangleY = y;
        try {
            ((JImageLabel) jImageLabel).getRect().setBounds(x, y, width, height);
            
            originalRectangleWidth = (int) ((JImageLabel) jImageLabel).getRect().getWidth();
            originalRectangleHeight = (int) ((JImageLabel) jImageLabel).getRect().getHeight();

        } catch (Exception e) {
            System.out.println("detailTableMouseClicked ERROR - "+e.getMessage());
        }
        actualSize();

    }//GEN-LAST:event_detailTableMouseClicked

    public void updateSelectedRectangleValue(){
        
        //put the new location of the selectionlabel in the selected zone
        String stringX = null;
        String stringY = null;
        String stringWidth = null;
        String stringHeight = null;
        int rand = (int) (Math.random() * 340 + 10);
        int x, y, width, height;
        try {

            if(((JImageLabel) jImageLabel).getRect().isEmpty()){
                x = rand;
                y = rand;
                width = rand;
                height = rand;
            }
            else{
                //actualSize rectangle
                scaleX = (float) imageIcon.getIconWidth() / originalW;
                scaleY = (float) imageIcon.getIconHeight() / originalH;
                x = (int) (((JImageLabel) jImageLabel).getRect().getX() / scaleX);
                stringX = Integer.toString(x);
                y = (int) (((JImageLabel) jImageLabel).getRect().getY() / scaleY);
                stringY = Integer.toString(y);
                width = (int) (((JImageLabel) jImageLabel).getRect().getWidth() / scaleX);
                stringWidth = Integer.toString(width);
                height = (int) (((JImageLabel) jImageLabel).getRect().getHeight() / scaleY);
                stringHeight = Integer.toString(height);

            }

        } catch (Exception e) {
            System.out.println("updateSelectedRectangleValue ERROR - "+e.getMessage());
//            e.printStackTrace();
        }


        jTextFieldX.setText(stringX);
        jTextFieldY.setText(stringY);
        jTextFieldWidth.setText(stringWidth);
        jTextFieldHeight.setText(stringHeight);
        
    }

    private void jImageLabelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jImageLabelMouseReleased

        updateSelectedRectangleValue();
        if(imageOpen==true){
            saveButton.setBackground(Color.red);
        }
    }//GEN-LAST:event_jImageLabelMouseReleased

    private void jButtonActualSizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonActualSizeActionPerformed
    actualSize();
    }//GEN-LAST:event_jButtonActualSizeActionPerformed

    private void actualSize(){
        this.jButtonFitImage.setEnabled(true);
        this.jButtonActualSize.setEnabled(false);

        scaleX = (float) imageIcon.getIconWidth() / originalW;
        scaleY = (float) imageIcon.getIconHeight() / originalH;

        fitImageChange(originalW, originalH, true);
        reset = true;
    }
    private void fitImageChange(final int width, final int height, final boolean check) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                for (ImageIconScalable tempImageIcon : imageList) {
                    int rectangleX = (int) ((JImageLabel) jImageLabel).getRect().getX();
                    int rectangleY = (int) ((JImageLabel) jImageLabel).getRect().getY();
                    int widthRectangle = (int)((JImageLabel) jImageLabel).getRect().getWidth();
                    int heightRectangle = (int)((JImageLabel) jImageLabel).getRect().getHeight();
                    
                    if (check==true) { //actual size
                        tempImageIcon.setScaledSize(width, height);

                        //get the X and Y position of the rectangle
                        int x = (int) (rectangleX / scaleX);
                        int y = (int) (rectangleY / scaleY);

                        ((JImageLabel) jImageLabel).getRect().setSize(
                                (int) (widthRectangle / scaleX),
                                (int) (heightRectangle / scaleY));
                        ((JImageLabel) jImageLabel).getRect().setLocation(x, y);
                    }
                    else {//fit image
                      tempImageIcon.setScaledSize(width, height);

                        //get the X and Y position of the rectangle
                        int x = (int) (((JImageLabel) jImageLabel).getRect().getX() / scaleX);
                        int y = (int) (((JImageLabel) jImageLabel).getRect().getY() / scaleY);

                        ((JImageLabel) jImageLabel).getRect().setSize(
                                (int) (widthRectangle / scaleX),
                                (int) (heightRectangle / scaleY));
                        ((JImageLabel) jImageLabel).getRect().setLocation(x, y);

                    }
                }
//                updateSelectedRectangleValue();
                imageIcon = imageList.get(imageIndex);
                jImageLabel.revalidate();
                jScrollPane1.repaint();
            }
        });
    }

    private void jButtonInputFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonInputFolderActionPerformed

        c = getFrame();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        // get the Jtable model
        modelo = (DefaultTableModel) jTableFileToProcess.getModel();
        resultModel = (DefaultTableModel) jTableResults.getModel();

        int returnVal = chooser.showOpenDialog(frame);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (!inputPath.equals(chooser.getSelectedFile().getAbsolutePath())) {
                inputPath = chooser.getSelectedFile().getAbsolutePath();
                this.jTextFieldInputFolder.setText(inputPath);

                if(modelo.getRowCount() > 0){
                    removeTableITems();
                }
                scanFolder();

            }
        }
        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_jButtonInputFolderActionPerformed

    private void jComboBoxSelectedZoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSelectedZoneActionPerformed

        resultModel = (DefaultTableModel) jTableResults.getModel();

          if (jComboBoxSelectedZone.getSelectedIndex() < 0)
              value = 0;
          else
              value = jComboBoxSelectedZone.getSelectedIndex();

        zoneDetailed = new String[list.get(value).getZoneDefinitionsList().size()];

        for(int i =0; i< list.get(value).getZoneDefinitionsList().size(); i++){
            zoneDetailed[i] = list.get(value).getZoneDefinitionsList().get(i).getZoneDefName().replace(" ", "");
            resultModel.addColumn(zoneDetailed[i].toString());
        }

        jTableResults.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },zoneDetailed
        ));
    }//GEN-LAST:event_jComboBoxSelectedZoneActionPerformed

   public void openFilesToProcess(File selectedFile) {

        iioImageList = ImageIOHelper.getIIOImageList(selectedFile);
        imageList = ImageIOHelper.getImageList(iioImageList);

        if (imageList == null) {
            JOptionPane.showMessageDialog(null, "Não é possivel abrir as Imagens", APP_NAME, JOptionPane.ERROR_MESSAGE);
            return;
        }
        imageTotal = imageList.size();
        imageIndex = 0;

        displayImage();

        originalW = imageIcon.getIconWidth();
        originalH = imageIcon.getIconHeight();

    }

   //reads and prints the name of all the files in a particular parent folder.
   private void scanFolder(){
       boolean haveImagesInside=false;
       listOfImagesFilesToProcess = new ArrayList<File>(); //contains all the images files to be processed

       removeTableITems();
       if (!listOfImagesFilesToProcess.isEmpty())
           removeAllElementsArrayListFileToProcess();

       //replace backslash special caracter to slash caracter, notice the java online real the adress with forward slash
       String backslash = System.getProperty("file.separator") ;
       inputPath = inputPath.replace(backslash,"/");
       folder = new File(inputPath);

       listOfFiles = folder.listFiles();
            for (int i = 0; i < listOfFiles.length; i++) {
              if (listOfFiles[i].isFile()) {
                boolean isImage= acceptFile(listOfFiles[i], listOfFiles[i].getName());
                    if(isImage){
                        listOfImagesFilesToProcess.add(listOfFiles[i]);
                        this.jButtonProcessFiles.setEnabled(true);
                        haveImagesInside=true;
                        colorTextField(haveImagesInside);
                        //put the first image in jImageLabel
                        scaleX = scaleY = 1f;
                        imageOpen=true;
                        jButtonOpen.setBackground(defaultColor);
                        jScrollPane1.setEnabled(true);
                        jImageLabel.setEnabled(true);
                        File temp = new File(listOfImagesFilesToProcess.get(0).getAbsolutePath());
                        openFile(temp);
                        // add a new row to the model
                        modelo.addRow( new String [] {listOfFiles[i].getName(), originalW+"x"+originalH} );
                        //put enable all the buttons
                        imageOpen=true;
                        jPanelZone.setEnabled(true);
                        masterScrollPane.setEnabled(true);
                        masterTable.setEnabled(true);
                        jTextFieldZoneTypeName.setEnabled(true);
                        newButton.setEnabled(true);
                        deleteButton.setEnabled(true);
                        detailScrollPane.setEnabled(true);
                        detailTable.setEnabled(true);
                        jTextFieldZoneName.setEnabled(true);
                        newDetailButton.setEnabled(true);
                        deleteDetailButton.setEnabled(true);
                        jTextFieldX.setEnabled(true);
                        jTextFieldY.setEnabled(true);
                        jTextFieldWidth.setEnabled(true);
                        jTextFieldHeight.setEnabled(true);
                    }
              }
            }

       if(haveImagesInside == false) {
           colorTextField(haveImagesInside);
           JOptionPane.showMessageDialog(frame, "A pasta selecionada não contêm imagens", "Atenção", JOptionPane.WARNING_MESSAGE);
       }
    }

   private void removeAllElementsArrayListFileToProcess(){
       listOfImagesFilesToProcess.clear();
   }

   private void removeAllElementsArrayListZoneResult(){
       zoneResult.clear();
   }

   //verify the format of imagens (tif;tiff;jpg;jpeg;gif;png;bmp)
   private boolean acceptFile(File dir, String name) {
      boolean isImage=false;
       if(name.endsWith(".tif"))  isImage = true;
       if(name.endsWith(".tiff")) isImage = true;
       if(name.endsWith(".jpg"))  isImage = true;
       if(name.endsWith(".jpeg")) isImage = true;
       if(name.endsWith(".gif"))  isImage = true;
       if(name.endsWith(".png"))  isImage = true;
       if(name.endsWith(".bmp"))  isImage = true;
     return isImage;
   }

   private void removeTableITems(){
    modelo.getDataVector().removeAllElements();
    jTableFileToProcess.updateUI();
   }

   private void removeTableResultITems(){
    resultModel.getDataVector().removeAllElements();
    jTableResults.updateUI();
   }

   //set the color JtextFieldInputFolder to red or green if the value is true or false
   private void colorTextField(boolean haveImagesInside){
//        if(jTextFieldInputFolder.getText().isEmpty())
        if(haveImagesInside)
            jTextFieldInputFolder.setBackground(new Color(231,252,238));
        else
            jTextFieldInputFolder.setBackground(new Color(252,231,231));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("progress" == evt.getPropertyName()) {
            progress = (Integer) evt.getNewValue();
            progressBar.setValue(progress);
        } 
    }

    private void jButtonProcessFilesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonProcessFilesActionPerformed
        jButtonProcessFiles.setBackground(defaultColor);
        jToolBar3.setEnabled(true);
        jPanelExportar.setEnabled(true);
        resultModel = (DefaultTableModel) jTableResults.getModel();
        zoneResult = new ArrayList<String>();
        scaleProgressbar = 100 / listOfImagesFilesToProcess.size();
        progressBar.setValue(0);
        statusMessageLabel.setText("A Correr OCR...");
        progressBar.setVisible(true);
        progressBar.setStringPainted(true);
        c = getFrame();
        c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

//        progressBarOCR();
        this.jButtonProcessFiles.setEnabled(false);

        if(resultModel.getRowCount() > 0){
            removeTableResultITems();
        }
        //run OCR
        performOCR();

        jToolBar3.setEnabled(true);
        jButtonRunExporter.setEnabled(true);
        jRadioButtonExportText.setEnabled(true);
        jRadioButtonExportXML.setEnabled(true);
        jToolBar4.setEnabled(true);
        jToolBar5.setEnabled(true);
        jPanelInformation.setEnabled(true);
        jComboBoxChangeTypeDoc.setEnabled(true);
        jScrollPane8.setEnabled(true);
        jTableTypeOfDocument.setEnabled(true);

    }//GEN-LAST:event_jButtonProcessFilesActionPerformed

    private void showResults(){  
           if(counter==zoneDetailed.length-1){
            resultModel.addRow(zoneResult.toArray());
           }
    }

    private void jRadioButtonMenuItemEnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemEnActionPerformed

        selectedLang = evt.getActionCommand();
        setIndexImage(selectedLang);
    }//GEN-LAST:event_jRadioButtonMenuItemEnActionPerformed

    private void jRadioButtonMenuItemPtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemPtActionPerformed
    jRadioButtonMenuItemEnActionPerformed(evt);
    }//GEN-LAST:event_jRadioButtonMenuItemPtActionPerformed

    private void jButtonRunExporterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRunExporterActionPerformed
        jButtonSaveExporter.setEnabled(true);
        jTextArea1 = new javax.swing.JTextArea();
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane7.setViewportView(jTextArea1);
        ArrayList<String> typeElementsXML;
        jButtonRunExporter.setBackground(defaultColor);
        jButtonChangeXML.setBackground(defaultColor);

        if(jRadioButtonExportXML.isSelected()){

        jComboBoxChangeTypeDoc.setVisible(true);
        jScrollPane8.setVisible(true);
        jPanelInformation.setEnabled(true);
        jComboBoxChangeTypeDoc.setEnabled(true);
        jButtonChangeXML.setEnabled(true);
        jScrollPane8.setEnabled(true);
        jTableTypeOfDocument.setEnabled(true);
        fc.addChoosableFileFilter(new MyFilterXML());
        fc.setAcceptAllFileFilterUsed(false);

        xmlJDOM = new XmlJDOM();
        String doc = null;
        //read type elements XML
        try {
            xmlJDOM.readXML();
            //convert ArrayList to ArrayString
            typeElementsXML = xmlJDOM.getTypeElementsXML();
            String[] bill = new String[typeElementsXML.size()];
            bill = typeElementsXML.toArray(bill);

            jToolBar4.remove(jComboBoxChangeTypeDoc);
            jComboBoxChangeTypeDoc = new javax.swing.JComboBox(bill);
            jComboBoxChangeTypeDoc.setName("jComboBoxChangeTypeDoc"); // NOI18N

            jComboBoxChangeTypeDoc.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    jComboBoxChangeTypeDocActionPerformed(evt);
                }
            });
            jToolBar4.add(jComboBoxChangeTypeDoc);
            
        } catch (JDOMException ex) {
            Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
        }
        //PROCESS XML and appent in jTextArea1
        try {
           doc =  xmlJDOM.writeXML(zoneDetailed, resultModel, listOfImagesFilesToProcess.size());
        } catch (IOException ex) {
            Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
        }
        jTextArea1.append(doc);
        jComboBoxChangeTypeDocActionPerformed(evt);
        }
        else if(jRadioButtonExportText.isSelected()){
            fc.addChoosableFileFilter(new MyFilterTXT());
            fc.setAcceptAllFileFilterUsed(false);
            for(int i =0; i< listOfImagesFilesToProcess.size(); i++){

                for(int j =0; j< zoneDetailed.length; j++){
                    String zoneName = (String) resultModel.getValueAt(i, j).toString();
                    String zoneNameTemp = zoneName.replaceAll("\n", "");
                    jTextArea1.append(zoneNameTemp+jTextFieldCustomText.getText());
                    jTextArea1.append("\n");
                }
            }
        }
        jPanelExportar.updateUI();

    }//GEN-LAST:event_jButtonRunExporterActionPerformed

    class MyTableModel extends AbstractTableModel {

    private String[] columnNames = {"Elementos do XML",
                                    "Nome a Mudar"};
    private Object[][] data;

    public MyTableModel(){
        Object[][] data = {
        {"Zone1","opcao"},
        {"Zone2", "opcao"},
        {"Zone3", "opcao"},
        {"Zone4", "opcao"}
    };
        this.data=data;
    }
    //create table with simple model
    public MyTableModel(Object[][] data){
        this.data=data;
    }
    
//    public final Object[] longValues = {"Sharon", "Campione"};

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return data.length;
    }

    public String getColumnName(int col) {
        return columnNames[col];
    }

    public Object getValueAt(int row, int col) {
        return data[row][col];
    }

    /*
     * JTable uses this method to determine the default renderer/
     * editor for each cell.  If we didn't implement this method,
     * then the last column would contain text ("true"/"false"),
     * rather than a check box.
     */
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /*
     * Don't need to implement this method unless your table's
     * editable.
     */
    public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears onscreen.
        if (col < 1) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * Don't need to implement this method unless your table's
     * data can change.
     */
    public void setValueAt(Object value, int row, int col) {
        if (DEBUG) {
            System.out.println("Setting value at " + row + "," + col
                               + " to " + value
                               + " (an instance of "
                               + value.getClass() + ")");
        }

        data[row][col] = value;
        fireTableCellUpdated(row, col);

        if (DEBUG) {
            System.out.println("New value of data:");
            printDebugData();
        }
    }

    private void printDebugData() {
        int numRows = getRowCount();
        int numCols = getColumnCount();

        for (int i=0; i < numRows; i++) {
            System.out.print("    row " + i + ":");
            for (int j=0; j < numCols; j++) {
                System.out.print("  " + data[i][j]);
            }
            System.out.println();
        }
        System.out.println("--------------------------");
    }
}
    //Factura Simples
    public void setUpColumn(JTable table, TableColumn sportColumn) {
            try {
            //Set up the editor for the sport cells.
            ArrayList<String> elementsXML;
            elementsXML = xmlJDOM.getSimpleElementsXML();
            String[] billElements = new String[elementsXML.size()];
            billElements = elementsXML.toArray(billElements);
            comboBox = new JComboBox();

            for (int i = 0; i < billElements.length; i++) {
                comboBox.addItem(billElements[i].toString());
            }

        sportColumn.setCellEditor(new DefaultCellEditor(comboBox));

        //Set up tool tips for the sport cells.
        DefaultTableCellRenderer renderer =
                new DefaultTableCellRenderer();
        renderer.setToolTipText("Seleccione para escolher a opção");
        sportColumn.setCellRenderer(renderer);
        } catch (NullPointerException e) {
        }
    }
    //Factura Composta
    public void setUpColumn2(JTable table, TableColumn sportColumn) {
        try {
            //Set up the editor for the sport cells.
            ArrayList<String> elementsXML;
            elementsXML = xmlJDOM.getCompostoElementsXML();
            String[] billElements = new String[elementsXML.size()];
            billElements = elementsXML.toArray(billElements);
            comboBox = new JComboBox();
            for (int i = 0; i < billElements.length; i++) {
                comboBox.addItem(billElements[i].toString());
            }

            sportColumn.setCellEditor(new DefaultCellEditor(comboBox));

            //Set up tool tips for the sport cells.
            DefaultTableCellRenderer renderer =
                    new DefaultTableCellRenderer();
            renderer.setToolTipText("Seleccione para escolher a opção");
            sportColumn.setCellRenderer(renderer);
        } catch (NullPointerException e) {
        }
    }

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed

        jTabbedPane1.setSelectedIndex(1);
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
    jTabbedPane1.setSelectedIndex(2);
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
    jTabbedPane1.setSelectedIndex(3);
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jComboBoxChangeTypeDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxChangeTypeDocActionPerformed

        Object[][] data =  new Object[zoneDetailed.length][2];
        for(int i=0; i<zoneDetailed.length;i++){
            for(int j=0; j<2;j++){
                if(j==0)
                    data[i][j] = zoneDetailed[i].toString();
                else
                    data[i][j] = "Opcao";

            }
        }

        if(jComboBoxChangeTypeDoc.getSelectedIndex()==0){

             jTableTypeOfDocument.setModel(new MyTableModel(data));

            setUpColumn(jTableTypeOfDocument, jTableTypeOfDocument.getColumnModel().getColumn(1));

            jPanelInformation.updateUI();
            jScrollPane8.updateUI();
            jTableTypeOfDocument.updateUI();
        }

        else{
             jTableTypeOfDocument.setModel(new MyTableModel(data));

            setUpColumn2(jTableTypeOfDocument, jTableTypeOfDocument.getColumnModel().getColumn(1));

            jPanelInformation.updateUI();
            jScrollPane8.updateUI();
            jTableTypeOfDocument.updateUI();

        }
        
    }//GEN-LAST:event_jComboBoxChangeTypeDocActionPerformed

    private void jButtonChangeXMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonChangeXMLActionPerformed

        String doc = null;
        String [] newValue;
        ArrayList<String> option = new ArrayList<String>();

        for(int i =0; i<jTableTypeOfDocument.getModel().getRowCount();i++){
            option.add(jTableTypeOfDocument.getModel().getValueAt(i, 1).toString());
        }
        
        try {
            newValue = xmlJDOM.changeElement(zoneDetailed.length, option);
            doc = xmlJDOM.writeXML(newValue, resultModel, listOfImagesFilesToProcess.size());
        } catch (JDOMException ex) {
            Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
        }

        jTextArea1 = new javax.swing.JTextArea();
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane7.setViewportView(jTextArea1);
        jTextArea1.append(doc);
        jButtonSaveExporter.setEnabled(true);
        jButtonChangeXML.setBackground(defaultColor);
        jPanelExportar.updateUI();

    }//GEN-LAST:event_jButtonChangeXMLActionPerformed

    private void jButtonSaveExporterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveExporterActionPerformed

        //Handle save button action.
        int returnVal = fc.showSaveDialog(frame);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if(jRadioButtonExportXML.isSelected()){
                try {
                    xmlJDOM.saveXML(file.getAbsolutePath());
                } catch (IOException ex) {
                    Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if(jRadioButtonExportText.isSelected()){
                try {
                    xmlJDOM.saveTXT(file.getAbsolutePath(), jTextArea1.getText());
                } catch (IOException ex) {
                    Logger.getLogger(EnaOCRView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            JOptionPane.showMessageDialog(frame,
            "Cancelado pelo Utilizador",
            "Atenção",
            JOptionPane.WARNING_MESSAGE);
        }
        jButtonSaveExporter.setBackground(defaultColor);
        jButtonChangeXML.setBackground(defaultColor);
        jScrollPane8.setFocusable(false);
        jTableTypeOfDocument.setFocusable(false);


    }//GEN-LAST:event_jButtonSaveExporterActionPerformed

    private void jRadioButtonExportTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonExportTextActionPerformed
        jPanelInformation.setEnabled(false);
        jComboBoxChangeTypeDoc.setVisible(false);
        jComboBoxChangeTypeDoc.setEnabled(false);
        jButtonChangeXML.setEnabled(false);
        jScrollPane8.setEnabled(false);
        jScrollPane8.setVisible(false);
        jTableTypeOfDocument.setEnabled(false);
        jTextFieldCustomText.setVisible(true);
        jTextFieldCustomText.setEnabled(true);
        jButtonSaveExporter.setEnabled(false);

        jTextFieldCustomText.setText(",");
        jTextArea1 = new javax.swing.JTextArea();
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane7.setViewportView(jTextArea1);
        jButtonChangeXML.setBackground(defaultColor);       
        jPanelExportar.updateUI();

    }//GEN-LAST:event_jRadioButtonExportTextActionPerformed

    private void jRadioButtonExportXMLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonExportXMLActionPerformed
        jTextFieldCustomText.setVisible(false);
        jTextFieldCustomText.setEnabled(false);
        jButtonSaveExporter.setEnabled(false);

        jTextArea1 = new javax.swing.JTextArea();
        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(5);
        jTextArea1.setName("jTextArea1"); // NOI18N
        jScrollPane7.setViewportView(jTextArea1);
        jButtonChangeXML.setBackground(defaultColor);
        jPanelExportar.updateUI();
    }//GEN-LAST:event_jRadioButtonExportXMLActionPerformed

    private void progressBarPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_progressBarPropertyChange
    propertyChange(evt);
    }//GEN-LAST:event_progressBarPropertyChange

    private void openBrowser(){

      String[] browsers = { "firefox", "opera", "konqueror", "epiphany",
      "seamonkey", "galeon", "kazehakase", "mozilla", "netscape", "chrome" };

      String url = "ajuda.html";
      
      String osName = System.getProperty("os.name");
      
      try {
         if (osName.startsWith("Mac OS")) {
            Class<?> fileMgr = Class.forName("com.apple.eio.FileManager");
            Method openURL = fileMgr.getDeclaredMethod("openURL",
               new Class[] {String.class});
            openURL.invoke(null, new Object[] {url});
            }
         else if (osName.startsWith("Windows"))
            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
         else { //assume Unix or Linux
            boolean found = false;
            for (String browser : browsers)
               if (!found) {
                  found = Runtime.getRuntime().exec(
                     new String[] {"which", browser}).waitFor() == 0;
                  if (found)
                     Runtime.getRuntime().exec(new String[] {browser, url});
                  }
            if (!found)
               throw new Exception(Arrays.toString(browsers));
            }
         }
      catch (Exception e) {
         JOptionPane.showMessageDialog(null,
            "Error attempting to launch web browser\n" + e.toString());
         }        
    }

    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHelpActionPerformed
      openBrowser();
    }//GEN-LAST:event_jMenuItemHelpActionPerformed

    private void jLabel8MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel8MouseClicked
        openBrowser();
    }//GEN-LAST:event_jLabel8MouseClicked

    private void jTableTypeOfDocumentMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableTypeOfDocumentMouseClicked
        jButtonChangeXML.setBackground(new Color(252,231,231));
    }//GEN-LAST:event_jTableTypeOfDocumentMouseClicked

    private void jTextFieldCustomTextKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldCustomTextKeyTyped
        jButtonRunExporter.setBackground(new Color(252,231,231));
    }//GEN-LAST:event_jTextFieldCustomTextKeyTyped

    private void jScrollPane8MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jScrollPane8MouseClicked
        jButtonChangeXML.setBackground(new Color(252,231,231));
    }//GEN-LAST:event_jScrollPane8MouseClicked

    private void jScrollPane8FocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jScrollPane8FocusGained
        jScrollPane8.setFocusable(true);
        jButtonChangeXML.setBackground(new Color(252,231,231));
    }//GEN-LAST:event_jScrollPane8FocusGained

    private void jTableTypeOfDocumentFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTableTypeOfDocumentFocusGained
        jScrollPane8.setFocusable(true);
        jButtonChangeXML.setBackground(new Color(252,231,231));
    }//GEN-LAST:event_jTableTypeOfDocumentFocusGained

    private void setIndexImage(String selectedLang){

        if (selectedLang.equals("en")) {
         indexImage = 0; //english
        }
        if (selectedLang.equals("por")) {
         indexImage = 1; //portuguese
        }
        if (selectedLang.equals("vie")) {
            indexImage = 2; //vietnami
        }
        
        prefs.get("langCode", null);
        if (langCodes != null && indexImage != -1)
            curLangCode = langCodes[indexImage];

    }

    private void performOCR() {
        
        setIndexImage("en");

        SwingWorker worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {

            for(int imageListIndex=0; imageListIndex<listOfImagesFilesToProcess.size();imageListIndex++){
                try {
                change=false;
                counter=-1;
                openFilesToProcess(listOfFiles[imageListIndex]);
                ii = (ImageIcon) jImageLabel.getIcon();

                for(int zoneListIndex=0; zoneListIndex<zoneDetailed.length; zoneListIndex++){

                int x = list.get(value).getZoneDefinitionsList().get(zoneListIndex).getUpperleftx();
                int y = list.get(value).getZoneDefinitionsList().get(zoneListIndex).getUpperlefty();
                int width = list.get(value).getZoneDefinitionsList().get(zoneListIndex).getZoneWith();
                int height = list.get(value).getZoneDefinitionsList().get(zoneListIndex).getZoneHeight();

               if (width >0 && height>0) {
                    try {
                        BufferedImage bi = ((BufferedImage) ii.getImage()).getSubimage((int) (x * scaleX), (int) (y * scaleY), (int) (width * scaleX), (int) (height * scaleY));
                        IIOImage iioImage = new IIOImage(bi, null, null);
                        ArrayList<IIOImage> tempList = new ArrayList<IIOImage>();
                        tempList.add(iioImage);

                        counter++;
                        OCRImageEntity entity = new OCRImageEntity(tempList, 0);
                        OCR ocrEngine = new OCR(tessPath);
                        String textRecognized = ocrEngine.recognizeText(entity.getClonedImageFiles(), curLangCode);
                        zoneResult.add(textRecognized);
                        showResults();
                        if (counter==zoneDetailed.length-1){
                                counter=-1;
                                removeAllElementsArrayListZoneResult();
                        }

                        } catch (RasterFormatException rfe) {
                            rfe.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        OCRImageEntity entity = new OCRImageEntity(iioImageList, imageIndex);
                        OCR ocrEngine = new OCR(tessPath);
                        String textRecognized = ocrEngine.recognizeText(entity.getClonedImageFiles(), curLangCode);
                        zoneResult.add(textRecognized);
                        showResults();
                        if (counter==zoneDetailed.length-1){
                                counter=-1;
                                removeAllElementsArrayListZoneResult();
                        }
                }
            }
                progressBar.setValue(0);
                progressBar.setValue(progress);
                progress = progress + scaleProgressbar;

                    Thread.sleep(450);
                } catch (InterruptedException ignore) {}
        }
                return textRecognized;
            }

            @Override
            protected void done() {
               
                try {
                    try {
                        String result = get().toString();
                    } catch (InterruptedException ignore) {
                        ignore.printStackTrace();
                    } catch (java.util.concurrent.ExecutionException e) {
                        String why = null;
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            if (cause instanceof IOException) {
                                why = "Cannot_find_Tesseract._Please_set_its_path.";
                            } else if (cause instanceof FileNotFoundException) {
                                why = "An_exception_occurred_in_Tesseract_engine_while_recognizing_this_image.";
                            } else if (cause instanceof OutOfMemoryError) {
                                why = "_has_run_out_of_memory.\nPlease_restart_";
                            } else {
                                why = cause.getMessage();
                            }
                        } else {
                            why = e.getMessage();
                        }
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(null, why, APP_NAME, JOptionPane.ERROR_MESSAGE);
                    } finally {
                        jButtonProcessFiles.setEnabled(true);
                        statusMessageLabel.setText("OCR Completo.");
                        progressBar.setValue(100);
                        c.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        progressBar.setVisible(false);
                        progress = 0;
                        scaleProgressbar = 0;

                    }
                } catch (NullPointerException nullException) {}
            }
        };

        worker.execute();
    }

    public void removeColumnAndData(JTable table, int vColIndex) {

        resultModel.getDataVector().removeAllElements();
    }

    private int snap(final int ideal, final int min, final int max) {
        final int TOLERANCE = 0;
        return ideal < min + TOLERANCE ? min : (ideal > max - TOLERANCE ? max : ideal);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JButton deleteButton;
    private javax.swing.JButton deleteDetailButton;
    private javax.swing.JScrollPane detailScrollPane;
    private javax.swing.JTable detailTable;
    private javax.persistence.EntityManager entityManager;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButtonActualSize;
    private javax.swing.JButton jButtonChangeXML;
    private javax.swing.JButton jButtonFitImage;
    private javax.swing.JButton jButtonInputFolder;
    private javax.swing.JButton jButtonOpen;
    private javax.swing.JButton jButtonProcessFiles;
    private javax.swing.JButton jButtonRunExporter;
    private javax.swing.JButton jButtonSaveExporter;
    private javax.swing.JButton jButtonZoomIn;
    private javax.swing.JButton jButtonZoomOut;
    private javax.swing.JComboBox jComboBoxChangeTypeDoc;
    private javax.swing.JComboBox jComboBoxSelectedZone;
    private javax.swing.JLabel jImageLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelDefinirZonas;
    private javax.swing.JLabel jLabelExportarResultados;
    private javax.swing.JLabel jLabelProcessarFicheiros;
    private javax.swing.JLabel jLabelSubTitle;
    private javax.swing.JLabel jLabelTitle;
    private javax.swing.JMenu jMenuDefinitions;
    private javax.swing.JMenuItem jMenuItemHelp;
    private javax.swing.JMenuItem jMenuItemTessPath;
    private javax.swing.JMenu jMenuProcessLanguage;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanelDefinirZonas;
    private javax.swing.JPanel jPanelExportar;
    private javax.swing.JPanel jPanelInformation;
    private javax.swing.JPanel jPanelInicio;
    private javax.swing.JPanel jPanelProcessarFicheiros;
    private javax.swing.JPanel jPanelTestImage;
    private javax.swing.JPanel jPanelTextArea;
    private javax.swing.JPanel jPanelZone;
    private javax.swing.JRadioButton jRadioButtonExportText;
    private javax.swing.JRadioButton jRadioButtonExportXML;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemEn;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemPt;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JToolBar.Separator jSeparator4;
    private javax.swing.JToolBar.Separator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JSplitPane jSplitPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTable jTableFileToProcess;
    private javax.swing.JTable jTableResults;
    private javax.swing.JTable jTableTypeOfDocument;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextFieldCustomText;
    private javax.swing.JTextField jTextFieldHeight;
    private javax.swing.JTextField jTextFieldInputFolder;
    private javax.swing.JTextField jTextFieldWidth;
    private javax.swing.JTextField jTextFieldX;
    private javax.swing.JTextField jTextFieldY;
    private javax.swing.JTextField jTextFieldZoneName;
    private javax.swing.JTextField jTextFieldZoneTypeName;
    private javax.swing.JTextPane jTextPane1;
    private javax.swing.JTextPane jTextPane2;
    private javax.swing.JTextPane jTextPane3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JToolBar jToolBar4;
    private javax.swing.JToolBar jToolBar5;
    private java.util.List<dataBase.Zonetype> list;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JScrollPane masterScrollPane;
    private javax.swing.JTable masterTable;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JButton newButton;
    private javax.swing.JButton newDetailButton;
    private javax.swing.JProgressBar progressBar;
    private javax.persistence.Query query;
    private javax.swing.JButton saveButton;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox, xmlBox;

    private boolean saveNeeded;
}
