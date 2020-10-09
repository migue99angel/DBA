package dragonfly_caixabank;

import IntegratedAgent.IntegratedAgent;
import jade.core.AID;
import jade.lang.acl.ACLMessage;

public class MyDragonfly extends IntegratedAgent{
    
    String receiver;
    @Override
    public void setup() {
        super.setup();
        receiver = "";
        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        
        
        ACLMessage out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(receiver,AID.ISLOCALNAME));
        out.setContent("Hello");
        this.sendServer(out);
        
        ACLMessage in = this.blockingReceive();
        String answer = in.getContent();
        Info("Respuesta"+answer);
        String reply = new StringBuilder(answer).reverse().toString();
        out = in.createReply();
        out.setContent(reply);
        this.send(out);

     
        _exitRequested = true;
    }
        


    @Override
    protected void takeDown() {
        Info("Bye World!");
        super.takeDown();
    }
    
}
