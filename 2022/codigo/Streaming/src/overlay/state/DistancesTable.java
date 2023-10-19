package overlay.state;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistancesTable {
    private Map<String, NodeLink> table;

    // tabela de distâncias de vetores, representado como um Map em que a chave é o nome do nodo, e o valor é a ligação ao nodo da chave
    public DistancesTable(){
        this.table = new HashMap<>();
    }

    public Map<String, NodeLink> getTable(){
        return this.table;
    }

    public void removeLink(String dest){
        this.table.remove(dest);
    }

    public void addLink(String dest, NodeLink newLink){
        this.table.put(dest, newLink);
    }

    public void addLink(String dest, String viaNode, InetAddress viaInterface, long cost){
        NodeLink link = new NodeLink(dest, viaNode, viaInterface, cost);
        this.table.put(dest, link);
    }

    public NodeLink getLinkTo(String key){
        return this.table.get(key);
    }

    // determina que nodo de uma lista é que está mais próxima, segunda a tabela (usada para determinar o servidor mais próximo)
    public NodeLink getClosestFromList(List<String> list){
        boolean initial = true;
        NodeLink min = new NodeLink();

        for(String entry: list){
            if (this.table.containsKey(entry)){
                NodeLink tmp = this.table.get(entry);
                
                if (initial){
                    initial = false;
                    min = tmp;
                }
                else if (tmp.getCost() < min.getCost())
                    min = tmp;
            }
        }

        return min;
    }

    // verifica se o estado da ligação atual mudou ou se surgiu uma melhor
    public boolean isLinkModified(String me, String key, NodeLink newLink){
        if (me.equals(key))
            return false;

        if (!table.containsKey(key))
            return true;

        NodeLink oldLink = table.get(key);
        if (oldLink.getViaNode().equals(newLink.getViaNode())){
            Long diff = Math.abs(oldLink.getCost() - newLink.getCost());
            double diffPercentage = diff / (oldLink.getCost() * 1.0);

            if (diffPercentage > 0.2)
                return true;
        }
        else {
            if (oldLink.getCost() > newLink.getCost())
                return true;
        }

        return false;
    }

    // remove as ligações dependentes de um nodo que se fechou, e devolve uma lista com os nomes dos nodos que foram removidos da tabela (exceto o nodo que se desligou)
    public List<String> handleClosedNode(String key){
        List<String> res = new ArrayList<>();

        if (this.table.containsKey(key))
            this.table.remove(key);
        
        for(Map.Entry<String, NodeLink> entry: this.table.entrySet())
            if (entry.getValue().getViaNode().equals(key))
                res.add(entry.getKey());

        for(String node: res)
            this.table.remove(node);

        return res;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        for(Map.Entry<String, NodeLink> entry: this.table.entrySet()){
            sb.append(entry.getValue().toString());
        }

        return sb.toString();
    }
}
