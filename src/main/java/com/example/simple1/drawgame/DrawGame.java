package com.example.simple1.drawgame;

import com.example.simple1.drawgame.dto.DrawGameResponse.DrawGamePlayerDto;
import com.example.simple1.drawgame.dto.DrawGameResponse.FetchLobbyAndGameStateResponse;
import com.example.simple1.drawgame.dto.DrawGameResponse.LobbyStateResponse;
import com.example.simple1.drawgame.dto.DrawGameResponse.PlayerScoreDto;
import com.example.simple1.exception.bad_request.BadRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DrawGame {

    private static final String GAME_TYPE = "CLICK";//"MOVE";
    private static final int COORDS_ROWS = GAME_TYPE.equals("MOVE") ? 40 : 10;
    private static final int COORDS_COLS = GAME_TYPE.equals("MOVE") ? 80 : 20;
    private static final int TOTAL_FIELDS = COORDS_ROWS * COORDS_COLS;
    private static final double WIN_THRESHOLD = 0.5;

    private final int gameId;
    private final String lobbyName;
    private final DrawGamePlayerDto[] players;

    private int[][] coords;
    private int joinedPlayers;
    private int player1Fields;
    private int player2Fields;
    private boolean gameIsOver;
    private int winnerPlayerId;

    public DrawGame(int gameId, String lobbyName) {
        this.gameId = gameId;
        this.lobbyName = lobbyName;
        this.players = new DrawGamePlayerDto[]{null, null};
        this.joinedPlayers = 0;
        this.resetGame();
    }

    public synchronized void resetGame() {
        coords = new int[COORDS_ROWS][COORDS_COLS];
        player1Fields = 0;
        player2Fields = 0;
        gameIsOver = false;
        winnerPlayerId = -1;
    }

    public int addPlayer(String playerName) {
        if (joinedPlayers >= 2) return -1;

        if (joinedPlayers == 0) {
            players[0] = new DrawGamePlayerDto(1, playerName, "red");
        } else {
            players[1] = new DrawGamePlayerDto(2, playerName, "blue");
        }
        joinedPlayers += 1;
        return joinedPlayers;
    }

    public synchronized void changeCoord(int playerId, int row, int col) {
        if (gameIsOver) return;

        if (coords[row][col] == 1) player1Fields--;
        else if (coords[row][col] == 2) player2Fields--;

        if (playerId == 1) player1Fields++;
        else if (playerId == 2) player2Fields++;
        else {
            throw new BadRequestException("player with id '%d' is not allowed to change coords in lobby '%d'".formatted(playerId, gameId));
        }

        coords[row][col] = playerId;

        gameOverCheck();
    }

    private void gameOverCheck() {
        if (player1Fields > (TOTAL_FIELDS * WIN_THRESHOLD)) {
            gameIsOver = true;
            winnerPlayerId = players[0].id();
        } else if (player2Fields > (TOTAL_FIELDS * WIN_THRESHOLD)) {
            gameIsOver = true;
            winnerPlayerId = players[1].id();
        }
    }

    public synchronized FetchLobbyAndGameStateResponse getFetchGameStateResponse() {
        return new FetchLobbyAndGameStateResponse(
                getLobbyStateResponse(),
                coords,
                getPlayerScores()
        );
    }

    public synchronized LobbyStateResponse getLobbyStateResponse() {
        return new LobbyStateResponse(
                Arrays.stream(players).filter(Objects::nonNull).toList(),
                gameIsOver,
                winnerPlayerId
        );
    }

    public synchronized List<PlayerScoreDto> getPlayerScores() {
        List<PlayerScoreDto> playerScores = new ArrayList<>();
        if (joinedPlayers >= 1) {
            playerScores.add(new PlayerScoreDto(players[0].id(), (double) player1Fields / TOTAL_FIELDS));
        }
        if (joinedPlayers >= 2) {
            playerScores.add(new PlayerScoreDto(players[1].id(), (double) player2Fields / TOTAL_FIELDS));
        }
        return playerScores;
    }

    public synchronized int getGameId() {
        return gameId;
    }

    public synchronized String getLobbyName() {
        return lobbyName;
    }

    public synchronized boolean isGameIsOver() {
        return gameIsOver;
    }

    public synchronized int getWinnerPlayerId() {
        return winnerPlayerId;
    }

    public void setWinnerPlayerId(int winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public List<DrawGamePlayerDto> getPlayers() {
        return Arrays.stream(players).filter(Objects::nonNull).toList();
    }

}
