package dragonfly_caixabank;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonParser;
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
        String mysensors[] = {"alive","gps"};
        JsonArray toContent = new JsonArray();
        toContent.add("alive");
        toContent.add("gps");
        obj.add("attach", toContent);
        
        out.setContent(obj.toString());
        this.sendServer(out);
        
        ACLMessage in = this.blockingReceive();

        JsonObject jsonObj = new JsonObject(in.getContent());
        
        obj = new JsonObject();
        obj.add("command", "read");
        obj.add("key", jsonObj.get("key"));
        
        out.setContent(answer);
        this.sendServer(out);
        /*
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
