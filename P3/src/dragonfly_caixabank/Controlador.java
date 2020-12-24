package dragonfly_caixabank;

import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;


public class Controlador extends AgenteBase {
    private int contador = 0;
    
    @Override
    public void setup() {
        super.setup();
        myAction = "problem";
        myValue = "Playground1";
        myWMProtocol = "ANALYTICS";
        contador = DRAGONFLY_CAIXABANK.dronesListener.size() + DRAGONFLY_CAIXABANK.dronesSeeker.size() +
                DRAGONFLY_CAIXABANK.dronesRescuer.size();
    }
    
    @Override
    public void plainExecute() {       
        super.plainExecute();
        
        loginWorldManager();
        despertarAgentes();
        
        // Recibir la señal de cada agente y desloguearlos
        while(contador > 0) {
            in = new ACLMessage();
            in = blockingReceive();
            
            if(in.getPerformative() != ACLMessage.INFORM) {
                Info("El agente " + in.getSender() + " no se ha podido desloguear correctamente");
                abortSession();
            } else {
                Info("El agente " + in.getSender() + " avisa que se ha deslogueado");
                contador--;
            }
        }
        
        // Cerrar la puerta
        logout();
        
        takeDown();
        _exitRequested = true;
    }
    
    protected void despertarAgentes() {
        Info("Controlador despertando a los agentes");
        
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
        }
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
