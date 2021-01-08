/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dragonfly_caixabank;

import Map2D.Map2DGrayscale;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 * Clase Listener. Intermediario entre los distintos drones.
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class Listener extends AgenteBase {
    
    protected static Map2DGrayscale mapa;
    protected boolean listening;
    protected ArrayList<JsonObject> alemanesEncontradosPrimerCuadrante = new ArrayList<>();
    protected ArrayList<JsonObject> alemanesEncontradosSegundoCuadrante = new ArrayList<>();
    protected Boolean rescuerOcupadoPrimerCuadrante = true;
    protected Boolean rescuerOcupadoSegundoCuadrante = true;
    protected Boolean rescuerPrimerCuadrantePrimeraIteracion = true;
    protected Boolean rescuerSegundoCuadrantePrimeraIteracion = true;
    protected JsonObject estadoRescuer0 = new JsonObject();
    protected JsonObject estadoRescuer1 = new JsonObject();
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "LISTENER";
        myWMProtocol = "REGULAR";
        listening = true;
        
        //Cargamos el mapa
        mapa = new Map2DGrayscale();
        try {
            mapa.loadMap("./maps/" + DRAGONFLY_CAIXABANK._filename + ".png");
        } catch (Exception ex){
            System.err.println("***ERROR "+ex.toString());
        }
    }
    
    @Override
    public void plainExecute() {   
        super.plainExecute();
        
        // Esperamos a que el Controlador nos despierte
        in = new ACLMessage();
        Info("Agente esperando a ser despertado");
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.QUERY_IF) {
            Info("Agente no ha recibido QUERY_IF");
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("Agente despertado");
            
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
        Info("Login al World Manager");
        
        enviarMensaje(myWorldManager, ACLMessage.SUBSCRIBE, myWMProtocol, new JsonObject().add(myAction, myValue).toString(), myConvId, false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en SUBSCRIBE de WM");
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("SUBSCRIBE WM OK");
        }
    } 
    
    /**
     * Se mantiene escuchando, realiza diferentes acciones dependiendo del mensaje que reciba
     */
    protected void comportamiento() {
        JsonObject aux = new JsonObject();
        JsonArray ruta = new JsonArray();
        
        Info("Listener escuchando mensajes");
        
        while(listening){
            in = blockingReceive();
            
            switch(in.getPerformative()){
                
                case ACLMessage.CANCEL:
                    listening = false;
                    break;
                
                case ACLMessage.REQUEST:
                    aux = Json.parse(in.getContent()).asObject();
                    
                    if(aux.get("type").asString().contains("SEEKER")){
                        ruta = calcularRutaSeeker(aux.get("cuadrante").asInt(), aux.get("posx").asInt(), aux.get("posy").asInt(), aux.get("altimeter").asInt(), aux.get("orientacion").asInt(), aux.get("energy").asInt());
                    }
                    
                    enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.INFORM, "REGULAR", ruta.toString(), myConvId, false);
                    ruta = new JsonArray();
                    break;
                
                case ACLMessage.QUERY_REF:
                    
                    aux = Json.parse(in.getContent()).asObject();
                    
                    JsonObject aleman = new JsonObject();
                    aleman.add("posx", aux.get("posx").asInt());
                    aleman.add("posy", aux.get("posy").asInt());
                    Boolean aniadido = false;
                    
                    if(!comprobarAleman(aleman) && aux.get("cuadrante").asInt() == 0) {
                        Info("Añadido aleman al primer cuadrante");
                        alemanesEncontradosPrimerCuadrante.add(aleman);
                        aniadido = true;
                    } else if (!comprobarAleman(aleman) && aux.get("cuadrante").asInt() == 1){
                        alemanesEncontradosSegundoCuadrante.add(aleman);
                        Info("Añadido aleman al segundo cuadrante");
                        aniadido = true;
                    } else {
                        Info("Aleman ya localizado");
                    }
                    
                    
                    if (aniadido){
                        enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
                    } else {
                        enviarMensaje(in.getSender().getName().replace("@DBA", ""), ACLMessage.DISCONFIRM, "REGULAR", "", myConvId, false);
                    }
                    
                    break;
                
                case ACLMessage.AGREE:
                    if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(0))){
                        Info("Guardando el estado del Rescuer 0");
                        
                        this.estadoRescuer0 = new JsonObject();
                        this.estadoRescuer0.add("posx", Json.parse(in.getContent()).asObject().get("posx").asInt());
                        this.estadoRescuer0.add("posy", Json.parse(in.getContent()).asObject().get("posy").asInt());
                        this.estadoRescuer0.add("energy", Json.parse(in.getContent()).asObject().get("energy").asInt());
                        this.estadoRescuer0.add("cuadrante", Json.parse(in.getContent()).asObject().get("cuadrante").asInt());
                        this.estadoRescuer0.add("altimeter", Json.parse(in.getContent()).asObject().get("altimeter").asInt());
                        this.estadoRescuer0.add("orientacion", Json.parse(in.getContent()).asObject().get("orientacion").asInt());
                        this.estadoRescuer0.add("posIniX", Json.parse(in.getContent()).asObject().get("posIniX").asInt());
                        this.estadoRescuer0.add("posIniY", Json.parse(in.getContent()).asObject().get("posIniY").asInt());
                        
                    } else if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(1))) {
                        Info ("Guardando el estado del Rescuer 1");
                        
                        this.estadoRescuer1 = new JsonObject();
                        this.estadoRescuer1.add("posx", Json.parse(in.getContent()).asObject().get("posx").asInt());
                        this.estadoRescuer1.add("posy", Json.parse(in.getContent()).asObject().get("posy").asInt());
                        this.estadoRescuer1.add("energy", Json.parse(in.getContent()).asObject().get("energy").asInt());
                        this.estadoRescuer1.add("cuadrante", Json.parse(in.getContent()).asObject().get("cuadrante").asInt());
                        this.estadoRescuer1.add("altimeter", Json.parse(in.getContent()).asObject().get("altimeter").asInt());
                        this.estadoRescuer1.add("orientacion", Json.parse(in.getContent()).asObject().get("orientacion").asInt());
                        this.estadoRescuer1.add("posIniX", Json.parse(in.getContent()).asObject().get("posIniX").asInt());
                        this.estadoRescuer1.add("posIniY", Json.parse(in.getContent()).asObject().get("posIniY").asInt());
                    }
                    
                    enviarMensaje(in.getSender().getName().replace("@DBA", ""),ACLMessage.INFORM, "REGULAR", "", myConvId, false);
                    
                    break;
                 
                
                case ACLMessage.INFORM:
                    if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(0))) {
                        Info("El Rescuer 0 ya no esta ocupado");
                        
                        if (!this.rescuerPrimerCuadrantePrimeraIteracion && !this.alemanesEncontradosPrimerCuadrante.isEmpty()){
                            this.alemanesEncontradosPrimerCuadrante.remove(0);
                        }
                        
                        rescuerOcupadoPrimerCuadrante = false;
                        String rescuerALlamar = DRAGONFLY_CAIXABANK.dronesRescuer.get(0);
                        enviarMensaje(rescuerALlamar, ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
                    } else if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(1))) {
                        Info("El Rescuer 1 ya no esta ocupado");
                        
                        if (!this.rescuerSegundoCuadrantePrimeraIteracion && !this.alemanesEncontradosSegundoCuadrante.isEmpty()){
                            this.alemanesEncontradosSegundoCuadrante.remove(0);
                        }
                        
                        rescuerOcupadoSegundoCuadrante = false;
                        String rescuerALlamar = DRAGONFLY_CAIXABANK.dronesRescuer.get(1);
                        enviarMensaje(rescuerALlamar, ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
                    }
                    break;
                    
                case ACLMessage.QUERY_IF:
                    if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(0))) {
                        Info("Devolviendo al Rescuer 0 a la posicion inicial");
                        
                        ruta = calcularRutaRescuer(this.estadoRescuer0.get("posx").asInt(), this.estadoRescuer0.get("posy").asInt(), this.estadoRescuer0.get("posIniX").asInt(),
                        this.estadoRescuer0.get("posIniY").asInt(), this.estadoRescuer0.get("energy").asInt(), this.estadoRescuer0.get("altimeter").asInt(), this.estadoRescuer0.get("orientacion").asInt(), true);    
                        
                        // Notificamos al rescuer
                        enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(this.estadoRescuer0.get("cuadrante").asInt()), ACLMessage.QUERY_REF, "REGULAR", ruta.toString(), myConvId, false);
                        this.rescuerOcupadoPrimerCuadrante = true;
                        this.estadoRescuer0 = new JsonObject();
                        ruta = new JsonArray();
                    } else if (in.getSender().getName().replace("@DBA", "").contains(DRAGONFLY_CAIXABANK.dronesRescuer.get(1))) { 
                        Info("Devolviendo al Rescuer 1 a la posicion inicial");
                        
                        ruta = calcularRutaRescuer(this.estadoRescuer1.get("posx").asInt(), this.estadoRescuer1.get("posy").asInt(), this.estadoRescuer1.get("posIniX").asInt(),
                        this.estadoRescuer1.get("posIniY").asInt(), this.estadoRescuer1.get("energy").asInt(), this.estadoRescuer1.get("altimeter").asInt(), this.estadoRescuer1.get("orientacion").asInt(), true);    
                        
                        // Notificamos al rescuer
                        enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(this.estadoRescuer1.get("cuadrante").asInt()), ACLMessage.QUERY_REF, "REGULAR", ruta.toString(), myConvId, false);
                        this.rescuerOcupadoSegundoCuadrante = true;
                        this.estadoRescuer1 = new JsonObject();
                        ruta = new JsonArray();
                    } 
            }
            
            if (!this.alemanesEncontradosPrimerCuadrante.isEmpty() && !this.rescuerOcupadoPrimerCuadrante && !this.estadoRescuer0.isEmpty()){
                Info("Calculando ruta para el Rescuer del primer cuadrante");
                
                ruta = calcularRutaRescuer(this.estadoRescuer0.get("posx").asInt(), this.estadoRescuer0.get("posy").asInt(), this.alemanesEncontradosPrimerCuadrante.get(0).asObject().get("posx").asInt(),
                        this.alemanesEncontradosPrimerCuadrante.get(0).asObject().get("posy").asInt(), this.estadoRescuer0.get("energy").asInt(), this.estadoRescuer0.get("altimeter").asInt(), this.estadoRescuer0.get("orientacion").asInt(), false);    
                
                // Notificamos al rescuer
                enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(this.estadoRescuer0.get("cuadrante").asInt()), ACLMessage.QUERY_REF, "REGULAR", ruta.toString(), myConvId, false);
                this.rescuerPrimerCuadrantePrimeraIteracion = false;
                this.rescuerOcupadoPrimerCuadrante = true;
                this.estadoRescuer0 = new JsonObject();
                ruta = new JsonArray();
                
            } else if (!this.alemanesEncontradosSegundoCuadrante.isEmpty() && !this.rescuerOcupadoSegundoCuadrante && !this.estadoRescuer1.isEmpty()){
                Info("Calculando ruta para el Rescuer del segundo cuadrante");
                
                ruta = calcularRutaRescuer(this.estadoRescuer1.get("posx").asInt(), this.estadoRescuer1.get("posy").asInt(), this.alemanesEncontradosSegundoCuadrante.get(0).get("posx").asInt(),
                        this.alemanesEncontradosSegundoCuadrante.get(0).get("posy").asInt(), this.estadoRescuer1.get("energy").asInt(), this.estadoRescuer1.get("altimeter").asInt(), this.estadoRescuer1.get("orientacion").asInt(), false);    
                
                // Notificamos al rescuer
                enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(this.estadoRescuer1.get("cuadrante").asInt()), ACLMessage.QUERY_REF, "REGULAR", ruta.toString(), myConvId, false);
                this.rescuerSegundoCuadrantePrimeraIteracion = false;
                this.rescuerOcupadoSegundoCuadrante = true;
                this.estadoRescuer1 = new JsonObject();
                ruta = new JsonArray();
            }
        }
    }
    
    /**
     * Calcula la ruta a seguir del Seeker
     * @param cuadrante Cuadrante en el que se encuentra el Seeker
     * @param posx Posición en el eje X del dron
     * @param posy Posición en el eje Y del dron
     * @param altura Altura actual del dron
     * @param orientacion Orientación actual del dron
     * @param energy Energía actual del dron
     * @return JsonArray con todos los movimientos y acciones a realizar
     */
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
                        if (!couldIRechargeThere(posx, posy, orientacion, altura-mapa.getLevel(posx, posy), energy, 1)){
                            
                            aux = new JsonObject();
                            
                            while(altura > mapa.getLevel(posx, posy)) {                   
                                aux.add("action", "move");
                                aux.add("value", "moveD");
                                altura -= 5;
                                ruta.add(aux);
                                energy-= 5;

                                aux = new JsonObject();
                            }

                            aux = new JsonObject();

                            aux.add("action", "move");
                            aux.add("action", "recharge");
                            ruta.add(aux);
                            energy = 995;
                        }
                        
                        while (mapa.getLevel(posx, posy+1)+5 > altura && ((altura+5) < DRAGONFLY_CAIXABANK.MAX_FLIGHT)){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (posy < mapa.getHeight() - 10) {
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
                            
                            aux = new JsonObject();
                            aux.add("action", "read");
                            ruta.add(aux);
                            energy -= Seeker.consumo;
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
                        if (!couldIRechargeThere(posx, posy, orientacion, altura-mapa.getLevel(posx, posy), energy, 1)){
                            
                            aux = new JsonObject();
                            
                            while(altura > mapa.getLevel(posx, posy)) {                   
                                aux.add("action", "move");
                                aux.add("value", "moveD");
                                altura -= 5;
                                ruta.add(aux);
                                energy-= 5;
                                aux = new JsonObject();
                            }

                            aux = new JsonObject();

                            aux.add("action", "move");
                            aux.add("action", "recharge");
                            ruta.add(aux);
                            energy = 995;
                        }
                        
                        while (mapa.getLevel(posx, posy-1)+5 > altura && ((altura+5) < DRAGONFLY_CAIXABANK.MAX_FLIGHT)){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (posy > 10) {
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
                            
                            aux = new JsonObject();
                            aux.add("action", "read");
                            ruta.add(aux);
                            energy -= Seeker.consumo;
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
                        if (!couldIRechargeThere(posx, posy, orientacion, altura-mapa.getLevel(posx, posy), energy, 1)){
                            
                            aux = new JsonObject();
                            
                            while(altura > mapa.getLevel(posx, posy)) {                   
                                aux.add("action", "move");
                                aux.add("value", "moveD");
                                altura -= 5;
                                ruta.add(aux);
                                energy-= 5;
                                aux = new JsonObject();
                            }

                            aux = new JsonObject();

                            aux.add("action", "move");
                            aux.add("action", "recharge");
                            ruta.add(aux);
                            energy = 995;
                        }
                        
                        while (mapa.getLevel(posx+1, posy)+5 > altura && ((altura+5) < DRAGONFLY_CAIXABANK.MAX_FLIGHT)){
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveUP");
                            ruta.add(aux);
                            energy -= 5;
                            altura += 5;
                        }
                        
                        if (contadorPasos != 0 && posx < (mapa.getWidth()/2)-10 + (cuadrante*(mapa.getWidth()/2))) {
                            aux = new JsonObject();
                            aux.add("action", "move");
                            aux.add("value", "moveF");
                            ruta.add(aux);
                            energy--;
                            posx++;
                            contadorPasos--;
                        } else {
                            if (posx >= (mapa.getWidth()/2)-10 + (cuadrante*(mapa.getWidth()/2))){
                                ultimoTramo = true;
                            }
                            
                            if (orientacionAnterior.contains("ABAJO")){
                                orientacionCamino = "ARRIBA";
                            } else {
                                orientacionCamino = "ABAJO";
                            }
                            
                            aux = new JsonObject();
                            aux.add("action", "read");
                            ruta.add(aux);
                            contadorPasos = 21;
                            energy -= Seeker.consumo;
                        }                       
                    }
                    break;
            }
            
            if(contadorPasosSinLeerSensores == 20) {
                contadorPasosSinLeerSensores = 0;
                aux = new JsonObject();
                aux.add("action", "read");
                ruta.add(aux);
                energy -= Seeker.consumo;
            } else {
                contadorPasosSinLeerSensores++;
            }
        }

        return ruta;
        
    }
    
    /**
     * Calcula la ruta a seguir del Rescuer
     * @param posIniX Posición actual en el eje X del Rescuer
     * @param posIniY Posición actual en el eje Y del Rescuer
     * @param posDestinoX Posición a la que necesita llegar el Rescuer
     * @param posDestinoY Posición en el eje Y a la que necesita llegar el Rescuer
     * @param energy Energía actual del Rescuer
     * @param altura Altura actual del Rescuer
     * @param orientacion Orientación actual del Rescuer
     * @param volviendoACasa Indica si se encuentra volviendo a su posición de inicio
     * @return JsonArray con todos los movimientos y acciones a realizar
     */
    protected JsonArray calcularRutaRescuer(int posIniX, int posIniY, int posDestinoX, int posDestinoY, int energy, int altura, int orientacion, Boolean volviendoACasa){
        JsonArray ruta = new JsonArray();
        JsonObject aux = new JsonObject();
        altura += mapa.getLevel(posIniX, posIniY);
            
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
                if (!couldIRechargeThere(posIniX, posIniY, orientacion, altura-mapa.getLevel(posIniX, posIniY), energy, 4)){
                    
                    aux = new JsonObject();
                
                    while(altura > mapa.getLevel(posIniX, posIniY)) {                   
                        aux.add("action", "move");
                        aux.add("value", "moveD");
                        altura -= 5;
                        ruta.add(aux);
                        energy-= 20;

                        aux = new JsonObject();
                    }

                    aux = new JsonObject();

                    aux.add("action", "move");
                    aux.add("action", "recharge");
                    ruta.add(aux);
                    energy = 995;
                }
                
                while (mapa.getLevel(posIniX+1, posIniY) > altura){
                    aux = new JsonObject();
                    aux.add("action", "move");
                    aux.add("value", "moveUP");
                    ruta.add(aux);
                    energy -= 20;
                    altura += 5;
                }
            } else {
                if (!couldIRechargeThere(posIniX, posIniY, orientacion, altura-mapa.getLevel(posIniX, posIniY), energy,4)){
                    
                    aux = new JsonObject();
                
                    while(altura > mapa.getLevel(posIniX, posIniY)) {                   
                        aux.add("action", "move");
                        aux.add("value", "moveD");
                        altura -= 5;
                        ruta.add(aux);
                        energy-= 20;

                        aux = new JsonObject();
                    }

                    aux = new JsonObject();

                    aux.add("action", "move");
                    aux.add("action", "recharge");
                    ruta.add(aux);
                    energy = 995;
                }
                
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

        if(posIniY < posDestinoY) {
            while(orientacion != 180) {
                aux = new JsonObject();
                   
                aux.add("action", "move");
                aux.add("value", "rotateR");
                orientacion += 45;

                energy -= 4;
                ruta.add(aux);
            }
        } else if(posIniY > posDestinoY) {
            while(orientacion != 0) {
                aux = new JsonObject();

                if(orientacion < 0) {                       
                    aux.add("action", "move");
                    aux.add("value", "rotateR");
                    orientacion += 45;
                } else if(orientacion > 0) {
                    aux.add("action", "move");
                    aux.add("value", "rotateL");
                    orientacion -= 45;
                }

                energy -= 4;
                ruta.add(aux);
            }
        }
                
        while(posIniY != posDestinoY) {
            if(orientacion == 180) {
                if (!couldIRechargeThere(posIniX, posIniY, orientacion, altura-mapa.getLevel(posIniX, posIniY), energy, 4)){
                    
                    aux = new JsonObject();
                
                    while(altura > mapa.getLevel(posIniX, posIniY)) {                   
                        aux.add("action", "move");
                        aux.add("value", "moveD");
                        altura -= 5;
                        ruta.add(aux);
                        energy-= 20;

                        aux = new JsonObject();
                    }

                    aux = new JsonObject();

                    aux.add("action", "move");
                    aux.add("action", "recharge");
                    ruta.add(aux);
                    energy = 995;
                }
                
                while (mapa.getLevel(posIniX, posIniY+1) > altura){
                    aux = new JsonObject();
                    aux.add("action", "move");
                    aux.add("value", "moveUP");
                    ruta.add(aux);
                    energy -= 20;
                    altura += 5;
                }
            } else {
                if (!couldIRechargeThere(posIniX, posIniY, orientacion, altura-mapa.getLevel(posIniX, posIniY), energy, 4)){
                    
                    aux = new JsonObject();
                
                    while(altura > mapa.getLevel(posIniX, posIniY)) {                   
                        aux.add("action", "move");
                        aux.add("value", "moveD");
                        altura -= 5;
                        ruta.add(aux);
                        energy-= 20;

                        aux = new JsonObject();
                    }

                    aux = new JsonObject();

                    aux.add("action", "move");
                    aux.add("action", "recharge");
                    ruta.add(aux);
                    energy = 995;
                }
                
                while (mapa.getLevel(posIniX, posIniY-1) > altura){
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

            if(orientacion == 180) {
                posIniY++;
            } else {
                posIniY--;
            }

            energy -= 4;
            ruta.add(aux);
        }
        
        while(altura > mapa.getLevel(posIniX, posIniY)) {
            aux = new JsonObject();
            aux.add("action", "move");
            aux.add("value", "moveD");
            altura -= 5;
            ruta.add(aux);
            energy -= 20;
        }
 
        if (!volviendoACasa){
            aux = new JsonObject();
            aux.add("action", "move");
            aux.add("value", "rescue");
            ruta.add(aux);
        
            while (orientacion != 90){
                if (orientacion < 90){
                    aux = new JsonObject();
                    aux.add("action", "move");
                    aux.add("value", "rotateR");
                    orientacion += 45;
                    ruta.add(aux);
                    energy -= 4;
                } else {
                    aux = new JsonObject();
                    aux.add("action", "move");
                    aux.add("value", "rotateL");
                    orientacion -= 45;
                    ruta.add(aux);
                    energy -= 4;
                }
            }
        } else {
            aux = new JsonObject();
            aux.add("action", "move");
            aux.add("value", "touchD");
            ruta.add(aux);
        }
        
        aux = new JsonObject();
        aux.add("action", "inform");
        aux.add("value", energy);
        ruta.add(aux);
        
        return ruta;
    }
    
    /**
     * Comprueba si el movimiento que va a realizar le va a imposibilitar la recarga
     * @param x Posición en el eje X
     * @param y Posición en el eje Y
     * @param ang Orientación actual del dron
     * @param alturaDron Altura actual del dron
     * @param energia Energía actual del dron
     * @param multiplicador Los Rescuer consumen cuatro veces más energía que los Seeker
     * @return Si se puede mover sin recargar o no
     */
    protected boolean couldIRechargeThere(int x, int y, int ang, int alturaDron, int energia, int multiplicador) {
        int lookingAlt = lookingAltitude(x, y, ang);
        int downAlt = 0;
        int upAlt = 0;
        int wasteEnergyToForward = 1*multiplicador;
        int wasteEnergyToDown = 5;
        int wasteEnergyToUp = 0;
        boolean couldI = false;
        
        if (lookingAlt == mapa.getLevel(x, y)) {
            downAlt = alturaDron;
        } else if (lookingAlt < mapa.getLevel(x, y)) {
            downAlt = alturaDron + (mapa.getLevel(x, y) - lookingAlt);
        } else {
            if (lookingAlt > mapa.getLevel(x, y) + alturaDron) {
                upAlt = lookingAlt - (alturaDron + mapa.getLevel(x, y));
            } else {
                downAlt = (alturaDron + mapa.getLevel(x, y)) - lookingAlt;
            }
        }
        
        wasteEnergyToUp = upAlt*multiplicador;
        wasteEnergyToDown = downAlt*multiplicador;
        
        if ((energia-100) - (wasteEnergyToUp + wasteEnergyToForward + wasteEnergyToDown) > 0) {
            couldI = true;
        }
        
        return couldI;
    }
    
    /**
     * Altura que tenemos justo en frente
     * @param x Posición en el eje X del dron
     * @param y Posición en el eje Y del dron
     * @param ang Orientación actual del dron
     * @return La altura de la siguiente casilla a la que estamos mirando
     */
    protected int lookingAltitude(int x, int y, int ang) {
        int lookingAlt = 0;
        switch (ang)
        {
            // +++++ . +++++
            // ++X++ . +   +
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            case -45:
                lookingAlt = mapa.getLevel(x-1, y-1);
                break;
                
            // +++++ . +++++
            // +   + X +   +
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++  
            case 0:
                lookingAlt = mapa.getLevel(x, y-1);
                break;
                
            // +++++ . +++++
            // +   + . ++X++
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            case 45:
                lookingAlt = mapa.getLevel(x+1, y-1);
                break;
                
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            // ..X...0......
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++ 
            case -90:
                lookingAlt = mapa.getLevel(x-1, y);
                break;
                
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            // ......0...X..
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++     
            case 90:
                lookingAlt = mapa.getLevel(x+1, y);
                break;
                
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // ++X++ . +   +
            // +++++ . +++++    
            case -135:
                lookingAlt = mapa.getLevel(x-1, y+1);
                break;
            
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // +   + X +   +
            // +++++ . +++++  
            case 180:
                lookingAlt = mapa.getLevel(x, y+1);
                break;
                
            // +++++ . +++++
            // +   + . +   +
            // +++++ . +++++
            // ......0......
            // +++++ . +++++
            // +   + . ++X++
            // +++++ . +++++     
            case 135:
                lookingAlt = mapa.getLevel(x+1, y+1);
                break;
            }
        
        return lookingAlt;
    }
    
    /**
     * Comprueba si un alemán ya ha sido detectado
     * @param alemanDetectado El alemán a comprobar
     * @return True si el alemán ya está localizado
     */
    protected Boolean comprobarAleman(JsonObject alemanDetectado){
        Boolean alemanYaDetectado = false;
 
        for (int i=0; i < this.alemanesEncontradosPrimerCuadrante.size() && !alemanYaDetectado; i++) {
            if (this.alemanesEncontradosPrimerCuadrante.get(i).asObject().get("posx").asInt() == alemanDetectado.get("posx").asInt()
                    && this.alemanesEncontradosPrimerCuadrante.get(i).asObject().get("posy").asInt() == alemanDetectado.get("posy").asInt()) {
                alemanYaDetectado = true;
            }
        }
        
        for (int i=0; i < this.alemanesEncontradosSegundoCuadrante.size() && !alemanYaDetectado; i++) {
            if (this.alemanesEncontradosSegundoCuadrante.get(i).asObject().get("posx").asInt() == alemanDetectado.get("posx").asInt()
                    && this.alemanesEncontradosSegundoCuadrante.get(i).asObject().get("posy").asInt() == alemanDetectado.get("posy").asInt()) {
                alemanYaDetectado = true;
            }
        }
        
        return alemanYaDetectado;
    }
}
