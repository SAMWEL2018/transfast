package com.sla.matercard.inbound.http;

import com.sla.matercard.inbound.Utility.AppConfig;
import com.sla.matercard.inbound.Utility.ConvertTo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * @author samwel.wafula
 * Created on 13/06/2024
 * Time 12:28
 * Project Transfast
 */
@Service

@Slf4j
public class HttpHandler {
    private final WebClient webClient;
    private final AppConfig appConfig;

    public HttpHandler(WebClient webClient, AppConfig appConfig) {
        this.webClient = webClient;
        this.appConfig = appConfig;
    }

    public JsonNode sendSyncCallWithoutBody(String url, HttpMethod httpMethod, MultiValueMap<String, String> params, String token) {

        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add("Authorization", token);
        return sendSyncCallWithoutBody(httpMethod, url, params, header);
    }

    /**
     * Sync Call without request Body Preferably GET request
     *
     * @param method      GET, POST ...
     * @param endpoint    full URL
     * @param queryParams Request Query Parameters
     * @param headers     Request headers
     * @return Response Body
     */

    public JsonNode sendSyncCallWithoutBody(HttpMethod method, String endpoint, MultiValueMap<String, String> queryParams, MultiValueMap<String, String> headers) {

        try {
            if (headers == null) {
                headers = new LinkedMultiValueMap<>();
            }
            if (queryParams == null) {
                queryParams = new LinkedMultiValueMap<>();
            }

            MultiValueMap<String, String> finalHeaders = headers;
            MultiValueMap<String, String> finalQueryParams = queryParams;

            return webClient
                    .method(method)
                    .uri(uriBuilder -> {
                        URI uri = UriComponentsBuilder
                                .fromUriString(endpoint)
                                .queryParams(finalQueryParams)
                                .build().toUri();
                        log.info("Request URL  :: " + uri);
                        return uri;
                    })
                    .headers(httpHeaders -> {
                        httpHeaders.addAll(finalHeaders);
                    })
                    .retrieve()
                    .onStatus( //Handle common HTTP error for Better Processing On services consuming this functions
                            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).flatMap(
                                    er -> Mono.error(new Exception(er))
                            )
                    )
                    .bodyToMono(JsonNode.class)
                    .block();

        } catch (Exception e) {
            log.error(e.getMessage());

            String body = e.getMessage().substring(e.getMessage().indexOf("{"));
            JsonNode res = ConvertTo.jsonNodeFromStr(body);

            if (res != null) {
                return res;
            } else {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    public JsonNode sendSyncRequest(String url, HttpMethod httpMethod, String token, MultiValueMap<String, String> params) {

        try {
            return webClient.method(httpMethod)
                    .uri(uriBuilder -> UriComponentsBuilder.fromUriString(url)
                            .queryParams(params)
                            .build().toUri())
                    .header("Authorization", token)
                    .retrieve()
                    .onStatus( //Handle common HTTP error for Better Processing On services consuming this functions
                            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).flatMap(
                                    er -> Mono.error(new Exception(er))
                            )
                    )
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            log.error("Error on Pay {}", e.getMessage());
            return null;
        }
    }

//    public JsonNode sendSynchronousInvoiceStatusCheck(String url, HttpMethod httpMethod, String token, MultiValueMap<String, String> params) {
//
//        return webClient.method(httpMethod)
//                .uri(uriBuilder -> UriComponentsBuilder.fromUriString(url)
//                        //    .queryParam("IncludeDownloadedInvoices", false)
//                        .queryParams(params)
//                        .build().toUri())
//                .header("Authorization", token)
//                .retrieve()
//                .bodyToMono(JsonNode.class)
//                .block();
//    }

    public ClientResponse sendSynchronousInvoiceLock(String url, HttpMethod httpMethod, String token, Map<String, String> data) {

        return webClient.method(httpMethod)
                .uri(uriBuilder -> UriComponentsBuilder.fromUriString(url)
                        //    .queryParam("IncludeDownloadedInvoices", false)
                        .build().toUri())
                .header("Authorization", token)
                .bodyValue(data)
                .exchange()
                .block();
    }

    public boolean sendSynchronousFailedStatus(String url, HttpMethod httpMethod, String token, Map<String, String> data) {

        try {
            JsonNode jsonNode = webClient.method(httpMethod)
                    .uri(uriBuilder -> UriComponentsBuilder.fromUriString(url)
                            .build().toUri())
                    .header("Authorization", token)
                    .bodyValue(data)
                    .retrieve()
                    .onStatus( //Handle common HTTP error for Better Processing On services consuming this functions
                            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map((Exception::new)
                            )
                    )
                    .bodyToMono(JsonNode.class)
                    .block();
            log.info("Response in sending failed status {}", jsonNode);
            return true;
        } catch (Exception e) {
            log.error("Error in sending failed status {}", e.getMessage());
            return false;
        }
    }

    public Mono<ClientResponse> sendAsyncRequest(String url, HttpMethod httpMethod, String token, Object requestBody) {
        return webClient.method(httpMethod)
                .uri(uriBuilder -> UriComponentsBuilder.fromUriString(url)
                        //.queryParam("TfPin", tfPin)
                        .build().toUri())
                .header("Authorization", token)
                .bodyValue(requestBody)
                .exchangeToMono(clientResponse -> Mono.just(clientResponse.mutate().build()));

    }

    public Mono<JsonNode> makeBridgeApiCall(HttpMethod method, String endpoint, MultiValueMap<String, String> params) {

        return webClient
                .method(method)
                .uri(uriBuilder -> UriComponentsBuilder.fromUriString(endpoint)
                        .queryParams(params)
                        .build().toUri()
                )
                .headers(headers -> headers.add("Authorization", bridgeAccessToken()))
                .retrieve()
                .onStatus(
                        httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).map(Exception::new)
                )
                .bodyToMono(JsonNode.class);

    }

    public Mono<JsonNode> makeBridgeApiCall(HttpMethod method, String endpoint, Object requestBody) {
        log.info("URL: " + endpoint);
        return webClient
                .method(method)
                .uri(endpoint)
                .headers(headers -> {
                    headers.add("Authorization", bridgeAccessToken());
                    headers.add("Content-Type", "application/json");
                })
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(
                        httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class).map(Exception::new)
                )
                .bodyToMono(JsonNode.class);

    }

    public JsonNode makeBridgeBlockingApiCall(HttpMethod method, String endpoint, Object requestBody) {
        log.info("URL: " + endpoint);
        try {
            return webClient
                    .method(method)
                    .uri(endpoint)
                    .headers(headers -> headers.add("Authorization", bridgeAccessToken()))
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(
                            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map(Exception::new)
                    )
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public String bridgeAccessToken() {

        String endpoint = appConfig.getBridgeEndpoint() + "/api/v1/oauth/token";
        String cred = appConfig.getBridgeApiKey() + ":" + appConfig.getBridgeApiPassword();
        try {
            JsonNode jsonNode = webClient
                    .method(HttpMethod.POST)
                    .uri(endpoint)
                    .headers(headers -> {
                        headers.add("Authorization", "Basic " + Base64.getEncoder().encodeToString(cred.getBytes(StandardCharsets.UTF_8)));
                        headers.add("grant_type", "client_credentials");
                    })
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .retrieve()
                    .onStatus(
                            httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(),
                            clientResponse -> clientResponse.bodyToMono(String.class).map(Exception::new)
                    )
                    .bodyToMono(JsonNode.class)
                    .block();

            assert jsonNode != null;
            String token = jsonNode.get("access_token").asText("");
            //log.info("String token {}",token);
            return "Bearer" + token;
        } catch (Exception e) {
            log.error("token exception {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
