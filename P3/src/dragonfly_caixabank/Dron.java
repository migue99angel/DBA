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
public abstract class Dron extends AgenteBase{
    
    protected JsonArray myCoins;
    protected ArrayList<String> sensoresRequeridos = new ArrayList<>();
    protected ArrayList<JsonObject> productos = new ArrayList<>();
    protected JsonArray mySensors = new JsonArray();
    protected ArrayList<String> myRechargeTickets = new ArrayList<>();
    protected int posx = 0;
    protected int posy = 0;
    protected int posz = 0;
    protected int posInix = 0;
    protected int posIniy = 0;
    protected int orientacion = 90;
    protected int cuadrante;
    protected int energy = 995;
    protected String movimiento;
    
    @Override
    public void setup() {
        super.setup();
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
            
            // Esperar entrada en el mundo
            esperarEntradaMundo();
            
            // Entramos al mundo
            entrarAlMundo(posx, posy);
            
            // Búsqueda de los alemanes
            comportamiento();
            
            // Cerrar la puerta
            logout();
            
            // Avisamos al Controlador de que nos deslogueamos
            enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.CANCEL, "REGULAR", "", myConvId, false);
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
        // Enviamos mensaje al controlador
        enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.INFORM, "REGULAR", "", myConvId, false);

        in = blockingReceive();

        if(in.getPerformative() != ACLMessage.CONFIRM) {
            Info("El controlador no confirma el mensaje de " + in.getSender());
            Info(in.getContent());
            Info(Integer.toString(in.getPerformative()));

            abortSession();
        } else {
            Info("El controlador confirma el mensaje de " + in.getSender());
        }
            
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
        if(tiendas.isEmpty()) {
            Info("No se ha encontrado ninguna tienda");
        }
        
        for(int i=0; i < tiendas.size(); i++) {
            JsonObject aux = new JsonObject();
            JsonArray referencias;
            JsonArray productosAux = new JsonArray();
            
            enviarMensaje(tiendas.get(i), ACLMessage.QUERY_REF, "REGULAR", aux.toString(), myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.INFORM) {
                Info("Error al consultar en la tienda " + tiendas.get(i));
                Info(in.getContent());
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                Info("Tienda " + tiendas.get(i) + " consultada con éxito");
                referencias = Json.parse(in.getContent()).asObject().get("products").asArray();
                
                for(int j=0; j < referencias.size(); j++) {
                    for(int k=0; k < sensoresRequeridos.size(); k++) {
                        if(referencias.get(j).asObject().get("reference").toString().contains(sensoresRequeridos.get(k))) {
                            comprobarProducto(referencias.get(j).asObject(), tiendas.get(i), sensoresRequeridos.get(k));
                        }
                    }
                }
            }
        }
        
        // Compramos los sensores
        for(int i=0; i < productos.size(); i++) {
            JsonObject auxProducto = new JsonObject();
            JsonArray auxCoins = new JsonArray();
            int precio = productos.get(i).get("price").asInt();
            
            while(precio > 0) {
                if(myCoins.size() < precio) {
                    Info("No tenemos dinero para hacer compras. Abortando sesión.");

                    abortSession();
                }
                
                auxCoins.add(myCoins.get(0));
                myCoins.remove(0);
                precio--;
            }
            
            auxProducto.add("operation", "buy");
            auxProducto.add("reference", productos.get(i).get("reference"));
            auxProducto.add("payment", auxCoins);
            
            Info("Tienda: " + productos.get(i).get("tienda").asString());
            enviarMensaje(productos.get(i).get("tienda").asString(), ACLMessage.REQUEST, "REGULAR", auxProducto.toString(), myConvId, false);
            
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.INFORM) {
                Info("Error al realizar la compra del sensor " + productos.get(i));
                Info(in.getContent());
                Info(Integer.toString(in.getPerformative()));

                abortSession();
            } else {
                
                if (Json.parse(in.getContent()).asObject().get("reference").toString().contains("CHARGE")){
                    Info("Compra del ticket de recarga " + productos.get(i) + " realizada con éxito");
                    Info(in.getContent());
                    myRechargeTickets.add(Json.parse(in.getContent()).asObject().get("reference").asString());
                } else {
                    Info("Compra del sensor " + productos.get(i) + " realizada con éxito");
                    Info(in.getContent());
                    mySensors.add(Json.parse(in.getContent()).asObject().get("reference"));
                }
            }
        }
        
        // Confirmo al controlador que ya he terminado de comprar
        enviarMensaje(DRAGONFLY_CAIXABANK.dronControlador, ACLMessage.CONFIRM, "REGULAR", "", myConvId, false);
    }
    
    protected void esperarEntradaMundo() {
        Info("Esperando autorización para entrar al mundo");
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.CONFIRM) {
            Info("No se me ha concedido la entrada al mundo");
            abortSession();
        } else{
            this.cuadrante = Json.parse(in.getContent()).asObject().get("cuadrante").asInt();
            this.posx = Json.parse(in.getContent()).asObject().get("posx").asInt();
            this.posy = Json.parse(in.getContent()).asObject().get("posy").asInt();
            this.posInix = posx;
            this.posIniy = posy;
        }
    }
    
    protected void entrarAlMundo(int posx, int posy) {
        JsonObject content = new JsonObject();
        content.add("operation", "login");
        content.add("attach", mySensors);
        content.add("posx", posx);
        content.add("posy", posy);
        
        enviarMensaje(myWorldManager, ACLMessage.REQUEST, "REGULAR", content.toString(), myConvId, false);
        
        in = blockingReceive();
        
        if(in.getPerformative() != ACLMessage.INFORM) {
            Info("WM no me deja entrar al mundo");
            abortSession();
        } else {
            Info("WM me deja entrar al mundo");
            Info(in.getContent());
        }
    }
    
    protected void comprobarProducto(JsonObject o, String tienda, String tipo) {
        boolean existe = false;
        
        // Comprobamos si existe el producto
        for(int i=0; i < productos.size() && !existe; i++) {
            if(productos.get(i).get("reference").toString().contains(tipo)) {
                // Dejamos el más barato
                if(o.get("price").asInt() < productos.get(i).get("price").asInt()) {
                    o.add("tienda", tienda);
                    productos.remove(i);
                    productos.add(o);
                }
                
                existe = true;
            }
        }
        
        if(!existe) {
            o.add("tienda", tienda);
            productos.add(o);
        }
    }
    
    protected void recargar(){
        JsonObject aux = new JsonObject();
        movimiento = "recharge";
        aux.add("operation", movimiento);
        Info("Vamos a recargar)");
        
        if (myRechargeTickets.isEmpty()) {
        
            this.productos.clear();
            this.sensoresRequeridos.clear();
            this.sensoresRequeridos.add("CHARGE");
            Info("Lista de la compra: " + this.sensoresRequeridos);
            this.hacerCompras();
        }
        
        aux.add("recharge", myRechargeTickets.get(0));
        myRechargeTickets.remove(0);
        
        enviarMensaje(myWorldManager, ACLMessage.REQUEST, "REGULAR", aux.toString(), myConvId, false);
        in = blockingReceive();
        
        if (in.getPerformative() != ACLMessage.INFORM){
            Info("Fallo al realizar " + movimiento);
            Info(Integer.toString(in.getPerformative()));
            Info(in.getContent());
            abortSession();
        } else{
            Info("Recarga realizada con exito");
            Info(in.getContent());
            
        }
    }
    
    protected void seguirRuta(JsonArray ruta) {
        JsonObject aux = new JsonObject();
        
        for (int i=0; i < ruta.size(); i++){
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
    
    protected void leerSensores(){
        JsonObject aux = new JsonObject();
        movimiento = "read";
        aux.add("operation", movimiento);
        enviarMensaje(myWorldManager, ACLMessage.QUERY_REF, "REGULAR", aux.toString(), myConvId, false);
        
        in = blockingReceive();
        
        if (in.getPerformative() != ACLMessage.INFORM){
            Info("Fallo al realizar " + movimiento);
            Info(in.toString());
            Info(Integer.toString(in.getPerformative()));
            Info(in.getContent());
            abortSession();
        } else {
            Info("Lectura de sensores realizada con exito");
            Info(in.getContent()); 
            JsonArray arrayAux = new JsonArray();
            arrayAux = Json.parse(in.getContent()).asObject().get("details").asObject().get("perceptions").asArray();
        
            for(int i = 0; i < arrayAux.size(); i++ )
            {
                lecturaSensoresConcretos(arrayAux.get(i).asObject());
            }
        }
    }
    
    protected abstract void lecturaSensoresConcretos(JsonObject o);
    
    protected abstract void comportamiento();
}
