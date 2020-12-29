/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dragonfly_caixabank;

import com.eclipsesource.json.JsonObject;
import jade.lang.acl.ACLMessage;

/**
 *
 * @author mumo
 */
public class Rescuer extends Dron{
    
    @Override
    public void setup() {
        super.setup();
        myAction = "type";
        myValue = "RESCUER";
        myWMProtocol = "REGULAR";
        
        // Rellenamos el array con los sensores que queremos
        sensoresRequeridos.add("ALIVE");
        sensoresRequeridos.add("GPS");
        sensoresRequeridos.add("CHARGE");
        
    }
    
    public void comportamiento(){

    }
    
     public void leerSensores(){
         
     }
    
}
