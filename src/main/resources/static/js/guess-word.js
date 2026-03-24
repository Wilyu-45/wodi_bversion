let currentRoomCode = '';
let currentNickname = '';
let playerData = null;
let gameData = null;
let myIdentity = null;
let ws = null;
let wsConnected = false;
let guessSubmitted = false;
let guessVerified = false;

function init() {
    const params = new URLSearchParams(window.location.search);
    const urlRoomCode = params.get('code');
    const urlNickname = params.get('nickname');

    if (urlRoomCode && urlNickname) {
        currentRoomCode = urlRoomCode;
        currentNickname = decodeURIComponent(urlNickname);
        localStorage.setItem('roomCode', currentRoomCode);
        localStorage.setItem('playerNickname', currentNickname);
    } else {
        currentRoomCode = localStorage.getItem('roomCode') || '';
        currentNickname = localStorage.getItem('playerNickname') || '';
    }

    if (!currentRoomCode || !currentNickname) {
        showToast('无效的房间或昵称', 'error');
        setTimeout(() => {
            window.location.href = 'game.html';
        }, 1500);
        return;
    }

    loadPlayerData();
    connectWebSocket();
}

function loadPlayerData() {
    fetch('/api/game/' + currentRoomCode + '/player?nickname=' + encodeURIComponent(currentNickname))
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            playerData = data.data;
            myIdentity = playerData.identity;
            loadGameData();
        }
    })
    .catch(error => {
        console.error('Error loading player data:', error);
        loadGameData();
    });
}

function loadGameData() {
    Promise.all([
        fetch('/api/game/room/' + currentRoomCode).then(r => r.json()),
        fetch('/api/game/' + currentRoomCode + '/player?nickname=' + encodeURIComponent(currentNickname)).then(r => r.json())
    ])
    .then(([roomResult, playerResult]) => {
        if (roomResult.success) {
            gameData = roomResult.data;
        }
        if (playerResult.success) {
            playerData = playerResult.data;
            myIdentity = playerData.identity;
        }
        updateUI();
    })
    .catch(error => {
        console.error('Error loading data:', error);
    });
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

    if (roomData.status === 'white_board_guess' && !guessVerified) {
        updateUI();
    } else if (roomData.status && roomData.status.endsWith('_win')) {
        const winnerMap = {
            'civilian_win': '平民胜利',
            'undercover_win': '卧底胜利',
            'blank_win': '白板胜利'
        };
        const winner = winnerMap[roomData.status] || '游戏结束';
        guessVerified = true;
        const endNickname = myIdentity === 'judge' ? 'judge' : encodeURIComponent(currentNickname);
        window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=' + endNickname + '&winner=' + encodeURIComponent(winner);
    }
}

function updateUI() {
    if (!gameData) return;

    const blankHint = document.getElementById('blankHint');
    const guessInputSection = document.getElementById('guessInputSection');
    const waitingSection = document.getElementById('waitingSection');
    const wordsDisplaySection = document.getElementById('wordsDisplaySection');
    const judgeButtonsSection = document.getElementById('judgeButtonsSection');
    const civilianWordDisplay = document.getElementById('civilianWordDisplay');
    const undercoverWordDisplay = document.getElementById('undercoverWordDisplay');
    const pageSubtitle = document.getElementById('pageSubtitle');

    if (blankHint) {
        if (gameData.settings && gameData.settings.blankHint) {
            blankHint.textContent = gameData.settings.blankHint;
        } else {
            blankHint.textContent = '无提示';
        }
    }

    const settings = gameData.settings || {};
    const isJudge = myIdentity === 'judge';
    const isBlank = myIdentity === 'blank';
    const guessActive = settings.whiteBoardGuessActive;
    const civilianWord = settings.whiteBoardGuessCivilianWord;
    const undercoverWord = settings.whiteBoardGuessUndercoverWord;
    const guessCorrect = settings.whiteBoardGuessCorrect;

    guessInputSection.classList.add('hidden');
    waitingSection.classList.add('hidden');
    wordsDisplaySection.classList.add('hidden');
    judgeButtonsSection.classList.add('hidden');
    waitingSection.innerHTML = '';

    if (guessCorrect !== null && guessCorrect !== undefined) {
        guessVerified = true;
        const endNickname = isJudge ? 'judge' : encodeURIComponent(currentNickname);
        if (guessCorrect) {
            window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=' + endNickname + '&winner=' + encodeURIComponent('白板胜利');
        } else {
            window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=' + endNickname + '&winner=' + encodeURIComponent('平民胜利');
        }
        return;
    }

    if (isBlank) {
        if (guessActive && civilianWord && undercoverWord) {
            wordsDisplaySection.classList.remove('hidden');
            waitingSection.classList.remove('hidden');
            waitingSection.innerHTML = '<p style="text-align:center;color:#4CAF50;padding:20px;">词语已提交，等待裁判审核...</p>';
            civilianWordDisplay.textContent = civilianWord;
            undercoverWordDisplay.textContent = undercoverWord;
        } else {
            guessInputSection.classList.remove('hidden');
            pageSubtitle.textContent = '请根据提示猜测好人和卧底的词语';
        }
    } else if (isJudge) {
        if (guessActive && civilianWord && undercoverWord) {
            wordsDisplaySection.classList.remove('hidden');
            civilianWordDisplay.textContent = civilianWord;
            undercoverWordDisplay.textContent = undercoverWord;
            judgeButtonsSection.classList.remove('hidden');
            pageSubtitle.textContent = '请审核白板玩家的猜测';
        } else {
            waitingSection.classList.remove('hidden');
            waitingSection.innerHTML = '<p style="text-align:center;color:rgba(255,255,255,0.7);padding:20px;">等待白板玩家提交猜测...</p>';
            pageSubtitle.textContent = '等待白板玩家提交猜测';
        }
    } else {
        if (guessActive && civilianWord && undercoverWord) {
            wordsDisplaySection.classList.remove('hidden');
            civilianWordDisplay.textContent = civilianWord;
            undercoverWordDisplay.textContent = undercoverWord;
            waitingSection.classList.remove('hidden');
            waitingSection.innerHTML = '<p style="text-align:center;color:#4CAF50;padding:20px;">词语已提交，等待裁判审核...</p>';
            pageSubtitle.textContent = '等待裁判审核';
        } else {
            waitingSection.classList.remove('hidden');
            waitingSection.innerHTML = '<p style="text-align:center;color:rgba(255,255,255,0.7);padding:20px;">等待白板玩家提交猜测...</p>';
            pageSubtitle.textContent = '等待白板玩家提交猜测';
        }
    }
}

function submitGuess() {
    if (myIdentity !== 'blank') {
        showToast('只有白板玩家才能提交猜测', 'error');
        return;
    }

    const civilianWord = document.getElementById('civilianWordInput').value.trim();
    const undercoverWord = document.getElementById('undercoverWordInput').value.trim();

    if (!civilianWord || !undercoverWord) {
        showToast('请填写两个词语', 'error');
        return;
    }

    const submitBtn = document.getElementById('submitBtn');
    submitBtn.disabled = true;
    submitBtn.textContent = '提交中...';

    fetch('/api/game/' + currentRoomCode + '/white-board-guess', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            civilianWord: civilianWord,
            undercoverWord: undercoverWord,
            playerNickname: currentNickname
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            guessSubmitted = true;
            showToast('提交成功，等待裁判审核', 'success');
            loadGameData();
        } else {
            showToast(data.message || '提交失败', 'error');
            submitBtn.disabled = false;
            submitBtn.textContent = '提交猜测';
        }
    })
    .catch(err => {
        console.error('submitGuess error:', err);
        showToast('提交失败', 'error');
        submitBtn.disabled = false;
        submitBtn.textContent = '提交猜测';
    });
}

function judgeVerify(correct) {
    if (myIdentity !== 'judge') {
        showToast('只有裁判才能审核', 'error');
        return;
    }

    const btnContainer = document.getElementById('judgeButtonsSection');
    if (btnContainer) {
        btnContainer.innerHTML = '<p style="text-align:center;color:#4CAF50;padding:20px;">处理中...</p>';
    }

    fetch('/api/game/' + currentRoomCode + '/white-board-verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correct: correct })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('已提交，等待页面跳转...', 'success');
        } else {
            showToast(data.message || '操作失败', 'error');
            if (btnContainer) {
                btnContainer.innerHTML = `
                    <p style="color: rgba(255, 255, 255, 0.7); margin-bottom: 20px;">请判断白板玩家的猜测是否正确：</p>
                    <p style="color: rgba(255, 255, 255, 0.5); font-size: 12px; margin-bottom: 15px;">（正确 = 猜测与原本词语相符，白板胜利；错误 = 猜测错误，平民胜利）</p>
                    <div class="btn-group">
                        <button class="btn btn-success" onclick="judgeVerify(true)">✓ 正确</button>
                        <button class="btn btn-error" onclick="judgeVerify(false)">✗ 错误</button>
                    </div>
                `;
            }
        }
    })
    .catch(err => {
        console.error('judgeVerify error:', err);
        showToast('操作失败', 'error');
    });
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast show ' + type;
    setTimeout(() => {
        toast.className = 'toast';
    }, 3000);
}

init();