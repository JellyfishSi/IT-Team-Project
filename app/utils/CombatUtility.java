package utils;

import java.util.ArrayList;
import java.util.List;

import structures.GameState;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.basic.Player;

/**
 * 战斗工具类，处理与单位战斗相关的逻辑
 * 包括伤害计算、战斗规则验证和效果触发
 *
 * @author Your Team
 */
public class CombatUtility {

    /**
     * 检查一个单位是否可以攻击另一个单位
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @param gameState 当前游戏状态
     * @return 如果可以攻击则返回true
     */
    public static boolean canAttack(Unit attacker, Unit defender, GameState gameState) {
        // 检查攻击者是否可以攻击
        if (!attacker.canAttack()) {
            return false;
        }

        // 检查防御者是否是敌方单位
        if (attacker.getOwner() == defender.getOwner()) {
            return false;
        }

        // 检查是否在攻击范围内
        if (!isInAttackRange(attacker, defender)) {
            return false;
        }

        // 检查嘲讽效果
        if (isAffectedByProvoke(attacker, gameState) && !hasProvoke(defender)) {
            return false;
        }

        return true;
    }

    /**
     * 检查单位是否在攻击范围内
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @return 如果在攻击范围内则返回true
     */
    public static boolean isInAttackRange(Unit attacker, Unit defender) {
        Position attackerPos = attacker.getPosition();
        Position defenderPos = defender.getPosition();

        int dx = Math.abs(attackerPos.getTilex() - defenderPos.getTilex());
        int dy = Math.abs(attackerPos.getTiley() - defenderPos.getTiley());

        // 默认攻击范围是相邻格子(包括斜角)
        return dx <= 1 && dy <= 1;
    }

    /**
     * 检查单位是否受到嘲讽影响
     * @param unit 要检查的单位
     * @param gameState 当前游戏状态
     * @return 如果受到嘲讽影响则返回true
     */
    public static boolean isAffectedByProvoke(Unit unit, GameState gameState) {
        Position pos = unit.getPosition();
        int tilex = pos.getTilex();
        int tiley = pos.getTiley();

        // 检查相邻位置是否有带嘲讽的敌方单位
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // 跳过自身

                int adjacentX = tilex + dx;
                int adjacentY = tiley + dy;

                if (gameState.isValidPosition(adjacentX, adjacentY)) {
                    Tile adjacentTile = gameState.getTile(adjacentX, adjacentY);
                    Unit adjacentUnit = gameState.getUnitAtTile(adjacentTile);

                    if (adjacentUnit != null &&
                            adjacentUnit.getOwner() != unit.getOwner() &&
                            adjacentUnit.hasAbility("Provoke")) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 检查单位是否具有嘲讽能力
     * @param unit 要检查的单位
     * @return 如果有嘲讽能力则返回true
     */
    public static boolean hasProvoke(Unit unit) {
        return unit.hasAbility("Provoke");
    }

    /**
     * 执行攻击操作
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @param gameState 当前游戏状态
     * @return 如果攻击成功则返回true
     */
    public static boolean performAttack(Unit attacker, Unit defender, GameState gameState) {
        // 再次检查是否可以攻击
        if (!canAttack(attacker, defender, gameState)) {
            return false;
        }

        // 计算和应用伤害
        int attackDamage = calculateDamage(attacker, defender);
        defender.takeDamage(attackDamage);

        // 标记攻击者已攻击
        attacker.onUnitAttacked();

        // 处理反击
        handleCounterAttack(attacker, defender, gameState);

        // 处理后续效果（如果单位死亡）
        handlePostAttackEffects(attacker, defender, gameState);

        return true;
    }

    /**
     * 计算攻击伤害
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @return 计算的伤害值
     */
    public static int calculateDamage(Unit attacker, Unit defender) {
        // 基础伤害等于攻击者的攻击力
        int damage = attacker.getAttackValue();

        // 这里可以添加更复杂的伤害计算逻辑
        // 例如：考虑特殊能力、装备或状态效果等

        return damage;
    }

    /**
     * 处理反击
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @param gameState 当前游戏状态
     */
    public static void handleCounterAttack(Unit attacker, Unit defender, GameState gameState) {
        // 如果防御单位未死亡且在攻击范围内，进行反击
        if (!defender.isDead() && isInAttackRange(defender, attacker)) {
            // 计算反击伤害
            int counterDamage = calculateDamage(defender, attacker);
            attacker.takeDamage(counterDamage);
        }
    }

    /**
     * 处理攻击后续效果
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @param gameState 当前游戏状态
     */
    public static void handlePostAttackEffects(Unit attacker, Unit defender, GameState gameState) {
        // 检查单位是否死亡
        if (defender.isDead()) {
            // 触发死亡相关效果
            triggerDeathEffects(defender, gameState);

            // 移除死亡单位
            gameState.removeUnit(defender);
        }

        if (attacker.isDead()) {
            // 触发死亡相关效果
            triggerDeathEffects(attacker, gameState);

            // 移除死亡单位
            gameState.removeUnit(attacker);
        }
    }

    /**
     * 触发单位死亡效果
     * @param deadUnit 死亡的单位
     * @param gameState 当前游戏状态
     */
    public static void triggerDeathEffects(Unit deadUnit, GameState gameState) {
        // 触发单位自身的死亡效果
        deadUnit.onDeath(gameState);

        // 触发场上其他单位的死亡监视效果
        for (Unit unit : gameState.getAllUnits()) {
            if (unit != deadUnit && !unit.isDead() && unit.hasAbility("Deathwatch")) {
                unit.triggerAbility("Deathwatch");
            }
        }
    }

    /**
     * 获取单位可以攻击的所有目标
     * @param unit 攻击单位
     * @param gameState 当前游戏状态
     * @return 可攻击目标列表
     */
    public static List<Unit> getValidAttackTargets(Unit unit, GameState gameState) {
        List<Unit> validTargets = new ArrayList<>();

        // 如果单位不能攻击，返回空列表
        if (!unit.canAttack()) {
            return validTargets;
        }

        // 检查单位是否受到嘲讽影响
        boolean provokedStatus = isAffectedByProvoke(unit, gameState);

        // 遍历所有单位
        for (Unit potentialTarget : gameState.getAllUnits()) {
            // 跳过友方单位
            if (potentialTarget.getOwner() == unit.getOwner()) {
                continue;
            }

            // 检查是否在攻击范围内
            if (!isInAttackRange(unit, potentialTarget)) {
                continue;
            }

            // 如果受到嘲讽影响，只能攻击带嘲讽的单位
            if (provokedStatus && !hasProvoke(potentialTarget)) {
                continue;
            }

            validTargets.add(potentialTarget);
        }

        return validTargets;
    }

    /**
     * 计算单位的有效攻击力（考虑各种加成）
     * @param unit 要计算的单位
     * @return 有效攻击力
     */
    public static int getEffectiveAttack(Unit unit) {
        // 起始攻击力
        int attack = unit.getAttackValue();

        // 这里可以添加更多的攻击力修饰逻辑
        // 例如：来自其他单位的光环效果、临时状态等

        return Math.max(0, attack); // 攻击力不能为负数
    }

    /**
     * 应用AOE(范围效果)伤害
     * @param centerX 效果中心X坐标
     * @param centerY 效果中心Y坐标
     * @param radius 效果半径
     * @param damage 伤害值
     * @param affectsOwnUnits 是否影响自己的单位
     * @param sourcePlayerIndex 效果来源玩家索引
     * @param gameState 当前游戏状态
     * @return 受到伤害的单位列表
     */
    public static List<Unit> applyAOEDamage(int centerX, int centerY, int radius, int damage,
                                            boolean affectsOwnUnits, Player sourcePlayer,
                                            GameState gameState) {
        List<Unit> affectedUnits = new ArrayList<>();

        // 遍历所有单位
        for (Unit unit : gameState.getAllUnits()) {
            Position unitPos = unit.getPosition();
            int unitX = unitPos.getTilex();
            int unitY = unitPos.getTiley();

            // 计算到中心的距离
            double distance = Math.sqrt(Math.pow(unitX - centerX, 2) + Math.pow(unitY - centerY, 2));

            // 检查是否在范围内
            if (distance <= radius) {
                // 检查是否应该影响该单位
                if (affectsOwnUnits || unit.getOwner() != sourcePlayer) {
                    // 应用伤害
                    unit.takeDamage(damage);
                    affectedUnits.add(unit);

                    // 检查单位是否死亡
                    if (unit.isDead()) {
                        triggerDeathEffects(unit, gameState);
                        gameState.removeUnit(unit);
                    }
                }
            }
        }

        return affectedUnits;
    }

    /**
     * 处理单位治疗
     * @param targetUnit 目标单位
     * @param healAmount 治疗量
     * @return 实际恢复的生命值
     */
    public static int healUnit(Unit targetUnit, int healAmount) {
        if (targetUnit == null || healAmount <= 0) {
            return 0;
        }

        // 应用治疗
        return targetUnit.heal(healAmount);
    }

    /**
     * 获取指定类型的所有敌方单位
     * @param playerUnit 参考单位（用于确定哪些是敌方）
     * @param gameState 当前游戏状态
     * @param targetType 目标单位类型（可选）
     * @return 敌方单位列表
     */
    public static List<Unit> getEnemyUnits(Unit playerUnit, GameState gameState, Unit.UnitType targetType) {
        List<Unit> enemyUnits = new ArrayList<>();

        for (Unit unit : gameState.getAllUnits()) {
            // 检查是否是敌方单位
            if (unit.getOwner() != playerUnit.getOwner()) {
                // 如果指定了类型，检查类型是否匹配
                if (targetType == null || unit.getUnitType() == targetType) {
                    enemyUnits.add(unit);
                }
            }
        }

        return enemyUnits;
    }

    /**
     * 检查是否有任何单位处于游戏胜利条件
     * @param gameState 当前游戏状态
     * @return 赢家（如果有），否则返回null
     */
    public static Player checkWinCondition(GameState gameState) {
        // 检查每个玩家的Avatar是否存活
        Player humanPlayer = gameState.getHumanPlayer();
        Player aiPlayer = gameState.getAiPlayer();

        if (humanPlayer.getAvatar() == null || humanPlayer.getAvatar().isDead()) {
            return aiPlayer; // AI获胜
        }

        if (aiPlayer.getAvatar() == null || aiPlayer.getAvatar().isDead()) {
            return humanPlayer; // 人类获胜
        }

        return null; // 游戏继续
    }
}
