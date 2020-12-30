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
public class Listener extends AgenteBase{
    
    protected static DBAMap mapa;
    protected boolean listening;
    
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
        Info("Listener escuchando mensajes");
        while(listening){
            in = blockingReceive();
            
            switch(in.getPerformative()){
                //Cancel
                case 2:
                    listening = false;
                    break;
                //Request
                case 16:
                    JsonObject aux = new JsonObject();
                    JsonArray ruta = new JsonArray(); 
                    aux = Json.parse(in.getContent()).asObject();
                    if(aux.get("type").asString().contains("SEEKER")){
                        ruta = calcularRutaSeeker(aux.get("cuadrante").asInt(), aux.get("posx").asInt(), aux.get("posy").asInt(), aux.get("posz").asInt(), aux.get("orientacion").asInt(), aux.get("energy").asInt());
                    } else{
                        ruta = calcularRutaRescuer(aux.get("posx").asInt(), aux.get("posy").asInt(), aux.get("posz").asInt(), aux.get("energy").asInt());
                    }
                    enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.INFORM, "REGULAR", ruta.toString(), myConvId,false);
                    break;
            }
        }
    }
    
    protected JsonArray calcularRutaSeeker(int cuadrante, int posx, int posy, int posz, int orientacion, int energy){
        Boolean generandoCamino = true;
        Boolean ultimoTramo = false;
        String orientacionCamino = "ABAJO";
        String orientacionAnterior = "";
        int contadorPasos = 21;
        JsonArray ruta = new JsonArray();
        JsonObject aux;
        
        while(generandoCamino){
            switch (orientacionCamino){
                
                case "ABAJO":
                    if (orientacion != 180){
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        ruta.add(aux);
                        orientacion += 45;
                        
                    } else {
                        
                        while (mapa.getLevel(posx, posy+1) > posz){
                            Info("Subiendo");
                            Info(mapa.getLevel(posx, posy+1) + " > " + posz);
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            posz += 5;
                        }
                        
                        if (posy < mapa.getHeight() - 11) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
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
                        orientacion -= 45;
                        
                    } else {
                        
                        while (mapa.getLevel(posx, posy-1) > posz){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            posz += 5;
                        }
                        
                        if (posy > 11) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
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
                        orientacion -= 45;
                        
                    } else if(orientacion != 90 && orientacionAnterior.contains("ARRIBA")) {
                        aux = new JsonObject();
                        aux.add("action", "move");
                        aux.add("value", "rotateR");
                        ruta.add(aux);
                        orientacion += 45;
                    } else {
                        
                        while (mapa.getLevel(posx+1, posy) > posz){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            posz += 5;
                        }
                        
                        if (contadorPasos != 0 && posx < (mapa.getWidth()/2)-11 + (cuadrante*(mapa.getWidth()/2))) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
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
            
            
        }

        return ruta;
        
    }
    
    protected JsonArray calcularRutaRescuer(int posx, int posy, int posz, int energy /*Falta las variables del punto objetivo*/){
        JsonArray ruta = new JsonArray();
        return ruta;
    }
    
    
}
