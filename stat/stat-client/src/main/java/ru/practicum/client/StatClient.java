package ru.practicum.client;


import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStats;

import java.time.LocalDateTime;
import java.util.List;

public class StatClient {

    private final String BASE_URL = "http://localhost:9090";

    private RestClient restClient = RestClient.create();

    public void saveHit(EndpointHitDto hitDto) {
        restClient.post()
                .uri(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .body(hitDto)
                .retrieve()
                .toBodilessEntity();

    }

    public List<ViewStats> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        String uriWithParams = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .path("/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique)
                .toUriString();

        return restClient.get()
                .uri(uriWithParams).retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

}
