package dragonfly_caixabank;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

public class Seeker extends AgenteBase {
    
    protected JsonArray myCoins;
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "SEEKER";
        myWMProtocol = "REGULAR";
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
            
            // Informamos al Controlador
            enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
            
            myConvId = in.getConversationId();
            loginWorldManager();
            
            // Compra de sensores
            hacerCompras();
            
            // Cerrar la puerta
            logout();
            
            // Avisamos al Controlador de que nos deslogueamos
            enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.INFORM, "REGULAR", "", myConvId, false);
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
            myCoins = Json.parse(in.getContent()).asObject().get("coins").asArray();
        }
    }
    
    protected void hacerCompras() {
        // Actualizamos las Yellow Pages
        enviarMensaje(_identitymanager, ACLMessage.QUERY_REF, "ANALYTICS", "", mySphinxConvId, false);
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("Error al actualizar las Yellow Pages para comprar");
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("Actualizando las Yellow Pages");
            yp.updateYellowPages(in);
            tiendas = new ArrayList(yp.queryProvidersofService("shop@" + myConvId));
        }
        
        // Primero pedimos el inventario a las tiendas
        for(int i=0; i < tiendas.size(); i++) {
            JsonObject aux = new JsonObject();
            enviarMensaje(tiendas.get(i), ACLMessage.QUERY_REF, "REGULAR", aux.toString(), myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.INFORM) {
                Info("Error al consultar en la tienda " + tiendas.get(i));
                Info(in.getContent());
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                Info("Tienda " + tiendas.get(i) + " consultada con éxito");
                // Elegir el sensor más barato de todas las tiendas y comprarlo
            }
        }
        
    }
}
