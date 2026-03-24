package com.wodi.undercover.controller;

import com.wodi.undercover.dto.*;
import com.wodi.undercover.entity.GamePlayer;
import com.wodi.undercover.entity.GameRoom;
import com.wodi.undercover.entity.GameSettings;
import com.wodi.undercover.entity.Guess;
import com.wodi.undercover.entity.Player;
import com.wodi.undercover.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Autowired
    private GameRoomService gameRoomService;

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> registerPlayer(@RequestBody RegisterPlayerRequest request) {
        try {
            Player player = gameRoomService.registerPlayer(request.getNickname());

            Map<String, Object> response = new HashMap<>();
            response.put("playerId", player.getId());
            response.put("nickname", player.getNickname());
            response.put("bindCode", player.getBindCode());

            return ApiResponse.success("注册成功", response);
        } catch (Exception e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }

    @PostMapping("/bind")
    public ApiResponse<Map<String, Object>> bindPlayer(@RequestBody RegisterPlayerRequest request) {
        try {
            Player player = gameRoomService.getPlayerByBindCode(request.getNickname());
            if (player == null) {
                return ApiResponse.error("绑定码无效");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("playerId", player.getId());
            response.put("nickname", player.getNickname());
            response.put("bindCode", player.getBindCode());

            return ApiResponse.success("绑定成功", response);
        } catch (Exception e) {
            return ApiResponse.error("绑定失败: " + e.getMessage());
        }
    }

    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createRoom(@RequestBody CreateRoomRequest request) {
        try {
            GameRoom room = gameRoomService.createRoom(request.getRoomName(), request.getHostNickname());

            Map<String, Object> response = new HashMap<>();
            response.put("roomCode", room.getRoomCode());
            response.put("hostNickname", room.getHostNickname());
            response.put("isHost", true);

            return ApiResponse.success("房间创建成功", response);
        } catch (Exception e) {
            return ApiResponse.error("创建房间失败: " + e.getMessage());
        }
    }

    @PostMapping("/join")
    public ApiResponse<Map<String, Object>> joinRoom(@RequestBody JoinRoomRequest request) {
        try {
            GameRoom room = gameRoomService.joinRoom(request.getRoomCode(), request.getPlayerNickname());

            Map<String, Object> response = new HashMap<>();
            response.put("roomCode", room.getRoomCode());
            response.put("hostNickname", room.getHostNickname());
            response.put("isHost", room.getHostNickname().equals(request.getPlayerNickname()));

            return ApiResponse.success("加入房间成功", response);
        } catch (Exception e) {
            return ApiResponse.error("加入房间失败: " + e.getMessage());
        }
    }

    @GetMapping("/room/{roomCode}")
    public ApiResponse<Map<String, Object>> getRoom(@PathVariable String roomCode) {
        try {
            GameRoom room = gameRoomService.getRoom(roomCode);
            if (room == null) {
                return ApiResponse.error("房间不存在");
            }

            List<GamePlayer> players = gameRoomService.getRoomPlayers(roomCode);
            GameSettings settings = gameRoomService.getSettings(roomCode);

            Map<String, Object> response = new HashMap<>();
            response.put("roomCode", room.getRoomCode());
            response.put("roomName", room.getRoomName());
            response.put("hostNickname", room.getHostNickname());
            response.put("status", room.getStatus());
            response.put("playerCount", room.getPlayerCount());
            response.put("currentRound", room.getCurrentRound());
            response.put("players", players);
            response.put("settings", settings);
            if (settings != null) {
                response.put("currentPhase", settings.getGamePhase());
            } else {
                response.put("currentPhase", "day");
            }

            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取房间信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/{roomCode}/player")
    public ApiResponse<Map<String, Object>> getPlayerInfo(@PathVariable String roomCode,
            @RequestParam String nickname) {
        try {
            GamePlayer player = gameRoomService.getPlayerByRoomAndNickname(roomCode, nickname);
            if (player == null) {
                return ApiResponse.error("玩家不存在");
            }

            GameSettings settings = gameRoomService.getSettings(roomCode);

            Map<String, Object> response = new HashMap<>();
            response.put("playerNickname", player.getPlayerNickname());
            response.put("identity", player.getIdentity());
            response.put("word", player.getWord());
            response.put("secondWord", player.getSecondWord());
            response.put("isAlive", player.isAlive());
            response.put("speakingStatus", player.getSpeakingStatus());
            response.put("hasVoted", player.isHasVoted());
            response.put("hasKilled", player.isHasKilled());
            response.put("settings", settings);

            return ApiResponse.success(response);
        } catch (Exception e) {
            return ApiResponse.error("获取玩家信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/settings")
    public ApiResponse<Void> createSettings(
            @PathVariable String roomCode,
            @RequestBody SetGameWordsRequest request) {
        try {
            gameRoomService.createSettings(roomCode, request.getCivilianWord(),
                    request.getUndercoverWord(), request.getBlankHint(),
                    request.getRestrictedWord(),
                    request.getCivilianCount(), request.getUndercoverCount(),
                    request.getBlankCount(), request.getAngelCount());
            return ApiResponse.success("设置成功", null);
        } catch (Exception e) {
            return ApiResponse.error("设置失败: " + e.getMessage());
        }
    }

    @GetMapping("/{roomCode}/settings")
    public ApiResponse<GameSettings> getSettings(@PathVariable String roomCode) {
        try {
            GameSettings settings = gameRoomService.getSettings(roomCode);
            if (settings == null) {
                return ApiResponse.error("游戏设置不存在");
            }
            return ApiResponse.success(settings);
        } catch (Exception e) {
            return ApiResponse.error("获取游戏设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/assign-identities")
    public ApiResponse<Void> assignIdentities(
            @PathVariable String roomCode,
            @RequestBody AssignIdentitiesRequest request) {
        try {
            gameRoomService.assignIdentities(roomCode, request.getCivilianCount(),
                    request.getUndercoverCount(), request.getBlankCount(), request.getAngelCount());
            return ApiResponse.success("身份分配成功", null);
        } catch (Exception e) {
            return ApiResponse.error("身份分配失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/distribute-words")
    public ApiResponse<Void> distributeWords(@PathVariable String roomCode) {
        try {
            gameRoomService.distributeWords(roomCode);
            return ApiResponse.success("词语分发成功", null);
        } catch (Exception e) {
            return ApiResponse.error("词语分发失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/player/{nickname}/identity")
    public ApiResponse<Void> setPlayerIdentity(
            @PathVariable String roomCode,
            @PathVariable String nickname,
            @RequestBody Map<String, String> request) {
        try {
            String identity = request.get("identity");
            gameRoomService.setPlayerIdentity(roomCode, nickname, identity);
            return ApiResponse.success("身份设置成功", null);
        } catch (Exception e) {
            return ApiResponse.error("身份设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/start")
    public ApiResponse<Void> startGame(@PathVariable String roomCode) {
        try {
            gameRoomService.startGame(roomCode);
            return ApiResponse.success("游戏开始", null);
        } catch (Exception e) {
            return ApiResponse.error("游戏开始失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/start-setting")
    public ApiResponse<Void> startSetting(@PathVariable String roomCode) {
        try {
            gameRoomService.startSetting(roomCode);
            return ApiResponse.success("进入设置阶段", null);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/end")
    public ApiResponse<Void> endGame(@PathVariable String roomCode) {
        try {
            gameRoomService.endGame(roomCode);
            return ApiResponse.success("游戏结束", null);
        } catch (Exception e) {
            return ApiResponse.error("游戏结束失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/next-phase")
    public ApiResponse<Void> nextPhase(@PathVariable String roomCode) {
        try {
            gameRoomService.togglePhase(roomCode);
            return ApiResponse.success("进入下一阶段", null);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/leave")
    public ApiResponse<Void> leaveRoom(
            @PathVariable String roomCode,
            @RequestParam String playerNickname) {
        try {
            gameRoomService.leaveRoom(roomCode, playerNickname);
            return ApiResponse.success("离开房间成功", null);
        } catch (Exception e) {
            return ApiResponse.error("离开房间失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/guess")
    public ApiResponse<Void> submitGuess(
            @PathVariable String roomCode,
            @RequestBody GuessRequest request) {
        try {
            gameRoomService.submitGuess(roomCode, request.getGuesserNickname(),
                    request.getTargetNickname(), request.getGuessIdentity(), request.getStatus());
            return ApiResponse.success("猜测已提交", null);
        } catch (Exception e) {
            return ApiResponse.error("猜测失败: " + e.getMessage());
        }
    }

    @GetMapping("/{roomCode}/guesses")
    public ApiResponse<?> getGuesses(@PathVariable String roomCode) {
        try {
            return ApiResponse.success(gameRoomService.getGuessesByRoom(roomCode));
        } catch (Exception e) {
            return ApiResponse.error("获取猜测记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/{roomCode}/vote-kill-records")
    public ApiResponse<?> getVoteKillRecords(@PathVariable String roomCode) {
        try {
            return ApiResponse.success(gameRoomService.getVoteKillRecordsByRoom(roomCode));
        } catch (Exception e) {
            return ApiResponse.error("获取投票刀人记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/white-board-guess")
    public ApiResponse<?> whiteBoardGuess(@PathVariable String roomCode, @RequestBody WhiteBoardGuessRequest request) {
        try {
            gameRoomService.whiteBoardGuess(roomCode, request.getPlayerNickname(),
                    request.getCivilianWord(), request.getUndercoverWord());
            return ApiResponse.success("提交成功", null);
        } catch (Exception e) {
            return ApiResponse.error("提交失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/white-board-verify")
    public ApiResponse<?> verifyWhiteBoardGuess(@PathVariable String roomCode, @RequestBody Map<String, Object> request) {
        try {
            Boolean correct = (Boolean) request.get("correct");
            gameRoomService.verifyWhiteBoardGuess(roomCode, correct);
            return ApiResponse.success("审核完成", null);
        } catch (Exception e) {
            return ApiResponse.error("审核失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/speaking")
    public ApiResponse<Void> setSpeaking(@PathVariable String roomCode, @RequestBody Map<String, Object> request) {
        try {
            String playerNickname = (String) request.get("playerNickname");
            Integer speakingStatus = (Integer) request.get("speakingStatus");
            gameRoomService.setSpeakingStatus(roomCode, playerNickname, speakingStatus);
            return ApiResponse.success("设置成功", null);
        } catch (Exception e) {
            return ApiResponse.error("设置失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/next-round")
    public ApiResponse<Void> nextRound(@PathVariable String roomCode) {
        try {
            gameRoomService.nextRound(roomCode);
            return ApiResponse.success("已进入下一轮", null);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/phase")
    public ApiResponse<Void> togglePhase(@PathVariable String roomCode) {
        try {
            gameRoomService.togglePhase(roomCode);
            return ApiResponse.success("已进入下一阶段", null);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/statement")
    public ApiResponse<Void> submitStatement(@PathVariable String roomCode, @RequestBody Map<String, Object> request) {
        try {
            String playerNickname = (String) request.get("playerNickname");
            String statement = (String) request.get("statement");
            Integer round = (Integer) request.get("round");
            gameRoomService.submitStatement(roomCode, playerNickname, statement, round);
            return ApiResponse.success("发言提交成功", null);
        } catch (Exception e) {
            return ApiResponse.error("发言提交失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/kill")
    public ApiResponse<Void> killPlayer(@PathVariable String roomCode, @RequestBody Map<String, Object> request) {
        try {
            String playerNickname = (String) request.get("playerNickname");
            String targetNickname = (String) request.get("targetNickname");
            gameRoomService.killPlayer(roomCode, playerNickname, targetNickname);
            return ApiResponse.success("刀人成功", null);
        } catch (Exception e) {
            return ApiResponse.error("刀人失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/reset")
    public ApiResponse<Void> resetGame(@PathVariable String roomCode) {
        try {
            gameRoomService.resetGame(roomCode);
            return ApiResponse.success("游戏重置成功", null);
        } catch (Exception e) {
            return ApiResponse.error("游戏重置失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/enable-voting")
    public ApiResponse<Void> enableVoting(@PathVariable String roomCode) {
        try {
            gameRoomService.enableVoting(roomCode);
            return ApiResponse.success("投票已开启", null);
        } catch (Exception e) {
            return ApiResponse.error("开启投票失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/finish-voting")
    public ApiResponse<Void> finishVoting(@PathVariable String roomCode) {
        try {
            gameRoomService.finishVoting(roomCode);
            return ApiResponse.success("投票已结束", null);
        } catch (Exception e) {
            return ApiResponse.error("结束投票失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/enable-killing")
    public ApiResponse<Void> enableKilling(@PathVariable String roomCode) {
        try {
            gameRoomService.enableKilling(roomCode);
            return ApiResponse.success("刀人已开启", null);
        } catch (Exception e) {
            return ApiResponse.error("开启刀人失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/finish-killing")
    public ApiResponse<Void> finishKilling(@PathVariable String roomCode) {
        try {
            gameRoomService.finishKilling(roomCode);
            return ApiResponse.success("刀人已结束", null);
        } catch (Exception e) {
            return ApiResponse.error("结束刀人失败: " + e.getMessage());
        }
    }
}