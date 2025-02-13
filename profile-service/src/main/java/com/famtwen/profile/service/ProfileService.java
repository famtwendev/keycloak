package com.famtwen.profile.service;

import com.famtwen.profile.dto.identity.Credential;
import com.famtwen.profile.dto.identity.TokenExchangeParam;
import com.famtwen.profile.dto.identity.UserCreationParam;
import com.famtwen.profile.dto.request.RegistrationRequest;
import com.famtwen.profile.dto.response.ProfileResponse;
import com.famtwen.profile.exception.ErrorNomalizer;
import com.famtwen.profile.mapper.ProfileMapper;
import com.famtwen.profile.repository.IdentityClient;
import com.famtwen.profile.repository.ProfileRepository;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;
    IdentityClient identityClient;

    ErrorNomalizer errorNormalizer;

    @Value("${idp.client-id}")
    @NonFinal
    String clientId;

    @Value("${idp.client_secret}")
    @NonFinal
    String clientSecret;



    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        return profiles.stream()
                       .map(profileMapper::toProfileResponse)
                       .toList();
    }

    public ProfileResponse register(RegistrationRequest request) {
        try {
            // Create account in keycloak
            var token = identityClient.exchangeToken(TokenExchangeParam
                    .builder()
                    .grant_type("client_credentials")
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope("openid")
                    .build());

            log.info("TokenInfo: {}", token);
            // Create user with client token and given info
            var creationResponse = identityClient.createUser(
                    "Bearer " + token.getAccessToken(),
                    UserCreationParam.builder()
                                     .username(request.getUsername())
                                     .firstName(request.getFirstName())
                                     .lastName(request.getLastName())
                                     .email(request.getEmail())
                                     .enabled(true)
                                     .emailVerified(false)
                                     .credentials(List.of(Credential.builder()
                                                                    .type("password")
                                                                    .temporary(false)
                                                                    .value(request.getPassword())
                                                                    .build()))
                                     .build());

            log.info("Creation Response: {}", creationResponse);
            String userId = extractUserId(creationResponse);
            log.info("UserId: {}", userId);
            // Get userId of KeyCloak account

            var profile = profileMapper.toProfile(request);
            profile.setUserId(userId);

            profile = profileRepository.save(profile);

            return profileMapper.toProfileResponse(profile);
        } catch (FeignException exception) {
            throw errorNormalizer.handleKeyCloakException(exception);
        }
    }

    private String extractUserId(ResponseEntity<?> response) {
        String location = response.getHeaders()
                                  .get("Location")
                                  .getFirst();
        if (location != null && !location.isEmpty()) {
            String[] splitedStr = location.split("/");
            return splitedStr[splitedStr.length - 1];
        }
        return null;
    }
}
