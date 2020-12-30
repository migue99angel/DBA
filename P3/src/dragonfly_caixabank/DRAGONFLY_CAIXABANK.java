package dragonfly_caixabank;

import AppBoot.ConsoleBoot;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Clase para ejecutar lanzar el agente
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class DRAGONFLY_CAIXABANK {
    
    protected static ArrayList<String> dronesSeeker = new ArrayList<>();
    protected static JsonArray posicionesSeeker = new JsonArray();
    protected static ArrayList<String> dronesRescuer = new ArrayList<>();
    protected static JsonArray posicionesRescuer = new JsonArray();
    protected static ArrayList<String> dronesListener = new ArrayList<>();
    protected static String dronControlador = "CaixaBank_Controlador";
    protected static String agenteAwacs = "Agente_Awacs";
    
    //Los alemanes a rescatar por mundo
    protected static final int alemanes = 10;
    protected static String _filename="Playground1";

    public static void main(String[] args) {
        
        dronesSeeker.add("CaixaBank_Seeker0");
        dronesSeeker.add("CaixaBank_Seeker1");
        
        dronesRescuer.add("CaixaBank_Rescuer0");
        dronesRescuer.add("CaixaBank_Rescuer1");
        
        dronesListener.add("CaixaBank_Listener0");

        
        ConsoleBoot app = new ConsoleBoot("HACKATHON", args);
        app.selectConnection();
        
        app.launchAgent(agenteAwacs, Awacs.class);
        
        app.launchAgent(dronControlador, Controlador.class);
        
        for(int i=0; i < dronesSeeker.size(); i++) {
            app.launchAgent(dronesSeeker.get(i), Seeker.class);
        }
        
        for(int i=0; i < dronesRescuer.size(); i++) {
            app.launchAgent(dronesRescuer.get(i), Rescuer.class);
        }
        
        for(int i=0; i < dronesListener.size(); i++) {
            app.launchAgent(dronesListener.get(i), Listener.class);
        }
        
        app.shutDown();
    }
    
}
