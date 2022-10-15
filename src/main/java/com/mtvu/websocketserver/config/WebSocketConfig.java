package com.mtvu.websocketserver.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@Profile("prod")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${broker.relay.host}")
    private String brokerRelayHost;

    @Value("${broker.relay.port}")
    private int brokerRelayPort;

    @Value("${broker.relay.user}")
    private String brokerUsername;

    @Value("${broker.relay.password}")
    private String brokerPassword;


    @Autowired
    private AuthenticationProvider authenticationProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue", "/topic")
            .setRelayHost(brokerRelayHost)
            .setRelayPort(brokerRelayPort)
            .setSystemLogin(brokerUsername)
            .setSystemPasscode(brokerPassword)
            .setClientLogin(brokerUsername)
            .setClientPasscode(brokerPassword)
            .setUserDestinationBroadcast("/topic/unresolved-user")
            .setUserRegistryBroadcast("/topic/log-user-registry");

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/websocket");
        registry.addEndpoint("/websocket").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                    MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token == null || !token.startsWith("Bearer ")) {
                        throw new AccessDeniedException("Unauthorised");
                    }
                    token = token.substring(7);
                    BearerTokenAuthenticationToken authenticationRequest = new BearerTokenAuthenticationToken(token);
                    Authentication user = authenticationProvider.authenticate(authenticationRequest);
                    if (!user.isAuthenticated()) {
                        throw new AccessDeniedException("Unauthorised");
                    }
                    accessor.setUser(user);
                }
                return message;
            }
        });
    }
}
