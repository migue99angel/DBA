package dragonfly_caixabank;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.getJsonContentACLM;
import IntegratedAgent.IntegratedAgent;
import Map2D.Map2DGrayscale;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;

/**
 *
 * @author lcv
 */
public class Cartographer extends IntegratedAgent {

    protected YellowPages myYP;
    protected String myStatus, myService, myWorldManager, myWorld, myConvID;
    protected boolean myError;
    protected ACLMessage in, out;
    protected Map2DGrayscale myMap;

    @Override
    public void setup()   {
        // Hardcoded the only known agent: Sphinx
        _identitymanager = "Sphinx";
        super.setup();

        Info("Booting");

        // Description of my group
        myService = "Analytics group CaixaBank";

        // The world I am going to open
        myWorld = "Playground1";

        // First state of the agent
        myStatus = "CHECKIN-LARVA";

        // To detect possible errors
        myError = false;

        _exitRequested = false;
    }

    @Override
    public void plainExecute() {
        plainWithErrors();
    }
    
    @Override
    public void takeDown() {
        Info("Taking down");
        super.takeDown();
    }

    protected ACLMessage sendCheckinLARVA(String im) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setContent("");
        out.setProtocol("ANALYTICS");
        out.setEncoding(_myCardID.getCardID());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage sendCheckoutLARVA(String im) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setContent("");
        out.setProtocol("ANALYTICS");
        out.setPerformative(ACLMessage.CANCEL);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage sendSubscribeWM(String problem) {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setProtocol("ANALYTICS");
        out.setContent(new JsonObject().add("problem", problem).toString());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        this.send(out);
        return this.blockingReceive();
    }

    protected ACLMessage sendCANCELWM() {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myWorldManager, AID.ISLOCALNAME));
        out.setContent("");
        out.setConversationId(myConvID);
        out.setProtocol("ANALYTICS");
        out.setPerformative(ACLMessage.CANCEL);
        send(out);
        return blockingReceive();
    }

    protected ACLMessage queryYellowPages(String im) {
        YellowPages res = null;

        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(im, AID.ISLOCALNAME));
        out.setProtocol("ANALYTICS");
        out.setContent("");
        out.setPerformative(ACLMessage.QUERY_REF);
        this.send(out);
        return blockingReceive();
    }


    public void plainWithErrors() {
        // Basic iteration
        switch (myStatus.toUpperCase()) {
            case "CHECKIN-LARVA":
                Info("Checkin in LARVA with " + _identitymanager);
                in = sendCheckinLARVA(_identitymanager); // As seen in slides
                myError = (in.getPerformative() != ACLMessage.INFORM);
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Checkin failed due to " + getDetailsLARVA(in));
                    myStatus = "EXIT";
                    break;
                }
                myStatus = "SUBSCRIBE-WM";
                Info("\tCheckin ok");
                break;
            case "SUBSCRIBE-WM":
                Info("Retrieve who is my WM");
                // First update Yellow Pages
                in = queryYellowPages(_identitymanager); // As seen oon slides
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info("\t" + ACLMessage.getPerformative(in.getPerformative())
                            + " Query YellowPages failed due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                myYP = new YellowPages();
                myYP.updateYellowPages(in);
                // It might be the case that YP are right but we dont find an appropriate service for us, then leave
                if (myYP.queryProvidersofService(myService).isEmpty()) {
                    Info("\t" + "There is no agent providing the service " + myService);
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                // Choose one of the available service providers, i.e., the first one
                myWorldManager = myYP.queryProvidersofService(myService).iterator().next();

                // Now it is time to start the game and turn on the lights within a given world
                in = sendSubscribeWM(myWorld);
                myError = in.getPerformative() != ACLMessage.INFORM;
                if (myError) {
                    Info(ACLMessage.getPerformative(in.getPerformative())
                            + " Could not open a session with "
                            + myWorldManager + " due to " + getDetailsLARVA(in));
                    myStatus = "CHECKOUT-LARVA";
                    break;
                }
                // Keep the Conversation ID and spread it amongs the team members
                myConvID = in.getConversationId();
                // Move on to get the map
                myStatus = "PROCESS-MAP";
                break;

            case "PROCESS-MAP":
                System("Save map of world " + myWorld);
                // Examines the content of the message from server
                JsonObject jscontent = getJsonContentACLM(in);
                if (jscontent.names().contains("map")) {
                    JsonObject jsonMapFile = jscontent.get("map").asObject();
                    String mapfilename = jsonMapFile.getString("filename", "nonamefound");
                    Info("Found map " + mapfilename);
                    myMap = new Map2DGrayscale();
                    if (myMap.fromJson(jsonMapFile)) {
                        Info("Map " + mapfilename + "( " + myMap.getWidth() + "cols x" + myMap.getHeight()
                                + "rows ) saved on disk (project's root folder) and ready in memory");
                        Info("Sampling three random points for cross-check:");
                        int px, py;
                        for (int ntimes = 0; ntimes < 3; ntimes++) {
                            px = (int) (Math.random() * myMap.getWidth());
                            py = (int) (Math.random() * myMap.getHeight());
                            Info("\tX: " + px + ", Y:" + py + " = " + myMap.getLevel(px, py));
                        }
                        myStatus = "CANCEL-WM";
                    } else {
                        Info("\t" + "There was an error processing and saving the image ");
                        myStatus = "CANCEL-WM";
                        break;
                    }
                } else {
                    Info("\t" + "There is no map found in the message");
                    myStatus = "CANCEL-WM";
                    break;
                }
                myStatus = "CANCEL-WM";
                break;
            case "CANCEL-WM":
                Info("Closing the game");
                in = sendCANCELWM();
                myStatus = "CHECKOUT-LARVA";
                break;
            case "CHECKOUT-LARVA":
                Info("Exit LARVA");
                in = sendCheckoutLARVA(_identitymanager);
                myStatus = "EXIT";
                break;
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }
    
    public void plainNoErrors() {
        // Basic iteration
        switch (myStatus.toUpperCase()) {
            case "CHECKIN-LARVA":
                Info("Checkin in LARVA with " + _identitymanager);
                in = sendCheckinLARVA(_identitymanager); // As seen in slides
                myStatus = "SUBSCRIBE-WM";
                break;
            case "SUBSCRIBE-WM":
                Info("Retrieve who is my WM");
                // First update Yellow Pages
                in = queryYellowPages(_identitymanager); // As seen oon slides
                myYP = new YellowPages();
                myYP.updateYellowPages(in);
                // Choose one of the available service providers, i.e., the first one
                myWorldManager = myYP.queryProvidersofService(myService).iterator().next();

                // Now it is time to start the game and turn on the lights within a given world
                in = sendSubscribeWM(myWorld);
                // Keep the Conversation ID and spread it amongs the team members
                myConvID = in.getConversationId();
                // Move on to get the map
                myStatus = "CANCEL-WM";
                break;

            case "CANCEL-WM":
                Info("Closing the game");
                in = sendCANCELWM();
                myStatus = "CHECKOUT-LARVA";
                break;
            case "CHECKOUT-LARVA":
                Info("Exit LARVA");
                in = sendCheckoutLARVA(_identitymanager);
                myStatus = "EXIT";
                break;
            case "EXIT":
                Info("The agent dies");
                _exitRequested = true;
                break;
        }
    }

}