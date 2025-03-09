package controllers;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.deck.Deck;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

/**
 * 游戏控制器类，负责游戏的高层逻辑和协调。
 * 管理游戏初始化、回合流程、结束条件检查等。
 *
 * @author Your Team
 */
public class GameController {

    private static GameController instance;

    private ActorRef out; // 用于向前端发送命令
    private GameState gameState; // 游戏状态
    private CardController cardController; // 卡牌控制器
    private CombatController combatController; // 战斗控制器
    private AIController aiController; // AI控制器

    /**
     * 私有构造函数（单例模式）
     */
    private GameController() {
        this.gameState = GameState.getInstance();
        this.cardController = new CardController();
        this.combatController = new CombatController();
        this.aiController = new AIController();
    }

    /**
     * 获取GameController单例实例
     */
    public static GameController getInstance() {
        if (instance == null) {
            instance = new GameController();
        }
        return instance;
    }

    /**
     * 设置ActorRef，用于向前端发送命令
     */
    public void setOut(ActorRef out) {
        this.out = out;
        cardController.setOut(out);
        combatController.setOut(out);
        aiController.setOut(out);
    }

    /**
     * 初始化游戏
     * 创建玩家、牌库、手牌和头像等
     */
    public void initializeGame() {
        try {
            // 显示初始化消息
            BasicCommands.addPlayer1Notification(out, "游戏初始化中...", 2);

            // 创建人类玩家（玩家1）
            Player humanPlayer = new Player(1, Player.PlayerType.HUMAN, "人类玩家", 20, 2);

            // 创建AI玩家（玩家2）
            Player aiPlayer = new Player(2, Player.PlayerType.AI, "AI玩家", 20, 2);

            // 创建玩家头像
            Unit humanAvatar = createAvatar(StaticConfFiles.humanAvatar, 1, 2, 2);
            Unit aiAvatar = createAvatar(StaticConfFiles.aiAvatar, 2, 7, 2);

            // 设置玩家头像
            humanPlayer.setAvatar(humanAvatar);
            aiPlayer.setAvatar(aiAvatar);

            // 创建牌库
            createPlayerDecks(humanPlayer, aiPlayer);

            // 初始化玩家（设置牌库、头像并抽初始手牌）
            humanPlayer.initialize(humanPlayer.getDeck(), humanAvatar);
            aiPlayer.initialize(aiPlayer.getDeck(), aiAvatar);

            // 更新游戏状态
            gameState.initializeGame(humanPlayer, aiPlayer);

            // 初始化棋盘显示
            initializeBoard();

            // 显示玩家头像
            BasicCommands.drawUnit(out, humanAvatar, gameState.getTile(2, 2));
            BasicCommands.setUnitAttack(out, humanAvatar, humanAvatar.getAttackValue());
            BasicCommands.setUnitHealth(out, humanAvatar, humanAvatar.getHealth());

            BasicCommands.drawUnit(out, aiAvatar, gameState.getTile(7, 2));
            BasicCommands.setUnitAttack(out, aiAvatar, aiAvatar.getAttackValue());
            BasicCommands.setUnitHealth(out, aiAvatar, aiAvatar.getHealth());

            // 更新玩家信息显示
            updatePlayerStats(humanPlayer, aiPlayer);

            // 显示初始手牌
            drawInitialHands(humanPlayer, aiPlayer);

            // 初始化完成通知
            BasicCommands.addPlayer1Notification(out, "游戏初始化完成，轮到你了", 2);

        } catch (Exception e) {
            e.printStackTrace();
            BasicCommands.addPlayer1Notification(out, "初始化游戏时出错", 2);
        }
    }

    /**
     * 创建头像单位
     */
    private Unit createAvatar(String avatarConf, int id, int tilex, int tiley) {
        Unit avatar = BasicObjectBuilders.loadUnit(avatarConf, id, Unit.class);
        avatar.setPositionByTile(gameState.getTile(tilex, tiley));
        avatar.setAttackValue(2);
        avatar.setHealth(20);
        avatar.setMaxHealth(20);
        avatar.setUnitType(Unit.UnitType.AVATAR);

        return avatar;
    }

    /**
     * 创建玩家牌库
     */
    private void createPlayerDecks(Player humanPlayer, Player aiPlayer) {
        // 加载人类玩家牌库（Abyssian Swarm）
        List<Card> humanCards = OrderedCardLoader.getPlayer1Cards(1);
        Deck humanDeck = new Deck(humanCards, humanPlayer);
        humanDeck.shuffle();
        humanPlayer.setDeck(humanDeck);

        // 加载AI玩家牌库（Lyonar Generalist）
        List<Card> aiCards = OrderedCardLoader.getPlayer2Cards(1);
        Deck aiDeck = new Deck(aiCards, aiPlayer);
        aiDeck.shuffle();
        aiPlayer.setDeck(aiDeck);
    }

    /**
     * 初始化棋盘显示
     */
    private void initializeBoard() {
        for (int x = 0; x < gameState.getBoardWidth(); x++) {
            for (int y = 0; y < gameState.getBoardHeight(); y++) {
                Tile tile = gameState.getTile(x, y);
                if (tile != null) {
                    BasicCommands.drawTile(out, tile, 0);
                }
            }
        }
    }

    /**
     * 更新玩家状态显示
     */
    private void updatePlayerStats(Player humanPlayer, Player aiPlayer) {
        // 更新生命值显示
        BasicCommands.setPlayer1Health(out, humanPlayer);
        BasicCommands.setPlayer2Health(out, aiPlayer);

        // 更新法力值显示
        BasicCommands.setPlayer1Mana(out, humanPlayer);
        BasicCommands.setPlayer2Mana(out, aiPlayer);
    }

    /**
     * 绘制初始手牌
     */
    private void drawInitialHands(Player humanPlayer, Player aiPlayer) {
        // 绘制人类玩家初始手牌
        if (humanPlayer.getHand() != null) {
            List<Card> humanCards = humanPlayer.getHand().getCards();
            for (int i = 0; i < humanCards.size(); i++) {
                Card card = humanCards.get(i);
                BasicCommands.drawCard(out, card, i + 1, 0);
            }
        }

        // AI玩家的手牌不显示给人类玩家
    }

    /**
     * 开始新回合
     */
    public void startNewTurn() {
        // 调用GameState开始新回合
        Player activePlayer = gameState.startNewTurn();

        // 更新回合计数显示
        // BasicCommands.setTurnNumber(out, gameState.getTurnNumber()); // 如需实现

        // 根据当前玩家类型执行不同操作
        if (activePlayer.isHuman()) {
            startHumanPlayerTurn(activePlayer);
        } else {
            startAIPlayerTurn(activePlayer);
        }
    }

    /**
     * 开始人类玩家回合
     */
    private void startHumanPlayerTurn(Player humanPlayer) {
        // 显示回合开始通知
        BasicCommands.addPlayer1Notification(out, "你的回合", 2);

        // 更新玩家法力值显示
        BasicCommands.setPlayer1Mana(out, humanPlayer);

        // 绘制新抽的牌
        Card newCard = humanPlayer.getHand().getCards().get(humanPlayer.getHand().size() - 1);
        if (newCard != null) {
            BasicCommands.drawCard(out, newCard, humanPlayer.getHand().size(), 0);
        }
    }

    /**
     * 开始AI玩家回合
     */
    private void startAIPlayerTurn(Player aiPlayer) {
        // 显示AI回合开始通知
        BasicCommands.addPlayer1Notification(out, "AI回合", 2);

        // 更新AI玩家法力值显示
        BasicCommands.setPlayer2Mana(out, aiPlayer);

        // 执行AI动作
        executeAITurn();
    }

    /**
     * 执行AI回合
     */
    public void executeAITurn() {
        // 延迟一小段时间，让玩家有时间看到状态变化
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 委托给AIController处理AI决策
        aiController.executeTurn(gameState);

        // AI回合结束
        endAITurn();
    }

    /**
     * 结束AI回合
     */
    private void endAITurn() {
        Player aiPlayer = gameState.getAiPlayer();

        // AI玩家结束回合
        aiPlayer.endTurn();

        // 更新游戏状态
        gameState.endTurn();

        // 通知回合结束
        BasicCommands.addPlayer1Notification(out, "AI回合结束", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 检查游戏是否结束
        if (checkGameOver()) {
            return;
        }

        // 开始人类玩家的新回合
        startNewTurn();
    }

    /**
     * 结束人类玩家回合
     */
    public void endHumanTurn() {
        Player humanPlayer = gameState.getHumanPlayer();

        // 清除所选内容
        clearSelections();

        // 人类玩家结束回合
        humanPlayer.endTurn();

        // 更新游戏状态
        gameState.endTurn();

        // 通知回合结束
        BasicCommands.addPlayer1Notification(out, "回合结束", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 检查游戏是否结束
        if (checkGameOver()) {
            return;
        }

        // 开始AI玩家的新回合
        startNewTurn();
    }

    /**
     * 清除选中的单位和卡牌，以及棋盘高亮
     */
    public void clearSelections() {
        // 清除单位选择
        gameState.setSelectedUnit(null);

        // 清除卡牌选择
        gameState.setSelectedCard(null);

        // 清除所有高亮格子
        clearTileHighlights();
    }

    /**
     * 清除棋盘高亮
     */
    private void clearTileHighlights() {
        for (int x = 0; x < gameState.getBoardWidth(); x++) {
            for (int y = 0; y < gameState.getBoardHeight(); y++) {
                Tile tile = gameState.getTile(x, y);
                if (tile != null) {
                    BasicCommands.drawTile(out, tile, 0); // 0表示不高亮
                }
            }
        }
    }

    /**
     * 处理格子点击事件
     */
    public void handleTileClick(int tilex, int tiley) {
        // 获取点击的格子
        Tile clickedTile = gameState.getTile(tilex, tiley);
        if (clickedTile == null) {
            return;
        }

        // 获取当前玩家
        Player humanPlayer = gameState.getHumanPlayer();

        // 获取选中的单位和卡牌
        Unit selectedUnit = gameState.getSelectedUnit();
        Card selectedCard = gameState.getSelectedCard();

        // 获取点击格子上的单位
        Unit unitOnTile = gameState.getUnitAtTile(clickedTile);

        // 根据当前选中状态和点击格子处理点击事件
        if (selectedUnit != null) {
            // 已经选中了一个单位，处理移动或攻击
            handleUnitAction(selectedUnit, clickedTile, unitOnTile);
        } else if (selectedCard != null) {
            // 已经选中了一张卡牌，尝试使用卡牌
            cardController.useCard(selectedCard, clickedTile, humanPlayer);
        } else if (unitOnTile != null) {
            // 点击了一个有单位的格子，尝试选中该单位
            handleUnitSelection(unitOnTile, clickedTile, humanPlayer);
        } else {
            // 点击了一个空格子，取消所有选择
            clearSelections();
        }
    }

    /**
     * 处理单位选择
     */
    private void handleUnitSelection(Unit unit, Tile tile, Player player) {
        // 如果单位是玩家的，选中该单位
        if (unit.getOwner() == player) {
            // 检查单位是否可以行动
            if (unit.canMove() || unit.canAttack()) {
                // 设置为选中的单位
                gameState.setSelectedUnit(unit);

                // 高亮显示这个格子
                BasicCommands.drawTile(out, tile, 1); // 1表示高亮

                // 高亮显示可移动的位置
                highlightValidMoves(unit);
            } else {
                BasicCommands.addPlayer1Notification(out, "该单位已经行动过", 2);
            }
        } else {
            // 如果是敌方单位，显示单位信息
            clearSelections();
        }
    }

    /**
     * 高亮显示有效移动位置
     */
    private void highlightValidMoves(Unit unit) {
        List<Tile> validMoves = gameState.getValidMoves(unit);

        for (Tile tile : validMoves) {
            BasicCommands.drawTile(out, tile, 1); // 1表示高亮
        }
    }

    /**
     * 处理单位行动（移动或攻击）
     */
    private void handleUnitAction(Unit unit, Tile targetTile, Unit targetUnit) {
        // 检查单位所属玩家
        if (unit.getOwner() != gameState.getHumanPlayer()) {
            clearSelections();
            return;
        }

        // 如果点击的格子上有单位
        if (targetUnit != null) {
            // 如果是敌方单位，尝试攻击
            if (targetUnit.getOwner() != unit.getOwner()) {
                combatController.attackUnit(unit, targetUnit);
            } else {
                // 如果是友方单位，切换选中的单位
                clearTileHighlights();
                gameState.setSelectedUnit(targetUnit);
                highlightValidMoves(targetUnit);
            }
        } else {
            // 如果点击的是空格子，尝试移动
            moveUnit(unit, targetTile);
        }
    }

    /**
     * 移动单位到目标位置
     */
    private void moveUnit(Unit unit, Tile targetTile) {
        if (gameState.isValidMove(unit, targetTile)) {
            // 移动单位
            BasicCommands.moveUnitToTile(out, unit, targetTile);
            try {Thread.sleep(1500);} catch (InterruptedException e) {e.printStackTrace();}

            // 更新游戏状态
            gameState.moveUnit(unit, targetTile);

            // 清除高亮和选择
            clearSelections();
        } else {
            BasicCommands.addPlayer1Notification(out, "无法移动到该位置", 2);
        }
    }

    /**
     * 处理卡牌点击事件
     */
    public void handleCardClick(int handPosition) {
        cardController.handleCardClick(handPosition, gameState);
    }

    /**
     * 检查游戏是否结束
     * @return 如果游戏结束返回true
     */
    public boolean checkGameOver() {
        if (gameState.checkGameOver()) {
            // 游戏结束，显示获胜者
            Player winner = gameState.getWinner();

            if (winner == gameState.getHumanPlayer()) {
                BasicCommands.addPlayer1Notification(out, "你赢了！", 5);
            } else {
                BasicCommands.addPlayer1Notification(out, "你输了！", 5);
            }

            return true;
        }

        return false;
    }

    /**
     * 播放效果动画
     */
    public void playEffectAnimation(EffectAnimation effect, Tile tile) {
        if (effect != null && tile != null) {
            BasicCommands.playEffectAnimation(out, effect, tile);
        }
    }

    // Getters

    public GameState getGameState() {
        return gameState;
    }

    public CardController getCardController() {
        return cardController;
    }

    public CombatController getCombatController() {
        return combatController;
    }

    public AIController getAIController() {
        return aiController;
    }
}