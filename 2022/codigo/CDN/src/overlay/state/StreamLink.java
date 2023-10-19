package overlay.state;

import java.util.ArrayList;
import java.util.List;

public class StreamLink {
    private List<String> nodes;
    private int id;
    private boolean active;
    private boolean withChange;
    private String changeAt;
    private boolean changeAfterMe;

    public StreamLink(String[] path, String rcv, int id, boolean withChange, String changeAt, String me){
        this.id = id;
        this.active = true;
        this.withChange = withChange;
        this.changeAt = changeAt;
        this.nodes = new ArrayList<>();
        for(String node: path)
            this.nodes.add(node);
        this.nodes.add(rcv);
        setChangeAfterMe(me);
    }

    public StreamLink(String[] args, int id){
        this.id = id;
        this.nodes = new ArrayList<>();
        this.active = true;
        this.withChange = false;
        this.changeAfterMe = true;
        this.changeAt = "";
        for(String s: args)
            this.nodes.add(s);
    }

    public int getStreamID(){
        return this.id;
    }

    public List<String> getStream(){
        return this.nodes;
    }

    public boolean getActive(){
        return this.active;
    }
    
    public boolean getWithChange(){
        return this.withChange;
    }

    public String getChangeAt(){
        return this.changeAt;
    }

    public boolean getChangeAfterMe(){
        return this.changeAfterMe;
    }

    public void setStream(List<String> stream){
        this.nodes = stream;
    }

    public void setActive(boolean active){
        this.active = active;
    }

    public void setWithChange(boolean withChange){
        this.withChange = withChange;
    }

    public void setChangeAt(String node){
        this.changeAt = node;
    }

    public void setChangeAfterMe(String me){
        if(this.changeAfterMe == true){
            for(String node: this.nodes){
                if (me.equals(node)){
                    this.changeAfterMe = true;
                    break;
                }
                else if(node.equals(this.changeAt)){
                    this.changeAfterMe = false;
                    break;
                }
            }
        }
    }

    public String getServer(){
        return this.nodes.get(0);
    }

    public String getReceivingNode(){
        return this.nodes.get(this.nodes.size() - 1);
    }

    public String[] convertLinkToArgs(){
        String[] res = new String[nodes.size()];

        for(int i = 0; i < this.nodes.size(); i++)
            res[i] = this.nodes.get(i);

        return res;
    }

    public String findNextNode(String me, boolean order){
        String nextNode = "";

        for(int i = 0; i < this.nodes.size(); i++){
            if (me.equals(this.nodes.get(i))){
                if (order){
                    if (i - 1 >= 0)
                        nextNode = this.nodes.get(i - 1);
                }
                else{
                    if (i + 1 < this.nodes.size())
                        nextNode = this.nodes.get(i + 1);
                }
                break;
            }
        }

        if (nextNode.equals(""))
            return me;
        else
            return nextNode;
    }

    public boolean isNodeInStream(String node){
        boolean res = false;

        for(String key: this.nodes)
            if (key.equals(node)){
                res = true;
                break;
            }

        return res;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();

        sb.append("\t\tSTREAM ID: " + this.id + "\n");
        sb.append("\t\tRECEIVING STREAM: " + getReceivingNode() + "\n\t\tGOING THROUGH:");
        for(int i = 0; i < this.nodes.size() - 1; i++){
            sb.append(" " + this.nodes.get(i));
        }
        sb.append("\n");

        if (active)
            sb.append("\t\tACTIVE: TRUE\n");
        else
            sb.append("\t\tACTIVE: FALSE\n");

        if (withChange){
            sb.append("\t\tCHANGES OCURRED: TRUE\n");
            sb.append("\t\tCHANGE AT: " + this.changeAt + "\n");
            if (changeAfterMe)
                sb.append("\t\tCHANGE AFTER ME\n");
            else
                sb.append("\t\tCHANGE BEFORE ME\n");
        }
        else
            sb.append("\t\tCHANGE OCURRED: FALSE\n");

        return sb.toString();
    }
}
