package com.example.simple1.drawgame.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@SuppressWarnings("unused")
public class DrawGameRequests {

    public record CreateGameRequest(
            @Size(min = 1, max = 14)
            @NotNull String lobbyName,
            @Size(min = 1, max = 14) @NotNull String playerName
    ) {
    }

    public record JoinGameRequest(
            @NotNull Integer lobbyId,
            @Size(min = 1, max = 14)
            @NotNull String playerName
    ) {
    }

    public record RestartGameRequest(
            @NotNull Integer lobbyId,
            @NotNull @Size(max = 100) String secret,
            @NotNull @Size(max = 100) String password
    ) {

    }

    public record FetchGameStateRequest(
            @NotNull Integer lobbyId,
            @NotNull @Size(max = 100) String secret
    ) {
    }

    public record FieldPlacementRequest(
            @NotNull List<CoordDto> updatedCoords,

            @NotNull @Size(max = 100) String secret
    ) {
    }

    public record CoordDto(
            @NotNull Integer row,
            @NotNull Integer col
    ) {
    }

}
