package com.example.simple1.controller;

import com.example.simple1.drawgame.dto.DrawGameResponse.FetchLobbyAndGameStateResponse;
import com.example.simple1.drawgame.dto.DrawGameResponse.ActiveLobbyDto;
import com.example.simple1.exception.bad_request.BadRequestException;
import com.example.simple1.service.DrawGameService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.simple1.drawgame.dto.DrawGameRequests.*;
import static com.example.simple1.drawgame.dto.DrawGameResponse.GameEnterResponse;

@RestController()
@RequestMapping("/epic-draw")
public class DrawGameController {

    private final DrawGameService drawGameService;
    private final SimpUserRegistry simpUserRegistry;

    @Autowired
    public DrawGameController(DrawGameService drawGameService, SimpUserRegistry simpUserRegistry) {
        this.drawGameService = drawGameService;
        this.simpUserRegistry = simpUserRegistry;
    }


    @GetMapping()
    public ResponseEntity<Resource> loadJoinGamePage(@RequestParam(required = false) Integer lobbyId) {
        if (lobbyId != null) {
            drawGameService.verifyLobbyId(lobbyId);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new ClassPathResource("templates/index.html"));
    }

    @PostMapping("/create-game")
    public GameEnterResponse createGame(@Valid @RequestBody CreateGameRequest createGameRequest) {
        return drawGameService.createGame(createGameRequest);
    }

    @PostMapping("/join-game")
    public GameEnterResponse joinGame(@Valid @RequestBody JoinGameRequest joinGameRequest) {
        return drawGameService.joinGame(joinGameRequest);
    }

    @PostMapping("/restart-game")
    public ResponseEntity<Void> restartGame(@Valid @RequestBody RestartGameRequest restartGameRequest) {
        drawGameService.restartGame(restartGameRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/fetch-game-state")
    public FetchLobbyAndGameStateResponse fetchGameState(@Valid @RequestBody FetchGameStateRequest fetchGameStateRequest) {
        return drawGameService.fetchGameState(fetchGameStateRequest);
    }

    @GetMapping("/active-lobbies")
    public List<ActiveLobbyDto> getActiveLobbies() {
        return drawGameService.getActiveLobbies();
    }

    @Deprecated()
    @PostMapping("/field-placement")
    public ResponseEntity<Void> FieldPlacement(@Valid @RequestBody FieldPlacementRequest fieldPlacementRequest) {
        drawGameService.fieldPlacement(fieldPlacementRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ws-users")
    public Map<String, String> getConnectedUsers(@RequestParam(required = false) String pw) {
        if (pw == null || !pw.equals("very-secret7657")) {
            throw new BadRequestException("Request denied");
        }
        String res = simpUserRegistry.getUsers().stream()
                .map(simpUser -> "User: " + simpUser.getName() + " Sessions: " + simpUser.getSessions().size())
                .collect(Collectors.joining("\n"));
        return Map.of("response", res);
    }

}
