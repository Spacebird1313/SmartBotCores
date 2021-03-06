package be.uantwerpen.sc.services;

import be.uantwerpen.sc.RobotCoreLoop;
import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.models.map.Path;
import be.uantwerpen.sc.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by Thomas on 14/04/2016.
 */
@Service
public class TerminalService
{
    private Terminal terminal;

    @Autowired
    private PathController pathController;
    @Autowired
    private CCommandSender sender;
    @Autowired
    private QueueService queueService;
    @Autowired
    private DataService dataService;

    private RobotCoreLoop robotCoreLoop;

    public TerminalService()
    {
        terminal = new Terminal()
        {
            @Override
            public void executeCommand(String commandString)
            {
                parseCommand(commandString);
            }
        };
    }

    public void systemReady()
    {
        terminal.printTerminal("\nSmartCar Core [Version " + getClass().getPackage().getImplementationVersion() + "]\n(c) 2015-2017 University of Antwerp. All rights reserved.");
        terminal.printTerminal("Type 'help' to display the possible commands.");

        terminal.activateTerminal();
    }

    @Deprecated
    public void setRobotCoreLoop(RobotCoreLoop robotCoreLoop)
    {
        this.robotCoreLoop = robotCoreLoop;
    }

    private void parseCommand(String commandString)
    {
        String command = commandString.split(" ", 2)[0].toLowerCase();

        switch(command)
        {
            case "navigate":
                try {
                    String end = commandString.split(" ", 2)[1].toLowerCase();
                    try {
                        int endInt = Integer.parseInt(end);
                        startPathPlanning(endInt);
                    } catch (NumberFormatException e) {
                        terminal.printTerminalError(e.getMessage());
                        terminal.printTerminalInfo("Usage: navigate start end");
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminalInfo("Usage: navigate start end");
                }
                break;
            case "path":
                try {
                    String command2 = commandString.split(" ", 2)[1].toLowerCase();

                    String start = command2.split(" ", 2)[0].toLowerCase();
                    String end = command2.split(" ", 2)[1].toLowerCase();
                    if (start == end) {
                        terminal.printTerminalInfo("Start cannot equal end.");
                    } else if (start == "" || end == "") {
                        terminal.printTerminalInfo("Usage: navigate start end");
                    } else {
                        try {
                            int startInt = Integer.parseInt(start);
                            int endInt = Integer.parseInt(end);
                            getPath(startInt, endInt);
                        } catch (NumberFormatException e) {
                            terminal.printTerminalError(e.getMessage());
                            terminal.printTerminalInfo("Usage: navigate start end");
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminalError("Usage: navigate start end");
                }
                break;
            case "random":
                try {
                    getRandomPath();
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminalError("Usage: navigate start end");
                }
                break;
            case "sendcommand":
                try {
                    String command2 = commandString.split(" ", 2)[1].toUpperCase();
                    //No override
                    queueService.insertJob(command2);
                    //Override
                    //sender.sendCommand(command2);
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminalInfo("Usage: navigate start end");
                }
                break;
            case "domusic":
                sender.sendCommand("SPEAKER UNMUTE");
                sender.sendCommand("SPEAKER PLAY QMusic");
                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }
                sender.sendCommand("SPEAKER PLAY cantina");
                break;
            case "stopmusic":
                sender.sendCommand("SPEAKER STOP");
                break;
            case "checkqueue":
                try {
                    System.out.println(queueService.getContentQueue().toString());
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminalError("error");
                }
                break;
            case "exit":
                exitSystem();
                break;
            case "help":
            case "?":
                printHelp("");
                break;
            default:
                terminal.printTerminalInfo("Command: '" + command + "' is not recognized.");
                break;
        }
    }

    private void exitSystem()
    {
        System.exit(0);
    }

    private void printHelp(String command)
    {
        switch(command)
        {
            default:
                terminal.printTerminal("Available commands:");
                terminal.printTerminal("-------------------");
                terminal.printTerminal("'navigate {start} {end}': navigates the robot from point {start} to {end}");
                terminal.printTerminal("'path {start} {end}': get the path from the server");
                terminal.printTerminal("'random': get random path from the server from current location");
                terminal.printTerminal("'simulate {true/false}': activate he simulator");
                terminal.printTerminal("'checkQueue': check content of the queue");
                terminal.printTerminal("'exit' : shutdown the core.");
                terminal.printTerminal("'help' / '?' : show all available commands.\n");
                break;
        }
    }

    private void startPathPlanning(int end2){
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
        Terminal.printTerminal("Starting pathplanning from point " + dataService.getCurrentLocation() + " to " + end2);
        dataService.navigationParser = new NavigationParser(robotCoreLoop.pathplanning.Calculatepath(dataService.map, dataService.getCurrentLocation(), end2));
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

    private void getPath(int start, int end){
        Path path = pathController.getPath(start, end);
        System.out.println(path.toString());
    }

    private void getRandomPath(){
        int currentLocation = dataService.getCurrentLocation();
        if(currentLocation < 0) {
            currentLocation = 4;
            dataService.setLookingCoordiante("N");
        }
        List<Vertex> path = pathController.getRandomPath(currentLocation).getPath();
        NavigationParser navigationParser = new NavigationParser(path);
        //System.out.println(navigationParser.parseRandomMap().toString());

    }
}