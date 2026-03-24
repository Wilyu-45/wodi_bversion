package com.wodi.undercover.controller;

import com.wodi.undercover.dto.*;
import com.wodi.undercover.entity.GamePlayer;
import com.wodi.undercover.service.GameLogicService;
import com.wodi.undercover.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameLogicController {

    @Autowired
    private GameLogicService gameLogicService;

    @Autowired
    private GameRoomService gameRoomService;

    @PostMapping("/{roomCode}/submit-description")
    public ApiResponse<Void> submitDescription(
            @PathVariable String roomCode,
            @RequestBody SubmitDescriptionRequest request) {
        try {
            GamePlayer player = gameRoomService.getRoomPlayers(roomCode).stream()
                    .filter(p -> p.getPlayerNickname().equals(request.getPlayerNickname()))
                    .findFirst()
                    .orElse(null);

            if (player == null) {
                return ApiResponse.error("玩家不存在");
            }

            boolean success = gameLogicService.submitDescription(
                    roomCode,
                    player.getId(),
                    request.getDescription(),
                    request.getRound()
            );
            if (success) {
                return ApiResponse.success("发言提交成功", null);
            } else {
                return ApiResponse.error("发言提交失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("发言提交失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/vote")
    public ApiResponse<Void> vote(
            @PathVariable String roomCode,
            @RequestBody VoteRequest request) {
        try {
            GamePlayer voter = gameRoomService.getRoomPlayers(roomCode).stream()
                    .filter(p -> p.getPlayerNickname().equals(request.getVoterNickname()))
                    .findFirst()
                    .orElse(null);

            GamePlayer target = gameRoomService.getRoomPlayers(roomCode).stream()
                    .filter(p -> p.getPlayerNickname().equals(request.getTargetNickname()))
                    .findFirst()
                    .orElse(null);

            if (voter == null || target == null) {
                return ApiResponse.error("玩家不存在");
            }

            boolean success = gameLogicService.vote(roomCode, voter.getId(), target.getId());
            if (success) {
                return ApiResponse.success("投票成功", null);
            } else {
                return ApiResponse.error("投票失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("投票失败: " + e.getMessage());
        }
    }

    @PostMapping("/{roomCode}/eliminate")
    public ApiResponse<Void> eliminate(
            @PathVariable String roomCode,
            @RequestParam String playerNickname) {
        try {
            GamePlayer player = gameRoomService.getRoomPlayers(roomCode).stream()
                    .filter(p -> p.getPlayerNickname().equals(playerNickname))
                    .findFirst()
                    .orElse(null);

            if (player == null) {
                return ApiResponse.error("玩家不存在");
            }

            boolean success = gameLogicService.eliminatePlayer(roomCode, player.getId());
            if (success) {
                return ApiResponse.success("淘汰玩家成功", null);
            } else {
                return ApiResponse.error("淘汰玩家失败");
            }
        } catch (Exception e) {
            return ApiResponse.error("淘汰玩家失败: " + e.getMessage());
        }
    }

    @GetMapping("/{roomCode}/alive-players")
    public ApiResponse<List<GamePlayer>> getAlivePlayers(@PathVariable String roomCode) {
        try {
            List<GamePlayer> players = gameLogicService.getAlivePlayers(roomCode);
            return ApiResponse.success(players);
        } catch (Exception e) {
            return ApiResponse.error("获取存活玩家列表失败: " + e.getMessage());
        }
    }
}