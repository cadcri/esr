//package src;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class nodeManager{
    
    //Hashmap name, node
    public HashMap<String, node> nodes = new HashMap<String, node>();

    public nodeManager(){
    
    }

    public void initializeNodes(String filePath){
        
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
            
            
            node nodeTemp=null;
            String nameTemp="";

            for (int lineNum=0; lineNum<contentSplited.length; lineNum++){
                String line = contentSplited[lineNum];
                if(line.contains("node")){
                    nodeTemp = new node();
                    //we classify the nodes based on their name
                    //i.e if the name is rp then it will be the rendevouz point
                    //or if the name is client then it will be the client
                    String[] lineSplitted = line.split(" ");
                    nameTemp = lineSplitted[1];
                    if(nameTemp.contains("rp")){
                        nodeTemp.setNodeType(node.type.rp);
                    }
                    else if(nameTemp.contains("client")){
                        nodeTemp.setNodeType(node.type.client);
                    }
                    else{
                        nodeTemp.setNodeType(node.type.node);
                    }
                    System.out.println(nameTemp);
                    nodeTemp.setNodeState(node.state.on);
                    
                }

                else if(line.contains("interface eth0")){
                    String ipLine = contentSplited[lineNum+1];
                    String[] lineTemp = ipLine.split(" ");
                    //Para remover a barras do ip
                    nodeTemp.setNodeIP(lineTemp[2].split("/")[0]);
                    nodes.put(nameTemp, nodeTemp);
                }

                // else if(line.contains("link")){
                
                //     // Create a matcher using the pattern
                //     Matcher matcher = pattern.matcher(contentSplited[lineNum+1]);

                //     // Find the first match
                //     boolean foundMatch = matcher.find();

                //     // If a match is found, get the matched group
                //     if (foundMatch) {
                //         System.out.println(foundMatch);
                //         String nodes = matcher.group(1);

                //         // Split the nodes string into an array
                //         String[] nodesArray = nodes.split(" ");

                //         // Get the n2 and n3 nodes
                //         String n0 = nodesArray[0];
                //         String n1 = nodesArray[1];

                //         this.nodes.get(n0).addNeighbour(this.nodes.get(n1));
                        
                //         this.nodes.get(n1).addNeighbour(this.nodes.get(n0));
                //     }
                // }
            }
    

        }   
        catch(Exception e){
            System.out.println("Error loading overlay file");
            nodes = null;
        }
    }
}
