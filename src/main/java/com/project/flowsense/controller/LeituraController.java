package com.project.flowsense.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.flowsense.dto.request.LeituraRequest;

 
    
@RestController
@RequestMapping("/api/leituras")
public class LeituraController {

    private final SimpMessagingTemplate messagingTemplate;

    public LeituraController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public void receber(@RequestBody LeituraRequest request) {
        messagingTemplate.convertAndSend("/topic/leituras", request);
    }
}
 
