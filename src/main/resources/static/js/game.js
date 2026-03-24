const avatars = ['😎', '🤔', '😄', '🎭', '🕵️', '👨‍💻', '👩‍💼', '🦸', '🧙', '🐱'];

let currentPlayerId = null;
let currentNickname = null;
let currentBindCode = null;

function init() {
    const savedPlayerId = localStorage.getItem('playerId');
    const savedNickname = localStorage.getItem('playerNickname');
    const savedBindCode = localStorage.getItem('playerBindCode');

    if (savedPlayerId && savedNickname) {
        currentPlayerId = savedPlayerId;
        currentNickname = savedNickname;
        currentBindCode = savedBindCode;
        showLoggedInState(savedNickname, savedBindCode);
    } else {
        showLoginState();
    }

    const randomAvatar = avatars[Math.floor(Math.random() * avatars.length)];
    document.getElementById('avatarIcon').textContent = randomAvatar;
}

function showLoginState() {
    document.getElementById('playerSection').style.display = 'flex';
    document.getElementById('bindCodeSection').style.display = 'none';
    document.getElementById('savedNameDisplay').textContent = '';
}

function showLoggedInState(nickname, bindCode) {
    document.getElementById('playerSection').style.display = 'none';
    document.getElementById('bindCodeSection').style.display = 'block';
    document.getElementById('displayBindCode').textContent = bindCode || '--------';
    document.getElementById('savedNameDisplay').textContent = '当前玩家: ' + nickname;
}

function showChangeName() {
    const modal = document.createElement('div');
    modal.className = 'modal show';
    modal.innerHTML = `
        <div class="modal-content">
            <span class="close" onclick="this.parentElement.parentElement.remove()">&times;</span>
            <h2>修改昵称</h2>
            <p style="color:#666;font-size:12px;margin-bottom:15px;">注意：修改昵称会生成新的绑定码</p>
            <div class="join-form">
                <input type="text" id="newNickname" placeholder="请输入新昵称" maxlength="12">
                <button class="btn btn-primary" onclick="registerPlayer()">确认修改</button>
            </div>
        </div>
    `;
    document.body.appendChild(modal);
}

function goBack() {
    window.location.href = 'index.html';
}

function registerPlayer() {
    let nickname = document.getElementById('playerName')?.value?.trim();
    const newNicknameInput = document.getElementById('newNickname');
    if (newNicknameInput) {
        nickname = newNicknameInput.value.trim();
    }

    if (!nickname) {
        showToast('请输入昵称', 'error');
        return;
    }

    if (nickname.length < 2) {
        showToast('昵称至少2个字符', 'error');
        return;
    }

    showToast('正在注册...', 'success');

    fetch('/api/game/register', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            nickname: nickname
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentPlayerId = data.data.playerId;
            currentNickname = data.data.nickname;
            currentBindCode = data.data.bindCode;

            localStorage.setItem('playerId', currentPlayerId);
            localStorage.setItem('playerNickname', currentNickname);
            localStorage.setItem('playerBindCode', currentBindCode);

            document.querySelector('.modal.show')?.remove();
            showLoggedInState(currentNickname, currentBindCode);
            showToast('注册成功', 'success');
        } else {
            showToast(data.message || '注册失败', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('注册失败', 'error');
    });
}

function bindPlayer() {
    const bindCode = document.getElementById('bindCodeInput').value.trim();

    if (!bindCode) {
        showToast('请输入绑定码', 'error');
        return;
    }

    if (bindCode.length !== 4) {
        showToast('绑定码必须是4位', 'error');
        return;
    }

    showToast('正在验证绑定码...', 'success');

    fetch('/api/game/bind', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            nickname: bindCode
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            currentPlayerId = data.data.playerId;
            currentNickname = data.data.nickname;
            currentBindCode = data.data.bindCode;

            localStorage.setItem('playerId', currentPlayerId);
            localStorage.setItem('playerNickname', currentNickname);
            localStorage.setItem('playerBindCode', currentBindCode);

            closeBindModal();
            showLoggedInState(currentNickname, currentBindCode);
            showToast('绑定成功', 'success');
        } else {
            showToast(data.message || '绑定码无效', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('绑定失败', 'error');
    });
}

function getCurrentNickname() {
    if (!currentNickname) {
        showToast('请先注册或绑定', 'error');
        return null;
    }
    return currentNickname;
}

function getCurrentPlayerId() {
    return currentPlayerId;
}

function createRoom() {
    const nickname = getCurrentNickname();
    if (!nickname) return;

    showToast('正在创建房间...', 'success');

    fetch('/api/game/create', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            roomName: nickname + '的房间',
            hostNickname: nickname
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            localStorage.setItem('roomCode', data.data.roomCode);
            localStorage.setItem('isHost', 'true');
            localStorage.setItem('hostNickname', data.data.hostNickname);
            window.location.href = 'room.html?code=' + data.data.roomCode;
        } else {
            showToast(data.message || '创建房间失败', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('创建房间失败', 'error');
    });
}

function showJoinModal() {
    if (!getCurrentNickname()) return;

    const modal = document.getElementById('joinModal');
    modal.classList.add('show');
    document.getElementById('roomCode').value = '';
    document.getElementById('roomCode').focus();
}

function closeJoinModal() {
    const modal = document.getElementById('joinModal');
    modal.classList.remove('show');
}

function showBindModal() {
    const modal = document.getElementById('bindModal');
    modal.classList.add('show');
    document.getElementById('bindCodeInput').value = '';
    document.getElementById('bindCodeInput').focus();
}

function closeBindModal() {
    const modal = document.getElementById('bindModal');
    modal.classList.remove('show');
}

function joinRoom() {
    const roomCode = document.getElementById('roomCode').value.trim().toUpperCase();
    const nickname = getCurrentNickname();

    if (!roomCode) {
        showToast('请输入房间号', 'error');
        return;
    }

    if (roomCode.length !== 6) {
        showToast('房间号必须是6位', 'error');
        return;
    }

    showToast('正在加入房间...', 'success');

    fetch('/api/game/join', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            roomCode: roomCode,
            playerNickname: nickname
        })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            localStorage.setItem('roomCode', roomCode);
            localStorage.setItem('isHost', data.data.isHost ? 'true' : 'false');
            localStorage.setItem('hostNickname', data.data.hostNickname);
            window.location.href = 'room.html?code=' + roomCode;
        } else {
            showToast(data.message || '加入房间失败', 'error');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        showToast('加入房间失败，请检查房间号是否正确', 'error');
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

document.getElementById('joinModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeJoinModal();
    }
});

document.getElementById('bindModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeBindModal();
    }
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeJoinModal();
        closeBindModal();
    }
});

document.getElementById('roomCode').addEventListener('input', function(e) {
    this.value = this.value.toUpperCase().replace(/[^A-Z0-9]/g, '');
});

document.getElementById('bindCodeInput').addEventListener('input', function(e) {
    this.value = this.value.toUpperCase().replace(/[^A-Za-z0-9!@#$%]/g, '');
});

document.getElementById('playerName').addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        registerPlayer();
    }
});

init();