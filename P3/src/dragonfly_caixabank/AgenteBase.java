package dragonfly_caixabank;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonParser;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;


public class AgenteBase extends IntegratedAgent {
    TTYControlPanel myControlPanel;
    String receiver;
    YellowPages yp = new YellowPages();
    
    ACLMessage in = new ACLMessage();
    ACLMessage out = new ACLMessage();
    
    protected String myService, myWorldManager, myConvId;
    protected ArrayList<String> agentesLanzados;
    
    
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
        loginWorldManager();
        
        logout();
        
        takeDown();
        _exitRequested = true;
    }
    
    @Override
    protected void takeDown() {
        doCheckoutPlatform();

        super.takeDown();
    }
    
    protected String whoLarvaAgent() {
        return "CaixaBank";
    }
    
    protected void loginSphinx() {
        
        Info("Requesting checkin to " + _identitymanager);
        out.setSender(getAID());
        out.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        out.setProtocol("ANALYTICS");
        out.setContent("");
        out.setEncoding(_myCardID.getCardID());
        out.setPerformative(ACLMessage.QUERY_IF);
        this.send(out);
        
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
                Info("SUBSCRIBE OK");
                myConvId = in.getConversationId();
                
                out = new ACLMessage();
                out = in.createReply();
                out.setContent("");
                out.setPerformative(ACLMessage.QUERY_REF);
                this.send(out);

                in = blockingReceive();
                
                if(in.getPerformative() == ACLMessage.INFORM) {
                    Info("QUERY_REF OK");
                    yp.updateYellowPages(in);
                    //System.out.println("\n" + yp.prettyPrint());
                    //Info(yp.queryProvidersofService("shop").toString());
                    
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
    
    protected void loginWorldManager() {
        Info("Login Controlador");
        
        out = new ACLMessage();
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));       
        out.setContent(new JsonObject().add("problem", "Playground1").toString());
        out.setConversationId(myConvId);
        out.setProtocol("ANALYTICS");
        out.setPerformative(ACLMessage.SUBSCRIBE);
        this.send(out);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en SUBSCRIBE de WM");
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("SUBSCRIBE WM OK");
        }
    }
    
    
    protected void logout() {
        Info("Requesting logout to " + _identitymanager);
        
        out = new ACLMessage();
        
        out.setSender(getAID());
        out.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        out.setPerformative(ACLMessage.CANCEL);
        out.setProtocol("ANALYTICS");
        out.setContent("");
        this.send(out);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en CANCEL de Sphinx");
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));
            
            abortSession();
        } else {
            Info("Logout OK");
        }
    }
}
