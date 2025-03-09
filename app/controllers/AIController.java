package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * AI控制器类，负责管理AI玩家的决策和行动。
 * 实现简单的AI行为，包括卡牌使用、单位移动和攻击等操作。
 *
 * @author Your Team
 */
public class AIController {

    private ActorRef out;
    private GameState gameState;
    private Random random = new Random();

    /**
     * 默认构造函数
     */
    public AIController() {
        this.gameState = GameState.getInstance();
    }

    /**
     * 设置ActorRef，用于向前端发送命令
     * @param out ActorRef引用
     */
    public void setOut(ActorRef out) {
        this.out = out;
    }

    /**
     * 执行AI回合
     * @param gameState 当前游戏状态
     */
    public void executeTurn(GameState gameState) {
        Player aiPlayer = gameState.getAiPlayer();
        Player humanPlayer = gameState.getHumanPlayer();

        // 确保是AI回合
        if (gameState.getCurrentPhase() != GameState.GamePhase.AI_TURN) {
            return;
        }

        try {
            // 添加延迟，让AI思考看起来更自然
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 1. 使用卡牌（优先使用能够直接攻击敌方头像或清除威胁的卡牌）
        useCards(aiPlayer);

        try {
            // 添加延迟
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 2. 移动单位（优先向敌方头像移动）
        moveUnits(aiPlayer, humanPlayer);

        try {
            // 添加延迟
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. 攻击敌方单位（优先攻击有威胁的单位和敌方头像）
        attackEnemyUnits(aiPlayer, humanPlayer);
    }

    /**
     * AI使用卡牌
     * @param aiPlayer AI玩家
     */
    private void useCards(Player aiPlayer) {
        // 获取当前可用的卡牌
        List<Card> playableCards = aiPlayer.getPlayableCards();
        if (playableCards.isEmpty()) {
            return;
        }

        // 按优先级排序卡牌（较简单的实现，可以根据需要扩展）
        Collections.sort(playableCards, new Comparator<Card>() {
            @Override
            public int compare(Card c1, Card c2) {
                // 优先使用法术卡
                if (!c1.isCreature() && c2.isCreature()) return -1;
                if (c1.isCreature() && !c2.isCreature()) return 1;

                // 其次按照法力值成本排序（优先使用高费卡）
                return c2.getManacost() - c1.getManacost();
            }
        });

        // 尝试使用卡牌
        for (Card card : playableCards) {
            List<Tile> validTargets = card.getValidTargets(gameState);
            if (validTargets.isEmpty()) {
                continue;
            }

            // 选择最佳目标
            Tile bestTarget = chooseBestTarget(card, validTargets);
            if (bestTarget != null) {
                // 使用卡牌
                aiPlayer.useCard(card, bestTarget, gameState);

                // 显示卡牌使用动画（可选）
                if (card.isCreature()) {
                    // 播放召唤效果
                    EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
                    BasicCommands.playEffectAnimation(out, summonEffect, bestTarget);
                } else {
                    // 播放法术效果
                    EffectAnimation spellEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
                    BasicCommands.playEffectAnimation(out, spellEffect, bestTarget);
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // 每次只使用一张卡牌，以免AI操作太快
                break;
            }
        }
    }

    /**
     * 选择卡牌的最佳目标
     * @param card 卡牌
     * @param validTargets 有效目标列表
     * @return 最佳目标位置
     */
    private Tile chooseBestTarget(Card card, List<Tile> validTargets) {
        if (validTargets.isEmpty()) {
            return null;
        }

        // 获取人类玩家的头像
        Unit humanAvatar = gameState.getHumanPlayer().getAvatar();

        // 对于法术卡
        if (!card.isCreature()) {
            // 优先选择敌方头像附近的目标
            for (Tile target : validTargets) {
                Unit unitOnTile = gameState.getUnitAtTile(target);
                if (unitOnTile != null && unitOnTile == humanAvatar) {
                    return target; // 直接攻击敌方头像
                }
            }

            // 其次选择有敌方单位的格子
            for (Tile target : validTargets) {
                Unit unitOnTile = gameState.getUnitAtTile(target);
                if (unitOnTile != null && unitOnTile.getOwner() == gameState.getHumanPlayer()) {
                    return target; // 攻击敌方单位
                }
            }
        }
        // 对于生物卡
        else {
            // 尝试找到离敌方头像最近的有效位置
            if (humanAvatar != null) {
                Tile humanAvatarTile = gameState.getTile(humanAvatar.getPosition().getTilex(), humanAvatar.getPosition().getTiley());
                Tile closestTile = null;
                int minDistance = Integer.MAX_VALUE;

                for (Tile target : validTargets) {
                    int distance = calculateDistance(target, humanAvatarTile);
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestTile = target;
                    }
                }

                if (closestTile != null) {
                    return closestTile;
                }
            }
        }

        // 如果没有特别好的目标，随机选择一个
        return validTargets.get(random.nextInt(validTargets.size()));
    }

    /**
     * 计算两个格子之间的距离
     * @param tile1 第一个格子
     * @param tile2 第二个格子
     * @return 两个格子之间的距离
     */
    private int calculateDistance(Tile tile1, Tile tile2) {
        int dx = Math.abs(tile1.getTilex() - tile2.getTilex());
        int dy = Math.abs(tile1.getTiley() - tile2.getTiley());
        return Math.max(dx, dy); // 使用切比雪夫距离
    }

    /**
     * AI移动单位
     * @param aiPlayer AI玩家
     * @param humanPlayer 人类玩家
     */
    private void moveUnits(Player aiPlayer, Player humanPlayer) {
        // 获取AI控制的所有单位
        List<Unit> aiUnits = new ArrayList<>();
        for (Unit unit : gameState.getAllUnits()) {
            if (unit.getOwner() == aiPlayer && unit.canMove()) {
                aiUnits.add(unit);
            }
        }

        // 如果没有可移动的单位，直接返回
        if (aiUnits.isEmpty()) {
            return;
        }

        // 优先移动能够攻击敌方头像的单位
        Unit humanAvatar = humanPlayer.getAvatar();
        if (humanAvatar != null) {
            Tile humanAvatarTile = gameState.getTile(humanAvatar.getPosition().getTilex(), humanAvatar.getPosition().getTiley());

            for (Unit unit : aiUnits) {
                // 获取单位的有效移动位置
                List<Tile> validMoves = gameState.getValidMoves(unit);
                if (validMoves.isEmpty()) {
                    continue;
                }

                // 找到移动后能攻击敌方头像的位置
                Tile bestMove = null;
                int minDistance = Integer.MAX_VALUE;

                for (Tile moveTile : validMoves) {
                    int distance = calculateDistance(moveTile, humanAvatarTile);
                    if (distance < minDistance) {
                        minDistance = distance;
                        bestMove = moveTile;
                    }
                }

                // 如果找到了合适的移动位置
                if (bestMove != null) {
                    // 移动单位
                    BasicCommands.moveUnitToTile(out, unit, bestMove);
                    try {
                        Thread.sleep(2000); // 等待移动动画完成
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 更新游戏状态
                    gameState.moveUnit(unit, bestMove);

                    // 每次只移动一个单位，以免AI操作太快
                    break;
                }
            }
        }
    }

    /**
     * AI攻击敌方单位
     * @param aiPlayer AI玩家
     * @param humanPlayer 人类玩家
     */
    private void attackEnemyUnits(Player aiPlayer, Player humanPlayer) {
        // 获取AI控制的所有单位
        List<Unit> aiUnits = new ArrayList<>();
        for (Unit unit : gameState.getAllUnits()) {
            if (unit.getOwner() == aiPlayer && unit.canAttack()) {
                aiUnits.add(unit);
            }
        }

        // 如果没有可攻击的单位，直接返回
        if (aiUnits.isEmpty()) {
            return;
        }

        // 优先攻击敌方头像
        Unit humanAvatar = humanPlayer.getAvatar();
        if (humanAvatar != null) {
            for (Unit unit : aiUnits) {
                if (gameState.canAttack(unit, humanAvatar)) {
                    // 执行攻击
                    performAttack(unit, humanAvatar);

                    // 每次只攻击一次，以免AI操作太快
                    return;
                }
            }
        }

        // 如果不能攻击敌方头像，攻击其他敌方单位
        List<Unit> humanUnits = new ArrayList<>();
        for (Unit unit : gameState.getAllUnits()) {
            if (unit.getOwner() == humanPlayer && unit != humanAvatar) {
                humanUnits.add(unit);
            }
        }

        // 按威胁程度排序敌方单位（简单实现，可以根据需要扩展）
        Collections.sort(humanUnits, new Comparator<Unit>() {
            @Override
            public int compare(Unit u1, Unit u2) {
                // 优先攻击攻击力高的单位
                return u2.getAttackValue() - u1.getAttackValue();
            }
        });

        // 尝试攻击敌方单位
        for (Unit humanUnit : humanUnits) {
            for (Unit aiUnit : aiUnits) {
                if (gameState.canAttack(aiUnit, humanUnit)) {
                    // 执行攻击
                    performAttack(aiUnit, humanUnit);

                    // 每次只攻击一次，以免AI操作太快
                    return;
                }
            }
        }
    }

    /**
     * 执行攻击操作
     * @param attacker 攻击单位
     * @param defender 防御单位
     */
    private void performAttack(Unit attacker, Unit defender) {
        // 播放攻击动画
        BasicCommands.playUnitAnimation(out, attacker, structures.basic.UnitAnimationType.attack);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 执行攻击
        gameState.attackUnit(attacker, defender);

        // 更新UI显示
        BasicCommands.setUnitHealth(out, defender, defender.getHealth());
        BasicCommands.setUnitHealth(out, attacker, attacker.getHealth());

        // 检查单位是否死亡
        if (defender.isDead()) {
            BasicCommands.playUnitAnimation(out, defender, structures.basic.UnitAnimationType.death);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BasicCommands.deleteUnit(out, defender);
        }

        if (attacker.isDead()) {
            BasicCommands.playUnitAnimation(out, attacker, structures.basic.UnitAnimationType.death);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BasicCommands.deleteUnit(out, attacker);
        }
    }
}