package dragonfly_caixabank;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import YellowPages.YellowPages;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 * Clase que hereda de IntegratedAgent, contiene la funcionalidad básica que heredarán el resto de drones
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
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
    
    /**
     * Inicio de sesión en el IdentityManager. En el proceso nos devuelve el conversationID con Sphinx, así como
     * el WorldManager, disponible a través de las YellowPages.
     */
    protected void loginSphinx() {
        
        Info("Requesting checkin to " + _identitymanager + " from agent");
        
        enviarMensaje(_identitymanager, ACLMessage.QUERY_IF, "ANALYTICS", "", "", true);
        
        in = this.blockingReceive();
        
        if(in.getPerformative() != ACLMessage.CONFIRM) {
            Info("Error en QUERY_IF de Sphinx");
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
                Info("SUBSCRIBE OK de Agente");
                
                mySphinxConvId = in.getConversationId();
                
                out = new ACLMessage();
                out = in.createReply();
                out.setContent("");
                out.setPerformative(ACLMessage.QUERY_REF);
                this.send(out);

                in = blockingReceive();
                
                if(in.getPerformative() == ACLMessage.INFORM) {
                    Info("QUERY_REF OK de Agente");
                    yp.updateYellowPages(in);
                                       
                    myWorldManager = yp.queryProvidersofService(myService).iterator().next();
                } else {
                    Info("Error en QUERY_REF de Sphinx");
                    Info(in.getContent());
                    Info(Integer.toString(in.getPerformative()));

                    abortSession();
                }
            } else {
                Info("Error en SUBSCRIBE de Sphinx");
                Info(in.getContent());
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            }
        }
    }
    
    
    /**
     * Inicio de sesión en el WorldManager
     */
    protected abstract void loginWorldManager();
    
    
    /**
     * Cerrar la sesión del IdentityMaanger
     */
    protected void logout() {
        Info("Requesting logout to " + _identitymanager);
        
        enviarMensaje(_identitymanager, ACLMessage.CANCEL, "ANALYTICS", "", "", false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en CANCEL de Sphinx de Agente");
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));
            
            abortSession();
        } else {
            Info("Logout OK de Agente");
        }
    }
    
    /**
     * Envia un mensaje
     * @param receiver El receptor del mensaje
     * @param performative La performativa ACLMessage a usar en el mensaje
     * @param protocol El protocolo del mensaje
     * @param content El contenido del mensaje
     * @param conversationID El conversationID a usar en el envío del mensaje
     * @param cardID El cardID a usar en el envío del mensaje
     */
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
