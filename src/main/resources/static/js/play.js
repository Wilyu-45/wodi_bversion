let currentRoomCode = '';
let currentNickname = '';
let refreshInterval = null;
let playerData = null;
let gameData = null;
let myIdentity = null;
let myWord = null;
let currentRound = 1;
let lastSpeakingStatus = null;
let lastPhase = null;
let lastActionBarContent = null;
let lastGameStatus = null;
let ws = null;
let wsConnected = false;
let myGuesses = [];
let localGuessInputs = {};

function init() {
    const params = new URLSearchParams(window.location.search);
    const urlRoomCode = params.get('code');
    const urlNickname = params.get('nickname');

    console.log('URL params:', { urlRoomCode, urlNickname });

    if (!urlRoomCode || !urlNickname) {
        showToast('无效的访问，请从房间加入', 'error');
        setTimeout(() => {
            window.location.href = 'game.html';
        }, 1500);
        return;
    }

    currentRoomCode = urlRoomCode;
    currentNickname = decodeURIComponent(urlNickname);

    console.log('After init:', { currentRoomCode, currentNickname });

    document.title = `谁是卧底 - ${currentNickname}`;

    loadData();
    refreshInterval = setInterval(loadData, 2000);
    connectWebSocket();
}

function connectWebSocket() {
    if (!currentNickname) return;

    const wsUrl = 'ws://' + window.location.host + '/ws/game?nickname=' + encodeURIComponent(currentNickname);
    ws = new WebSocket(wsUrl);

    ws.onopen = function() {
        console.log('WebSocket connected');
        wsConnected = true;
        sendWsMessage({ type: 'GET_ROOM_INFO', roomCode: currentRoomCode });
    };

    ws.onmessage = function(event) {
        try {
            const data = JSON.parse(event.data);
            console.log('WebSocket message received:', data);
            if (data.success && data.data) {
                handleWebSocketUpdate(data.data);
            }
        } catch (e) {
            console.error('Error parsing WebSocket message:', e);
        }
    };

    ws.onclose = function() {
        console.log('WebSocket disconnected');
        wsConnected = false;
        setTimeout(connectWebSocket, 3000);
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
    };
}

function sendWsMessage(message) {
    if (ws && wsConnected && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(message));
    }
}

function handleWebSocketUpdate(roomData) {
    if (!roomData || !roomData.status) return;

    gameData = roomData;

    if (roomData.status === 'white_board_guess') {
        window.location.href = 'guess-word.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
    } else if (roomData.status && roomData.status.endsWith('_win')) {
        const winnerMap = {
            'civilian_win': '平民胜利',
            'undercover_win': '卧底胜利',
            'blank_win': '白板胜利'
        };
        const winner = winnerMap[roomData.status] || '游戏结束';
        window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname) + '&winner=' + encodeURIComponent(winner);
    }
}

function loadData() {
    console.log('loadData called, currentRoomCode:', currentRoomCode, 'currentNickname:', currentNickname);

    Promise.all([
        fetch('/api/game/' + currentRoomCode + '/player?nickname=' + encodeURIComponent(currentNickname)).then(r => r.json()),
        fetch('/api/game/room/' + currentRoomCode).then(r => r.json()),
        fetch('/api/game/' + currentRoomCode + '/guesses').then(r => r.json())
    ])
    .then(([playerResult, roomResult, guessesResult]) => {
        console.log('playerResult:', playerResult);
        console.log('roomResult:', roomResult);
        console.log('guessesResult:', guessesResult);

        if (guessesResult.success) {
            myGuesses = guessesResult.data.filter(g => g.guesserNickname === currentNickname);
            myGuesses.forEach(g => {
                if (g.status === 'editing' && g.guessIdentity) {
                    localGuessInputs[g.targetNickname] = g.guessIdentity;
                }
            });
        }

        if (playerResult.success) {
            playerData = playerResult.data;
            myIdentity = playerData.identity;
            myWord = playerData.word;
            console.log('myIdentity:', myIdentity, 'myWord:', myWord);
            updateMyInfo();
        }

        if (roomResult.success) {
            gameData = roomResult.data;
            console.log('gameData loaded:', gameData);

            if (gameData.currentRound) {
                currentRound = gameData.currentRound;
            }

            updatePlayerTable();
            updateGuessSection();
            updateRestrictedWord();

            if (playerData) {
                const currentSpeakingStatus = playerData.speakingStatus == 1 || playerData.speakingStatus === '1';
                if (lastSpeakingStatus !== currentSpeakingStatus) {
                    lastSpeakingStatus = currentSpeakingStatus;
                    updateActionBar();
                }
            }

            const currentPhase = gameData.currentPhase || 'day';
            if (lastPhase !== currentPhase) {
                lastPhase = currentPhase;
                updateActionBar();
            }

            const currentStatus = gameData.status;
            if (lastGameStatus !== currentStatus) {
                lastGameStatus = currentStatus;
                if (currentStatus === 'white_board_guess') {
                    if (playerData && myIdentity === 'blank') {
                        window.location.href = 'guess-word.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
                    } else {
                        updateActionBar();
                    }
                }
            }

            if (gameData.status === 'waiting' || gameData.status === 'setting') {
                window.location.href = 'waiting.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
            } else if (gameData.status && gameData.status.endsWith('_win')) {
                const winnerMap = {
                    'civilian_win': '平民胜利',
                    'undercover_win': '卧底胜利',
                    'blank_win': '白板胜利'
                };
                const winner = winnerMap[gameData.status] || '游戏结束';
                window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname) + '&winner=' + encodeURIComponent(winner);
            }
        }
    })
    .catch(error => {
        console.error('Error loading data:', error);
    });
}

function updateMyInfo() {
    const wordDisplayEl = document.getElementById('wordDisplay');
    const secondWordDisplayEl = document.getElementById('secondWordDisplay');
    const wordDividerEl = document.getElementById('wordDivider');

    if (wordDisplayEl) {
        wordDisplayEl.textContent = myWord || '等待分发';
    }

    if (playerData && playerData.secondWord) {
        if (secondWordDisplayEl) {
            secondWordDisplayEl.textContent = playerData.secondWord;
            secondWordDisplayEl.style.display = '';
        }
        if (wordDividerEl) {
            wordDividerEl.style.display = '';
        }
    } else {
        if (secondWordDisplayEl) {
            secondWordDisplayEl.style.display = 'none';
        }
        if (wordDividerEl) {
            wordDividerEl.style.display = 'none';
        }
    }
}

function updateRestrictedWord() {
    const restrictedWordCard = document.getElementById('restrictedWordCard');
    const restrictedWordDisplay = document.getElementById('restrictedWordDisplay');
    if (!restrictedWordCard || !restrictedWordDisplay || !gameData) return;

    const restrictedWord = gameData.settings?.restrictedWord;
    if (restrictedWord) {
        restrictedWordDisplay.textContent = restrictedWord;
        restrictedWordCard.style.display = '';
    } else {
        restrictedWordCard.style.display = 'none';
    }
}

function updateGuessSection() {
    const guessList = document.getElementById('guessList');
    if (!guessList) return;

    if (!gameData) {
        guessList.innerHTML = '<p class="no-guesses">等待游戏开始...</p>';
        return;
    }

    const otherPlayers = gameData.players.filter(p => p.identity !== 'judge' && p.playerNickname !== currentNickname);

    otherPlayers.forEach(player => {
        const myGuessForThisPlayer = myGuesses.find(g => g.targetNickname === player.playerNickname);
        const guessValue = myGuessForThisPlayer ? myGuessForThisPlayer.guessIdentity : '';
        const savedValue = localGuessInputs[player.playerNickname] || guessValue || '';

        let guessItem = document.getElementById('guess-item-' + player.playerNickname);
        let inputEl = document.getElementById('guess-input-' + player.playerNickname);

        if (!guessItem) {
            guessItem = document.createElement('div');
            guessItem.className = 'guess-item';
            guessItem.id = 'guess-item-' + player.playerNickname;

            const nameSpan = document.createElement('span');
            nameSpan.className = 'player-name';
            nameSpan.textContent = player.playerNickname;

            guessItem.appendChild(nameSpan);
            guessList.appendChild(guessItem);
        }

        if (!inputEl) {
            inputEl = document.createElement('input');
            inputEl.type = 'text';
            inputEl.className = 'guess-input';
            inputEl.id = 'guess-input-' + player.playerNickname;
            inputEl.placeholder = '输入猜测';
            inputEl.oninput = function() {
                handleGuessInput(player.playerNickname, this.value);
            };
            guessItem.appendChild(inputEl);
        }
        if (document.activeElement !== inputEl) {
            inputEl.value = savedValue;
        }
    });

    const existingItems = guessList.querySelectorAll('.guess-item');
    existingItems.forEach(item => {
        const playerNickname = item.id.replace('guess-item-', '');
        if (!otherPlayers.find(p => p.playerNickname === playerNickname)) {
            item.remove();
        }
    });

    if (guessList.children.length === 0) {
        guessList.innerHTML = '<p class="no-guesses">暂无其他玩家</p>';
    }
}

let guessSaveTimers = {};

function handleGuessInput(targetNickname, value) {
    localGuessInputs[targetNickname] = value;

    if (guessSaveTimers[targetNickname]) {
        clearTimeout(guessSaveTimers[targetNickname]);
    }

    guessSaveTimers[targetNickname] = setTimeout(() => {
        submitGuessAjax(targetNickname, value);
    }, 5000);
}

function updatePlayerTable() {
    if (!gameData) return;

    const tbody = document.getElementById('playerTableBody');
    if (!tbody) return;

    tbody.innerHTML = '';

    const alivePlayers = gameData.players.filter(p => p.isAlive && p.identity !== 'judge');
    const allPlayers = gameData.players.filter(p => p.identity !== 'judge');

    allPlayers.forEach(player => {
        const isPlayerAlive = player.alive !== undefined ? player.alive : (player.isAlive !== undefined ? player.isAlive : true);
        const tr = document.createElement('tr');
        tr.className = isPlayerAlive ? '' : 'eliminated';

        const statement1 = player.statement1 || '-';
        const statement2 = player.statement2 || '-';
        const statement3 = player.statement3 || '-';

        const isNight = gameData.currentPhase === 'night';
        const voteEnabled = gameData.settings?.voteComplete && !gameData.settings?.votingFinished;
        const killEnabled = gameData.settings?.votingFinished && gameData.settings?.killComplete;
        const currentPlayerHasVoted = playerData && playerData.hasVoted;
        const currentPlayerHasKilled = playerData && playerData.hasKilled;
        const currentPlayerIsAlive = playerData && (playerData.alive !== false);
        const canVote = currentPlayerIsAlive && isPlayerAlive && player.playerNickname !== currentNickname && isNight && voteEnabled && !currentPlayerHasVoted;
        const canKill = currentPlayerIsAlive && isPlayerAlive && player.playerNickname !== currentNickname && isNight && killEnabled && !currentPlayerHasKilled;

        const identityMap = {
            'civilian': '平民',
            'undercover': '卧底',
            'blank': '白板',
            'angel': '天使'
        };
        const displayIdentity = !isPlayerAlive && player.identity ? identityMap[player.identity] || player.identity : '-';

        tr.innerHTML = `
            <td class="col-player">${player.playerNickname}${player.playerNickname === currentNickname ? ' (我)' : ''}</td>
            <td class="col-round">${statement1}</td>
            <td class="col-round">${statement2}</td>
            <td class="col-round">${statement3}</td>
            <td class="col-status">${isPlayerAlive ? '<span class="alive-badge">存活</span>' : '<span class="dead-badge">死亡</span>'}</td>
            <td class="col-identity">${displayIdentity}</td>
            <td class="col-vote">
                ${isPlayerAlive ? (canVote ?
                    `<button class="btn-small" onclick="votePlayer('${player.playerNickname}')">投票</button>` :
                    (player.playerNickname === currentNickname ? '<span class="me-text">-</span>' :
                    '<span class="disabled-text">禁止</span>')) :
                    '<span class="eliminated-text">-</span>'}
            </td>
            <td class="col-kill">
                ${isPlayerAlive ? (canKill ?
                    `<button class="btn-small btn-danger" onclick="killPlayer('${player.playerNickname}')">刀人</button>` :
                    (player.playerNickname === currentNickname ? '<span class="me-text">-</span>' :
                    '<span class="disabled-text">禁止</span>')) :
                    '<span class="eliminated-text">-</span>'}
            </td>`;
        tbody.appendChild(tr);
    });

    document.getElementById('currentRound').textContent = gameData.currentRound || 1;
}

function updateActionBar() {
    const actionBar = document.getElementById('actionBar');
    if (!actionBar || !gameData) return;

    let newContent = '';

    if (gameData.status === 'playing') {
        const currentPhase = gameData.currentPhase || 'day';
        const phaseText = currentPhase === 'day' ? '白天' : '夜晚';
        const mySpeakingStatus = playerData && (playerData.speakingStatus == 1 || playerData.speakingStatus === '1');

        if (mySpeakingStatus) {
            newContent = `
                <textarea id="statementInput" class="statement-input" placeholder="请输入你的发言描述..." maxlength="100"></textarea>
                <button class="btn btn-primary" onclick="submitStatement()">提交发言</button>
            `;
        } else {
            let waitingMessage = '等待裁判开启发言...';
            if (currentPhase === 'night') {
                waitingMessage = '请投票/刀人';
            }
            newContent = `
                <div class="waiting-speaking">
                    <span class="phase-badge">${phaseText}</span>
                    <p>${waitingMessage}</p>
                </div>
            `;
        }
    } else if (gameData.status === 'white_board_guess') {
        const isWhiteBoard = myIdentity === 'blank';
        if (isWhiteBoard) {
            window.location.href = 'guess-word.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
            return;
        } else {
            newContent = '<p style="text-align:center;color:rgba(255,255,255,0.7);">等待白板玩家猜词...</p>';
        }
    } else {
        newContent = '<p style="text-align:center;color:rgba(255,255,255,0.7);">等待游戏开始...</p>';
    }

    if (lastActionBarContent !== newContent) {
        lastActionBarContent = newContent;
        actionBar.innerHTML = newContent;
    }
}

function submitStatement() {
    const input = document.getElementById('statementInput');
    if (!input || !input.value.trim()) {
        showToast('请输入发言内容', 'error');
        return;
    }

    fetch('/api/game/' + currentRoomCode + '/statement', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            playerNickname: currentNickname,
            statement: input.value.trim(),
            round: currentRound
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('发言提交成功', 'success');
            input.value = '';
            loadData();
        } else {
            showToast(data.message || '提交失败', 'error');
        }
    })
    .catch(err => {
        console.error('submitStatement error:', err);
        showToast('提交失败', 'error');
    });
}

function votePlayer(nickname) {
    if (!confirm('确定投票给 ' + nickname + ' 吗？')) return;

    fetch('/api/game/' + currentRoomCode + '/vote', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            voterNickname: currentNickname,
            targetNickname: nickname
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('投票成功', 'success');
            loadData();
        } else {
            showToast(data.message || '投票失败', 'error');
        }
    })
    .catch(err => {
        console.error('votePlayer error:', err);
        showToast('投票失败', 'error');
    });
}

function killPlayer(nickname) {
    if (!confirm('确定要刀 ' + nickname + ' 吗？')) return;

    fetch('/api/game/' + currentRoomCode + '/kill', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            playerNickname: currentNickname,
            targetNickname: nickname
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('刀人成功', 'success');
            loadData();
        } else {
            showToast(data.message || '刀人失败', 'error');
        }
    })
    .catch(err => {
        console.error('killPlayer error:', err);
        showToast('刀人失败', 'error');
    });
}

function submitGuessAjax(targetNickname, guessValue) {
    localGuessInputs[targetNickname] = guessValue.trim();

    const existingIndex = myGuesses.findIndex(g => g.targetNickname === targetNickname);
    if (existingIndex >= 0) {
        myGuesses[existingIndex].guessIdentity = guessValue.trim();
        myGuesses[existingIndex].status = 'editing';
    } else {
        myGuesses.push({
            guesserNickname: currentNickname,
            targetNickname: targetNickname,
            guessIdentity: guessValue.trim(),
            status: 'editing'
        });
    }
}



function returnToLobby() {
    window.location.href = 'game.html';
}

function goBack() {
    if (confirm('确定要退出游戏吗？')) {
        window.location.href = 'game.html';
    }
}

function getIdentityText(identity) {
    const map = {
        'civilian': '平民',
        'undercover': '卧底',
        'blank': '白板',
        'angel': '天使',
        'judge': '裁判'
    };
    return map[identity] || '未知';
}

function getIdentityIcon(identity) {
    const map = {
        'civilian': '👤',
        'undercover': '🎭',
        'blank': '❓',
        'angel': '👼',
        'judge': '👨‍⚖️'
    };
    return map[identity] || '❓';
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}

init();