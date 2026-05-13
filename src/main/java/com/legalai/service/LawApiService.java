package com.legalai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Service
public class LawApiService {

    @Value("${law.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String searchLaw(String query) {
        URI uri = UriComponentsBuilder.fromUriString("https://www.law.go.kr/DRF/lawSearch.do")
                .queryParam("OC", apiKey)
                .queryParam("target", "law")
                .queryParam("query", query)
                .queryParam("search", 2)    // 1: 법령명 검색, 2: 본문 검색
                .queryParam("display", 20)  // 한 번에 가져올 결과 수 (기본 20, 최대 100)
                .queryParam("type", "JSON")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        return restTemplate.getForObject(uri, String.class);
    }

    public String getLawDetail(String lawId) {
        URI uri = UriComponentsBuilder.fromUriString("https://www.law.go.kr/DRF/lawService.do")
                .queryParam("OC", apiKey)
                .queryParam("target", "law")
                .queryParam("ID", lawId)
                .queryParam("type", "JSON")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        return restTemplate.getForObject(uri, String.class);
    }
}