import Nodes.*;
import Structs.*;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.util.HashMap;
import java.util.ArrayList;

public class XMLParser {

    public HashMap<String,Node> parse(String path) {

     HashMap<String,Node> nodes = new HashMap<String,Node>();

        try {
            File inputFile = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            org.w3c.dom.NodeList nList = doc.getElementsByTagName("node");
            //com.example.NodeList nList = doc.getElementsByTagName("node");
            System.out.println("----------------------------");

            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Node nNode = nList.item(temp);


                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                     Element eElement = (Element) nNode;

                     Node nodeTemp = new Node();
                     String nodeHostName = eElement.getAttribute("name");
                     ArrayList<String> gateways = new ArrayList<>();
                     for(int i=0; i<eElement.getElementsByTagName("gateway").getLength(); i++){
                          gateways.add(eElement.getElementsByTagName("gateway").item(i).getTextContent());
                     }
                     nodeTemp.setNodeName(nodeHostName);
                     String nodeType = eElement.getElementsByTagName("type").item(0).getTextContent().toString();
                     nodeTemp.setNodeType(nodeType);
                     nodeTemp.setGateways(gateways);
                     nodes.put(nodeHostName,nodeTemp);
                }
            }
          return nodes;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}