package dragonfly_caixabank;

import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import YellowPages.YellowPages;
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


public class Controlador extends AgenteBase {  
    
    @Override
    public void setup() {
        super.setup();
        
    }
    
    @Override
    public void plainExecute() {       
        loginSphinx();
        loginWorldManager();
        despertarAgentes();
        
        // Recibir la señal de cada agente y desloguearlos
        // Cerrar la puerta
        logout();
        
        takeDown();
        _exitRequested = true;
    }
    
    @Override
    protected void takeDown() {
        super.takeDown();
    }
    
    protected void despertarAgentes() {
        out = new ACLMessage();
        
    }
}
