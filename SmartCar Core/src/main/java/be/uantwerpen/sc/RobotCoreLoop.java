package be.uantwerpen.sc;

import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.controllers.mqtt.MqttJobSubscriber;
import be.uantwerpen.sc.services.*;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * Created by Arthur on 4/05/2016.
 */
@Service
public class RobotCoreLoop implements Runnable
{
    @Autowired
    private DataService dataService;

    @Autowired
    private PathplanningType pathplanningType;

    @Autowired
    private MqttJobSubscriber jobSubscriber;

    @Autowired
    private JobService jobService;

    @Value("${sc.core.ip:localhost}")
    private String serverIP;

    @Value("#{new Integer(${sc.core.port}) ?: 1994}")
    private int serverPort;

    @Autowired
    private QueueService queueService;
    @Autowired
    private MapController mapController;
    @Autowired
    private PathController pathController;

    public IPathplanning pathplanning;

    private boolean first = true;

    /*public RobotCoreLoop(QueueService queueService, MapController mapController, PathController pathController, PathplanningType pathplanningType, DataService dataService){
        this.queueService = queueService;
        this.mapController = mapController;
        this.pathController = pathController;
        this.pathplanningType = pathplanningType;
        this.dataService = dataService;

    }*/

    public RobotCoreLoop(){

    }

    @PostConstruct
    private void postconstruct(){
        //Setup type
        Terminal.printTerminalInfo("Selected PathplanningType: " + pathplanningType.getType().name());
    }

    @Deprecated
    public void setServerCoreIP(String ip, int port)
    {
        this.serverIP = ip;
        this.serverPort = port;
    }

    public void run() {
        //getRobotId
        RestTemplate restTemplate = new RestTemplate();
        Long robotID = restTemplate.getForObject("http://" + serverIP + ":" + serverPort + "/bot/newRobot", Long.class);
        dataService.setRobotID(robotID);
        jobService.setRobotCoreLoop(this);

        if(!jobSubscriber.initialisation())
        {
            System.err.println("Could not initialise MQTT Job service!");
        }

        Terminal.printTerminal("Got ID: " + robotID);

        //Wait for tag read
        synchronized (this) {
            while (dataService.getTag().trim().equals("NONE") || dataService.getTag().equals("NO_TAG")) {
                try {
                    //Read tag
                    queueService.insertJob("TAG READ UID");
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        Terminal.printTerminal("Tag: " + dataService.getTag());

        updateStartLocation();
        Terminal.printTerminal("Start Location: " + dataService.getCurrentLocation());

        //Setup interface for correct mode
        setupInterface();

        dataService.map = mapController.getMap();

        //We have the map now, update link
        dataService.firstLink();

        dataService.setLookingCoordiante("N");
        while (!Thread.interrupted() && pathplanningType.getType() == PathplanningEnum.RANDOM) {
            //Use pathplanning (Described in Interface)
            if (queueService.getContentQueue().isEmpty() && dataService.locationUpdated) {
                dataService.setCurrentLocationAccordingTag();
                //Endpoint wont be used -> does not matter
                dataService.navigationParser = new NavigationParser(pathplanning.Calculatepath(dataService.map, dataService.getCurrentLocation(), -1));
                //Parse Map
                //dataService.navigationParser.parseMap();
                dataService.navigationParser.parseRandomMap(dataService);

                //Setup for driving
                int start = dataService.navigationParser.list.get(0).getId();
                int end = dataService.navigationParser.list.get(1).getId();
                dataService.setNextNode(end);
                dataService.setPrevNode(start);
                if (first) {
                    queueService.insertJob("DRIVE FOLLOWLINE");
                    //queueService.insertJob("DRIVE FORWARD 50");
                    first = false;
                }
                //Process map
                for (DriveDir command : dataService.navigationParser.commands) {
                    Terminal.printTerminal("Adding command: " + command.toString());
                    queueService.insertJob(command.toString());
                }
                queueService.insertJob("TAG READ UID");
                dataService.locationUpdated = false;
            }else if(queueService.getContentQueue().isEmpty()){
                try {
                    queueService.insertJob("TAG READ UID");
                    if(!dataService.locationUpdated) {
                        queueService.insertJob("DRIVE BACKWARDS 20");
                    }
                    Thread.sleep(200);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (pathplanningType.getType() == PathplanningEnum.DIJKSTRA) {
            dataService.locationUpdated = false;
            while(!dataService.locationUpdated){
                //Wait
                try {
                    //Read tag
                    queueService.insertJob("TAG READ UID");
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            dataService.navigationParser = new NavigationParser(pathplanning.Calculatepath(dataService.map, dataService.getCurrentLocation(), 12));
            //Parse Map
            dataService.navigationParser.parseMap();
            //dataService.navigationParser.parseRandomMap(dataService);

            //Setup for driving
            int start = dataService.navigationParser.list.get(0).getId();
            int end = dataService.navigationParser.list.get(1).getId();
            dataService.setNextNode(end);
            dataService.setPrevNode(start);
            queueService.insertJob("DRIVE FOLLOWLINE");
            queueService.insertJob("DRIVE FORWARD 110");

            //Process map
            for (DriveDir command : dataService.navigationParser.commands) {
                queueService.insertJob(command.toString());
            }
        }
    }

    public IPathplanning getPathplanning()
    {
        return this.pathplanning;
    }


    private void setupInterface(){
        switch (pathplanningType.getType()){
            case DIJKSTRA:
                pathplanning = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
                break;
            case RANDOM:
                pathplanning = new RandomPathPlanning(pathController);
                dataService.setPathplanningEnum(PathplanningEnum.RANDOM);
                break;
            default:
                //Dijkstra
                pathplanning = new PathplanningService();
                dataService.setPathplanningEnum(PathplanningEnum.DIJKSTRA);
        }
    }

    public void updateStartLocation(){
        switch(dataService.getTag().trim()){
            case "04 70 39 32 06 27 80":
                dataService.setCurrentLocation(1);
                break;
            case "04 67 88 8A C8 48 80":
                dataService.setCurrentLocation(2);
                break;
            case "04 97 36 A2 F7 22 80":
                dataService.setCurrentLocation(3);
                break;
            case "04 36 8A 9A F6 1F 80":
                dataService.setCurrentLocation(4);
                break;
            case "04 7B 88 8A C8 48 80":
                dataService.setCurrentLocation(5);
                break;
            case "04 6C 6B 32 06 27 80":
                dataService.setCurrentLocation(6);
                break;
            case "04 84 88 8A C8 48 80":
                dataService.setCurrentLocation(7);
                break;
            case "04 B3 88 8A C8 48 80":
                dataService.setCurrentLocation(8);
                break;
            case "04 8D 88 8A C8 48 80":
                dataService.setCurrentLocation(9);
                break;
            case "04 AA 88 8A C8 48 80":
                dataService.setCurrentLocation(10);
                break;
            case "04 C4 FD 12 Q9 34 80":
                dataService.setCurrentLocation(11);
                break;
            case "04 96 88 8A C8 48 80":
                dataService.setCurrentLocation(12);
                break;
            case "04 A1 88 8A C8 48 80":
                dataService.setCurrentLocation(13);
                break;
            case "04 86 04 22 A9 34 84":
                dataService.setCurrentLocation(14);
                break;
            case "04 18 25 9A 7F 22 80":
                dataService.setCurrentLocation(15);
                break;
            case "04 BC 88 8A C8 48 80":
                dataService.setCurrentLocation(16);
                break;
            case "04 C5 88 8A C8 48 80":
                dataService.setCurrentLocation(17);
                break;
            case "04 EC 88 8A C8 48 80":
                dataService.setCurrentLocation(18);
                break;
            case "04 E3 88 8A C8 48 80":
                dataService.setCurrentLocation(19);
                break;
            case "04 26 3E 92 1E 25 80":
                dataService.setCurrentLocation(20);
                break;
            case "04 DA 88 8A C8 48 80":
                dataService.setCurrentLocation(21);
                break;
            case "04 D0 88 8A C8 48 80":
                dataService.setCurrentLocation(22);
                break;
            case "04 41 70 92 1E 25 80":
                dataService.setCurrentLocation(23);
                break;
            case "04 3C 67 9A F6 1F 80":
                dataService.setCurrentLocation(24);
                break;
            default:
                dataService.setCurrentLocation(-1);
                break;
        }
    }
}
