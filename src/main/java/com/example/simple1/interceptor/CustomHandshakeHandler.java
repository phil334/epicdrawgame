package com.example.simple1.interceptor;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.Map;

public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String secret = UriComponentsBuilder
                .fromUriString(request.getURI().toString())
                .build()
                .getQueryParams()
                .getFirst("secret");

        if (secret == null) System.out.println("Warning: No secret provided in handshake");
        return () -> secret;
    }


}
