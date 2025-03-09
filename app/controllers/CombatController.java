package controllers;

import java.util.List;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.CombatUtility;
import utils.StaticConfFiles;

/**
 * 战斗控制器类，负责处理与战斗相关的逻辑
 * 包括单位攻击、战斗规则验证和战斗效果处理
 *
 * @author Your Team
 */
public class CombatController {

    private ActorRef out;
    private GameState gameState;

    /**
     * 构造函数
     */
    public CombatController() {
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
     * 执行单位攻击
     * @param attacker 攻击单位
     * @param defender 防御单位
     * @return 是否成功攻击
     */
    public boolean attackUnit(Unit attacker, Unit defender) {
        // 检查是否可以攻击
        if (!CombatUtility.canAttack(attacker, defender, gameState)) {
            BasicCommands.addPlayer1Notification(out, "无法攻击该单位", 2);
            return false;
        }

        // 播放攻击动画
        BasicCommands.playUnitAnimation(out, attacker, structures.basic.UnitAnimationType.attack);
        try {Thread.sleep(1500);} catch (InterruptedException e) {e.printStackTrace();}

        // 计算和应用伤害
        int attackDamage = CombatUtility.calculateDamage(attacker, defender);
        int originalHealth = defender.getHealth();
        defender.takeDamage(attackDamage);

        // 更新防御单位生命值显示
        BasicCommands.setUnitHealth(out, defender, defender.getHealth());

        // 标记攻击者已攻击
        attacker.onUnitAttacked();

        // 处理防御单位的反击（如果未死亡且在攻击范围内）
        handleCounterAttack(attacker, defender);

        // 处理死亡效果
        handleDeathEffects(attacker, defender);

        return true;
    }

    /**
     * 处理反击
     * @param attacker 攻击单位
     * @param defender 防御单位
     */
    private void handleCounterAttack(Unit attacker, Unit defender) {
        // 如果防御单位已死亡，不进行反击
        if (defender.isDead()) {
            return;
        }

        // 如果防御单位不在攻击范围内，不进行反击
        if (!CombatUtility.isInAttackRange(defender, attacker)) {
            return;
        }

        // 播放防御单位的攻击动画
        BasicCommands.playUnitAnimation(out, defender, structures.basic.UnitAnimationType.attack);
        try {Thread.sleep(1500);} catch (InterruptedException e) {e.printStackTrace();}

        // 计算和应用反击伤害
        int counterDamage = CombatUtility.calculateDamage(defender, attacker);
        attacker.takeDamage(counterDamage);

        // 更新攻击单位生命值显示
        BasicCommands.setUnitHealth(out, attacker, attacker.getHealth());
    }

    /**
     * 处理单位死亡效果
     * @param attacker 攻击单位
     * @param defender 防御单位
     */
    private void handleDeathEffects(Unit attacker, Unit defender) {
        boolean attackerDied = false;
        boolean defenderDied = false;

        // 检查防御单位是否死亡
        if (defender.isDead()) {
            defenderDied = true;
            // 播放死亡动画
            BasicCommands.playUnitAnimation(out, defender, structures.basic.UnitAnimationType.death);
            try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

            // 触发死亡相关效果
            triggerDeathEffects(defender);

            // 移除单位
            BasicCommands.deleteUnit(out, defender);
            gameState.removeUnit(defender);
        }

        // 检查攻击单位是否死亡
        if (attacker.isDead()) {
            attackerDied = true;
            // 播放死亡动画
            BasicCommands.playUnitAnimation(out, attacker, structures.basic.UnitAnimationType.death);
            try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

            // 触发死亡相关效果
            triggerDeathEffects(attacker);

            // 移除单位
            BasicCommands.deleteUnit(out, attacker);
            gameState.removeUnit(attacker);
        }

        // 如果有单位死亡，检查游戏是否结束
        if (attackerDied || defenderDied) {
            checkGameOver();
        }
    }

    /**
     * 触发单位死亡效果
     * @param deadUnit 死亡的单位
     */
    private void triggerDeathEffects(Unit deadUnit) {
        // 让单位处理自己的死亡
        deadUnit.onDeath(gameState);

        // 触发场上所有单位的死亡监视效果
        for (Unit unit : gameState.getAllUnits()) {
            if (unit != deadUnit && !unit.isDead() && unit.hasAbility("Deathwatch")) {
                // 播放效果动画
                Tile unitTile = gameState.getTile(unit.getPosition().getTilex(), unit.getPosition().getTiley());
                EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
                BasicCommands.playEffectAnimation(out, effect, unitTile);
                try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

                // 触发死亡监视能力
                unit.triggerAbility("Deathwatch");

                // 更新单位显示
                BasicCommands.setUnitAttack(out, unit, unit.getAttackValue());
                BasicCommands.setUnitHealth(out, unit, unit.getHealth());
            }
        }
    }

    /**
     * 应用范围伤害
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param radius 影响半径
     * @param damage 伤害值
     * @param sourcePlayer 伤害来源玩家
     * @param affectsOwnUnits 是否影响自己的单位
     * @return 受到伤害的单位数
     */
    public int applyAreaDamage(int centerX, int centerY, int radius, int damage,
                               Player sourcePlayer, boolean affectsOwnUnits) {
        // 获取范围内的所有单位
        List<Unit> affectedUnits = CombatUtility.applyAOEDamage(
                centerX, centerY, radius, damage, affectsOwnUnits, sourcePlayer, gameState
        );

        // 播放范围效果动画
        Tile centerTile = gameState.getTile(centerX, centerY);
        if (centerTile != null) {
            EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
            BasicCommands.playEffectAnimation(out, effect, centerTile);
            try {Thread.sleep(1500);} catch (InterruptedException e) {e.printStackTrace();}
        }

        // 更新受影响单位的UI
        for (Unit unit : affectedUnits) {
            // 如果单位还活着，更新生命值显示
            if (!unit.isDead()) {
                BasicCommands.setUnitHealth(out, unit, unit.getHealth());
            }
        }

        // 检查游戏是否结束
        checkGameOver();

        return affectedUnits.size();
    }

    /**
     * 治疗单位
     * @param targetUnit 目标单位
     * @param healAmount 治疗量
     * @return 实际恢复的生命值
     */
    public int healUnit(Unit targetUnit, int healAmount) {
        if (targetUnit == null || healAmount <= 0) {
            return 0;
        }

        // 播放治疗效果动画
        Tile unitTile = gameState.getTile(targetUnit.getPosition().getTilex(), targetUnit.getPosition().getTiley());
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, effect, unitTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 应用治疗
        int healedAmount = targetUnit.heal(healAmount);

        // 更新单位生命值显示
        BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());

        // 如果是头像单位，还需要更新玩家生命值显示
        Player owner = targetUnit.getOwner();
        if (targetUnit.getUnitType() == Unit.UnitType.AVATAR && owner != null) {
            if (owner.isHuman()) {
                BasicCommands.setPlayer1Health(out, owner);
            } else {
                BasicCommands.setPlayer2Health(out, owner);
            }
        }

        return healedAmount;
    }

    /**
     * 增加单位属性
     * @param targetUnit 目标单位
     * @param attackBuff 攻击力增加值
     * @param healthBuff 生命值增加值
     */
    public void buffUnit(Unit targetUnit, int attackBuff, int healthBuff) {
        if (targetUnit == null) {
            return;
        }

        // 播放增益效果动画
        Tile unitTile = gameState.getTile(targetUnit.getPosition().getTilex(), targetUnit.getPosition().getTiley());
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_buff);
        BasicCommands.playEffectAnimation(out, effect, unitTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 应用属性增加
        if (attackBuff > 0) {
            targetUnit.buffAttack(attackBuff);
        }

        if (healthBuff > 0) {
            targetUnit.buffHealth(healthBuff);
        }

        // 更新单位显示
        BasicCommands.setUnitAttack(out, targetUnit, targetUnit.getAttackValue());
        BasicCommands.setUnitHealth(out, targetUnit, targetUnit.getHealth());
    }

    /**
     * 施加状态效果（如晕眩）
     * @param targetUnit 目标单位
     * @param status 状态类型
     * @param value 状态值
     * @return 是否成功施加
     */
    public boolean applyStatusEffect(Unit targetUnit, Unit.UnitStatus status, boolean value) {
        if (targetUnit == null) {
            return false;
        }

        // 播放相应效果动画
        Tile unitTile = gameState.getTile(targetUnit.getPosition().getTilex(), targetUnit.getPosition().getTiley());
        EffectAnimation effect = BasicObjectBuilders.loadEffect(StaticConfFiles.f1_inmolation);
        BasicCommands.playEffectAnimation(out, effect, unitTile);
        try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

        // 设置状态
        targetUnit.setStatus(status, value);

        return true;
    }

    /**
     * 消灭单位（无论当前生命值）
     * @param targetUnit 目标单位
     * @return 是否成功消灭
     */
    public boolean destroyUnit(Unit targetUnit) {
        if (targetUnit == null || targetUnit.getUnitType() == Unit.UnitType.AVATAR) {
            // 不能直接消灭头像单位
            return false;
        }

        // 播放消灭效果动画
        Tile unitTile = gameState.getTile(targetUnit.getPosition().getTilex(), targetUnit.getPosition().getTiley());
        EffectAnimation effect = BasicObjectBuilders.loadEffect("conf/gameconfs/f1_soulshatter.json");
        BasicCommands.playEffectAnimation(out, effect, unitTile);
        try {Thread.sleep(1500);} catch (InterruptedException e) {e.printStackTrace();}

        // 播放死亡动画
        BasicCommands.playUnitAnimation(out, targetUnit, structures.basic.UnitAnimationType.death);
        try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}

        // 触发死亡相关效果
        triggerDeathEffects(targetUnit);

        // 移除单位
        BasicCommands.deleteUnit(out, targetUnit);
        gameState.removeUnit(targetUnit);

        return true;
    }

    /**
     * 检查游戏是否结束
     * @return 如果游戏结束返回true
     */
    public boolean checkGameOver() {
        Player winner = CombatUtility.checkWinCondition(gameState);
        if (winner != null) {
            // 游戏结束，进入游戏结束状态
            gameState.setCurrentPhase(GameState.GamePhase.GAME_OVER);

            // 显示胜利/失败消息
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
     * 获取单位的有效攻击目标
     * @param unit 单位
     * @return 可攻击目标列表
     */
    public List<Unit> getValidAttackTargets(Unit unit) {
        return CombatUtility.getValidAttackTargets(unit, gameState);
    }

    /**
     * 高亮显示单位的有效攻击目标
     * @param unit 单位
     */
    public void highlightValidAttackTargets(Unit unit) {
        List<Unit> targets = getValidAttackTargets(unit);

        for (Unit target : targets) {
            // 获取目标所在的格子
            Tile targetTile = gameState.getTile(
                    target.getPosition().getTilex(),
                    target.getPosition().getTiley()
            );

            if (targetTile != null) {
                // 使用红色高亮显示可攻击目标
                BasicCommands.drawTile(out, targetTile, 2); // 2表示红色高亮
            }
        }
    }

}