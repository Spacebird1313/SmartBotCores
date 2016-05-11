package be.uantwerpen.sc.services;

import be.uantwerpen.sc.controllers.CCommandSender;
import be.uantwerpen.sc.controllers.MapController;
import be.uantwerpen.sc.controllers.PathController;
import be.uantwerpen.sc.models.map.Path;
import be.uantwerpen.sc.tools.DriveDir;
import be.uantwerpen.sc.tools.IPathplanning;
import be.uantwerpen.sc.tools.NavigationParser;
import be.uantwerpen.sc.tools.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

/**
 * Created by Thomas on 14/04/2016.
 */
@Service
public class TerminalService
{
    private Terminal terminal;
    @Autowired
    private MapController mapController;
    @Autowired
    private PathController pathController;
    @Autowired
    private CCommandSender sender;
    @Autowired
    private QueueService queueService;

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
        terminal.printTerminal(" :: SmartCar Core - 2016 ::  -  Developed by: Huybrechts T., Janssens A., Joosens D., Vervliet N.");
        terminal.printTerminal("Type 'help' to display the possible commands.");

        terminal.activateTerminal();
    }

    private void parseCommand(String commandString)
    {
        String command = commandString.split(" ", 2)[0].toLowerCase();

        switch(command)
        {
            case "navigate":
                try {
                    String command2 = commandString.split(" ", 2)[1].toLowerCase();

                    String start = command2.split(" ", 2)[0].toLowerCase();
                    String end = command2.split(" ", 2)[1].toLowerCase();
                    if (start == end) {
                        terminal.printTerminal("Start cannot equal end.");
                    } else if (start == "" || end == "") {
                        terminal.printTerminal("Usage: navigate start end");
                    } else {
                        try {
                            int startInt = Integer.parseInt(start);
                            int endInt = Integer.parseInt(end);
                            startPathPlanning(startInt, endInt);
                        } catch (NumberFormatException e) {
                            terminal.printTerminalError(e.getMessage());
                            terminal.printTerminal("Usage: navigate start end");
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminal("Usage: navigate start end");
                }
                break;
            case "path":
                try {
                    String command2 = commandString.split(" ", 2)[1].toLowerCase();

                    String start = command2.split(" ", 2)[0].toLowerCase();
                    String end = command2.split(" ", 2)[1].toLowerCase();
                    if (start == end) {
                        terminal.printTerminal("Start cannot equal end.");
                    } else if (start == "" || end == "") {
                        terminal.printTerminal("Usage: navigate start end");
                    } else {
                        try {
                            int startInt = Integer.parseInt(start);
                            int endInt = Integer.parseInt(end);
                            getPath(startInt, endInt);
                        } catch (NumberFormatException e) {
                            terminal.printTerminalError(e.getMessage());
                            terminal.printTerminal("Usage: navigate start end");
                        }
                    }
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminal("Usage: navigate start end");
                }
                break;
            case "sendcommand":
                try {
                    String command2 = commandString.split(" ", 2)[1].toUpperCase();
                    sender.sendCommand(command2);
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminal("Usage: navigate start end");
                }
                break;
            case "domusic":
                try {
                    //sender.sendCommand("DRIVE FOLLOWLINE");
                    sender.sendCommand("SPEAKER UNMUTE");
                    sender.sendCommand("SPEAKER PLAY QMusic");
                    try{
                        Thread.sleep(1000);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    sender.sendCommand("SPEAKER PLAY cantina");
                }catch(ArrayIndexOutOfBoundsException e){
                    terminal.printTerminal("Usage: navigate start end");
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
                terminal.printTerminal("'navigate {start} {end}': get the path from the server");
                terminal.printTerminal("'exit' : shutdown the core.");
                terminal.printTerminal("'help' / '?' : show all available commands.\n");
                break;
        }
    }

    private void startPathPlanning(int start, int end){
        terminal.printTerminal("Starting pathplanning from point " + start + " to " + end);
        //get Map from server
        //Send map + start + end to pathplanning

       /* Vertex[] list = mapController.getPath();
        List<Vertex> list2 = Arrays.asList(list);
        NavigationParser navigationParser = new NavigationParser(list2);
        navigationParser.parseMap();*/
        IPathplanning pathplanning = new PathplanningService();
        NavigationParser navigationParser = new NavigationParser(pathplanning.Calculatepath(mapController.getMap(),start,end));
        for (DriveDir command : navigationParser.parseMap()){
            queueService.insertJob(command.toString());
        }
        System.out.println(navigationParser.parseMap().toString());
    }

    private void getPath(int start, int end){
        Path path = pathController.getPath(start, end);
        System.out.println(path.toString());
    }
}