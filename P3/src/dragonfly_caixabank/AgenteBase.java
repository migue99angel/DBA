package dragonfly_caixabank;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import YellowPages.YellowPages;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;


public abstract class AgenteBase extends IntegratedAgent {
    TTYControlPanel myControlPanel;
    String receiver;
    YellowPages yp = new YellowPages();
    
    ACLMessage in = new ACLMessage();
    ACLMessage out = new ACLMessage();
    
    protected String myService, myWorldManager, myConvId, myValue, myAction, myWMProtocol, mySphinxConvId;
    protected ArrayList<String> tiendas;
    
    @Override
    public void setup() {
        _identitymanager = "Sphinx";
        super.setup();
        
        doCheckinPlatform();
        
        // Description of my group
        myService = "Analytics group CaixaBank";

        receiver = this.whoLarvaAgent();
        myControlPanel = new TTYControlPanel(getAID());
        _exitRequested = false;
    }
    
    @Override
    public void plainExecute() {       
        loginSphinx();
    }
    
    @Override
    protected void takeDown() {
        doCheckoutPlatform();

        super.takeDown();
    }
    
    @Override
    protected String whoLarvaAgent() {
        return "CaixaBank";
    }
    
    protected void loginSphinx() {
        
        Info("Requesting checkin to " + _identitymanager + " from agent " + getAID());
        
        enviarMensaje(_identitymanager, ACLMessage.QUERY_IF, "ANALYTICS", "", "", true);
        
        in = this.blockingReceive();
        
        if(in.getPerformative() != ACLMessage.CONFIRM) {
            Info("Error en QUERY_IF de Sphinx de Agente " + getAID());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));
            
            abortSession();
        } else {
            out = new ACLMessage();
            out = in.createReply();
            out.setContent("");
            out.setEncoding(_myCardID.getCardID());
            out.setPerformative(ACLMessage.SUBSCRIBE);
            this.send(out);
            
            in = blockingReceive();
            
            if(in.getPerformative() == ACLMessage.CONFIRM || (in.getPerformative() == ACLMessage.INFORM && in.getContent().contains("ok"))) {
                Info("SUBSCRIBE OK de Agente " + getAID());
                
                mySphinxConvId = in.getConversationId();
                
                out = new ACLMessage();
                out = in.createReply();
                out.setContent("");
                out.setPerformative(ACLMessage.QUERY_REF);
                this.send(out);

                in = blockingReceive();
                
                if(in.getPerformative() == ACLMessage.INFORM) {
                    Info("QUERY_REF OK de Agente " + getAID());
                    yp.updateYellowPages(in);
                    //System.out.println("\n" + yp.prettyPrint());
                                       
                    myWorldManager = yp.queryProvidersofService(myService).iterator().next();
                } else {
                    Info("Error en QUERY_REF de Sphinx de Agente " + getAID());
                    Info(in.getContent());
                    Info(Integer.toString(in.getPerformative()));

                    abortSession();
                }
            } else {
                Info("Error en SUBSCRIBE de Sphinx de Agente " + getAID());
                Info(in.getContent());
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            }
        }
    }
    
    protected abstract void loginWorldManager();
    
    protected void logout() {
        Info("Requesting logout to " + _identitymanager);
        
        enviarMensaje(_identitymanager, ACLMessage.CANCEL, "ANALYTICS", "", "", false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en CANCEL de Sphinx de Agente " + getAID());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));
            
            abortSession();
        } else {
            Info("Logout OK de Agente " + getAID());
        }
    }
    
    protected void enviarMensaje(String receiver, int performative, String protocol, String content, String conversationID, boolean cardID) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver, AID.ISLOCALNAME));
        out.setPerformative(performative);
        out.setProtocol(protocol);
        out.setContent(content);
        
        if(conversationID != "") {
            out.setConversationId(conversationID);
        }
        
        if(cardID) {
            out.setEncoding(_myCardID.getCardID());
        }
        
        this.send(out);
    }
}
