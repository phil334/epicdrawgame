package com.example.simple1.controller;

import com.example.simple1.service.DrawGameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;

import static com.example.simple1.drawgame.dto.DrawGameRequests.FieldPlacementRequest;
import static com.example.simple1.drawgame.dto.DrawGameRequests.UseAbilityRequest;

@Controller
public class DrawGameWsController {

    private final DrawGameService drawGameService;

    @Autowired
    public DrawGameWsController(DrawGameService drawGameService) {
        this.drawGameService = drawGameService;
    }

    @MessageMapping("/field-placement")
    public void fieldPlacement(@Valid @RequestBody FieldPlacementRequest fieldPlacementRequest) {
        drawGameService.fieldPlacement(fieldPlacementRequest);
    }

    @MessageMapping("/use-ability")
    public void useAbility(@Valid @RequestBody UseAbilityRequest useAbilityRequest) {
        drawGameService.useAbility(useAbilityRequest);
    }
}
