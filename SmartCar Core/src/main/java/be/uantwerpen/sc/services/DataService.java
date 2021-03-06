package be.uantwerpen.sc.services;

import be.uantwerpen.sc.models.LinkEntity;
import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.models.map.Node;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Created by Arthur on 24/04/2016.
 */
@Service
public class DataService
{
    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    private Long robotID;

    private int millis;
    private int linkMillis;

    public int getNextNode() {
        return nextNode;
    }

    public void setNextNode(int nextNode) {
        this.nextNode = nextNode;
    }

    private int nextNode = -1;

    public boolean isLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(boolean locationVerifier) {
        this.locationVerified = locationVerifier;
    }

    boolean locationVerified = false;

    public int getPrevNode() {
        return prevNode;
    }

    public void setPrevNode(int prevNode) {
        this.prevNode = prevNode;
    }

    private int prevNode = -1;

    public int hasPermission() {
        return hasPermission;
    }

    public void setPermission(int hasPermission) {
        this.hasPermission = hasPermission;
    }

    private int hasPermission = -1;

    public boolean robotBusy = false;

    public boolean locationUpdated = true;

    public String trafficLightStatus;

    public Map map = null;
    public NavigationParser navigationParser = null;

    private String tag = "NO_TAG";
    private int currentLocation = -1;

    public Long getRobotID() {
        return robotID;
    }

    public void setRobotID(Long robotID) {
        this.robotID = robotID;
    }

    public int getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(int currentLocation) {
        this.currentLocation = currentLocation;
    }

    public int getMillis() {return millis;}
    public void setMillis(int millis) {this.millis = millis;}

    public int getLinkMillis() {
        return linkMillis;
    }

    public void setLinkMillis(int linkMillis) {
        this.linkMillis = linkMillis;
    }

    public String getTag() {return tag;}
    public void setTag(String tag) {this.tag = tag;}

    private String LookingCoordiante;

    public String getLookingCoordiante() {
        return LookingCoordiante;
    }

    public void setLookingCoordiante(String lookingCoordiante) {
        LookingCoordiante = lookingCoordiante;
    }

    private PathplanningEnum pathplanningEnum;

    public PathplanningEnum getPathplanningEnum() {
        return pathplanningEnum;
    }

    public void setPathplanningEnum(PathplanningEnum pathplanningEnum) {
        this.pathplanningEnum = pathplanningEnum;
    }

    public void firstLink(){
        if(map != null) {
            int start = getCurrentLocation();
            int lid = -1;
            for(Node node : map.getNodeList()){
                if(node.getNodeId() == start){
                    LinkEntity link = node.getNeighbours().get(0);
                    lid = link.getLid();
                    nextNode = link.getStopId().getPid();
                    prevNode = link.getStartId().getPid();
                    linkMillis = link.getLength();
                }
            }

            Terminal.printTerminal("Current Link: " + lid);
            RestTemplate rest = new RestTemplate();
            rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + robotID + "/lid/" + lid, Integer.class);
        }
    }

    public void nextLink(){
        if(map != null && navigationParser != null && navigationParser.list != null && !navigationParser.list.isEmpty() && navigationParser.list.size() != 1) {
            int start = navigationParser.list.get(0).getId();
            int end = navigationParser.list.get(1).getId();
            if(getTag().trim().equals("NONE")){
                currentLocation = nextNode;
            }
            //setCurrentLocationAccordingTag();
            nextNode = end;
            prevNode = start;
            int lid = -1;
            //find link from start to end
            for (Edge e : navigationParser.list.get(0).getAdjacencies()) {
                if (e.getTarget() == end) {
                    lid = e.getLinkEntity().getLid();
                    linkMillis = e.getLinkEntity().getLength();
                    Terminal.printTerminal("New Link Distance: " + linkMillis);
                }
            }

            Terminal.printTerminal("Current Link: " + lid);
            if(this.pathplanningEnum == PathplanningEnum.DIJKSTRA) {
                //delete entry from navigationParser
                navigationParser.list.remove(0);
            }
            RestTemplate rest = new RestTemplate();
            rest.getForObject("http://" + serverIP + ":" + serverPort + "/bot/" + robotID + "/lid/" + lid, Integer.class);
        }else{
            //TODO update location
            Terminal.printTerminal("Entering manual manouvering mode. Location will be inacurate");
            prevNode = nextNode;
        }
    }

    public void setCurrentLocationAccordingTag() {
        switch(getTag()){
            case "04 70 39 32 06 27 80":
                setCurrentLocation(1);
                break;
            case "04 67 88 8A C8 48 80":
                setCurrentLocation(2);
                break;
            case "04 97 36 A2 F7 22 80":
                setCurrentLocation(3);
                break;
            case "04 36 8A 9A F6 1F 80":
                setCurrentLocation(4);
                break;
            case "04 7B 88 8A C8 48 80":
                setCurrentLocation(5);
                break;
            case "04 6C 6B 32 06 27 80":
                setCurrentLocation(6);
                break;
            case "04 84 88 8A C8 48 80":
                setCurrentLocation(7);
                break;
            case "04 B3 88 8A C8 48 80":
                setCurrentLocation(8);
                break;
            case "04 8D 88 8A C8 48 80":
                setCurrentLocation(9);
                break;
            case "04 AA 88 8A C8 48 80":
                setCurrentLocation(10);
                break;
            case "04 C4 FD 12 Q9 34 80":
                setCurrentLocation(11);
                break;
            case "04 96 88 8A C8 48 80":
                setCurrentLocation(12);
                break;
            case "04 A1 88 8A C8 48 80":
                setCurrentLocation(13);
                break;
            case "04 86 04 22 A9 34 84":
                setCurrentLocation(14);
                break;
            case "04 18 25 9A 7F 22 80":
                setCurrentLocation(15);
                break;
            case "04 BC 88 8A C8 48 80":
                setCurrentLocation(16);
                break;
            case "04 C5 88 8A C8 48 80":
                setCurrentLocation(17);
                break;
            case "04 EC 88 8A C8 48 80":
                setCurrentLocation(18);
                break;
            case "04 E3 88 8A C8 48 80":
                setCurrentLocation(19);
                break;
            case "04 26 3E 92 1E 25 80":
                setCurrentLocation(20);
                break;
            case "04 DA 88 8A C8 48 80":
                setCurrentLocation(21);
                break;
            case "04 D0 88 8A C8 48 80":
                setCurrentLocation(22);
                break;
            case "04 41 70 92 1E 25 80":
                setCurrentLocation(23);
                break;
            case "04 3C 67 9A F6 1F 80":
                setCurrentLocation(24);
                break;
            case "NONE":
                break;
            default:
                setCurrentLocation(-1);
                break;
        }
    }
}
