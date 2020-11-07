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

/**
 * Clase principal del proyecto
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class MyDragonfly extends IntegratedAgent{
    
    float energy = 0.0f;
    int cooldown = 0;
    String mejorR = "rotateR"; //R
    
    TTYControlPanel myControlPanel;
    String receiver;
    int alive;
    float angular;
    float fixedAngular;
    float compass;
    int onTarget = 0;
    boolean recargando = false;
    int alturaDron;
    int alturaSuelo;
    int energia;
    float distance;
    int numSensores;
    int maxflight;
    String estado = "LOCALIZADO";
    String estadoAnterior = "";
    ArrayList<Integer> visual = new ArrayList<>();
    ArrayList<Coordenadas> coordenadas = new ArrayList<>();
    
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
        jsonObjIn.add("world", "World9");

        JsonArray toContent = new JsonArray();
        toContent.add("alive");
        toContent.add("gps");
        toContent.add("distance");
        toContent.add("energy");
        toContent.add("ontarget");
        toContent.add("altimeter");
        toContent.add("compass");
        toContent.add("visual");
        toContent.add("lidar");
        toContent.add("angular");
        
        this.numSensores = toContent.size();
        this.energia = 1000 + this.numSensores;
       
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
        this.maxflight = jsonObjOut.get("maxflight").asInt();
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
        
        while(onTarget == 0 && this.alive == 1)
        {
  
            myControlPanel.feedData(in,width , height, this.maxflight);
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
            Info(in.getContent());
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
    
    /**
     * Método donde se elige qué movimiento realizar según las casuísticas del mundo,
     * nuestra función heurística.
     * @return String El mejor movimiento seleccionado
     */
    protected String funcionHeuristica() {
        String eleccion = "";
        int alturaProxima = 0;
        Coordenadas coorProxima = new Coordenadas(this.coordenadas.get(this.coordenadas.size()-1).getX(),this.coordenadas.get(this.coordenadas.size()-1).getY());
        
        switch ((int)this.compass){
            case -45:
                alturaProxima = this.visual.get(0);
                coorProxima.setX(coorProxima.getX()-1);
                coorProxima.setY(coorProxima.getY()-1);
                break;
            case 0:
                alturaProxima = this.visual.get(1);
                coorProxima.setY(coorProxima.getY()-1);
                break;
            case 45:
                alturaProxima = this.visual.get(2);
                coorProxima.setX(coorProxima.getX()+1);
                coorProxima.setY(coorProxima.getY()-1);
                break;
            case -90:
                alturaProxima = this.visual.get(3);
                coorProxima.setX(coorProxima.getX()-1);
                break;
            case 90:
                alturaProxima = this.visual.get(5);
                coorProxima.setX(coorProxima.getX()+1);
                break;
            case -135:
                alturaProxima = this.visual.get(6);
                coorProxima.setX(coorProxima.getX()-1);
                coorProxima.setY(coorProxima.getY()+1);
                break;
            case 180:
                alturaProxima = this.visual.get(7);
                coorProxima.setY(coorProxima.getY()+1);
                break;
            case 135:
                alturaProxima = this.visual.get(8);
                coorProxima.setX(coorProxima.getX()+1);
                coorProxima.setY(coorProxima.getY()+1);
                break; 
        }
        
        // Comprobamos si tenemos que recargar
        /*if (this.energia - ((this.alturaDron/5)*(5 + this.numSensores)) < 100 && this.estado != "RECARGANDO") {
            this.estadoAnterior = this.estado;
            this.estado = "RECARGANDO";
        }*/
       
        
        if (this.estado == "LOCALIZADO") {
            if (this.distance == 0) {
                if (this.alturaDron > 5) {
                    eleccion = "moveD";
                    this.energia -= 5;
                } else if (this.alturaDron > 0) {
                    eleccion = "touchD";
                    this.energia -= 1;
                } else {
                    eleccion = "rescue";
                    this.onTarget = 1;
                }
            } else if (this.alturaDron + this.alturaSuelo < alturaProxima) {
                if ((this.alturaDron+this.alturaSuelo + 5 < this.maxflight) && (alturaProxima <= this.maxflight)) {
                    System.out.println(this.alturaDron+this.alturaSuelo);
                    System.out.println(this.maxflight);
                    eleccion = "moveUP";
                    this.energia -= 5;
                } else {
                    this.estado = "BORDEANDO";
                }
            } else {
                if (this.angular == this.compass || (this.fixedAngular == this.compass && this.angular%45 != 0.0f)) {
                    if (this.compruebaCasilla(coorProxima)) {
                        this.estado = "BORDEANDO";
                    } else {
                        eleccion = "moveF";
                        this.energia -= 1;
                    }
                } else {
                    eleccion = mejorRotacion();
                    this.energia -= 1;
                }   
            }
            
            cooldown--;
        }
        
        if (estado == "BORDEANDO") {
            if (cooldown < 0) {
                /*if (mejorR == "rotateL") {
                    mejorR = "rotateR";
                } else {
                    mejorR = "rotateL";
                }*/
                mejorR = "rotateL";
            }
            
            cooldown = 12;

            if ((alturaProxima+1 >= this.maxflight) || this.compruebaCasilla(coorProxima)) {
                eleccion = mejorR;
                this.energia -= 1;
            } else if (this.alturaDron+this.alturaSuelo >= alturaProxima) {
                eleccion = "moveF";
                this.estado = "LOCALIZADO";
                this.energia -= 1;
            } else if (this.alturaDron+this.alturaSuelo+5 <= this.maxflight) {
                eleccion = "moveUP";
                this.energia -= 5;
            }
        }
        
        if (eleccion == "moveF" || eleccion == "moveUP") {
            if (!couldIRechargeThere() && this.estado != "RECARGANDO") {
                this.estadoAnterior = this.estado;
                this.estado = "RECARGANDO";
            }
        } else if (eleccion == "rotateL" || eleccion == "rotateR") {
            if (!couldIRechargeHereIfRotate() && this.estado != "RECARGANDO") {
                this.estadoAnterior = this.estado;
                this.estado = "RECARGANDO";
            }
        }
        
        if (estado == "RECARGANDO") { 
            cooldown++;

            if (this.alturaDron > 5) {
                eleccion = "moveD";
                this.energia -= 5;
            } else if (this.alturaDron > 0) {
                eleccion = "touchD";
                this.energia -= this.alturaDron;
            } else {
                eleccion = "recharge";
                this.energia = 1000 + this.numSensores;
                this.estado = this.estadoAnterior;
            }
        }
        
        return eleccion;
    }
    
    /**
     * Método para leer los sensores y asignarlos a las variables
     * de clase previamente declaradas
     * @param jsonObjOut JsonObject con todos los sensores leídos
     */
    protected void lecturaSensores(JsonObject jsonObjOut)
    {
        this.energia -= (1 * this.numSensores);
        JsonArray aux = jsonObjOut.get("details").asObject().get("perceptions").asArray();
        
        for(int i = 0; i < aux.size(); i++)
        {
            switch(aux.get(i).asObject().get("sensor").asString())
            {
                case "alive":
                   this.alive = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                   break;
                case "angular":
                    this.angular = aux.get(i).asObject().get("data").asArray().get(0).asFloat();
                    this.fixedAngular = fixAngular();                  
                    break;
                case "compass":
                    this.compass = aux.get(i).asObject().get("data").asArray().get(0).asFloat();
                    break;
                case "ontarget":
                    this.onTarget = aux.get(i).asObject().get("data").asArray().get(0).asInt();
                    break;
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
                case "gps":
                    float x =  aux.get(i).asObject().get("data").asArray().get(0).asArray().get(0).asFloat();
                    float y =  aux.get(i).asObject().get("data").asArray().get(0).asArray().get(1).asFloat();
                    float z =  aux.get(i).asObject().get("data").asArray().get(0).asArray().get(2).asFloat();
                    
                    Coordenadas c_aux = new Coordenadas(x,y,z);

                    if (!compruebaCasilla(c_aux)) {
                        this.coordenadas.add(c_aux);
                    }
                        
                    break;
            }
        }
        
    }
    
    /**
     * Método para aproximar el angular de nuestro dron a valores
     * que puedan ser traducidos a giros
     * @return Angular aproximado
     */
    protected float fixAngular()
    {
        float fixedAng = this.angular;
        
        if (this.angular % 45.0f != 0.0f) {        
            if (this.angular < 0.0f) {
                if (this.angular > -90.0f) {
                    fixedAng = -45.0f;
                } else {
                    fixedAng = -135.0f;
                }
            } else {
                if (this.angular < 90.0f) {
                    fixedAng = 45.0f;
                } else {
                    fixedAng = 135.0f;
                }
            }
        }
        
        return fixedAng;
    }
    
    /**
     * Método para escoger la mejor rotación a realizar
     * teniendo en cuenta los valores actuales del compass y el angular
     * @return Elección tomada
     */
    protected String mejorRotacion()
    {
        String rotacion = "R";
        String rotacionaccion = "rotate";

        if (this.compass > 0 && this.angular > 0) {
            if (this.compass > this.angular) {
                rotacion = "L";
            }
        } else if (this.compass < 0 && this.angular < 0) {
            if (this.compass > this.angular) {
                rotacion = "L";
            }
        } else if (this.compass < 0 && this.angular > 0) {
            if (this.compass < -90 && this.angular >= 90) {
                rotacion = "L";
            }
        } else {
            if (this.compass < 90 && this.angular <= -90) {
                rotacion = "L";
            }
        }
        
        return rotacionaccion.concat(rotacion);
    }
    
    /**
     * Método para comprobar si los componentes x,y de dos objetos Coordenadas
     * coinciden
     * @param aux Coordenadas a comprobar
     * @return True si coinciden ambas coordenadas, false en caso contrario
     */
    protected Boolean compruebaCasilla(Coordenadas aux){
        for(int i = 0; (i < this.coordenadas.size()); i++) {
            if(this.coordenadas.get(i).esIgual(aux)) {
                return true;
            }    
        }
        
        return false;
    }
    
    /**
     * Método para comprobar si puedo recargar si efectuase una rotación
     * @return True si puede recargar tras rotar, false en caso contrario
     */
    protected boolean couldIRechargeHereIfRotate()
    {
        boolean couldI = false;
        int wasteEnergyToDown = 0;
        int wasteEnergyToRotate = 1 + this.numSensores;
        
        if (this.alturaDron > 5) {
            wasteEnergyToDown = (this.alturaDron/5 - 1) * (5 + this.numSensores) + 5 * this.numSensores + 5;
        } else {
            wasteEnergyToDown = this.alturaDron * (1 + this.numSensores);
        }
        
        if (this.energia - (wasteEnergyToRotate + wasteEnergyToDown + 100) > 0) {
            couldI = true;
        }
        
        return couldI;
    }
    
    /**
     * Método para comprobar si puedo recargar si efectuase un movimiento
     * hacia adelante o hacia arriba
     * @return True si puede recargar tras moverse, false en caso contrario
     */
    protected boolean couldIRechargeThere()
    {
        int lookingAlt = lookingAltitude(this.compass);
        int downAlt = 0;
        int upAlt = 0;
        int wasteEnergyToForward = 1 + this.numSensores;
        int wasteEnergyToDown = downAlt;
        int wasteEnergyToUp = upAlt;
        boolean couldI = false;
        
        if (lookingAlt == this.alturaSuelo) {
            downAlt = this.alturaDron;
        } else if (lookingAlt < this.alturaSuelo) {
            downAlt = this.alturaDron + (this.alturaSuelo - lookingAlt);
        } else {
            if (lookingAlt > this.alturaSuelo + this.alturaDron) {
                upAlt = lookingAlt - (this.alturaDron + this.alturaSuelo);
            } else {
                downAlt = (this.alturaDron + this.alturaSuelo) - lookingAlt;
            }
        }
        
        wasteEnergyToUp = (upAlt/5) * (this.numSensores + 5);
        
        if (downAlt > 5) {
            wasteEnergyToDown = ((downAlt/5) - 1) * (this.numSensores + 5) + 5 * this.numSensores + 5;
        } else {
            wasteEnergyToDown = downAlt * (this.numSensores + 1);
        }
        
        if (this.energia - (wasteEnergyToUp + wasteEnergyToForward + wasteEnergyToDown + 100) > 0) {
            couldI = true;
        }
        
        return couldI;
    }
    
    /**
     * Método para obtener la altura de la casilla a la que está orientado
     * nuestro dron
     * @param ang Valor actual del compass
     * @return Altura de la casilla
     */
    protected int lookingAltitude(float ang)
    {
        int lookingAlt = 0;
        switch ((int) ang){
            case -45:
                lookingAlt = this.visual.get(0);
                break;
            case 0:
                lookingAlt = this.visual.get(1);
                break;
            case 45:
                lookingAlt = this.visual.get(2);
                break;
            case -90:
                lookingAlt = this.visual.get(3);
                break;
            case 90:
                lookingAlt = this.visual.get(5);
                break;
            case -135:
                lookingAlt = this.visual.get(6);
                break;
            case 180:
                lookingAlt = this.visual.get(7);
                break;
            case 135:
                lookingAlt = this.visual.get(8);
                break; 
            }
        
        return lookingAlt;
    }
}
