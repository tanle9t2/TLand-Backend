package com.tanle.tland.user_service.service.impl;

import com.tanle.tland.user_service.entity.UserRole;
import com.tanle.tland.user_service.request.UserSignUpRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    public String getAdminToken() {
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("username", adminUsername);
        body.add("password", adminPassword);
        body.add("grant_type", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return (String) response.getBody().get("access_token");
    }

    public void createUser(UserSignUpRequest signUpRequest) {
        String url = keycloakUrl + "/admin/realms/" + realm + "/users";

        Map<String, Object> user = new HashMap<>();
        user.put("username", signUpRequest.getUsername());
        user.put("email", signUpRequest.getEmail());
        user.put("enabled", true);

        Map<String, Object> credential = new HashMap<>();
        credential.put("type", "password");
        credential.put("value", signUpRequest.getPassword());
        credential.put("temporary", false);

        user.put("credentials", List.of(credential));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        String location = response.getHeaders().getFirst("Location");
        String userId = location != null ? location.substring(location.lastIndexOf("/") + 1) : null;

        signUpRequest.setUserId(userId);

        if (userId != null) {
            assignRoleToUser(userId, UserRole.ROLE_USER.name());
        }
    }

    private void assignRoleToUser(String userId, String roleName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String roleUrl = keycloakUrl + "/admin/realms/" + realm + "/roles/" + roleName;
        ResponseEntity<Map> roleResponse = restTemplate.exchange(
                roleUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
        Map<String, Object> role = roleResponse.getBody();

        if (role == null) {
            throw new RuntimeException("Role not found: " + roleName);
        }

        // 2. Assign role to user
        String assignUrl = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";
        restTemplate.exchange(
                assignUrl,
                HttpMethod.POST,
                new HttpEntity<>(List.of(role), headers),
                String.class
        );
    }
}
