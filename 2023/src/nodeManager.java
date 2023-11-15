//package src;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class nodeManager{
    
    //Hashmap name, Node
    public static HashMap<String, Node> nodes = new HashMap<String, Node>();
    public static String rpName = "";

    public nodeManager(){
    
    }

    public void parseNodes(String filePath){
        try{
             if (!filePath.endsWith(".imn"))
                filePath=filePath+".imn";
            String content="";
            Path path = Path.of(filePath);
            try{
                content = Files.readString(path);
            }
            catch(Exception e){
                System.out.println("Does the overlay file exist or the correct path specified?");
            }

            //Path path = FileSystems.getDefault().getPath("logs", nameOfFile);
            String[] contentSplited = content.split("\n");
            
            
            Node nodeTemp=null;
            String nameTemp="";
            //Key = node name values = neighbourname, ip
            HashMap <String, HashMap<String,String>> links = new HashMap<>();
            //Interface name, interface IP
            HashMap <String, String> interfaces=new HashMap<>();

            HashMap <String, Node> nodesTemp = new HashMap<>();
            Boolean finished = false;

            for (int lineNum=0; lineNum<contentSplited.length; lineNum++){
                String line = contentSplited[lineNum];
                //System.out.println(line);
                if(line.startsWith("node")){
                    //we classify the nodes based on their name
                    //i.e if the name is rp then it will be the rendevouz point
                    //or if the name is client then it will be the client
                    String[] lineSplitted = line.split(" ");
                    nameTemp = lineSplitted[1];

                    nodeTemp = new Node();
                    interfaces = new HashMap<>();
                    links.put(nameTemp, new HashMap<String,String>());
                    
                    
                }

                else if(line.contains("hostname")){
                    String type = line.split(" ")[1];
                    //System.out.println(type);
                     if(type.contains("rp")){
                        nodeTemp.setNodeType(Node.type.rp);
                        rpName=nameTemp;
                    }
                    else if(type.contains("client")){
                        nodeTemp.setNodeType(Node.type.client);
                    }
                    else{
                        nodeTemp.setNodeType(Node.type.Node);
                    }
                    nodeTemp.setNodeName(nameTemp);
                }

                Pattern interfacePattern = Pattern.compile("interface (eth[0-9]+)");
                Matcher interfaceMatcher = interfacePattern.matcher(line);

                if (interfaceMatcher.find()){
                    String interfaceTemp = interfaceMatcher.group(1);
                    String ipLine = contentSplited[lineNum+1];
                    if (interfaceTemp.equals("eth0")){
                        nodeTemp.setNodeIP(ipLine.split(" ")[3].split("/")[0]);
                    }
                    
                    //Adiciona a interface e o ip da mesma ao hashmap
                    interfaces.put(interfaceTemp, ipLine.split(" ")[3].split("/")[0]);
                }

                Pattern interfacePeer = Pattern.compile("interface-peer \\{(.*)\\}");
                Matcher interfacePeerMatcher = interfacePeer.matcher(line);
                while(interfacePeerMatcher.find()){
                    
                    String[] lineTemp = interfacePeerMatcher.group(1).split(" ");
                    
                    //interface node
                    String interfaceNameTemp = lineTemp[0];
                    String neighbournNameTemp = lineTemp[1];
                
                    
                    //We now have the interface IP
                    String interfaceIpToNode = interfaces.get(interfaceNameTemp);
                
                    //Lets add it to this nodes neighbours
                    HashMap<String,String> temp = links.get(nameTemp);
                    temp.put(neighbournNameTemp, interfaceIpToNode);
                    finished=true;
                }

                if (finished){
                    nodesTemp.put(nodeTemp.getNodeName(), nodeTemp);
                    finished=false;
                    continue;
                }
                

            }

            for(String nodeName : links.keySet()){
                Node node = nodesTemp.get(nodeName);          
                for(String neighbourName : links.get(nodeName).keySet()){
                    Node neighbour = nodesTemp.get(neighbourName).clone();
                    String neighbourIP = links.get(nodeName).get(neighbourName);
                    neighbour.setNodeIP(neighbourIP);
                    neighbour.addNeighbour(node);
                }
                nodes.put(nodeName, node);
            }
        }   
        catch(Exception e){
            System.out.println("Error loading overlay file");
            nodes = null;
        }
    }
}
