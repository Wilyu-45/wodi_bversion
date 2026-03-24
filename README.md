# 谁是卧底 - 网页游戏

谁是卧底是一款经典的派对词语猜测游戏，支持多人在线对战。本游戏基于哔哩哔哩动画区的规则开发。

## 技术栈

- **后端**：Spring Boot 2.7.18 + JPA + WebSocket
- **前端**：HTML + CSS + JavaScript
- **数据库**：MySQL 8.0
- **构建工具**：Maven

## 项目结构

```
src/main/java/com/wodi/undercover/
├── controller/          # 控制器层
│   ├── GameController.java
│   ├── GameLogicController.java
│   └── GuessController.java
├── entity/             # 实体类
│   ├── GameRoom.java
│   ├── GamePlayer.java
│   ├── GameSettings.java
│   ├── Guess.java
│   └── VoteKillRecord.java
├── repository/          # 数据访问层
├── service/            # 业务逻辑层
│   ├── GameLogicService.java
│   └── GameRoomService.java
└── websocket/          # WebSocket处理
    └── GameWebSocketHandler.java
```

## 页面说明

| 页面 | 说明 |
|------|------|
| index.html | 首页，创建或加入房间 |
| room.html | 房间页面，显示玩家列表 |
| waiting.html | 等待页面 |
| game.html | 游戏设置页面（裁判） |
| judge.html | 裁判控制台 |
| play.html | 玩家游戏页面 |
| guess-word.html | 白板玩家猜词页面 |
| game-end.html | 游戏结束页面 |

## 游戏角色

- **平民**：不知道自己的身份，需通过描述词语找到同伴并找出卧底
- **卧底**：不知道自己的身份，需通过描述词语找到同伴并找出好人
- **白板**：只有提示词语，可通过其他玩家发言推断出双方词语
- **天使**：知道双方词语，但不知道哪个是卧底词，哪个是平民词，需带领平民玩家取得胜利
- **裁判**：游戏主持人，控制游戏流程

## 游戏流程

1. **创建房间**：输入昵称创建房间，获得房间号
2. **等待加入**：其他玩家使用房间号加入
3. **设置游戏**：裁判设置好人词、卧底词、提示词及人数
4. **分发词语**：裁判授予身份并分发词语
5. **开始游戏**：进入白天/夜晚循环
6. **投票阶段**：存活玩家投票
7. **刀人阶段**：存活玩家可刀人
8. **结束判断**：1.白板胜利条件：所有卧底出局，白板存活时进入猜词阶段，白板猜对词语则取得胜利，猜错则平民取得胜利。
   2.平民胜利条件：所有卧底与白板出局，平民胜利。
   3.卧底胜利条件：玩家数大于等于6人时，存活数只剩3人时，且卧底存活，则卧底胜利；玩家数小于6人时，存活数只剩2人时，且卧底存活，则卧底胜利。



## API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/game/create | POST | 创建房间 |
| /api/game/join | POST | 加入房间 |
| /api/game/room/{roomCode} | GET | 获取房间信息 |
| /api/game/{roomCode}/vote | POST | 投票 |
| /api/game/{roomCode}/kill | POST | 刀人 |
| /api/game/{roomCode}/enable-voting | POST | 开启投票 |
| /api/game/{roomCode}/finish-voting | POST | 结束投票 |
| /api/game/{roomCode}/enable-killing | POST | 开启刀人 |
| /api/game/{roomCode}/finish-killing | POST | 结束刀人 |
| /api/game/{roomCode}/next-round | POST | 下一轮 |
| /api/game/{roomCode}/end | POST | 结束游戏 |

## 数据库表

- `game_room` - 房间信息
- `game_player` - 玩家信息
- `game_settings` - 游戏设置
- `guess` - 身份猜测记录
- `vote_kill_record` - 投票刀人记录

## 运行方法

1. 配置 MySQL 数据库连接
2. 执行数据库初始化脚本
3. 运行 `mvn spring-boot:run`
4. 访问 `http://localhost:8080`//根据实际情况修改

## 数据库配置

在 `application.properties` 中配置：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/undercover
spring.datasource.username=root
spring.datasource.password=your_password
```

## 功能特性

- 实时多人游戏同步
- 完整的投票和刀人机制
- 身份猜测系统
- 白板玩家特殊猜词功能
- 游戏轮次和阶段管理
- 投票刀人记录追踪
