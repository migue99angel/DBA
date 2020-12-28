package dragonfly_caixabank;

import static ACLMessageTools.ACLMessageTools.getDetailsLARVA;
import static ACLMessageTools.ACLMessageTools.isJsonString;
import ConsoleAnsi.ConsoleAnsi;
import static ConsoleAnsi.ConsoleAnsi.black;
import static ConsoleAnsi.ConsoleAnsi.defBackground;
import static ConsoleAnsi.ConsoleAnsi.defText;
import static ConsoleAnsi.ConsoleAnsi.graphite;
import static ConsoleAnsi.ConsoleAnsi.gray;
import static ConsoleAnsi.ConsoleAnsi.lightblue;
import static ConsoleAnsi.ConsoleAnsi.lightgreen;
import static ConsoleAnsi.ConsoleAnsi.lightmagenta;
import static ConsoleAnsi.ConsoleAnsi.lightred;
import static ConsoleAnsi.ConsoleAnsi.red;
import static ConsoleAnsi.ConsoleAnsi.white;
import ControlPanel.TTYControlPanel;
import IntegratedAgent.IntegratedAgent;
import Map2D.Map2DGrayscale;
import Map2D.Palette;
import static PublicKeys.KeyGen.getKey;
import World.Thing;
import World.liveBot;
import YellowPages.YellowPages;
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Class that implements AWACS, a display service of any session session by registering 
 * the agent as a legal listener of that session. In order to do that the following process 
 * must be followed. Since this agent is intended to co-exist wiht student's own agents, it
 * has been designed to be minimally disruptive.
 *   - First, integrate the .java file within your project. Please consider defining an external package
 *   - Second, launch it as any other agent. It will enter into a sleep mode waiting for you to wake it up
 *   - Third, wake it up by sending it any message with the ConversationID that you aim to display
 *   - It wakes up, gather connection information from you files in the project, checks in with Sphinx, and subscribe with your assigned world manager as a listener
 *   - Fourth, it updates all the information from the session's radio and display the map, the drones, and all the information available from any drone, which had been broadcast thru the radio
 *   - When the session is closed by the student's agents, it is announced also in the radio, AWACS detects it and closes autonomously.
 *   - If for some reason the closing of the session is not announced in the radio, you can send AWACS an auto-destroy message with just any message that contains a CANCEL performative
 * 
 * @author lcv
 */
public class Awacs extends IntegratedAgent {

    public final int BACKGR = graphite, FOREGR = white, DIALFRG = lightgreen, DIALBGR = black, H = 13, W = 50;

    protected String myService, myServiceProvider, myStatus = "", myConvID = "", myMap = "";
    protected String reqReplyWith = "";
    protected ACLMessage in, out;
    protected JsonObject jsContent;
    protected Map2DGrayscale myHeightMap;
    protected YellowPages myYellowPages;
    protected ConsoleAnsi myWorld, myTable;
    protected HashMap<String, liveBot> knownDrones;
    protected int wshiftx = 6, wshifty = 6, tshiftx = 1, tshifty = 1;
    protected Palette palette = new Palette().intoBW(256);
    protected int droneColors[];
    private int ligthred;

    @Override
    public void setup() {
        _identitymanager = "Sphinx";
        super.setup();
        knownDrones = new HashMap();
        _exitRequested = false;
        myStatus = "WAIT_TO_WAKEUP"; //WAIT_TO_WAKEUP";
        droneColors = new int[]{lightred, lightgreen, lightblue, lightmagenta};
        // System("Starting execution");
    }

    @Override
    public void plainExecute() {
        switch (this.myStatus.toUpperCase()) {
            case "WAIT_TO_WAKEUP":
                // System("Sleeps while someone else opens the world world,\n"
//                        + "To wake up please send me any performative with an\n"
//                        + "appropriate ConversationID");
                in = blockingReceive();

                myStatus = "GATHER";
                break;
            case "GATHER":
//                myConvID = "SESSION#8eo3e";
                myConvID = in.getConversationId();
                if (!doGatherData()) {
                    myStatus = "EXIT";
                } else {
                    myStatus = "CHECKIN";
                }
                break;
            case "CHECKIN":
                // System("Waking up and registering as LISTENER into WM " + myServiceProvider);
                if (!doCheckinLARVA() || !doSubscribeWM()) {
                    myStatus = "EXIT";
                    break;
                } else {
                    myStatus = "LISTEN";
                }
                break;
            case "LISTEN":
                if (!ReceiveAndShowTerminal()) {
                    myStatus = "EXIT";
                }
                break;
            case "EXIT":
            default:
                // System("Ending execution");
                this.doCheckoutLARVA();
                // Close consoles
                if (myWorld != null){
                    myWorld.waitToClose();                    
                }
                if (myTable !=  null){
                    myTable.close();
                }
                _exitRequested = true;
                break;
        }
    }

    protected boolean ReceiveAndShowTerminal() {
        boolean res = false;
//        in = blockingReceive();
        // Filter out
//        in = this.blockingReceive(MessageTemplate.MatchProtocol("BROADCAST"));
    in = blockingReceive();
        if (in.getPerformative() == ACLMessage.CANCEL){
            return false;
        }
        if (in.getProtocol() == null || !in.getProtocol().equals("BROADCAST"))
            return true;
        String multiline = in.getContent(), line;
        Scanner mys = new Scanner(new ByteArrayInputStream(multiline.getBytes()));
        while (mys.hasNextLine()) {
            line = mys.nextLine();
            //Info(line);
            if (isJsonString(line)) {
                try {
                    jsContent = Json.parse(line).asObject();
                    if (jsContent.names().contains("drones")) {
                        // Info("Received drone update");
                        JsonArray jsaDrones = jsContent.get("drones").asArray();
                        for (JsonValue jsvd : jsaDrones) {
                            this.updateDrone(jsvd.asObject());
                        }
                    }
                    if (jsContent.names().contains("map")) {
                        myMap = jsContent.getString("problem", "unknown");
                        // Info("Received map " + myMap);
                        myHeightMap = new Map2DGrayscale(10, 10);
                        if (myHeightMap.fromJson(jsContent.get("map").asObject())) {
                            //Info(myMap + " successfully loaded");
                            showFullWorld();
                        } else {
                            Error("Could not load " + myMap + " properly");
                        }
                    }
                } catch (Exception ex) {
                    Exception(ex);
                    Error("Ignoring pseudo JSON " + line);
                }
            } else {
                if (line.contains("END-OF-TRANSMISSION")) {
                    return false;
                }
            }
        }

        return true;
    }

    protected boolean doGatherData() {
        YellowPages yp = new YellowPages();
        String whoami = this.whoAmI();
        if (!whoami.contains("group")) {
            Error("Unable to get identification from existing project folders and data");
            return false;
        }
        myService = whoami.split("group")[1].trim();
        // Info("Apparently this project belongs to group " + myService);
        myService = "Analytics group " + myService;
        return true;
    }

    @Override
    protected boolean doCheckinLARVA() {
        // Info("Requesting checkin to " + _identitymanager);
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        out.setEncoding(_myCardID.getCardID());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        out.setContent("");
        out.setProtocol("ANALYTICS");
        reqReplyWith = getLocalName() + getKey();
        out.setReplyWith(reqReplyWith);
        this.send(out);
        in = this.blockingReceive(MessageTemplate.MatchInReplyTo(reqReplyWith), 3000);
        if (in == null) {
            Error("No answer from " + _identitymanager);
            return false;
        }
        if (in.getPerformative() == ACLMessage.INFORM) {
            // Info("Chekin confirmed in the LARVA");
            return true;
        } else {
            Error(ACLMessage.getPerformative(in.getPerformative()) + "Could not confirm the registration in LARVA due to "
                    + getDetailsLARVA(in));
            return false;
        }
    }

    @Override
    protected boolean doCheckoutLARVA() {
        // Info("Requesting checkout to " + _identitymanager);
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        out.setPerformative(ACLMessage.CANCEL);
        out.setContent("");
        out.setProtocol("ANALYTICS");
        reqReplyWith = getLocalName() + getKey();
        out.setReplyWith(reqReplyWith);
        this.send(out);
        in = this.blockingReceive(MessageTemplate.MatchInReplyTo(reqReplyWith), 3000);
        if (in == null) {
            Error("No answer from " + _identitymanager);
            return false;
        }
        if (in.getPerformative() == ACLMessage.INFORM) {
            // Info("Chekout confirmed from LARVA");
            return true;
        } else {
            Error(ACLMessage.getPerformative(in.getPerformative()) + "Could not confirm checkout from LARVA due to "
                    + getDetailsLARVA(in));
            return false;
        }
    }

    protected boolean getWorldManager(String service) {
        // Info("Requesting a copy of YP");
        if (!getYellowPages()) {
            return false;
        }
        ArrayList<String> serviceProviders = new ArrayList(myYellowPages.queryProvidersofService(service));
        if (serviceProviders.isEmpty()) {
            Error("The service " + service + " is not provided by any running agent currently");
            return false;
        }
        // Info("List of agents who provide the service " + service + ": "
//                + serviceProviders.toString());
        myServiceProvider = serviceProviders.get(0);

        // Info("Selecting the first one: " + myServiceProvider);
        return true;
    }

    protected boolean getYellowPages() {
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(_identitymanager, AID.ISLOCALNAME));
        out.setPerformative(ACLMessage.QUERY_REF);
        out.setContent("");
        out.setProtocol("ANALYTICS");
        reqReplyWith = getLocalName() + getKey();
        out.setReplyWith(reqReplyWith);
        this.send(out);
        in = this.blockingReceive(MessageTemplate.MatchInReplyTo(reqReplyWith), 3000);
        if (in == null) {
            Error("No answer from " + _identitymanager);
            return false;
        }
        if (in.getPerformative() == ACLMessage.INFORM) {
            myYellowPages = new YellowPages();
            myYellowPages.updateYellowPages(in);
            return true;
        } else {
            Error(ACLMessage.getPerformative(in.getPerformative()) + "Could not get a copy of the YellowPages due to " + getDetailsLARVA(in));
            return false;
        }
    }

    protected boolean doSubscribeWM() {
        if (!getWorldManager(myService)) {
            return false;
        }
        // Info("Subscribing as LISTENER to " + myServiceProvider);
        out = new ACLMessage();
        out.setSender(getAID());
        out.addReceiver(new AID(myServiceProvider, AID.ISLOCALNAME));
        out.setProtocol("REGULAR");
        out.setConversationId(myConvID);
        out.setContent(new JsonObject().add("type", "LISTENER").toString());
        out.setPerformative(ACLMessage.SUBSCRIBE);
        reqReplyWith = getLocalName() + getKey();
        out.setReplyWith(reqReplyWith);
        this.send(out);
        in = this.blockingReceive(MessageTemplate.MatchInReplyTo(reqReplyWith)); //, 3000);
        if (in == null) {
            Error("No answer from " + myServiceProvider);
            return false;
        }
        if (in.getPerformative() == ACLMessage.INFORM) {
            // Info("Confirmed subscription as LISTENER");
            return true;
        } else {
            Error(ACLMessage.getPerformative(in.getPerformative())
                    + " Could not open a session with "
                    + myServiceProvider + " due to " + getDetailsLARVA(in));
            return false;
        }
    }

    //
    // Visualization
    // 
    protected void openDisplay() {
        if (myHeightMap != null && myWorld == null) {
            myWorld = new ConsoleAnsi(myMap, myHeightMap.getWidth() + 10, myHeightMap.getHeight() + 10, -(300 / myHeightMap.getWidth()));
            myWorld.setCursorOff().setBackground(BACKGR).setText(FOREGR).clearScreen();
            myWorld.setBackground(black);
            myWorld.doRectangle(6, 6, myHeightMap.getWidth(), myHeightMap.getHeight());
            myWorld.printWRuler(6, 6, myHeightMap.getWidth(), myHeightMap.getHeight(), 20, myHeightMap.getWidth(), myHeightMap.getHeight());
        }
        if (myTable == null) {
            myTable = new ConsoleAnsi(myMap + "_TABLE", 52, 53, 8).setCursorOff()
                    .setBackground(TTYControlPanel.BACKGR).setText(white).clearScreen();
        }
    }

    //
    //
    //
    protected void updateDrone(JsonObject jsDrone) {
        String name = jsDrone.getString("name", "$$$$");
        liveBot myDrone;
        if (name.equals("$$$")) {
            return;
        }
        if (knownDrones.get(name) == null) {
            myDrone = new liveBot(jsDrone.getString("name", "nonamed"));
            myDrone.order = knownDrones.size();
            myTable.doRectangleFrame(tshiftx + 1, tshifty + H * myDrone.order, W, H);
            myTable.setCursorXY(tshiftx + 3, tshifty + H * myDrone.order).setText(droneColors[myDrone.order]).setBackground(BACKGR);
            myTable.print(defText(droneColors[myDrone.order]) + defBackground(BACKGR) + ConsoleAnsi.windowFrames[2].charAt(7) + ConsoleAnsi.windowFrames[2].charAt(7)
                    + defText(white) + myDrone.getName() + defText(droneColors[myDrone.order]) + defBackground(BACKGR)
                    + ConsoleAnsi.windowFrames[2].charAt(7) + ConsoleAnsi.windowFrames[2].charAt(7));
        } else {
            myDrone = knownDrones.get(name);
            hideDrone(myDrone);
        }
        myDrone.fromJson(jsDrone);
        knownDrones.put(name, myDrone);
        showDrone(myDrone);
        printDetails(myDrone);
    }

    public void printDetails(liveBot dr) {
        myTable.setCursorXY(tshiftx + 2, tshifty + 2 + H * dr.order).
                resetColors().
                print("AL").doRadioColor(dr.alive == 1);
        myTable.resetColors().
                print(" OT").doRadioColor(dr.ontarget == 1);
        myTable.resetColors().
                print(" X").
                setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%3.0f", dr.getPosition().getX()));
        myTable.resetColors().
                print(" Y").
                setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%3.0f", dr.getPosition().getY()));
        myTable.resetColors().
                print(" Z").
                setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%3.0f", dr.getPosition().getZ()));
        ArrayList<String> ALaux = new ArrayList();
        for (Thing t : dr.payload) {
            ALaux.add(t.getName());
        }
        myTable.resetColors().
                setCursorXY(tshiftx + 28, tshifty + 1 + H * dr.order).
                print("PL").
                doTextArea(tshiftx + 31, tshifty + 1 + H * dr.order, 17, 10, ALaux);
        ALaux = new ArrayList();
        ALaux.add(dr.lastEvent);
        myTable.resetColors().
                setCursorXY(tshiftx + 2, tshifty + 4 + H * dr.order).
                print("ST").
                doTextArea(tshiftx + 5, tshifty + 4 + H * dr.order, 20, 1, ALaux);
        //myTable.                        //setCursorXY(origx, origy);
        myTable.resetColors().
                setCursorXY(tshiftx + 2, tshifty + 6 + H * dr.order).
                print("EN");
        myTable.printHMinibar(tshiftx + 5, tshifty + 6 + H * dr.order, 1000, 1000, 20, gray, gray);
        myTable.printHMinibar(tshiftx + 5, tshifty + 6 + H * dr.order, dr.energylevel, 1000, 20, lightblue, gray);
        myTable.setCursorXY(tshiftx + 25, tshifty + 6 + H * dr.order).
                setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%04d", dr.energylevel));
        myTable.resetColors().
                setCursorXY(tshiftx + 2, tshifty + 8 + H * dr.order).
                print("AL");
        myTable.printHMinibar(tshiftx + 5, tshifty + 8 + H * dr.order, 256, 256, 20, gray, gray);
        myTable.printHMinibar(tshiftx + 5, tshifty + 8 + H * dr.order, dr.altitude, 256, 20, lightblue, gray);
        myTable.setCursorXY(tshiftx + 25, tshifty + 8 + H * dr.order).
                setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%03d", dr.altitude));
        myTable.setCursorXY(tshiftx + 2, tshifty + 10 + H * dr.order).
                resetColors().
                print("DI").setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%03.0f", dr.distance));
        myTable.setCursorXY(tshiftx + 12, tshifty + 10 + H * dr.order).
                resetColors().
                print("AN").setBackground(DIALBGR).setText(DIALFRG).
                print(String.format("%3.0f", dr.angle));
        myTable.resetColors();
//        myTable.setCursorXY(tshiftx + 2, tshifty + 7 + H*dr.order).print("AL");
//        myTable.printHMinibar(tshiftx+5, tshifty+5+H*dr.order, dr.distance, , 20, lightblue, gray);
//        myTable.setBackground(black).print(String.format("%3.0f", dr.energylevel));
//        myTable.resetColors();
//        myTable.setBackground(black).setText(white);
//        if (getInt() == NOREADING) { 
//            myTable.println("X");
//            return this;
//        }
//        if (max >= 1000) {
//            myTable.print(String.format("%4.0f", getDouble()));
//        } else if (max >= 100) {
//            myTable.print(String.format("%3.0f", getDouble()));
//        } else {
//            myTable.print(String.format("%2.0f", getDouble()));
//        }

    }

//    protected void showDrone(liveBot dr) {
//        this.plot(dr.getPosition().getX(), dr.getPosition().getY(), droneColors[dr.order]);
//        this.plot(dr.getPosition().getX() - 1, dr.getPosition().getY() - 1, droneColors[dr.order]);
//        this.plot(dr.getPosition().getX() - 1, dr.getPosition().getY() + 1, droneColors[dr.order]);
//        this.plot(dr.getPosition().getX() + 1, dr.getPosition().getY() + 1, droneColors[dr.order]);
//        this.plot(dr.getPosition().getX() + 1, dr.getPosition().getY() - 1, droneColors[dr.order]);
//    }
//
    protected void showDrone(liveBot dr) {
        double x = dr.getPosition().getX(), y = dr.getPosition().getY();
        this.plot(x, y, black);
        this.plot(x - 1, y - 1, black);
        this.plot(x - 1, y + 1, black);
        this.plot(x + 1, y + 1, black);
        this.plot(x + 1, y - 1, black);
        x -= dr.altitude / 25;
        y -= dr.altitude / 25;
        this.plot(x, y, droneColors[dr.order]);
        this.plot(x - 1, y - 1, droneColors[dr.order]);
        this.plot(x - 1, y + 1, droneColors[dr.order]);
        this.plot(x + 1, y + 1, droneColors[dr.order]);
        this.plot(x + 1, y - 1, droneColors[dr.order]);
    }

    protected void hideDrone(liveBot dr) {
        double x = dr.getPosition().getX(), y = dr.getPosition().getY();
        this.unplot(x, y);
        this.unplot(x - 1, y - 1);
        this.unplot(x - 1, y + 1);
        this.unplot(x + 1, y - 1);
        this.unplot(x + 1, y + 1);
        x -= dr.altitude / 25;
        y -= dr.altitude / 25;
        this.unplot(x, y);
        this.unplot(x - 1, y - 1);
        this.unplot(x - 1, y + 1);
        this.unplot(x + 1, y - 1);
        this.unplot(x + 1, y + 1);
    }

    public void showFullWorld() {
        int bg, fg;
        if (myHeightMap == null) {
            return;
        }
        if (myWorld == null) {
            openDisplay();
        }
        for (int x = 0; x < myHeightMap.getWidth(); x++) {
            for (int y = 0; y < myHeightMap.getHeight(); y++) {
                int level = myHeightMap.getLevel(x, y);
                plot(x, y, palette.getColor(level));
                if (level < 0) {
                    bg = red;
                    fg = lightred;
                } else {
                    bg = palette.getColor(level);
                    fg = ConsoleAnsi.negColor(bg);
                }
                myWorld.setCursorXY(wshiftx + x, wshifty + y).setText(fg).setBackground(bg);
                myWorld.print(" ");
            }
        }
    }

    //
    // Drawing primitives
    //
    public void plot(double dx, double dy, int color) {
        int sx = (int) dx, sy = (int) dy;
        if (-wshiftx <= sx && sx <= myHeightMap.getWidth() + wshiftx && -wshifty <= sy && sy <= myHeightMap.getHeight() + wshifty) {
            myWorld.setCursorXY(wshiftx + sx, wshifty + sy).setText(color).setBackground(color);
            myWorld.print(ConsoleAnsi.windowFrames[2].charAt(7) + "");
        }

    }

    public void unplot(double dx, double dy) {
        int sx = (int) dx, sy = (int) dy;
        if (-wshiftx <= sx && sx <= myHeightMap.getWidth() + wshiftx && -wshifty <= sy && sy <= myHeightMap.getHeight() + wshifty) {
            if (0 <= sx && sx < myHeightMap.getWidth() && 0 <= sy && sy < myHeightMap.getHeight()) {
                myWorld.setCursorXY(wshiftx + sx, wshifty + sy).
                        setText(palette.getColor(myHeightMap.getLevel(sx, sy))).
                        setBackground(palette.getColor(myHeightMap.getLevel(sx, sy))).
                        print(" ");
            } else {
                myWorld.setCursorXY(wshiftx + sx, wshifty + sy).
                        setText(FOREGR).
                        setBackground(BACKGR).
                        print(" ");

            }
        }
    }

//
//class DroneRecord {
//
//    protected int x, y, z;
//    protected String name, team;
//    protected int color;
//    protected int compass;
//    protected int energy;
//    protected int altitude;
//
//    public int getX() {
//        return x;
//    }
//
//    public int getY() {
//        return y;
//    }
//
//    public int getZ() {
//        return z;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public String getTeam() {
//        return team;
//    }
//
//    public int getColor() {
//        return color;
//    }
//
//    public int getCompass() {
//        return compass;
//    }
//
//    public int getEnergy() {
//        return energy;
//    }
//
//    public int getAltitude() {
//        return altitude;
//    }
//
//    public void setX(int x) {
//        this.x = x;
//    }
//
//    public void setY(int y) {
//        this.y = y;
//    }
//
//    public void setZ(int z) {
//        this.z = z;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public void setTeam(String team) {
//        this.team = team;
//    }
//
//    public void setColor(int color) {
//        this.color = color;
//    }
//
//    public void setCompass(int compass) {
//        this.compass = compass;
//    }
//
//    public void setEnergy(int energy) {
//        this.energy = energy;
//    }
//
//    public void setAltitude(int altitude) {
//        this.altitude = altitude;
//    }
//
//    public void fromJson(JsonObject update) {
//        setX(update.getInt("x", -1));
//        setY(update.getInt("y", -1));
//        setZ(update.getInt("z", -1));
//        setEnergy(update.getInt("energy", -1));
//        setAltitude(update.getInt("altitude", -1));
//        setCompass(update.getInt("compass", -1));
//        setName(update.getString("name", "unknown"));
//        setTeam(update.getString("team", "unknown"));
//    }
}
