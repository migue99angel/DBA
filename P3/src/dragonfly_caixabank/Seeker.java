package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Seeker extends Dron {
    protected static int alemanesDetectados = 0;
    protected JsonArray thermal = new JsonArray();
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
        
        //Comportamiento general
        while (Seeker.alemanesDetectados < DRAGONFLY_CAIXABANK.alemanes){
            leerSensores();
            
            realizarSiguienteMovimiento();
            
            //Provisional
            Seeker.alemanesDetectados = 10;
        }

    }
    
    public void leerSensores(){
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
                        this.posx = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(0).asInt();
                        this.posy = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(1).asInt();
                        this.posz = arrayAux.get(i).asObject().get("data").asArray().get(0).asArray().get(2).asInt();
                        break;
                }
            }
        }
    }
    
    public void realizarSiguienteMovimiento(){
        for (int i=0; i<thermal.size(); i++){
            for (int j=0; j<thermal.size(); j++){
                
            }
        }
    }

}
