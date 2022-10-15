package com.mtvu.websocketserver.controller;

import com.mtvu.websocketserver.domain.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class WebsocketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);

    @MessageMapping("/message")
    public Message incoming(Principal principal, @Payload Message message) {
        LOGGER.info("Sending a message from user {} to user {}", principal.getName(), message.getUsername());
        return message;
    }
}
