package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;

/**
 * Clase para modelar a los drones de tipo Seeker
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class Seeker extends Dron {
    protected static int alemanesDetectados = 0;
    protected JsonArray thermal = new JsonArray();
    protected JsonArray ruta = new JsonArray();
    protected int energy = 995;
    protected int altimeter;
    protected static int consumo = 7;
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "SEEKER";
        myWMProtocol = "REGULAR";
        
        // Rellenamos el array con los sensores que queremos
        sensoresRequeridos.add("ALTIMETER");
        sensoresRequeridos.add("GPS");
        sensoresRequeridos.add("THERMALHQ");
        
        // La recarga requerida
        sensoresRequeridos.add("CHARGE");
        
    }
    
    @Override
    public void comportamiento() {
        // Recarga inicial
        recargar();
        
        // Leemos sensores
        leerSensores();
        
        // Pedimos la ruta a seguir
        pedirRuta();
        
        // Comportamiento general      
        seguirRuta(ruta);

    }
    
    /**
     * Pedir la ruta a seguir al Listener
     */
    protected void pedirRuta(){
        Info("Pidiendo ruta");
        
        JsonObject aux = new JsonObject();
        aux.add("type", this.myValue);
        aux.add("cuadrante", this.cuadrante);
        aux.add("posx", this.posx);
        aux.add("posy", this.posy);
        aux.add("posz", this.posz);
        aux.add("energy", this.energy);
        aux.add("orientacion", this.orientacion);
        aux.add("altimeter", this.altimeter);
        
        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.REQUEST, "REGULAR", aux.toString(), myConvId, false);
        
        in = blockingReceive();
        
        if (in.getPerformative() != ACLMessage.INFORM){
            Info("Error al recibir la ruta");
            abortSession();
        } else {
            this.ruta = Json.parse(in.getContent()).asArray();
            Info("Ruta recibida correctamente");
            Info("Distancia de la ruta: " + Integer.toString(this.ruta.size()));
        }
    }
    
    /**
     * Leemos el sensor THERMAL para detectar posibles alemanes en nuestro entorno
     */
    protected void detectarAlemanes() {
        JsonObject posicion = new JsonObject();
        int aleman_posx, aleman_posy;
        
        // El dron está en la posición central de la matriz [10,10]
        int posFijaX = 10, posFijaY = 10;
        
        for(int i=0; i < this.thermal.size(); i++) {   
            for(int j=0; j < this.thermal.get(i).asArray().size(); j++) {
                if(this.thermal.get(i).asArray().get(j).asFloat() == 0.00f) {
                    
                    //IMPORTANTE | ESTO ESTA AL REVES PORQUE LO LEIAMOS AL REVES, MIRARLO LUEGO
                    
                    aleman_posx = j - posFijaX + this.posx;
                    aleman_posy = i - posFijaY + this.posy;
                    
                    Info("Alemán encontrado en posición del mundo " + aleman_posx + "," + aleman_posy);
                    
                    posicion.add("posx", aleman_posx);
                    posicion.add("posy", aleman_posy);
                    posicion.add("type", this.myValue);
                    posicion.add("cuadrante", this.cuadrante);
                    
                    enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.QUERY_REF, "REGULAR", posicion.toString(), myConvId, false);
                    in = blockingReceive();
                    
                    if(in.getPerformative() == ACLMessage.CONFIRM) {
                        Info("Alemán confirmado");
                        Seeker.alemanesDetectados++;
                        Info("Alemanes detectados hasta ahora: " + Seeker.alemanesDetectados);
                    } else if(in.getPerformative() != ACLMessage.DISCONFIRM) {
                        Info("Fallo al encontrar alemán");
                        Info(in.getContent());
                        abortSession();
                    }
                    
                    posicion = new JsonObject();
                }
            }           
        }
    }
    
    @Override
    protected void lecturaSensoresConcretos(JsonObject o) {
        switch(o.get("sensor").asString())
        {
            case "thermal":
                this.thermal = o.get("data").asArray();
                detectarAlemanes();

                break;
                
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
    
    @Override
    protected void seguirRuta(JsonArray ruta) {
        JsonObject aux = new JsonObject();
        
        for (int i=0; i < ruta.size() && (Seeker.alemanesDetectados < DRAGONFLY_CAIXABANK.alemanes); i++){
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
                    
                case "inform":
                    this.energy = ruta.get(i).asObject().get("value").asInt();
                    break;
            }
        }
    }
}
