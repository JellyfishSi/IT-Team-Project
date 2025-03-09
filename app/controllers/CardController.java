package controllers;

import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.hand.Hand;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * 卡牌控制器类，负责处理卡牌相关的操作。
 * 包括卡牌选择、使用和效果处理等功能。
 *
 * @author Your Team
 */
public class CardController {

    private ActorRef out;
    private GameState gameState;

    /**
     * 构造函数
     */
    public CardController() {
        this.gameState = GameState.getInstance();
    }

    /**
     * 设置ActorRef，用于向前端发送命令
     */
    public void setOut(ActorRef out) {
        this.out = out;
    }

    /**
     * 处理卡牌点击事件
     * @param handPosition 手牌位置
     * @return 是否成功处理
     */
    public boolean handleCardClick(int handPosition) {
        // 检查游戏是否已初始化
        if (!gameState.isGameInitialized()) {
            return false;
        }

        // 检查是否是游戏结束状态
        if (gameState.isGameEnded()) {
            return false;
        }

        // 检查是否是玩家回合
        if (gameState.getCurrentPhase() != GameState.GamePhase.PLAYER_TURN) {
            BasicCommands.addPlayer1Notification(out, "不是您的回合", 2);
            return false;
        }

        // 获取人类玩家
        Player humanPlayer = gameState.getHumanPlayer();
        if (humanPlayer == null || humanPlayer.getHand() == null) {
            return false;
        }

        // 清除先前的选择
        clearPreviousSelections();

        // 获取点击的卡牌
        Hand hand = humanPlayer.getHand();
        Card clickedCard = hand.getCardByGamePosition(handPosition);

        if (clickedCard == null) {
            return false; // 该位置没有卡牌
        }

        // 检查法力值是否足够
        if (humanPlayer.getMana() < clickedCard.getManacost()) {
            BasicCommands.addPlayer1Notification(out, "法力值不足", 2);
            return false;
        }

        // 设置为当前选择的卡牌
        gameState.setSelectedCard(clickedCard);

        // 设置卡牌的手牌位置（方便后续使用）
        clickedCard.setHandPosition(handPosition);

        // 高亮显示卡牌
        BasicCommands.drawCard(out, clickedCard, handPosition, 1); // 1表示高亮

        // 高亮显示有效的目标位置
        highlightValidTargets(clickedCard);

        return true;
    }

    /**
     * 高亮显示卡牌的有效目标位置
     * @param card 选中的卡牌
     */
    private void highlightValidTargets(Card card) {
        // 获取卡牌的所有有效目标位置
        List<Tile> validTargets = card.getValidTargets(gameState);

        // 高亮显示这些位置
        for (Tile tile : validTargets) {
            // 对于生物卡，使用普通高亮
            if (card.isCreature()) {
                BasicCommands.drawTile(out, tile, 1); // 1表示高亮
            }
            // 对于法术卡，根据目标类型使用不同的高亮
            else {
                // 如果目标是敌方单位，使用红色高亮
                Unit unitOnTile = gameState.getUnitAtTile(tile);
                if (unitOnTile != null && unitOnTile.getOwner() != card.getOwner()) {
                    BasicCommands.drawTile(out, tile, 2); // 2表示红色高亮
                } else {
                    BasicCommands.drawTile(out, tile, 1); // 1表示普通高亮
                }
            }
        }

        // 如果没有有效目标，给出提示
        if (validTargets.isEmpty()) {
            BasicCommands.addPlayer1Notification(out, "没有有效的目标", 2);
            // 取消卡牌选择
            gameState.setSelectedCard(null);
            BasicCommands.drawCard(out, card, card.getHandPosition(), 0); // 0表示不高亮
        }
    }

    /**
     * 使用卡牌
     * @param card 要使用的卡牌
     * @param targetTile 目标格子
     * @param player 卡牌所属玩家
     * @return 是否成功使用
     */
    public boolean useCard(Card card, Tile targetTile, Player player) {
        if (card == null || targetTile == null || player == null) {
            return false;
        }

        // 检查卡牌是否可以在该位置使用
        if (!card.isValidTarget(gameState, targetTile)) {
            BasicCommands.addPlayer1Notification(out, "无效的目标", 2);
            return false;
        }

        // 检查玩家法力值是否足够
        if (player.getMana() < card.getManacost()) {
            BasicCommands.addPlayer1Notification(out, "法力值不足", 2);
            return false;
        }

        // 扣除法力值
        int originalMana = player.getMana();
        player.setMana(originalMana - card.getManacost());

        // 更新法力值显示
        if (player.isHuman()) {
            BasicCommands.setPlayer1Mana(out, player);
        } else {
            BasicCommands.setPlayer2Mana(out, player);
        }

        // 从手牌中移除卡牌
        int handPosition = card.getHandPosition();
        player.getHand().removeCardByGamePosition(handPosition);

        // 更新UI显示
        if (player.isHuman()) {
            BasicCommands.deleteCard(out, handPosition);
        }

        // 执行卡牌效果
        boolean success = executeCardEffect(card, targetTile, player);

        // 如果卡牌效果执行失败，恢复法力值和手牌
        if (!success) {
            player.setMana(originalMana);
            if (player.isHuman()) {
                BasicCommands.setPlayer1Mana(out, player);
            } else {
                BasicCommands.setPlayer2Mana(out, player);
            }
            player.getHand().addCard(card);
            return false;
        }

        // 清除选择
        clearPreviousSelections();

        return true;
    }

    /**
     * 执行卡牌效果
     * @param card 要执行效果的卡牌
     * @param targetTile 目标格子
     * @param player 卡牌所属玩家
     * @return 是否成功执行
     */
    private boolean executeCardEffect(Card card, Tile targetTile, Player player) {
        // 根据卡牌类型执行不同效果
        if (card.isCreature()) {
            // 生物卡 - 召唤单位
            return summonUnit(card, targetTile, player);
        } else {
            // 法术卡 - 根据卡牌名称执行不同效果
            return executeSpellEffect(card, targetTile, player);
        }
    }

    /**
     * 召唤单位
     * @param card 生物卡
     * @param targetTile 目标格子
     * @param player 卡牌所属玩家
     * @return 是否成功召唤
     */
    private boolean summonUnit(Card card, Tile targetTile, Player player) {
        try {
            // 创建单位实例
            Unit unit = card.createUnit(targetTile);
            if (unit == null) {
                return false;
            }

            // 设置单位所有者
            unit.setOwner(player);

            // 播放召唤效果动画
            EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
            BasicCommands.playEffectAnimation(out, summonEffect, targetTile);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            // 在UI上显示单位
            BasicCommands.drawUnit(out, unit, targetTile);
            BasicCommands.setUnitAttack(out, unit, unit.getAttackValue());
            BasicCommands.setUnitHealth(out, unit, unit.getHealth());

            // 添加单位到游戏状态
            gameState.addUnit(unit, targetTile);

            // 标记单位为本回合召唤
            unit.setSummonedThisTurn(true);

            // 触发入场效果
            unit.onSummon(gameState);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 执行法术效果
     * @param card 法术卡
     * @param targetTile 目标格子
     * @param player 卡牌所属玩家
     * @return 是否成功执行
     */
    private boolean executeSpellEffect(Card card, Tile targetTile, Player player) {
        String cardName = card.getCardname();

        try {
            // 根据卡牌名称执行不同的法术效果
            switch (cardName) {
                case "Truestrike":
                    return castTruestrike(targetTile);
                case "Sundrop Elixir":
                    return castSundropElixir(targetTile);
                case "Beam Shock":
                    return castBeamShock(targetTile);
                case "Dark Terminus":
                    return castDarkTerminus(targetTile, player);
                case "Wraithling Swarm":
                    return castWraithlingSwarm(targetTile, player);
                case "Horn of the Forsaken":
                    return castHornOfTheForsaken(targetTile, player);
                default:
                    BasicCommands.addPlayer1Notification(out, "未实现的法术卡：" + cardName, 2);
                    return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 施放真实打击法术
     * 对敌方单位造成2点伤害
     */
    private boolean castTruestrike(Tile targetTile) {
        // 获取目标单位
        Unit targetUnit = gameState.getUnitAtTile(targetTile);
        if (targetUnit == null) {
            return false;
        }

        // 播放法术效果动画
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_projectiles);
        BasicCommands.playEffectAnimation(out, effect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 对目标单位造成2点伤害
        int originalHealth = targetUnit.getHealth();
        targetUnit.takeDamage(2);

        // 更新单位生命值显示
        BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());

        // 如果单位死亡，播放死亡动画并移除
        if (targetUnit.isDead()) {
            BasicCommands.playUnitAnimation(out, targetUnit, structures.basic.UnitAnimationType.death);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
            BasicCommands.deleteUnit(out, targetUnit);
            gameState.removeUnit(targetUnit);
        }

        return true;
    }

    /**
     * 施放阳滴甘露法术
     * 治疗友方单位4点生命值
     */
    private boolean castSundropElixir(Tile targetTile) {
        // 获取目标单位
        Unit targetUnit = gameState.getUnitAtTile(targetTile);
        if (targetUnit == null) {
            return false;
        }

        // 播放治疗效果动画
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, effect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 治疗目标单位4点生命值
        targetUnit.heal(4);

        // 更新单位生命值显示
        BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());

        return true;
    }

    /**
     * 施放光束冲击法术
     * 晕眩敌方单位一回合
     */
    private boolean castBeamShock(Tile targetTile) {
        // 获取目标单位
        Unit targetUnit = gameState.getUnitAtTile(targetTile);
        if (targetUnit == null) {
            return false;
        }

        // 播放晕眩效果动画
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
        BasicCommands.playEffectAnimation(out, effect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 给目标单位添加晕眩状态
        targetUnit.setStatus(Unit.UnitStatus.STUNNED, true);

        return true;
    }

    /**
     * 施放黑暗终结法术
     * 消灭一个敌方单位，并在其位置召唤一个Wraithling
     */
    private boolean castDarkTerminus(Tile targetTile, Player player) {
        // 获取目标单位
        Unit targetUnit = gameState.getUnitAtTile(targetTile);
        if (targetUnit == null) {
            return false;
        }

        // 播放消灭效果动画
        EffectAnimation effect = BasicObjectBuilders.loadEffect("conf/gameconfs/f1_soulshatter.json");
        BasicCommands.playEffectAnimation(out, effect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 先移除目标单位
        BasicCommands.deleteUnit(out, targetUnit);
        gameState.removeUnit(targetUnit);

        // 在该位置召唤一个Wraithling
        Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, generateUnitId(), Unit.class);
        wraithling.setPositionByTile(targetTile);
        wraithling.setOwner(player);
        wraithling.setCombatStats(1, 1); // Wraithling 是1/1单位

        // 播放召唤效果
        EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_summon);
        BasicCommands.playEffectAnimation(out, summonEffect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 显示新单位
        BasicCommands.drawUnit(out, wraithling, targetTile);
        BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackValue());
        BasicCommands.setUnitHealth(out, wraithling, wraithling.getHealth());

        // 添加到游戏状态
        gameState.addUnit(wraithling, targetTile);

        // 标记为本回合召唤
        wraithling.setSummonedThisTurn(true);

        return true;
    }

    /**
     * 施放亡魂大军法术
     * 召唤三个Wraithling
     */
    private boolean castWraithlingSwarm(Tile targetTile, Player player) {
        // 当前目标位置
        Tile currentTile = targetTile;

        // 连续召唤三个Wraithling
        for (int i = 0; i < 3; i++) {
            // 检查当前位置是否有单位
            if (gameState.getUnitAtTile(currentTile) != null) {
                // 寻找附近的空位置
                currentTile = findNearbyEmptyTile(currentTile);
                if (currentTile == null) {
                    // 如果没有空位置，提前结束
                    return i > 0; // 如果至少召唤了一个，认为成功
                }
            }

            // 创建Wraithling
            Unit wraithling = BasicObjectBuilders.loadUnit(StaticConfFiles.wraithling, generateUnitId(), Unit.class);
            wraithling.setPositionByTile(currentTile);
            wraithling.setOwner(player);
            wraithling.setCombatStats(1, 1); // Wraithling 是1/1单位

            // 播放召唤效果
            EffectAnimation summonEffect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_wraithsummon);
            BasicCommands.playEffectAnimation(out, summonEffect, currentTile);
            try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

            // 显示新单位
            BasicCommands.drawUnit(out, wraithling, currentTile);
            BasicCommands.setUnitAttack(out, wraithling, wraithling.getAttackValue());
            BasicCommands.setUnitHealth(out, wraithling, wraithling.getHealth());

            // 添加到游戏状态
            gameState.addUnit(wraithling, currentTile);

            // 标记为本回合召唤
            wraithling.setSummonedThisTurn(true);

            // 寻找下一个可用位置
            if (i < 2) { // 只在前两次召唤后寻找
                Tile nextTile = findNearbyEmptyTile(currentTile);
                if (nextTile == null) {
                    // 如果没有找到，结束召唤
                    return true;
                }
                currentTile = nextTile;
            }
        }

        return true;
    }

    /**
     * 施放被弃之角法术
     * 给头像装备神器
     */
    private boolean castHornOfTheForsaken(Tile targetTile, Player player) {
        // 获取目标单位（应该是头像）
        Unit targetUnit = gameState.getUnitAtTile(targetTile);
        if (targetUnit == null || targetUnit.getUnitType() != Unit.UnitType.AVATAR) {
            return false;
        }

        // 播放装备效果动画
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, effect, targetTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 创建Horn of the Forsaken神器
        Unit.Artifact artifact = new Unit.Artifact(
                "Horn of the Forsaken",
                3, // 耐久度
                new Unit.Artifact.ArtifactEffect() {
                    @Override
                    public void onTrigger(Unit unit, Unit.GameEvent event) {
                        // 当攻击成功时，召唤一个Wraithling
                        if (event == Unit.GameEvent.ON_ATTACK) {
                            // 在实际实现中，需要查找周围的空格子并召唤Wraithling
                            // 这里简化处理
                        }
                    }
                }
        );

        // 装备到头像
        targetUnit.equipArtifact(artifact);

        return true;
    }

    /**
     * 寻找附近的空格子
     * @param tile 中心格子
     * @return 找到的空格子，如果没有则返回null
     */
    private Tile findNearbyEmptyTile(Tile tile) {
        int centerX = tile.getTilex();
        int centerY = tile.getTiley();

        // 检查周围8个方向
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // 跳过中心

                int x = centerX + dx;
                int y = centerY + dy;

                if (gameState.isValidPosition(x, y)) {
                    Tile nearbyTile = gameState.getTile(x, y);
                    if (nearbyTile != null && gameState.getUnitAtTile(nearbyTile) == null) {
                        return nearbyTile;
                    }
                }
            }
        }

        return null; // 没有找到空格子
    }

    /**
     * 生成唯一的单位ID
     */
    private int generateUnitId() {
        // 简单实现，使用当前时间戳的最后4位
        return (int)(System.currentTimeMillis() % 10000);
    }

    /**
     * 清除先前的选择
     */
    private void clearPreviousSelections() {
        // 清除先前选择的单位
        Unit previousUnit = gameState.getSelectedUnit();
        if (previousUnit != null) {
            gameState.setSelectedUnit(null);
        }

        // 清除先前选择的卡牌
        Card previousCard = gameState.getSelectedCard();
        if (previousCard != null) {
            // 取消高亮显示
            int position = previousCard.getHandPosition();
            if (position > 0) {
                BasicCommands.drawCard(out, previousCard, position, 0); // 0表示不高亮
            }
            gameState.setSelectedCard(null);
        }

        // 清除所有格子高亮
        clearTileHighlights();
    }

    /**
     * 清除所有格子的高亮
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
}