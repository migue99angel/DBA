package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Seeker extends Dron {
    protected static int alemanesDetectados = 0;
    protected JsonArray thermal = new JsonArray();
    protected JsonArray ruta = new JsonArray();
    protected int energy = 995;
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "SEEKER";
        myWMProtocol = "REGULAR";
        
        // Rellenamos el array con los sensores que queremos
        sensoresRequeridos.add("ALIVE");
        sensoresRequeridos.add("GPS");
        sensoresRequeridos.add("THERMALHQ");
        sensoresRequeridos.add("CHARGE");
        
    }
    
    public void comportamiento(){
        JsonObject aux = new JsonObject();
        //Recarga inicial
        recargar();
        
        leerSensores();
        Info ("He leido sensores");
        Info (posx + "," + posy + "," + posz + "");
        pedirRuta();
        
        //Comportamiento general
        for (int i=0; i < ruta.size() && alemanesDetectados < DRAGONFLY_CAIXABANK.alemanes; i++){
            switch(ruta.get(i).asObject().get("action").asString()){
                case "move":
                    aux = new JsonObject();
                    movimiento = ruta.get(i).asObject().get("value").asString();
                    aux.add("operation", movimiento);
                    enviarMensaje(myWorldManager, ACLMessage.REQUEST, "REGULAR", aux.toString(), myConvId, false);
                    
                    in = blockingReceive();
                    if (in.getPerformative() != ACLMessage.INFORM){
                        Info("Fallo al realizar " + movimiento);
                        Info(Integer.toString(in.getPerformative()));
                        Info(in.getContent());
                        abortSession();
                    }
                    break;
                case "read":
                    leerSensores();
                    break;
                case "recharge":
                    recargar();
                    break;
            }
        }

    }
    
    protected void pedirRuta(){
        Info("Pidiendo ruta");
        JsonObject aux = new JsonObject();
        aux.add("type",this.myValue);
        aux.add("cuadrante", this.cuadrante);
        aux.add("posx",this.posx);
        aux.add("posy",this.posy);
        aux.add("posz",this.posz);
        aux.add("energy",this.energy);
        aux.add("orientacion",this.orientacion);
        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0),ACLMessage.REQUEST,"REGULAR",aux.toString(),myConvId,false);
        
        in = blockingReceive();
        
        //Gestionar la ruta
        
        if (in.getPerformative() != ACLMessage.INFORM){
            Info("Error al recibir la ruta");
            abortSession();
        } else{
            this.ruta = Json.parse(in.getContent()).asArray();
            Info (in.getContent());
            Info (Integer.toString(this.ruta.size()));
            Info("Ruta recibida correctamente");
        }
    }
    
    protected void leerSensores(){
        JsonObject aux = new JsonObject();
        movimiento = "read";
        aux.add("operation", movimiento);
        enviarMensaje(myWorldManager, ACLMessage.QUERY_REF, "REGULAR", aux.toString(), myConvId, false);
        
        in = blockingReceive();
        if (in.getPerformative() != ACLMessage.INFORM){
            Info("Fallo al realizar " + movimiento);
            Info(Integer.toString(in.getPerformative()));
            Info(in.getContent());
            abortSession();
        } else{
            Info("Lectura realizada con exito");
            Info(in.getContent()); 
            JsonArray arrayAux = new JsonArray();
            arrayAux = Json.parse(in.getContent()).asObject().get("details").asObject().get("perceptions").asArray();
        
            for(int i = 0; i < arrayAux.size(); i++ )
            {
                switch(arrayAux.get(i).asObject().get("sensor").asString())
                {
                    case "thermal":
                        this.thermal = arrayAux.get(i).asObject().get("data").asArray();
                        break;
                    case "gps":
                        Info("Lectura GPS");
                        Info(arrayAux.get(i).asObject().get("data").toString());
                        this.posx = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(0).asInt();
                        this.posy = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(1).asInt();
                        this.posz = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(2).asInt();
                        break;
                }
            }
        }
    }
    

}
