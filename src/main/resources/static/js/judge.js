let currentRoomCode = '';
let currentNickname = '';
let isHost = false;
let refreshInterval = null;
let gameData = null;
let selectedPlayerForIdentity = null;

function init() {
    const params = new URLSearchParams(window.location.search);
    const urlRoomCode = params.get('code');
    const urlNickname = params.get('nickname');

    if (urlRoomCode && urlNickname) {
        currentRoomCode = urlRoomCode;
        currentNickname = decodeURIComponent(urlNickname);
        localStorage.setItem('roomCode', currentRoomCode);
        localStorage.setItem('playerNickname', currentNickname);
        localStorage.setItem('isHost', 'true');
        isHost = true;
    } else {
        currentRoomCode = localStorage.getItem('roomCode') || '';
        currentNickname = localStorage.getItem('playerNickname') || '';
        isHost = localStorage.getItem('isHost') === 'true';
    }

    document.title = `裁判控制台 - ${currentNickname}`;

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
            updatePage();
            loadVoteKillRecords();
        }
    })
    .catch(error => {
        console.error('Error loading game:', error);
    });
}

function loadVoteKillRecords() {
    fetch('/api/game/' + currentRoomCode + '/vote-kill-records')
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            updateVoteKillRecordsTable(data.data);
        }
    })
    .catch(error => {
        console.error('Error loading vote kill records:', error);
    });
}

function updateVoteKillRecordsTable(records) {
    const section = document.getElementById('voteKillRecordsSection');
    const tbody = document.getElementById('voteKillRecordsBody');

    if (!records || records.length === 0) {
        section.style.display = 'none';
        return;
    }

    section.style.display = 'block';
    tbody.innerHTML = records.map(record => {
        const actionText = record.actionType === 'vote' ? '投票' : '刀人';
        const actionClass = record.actionType === 'vote' ? 'action-vote' : 'action-kill';
        const performer = record.actionType === 'vote' ? record.voterNickname : record.killerNickname;
        const target = record.actionType === 'vote' ? record.votedNickname : record.killedNickname;
        const time = record.createTime ? new Date(record.createTime).toLocaleString('zh-CN') : '';

        return `
            <tr>
                <td>第${record.roundNumber}轮</td>
                <td class="${actionClass}">${actionText}</td>
                <td>${performer}</td>
                <td>${target}</td>
                <td>${time}</td>
            </tr>
        `;
    }).join('');
}

function updatePage() {
    if (!gameData) return;

    if (gameData.status === 'white_board_guess') {
        window.location.href = 'guess-word.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
        return;
    }

    if (gameData.status && gameData.status.endsWith('_win')) {
        const winnerMap = {
            'civilian_win': '平民胜利',
            'undercover_win': '卧底胜利',
            'blank_win': '白板胜利'
        };
        const winner = winnerMap[gameData.status] || '游戏结束';
        window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=judge&winner=' + encodeURIComponent(winner);
        return;
    }

    renderJudgeView();
}

function renderWhiteBoardGuessSection() {
    const section = document.getElementById('whiteBoardGuessSection');
    const hintSection = document.getElementById('voteKillRecordsSection');
    const actionBar = document.getElementById('actionBar');

    if (hintSection) hintSection.style.display = 'none';

    const settings = gameData.settings || {};

    if (!settings.whiteBoardGuessActive) {
        section.style.display = 'none';
        if (actionBar) {
            const waitingMsg = '<p style="text-align:center;color:rgba(255,255,255,0.7);">等待白板玩家提交猜测...</p>';
            if (actionBar.innerHTML !== waitingMsg) {
                actionBar.innerHTML = waitingMsg;
            }
        }
        return;
    }

    section.style.display = 'block';

    const whiteBoardPlayer = gameData.players.find(p => p.identity === 'blank' && p.isAlive);
    document.getElementById('whiteBoardPlayerName').textContent = whiteBoardPlayer ? whiteBoardPlayer.playerNickname : '未知';
    document.getElementById('whiteBoardGuessCivilian').textContent = settings.whiteBoardGuessCivilianWord || '未填写';
    document.getElementById('whiteBoardGuessUndercover').textContent = settings.whiteBoardGuessUndercoverWord || '未填写';
}

function verifyWhiteBoardGuess(correct) {
    fetch('/api/game/' + currentRoomCode + '/white-board-verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ correct: correct })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast(correct ? '判定正确，白板获胜' : '判定错误，平民获胜', 'success');
            setTimeout(() => {
                window.location.href = 'game-end.html?code=' + currentRoomCode + '&nickname=judge&winner=' + (correct ? '白板胜利' : '平民胜利');
            }, 1500);
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(err => {
        console.error('verifyWhiteBoardGuess error:', err);
        showToast('操作失败', 'error');
    });
}

function renderJudgeView() {
    const tbody = document.getElementById('playerTableBody');
    tbody.innerHTML = '';

    const actionBar = document.getElementById('actionBar');
    const status = gameData.status || 'waiting';

    const wordsSet = gameData.settings?.civilianWord && gameData.settings?.undercoverWord;
    const roleCountsSet = gameData.settings?.civilianCount != null && gameData.settings?.undercoverCount != null;
    const allIdentitiesSetVar = areAllIdentitiesSet();
    const wordsDistributedVar = areWordsDistributed();

    if (status === 'waiting' || status === 'setting') {
        if (isHost) {
            actionBar.innerHTML = `
                <div class="setup-hint" id="setupHint">
                    <span class="step ${wordsSet ? 'completed' : 'disabled'}">1. 设置词语</span>
                    <span class="step-arrow">→</span>
                    <span class="step ${roleCountsSet ? 'completed' : (wordsSet ? 'active' : 'disabled')}">2. 设置人数</span>
                    <span class="step-arrow">→</span>
                    <span class="step ${allIdentitiesSetVar ? 'completed' : (wordsSet && roleCountsSet ? 'active' : 'disabled')}">3. 授予身份</span>
                    <span class="step-arrow">→</span>
                    <span class="step ${wordsDistributedVar ? 'completed' : (allIdentitiesSetVar ? 'active' : 'disabled')}">4. 分发词语</span>
                    <span class="step-arrow">→</span>
                    <span class="step ${wordsDistributedVar ? 'active' : 'disabled'}">5. 开始游戏</span>
                </div>
                <div class="judge-controls">
                    <button class="btn ${wordsSet ? 'btn-success' : 'btn-warning'}" onclick="showWordsModal()" id="wordsBtn">设置词语</button>
                    <button class="btn ${roleCountsSet ? 'btn-success' : (!wordsSet ? 'btn-secondary' : 'btn-warning')}" onclick="showRoleCountModal()" id="roleCountBtn" ${!wordsSet ? 'disabled' : ''}>设置人数</button>
                    <button class="btn ${wordsDistributedVar ? 'btn-success' : (!roleCountsSet || !allIdentitiesSetVar ? 'btn-secondary' : 'btn-warning')}" onclick="distributeWords()" id="distributeBtn" ${!allIdentitiesSetVar ? 'disabled' : ''}>分发词语</button>
                    <button class="btn ${wordsDistributedVar ? 'btn-success' : 'btn-secondary'}" onclick="startGame()" id="startGameBtn" ${!wordsDistributedVar ? 'disabled' : ''}>开始游戏</button>
                </div>
            `;
        } else {
            actionBar.innerHTML = `
                <div class="setup-hint">
                    <span class="step active">等待裁判设置游戏...</span>
                </div>
            `;
        }
    } else if (status === 'playing') {
        const currentRound = gameData.currentRound || 1;
        document.getElementById('currentRound').textContent = currentRound;
        const currentPhase = gameData.currentPhase || 'day';
        const phaseText = currentPhase === 'day' ? '白天' : '夜晚';
        const votingActive = gameData.settings?.voteComplete || false;
        const killingActive = gameData.settings?.killComplete || false;
        const votingFinished = gameData.settings?.votingFinished || false;
        const isNight = currentPhase === 'night';

        let actionButtons = '';
        if (isNight) {
            if (!votingActive && !killingActive && !votingFinished) {
                actionButtons += `<button class="btn btn-primary" onclick="enableVoting()">开启投票</button>`;
            } else if (votingActive) {
                actionButtons += `<button class="btn btn-danger" onclick="finishVoting()">结束投票</button>`;
            } else if (votingFinished && !killingActive) {
                actionButtons += `<button class="btn btn-primary" onclick="enableKilling()">开始刀人</button>`;
            } else if (killingActive) {
                actionButtons += `<button class="btn btn-danger" onclick="finishKilling()">结束刀人</button>`;
            }
        }

        actionBar.innerHTML = `
            <div class="judge-controls">
                <span class="phase-display">第 ${currentRound} 轮 - ${phaseText}</span>
                ${actionButtons}
                <button class="btn btn-warning" onclick="togglePhase()">下一阶段</button>
                <button class="btn btn-secondary" onclick="nextRound()">下一轮</button>
                <button class="btn btn-danger" onclick="endGame()">结束游戏</button>
            </div>
        `;
    }

    const players = gameData.players || [];
    players.forEach((player) => {
        if (player.identity === 'judge') return;

        const tr = document.createElement('tr');
        tr.className = 'player-row';

        const playerCell = `
            <td>
                <div class="player-cell">
                    <span class="player-name">${player.playerNickname}</span>
                </div>
            </td>
        `;

        let roundCells = '';
        for (let r = 1; r <= 3; r++) {
            const statement = r === 1 ? player.statement1 : r === 2 ? player.statement2 : player.statement3;
            roundCells += `<td class="round-cell">${statement || '-'}</td>`;
        }

        let identityText = '待设置';
        let identityClass = '';
        let clickHandler = '';
        if (wordsSet && roleCountsSet && status !== 'playing') {
            clickHandler = `onclick="openIdentityModal('${player.playerNickname}')" style="cursor:pointer;"`;
        }
        if (player.identity === 'civilian') { identityText = '平民'; identityClass = 'civilian'; }
        else if (player.identity === 'undercover') { identityText = '卧底'; identityClass = 'undercover'; }
        else if (player.identity === 'blank') { identityText = '白板'; identityClass = 'blank'; }
        else if (player.identity === 'angel') { identityText = '天使'; identityClass = 'angel'; }

        const identityCell = `
            <td class="identity-cell">
                <span class="identity-badge ${identityClass}" ${clickHandler}>
                    ${identityText}
                </span>
            </td>
        `;

        const isPlayerAlive = player.alive !== undefined ? player.alive : (player.isAlive !== undefined ? player.isAlive : true);
        const statusCell = `
            <td class="status-cell">
                ${isPlayerAlive ?
                    '<span class="alive-badge">存活</span>' :
                    '<span class="dead-badge">死亡</span>'}
            </td>
        `;

        const deathCauseCell = `
            <td class="death-cell">
                ${player.deathCause || '-'}
            </td>
        `;

        const voteCountCell = `
            <td class="vote-count-cell">
                ${player.voteCount || 0}
            </td>
        `;

        const currentRound = gameData.currentRound || 1;
        const hasSpokenInCurrentRound =
            (currentRound === 1 && player.statement1) ||
            (currentRound === 2 && player.statement2) ||
            (currentRound === 3 && player.statement3);

        const speakingBtn = gameData.status === 'playing' ?
            (player.speakingStatus === 1 ?
                `<button class="btn-small btn-success" disabled>发言中</button>` :
            hasSpokenInCurrentRound ?
                `<button class="btn-small btn-secondary" disabled>已发言</button>` :
                `<button class="btn-small" onclick="toggleSpeaking('${player.playerNickname}', 1)">开始发言</button>`) :
            `<button class="btn-small" disabled>开始发言</button>`;

        const speakingCell = `
            <td class="speaking-cell">
                ${speakingBtn}
            </td>
        `;

        tr.innerHTML = playerCell + roundCells + identityCell + statusCell + deathCauseCell + voteCountCell + speakingCell;
        tbody.appendChild(tr);
    });
}

function areAllIdentitiesSet() {
    if (!gameData || !gameData.players) return false;
    return gameData.players
        .filter(p => p.identity !== 'judge')
        .every(p => p.identity && p.identity.trim() !== '');
}

function areWordsDistributed() {
    if (!gameData || !gameData.players) return false;
    return gameData.players
        .filter(p => p.identity !== 'judge')
        .every(p => p.word && p.word.trim() !== '');
}

function openIdentityModal(playerNickname) {
    const wordsSet = gameData.settings?.civilianWord && gameData.settings?.undercoverWord;
    const roleCountsSet = gameData.settings?.civilianCount != null && gameData.settings?.undercoverCount != null;
    if (!wordsSet || !roleCountsSet) {
        showToast('请先完成步骤1和2', 'error');
        return;
    }
    selectedPlayerForIdentity = playerNickname;
    const modal = document.getElementById('identityModal');
    document.getElementById('identityModalHint').textContent = `为 ${playerNickname} 选择身份`;
    modal.style.display = 'block';
}

function closeIdentityModal() {
    document.getElementById('identityModal').style.display = 'none';
    selectedPlayerForIdentity = null;
}

function assignIdentity(identity) {
    if (!selectedPlayerForIdentity) return;

    const settings = gameData.settings;
    if (settings) {
        const currentPlayers = gameData.players.filter(p => p.identity !== 'judge');
        const selectedPlayer = currentPlayers.find(p => p.playerNickname === selectedPlayerForIdentity);
        const isAlreadyThisIdentity = selectedPlayer && selectedPlayer.identity === identity;

        const currentCivilianCount = currentPlayers.filter(p => p.identity === 'civilian').length;
        const currentUndercoverCount = currentPlayers.filter(p => p.identity === 'undercover').length;
        const currentBlankCount = currentPlayers.filter(p => p.identity === 'blank').length;
        const currentAngelCount = currentPlayers.filter(p => p.identity === 'angel').length;

        if (identity === 'civilian' && currentCivilianCount >= (settings.civilianCount || 0) && !isAlreadyThisIdentity) {
            showToast('平民人数已达到设置上限(' + settings.civilianCount + ')', 'error');
            return;
        }
        if (identity === 'undercover' && currentUndercoverCount >= (settings.undercoverCount || 0) && !isAlreadyThisIdentity) {
            showToast('卧底人数已达到设置上限(' + settings.undercoverCount + ')', 'error');
            return;
        }
        if (identity === 'blank' && currentBlankCount >= (settings.blankCount || 0) && !isAlreadyThisIdentity) {
            showToast('白板人数已达到设置上限(' + settings.blankCount + ')', 'error');
            return;
        }
        if (identity === 'angel' && currentAngelCount >= (settings.angelCount || 0) && !isAlreadyThisIdentity) {
            showToast('天使人数已达到设置上限(' + settings.angelCount + ')', 'error');
            return;
        }
    }

    fetch('/api/game/' + currentRoomCode + '/player/' + selectedPlayerForIdentity + '/identity', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ identity: identity })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('身份设置成功', 'success');
            closeIdentityModal();
            loadGameData();
        } else {
            showToast(data.message || '设置失败', 'error');
        }
    })
    .catch(err => {
        console.error('assignIdentity error:', err);
        showToast('设置失败', 'error');
    });
}

function showRoleCountModal() {
    const wordsSet = gameData.settings?.civilianWord && gameData.settings?.undercoverWord;
    if (!wordsSet) {
        showToast('请先设置词语', 'error');
        return;
    }
    const modal = document.getElementById('roleCountModal');
    if (gameData.settings) {
        document.getElementById('setCivilianCount').value = gameData.settings.civilianCount || 3;
        document.getElementById('setUndercoverCount').value = gameData.settings.undercoverCount || 1;
        document.getElementById('setBlankCount').value = gameData.settings.blankCount || 0;
        document.getElementById('setAngelCount').value = gameData.settings.angelCount || 0;
    }
    modal.style.display = 'block';
}

function closeRoleCountModal() {
    document.getElementById('roleCountModal').style.display = 'none';
}

function saveRoleCounts() {
    const civilianCount = parseInt(document.getElementById('setCivilianCount').value) || 3;
    const undercoverCount = parseInt(document.getElementById('setUndercoverCount').value) || 1;
    const blankCount = parseInt(document.getElementById('setBlankCount').value) || 0;
    const angelCount = parseInt(document.getElementById('setAngelCount').value) || 0;

    const totalRoles = civilianCount + undercoverCount + blankCount + angelCount;
    const playerCount = gameData.players.filter(p => p.identity !== 'judge').length;

    if (totalRoles !== playerCount) {
        showToast('角色人数总和(' + totalRoles + ')必须等于玩家人数(' + playerCount + ')', 'error');
        return;
    }

    const settingsData = {
        civilianCount: civilianCount,
        undercoverCount: undercoverCount,
        blankCount: blankCount,
        angelCount: angelCount
    };

    fetch('/api/game/' + currentRoomCode + '/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(settingsData)
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('人数设置成功', 'success');
            closeRoleCountModal();
            loadGameData();
        } else {
            showToast(data.message || '设置失败', 'error');
        }
    })
    .catch(err => {
        console.error('saveRoleCounts error:', err);
        showToast('设置失败', 'error');
    });
}

function showWordsModal() {
    const modal = document.getElementById('wordsModal');
    if (gameData.settings) {
        document.getElementById('setCivilianWord').value = gameData.settings.civilianWord || '';
        document.getElementById('setUndercoverWord').value = gameData.settings.undercoverWord || '';
        document.getElementById('setBlankHint').value = gameData.settings.blankHint || '';
        document.getElementById('setRestrictedWord').value = gameData.settings.restrictedWord || '';
    }
    modal.style.display = 'block';
}

function closeWordsModal() {
    document.getElementById('wordsModal').style.display = 'none';
}

function saveWords() {
    const civilianWord = document.getElementById('setCivilianWord').value.trim();
    const undercoverWord = document.getElementById('setUndercoverWord').value.trim();
    const blankHint = document.getElementById('setBlankHint').value.trim();
    const restrictedWord = document.getElementById('setRestrictedWord').value.trim();

    if (!civilianWord || !undercoverWord) {
        showToast('请设置平民和卧底词语', 'error');
        return;
    }

    fetch('/api/game/' + currentRoomCode + '/settings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            civilianWord: civilianWord,
            undercoverWord: undercoverWord,
            blankHint: blankHint,
            restrictedWord: restrictedWord
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('词语设置成功', 'success');
            closeWordsModal();
            loadGameData();
        } else {
            showToast(data.message || '设置失败', 'error');
        }
    })
    .catch(err => {
        console.error('saveWords error:', err);
        showToast('设置失败', 'error');
    });
}

function distributeWords() {
    if (!areAllIdentitiesSet()) {
        showToast('请先为所有玩家授予身份', 'error');
        return;
    }
    showToast('正在分发词语...', 'success');

    fetch('/api/game/' + currentRoomCode + '/distribute-words', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('词语分发成功', 'success');
            loadGameData();
        } else {
            showToast(data.message || '分发失败', 'error');
        }
    })
    .catch(err => {
        console.error('distributeWords error:', err);
        showToast('分发失败', 'error');
    });
}

function startGame() {
    if (!areWordsDistributed()) {
        showToast('请先分发词语', 'error');
        return;
    }
    showToast('游戏开始...', 'success');

    fetch('/api/game/' + currentRoomCode + '/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('游戏开始！', 'success');
            loadGameData();
        } else {
            showToast(data.message || '开始失败', 'error');
        }
    })
    .catch(err => {
        console.error('startGame error:', err);
        showToast('开始失败', 'error');
    });
}

function nextRound() {
    fetch('/api/game/' + currentRoomCode + '/next-round', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('进入下一轮', 'success');
            loadGameData();
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(err => {
        console.error('nextRound error:', err);
        showToast('操作失败', 'error');
    });
}

function enableVoting() {
    fetch('/api/game/' + currentRoomCode + '/enable-voting', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('投票已开启', 'success');
            loadGameData();
        } else {
            showToast(data.message || '开启投票失败', 'error');
        }
    })
    .catch(err => {
        console.error('enableVoting error:', err);
        showToast('开启投票失败', 'error');
    });
}

function enableKilling() {
    fetch('/api/game/' + currentRoomCode + '/enable-killing', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('刀人已开启', 'success');
            loadGameData();
        } else {
            showToast(data.message || '开启刀人失败', 'error');
        }
    })
    .catch(err => {
        console.error('enableKilling error:', err);
        showToast('开启刀人失败', 'error');
    });
}

function finishVoting() {
    fetch('/api/game/' + currentRoomCode + '/finish-voting', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('投票已结束', 'success');
            loadGameData();
        } else {
            showToast(data.message || '结束投票失败', 'error');
        }
    })
    .catch(err => {
        console.error('finishVoting error:', err);
        showToast('结束投票失败', 'error');
    });
}

function finishKilling() {
    fetch('/api/game/' + currentRoomCode + '/finish-killing', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('刀人已结束', 'success');
            loadGameData();
        } else {
            showToast(data.message || '结束刀人失败', 'error');
        }
    })
    .catch(err => {
        console.error('finishKilling error:', err);
        showToast('结束刀人失败', 'error');
    });
}

function toggleSpeaking(playerNickname, status) {
    fetch('/api/game/' + currentRoomCode + '/speaking', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            playerNickname: playerNickname,
            speakingStatus: status
        })
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast(status === 1 ? playerNickname + ' 开始发言' : playerNickname + ' 结束发言', 'success');
            loadGameData();
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(err => {
        console.error('toggleSpeaking error:', err);
        showToast('操作失败', 'error');
    });
}

function togglePhase() {
    fetch('/api/game/' + currentRoomCode + '/phase', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('已进入下一阶段', 'success');
            loadGameData();
        } else {
            showToast(data.message || '操作失败', 'error');
        }
    })
    .catch(err => {
        console.error('togglePhase error:', err);
        showToast('操作失败', 'error');
    });
}

function endGame() {
    if (!confirm('确定要结束游戏吗？')) return;

    fetch('/api/game/' + currentRoomCode + '/end', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            showToast('游戏已结束', 'success');
            if (refreshInterval) {
                clearInterval(refreshInterval);
                refreshInterval = null;
            }
            localStorage.removeItem('playerNickname');
            setTimeout(() => {
                window.location.href = 'judge.html?code=' + encodeURIComponent(currentRoomCode) + '&nickname=' + encodeURIComponent(currentNickname);
            }, 1000);
        } else {
            showToast(data.message || '结束失败', 'error');
        }
    })
    .catch(err => {
        console.error('endGame error:', err);
        showToast('结束失败', 'error');
    });
}

function goBack() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
    window.location.href = 'game.html';
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