//package src;


public class main {
    public static void main(String[] args){
        nodeManager manager = new nodeManager();
        manager.parseNodes("/Users/rkeat/Desktop/Universidade/1anoMestrado/1semestre/ESR-TP/2023/test.imn");
        System.out.println(manager.nodes.size());
        for (node nodo : manager.nodes.values()) {
            System.out.println(nodo.neighbors.toString());
        }
    }
}
