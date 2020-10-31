/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dragonfly_caixabank;

import AppBoot.ConsoleBoot;

public class DRAGONFLY_CAIXABANK {

    public static void main(String[] args) {
        ConsoleBoot app = new ConsoleBoot("HACKATHON", args);
        app.selectConnection();
        app.launchAgent("759431701111", MyDragonfly.class);
        app.shutDown();        
    }
    
}
