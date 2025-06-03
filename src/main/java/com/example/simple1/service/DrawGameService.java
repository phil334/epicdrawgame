package com.example.simple1.service;

import com.example.simple1.drawgame.DrawGame;
import com.example.simple1.drawgame.dto.DrawGameRequests.*;
import com.example.simple1.drawgame.dto.DrawGameResponse.DrawGamePlayerDto;
import com.example.simple1.drawgame.dto.DrawGameResponse.FetchLobbyAndGameStateResponse;
import com.example.simple1.drawgame.dto.DrawGameResponse.GameStateUpdateResponse;
import com.example.simple1.drawgame.dto.DrawGameResponse.UpdatedCoordDto;
import com.example.simple1.drawgame.dto.DrawGameResponse.ActiveLobbyDto;
import com.example.simple1.exception.bad_request.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.simple1.drawgame.dto.DrawGameResponse.GameEnterResponse;

@Service
public class DrawGameService {


    private final SimpMessagingTemplate simpMessagingTemplate;
    private final Map<Integer, DrawGame> lobbyIdToGame;
    private final Map<String, Integer> playerSecretToLobbyId;
    private final Map<String, Integer> playerSecretToPlayerId;
    private final Map<String, String> lobbyIdAndPlayerIdToSecret;

    private final AtomicInteger gameIdCounter;

    @Autowired
    public DrawGameService(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;

        lobbyIdToGame = new ConcurrentHashMap<>();
        playerSecretToLobbyId = new ConcurrentHashMap<>();
        playerSecretToPlayerId = new ConcurrentHashMap<>();
        lobbyIdAndPlayerIdToSecret = new ConcurrentHashMap<>();
        gameIdCounter = new AtomicInteger(0);
    }

    public GameEnterResponse createGame(CreateGameRequest createGameRequest) {
        int lobbyId = gameIdCounter.incrementAndGet();
        DrawGame drawGame = new DrawGame(lobbyId, createGameRequest.lobbyName());
        lobbyIdToGame.put(lobbyId, drawGame);

        return addPlayer(drawGame, createGameRequest.playerName());
    }

    public GameEnterResponse joinGame(JoinGameRequest joinGameRequest) {
        verifyLobbyId(joinGameRequest.lobbyId());
        DrawGame drawGame = lobbyIdToGame.get(joinGameRequest.lobbyId());
        GameEnterResponse gameEnterResponse = addPlayer(drawGame, joinGameRequest.playerName());
        sendToAllLobbyPlayers(drawGame, "/queue/lobby-state", drawGame.getLobbyStateResponse());
        sendToAllLobbyPlayers(drawGame, "/queue/game-coords-update", new GameStateUpdateResponse(new ArrayList<>(), drawGame.getPlayerScores()));
        return gameEnterResponse;
    }

    public void restartGame(RestartGameRequest restartGameRequest) {
        if (!restartGameRequest.password().equals("supersecret123")) {
            throw new BadRequestException("Restart request was denied!");
        }
        verifyLobbyId(restartGameRequest.lobbyId());
        DrawGame drawGame = lobbyIdToGame.get(restartGameRequest.lobbyId());
        drawGame.resetGame();

        GameStateUpdateResponse gameStateUpdateResponse = new GameStateUpdateResponse(new ArrayList<>(), drawGame.getPlayerScores());
        // todo temp solution
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                gameStateUpdateResponse.updatedCoords().add(new UpdatedCoordDto(row, col, 0));
            }
        }
        sendToAllLobbyPlayers(drawGame, "/queue/game-coords-update", gameStateUpdateResponse);

    }

    public FetchLobbyAndGameStateResponse fetchGameState(FetchGameStateRequest fetchGameStateRequest) {
        verifyLobbyId(fetchGameStateRequest.lobbyId());
        DrawGame drawGame = lobbyIdToGame.get(fetchGameStateRequest.lobbyId());
        if (!fetchGameStateRequest.lobbyId().equals(playerSecretToLobbyId.get(fetchGameStateRequest.secret()))) {
            throw new BadRequestException("The player is not part of the requested lobby '%s'!".formatted(fetchGameStateRequest.lobbyId()));
        }

        return drawGame.getFetchGameStateResponse();
    }

    public void fieldPlacement(FieldPlacementRequest fieldPlacementRequest) {
        Integer lobbyId = playerSecretToLobbyId.get(fieldPlacementRequest.secret());
        if (lobbyId == null) {
            throw new BadRequestException("The provided secret doesn't belong to any active lobby!");
        }

        verifyLobbyId(lobbyId);
        DrawGame drawGame = lobbyIdToGame.get(lobbyId);

        if (drawGame.isGameIsOver()) return;

        Integer playerId = playerSecretToPlayerId.get(fieldPlacementRequest.secret());
        if (playerId == null) {
            throw new BadRequestException("The provided secret does not have a playerId!");
        }

        List<UpdatedCoordDto> updatedCoordDtoList = new ArrayList<>();
        for (CoordDto coordDto : fieldPlacementRequest.updatedCoords()) {
            drawGame.changeCoord(playerId, coordDto.row(), coordDto.col());
            updatedCoordDtoList.add(new UpdatedCoordDto(coordDto.row(), coordDto.col(), playerId));
        }
        GameStateUpdateResponse gameStateUpdateResponse = new GameStateUpdateResponse(updatedCoordDtoList, drawGame.getPlayerScores());

        sendToAllLobbyPlayers(drawGame, "/queue/game-coords-update", gameStateUpdateResponse);
        if (drawGame.isGameIsOver()) {
            sendToAllLobbyPlayers(drawGame, "/queue/lobby-state", drawGame.getLobbyStateResponse());
        }
    }

    public void verifyLobbyId(Integer lobbyId) {
        if (lobbyId == null || !lobbyIdToGame.containsKey(lobbyId)) {
            throw new BadRequestException("LobbyId '%d' has not been found!".formatted(lobbyId));
        }
    }

    public List<ActiveLobbyDto> getActiveLobbies() {
        return lobbyIdToGame.values().stream()
                .map(game -> new ActiveLobbyDto(game.getGameId(), game.getLobbyName(), game.getPlayers().size()))
                .toList();
    }

    private GameEnterResponse addPlayer(DrawGame drawGame, String playerName) {
        int playerId = drawGame.addPlayer(playerName);
        if (playerId < 0) {
            throw new BadRequestException("Could not join lobby '%d'. It likely is already full".formatted(drawGame.getGameId()));
        }
        String playerSecret = createPlayerSecret();
        playerSecretToLobbyId.put(playerSecret, drawGame.getGameId());
        playerSecretToPlayerId.put(playerSecret, playerId);
        lobbyIdAndPlayerIdToSecret.put(drawGame.getGameId() + ";" + playerId, playerSecret);

        return new GameEnterResponse(drawGame.getGameId(), drawGame.getLobbyName(), playerId, playerSecret);
    }

    private void sendToAllLobbyPlayers(DrawGame drawGame, String destination, Object payload) {
        for (DrawGamePlayerDto drawGamePlayerDto : drawGame.getPlayers()) {
            String drawGamePlayerSecret = lobbyIdAndPlayerIdToSecret.get(drawGame.getGameId() + ";" + drawGamePlayerDto.id());
            if (drawGamePlayerSecret == null) {
                System.out.println("Warning: No secret available for a player");
                continue;
            }
            simpMessagingTemplate.convertAndSendToUser(drawGamePlayerSecret, destination, payload);
        }
    }

    private String createPlayerSecret() {
        return "SECRET_" + UUID.randomUUID();
    }

}
