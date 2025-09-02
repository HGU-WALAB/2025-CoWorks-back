package com.hiswork.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hiswork.backend.dto.AuthDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
public class HisnetLoginService {

    @Value("${hisnet.access_key}")
    private String ACCESS_KEY;

    @Value("${hisnet.url}")
    private String HISNET_URL;

    // 받아온 히즈넷 로그인 토큰으로 사용자 정보 반환
    public AuthDto callHisnetLoginApi(AuthDto dto) {
        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put("accessKey", ACCESS_KEY);
        requestBody.put("token", dto.getHisnetToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();
        UriComponents uri = UriComponentsBuilder.fromUriString(HISNET_URL).build();

        try {
            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {};
            ResponseEntity<Map<String, Object>> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.POST, entity, typeRef);
            Map<String, Object> result = resultMap.getBody();

            assert result != null;

            return AuthDto.builder()
                    .uniqueId(result.get("uniqueId").toString())
                    .name(result.get("name").toString())
                    .email(result.get("email").toString())
                    .department(result.get("department").toString())
                    .major1(result.get("major1").toString())
                    .major2(result.get("major2").toString())
                    .grade(Integer.parseInt(result.get("grade").toString()))
                    .semester(Integer.parseInt(result.get("semester").toString()))
                    .build();
        } catch (HttpStatusCodeException e) {
            Map<String, Object> result = new HashMap<>();

            try {
                result = new ObjectMapper().readValue(e.getResponseBodyAsString(), new TypeReference<>() {});
                throw new RuntimeException(result.get("message").toString());
            } catch (JsonProcessingException ex) {
                throw new RuntimeException("Failed to parse error response from Hisnet API", ex);
            }
        }
    }
}
