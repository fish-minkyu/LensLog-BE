package com.example.LensLog.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.util.UriComponentsBuilder;


@Configuration
@RequiredArgsConstructor
public class OAuth2LoginConfig {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private static final String AUTHORIZATION_REQUEST_BASE_URI = "/oauth2/authorization";
    private final String externalBaseUrl = "https://www.lenslog.cloud"; // <<<<<< 외부 도메인 하드코딩

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        // 기본 Resolver를 생성합니다.
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
            new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, AUTHORIZATION_REQUEST_BASE_URI);

        // 커스텀 로직으로 래핑합니다.
        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customizeRedirectUri(defaultResolver.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return customizeRedirectUri(defaultResolver.resolve(request, clientRegistrationId));
            }

            private OAuth2AuthorizationRequest customizeRedirectUri(OAuth2AuthorizationRequest authorizationRequest) {
                if (authorizationRequest == null) {
                    return null;
                }

                String originalRedirectUri = authorizationRequest.getRedirectUri();

                String correctRedirectUri = externalBaseUrl +
                    UriComponentsBuilder.fromUriString(originalRedirectUri)
                        .build().getPath();

                return OAuth2AuthorizationRequest.from(authorizationRequest)
                    .redirectUri(correctRedirectUri) // <<<<<< 최종 redirectUri를 강제
                    .build();
            }
        };
    }
}