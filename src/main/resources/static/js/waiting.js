let currentRoomCode = '';
let currentNickname = '';
let isHost = false;
let refreshInterval = null;
let gameData = null;

function init() {
    const params = new URLSearchParams(window.location.search);
    const urlRoomCode = params.get('code');
    const urlNickname = params.get('nickname');

    if (urlRoomCode) {
        currentRoomCode = urlRoomCode;
    } else {
        currentRoomCode = localStorage.getItem('roomCode') || '';
    }

    if (urlNickname) {
        currentNickname = decodeURIComponent(urlNickname);
    } else {
        currentNickname = localStorage.getItem('playerNickname') || '';
    }

    isHost = localStorage.getItem('isHost') === 'true';

    document.title = `谁是卧底 - 等待开始`;

    if (!currentRoomCode) {
        showToast('无效的房间', 'error');
        setTimeout(() => {
            window.location.href = 'game.html';
        }, 1500);
        return;
    }

    loadGameData();
    refreshInterval = setInterval(loadGameData, 2000);
}

function loadGameData() {
    fetch('/api/game/room/' + currentRoomCode)
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            gameData = data.data;
            updateProgress();
            checkGameStatus();
        }
    })
    .catch(error => {
        console.error('Error loading game:', error);
    });
}

function updateProgress() {
    if (!gameData) return;

    const settings = gameData.settings || {};
    const wordsSet = settings.civilianWord && settings.undercoverWord;
    const roleCountsSet = settings.civilianCount != null && settings.undercoverCount != null;
    const allIdentitiesSet = gameData.players && gameData.players.every(p => p.identity);
    const wordsDistributed = gameData.players && gameData.players.every(p => p.word);

    updateStep('step1', wordsSet);
    updateStep('step2', wordsSet && roleCountsSet);
    updateStep('step3', wordsSet && roleCountsSet && allIdentitiesSet);
    updateStep('step4', wordsSet && roleCountsSet && allIdentitiesSet && wordsDistributed);
    updateStep('step5', gameData.status === 'playing');
}

function updateStep(stepId, completed) {
    const step = document.getElementById(stepId);
    if (!step) return;

    if (completed) {
        step.className = 'progress-step completed';
        step.querySelector('.step-icon').textContent = '✓';
    } else {
        step.className = 'progress-step active';
        step.querySelector('.step-icon').textContent = stepId.replace('step', '');
    }
}

function checkGameStatus() {
    if (!gameData) return;

    if (gameData.status === 'playing') {
        clearInterval(refreshInterval);
        showToast('游戏开始！', 'success');
        setTimeout(() => {
            window.location.href = 'play.html?code=' + currentRoomCode + '&nickname=' + encodeURIComponent(currentNickname);
        }, 1000);
    }
}

function leaveRoom() {
    if (!confirm('确定要退出房间吗？')) return;

    fetch('/api/game/' + currentRoomCode + '/leave', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            playerNickname: currentNickname
        })
    })
    .then(response => response.json())
    .then(data => {
        localStorage.removeItem('roomCode');
        localStorage.removeItem('isHost');
        window.location.href = 'game.html';
    })
    .catch(error => {
        console.error('Error leaving room:', error);
        localStorage.removeItem('roomCode');
        localStorage.removeItem('isHost');
        window.location.href = 'game.html';
    });
}

function showToast(message, type) {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type + ' show';

    setTimeout(() => {
        toast.className = 'toast';
    }, 2500);
}

window.addEventListener('beforeunload', function() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
});

init();