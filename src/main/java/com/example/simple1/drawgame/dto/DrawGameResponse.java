package com.example.simple1.drawgame.dto;

import java.util.List;

@SuppressWarnings("unused")
public class DrawGameResponse {

    public record GameEnterResponse(
            int lobbyId,
            String lobbyName,
            int playerId,
            String playerSecret
    ) {
    }

    public record FetchLobbyAndGameStateResponse(
            LobbyStateResponse lobbyState,
            int[][] coords,
            List<PlayerScoreDto> playerScores
    ) {
    }

    public record LobbyStateResponse(
            List<DrawGamePlayerDto> players,
            boolean gameIsOver,
            int winningPlayerId
    ) {
    }

    public record DrawGamePlayerDto(
            int id,
            String name,
            String color
    ) {
    }

    public record GameStateUpdateResponse(
            List<UpdatedCoordDto> updatedCoords,
            List<PlayerScoreDto> playerScores
    ) {
    }

    public record UpdatedCoordDto(
            int row,
            int col,
            int value
    ) {
    }

    public record PlayerScoreDto(
            int id,
            double score
    ) {
    }
}
