package dragonfly_caixabank;

import IntegratedAgent.IntegratedAgent;
import com.eclipsesource.json.Json;
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
        
        //Hacemos el login
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
        
        //Recibimos la key
        ACLMessage in = this.blockingReceive();
        Info("Respuesta");
        Info(in.getContent());
        JsonObject jsonObj = new JsonObject();
        jsonObj = Json.parse(in.getContent().toString()).asObject();

        //Hacemos la lectura de los sensores
        obj = new JsonObject();
        obj.add("command", "read");
        obj.add("key", jsonObj.get("key"));
        
        out.setContent(obj.toString());
        this.sendServer(out);
        
        //Recibimos los valores de los sensores
        ACLMessage in2 = this.blockingReceive();
        Info("Lectura");
        Info(in2.getContent());
        
        //Ejecutamos una acción
        obj = new JsonObject();
        obj.add("command", "execute");
        obj.add("action", "rotateR");
        obj.add("key", jsonObj.get("key"));
        
        out.setContent(obj.toString());
        this.sendServer(out);
        
        //Recibimos respuesta de la ejecución de la acción
        ACLMessage in3 = this.blockingReceive();
        Info("Ejecución");
        Info(in3.getContent());

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
