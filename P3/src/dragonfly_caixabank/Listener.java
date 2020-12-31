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
import java.util.ArrayList;

/**
 *
 * @author mumo
 */
public class Listener extends AgenteBase{
    
    protected static DBAMap mapa;
    protected boolean listening;
    protected ArrayList<JsonObject> alemanesEncontrados = new ArrayList<>();
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "LISTENER";
        myWMProtocol = "REGULAR";
        listening = true;
        
        //Cargamos el mapa
        mapa = new DBAMap();
        try{
            mapa.load("./maps/" + DRAGONFLY_CAIXABANK._filename + ".png");
        }catch (Exception ex){
            System.err.println("***ERROR "+ex.toString());
        }
    }
    
    @Override
    public void plainExecute() {   
        super.plainExecute();
        
        // Esperamos a que el Controlador nos despierte
        in = new ACLMessage();
        Info("Agente " + getAID() + " esperando a ser despertado");
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.QUERY_IF) {
            Info("Agente " + getAID() + " no ha recibido QUERY_IF");
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("Agente " + getAID() + " despertado");
            
            // Informamos al Controlador de las dimensiones del mapa
            JsonObject aux = new JsonObject();
            aux.add("height",mapa.getHeight());
            aux.add("width",mapa.getWidth());
            enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.CONFIRM, "REGULAR", aux.toString(), myConvId, false);
            
            myConvId = in.getConversationId();
            loginWorldManager();
            
            comportamiento();
            
            // Cerrar la puerta
            logout();
            
            // Avisamos al Controlador de que nos deslogueamos
            enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
        }
        
        takeDown();
        _exitRequested = true;
    }

    @Override
    protected void loginWorldManager() {
        Info("Login al World Manager de " + getAID());
        
        enviarMensaje(myWorldManager, ACLMessage.SUBSCRIBE, myWMProtocol, new JsonObject().add(myAction, myValue).toString(), myConvId, false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en SUBSCRIBE de WM de Agente " + getAID());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("SUBSCRIBE WM OK de Agente " + getAID());
        }
    } 
    
    protected void comportamiento() {
        JsonObject aux = new JsonObject();
        JsonArray ruta = new JsonArray();
        int posIniXRescuer = -1, posIniYRescuer = -1, energiaRescuer = -1, cuadrante = -1;
        
        Info("Listener escuchando mensajes");
        
        while(listening){
            in = blockingReceive();
            
            switch(in.getPerformative()){
                //Cancel
                case ACLMessage.CANCEL:
                    listening = false;
                    break;
                //Request
                case ACLMessage.REQUEST:
                    aux = Json.parse(in.getContent()).asObject();
                    
                    if(aux.get("type").asString().contains("SEEKER")){
                        ruta = calcularRutaSeeker(aux.get("cuadrante").asInt(), aux.get("posx").asInt(), aux.get("posy").asInt(), aux.get("altimeter").asInt(), aux.get("orientacion").asInt(), aux.get("energy").asInt());
                    }
                    
                    enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.INFORM, "REGULAR", ruta.toString(), myConvId, false);
                    break;
                case ACLMessage.QUERY_REF:
                    aux = Json.parse(in.getContent()).asObject();
                    
                    JsonObject aleman = new JsonObject();
                    aleman.add("posx", aux.get("posx").asInt());
                    aleman.add("posy", aux.get("posy").asInt());
                    
                    if(this.alemanesEncontrados.indexOf(aleman) == -1) {
                        alemanesEncontrados.add(aleman);
                        
                        Info("Nuevo alemán encontrado");
                        
                        String rescuerALlamar = DRAGONFLY_CAIXABANK.dronesRescuer.get(aux.get("cuadrante").asInt());
                        
                        enviarMensaje(rescuerALlamar, ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
                        
                        // Confirmamos al seeker
                        enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
                    } else {
                        enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.DISCONFIRM, "REGULAR", "", myConvId, false);
                    }
                    
                    break;

                case ACLMessage.AGREE:
                    aux = Json.parse(in.getContent()).asObject();
                    posIniXRescuer = Json.parse(in.getContent()).asObject().get("posx").asInt();
                    posIniYRescuer = Json.parse(in.getContent()).asObject().get("posy").asInt();
                    energiaRescuer = Json.parse(in.getContent()).asObject().get("energy").asInt();
                    cuadrante = Json.parse(in.getContent()).asObject().get("cuadrante").asInt();
                    
                    ruta = calcularRutaRescuer(posIniXRescuer, posIniYRescuer, aux.get("posx").asInt(), aux.get("posy").asInt(), energiaRescuer, aux.get("altimeter").asInt(), aux.get("orientacion").asInt());
                    
                    // Notificamos al rescuer
                    enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(cuadrante), ACLMessage.QUERY_REF, "REGULAR", ruta.toString(), myConvId, false);
                    
                    break;

            }
        }
    }
    
    protected JsonArray calcularRutaSeeker(int cuadrante, int posx, int posy, int altura, int orientacion, int energy){
        Boolean generandoCamino = true;
        Boolean ultimoTramo = false;
        String orientacionCamino = "ABAJO";
        String orientacionAnterior = "";
        int contadorPasos = 21;
        int contadorPasosSinLeerSensores = 0;
        JsonArray ruta = new JsonArray();
        JsonObject aux = new JsonObject();
        
        altura += mapa.getLevel(posx, posy);
        
        while(generandoCamino){
            switch (orientacionCamino){
                
                case "ABAJO":
                    if (orientacion != 180){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        ruta.add(aux);
                        energy--;
                        orientacion += 45;
                        
                    } else {
                        
                        while (mapa.getLevel(posx, posy+1) > altura){
                            Info("Subiendo");
                            Info(mapa.getLevel(posx, posy+1) + " > " + altura);
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (posy < mapa.getHeight() - 11) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
                            energy--;
                            posy++;
                        } else {
                            orientacionCamino = "DERECHA";
                            orientacionAnterior = "ABAJO";
                            if (ultimoTramo){
                                generandoCamino = false;
                            }
                        }
                    }
                    break;
                case "ARRIBA":
                    if (orientacion != 0){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        ruta.add(aux);
                        energy--;
                        orientacion -= 45;
                        
                    } else {
                        
                        while (mapa.getLevel(posx, posy-1) > altura){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (posy > 11) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
                            energy--;
                            posy--;
                        } else {
                            if (ultimoTramo){
                                generandoCamino = false;
                            }
                            orientacionCamino = "DERECHA";
                            orientacionAnterior = "ARRIBA";
                        }
                        
                    }
                    break;
                case "DERECHA":
                    if (orientacion != 90 && orientacionAnterior.contains("ABAJO")){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        ruta.add(aux);
                        energy--;
                        orientacion -= 45;
                        
                    } else if(orientacion != 90 && orientacionAnterior.contains("ARRIBA")) {
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        ruta.add(aux);
                        energy--;
                        orientacion += 45;
                    } else {
                        
                        while (mapa.getLevel(posx+1, posy) > altura){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (contadorPasos != 0 && posx < (mapa.getWidth()/2)-11 + (cuadrante*(mapa.getWidth()/2))) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
                            energy--;
                            posx++;
                            contadorPasos--;
                        } else {
                            Info(Integer.toString(posx) + " > " + Integer.toString((mapa.getWidth()/2)-11 + (cuadrante*(mapa.getWidth()/2))));
                            if (posx >= (mapa.getWidth()/2)-11 + (cuadrante*(mapa.getWidth()/2))){
                                Info("He entrado al ultimo tramo");
                                ultimoTramo = true;
                            }
                            if (orientacionAnterior.contains("ABAJO")){
                                orientacionCamino = "ARRIBA";
                            } else {
                                orientacionCamino = "ABAJO";
                            }
                            contadorPasos = 21;
                        }
                        
                    }
                    break;
            }
            
            if(contadorPasosSinLeerSensores == 20) {
                contadorPasosSinLeerSensores = 0;
                aux = new JsonObject();
                aux.add("action", "read");
                ruta.add(aux);
            } else {
                contadorPasosSinLeerSensores++;
            }
            
            // @todo Calcular cuándo es necesario recargar            
            if(energy < 200) {
                aux = new JsonObject();
                
                while(altura > 0) {                   
                    aux.add("action", "move");
                    aux.add("value", "moveD");
                    ruta.add(aux);
                    
                    aux = new JsonObject();
                }

                aux = new JsonObject();
                
                aux.add("action", "move");
                aux.add("action", "recharge");
                ruta.add(aux);
            }
        }

        return ruta;
        
    }
    
    protected JsonArray calcularRutaRescuer(int posIniX, int posIniY, int posDestinoX, int posDestinoY, int energy, int altura, int orientacion){
        JsonArray ruta = new JsonArray();
        JsonObject aux = new JsonObject();
        
        altura += mapa.getLevel(posIniX, posIniY);
        
        while(posIniX != posDestinoX) {
            
            if(posIniX < posDestinoX) {
                while(orientacion != 90) {
                    aux = new JsonObject();
                    
                    if(orientacion < 90) {                       
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        orientacion += 45;
                    } else if(orientacion > 90) {
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        orientacion -= 45;
                    }
                    
                    energy -= 4;
                    ruta.add(aux);
                }
            } else if(posIniX > posDestinoX) {
                while(orientacion != -90) {
                    aux = new JsonObject();
                    
                    if(orientacion < -90) {                       
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        orientacion += 45;
                    } else if(orientacion > -90) {
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        orientacion -= 45;
                    }
                    
                    energy -= 4;
                    ruta.add(aux);
                }
            }
                
            while(posIniX != posDestinoX) {
                if(orientacion == 90) {
                    while (mapa.getLevel(posIniX+1, posIniY) > altura){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "moveUP");
                        ruta.add(aux);
                        energy -= 20;
                        altura += 5;
                    }
                } else {
                    while (mapa.getLevel(posIniX-1, posIniY) > altura){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "moveUP");
                        ruta.add(aux);
                        energy -= 20;
                        altura += 5;
                    }
                }               

                aux = new JsonObject();
                aux.add("action", "move");
                aux.add("value", "moveF");
                
                if(orientacion == 90) {
                    posIniX++;
                } else {
                    posIniX--;
                }
                
                energy -= 4;
                ruta.add(aux);
            }
            
            // @todo Calcular cuándo es necesario recargar            
            if(energy < 200) {
                aux = new JsonObject();
                
                while(altura > 0) {                   
                    aux.add("action", "move");
                    aux.add("value", "moveD");
                    ruta.add(aux);
                    
                    aux = new JsonObject();
                }

                aux = new JsonObject();
                
                aux.add("action", "move");
                aux.add("action", "recharge");
                ruta.add(aux);
            }
        }
        
        while(posIniY != posDestinoY) {
            
            if(posIniY < posDestinoY) {
                while(orientacion != 90) {
                    aux = new JsonObject();
                    
                    if(orientacion < 90) {                       
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        orientacion += 45;
                    } else if(orientacion > 90) {
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        orientacion -= 45;
                    }
                    
                    energy -= 4;
                    ruta.add(aux);
                }
            } else if(posIniY > posDestinoY) {
                while(orientacion != -90) {
                    aux = new JsonObject();
                    
                    if(orientacion < -90) {                       
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        orientacion += 45;
                    } else if(orientacion > -90) {
                        aux.add("action", "move");
                        aux.add("value", "rotateL");
                        orientacion -= 45;
                    }
                    
                    energy -= 4;
                    ruta.add(aux);
                }
            }
                
            while(posIniY != posDestinoY) {
                if(orientacion == 90) {
                    while (mapa.getLevel(posIniX+1, posIniY) > altura){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "moveUP");
                        ruta.add(aux);
                        energy -= 20;
                        altura += 5;
                    }
                } else {
                    while (mapa.getLevel(posIniX-1, posIniY) > altura){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "moveUP");
                        ruta.add(aux);
                        energy -= 20;
                        altura += 5;
                    }
                }               

                aux = new JsonObject();
                aux.add("action", "move");
                aux.add("value", "moveF");
                
                if(orientacion == 90) {
                    posIniY++;
                } else {
                    posIniY--;
                }
                
                energy -= 4;
                ruta.add(aux);
            }
            
            // @todo Calcular cuándo es necesario recargar            
            if(energy < 200) {
                aux = new JsonObject();
                
                while(altura > 0) {                   
                    aux.add("action", "move");
                    aux.add("value", "moveD");
                    ruta.add(aux);
                    
                    aux = new JsonObject();
                }

                aux = new JsonObject();
                
                aux.add("action", "move");
                aux.add("action", "recharge");
                ruta.add(aux);
            }
        }
        
        while(mapa.getLevel(posIniX, posIniY) > 0) {
            aux = new JsonObject();
            aux.add("action", "move");
            aux.add("value", "moveD");
            ruta.add(aux);
        }
 
        aux = new JsonObject();
        aux.add("action", "move");
        aux.add("value", "rescue");
        ruta.add(aux);
        
        return ruta;
    }  
}
