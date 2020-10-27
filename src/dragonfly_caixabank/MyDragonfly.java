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
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;



public class MyDragonfly extends IntegratedAgent{
    TTYControlPanel myControlPanel;
    String receiver;
    int alive;
    float angular;
    float fixedAngular;
    float compass;
    boolean onTarget = false;
    int alturaDron;
    int alturaSuelo;
    int energia = 1000;
    float distance;
    int numSensores;
    boolean alineado = false;
    ArrayList<Integer> visual = new ArrayList<>();
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
        out.setSender(getAID());
        out.addReceiver(new AID(receiver,AID.ISLOCALNAME));
        
        
        //Hacemos el login
        JsonObject jsonObjIn = new JsonObject();
        jsonObjIn.add("command", "login");
        jsonObjIn.add("world", "World1");

        JsonArray toContent = new JsonArray();
        toContent.add("alive");
        toContent.add("gps");
        toContent.add("distance");
        //toContent.add("ontarget");
        toContent.add("altimeter");
        toContent.add("compass");
        toContent.add("visual");
        toContent.add("lidar");
        toContent.add("angular");
        
        this.numSensores = toContent.size();
       
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
        while(!onTarget && this.alive == 1)
        {
            System.out.println(this.alive);
            System.out.println(this.compass);
            System.out.println(this.angular);
            
            myControlPanel.feedData(in,width , height, maxheight);
            myControlPanel.fancyShow();
            
            Info("Lectura");
            Info(in.getContent());

            //Ejecutamos una acción
            jsonObjIn = new JsonObject();
            jsonObjIn.add("command", "execute");
           

            jsonObjIn.add("action", funcionHeuristica() );
                        
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
            System.out.println("On target " + this.onTarget);
        }
        //myControlPanel.close();
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
    
    String funcionHeuristica() {
        String eleccion = "";
        Integer maxAltura = Collections.max(this.visual);
        if (this.distance == 0){
            if (this.alturaDron > 5){
                eleccion = "moveD";
                this.energia -= 5;
            } else if (this.alturaDron > 0) {
                eleccion = "touchD";
                this.energia -= 1;
            } else {
                eleccion = "rescue";
                this.onTarget = true;
            }
        } 
        else if (this.energia < this.alturaDron/5 * 5 *(this.numSensores + 5) +100) {
            if (this.alturaDron > 5) {
                eleccion = "moveD";
                this.energia -= 5;
            } else if (this.alturaDron > 0){
                eleccion = "touchD";
                this.energia -= 1;
            }else {
                eleccion = "recharge";
                this.energia = 1000;
            }
        }
        else if (this.alturaDron + this.alturaSuelo <= maxAltura ) {
            eleccion = "moveUP";
            this.energia -= 5;
        } else { // 0 45 90 135 180 -45 -90 -135
            if (this.angular == this.compass || (this.fixedAngular == this.compass && !this.alineado) ) {
                eleccion = "moveF";
                this.energia -= 1;
            }
            else {
                eleccion = "rotateR";
                this.energia -= 1;
            }   
        }
        
        return eleccion;
    }
    
    void lecturaSensores(JsonObject jsonObjOut)
    {
        this.energia -= (1 * this.numSensores);
        JsonArray aux = jsonObjOut.get("details").asObject().get("perceptions").asArray();
        
        for(int i = 0; i < aux.size(); i++ )
        {
            switch(aux.get(i).asObject().get("sensor").asString())
            {
                case "alive":
                   this.alive = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                   break;
                case "angular":
                    this.angular = aux.get(i).asObject().get("data").asArray().get(0).asFloat();
                    
                    if (this.angular%45 != 0.0f){
                    
                        if (this.angular < 0.0f) {
                            if (this.angular < -90.0f) {
                                this.fixedAngular = -135.0f;
                            } else {
                                this.fixedAngular = -45.0f;
                            }
                        } else {
                            if (this.angular < 90.0f) {
                               this.fixedAngular = 45f;
                            } else {
                                this.fixedAngular = 135f;
                            }
                        }
                    } else {
                        this.alineado = true;
                    }
                    
                                        
                   break;
                case "compass":
                    this.compass = aux.get(i).asObject().get("data").asArray().get(0).asFloat();
                   break;
                /*case "ontarget":
                    this.onTarget = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                   break;*/
                case "visual":
                    this.visual.clear();
                    JsonArray array_aux = aux.get(i).asObject().get("data").asArray();
                    //Nos quedamos con las casillas adyacentes al agente
                    for (int j=2; j<= 4; j++) {
                        for (int k=2; k<= 4; k++) {
                            this.visual.add(array_aux.asArray().get(j).asArray().get(k).asInt());
                        }     
                    }
                    // La altura es la posicion central de la matriz
                    this.alturaSuelo = this.visual.get(4);
                    break;
                case "altimeter":
                    this.alturaDron = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                    break;
                case "distance":
                    this.distance = aux.get(i).asObject().get("data").asArray().get(0).asFloat();
                    break;
            }
        }
        
    }
}
