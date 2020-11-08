package dragonfly_caixabank;

import AppBoot.ConsoleBoot;

/**
 * Clase para ejecutar lanzar el agente
 * @version 1.0
 * @author Francisco Domínguez Lorente
 * @author José María Gómez García
 * @author Miguel Muñoz Molina
 * @author Miguel Ángel Posadas Arráez
 */
public class DRAGONFLY_CAIXABANK {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("HACKATHON", args);
        app.selectConnection();
        app.launchAgent("7594314", MyDragonfly.class);
        app.shutDown();        
    }
    
}
