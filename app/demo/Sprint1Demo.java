package demo;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.UnitAnimationType;
import structures.deck.Deck;
import structures.hand.Hand;
import utils.BasicObjectBuilders;
import utils.OrderedCardLoader;
import utils.StaticConfFiles;

import java.util.List;

/**
 * 冲刺周期一演示类
 * 用于测试核心数据模型和基础框架的功能
 *
 * @author Your Team
 */
public class Sprint1Demo {

    /**
     * 执行冲刺周期一功能的演示
     * @param out ActorRef 用于发送命令到前端
     */
    public static void executeDemo(ActorRef out) {
        BasicCommands.addPlayer1Notification(out, "Sprint 1 Demo", 2);
        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

        // 创建游戏状态
        GameState gameState = GameState.getInstance();

        // 创建玩家
        demoPlayerCreation(out, gameState);

        // 演示牌库和手牌管理
        demoDeckAndHand(out, gameState);

        // 演示单位创建和能力
        demoUnitCreation(out, gameState);

        // 演示移动和攻击
        demoMovementAndCombat(out, gameState);

        // 演示卡牌使用
        demoCardUsage(out, gameState);

        BasicCommands.addPlayer1Notification(out, "Sprint 1 Demo Completed", 3);
        try {Thread.sleep(3000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    /**
     * 玩家创建演示
     */
    private static void demoPlayerCreation(ActorRef out, GameState gameState) {
        BasicCommands.addPlayer1Notification(out, "创建玩家...", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 创建人类玩家
        Player humanPlayer = new Player(1, Player.PlayerType.HUMAN, "人类玩家", 20, 2);

        // 创建AI玩家
        Player aiPlayer = new Player(2, Player.PlayerType.AI, "AI玩家", 20, 2);

        // 加载头像单位
        Unit humanAvatar = BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 1, Unit.class);
        humanAvatar.setPositionByTile(BasicObjectBuilders.loadTile(2, 2));
        humanAvatar.setAttackValue(2);
        humanAvatar.setHealth(20);
        humanAvatar.setMaxHealth(20);

        Unit aiAvatar = BasicObjectBuilders.loadUnit(StaticConfFiles.aiAvatar, 2, Unit.class);
        aiAvatar.setPositionByTile(BasicObjectBuilders.loadTile(7, 2));
        aiAvatar.setAttackValue(2);
        aiAvatar.setHealth(20);
        aiAvatar.setMaxHealth(20);

        // 设置玩家头像
        humanPlayer.setAvatar(humanAvatar);
        aiPlayer.setAvatar(aiAvatar);

        // 初始化游戏状态
        gameState.initializeGame(humanPlayer, aiPlayer);

        // 在UI上显示玩家
        BasicCommands.drawUnit(out, humanAvatar, BasicObjectBuilders.loadTile(2, 2));
        try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

        BasicCommands.drawUnit(out, aiAvatar, BasicObjectBuilders.loadTile(7, 2));
        try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

        BasicCommands.setPlayer1Health(out, humanPlayer);
        BasicCommands.setPlayer2Health(out, aiPlayer);
        try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

        BasicCommands.setPlayer1Mana(out, humanPlayer);
        BasicCommands.setPlayer2Mana(out, aiPlayer);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        BasicCommands.addPlayer1Notification(out, "玩家创建成功", 1);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    /**
     * 牌库和手牌演示
     */
    private static void demoDeckAndHand(ActorRef out, GameState gameState) {
        BasicCommands.addPlayer1Notification(out, "牌库和手牌演示...", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 获取人类玩家
        Player humanPlayer = gameState.getHumanPlayer();

        // 创建牌库
        List<Card> cardList = OrderedCardLoader.getPlayer1Cards(1);
        Deck deck = new Deck(cardList, humanPlayer);

        // 洗牌
        deck.shuffle();

        // 创建手牌
        Hand hand = new Hand(humanPlayer);

        // 设置玩家的牌库和手牌
        humanPlayer.setDeck(deck);
        humanPlayer.setHand(hand);

        // 抽取一些卡牌并显示在UI上
        for (int i = 0; i < 4; i++) {
            Card card = humanPlayer.drawCard();
            if (card != null) {
                BasicCommands.drawCard(out, card, i + 1, 0);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }

        BasicCommands.addPlayer1Notification(out, "牌库大小: " + deck.size() + ", 手牌大小: " + hand.size(), 2);
        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    /**
     * 单位创建和能力演示
     */
    private static void demoUnitCreation(ActorRef out, GameState gameState) {
        BasicCommands.addPlayer1Notification(out, "单位创建和能力演示...", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 获取人类玩家
        Player humanPlayer = gameState.getHumanPlayer();

        // 创建一个具有嘲讽能力的单位
        Unit provokeUnit = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, 3, Unit.class);
        provokeUnit.setPositionByTile(BasicObjectBuilders.loadTile(3, 1));
        provokeUnit.setAttackValue(2);
        provokeUnit.setHealth(5);
        provokeUnit.setMaxHealth(5);
        provokeUnit.setOwner(humanPlayer);

        // 添加嘲讽能力
        provokeUnit.addAbility(new Unit.ProvokeAbility());

        // 在UI上显示单位
        BasicCommands.drawUnit(out, provokeUnit, BasicObjectBuilders.loadTile(3, 1));
        BasicCommands.setUnitAttack(out, provokeUnit, provokeUnit.getAttackValue());
        BasicCommands.setUnitHealth(out, provokeUnit, provokeUnit.getHealth());
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 创建一个具有飞行能力的单位
        Unit flyingUnit = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, 4, Unit.class);
        flyingUnit.setPositionByTile(BasicObjectBuilders.loadTile(4, 3));
        flyingUnit.setAttackValue(3);
        flyingUnit.setHealth(2);
        flyingUnit.setMaxHealth(2);
        flyingUnit.setOwner(humanPlayer);

        // 添加飞行能力
        flyingUnit.addAbility(new Unit.FlyingAbility());

        // 在UI上显示单位
        BasicCommands.drawUnit(out, flyingUnit, BasicObjectBuilders.loadTile(4, 3));
        BasicCommands.setUnitAttack(out, flyingUnit, flyingUnit.getAttackValue());
        BasicCommands.setUnitHealth(out, flyingUnit, flyingUnit.getHealth());
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 添加单位到游戏状态
        gameState.addUnit(provokeUnit, BasicObjectBuilders.loadTile(3, 1));
        gameState.addUnit(flyingUnit, BasicObjectBuilders.loadTile(4, 3));

        BasicCommands.addPlayer1Notification(out, "单位创建成功，已添加嘲讽和飞行能力", 2);
        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    /**
     * 移动和战斗演示
     */
    private static void demoMovementAndCombat(ActorRef out, GameState gameState) {
        BasicCommands.addPlayer1Notification(out, "移动和战斗演示...", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 获取AI玩家
        Player aiPlayer = gameState.getAiPlayer();

        // 创建敌方单位
        Unit enemyUnit = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, 5, Unit.class);
        enemyUnit.setPositionByTile(BasicObjectBuilders.loadTile(5, 2));
        enemyUnit.setAttackValue(1);
        enemyUnit.setHealth(3);
        enemyUnit.setMaxHealth(3);
        enemyUnit.setOwner(aiPlayer);

        // 在UI上显示单位
        BasicCommands.drawUnit(out, enemyUnit, BasicObjectBuilders.loadTile(5, 2));
        BasicCommands.setUnitAttack(out, enemyUnit, enemyUnit.getAttackValue());
        BasicCommands.setUnitHealth(out, enemyUnit, enemyUnit.getHealth());
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 添加单位到游戏状态
        gameState.addUnit(enemyUnit, BasicObjectBuilders.loadTile(5, 2));

        // 移动飞行单位
        List<Unit> allUnits = gameState.getAllUnits();
        Unit flyingUnit = null;
        for (Unit unit : allUnits) {
            if (unit.hasAbility("Flying")) {
                flyingUnit = unit;
                break;
            }
        }

        if (flyingUnit != null) {
            Tile targetTile = BasicObjectBuilders.loadTile(6, 3);

            BasicCommands.addPlayer1Notification(out, "移动飞行单位...", 1);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            // 移动单位
            BasicCommands.moveUnitToTile(out, flyingUnit, targetTile);
            try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

            // 更新游戏状态中的单位位置
            gameState.moveUnit(flyingUnit, targetTile);

            // 飞行单位攻击敌方单位
            BasicCommands.addPlayer1Notification(out, "飞行单位攻击敌方单位...", 1);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            // 播放攻击动画
            BasicCommands.playUnitAnimation(out, flyingUnit, UnitAnimationType.attack);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            // 敌方单位受到伤害
            enemyUnit.takeDamage(flyingUnit.getAttackValue());
            BasicCommands.setUnitHealth(out, enemyUnit, enemyUnit.getHealth());
            try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

            // 反击
            BasicCommands.playUnitAnimation(out, enemyUnit, UnitAnimationType.attack);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            flyingUnit.takeDamage(enemyUnit.getAttackValue());
            BasicCommands.setUnitHealth(out, flyingUnit, flyingUnit.getHealth());
            try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

            // 如果单位死亡，移除它
            if (enemyUnit.isDead()) {
                BasicCommands.addPlayer1Notification(out, "敌方单位被消灭", 1);
                BasicCommands.playUnitAnimation(out, enemyUnit, UnitAnimationType.death);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                BasicCommands.deleteUnit(out, enemyUnit);
                gameState.removeUnit(enemyUnit);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
            }

            if (flyingUnit.isDead()) {
                BasicCommands.addPlayer1Notification(out, "飞行单位被消灭", 1);
                BasicCommands.playUnitAnimation(out, flyingUnit, UnitAnimationType.death);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                BasicCommands.deleteUnit(out, flyingUnit);
                gameState.removeUnit(flyingUnit);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }

        BasicCommands.addPlayer1Notification(out, "移动和战斗演示完成", 1);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
    }

    /**
     * 卡牌使用演示
     */
    private static void demoCardUsage(ActorRef out, GameState gameState) {
        BasicCommands.addPlayer1Notification(out, "卡牌使用演示...", 2);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 获取人类玩家
        Player humanPlayer = gameState.getHumanPlayer();

        // 获取AI玩家
        Player aiPlayer = gameState.getAiPlayer();

        // 获取人类玩家手牌中的第一张卡
        Hand hand = humanPlayer.getHand();
        if (hand != null && !hand.isEmpty()) {
            Card card = hand.getCard(0);

            if (card != null) {
                BasicCommands.addPlayer1Notification(out, "高亮显示卡牌: " + card.getCardname(), 1);
                BasicCommands.drawCard(out, card, 1, 1);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                // 如果是法术卡，模拟使用
                if (!card.isCreature()) {
                    // 获取AI头像作为目标
                    Unit target = aiPlayer.getAvatar();
                    Tile targetTile = BasicObjectBuilders.loadTile(target.getPosition().getTilex(), target.getPosition().getTiley());

                    // 播放法术效果 - 修正loadEffect调用
                    EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
                    BasicCommands.playEffectAnimation(out, effect, targetTile);
                    try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

                    // 对目标造成伤害
                    target.takeDamage(2);
                    BasicCommands.setUnitHealth(out, target, target.getHealth());
                    BasicCommands.setPlayer2Health(out, aiPlayer);
                    try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
                }
                // 如果是生物卡，模拟召唤
                else {
                    Tile summonTile = BasicObjectBuilders.loadTile(3, 3);

                    // 检查该位置是否可用
                    if (gameState.getUnitAtTile(summonTile) == null) {
                        Unit unit = BasicObjectBuilders.loadUnit(card.getUnitConfig(), 6, Unit.class);
                        unit.setPositionByTile(summonTile);
                        unit.setOwner(humanPlayer);

                        // 从大卡片获取单位属性
                        if (card.getBigCard() != null) {
                            unit.setAttackValue(card.getBigCard().getAttack());
                            unit.setHealth(card.getBigCard().getHealth());
                            unit.setMaxHealth(card.getBigCard().getHealth());
                        } else {
                            unit.setAttackValue(2);
                            unit.setHealth(2);
                            unit.setMaxHealth(2);
                        }

                        // 播放召唤效果 - 修正loadEffect调用
                        EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
                        BasicCommands.playEffectAnimation(out, summonEffect, summonTile);
                        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

                        // 在UI上显示单位
                        BasicCommands.drawUnit(out, unit, summonTile);
                        BasicCommands.setUnitAttack(out, unit, unit.getAttackValue());
                        BasicCommands.setUnitHealth(out, unit, unit.getHealth());
                        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                        // 添加单位到游戏状态
                        gameState.addUnit(unit, summonTile);
                    }
                }

                // 从手牌删除卡牌
                BasicCommands.deleteCard(out, 1);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}

                // 减少玩家法力值
                humanPlayer.setMana(Math.max(0, humanPlayer.getMana() - card.getManacost()));
                BasicCommands.setPlayer1Mana(out, humanPlayer);
                try {Thread.sleep(500);} catch (InterruptedException e) {e.printStackTrace();}
            }
        }

        BasicCommands.addPlayer1Notification(out, "卡牌使用演示完成", 1);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
    }
}