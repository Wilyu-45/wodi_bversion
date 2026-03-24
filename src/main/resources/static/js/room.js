let currentRoomCode = '';
let currentNickname = '';
let isHost = false;
let refreshInterval = null;
let ws = null;
let wsConnected = false;

function init() {
    const params = new URLSearchParams(window.location.search);
    currentRoomCode = params.get('code');
    const action = params.get('action');

    currentNickname = localStorage.getItem('playerNickname') || '';
    isHost = localStorage.getItem('isHost') === 'true';

    document.title = `谁是卧底 - ${currentNickname}`;

    if (!currentRoomCode) {
        showToast('无效的房间', 'error');
        setTimeout(() => {
            window.location.href = 'game.html';
        }, 1500);
        return;
    }

    document.getElementById('displayRoomCode').textContent = currentRoomCode;
    document.getElementById('hostName').textContent = localStorage.getItem('hostNickname') || '加载中...';

    if (isHost) {
        document.getElementById('startBtn').disabled = false;
        document.getElementById('startBtn').onclick = goToSettings;
    }

    const waitingText = action === 'create' ? '等待其他玩家加入...' : '已加入房间';
    document.getElementById('waitingText').textContent = waitingText;

    connectWebSocket();
    loadRoomInfo();
    refreshInterval = setInterval(loadRoomInfo, 2000);
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
                updatePlayerList(data.data);

                if (data.data.status === 'playing' || data.data.status === 'civilian_win' || data.data.status === 'undercover_win') {
                    showToast('游戏已开始！', 'success');
                    setTimeout(() => {
                        window.location.href = isHost ? 'judge.html?code=' + currentRoomCode : 'play.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
                    }, 1000);
                }
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
    if (ws && wsConnected) {
        ws.send(JSON.stringify(message));
    }
}

function loadRoomInfo() {
    fetch('/api/game/room/' + currentRoomCode)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            const room = data.data;
            updatePlayerList(room);

            if (room.status === 'playing' || room.status === 'civilian_win' || room.status === 'undercover_win') {
                showToast('游戏已开始！', 'success');
                setTimeout(() => {
                    window.location.href = isHost ? 'judge.html?code=' + currentRoomCode : 'play.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
                }, 1000);
            } else if (room.status === 'setting') {
                if (isHost) {
                    window.location.href = 'judge.html';
                } else {
                    window.location.href = 'waiting.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
                }
            }
        }
    })
    .catch(error => {
        console.error('Error loading room:', error);
    });
}

function updatePlayerList(room) {
    const playerList = document.getElementById('playerList');
    playerList.innerHTML = '';

    if (room.players && room.players.length > 0) {
        room.players.forEach(player => {
            const playerItem = document.createElement('div');
            playerItem.className = 'player-item' + (player.identity === 'judge' ? ' host' : '');

            playerItem.innerHTML = `
                <span class="player-avatar">😎</span>
                <span class="player-name">${player.playerNickname}</span>
                ${player.identity === 'judge' ? '<span class="host-badge">房主</span>' : ''}
            `;
            playerList.appendChild(playerItem);
        });

        document.getElementById('waitingText').textContent = `已有 ${room.players.length} 名玩家`;

        if (isHost && room.players && room.players.length >= 2) {
            const playerCount = room.players.filter(p => p.identity !== 'judge').length;
            if (playerCount >= 2) {
                document.getElementById('startBtn').disabled = false;
            }
        }
    }
}

function goToSettings() {
    showToast('正在进入设置...', 'success');

    fetch('/api/game/' + currentRoomCode + '/start-setting', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            window.location.href = 'judge.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('操作失败', 'error');
    });
}

function showGameSetup() {
    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.id = 'setupModal';
    modal.innerHTML = `
        <div class="modal-content" style="max-width:400px;">
            <span class="close" onclick="this.parentElement.parentElement.remove()">&times;</span>
            <h2>游戏设置</h2>
            <div style="margin:15px 0;">
                <label style="display:block;margin-bottom:10px;">平民数量：</label>
                <input type="number" id="civilianCount" value="3" min="1" max="10" style="width:100%;padding:8px;">
            </div>
            <div style="margin:15px 0;">
                <label style="display:block;margin-bottom:10px;">卧底数量：</label>
                <input type="number" id="undercoverCount" value="1" min="1" max="3" style="width:100%;padding:8px;">
            </div>
            <div style="margin:15px 0;">
                <label style="display:block;margin-bottom:10px;">白板数量：</label>
                <input type="number" id="blankCount" value="0" min="0" max="2" style="width:100%;padding:8px;">
            </div>
            <div style="margin:15px 0;">
                <label style="display:block;margin-bottom:10px;">天使数量：</label>
                <input type="number" id="angelCount" value="0" min="0" max="2" style="width:100%;padding:8px;">
            </div>
            <button class="btn btn-primary" onclick="startGameSetup()" style="width:100%;margin-top:10px;">开始分配</button>
        </div>
    `;
    document.body.appendChild(modal);
}

function startGameSetup() {
    const civilianCount = parseInt(document.getElementById('civilianCount').value) || 3;
    const undercoverCount = parseInt(document.getElementById('undercoverCount').value) || 1;
    const blankCount = parseInt(document.getElementById('blankCount').value) || 0;
    const angelCount = parseInt(document.getElementById('angelCount').value) || 0;

    showToast('正在分配身份...', 'success');

    fetch('/api/game/' + currentRoomCode + '/assign-identities', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            civilianCount: civilianCount,
            undercoverCount: undercoverCount,
            blankCount: blankCount,
            angelCount: angelCount
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            document.getElementById('setupModal')?.remove();
            showToast('身份分配成功，准备分发词语', 'success');
            loadRoomInfo();
        } else {
            showToast(data.message || '身份分配失败', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('身份分配失败', 'error');
    });
}

function goBack() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }

    if (ws) {
        ws.close();
        ws = null;
    }

    if (currentNickname && currentRoomCode) {
        fetch('/api/game/' + currentRoomCode + '/leave?playerNickname=' + encodeURIComponent(currentNickname), {
            method: 'POST'
        }).catch(() => {});
    }

    localStorage.removeItem('roomCode');
    localStorage.removeItem('isHost');
    localStorage.removeItem('hostNickname');
    window.location.href = 'game.html';
}

function startGame() {
    showToast('游戏即将开始...', 'success');

    fetch('/api/game/' + currentRoomCode + '/distribute-words', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            return fetch('/api/game/' + currentRoomCode + '/start', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });
        } else {
            throw new Error(data.message || '分发词语失败');
        }
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showToast('游戏开始！', 'success');
            setTimeout(() => {
                window.location.href = 'judge.html?code=' + currentRoomCode;
            }, 1000);
        } else {
            showToast(data.message || '无法开始游戏', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('开始游戏失败', 'error');
    });
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';

    setTimeout(() => {
        toast.classList.remove('show');
    }, 2500);
}

window.addEventListener('beforeunload', function() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});

init();