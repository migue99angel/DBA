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
 * Clase para modelar a los drones de tipo Rescuer
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class Rescuer extends Dron {
    protected static int alemanesEncontrados = 0;
    protected static boolean escuchando = true;
    protected int altimeter;
    protected int alemanesEnDron = 0;
    
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
    public void comportamiento() {
        JsonObject aux = new JsonObject();
        JsonArray ruta = new JsonArray();
        
        // Recarga inicial
        recargar();
        
        // Leemos los sensores
        leerSensores();
        
        // Informamos al listener
        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.INFORM, "REGULAR", "", myConvId, false);
        
        while(Rescuer.escuchando) {
            Info ("Escuchando mensajes");
            in = blockingReceive(5000);
            if (in != null){
                switch(in.getPerformative()) {
                    case ACLMessage.QUERY_IF:

                        aux = new JsonObject();
                        aux.add("posx", this.posx);
                        aux.add("posy", this.posy);
                        aux.add("energy", this.energy);
                        aux.add("altimeter", this.altimeter);
                        aux.add("orientacion", this.orientacion);
                        aux.add("cuadrante", this.cuadrante);
                        aux.add("posIniX", this.posInix);
                        aux.add("posIniY", this.posIniy);
                        Info ("Enviando mi posicion al listener");
                        
                        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.AGREE, "REGULAR", aux.toString(), myConvId, false);
                        break;

                    case ACLMessage.QUERY_REF:
                        ruta = Json.parse(in.getContent()).asArray();
                        Info("Ruta recibida correctamente");
                        Info("Distancia de la ruta: " + Integer.toString(ruta.size()));

                        seguirRuta(ruta);

                        Info("Aleman rescatado correctamente");

                        Rescuer.alemanesEncontrados++;
                        this.alemanesEnDron++;
                        
                        leerSensores();
                        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.INFORM, "REGULAR", "", myConvId, false);

                        if(DRAGONFLY_CAIXABANK.alemanes == Rescuer.alemanesEncontrados) {
                            Rescuer.escuchando = false;
                        }

                        break;
                    case ACLMessage.INFORM:
                        Info("Mis coordenadas se han enviado correctamente");
                        break;
                        
                    default:
                        break;
                }
            }
        }
        
        Info("He rescatado a " + this.alemanesEnDron + " alemanes");
        
        if (this.alemanesEnDron > 0){
            //Volver al punto inicial
            if (in == null){
                enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.INFORM, "REGULAR", "", myConvId, false);
            }
            
            in = blockingReceive();
            
            aux = new JsonObject();
            aux.add("posx", this.posx);
            aux.add("posy", this.posy);
            aux.add("energy", this.energy);
            aux.add("altimeter", this.altimeter);
            aux.add("orientacion", this.orientacion);
            aux.add("cuadrante", this.cuadrante);
            aux.add("posIniX", this.posInix);
            aux.add("posIniY", this.posIniy);
            
            Info("Enviando mi posicion al listener");
            
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.AGREE, "REGULAR", aux.toString(), myConvId, false);
            
            in = blockingReceive();
            
            if (in.getPerformative() != ACLMessage.INFORM){
                Info("Ha habido un fallo a la hora de enviar mis coordenadas");
                abortSession();
            } else {
                enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);

                in = blockingReceive();
                ruta = Json.parse(in.getContent()).asArray();
                Info("Ruta recibida correctamente");
                Info("Distancia de la ruta: " + Integer.toString(ruta.size()));
                Info("Volviendo a mi posición inicial");
                
                seguirRuta(ruta);
            }
            
        }

    }

    @Override
    protected void lecturaSensoresConcretos(JsonObject o) {
        switch(o.get("sensor").asString())
        {
            case "gps":
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
