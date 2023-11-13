//package src;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
             if (!filePath.contains(".imn"))
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
            
            Pattern pattern = Pattern.compile("nodes ");
            
            
            Node nodeTemp=null;
            String nameTemp="";

            for (int lineNum=0; lineNum<contentSplited.length; lineNum++){
                String line = contentSplited[lineNum];
                //System.out.println(line);
                if(line.contains("node")){
                    nodeTemp = new Node();
                    //we classify the nodes based on their name
                    //i.e if the name is rp then it will be the rendevouz point
                    //or if the name is client then it will be the client
                    String[] lineSplitted = line.split(" ");
                    nameTemp = lineSplitted[1];
                    
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
                    nodeTemp.setNodeState(Node.state.on);
                }

                else if(line.contains("interface eth0")){
                    String ipLine = contentSplited[lineNum+1];
                    //System.out.println(ipLine);
                    String[] lineTemp = ipLine.split(" ");
                    //Para remover as interfaces
                    nodeTemp.setNodeIP(lineTemp[3].split("/")[0]);
                    nodes.put(nameTemp, nodeTemp);
                }

                else if(line.contains("link")){
                   String newLine = contentSplited[lineNum+1];
                    
                    Pattern pattern2 = Pattern.compile("\\{(.*?)\\}");

                    Matcher matcher = pattern2.matcher(newLine);
                    if(matcher.find()){
                        String nodes = matcher.group(1);
                        String n1 = nodes.split(" ")[0];
                        String n2 = nodes.split(" ")[1];
                        //Partindo de que os nodos já estão todos criados por esta altura, então
                        //basta adicionar os vizinhos
                        this.nodes.get(n1).addNeighbour(this.nodes.get(n2));
                        this.nodes.get(n2).addNeighbour(this.nodes.get(n1));
                        
                    }
                }
            }
        }   
        catch(Exception e){
            System.out.println("Error loading overlay file");
            nodes = null;
        }
    }
}
