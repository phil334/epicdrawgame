const SERVER_ADDRESS = window.location.origin;

const canvas = document.getElementById('mainCanvas');
const context = canvas.getContext('2d');

const COORDS_ROWS = 40;//81;
const COORDS_COLS = 80;//162;

const ViewMode = {
    CREATE_LOBBY: 'createLobby',
    JOIN_LOBBY: 'joinLobby',
    IN_GAME: 'inGame'
};

const canvasState = {
    tileSize: null,
    topOffset: null,
    currentViewMode: null,
    input: {
        username: null,
        lobbyName: null
    }
};

const lobbyState = {
    stompClient: null,
    lobbyId: null,
    lobbyName: null,
    playerId: null,
    playerSecret: null,
    players: null,
    gameIsOver: false,
    winningPlayerId: null
};

const gameState = {
    coords: null,
    playerScores: null,
    fetchGameStateInterval: null
}

const menuItems = []


const initializeMenuItems = () => {
    menuItems.push({
        viewMode: ViewMode.CREATE_LOBBY,
        text: () => `${canvasState.input.username}`,
        x: () => (canvas.width / 2) - 10 * canvasState.tileSize,
        y: () => canvas.height / 2 - 7 * canvasState.tileSize,
        width: 300,
        height: 40,
        backgroundColor: 'white',
        onload: () => canvasState.input.username ||= 'Enter username',
        onclick: () => {
            canvasState.input.username = prompt('Please enter your username');
            renderLobbyMenu();
        }
    });

    menuItems.push({
        viewMode: ViewMode.CREATE_LOBBY,
        text: () => `${canvasState.input.lobbyName}`,
        x: () => (canvas.width / 2) - 10 * canvasState.tileSize,
        y: () => canvas.height / 2 - 1 * canvasState.tileSize,
        width: 300,
        height: 40,
        backgroundColor: 'white',
        onload: () => canvasState.input.lobbyName ||= 'Enter lobbyname',
        onclick: () => {
            canvasState.input.lobbyName = prompt('Please enter your lobbyName');
            renderLobbyMenu();
        }
    });

    menuItems.push({
        viewMode: ViewMode.CREATE_LOBBY,
        text: () => 'Create Lobby',
        x: () => (canvas.width / 2) - 8 * canvasState.tileSize,
        y: () => canvas.height / 2 + 6 * canvasState.tileSize,
        width: 220,
        height: 70,
        backgroundColor: '#bec2ed',
        onclick: () => createGameRequest(canvasState.input.lobbyName, canvasState.input.username)
    });


    menuItems.push({
        viewMode: ViewMode.JOIN_LOBBY,
        text: () => `${canvasState.input.username}`,
        x: () => (canvas.width / 2) - 10 * canvasState.tileSize,
        y: () => canvas.height / 2 - 7 * canvasState.tileSize,
        width: 300,
        height: 40,
        backgroundColor: 'white',
        onload: () => canvasState.input.username ||= 'Enter username',
        onclick: () => {
            canvasState.input.username = prompt('Please enter your username');
            renderLobbyMenu();
        }
    });
    menuItems.push({
        viewMode: ViewMode.JOIN_LOBBY,
        text: () => 'Join Lobby',
        x: () => (canvas.width / 2) - 8 * canvasState.tileSize,
        y: () => canvas.height / 2 + -1 * canvasState.tileSize,
        width: 220,
        height: 70,
        backgroundColor: '#bec2ed',
        onclick: () => joinGameRequest(new URL(window.location.href).searchParams.get('lobbyId'), canvasState.input.username)
    });
}


const renderCanvas = () => {
    canvasState.tileSize = Math.min(
        Math.floor((window.innerHeight * 0.8) / COORDS_ROWS),
        Math.floor((window.innerWidth * 0.6) / COORDS_COLS)
    );

    canvas.width = canvasState.tileSize * COORDS_COLS;
    canvas.height = (canvasState.tileSize * (COORDS_ROWS + 2)) * 1.2;
    canvasState.topOffset = canvas.height * 0.2

    const bounding = canvas.getBoundingClientRect();

    context.beginPath();
    context.rect(0, 0, bounding.right, bounding.bottom);
    let gradient = context.createLinearGradient(0, 0, bounding.right, canvasState.topOffset);
    gradient.addColorStop(0, '#ff6369');  // Light pink color at the start of gradient
    gradient.addColorStop(1, '#3377ff');  // Light yellow color at the end of gradient
    context.fillStyle = gradient; //'white';
    context.fill();

    if (canvasState.currentViewMode === ViewMode.IN_GAME) {
        renderGameBackground();
        renderScore();
    } else {
        renderLobbyMenu();
    }
};

const renderLobbyMenu = () => {
    if (menuItems.length === 0) initializeMenuItems();

    context.textAlign = 'center';
    context.textBaseline = 'middle';

    context.beginPath();
    context.font = 'bold 90px monospace';
    context.fillStyle = 'black';
    context.fillText('Epic Draw Duel', (canvas.width / 2) - 0 * canvasState.tileSize, canvas.height / 2 - 15 * canvasState.tileSize)
    context.strokeStyle = '#1ceb80';
    context.lineWidth = 3
    context.strokeText('Epic Draw Duel', (canvas.width / 2) - 0 * canvasState.tileSize, canvas.height / 2 - 15 * canvasState.tileSize)
    // context.fillText('Epic Draw Duel', 100, 100)


    context.font = 'bold 30px monospace';

    menuItems.filter(item => item.viewMode === canvasState.currentViewMode).forEach(item => {
        if (item.onload) item.onload();
        context.beginPath();
        context.fillStyle = item.backgroundColor;
        context.fillRect(item.x(), item.y(), item.width, item.height);
        context.fillStyle = 'black';
        context.strokeStyle = 'black';
        context.lineWidth = 5;
        context.strokeRect(item.x(), item.y(), item.width, item.height);
        context.fillText(item.text(), item.x() + item.width / 2, item.y() + item.height / 2);
    });
}

const renderGameBackground = timestamp => {
    for (let x = 0; x <= gameState.coords[0].length; x++) {
        context.beginPath();
        context.moveTo(x * canvasState.tileSize, canvasState.topOffset)
        context.lineTo(x * canvasState.tileSize, (gameState.coords.length * canvasState.tileSize) + canvasState.topOffset);
        context.strokeStyle = 'black';
        context.lineWidth = 1;
        context.stroke();
    }

    for (let y = 0; y <= gameState.coords.length; y++) {
        context.beginPath();
        context.moveTo(0, (y * canvasState.tileSize) + canvasState.topOffset)
        context.lineTo(gameState.coords[0].length * canvasState.tileSize, (y * canvasState.tileSize) + canvasState.topOffset);
        context.strokeStyle = 'black';
        context.lineWidth = 1;
        context.stroke();
    }
};

const renderScore = () => {

    // overwrite previous score area
    const bounding = canvas.getBoundingClientRect();

    let gradient = context.createLinearGradient(0, 0, bounding.right, canvasState.topOffset);
    gradient.addColorStop(0, '#ff6369');  // Light pink color at the start of gradient
    gradient.addColorStop(1, '#3377ff');  // Light yellow color at the end of gradient

    // Score background
    context.beginPath();
    context.fillStyle = gradient;
    context.rect(0, 0, bounding.right, canvasState.topOffset);
    context.fillStyle = gradient//'white';
    context.fill();
    context.strokeStyle = 'black';
    context.lineWidth = 4;
    context.strokeRect(2, 2, bounding.width - 4, canvasState.topOffset - 4)

    let textX = canvasState.tileSize * 12;
    let textY = canvasState.tileSize * 3;


    // TODO stop relying on index (use id instead)
    for (let i = 0; i < 2; i++) {
        if (!gameState.playerScores[i]) continue;
        // Playername
        let playerName = lobbyState.players[i].name;
        if (lobbyState.players[i].id === lobbyState.playerId) {
            playerName += '(YOU)';
        }
        context.beginPath();
        context.font = `${Math.round(canvasState.tileSize * 2.5)}px monospace`;
        context.fillStyle = 'black';
        context.fillText(playerName, textX, textY);
        const textMetrics = context.measureText(playerName);

        // Playercolor box
        context.beginPath();
        context.fillStyle = lobbyState.players[i].color;
        context.fillRect(textX + textMetrics.actualBoundingBoxRight + canvasState.tileSize * 0.8, textY + textMetrics.fontBoundingBoxAscent + canvasState.tileSize * -3.7, canvasState.tileSize * 1.5, canvasState.tileSize * 1.5);
        context.lineWidth = 4;
        context.strokeStyle = 'black';
        context.strokeRect(textX + textMetrics.actualBoundingBoxRight + canvasState.tileSize * 0.8, textY + textMetrics.fontBoundingBoxAscent + canvasState.tileSize * -3.7, canvasState.tileSize * 1.5, canvasState.tileSize * 1.5);

        // Player % score
        context.beginPath();
        context.font = `${Math.round(canvasState.tileSize * 3.5)}px monospace`;
        context.fillStyle = 'black';
        context.fillText(`${Math.floor(gameState.playerScores[i].score * 100)}.${(Math.round(gameState.playerScores[i].score * 1000) % 10)}%`, textX + canvasState.tileSize * 2, textY + canvasState.tileSize * 4);

        textX += canvasState.tileSize * 38;
    }

};

const renderAllFields = () => {
    for (let row = 0; row < gameState.coords.length; row++) {
        for (let col = 0; col < gameState.coords[0].length; col++) {
            const nr = gameState.coords[row][col];

            context.beginPath()
            context.rect((col * canvasState.tileSize) + 1, (row * canvasState.tileSize) + 1 + canvasState.topOffset, canvasState.tileSize - 2, canvasState.tileSize - 2);
            context.fillStyle = nr >= 1 ? lobbyState.players[nr - 1].color : 'white';
            context.fill();
        }
    }
};

const createGameRequest = (lobbyName, playerName) => {
    if (lobbyName.length === 0 || lobbyName.length > 14 || lobbyName === 'Enter lobbyname') {
        alert('Please enter a valid lobbyname (1-14 characters)');
        return;
    }
    if (playerName.length === 0 || playerName.length > 14 || playerName === 'Enter username') {
        alert('Please enter a valid username (1-14 characters)');
        return;
    }


    fetch(SERVER_ADDRESS + '/epic-draw/create-game', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({lobbyName, playerName})
    })
        .then(response => response.json())
        .then(handleLobbyEnterResponse);
}

const joinGameRequest = (lobbyId, playerName) => {
    if (playerName.length === 0 || playerName.length > 14 || playerName === 'Enter username') {
        alert('Please enter a valid name');
        return;
    }

    fetch(SERVER_ADDRESS + '/epic-draw/join-game', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({lobbyId, playerName})
    })
        .then(async response => {
            return response.json()
        })
        .then(handleLobbyEnterResponse);
}

const handleLobbyEnterResponse = responseBody => {
    if (responseBody.error) {
        alert(responseBody.error)
        return;
    }
    lobbyState.lobbyId = responseBody.lobbyId;
    lobbyState.lobbyName = responseBody.lobbyName;
    lobbyState.playerId = responseBody.playerId;
    lobbyState.playerSecret = responseBody.playerSecret;
    startGame();
}


const fetchLobbyAndGameState = async () => {
    return fetch(SERVER_ADDRESS + '/epic-draw/fetch-game-state', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({lobbyId: lobbyState.lobbyId, secret: lobbyState.playerSecret})
    })
        .then(response => response.json())
        .then(responseBody => {
            if (responseBody.error) throw new Error(responseBody.error);

            if (responseBody.lobbyState.gameIsOver) {
                // TODO search by Id instead of relying on index
                alert(`${lobbyState.players[responseBody.lobbyState.winningPlayerId - 1].name} has won the game!`);
                // disable mouse move event handler
            }

            lobbyState.players = responseBody.lobbyState.players;
            lobbyState.gameIsOver = responseBody.lobbyState.gameIsOver;
            lobbyState.winningPlayerId = responseBody.lobbyState.winningPlayerId;
            gameState.coords = responseBody.coords;
            gameState.playerScores = responseBody.playerScores;
        })
        .catch(error => {
            console.error(error);
            handleLobbyLeave();
        });
}


const fieldPlaceRequest = (updatedCoords) => {
    lobbyState.stompClient.send(
        "/app/field-placement",
        {},
        JSON.stringify({updatedCoords, secret: lobbyState.playerSecret})
    );
}


// TODO only render updates
// const updateFields = timestamp => {
//     for (let row = 0; row < gameState['coords'].length; row++) {
//         for (let col = 0; col < gameState['coords'][0].length; col++) {
//             const nr = gameState['coords'][row][col];
//
//             context.beginPath()
//             context.rect((col * canvasState.tileSize) + 1, (row * canvasState.tileSize) + 1 + canvasState.topOffset, canvasState.tileSize - 2, canvasState.tileSize - 2);
//             context.fillStyle = nr >= 1 ? gameState['players'][nr - 1].color : 'white';
//             context.fill();
//         }
//     }
// };


const restartGameRequest = password => {
    fetch(SERVER_ADDRESS + '/epic-draw/restart-game', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({password, lobbyId: lobbyState.lobbyId, secret: lobbyState.playerSecret})
    })
        .then(response => {
            if (!response.ok) {
                alert('Nice try lilbro');
            }
        });
}
const handleMenuClick = event => {
    const isIntersect = (point, box) => {
        return point.x >= box.x() && point.x <= box.x() + box.width && point.y >= box.y() && point.y <= box.y() + box.height;
    }

    const pos = {
        x: event.clientX - canvas.getBoundingClientRect().left,
        y: event.clientY - canvas.getBoundingClientRect().top,
    };

    menuItems
        .filter(item => item.onclick)
        .filter(item => item.viewMode === canvasState.currentViewMode)
        .filter(item => isIntersect(pos, item))
        .forEach(item => item.onclick());
};

const handleMouseMove = event => {
    if (event.buttons === 0) return;

    const bounding = canvas.getBoundingClientRect();
    const x = event.clientX - bounding.left;
    const y = event.clientY - bounding.top;

    const col_idx = Math.floor(x / canvasState.tileSize);
    const row_idx = Math.floor((y - canvasState.topOffset) / canvasState.tileSize);


    if (col_idx < 0 || row_idx < 0 || row_idx >= gameState.coords.length || col_idx >= gameState.coords[0].length
        || gameState.coords[row_idx][col_idx] === lobbyState.playerId) {
        return;
    }

    fieldPlaceRequest([{'row': row_idx, 'col': col_idx}])
};

const handleGameStateUpdateMessage = message => {
    // TODO wait for fetchLobbyAndGameState to be complete
    const messageBody = JSON.parse(message.body);
    for (let updatedCoord of messageBody.updatedCoords) {
        gameState.coords[updatedCoord.row][updatedCoord.col] = updatedCoord.value;
    }
    gameState.playerScores = messageBody.playerScores;
}

const handleLobbyStateMessage = message => {
    const messageBody = JSON.parse(message.body);
    lobbyState.players = messageBody.players;
    if (messageBody.gameIsOver && !lobbyState.gameIsOver) {
        // TODO search by Id instead of relying on index
        alert(`${lobbyState.players[messageBody.winningPlayerId - 1].name} has won the game!`);
        // TODO disable mouse move event handler
    }
    lobbyState.gameIsOver = messageBody.gameIsOver;
    lobbyState.winningPlayerId = messageBody.winningPlayerId;
}

const handleLobbyLeave = () => {
    canvasState.currentViewMode = ViewMode.CREATE_LOBBY;
    if (gameState.fetchGameStateInterval !== null) {
        clearInterval(gameState.fetchGameStateInterval);
    }
    if (new URL(window.location.href).searchParams.has('lobbyId')) {
        history.pushState(null, '', '/epic-draw');
    }
    removeEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('click', handleMenuClick);
    renderCanvas();
}

const gameLoopRender = timestamp => {
    if (canvasState.currentViewMode !== ViewMode.IN_GAME) return;

    renderAllFields();
    renderScore();
    requestAnimationFrame(gameLoopRender);
};

const connectToGameSocket = () => {
    return new Promise((resolve, reject) => {
        const socket = new SockJS('/epic-draw-ws?secret=' + lobbyState.playerSecret);
        lobbyState.stompClient = Stomp.over(socket);

        lobbyState.stompClient.connect(
            {Secret: lobbyState.playerSecret},
            (_) => {
                resolve();
            },
            (error) => {
                console.log("ERROR WHEN CONNECT")
                reject(error);
            });
    });
}

const startGame = () => {
    canvasState.currentViewMode = ViewMode.IN_GAME;
    connectToGameSocket()
        .then(() => {
            return Promise.all(
                [lobbyState.stompClient.subscribe('/user/queue/game-coords-update', handleGameStateUpdateMessage)],
                [lobbyState.stompClient.subscribe('/user/queue/lobby-state', handleLobbyStateMessage)]
            );
        })
        .then(fetchLobbyAndGameState)
        .then(() => {
            if (canvasState.currentViewMode !== ViewMode.IN_GAME) return;

            if (!new URL(window.location.href).searchParams.has('lobbyId')) {
                history.pushState(null, '', '?lobbyId=' + lobbyState.lobbyId);
            }
            canvas.removeEventListener('click', handleMenuClick);
            canvas.addEventListener('mousemove', handleMouseMove);

            renderCanvas();
            renderGameBackground();
            requestAnimationFrame(gameLoopRender);

            // gameState.fetchGameStateInterval = setInterval(fetchGameState, 1000 / 30);
        })
        .catch(error => {
            console.log("ERROR WHEN STARTING GAME")
            console.error(error);
            handleLobbyLeave();
        });
}

if (new URL(window.location.href).searchParams.get('lobbyId') != null) {
    canvasState.currentViewMode = ViewMode.JOIN_LOBBY;
} else {
    canvasState.currentViewMode = ViewMode.CREATE_LOBBY;
}

renderCanvas();

window.addEventListener('resize', renderCanvas);
canvas.addEventListener('click', handleMenuClick);
