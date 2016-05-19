package be.uantwerpen.sc.controllers;

import be.uantwerpen.sc.models.map.Map;
import be.uantwerpen.sc.models.map.Path;
import be.uantwerpen.sc.services.DataService;
import be.uantwerpen.sc.tools.Vertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Created by Niels on 10/05/2016.
 */
@RestController
@RequestMapping(value = "/path/")
public class PathController {

    @Autowired
    DataService dataService;

    @RequestMapping(method = RequestMethod.GET)
    public Path getPath(int start, int stop){
        String coreIP = "http://localhost:1994";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Path> responseList;
        responseList = restTemplate.getForEntity("http://"+dataService.serverIP+"/map/"+Integer.toString(start)+"/path/"+Integer.toString(stop), Path.class);
        Path path = responseList.getBody();
        return path;
    }

    @RequestMapping(value = "random",method = RequestMethod.GET)
    public Path getRandomPath(int start){
        String coreIP = "http://localhost:1994";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<Path> responseList;
        responseList = restTemplate.getForEntity("http://"+dataService.serverIP+"/map/random/"+Integer.toString(start), Path.class);
        Path path = responseList.getBody();
        return path;
    }
}