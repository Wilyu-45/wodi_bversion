let currentRoomCode = '';
let currentNickname = '';
let isJudge = false;

function init() {
    const params = new URLSearchParams(window.location.search);
    currentRoomCode = params.get('code');
    currentNickname = params.get('nickname');

    console.log('game-end init:', { roomCode: currentRoomCode, nickname: currentNickname, url: window.location.href });

    if (currentNickname === 'judge') {
        isJudge = true;
    } else if (!currentNickname) {
        currentNickname = localStorage.getItem('playerNickname') || '';
        console.log('nickname from localStorage:', currentNickname);
        if (currentNickname === 'judge') {
            isJudge = true;
        }
    }

    const winner = params.get('winner');

    document.getElementById('winnerName').textContent = winner || '游戏结束';

    const winnerEl = document.getElementById('winnerName');
    if (winner && winner.includes('平民')) {
        winnerEl.classList.add('civilian');
    } else if (winner && winner.includes('卧底')) {
        winnerEl.classList.add('undercover');
    } else if (winner && winner.includes('白板')) {
        winnerEl.classList.add('blank');
    }

    if (isJudge) {
        document.getElementById('confirmBtn').textContent = '下一局';
        document.getElementById('playerInfo').textContent = '裁判模式';
    } else {
        document.getElementById('playerInfo').textContent = currentNickname || '玩家';
    }

    document.getElementById('confirmBtn').onclick = function() {
        if (isJudge) {
            startNextRound();
        } else {
            window.location.href = 'waiting.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
        }
    };

    loadPlayers();
}

function loadPlayers() {
    fetch('/api/game/room/' + currentRoomCode)
        .then(res => res.json())
        .then(data => {
            if (data.success && data.data) {
                renderPlayers(data.data.players || []);
            }
        })
        .catch(err => {
            document.getElementById('playerList').textContent = '加载失败';
        });
}

function renderPlayers(players) {
    const playerList = document.getElementById('playerList');
    const identityMap = {
        'civilian': '平民',
        'undercover': '卧底',
        'blank': '白板',
        'angel': '天使'
    };

    playerList.innerHTML = players
        .filter(p => p.identity !== 'judge')
        .map(player => {
            const identity = identityMap[player.identity] || player.identity || '-';
            const isAlive = player.alive !== false;
            const statusClass = isAlive ? 'alive' : 'dead';
            const deathCause = player.deathCause ? `(${player.deathCause})` : '';
            const isMe = player.playerNickname === currentNickname ? ' (我)' : '';

            return `
                <div class="player-item ${statusClass}">
                    <span class="status-dot"></span>
                    <span>${player.playerNickname}${isMe}</span>
                    <span class="identity-tag">[${identity}]</span>
                    <span class="death-cause">${deathCause}</span>
                </div>
            `;
        }).join('');
}

function startNextRound() {
    const btn = document.getElementById('confirmBtn');
    btn.disabled = true;
    btn.textContent = '游戏中...';

    fetch('/api/game/' + currentRoomCode + '/reset', { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            console.log('reset response:', data);
            if (data.success) {
                window.location.href = 'judge.html?code=' + currentRoomCode + '&nickname=judge';
            } else {
                alert('开始下一局失败: ' + (data.message || '未知错误'));
                btn.disabled = false;
                btn.textContent = '下一局';
            }
        })
        .catch(err => {
            console.error('resetGame error:', err);
            alert('开始下一局失败，请重试');
            btn.disabled = false;
            btn.textContent = '下一局';
        });
}

init();