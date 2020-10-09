package dragonfly_caixabank;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyDragonfly extends IntegratedAgent{
    
    String receiver;
    @Override
    public void setup() {
        super.setup();
        

        doCheckinPlatform();
        doCheckinLARVA();
        receiver = this.whoLarvaAgent();
        
        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        
        
        ACLMessage out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver,AID.ISLOCALNAME));
        JsonObject obj = new JsonObject();
        obj.add("command", "login");
        obj.add("world", "BasePlayground");
        obj.add("attach", "alive");
        out.setContent(obj.toString());
        
        this.sendServer(out);
        
        System.out.println(obj.toString());
        
        /*ACLMessage in = this.blockingReceive();
        String answer = in.getContent();
        Info("Respuesta"+answer);
        
        //ESTO ES DEL HACKATON, QUE RESPONDIAMOS DE VUELTA, CON LO DE ARRIBA DEBERIA DE SER SUFICIENTE PARA RECIBIR EL JSON RESPUESTA
        
        String reply = new StringBuilder(answer).reverse().toString();
        out = in.createReply();
        out.setContent(reply);
        this.send(out);*/

     
        _exitRequested = true;
    }
        


    @Override
    protected void takeDown() {
        doCheckoutPlatform();
        doCheckoutLARVA();
        super.takeDown();
    }

    protected String whoLarvaAgent() {
        return "WorldManager";
    }
    
}
