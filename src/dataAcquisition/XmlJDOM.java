package dataAcquisition;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.jdom.output.XMLOutputter;
import java.io.FileWriter.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.table.DefaultTableModel;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
/**
 *
 * @author Joao Machete
 */
public class XmlJDOM extends Element {

    private FileWriter writer;
    //Declaração dos elementos que irão compor a estrutura do documento.
    private Element newData, nameElement;
    private Element enaOCR, zone;
    private Document doc;
    private String [] zoneDetailed, newZoneDetailed;
    private XMLOutputter xout;
    private DefaultTableModel model;
    private int sizeFiles=0;
    private ArrayList<String> typeElementsXML, elementsSimplesXML, elementsCompostoXML;


    public String writeXML(String [] zoneDetailed, DefaultTableModel model, int sizeFiles) throws IOException{

        this.zoneDetailed=zoneDetailed;
        this.model=model;
        this.sizeFiles=sizeFiles;
        newData = new Element("NewData");
        
        for(int imageListIndex=0; imageListIndex<sizeFiles;imageListIndex++){
            enaOCR = new Element("EnaOCR");

            for(int i =0; i< zoneDetailed.length; i++){
                zone = new Element(zoneDetailed[i].toString());
//                enaOCR.addContent(newLine);
                enaOCR.addContent(zone);
                String zoneName = (String) model.getValueAt(imageListIndex, i).toString();
                //remove any space inside string zoneName
                String padrao = "\\s{2,}";
                Pattern regPat = Pattern.compile(padrao);
                Matcher matcher = regPat.matcher(zoneName);
                String res = matcher.replaceAll("").trim();
                zone.addContent(res);
            }
//            newData.addContent(newLine);
            newData.addContent(enaOCR);
        }

        //Criando o documento XML (montado)
        doc = new Document();
        doc.setRootElement(newData);

        //Imptrimindo o XML
        xout = new XMLOutputter(Format.getPrettyFormat());
//        writer = new FileWriter("dataOCR.xml");
//        xout.output(doc, System.out);
//        xout.output(doc, writer);
//            writer.flush();
//            writer.close();
            return xout.outputString(doc);
    }

    public String [] changeElement(int size, ArrayList<String> option) throws JDOMException, IOException{

        newZoneDetailed = new String[size];
        for(int i=0; i<size;i++){
            newZoneDetailed[i]=option.get(i).toString();
        }

        return newZoneDetailed;

    }

    public void saveXML(String absolutePathFile) throws IOException{

//        writer = new FileWriter("dataOCR.xml");
        writer = new FileWriter(absolutePathFile+".xml");
        xout.output(doc, writer);
            writer.flush();
            writer.close();
    }

    public void saveTXT(String absolutePathFile, String output) throws IOException{

        writer = new FileWriter(absolutePathFile+".txt");
        writer.write(output);
            writer.flush();
            writer.close();
    }

    public void readXML() throws JDOMException, IOException{
        //Aqui informa o nome do arquivo XML.
        File f = new File("data/elements.xml");
        typeElementsXML = new ArrayList<String>();
        elementsSimplesXML = new ArrayList<String>();
        elementsCompostoXML = new ArrayList<String>();
        int counter =0;

        //Criamos uma classe SAXBuilder que vai processar o XML4
        SAXBuilder sb = new SAXBuilder();

        //Este documento agora possui toda a estrutura do arquivo.
        Document d = sb.build(f);

        //Recuperamos o elemento root
        Element enaOCR = d.getRootElement();

        //Recuperamos os elementos filhos (children)
        List elements = enaOCR.getChildren();
        Iterator i = elements.iterator();
        //Iteramos com os elementos filhos, e filhos dos filhos
        while (i.hasNext()) {
           Element element = (Element) i.next();

          if(counter==0){
           typeElementsXML.add(element.getAttributeValue("nomeTipo"));
           elementsSimplesXML.add(element.getChildText("nome1"));
           elementsSimplesXML.add(element.getChildText("nome2"));
           elementsSimplesXML.add(element.getChildText("nome3"));
           elementsSimplesXML.add(element.getChildText("nome4"));
           elementsSimplesXML.add(element.getChildText("nome5"));
           elementsSimplesXML.add(element.getChildText("nome6"));
           elementsSimplesXML.add(element.getChildText("nome7"));
           elementsSimplesXML.add(element.getChildText("nome8"));
           elementsSimplesXML.add(element.getChildText("nome9"));
           elementsSimplesXML.add(element.getChildText("nome10"));
           elementsSimplesXML.add(element.getChildText("nome11"));
           elementsSimplesXML.add(element.getChildText("nome12"));
           elementsSimplesXML.add(element.getChildText("nome13"));
           elementsSimplesXML.add(element.getChildText("nome14"));
           elementsSimplesXML.add(element.getChildText("nome15"));
          }
          else{
           typeElementsXML.add(element.getAttributeValue("nomeTipo"));
           elementsCompostoXML.add(element.getChildText("nome1"));
           elementsCompostoXML.add(element.getChildText("nome2"));
           elementsCompostoXML.add(element.getChildText("nome3"));
           elementsCompostoXML.add(element.getChildText("nome4"));
           elementsCompostoXML.add(element.getChildText("nome5"));
           elementsCompostoXML.add(element.getChildText("nome6"));
           elementsCompostoXML.add(element.getChildText("nome7"));
           elementsCompostoXML.add(element.getChildText("nome8"));
           elementsCompostoXML.add(element.getChildText("nome9"));
           elementsCompostoXML.add(element.getChildText("nome10"));
           elementsCompostoXML.add(element.getChildText("nome11"));
           elementsCompostoXML.add(element.getChildText("nome12"));
           elementsCompostoXML.add(element.getChildText("nome13"));
           elementsCompostoXML.add(element.getChildText("nome14"));
           elementsCompostoXML.add(element.getChildText("nome15"));
           elementsCompostoXML.add(element.getChildText("nome16"));
           elementsCompostoXML.add(element.getChildText("nome17"));
           elementsCompostoXML.add(element.getChildText("nome18"));
           elementsCompostoXML.add(element.getChildText("nome19"));
           elementsCompostoXML.add(element.getChildText("nome20"));
           elementsCompostoXML.add(element.getChildText("nome21"));
           elementsCompostoXML.add(element.getChildText("nome22"));
           elementsCompostoXML.add(element.getChildText("nome23"));
           elementsCompostoXML.add(element.getChildText("nome24"));
           elementsCompostoXML.add(element.getChildText("nome25"));
           elementsCompostoXML.add(element.getChildText("nome26"));
           elementsCompostoXML.add(element.getChildText("nome27"));
           elementsCompostoXML.add(element.getChildText("nome28"));
           elementsCompostoXML.add(element.getChildText("nome29"));
           elementsCompostoXML.add(element.getChildText("nome30"));
           elementsCompostoXML.add(element.getChildText("nome31"));
          }
           counter++;
        }
    }

    public ArrayList<String> getTypeElementsXML(){
        return typeElementsXML;
    }

    public ArrayList<String> getSimpleElementsXML(){
        return elementsSimplesXML;
    }

    public ArrayList<String> getCompostoElementsXML(){
        return elementsCompostoXML;
    }    
}
