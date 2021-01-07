package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;


public class Controlador extends AgenteBase {
    private int contador = 0;
    private int alemanesRescatados = 0;
    private int height = 0;
    private int width = 0;
    private final int radioThermal = 10;
    private JsonArray posicionSeekers = new JsonArray();
    private JsonArray posicionRescuers = new JsonArray();
    private ArrayList<ACLMessage> mensajesRecibidos = new ArrayList<>();
    
    @Override
    public void setup() {
        super.setup();
        
        myAction = "problem";
        myValue = DRAGONFLY_CAIXABANK._filename;
        myWMProtocol = "ANALYTICS";
        contador = DRAGONFLY_CAIXABANK.dronesSeeker.size() +
                DRAGONFLY_CAIXABANK.dronesRescuer.size();
        
        mensajesRecibidos = new ArrayList<>();
    }
    
    @Override
    public void plainExecute() {       
        super.plainExecute();
        
        loginWorldManager();

        despertarAgentes();
        
        autorizarCompra();
        
        calcularPosicionesIniciales();
        
        // Autorizar la entrada al mundo
        autorizarEntradaMundo();
        
        while (Rescuer.alemanesEncontrados < DRAGONFLY_CAIXABANK.alemanes || contador > 0) {
            String agenteConversacion = "";
            
            if (!mensajesRecibidos.isEmpty()) {
                in = mensajesRecibidos.get(0);
                mensajesRecibidos.remove(0);
            }
            else {
                in = blockingReceive(2000);
            }
            
            if (in != null) {
                switch(in.getPerformative()) {
                    case ACLMessage.INFORM:
                        agenteConversacion = in.getSender().getName();
                        Info("El agente " + agenteConversacion + " solicita comprar una recarga");
                        enviarMensaje(agenteConversacion.replace("@DBA", ""), ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
                        Info("Confirmamos la peticición de compra de recarga al agente " + agenteConversacion);
                        Info("Esperando la confirmación de compra de recarga del agente " + agenteConversacion);
                        do {
                            in = new ACLMessage();
                            in = blockingReceive();

                            if (in.getPerformative() == ACLMessage.CONFIRM && in.getSender().getName().equals(agenteConversacion)) {
                                Info("El agente " + agenteConversacion + " ha confirmado la compra de recarga");
                            }
                            else {
                                mensajesRecibidos.add(in);
                                Info("Mensaje no esperado archivado en la cola de mensajes");
                            }
                        } while (!in.getSender().getName().equals(agenteConversacion));
                        break;
                    case ACLMessage.CANCEL:
                        Info("El agente " + in.getSender() + " avisa que se ha deslogueado");
                        contador--;
                        break;
                    default:
                        Info("No puedo manejar el mensaje recibido: " + in);
                        break;
                }
            }
        }
        /*
        // Recibir la señal de cada agente y desloguearlos
        while(contador > 0) {
            in = new ACLMessage();
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.CANCEL) {
                Info("El agente " + in.getSender() + " no se ha podido desloguear correctamente");
                abortSession();
            } else {
                Info("El agente " + in.getSender() + " avisa que se ha deslogueado");
                contador--;
            }
        }*/
        
        enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(0), ACLMessage.CANCEL, "REGULAR", "", myConvId, false);
        in = blockingReceive();
        if(in.getPerformative() != ACLMessage.CONFIRM) {
            Info("El agente " + in.getSender() + " no se ha podido desloguear correctamente");
            abortSession();
        } else {
            Info("El agente " + in.getSender() + " avisa que se ha deslogueado");
        }

        // Cerrar la puerta
        logout();
        
        takeDown();
        _exitRequested = true;
    }
    
    protected void despertarAgentes() {
        Info("Controlador despertando a los agentes");
        
        // Despertando AWACS
        enviarMensaje(DRAGONFLY_CAIXABANK.agenteAwacs, ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
        
        // Despertando a los Seeker
        for(int i=0; i < DRAGONFLY_CAIXABANK.dronesSeeker.size(); i++) {
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesSeeker.get(i), ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.CONFIRM) {
                Info("Agente " + DRAGONFLY_CAIXABANK.dronesSeeker.get(i) + " ha fallado");
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                Info("Comunicación con Agente " + DRAGONFLY_CAIXABANK.dronesSeeker.get(i) + " confirmada");
            }
        }
        
        // Despertando a los Listener
        for(int i=0; i < DRAGONFLY_CAIXABANK.dronesListener.size(); i++) {
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesListener.get(i), ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.CONFIRM) {
                Info("Agente " + DRAGONFLY_CAIXABANK.dronesListener.get(i) + " ha fallado");
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                Info("Comunicación con Agente " + DRAGONFLY_CAIXABANK.dronesListener.get(i) + " confirmada");
                height = Json.parse(in.getContent()).asObject().get("height").asInt();
                width = Json.parse(in.getContent()).asObject().get("width").asInt();
            }
        }
        
        // Despertando a los Rescuer
        for(int i=0; i < DRAGONFLY_CAIXABANK.dronesRescuer.size(); i++) {
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(i), ACLMessage.QUERY_IF, "REGULAR", "", myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.CONFIRM) {
                Info("Agente " + DRAGONFLY_CAIXABANK.dronesRescuer.get(i) + " ha fallado");
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                Info("Comunicación con Agente " + DRAGONFLY_CAIXABANK.dronesRescuer.get(i) + " confirmada");
            }
        }
    }
    
    protected void autorizarCompra() {
        int contadorCopia = DRAGONFLY_CAIXABANK.dronesSeeker.size() +
                DRAGONFLY_CAIXABANK.dronesRescuer.size();
        ArrayList<String> ordenCompra = new ArrayList<>();
        
        while(contadorCopia > 0) {
            in = new ACLMessage();
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.INFORM) {
                Info("No se ha podido procesar la petición de compra de " + in.getSender());
                abortSession();
            } else {
                Info("Recibimos petición de compra de " + in.getSender());
                contadorCopia--;
                ordenCompra.add(in.getSender().getName().replace("@DBA", ""));
            }
        }
        
        for(int i=0; i < ordenCompra.size(); i++) {
            enviarMensaje(ordenCompra.get(i), ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
            
            in = new ACLMessage();
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.CONFIRM) {
                Info("Agente " + in.getSender() + " no confirma la compra");
                abortSession();
            } else {
                Info("Agente " + in.getSender() + " confirma la compra");
            }
        }
    }
    
    protected void autorizarEntradaMundo() {
        
        for(int i=0; i < DRAGONFLY_CAIXABANK.dronesSeeker.size(); i++) {
            Info("Autorizando entrada a mundo de " + DRAGONFLY_CAIXABANK.dronesSeeker.get(i));
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesSeeker.get(i), ACLMessage.CONFIRM, "REGULAR", posicionSeekers.get(i).toString(), myConvId, false);
        }
        for(int i=0; i < DRAGONFLY_CAIXABANK.dronesRescuer.size(); i++) {
            Info("Autorizando entrada a mundo de " + DRAGONFLY_CAIXABANK.dronesRescuer.get(i));
            enviarMensaje(DRAGONFLY_CAIXABANK.dronesRescuer.get(i), ACLMessage.CONFIRM, "REGULAR", posicionRescuers.get(i).toString(), myConvId, false);
        }
    }
    
    @Override
    protected void loginWorldManager() {
        Info("Login " + getAID());
        
        enviarMensaje(myWorldManager, ACLMessage.SUBSCRIBE, myWMProtocol, new JsonObject().add(myAction, myValue).toString(), "", false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en SUBSCRIBE de WM de Agente " + getAID());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("SUBSCRIBE WM OK de Agente " + getAID());
            myConvId = in.getConversationId();
            Info("WM: " + in.getContent());
        }
    }
    
    protected void calcularPosicionesIniciales(){
        JsonObject aux = new JsonObject();
        aux.add("cuadrante",0);
        aux.add("posx",0 + radioThermal);
        aux.add("posy",0 + radioThermal);
        posicionSeekers.add(aux);
        
        aux = new JsonObject();
        aux.add("cuadrante",1);
        aux.add("posx",(width/2) + radioThermal);
        aux.add("posy",0 + radioThermal);
        posicionSeekers.add(aux);
        
        aux = new JsonObject();
        aux.add("cuadrante",0);
        aux.add("posx",width/4);
        aux.add("posy",height/2);
        posicionRescuers.add(aux);
        
        aux = new JsonObject();
        aux.add("cuadrante",1);
        aux.add("posx",3 * (width/4));
        aux.add("posy",height/2);
        posicionRescuers.add(aux);
    }
    
    @Override
    protected void logout() {
        Info("Requesting logout to " + myWorldManager);
        
        enviarMensaje(myWorldManager, ACLMessage.CANCEL, "ANALYTICS", "", myConvId, false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error en CANCEL de WM de Agente " + getAID());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));
            
            abortSession();
        } else {
            Info("Logout WM OK de Agente " + getAID());
        }
        
        super.logout();
    }
}
