package com.sickton.jgaffer.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class RagService {

    private static final String PYTHON_SERVICE_URL = "http://localhost:8000";
    private final RestTemplate restTemplate = new RestTemplate();

    public Object getStory(String team, String league, String mode, String queryType)
    {
        String url = PYTHON_SERVICE_URL + "/story";
        Map<String, String> body = Map.of(
                "team", team,
                "league", league,
                "mode", mode,
                "query_type", queryType
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<Map<String, String>>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }

    public Object explainMoment(Map<String, Object> momentData)
    {
        String url = PYTHON_SERVICE_URL + "/explain";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(momentData, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }

    public Object getNetwork(String escapingTeam, String escapingFormation,
                             String pressingTeam,  String pressingFormation,
                             String league, int minute, int homeGoals, int awayGoals)
    {
        String url = PYTHON_SERVICE_URL + "/network";

        Map<String, Object> body = new HashMap<>();
        body.put("escaping_team",      escapingTeam);
        body.put("escaping_formation", escapingFormation);
        body.put("pressing_team",      pressingTeam);
        body.put("pressing_formation", pressingFormation);
        body.put("league",             league);
        body.put("minute",             minute);
        body.put("home_goals",         homeGoals);
        body.put("away_goals",         awayGoals);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }

    public Object getPuzzle(Map<String, Object> body)
    {
        String url = PYTHON_SERVICE_URL + "/puzzle";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }

    public Object evaluatePuzzle(Map<String, Object> body)
    {
        String url = PYTHON_SERVICE_URL + "/evaluate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);

        return response.getBody();
    }
}
