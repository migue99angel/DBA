package dragonfly_caixabank;

import AppBoot.ConsoleBoot;
import java.util.ArrayList;

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
    protected static ArrayList<String> dronesRescuer = new ArrayList<>();
    protected static ArrayList<String> dronesListener = new ArrayList<>();
    protected static String dronControlador = "CaixaBank_Controlador";
    protected static String agenteAwacs = "Agente_Awacs";

    public static void main(String[] args) {
        dronesSeeker.add("CaixaBank_Seeker0");
        dronesSeeker.add("CaixaBank_Seeker1");
        
        ConsoleBoot app = new ConsoleBoot("HACKATHON", args);
        app.selectConnection();
        
        app.launchAgent(agenteAwacs, Awacs.class);
        
        app.launchAgent(dronControlador, Controlador.class);
        
        for(int i=0; i < dronesSeeker.size(); i++) {
            app.launchAgent(dronesSeeker.get(i), Seeker.class);
        }
        
        app.shutDown();
    }
    
}
