import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class main {
    

    //Criar um parser para o ficheiro de entrada imn
    //Este parser deve instanciar nodos com os respetivos tipos
    //TODO definir a constituição de um nodo
    
    public static void main(String[] args) {
        try {
            parse("test.imn");
        } catch(Exception e) {
            System.out.println("merda1");
        }
    }

    public static void parse(String nameOfFile) throws IOException{
        
        try{
            if (!nameOfFile.contains(".imn"))
                nameOfFile=nameOfFile+".imn";
            String content="";
            Path path = Path.of("../"+nameOfFile);
            try{
                content = Files.readString(path);
            }
            catch(Exception e){
                System.out.println("merda2");
            }

            //Path path = FileSystems.getDefault().getPath("logs", nameOfFile);
            String[] contentSplited = content.split("\n");
            String[] splitLine;
    
            for (String linha : contentSplited) {
                String trim = linha.trim();
                String replace = trim.replace(" {", "");
                splitLine = replace.split(" ", 2);
    
                switch (splitLine[0]) {
                    case "node" -> {
                        parseField("node", splitLine[1]);
                    }
                    case "type" -> {
                        parseField("type", splitLine[1]);
                    }
                    case "model" -> {
                        parseField("model", splitLine[1]);
                    }
                }
            }
            System.out.println("Files parsed.");
        }
        catch(Exception e){
            System.out.println("merda3");
        }
      
    }

    /**
     * Função que dá parse às casas em uma SmartCity
     * @param input String atual a ser lida das logs
     */
    public static void parseField(String field, String input){
        try {
        System.out.println(field + ": " + input);
        }
        catch(Exception e){
            System.out.println("merda4");
        }
    }

}
