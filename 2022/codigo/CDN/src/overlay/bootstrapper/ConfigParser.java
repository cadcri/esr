package overlay.bootstrapper;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import overlay.state.Graph;
import overlay.state.Vertex;

public class ConfigParser {
    private File file;
    private String bstrapperName;
 
    public ConfigParser(String filepath){
        this.file = new File(filepath);
    }

    public String getBootstrapperName(){
        return this.bstrapperName;
    }

    public Graph parseXML(){
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList nodeList = doc.getElementsByTagName("nodes");
            NodeList bStrapper = doc.getElementsByTagName("bootstrapper");
            NodeList nodes = doc.getElementsByTagName("node");

            Graph g = getAllOverlayNodes(nodeList);
            this.bstrapperName = getBStrapperName(bStrapper);
            readNodesAdjacents(nodes, g);
            g.setNodeState(bstrapperName, Vertex.ON);
            return g;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public Graph getAllOverlayNodes(NodeList nodeList) throws UnknownHostException{
        Map<String, Vertex> graph = new HashMap<>();
        Element tmp = (Element) nodeList.item(0);

        NodeList entries = tmp.getElementsByTagName("entry");
        for(int i = 0; i < entries.getLength(); i++){
            Element entry = (Element) tmp.getElementsByTagName("entry").item(i);

            String name = entry.getAttribute("n");
            List<InetAddress> ipList = getIPList(entry);
            Vertex v = new Vertex(name, ipList, Vertex.OFF);
            graph.put(name, v);
        }

        return new Graph(graph);
    }

    public List<InetAddress> getIPList(Element node) throws UnknownHostException{
        List<InetAddress> ipList = new ArrayList<>();

        NodeList addresses = node.getElementsByTagName("address");
            
        for(int i = 0; i < addresses.getLength(); i++){
            String ip = addresses.item(i).getTextContent();
            ipList.add(InetAddress.getByName(ip));
        }

        return ipList;
    }

    public String getBStrapperName(NodeList bStrapper){
        Element entry = (Element) bStrapper.item(0);
        return entry.getAttribute("name");
    }

    public void readNodesAdjacents(NodeList nodes, Graph g){
        for(int i = 0; i < nodes.getLength(); i++){
            Element node = (Element) nodes.item(i);
            String name = node.getAttribute("n");
            Map<String, List<InetAddress>> adjs = readNodeAdjacents(node, g);
            g.setAdjacentsInNode(name, adjs);
        }
    }

    public Map<String, List<InetAddress>> readNodeAdjacents(Element node, Graph g){
        try{
            Map<String, List<InetAddress>> adjs = new HashMap<>();
            NodeList adjacents = node.getElementsByTagName("adj");
            for(int i = 0; i < adjacents.getLength(); i++){
                String nodeName = adjacents.item(i).getTextContent();
                adjs.put(adjacents.item(i).getTextContent(), g.getNodeIPList(nodeName));
            }

            return adjs;
        }
        catch (Exception e){
            return null;
        }
    }
}
