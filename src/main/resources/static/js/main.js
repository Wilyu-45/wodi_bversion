function startGame() {
    window.location.href = 'game.html';
}

function showRules() {
    const modal = document.getElementById('rulesModal');
    modal.classList.add('show');
}

function closeRules() {
    const modal = document.getElementById('rulesModal');
    modal.classList.remove('show');
}

document.getElementById('rulesModal').addEventListener('click', function(e) {
    if (e.target === this) {
        closeRules();
    }
});

document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeRules();
    }
});
