package dragonfly_caixabank;

import ControlPanel.TTYControlPanel;
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
    TTYControlPanel myControlPanel;
    String receiver;
    int alive;
    @Override
    public void setup() {
        super.setup();
        
        doCheckinPlatform();
        doCheckinLARVA();
        receiver = this.whoLarvaAgent();
        myControlPanel = new TTYControlPanel(getAID());
        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        
        ACLMessage in;
        ACLMessage out = new ACLMessage();
        boolean onTarget = false;
        out.setSender(getAID());
        out.addReceiver(new AID(receiver,AID.ISLOCALNAME));
        
        
        //Hacemos el login
        JsonObject jsonObjIn = new JsonObject();
        jsonObjIn.add("command", "login");
        jsonObjIn.add("world", "Playground1");

        JsonArray toContent = new JsonArray();
        toContent.add("alive");
        toContent.add("gps");
        toContent.add("ontarget");
        toContent.add("altimeter");
        toContent.add("compass");
        toContent.add("visual");
        toContent.add("lidar");
       
        jsonObjIn.add("attach", toContent);
        
        out.setContent(jsonObjIn.toString());
        this.sendServer(out);
        
        //Recibimos la key
        in = this.blockingReceive();
        Info("Respuesta");
        Info(in.getContent());
        JsonObject jsonObjOut = new JsonObject();
        jsonObjOut = Json.parse(in.getContent().toString()).asObject();
        
        int width = jsonObjOut.get("width").asInt();
        int height = jsonObjOut.get("height").asInt();
        int maxheight = jsonObjOut.get("maxflight").asInt();
        String key = jsonObjOut.get("key").asString();
        
        //Hacemos la lectura de los sensores
        jsonObjIn = new JsonObject();
        jsonObjIn.add("command", "read");
        jsonObjIn.add("key", key);

        out = in.createReply();
        out.setContent(jsonObjIn.toString());
        this.sendServer(out);

        //Recibimos los valores de los sensores
        in = this.blockingReceive();
        jsonObjOut = Json.parse(in.getContent().toString()).asObject();
        lecturaSensores(jsonObjOut);
        //while(!onTarget && this.alive == 1)
        for(int i = 0; i < 10; i++)
        {
            System.out.println(this.alive);
            
            myControlPanel.feedData(in,width , height, maxheight);
            myControlPanel.fancyShow();
            
            Info("Lectura");
            Info(in.getContent());

            //Ejecutamos una acción
            jsonObjIn = new JsonObject();
            jsonObjIn.add("command", "execute");
     
            jsonObjIn.add("action", "rotateL");
            jsonObjIn.add("key", key);

            out = in.createReply();
            out.setContent(jsonObjIn.toString());
            
            this.sendServer(out);
            
            //Recibimos respuesta de la ejecución de la acción
            in = this.blockingReceive();
            Info("Ejecución");
            Info(in.getContent());
            
            //Hacemos la lectura de los sensores
            jsonObjIn = new JsonObject();
            jsonObjIn.add("command", "read");
            jsonObjIn.add("key", key);

            out = in.createReply();
            out.setContent(jsonObjIn.toString());
            this.sendServer(out);

            //Recibimos los valores de los sensores
            in = this.blockingReceive();
            jsonObjOut = Json.parse(in.getContent().toString()).asObject();
            lecturaSensores(jsonObjOut);
        }
        myControlPanel.close();
        //Logout
        jsonObjIn = new JsonObject();
        jsonObjIn.add("command", "logout");
        jsonObjIn.add("key", key);
        
        out = in.createReply();
        out.setContent(jsonObjIn.toString());
        this.sendServer(out);

        _exitRequested = true;
    }

    @Override
    protected void takeDown() {
        doCheckoutPlatform();
        doCheckoutLARVA();
        super.takeDown();
    }

    protected String whoLarvaAgent() {
        return "CaixaBank";
    }
    
    void lecturaSensores(JsonObject jsonObjOut)
    {
        JsonArray aux = jsonObjOut.get("details").asObject().get("perceptions").asArray();
        
        for(int i = 0; i < aux.size(); i++ )
        {
            switch(aux.get(i).asObject().get("sensor").asString())
            {
                case "alive":
                   this.alive = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                   break;
            }
        }
        
    }
}
