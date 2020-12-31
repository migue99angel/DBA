/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author mumo
 */
public class Rescuer extends Dron {
    protected static int alemanesEncontrados = 0;
    protected boolean escuchando = true;
    protected int energy = 995;
    protected int altimeter;
    protected int orientacion = 45;
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "RESCUER";
        myWMProtocol = "REGULAR";
        
        // Rellenamos el array con los sensores que queremos
        sensoresRequeridos.add("GPS");
        sensoresRequeridos.add("ALTIMETER");
        sensoresRequeridos.add("CHARGE");
        
    }
    
    @Override
    public void comportamiento(){
        JsonObject aux = new JsonObject();
        JsonArray ruta = new JsonArray();
        
        //Recarga inicial
        recargar();
        
        while(escuchando) {
            leerSensores();
            in = blockingReceive();
            
            switch(in.getPerformative()) {
                case ACLMessage.QUERY_IF:
                    
                    aux = new JsonObject();
                    aux.add("posx", this.posInix);
                    aux.add("posy", this.posIniy);
                    aux.add("energy", this.energy);
                    aux.add("altimeter", this.altimeter);
                    aux.add("orientacion", this.orientacion);
                    aux.add("cuadrante", this.cuadrante);
                    
                    enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.AGREE, "REGULAR", aux.toString(), myConvId, false);
                    break;
                    
                case ACLMessage.QUERY_REF:
                    ruta = Json.parse(in.getContent()).asArray();
                    Info (in.getContent());
                    Info (Integer.toString(ruta.size()));
                    Info("Ruta recibida correctamente");
                    
                    seguirRuta(ruta, Rescuer.alemanesEncontrados);
                    
                    Rescuer.alemanesEncontrados++;
                    
                    if(DRAGONFLY_CAIXABANK.alemanes == Rescuer.alemanesEncontrados) {
                        escuchando = false;
                    }
                    
                    break;
            }
        }
    }

    @Override
    protected void lecturaSensoresConcretos(JsonObject o) {
        switch(o.get("sensor").asString())
        {
            case "gps":
                Info(o.get("data").toString());
                this.posx = o.get("data").asArray().get(0).asArray().get(0).asInt();
                this.posy = o.get("data").asArray().get(0).asArray().get(1).asInt();
                this.posz = o.get("data").asArray().get(0).asArray().get(2).asInt();
                break;
            case "altimeter":
                this.altimeter = o.get("data").asArray().get(0).asInt();
                break;
        }
    }
    
}
