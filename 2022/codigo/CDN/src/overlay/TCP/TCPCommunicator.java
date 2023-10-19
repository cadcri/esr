package overlay.TCP;

import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;

import overlay.bootstrapper.BStrapper;
import overlay.state.NodeLink;
import overlay.state.NodeState;


public class TCPCommunicator extends Thread{
    private NodeState state;
    private InetAddress neighbor;
    private int behaviour;
    
    private Object extraInfo;

    public static final int HELLO = 1;
    public static final int HELLO_SERVER = 2;
    public static final int PROBE_INITIAL = 4;
    public static final int PROBE_REGULAR = 5;
    public static final int SEND_NEW_LINK = 6;
    public static final int SEND_ROUTES = 7;
    public static final int INIT_MONITORING = 8;
    public static final int MONITORING = 9;
    public static final int OPEN_STREAM_CLIENT = 10;
    public static final int ASK_STREAMING = 11;
    public static final int REDIRECT_ASK_STREAMING = 12;
    public static final int OPEN_UDP_MIDDLEMAN = 14;
    public static final int ACK_OPEN_UDP_MIDDLEMAN = 15;
    public static final int CLOSED_NODE = 16;
    public static final int REQUEST_LINK = 17;
    public static final int PAUSE_STREAM_CLIENT = 18;
    public static final int PAUSE_STREAMING = 19;
    public static final int CANCEL_STREAM_CLIENT = 20;
    public static final int CANCEL_STREAM = 21;
    public static final int END_STREAM_CLIENT = 22;
    public static final int END_STREAM = 23;
    public static final int SEND_NEW_LINK_FIXER = 24;
    public static final int FIX_STREAM = 25;
    public static final int ACK_FIX_STREAM = 26;


    public TCPCommunicator(NodeState state, InetAddress neighbor, int behaviour){
        this.state = state;
        this.neighbor = neighbor;
        this.behaviour = behaviour;
    }

    public TCPCommunicator(NodeState state, InetAddress neighbor, int behaviour, Object extraInfo){
        this.state = state;
        this.neighbor = neighbor;
        this.behaviour = behaviour;
        this.extraInfo = extraInfo;
    }

    public void run(){
        try {
            Socket socket = new Socket(this.neighbor, TCPHandler.PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            TCPMessageSender sender = new TCPMessageSender(out);

            switch(this.behaviour){
                case HELLO:
                    sender.hello(); break;

                case HELLO_SERVER:
                    sender.helloServer(); break;

                case PROBE_INITIAL:
                    sender.probe(true, this.state); break;

                case PROBE_REGULAR:
                    sender.probe(false, this.state); break;

                case SEND_NEW_LINK:
                    String dest = (String) extraInfo;
                    NodeLink link = this.state.getLinkTo(dest);

                    sender.sendNewLink(null, link, this.state.getSelf(), false);
                    break;
                
                case SEND_ROUTES:
                    sender.sendRoutes(this.state, (String) extraInfo); break;

                case INIT_MONITORING:
                    sender.sendInitialMonitoringMessage(this.state); break;

                case MONITORING:
                    String[] nodesVisited = (String[]) extraInfo;
                    sender.sendMonitoringMessage(this.state, nodesVisited); break;

                case OPEN_STREAM_CLIENT:
                    sender.streamClient(); break;

                case ASK_STREAMING:
                    String fromServer = (String) extraInfo;
                    sender.sendAskStreaming(this.state, fromServer); 
                    break;

                case REDIRECT_ASK_STREAMING:
                    String[] args = (String[]) extraInfo;
                    sender.sendAskStreaming(args); 
                    break;

                case OPEN_UDP_MIDDLEMAN:
                    String[] nodesInfo2 = (String[]) extraInfo;
                    String[] visited2 = new String[nodesInfo2.length - 1];
                    for(int i = 0; i < nodesInfo2.length - 1; i++){
                        visited2[i] = nodesInfo2[i];
                    }
                    String newDest2 = nodesInfo2[nodesInfo2.length - 1];

                    sender.sendOpenUDPMiddleManSignal(visited2, newDest2); 
                    break;

                case ACK_OPEN_UDP_MIDDLEMAN:
                    String[] newArgs = (String[]) extraInfo;

                    sender.ackOpenUDPMiddleManSignal(newArgs); 
                    break;
                
                case CLOSED_NODE:
                    String closedNode = (String) extraInfo;
                    sender.warnNodeClosed(closedNode);
                    break;

                case REQUEST_LINK:
                    String linkTo = (String) extraInfo;
                    sender.requestLink(linkTo);
                    break;

                case PAUSE_STREAM_CLIENT:
                    sender.pauseStreamClient(); break;

                case PAUSE_STREAMING:
                    String[] stream = (String[]) extraInfo;
                    sender.pauseStream(stream); break;

                case CANCEL_STREAM_CLIENT:
                    sender.cancelStreamClient(); break;

                case CANCEL_STREAM:
                    String[] stream2 = (String[]) extraInfo;
                    sender.cancelStream(stream2); break;

                case END_STREAM_CLIENT:
                    String[] stream3 = (String[]) extraInfo;
                    sender.endStreamClient(stream3); break;

                case END_STREAM:
                    String[] stream4 = (String[]) extraInfo;
                    sender.endStream(stream4); break;

                case SEND_NEW_LINK_FIXER:
                    String dest2 = (String) extraInfo;
                    NodeLink link2 = this.state.getLinkTo(dest2);

                    sender.sendNewLink(dest2, link2, this.state.getSelf(), true);
                    break;

                case FIX_STREAM:
                    String[] args2 = (String[]) extraInfo;
                    sender.fixStream(args2);
                    break;

                case ACK_FIX_STREAM:
                    String[] args3 = (String[]) extraInfo;
                    sender.ackFixStream(args3);
                    break;
            }

            socket.close();

        } catch (Exception e) {
            warnNodeClosed(this.neighbor);
            System.out.println("Connection refused with " + neighbor);
        }
    }

    public void warnNodeClosed(InetAddress closedNodeIP){
        try{
            InetAddress bstrapper = this.state.getBstrapperIP();
            if (bstrapper == null){
                List<InetAddress> myIPs = this.state.getSelfIPs();
                bstrapper = myIPs.get(0);
            }
            Socket socket = new Socket(bstrapper, BStrapper.PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            TCPMessageSender sender = new TCPMessageSender(out);

            String closedNode = this.state.findAdjNodeFromAddress(closedNodeIP);
            sender.warnNodeClosed(closedNode);
            sender.end();
        }
        catch (Exception e){

        }
    }
}
