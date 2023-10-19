package overlay.TCP;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import overlay.state.NodeState;
import overlay.state.Vertex;

public class TCPMonitor extends TimerTask{
    private NodeState state;

    public TCPMonitor(NodeState state){
        this.state = state;
    }

    @Override
    public void run(){
        Map<String, Integer> adjsState = this.state.getNodeAdjacentsState();
        Map<String, List<InetAddress>> adjs = this.state.getNodeAdjacents();

        for(Map.Entry<String, Integer> entry: adjsState.entrySet()){
            if (entry.getValue() == Vertex.ON){
                List<InetAddress> ips = adjs.get(entry.getKey());
                Thread client = new Thread(new TCPCommunicator(this.state, ips.get(0), TCPCommunicator.INIT_MONITORING));
                client.start();
                
                try {
                    client.join();
                } catch (Exception e) {}
            }
        }
    }
}
