package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;
import structures.basic.Player;
import structures.basic.Unit;

/**
 * Indicates that a unit instance has stopped moving.
 * The event reports the unique id of the unit.
 *
 * {
 *   messageType = "unitStopped"
 *   id = <unit id>
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class UnitStopped implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		// 获取停止移动的单位ID
		int unitId = message.get("id").asInt();

		// 检查游戏是否已初始化
		if (!gameState.isGameInitialized()) {
			return;
		}

		// 查找对应的单位
		Unit stoppedUnit = findUnitById(gameState, unitId);

		if (stoppedUnit != null) {
			// 设置单位为非移动状态
			gameState.setUnitMoving(stoppedUnit, false);

			// 清除正在移动的单位记录
			if (gameState.getUnitInMotion() == stoppedUnit) {
				gameState.clearUnitInMotion();
			}

			// 计算移动耗时（可用于某些游戏机制）
			long moveDuration = System.currentTimeMillis() - gameState.getUnitMoveStartTime();

			// 处理移动后的效果
			handlePostMoveEffects(gameState, stoppedUnit, moveDuration);
		}
	}

	/**
	 * 通过ID查找单位
	 * @param gameState 游戏状态
	 * @param unitId 单位ID
	 * @return 找到的单位，如果未找到则返回null
	 */
	private Unit findUnitById(GameState gameState, int unitId) {
		for (Unit unit : gameState.getAllUnits()) {
			if (unit.getId() == unitId) {
				return unit;
			}
		}
		return null;
	}

	/**
	 * 处理单位移动后的效果
	 * @param gameState 游戏状态
	 * @param unit 刚完成移动的单位
	 * @param moveDuration 移动耗时（毫秒）
	 */
	private void handlePostMoveEffects(GameState gameState, Unit unit, long moveDuration) {
		// 如果是玩家的单位，标记为已移动
		Player humanPlayer = gameState.getHumanPlayer();
		if (unit.getOwner() == humanPlayer) {
			unit.onUnitMoved();
		}

		// 检查单位的新位置是否触发特殊效果
		// 例如，某些位置可能有特殊效果，如恢复生命、造成伤害等
		checkPositionEffects(gameState, unit);

		// 检查单位移动是否触发了其他单位的能力
		// 例如，某些单位可能有"当敌方单位移动时"的触发效果
		checkMovementTriggeredAbilities(gameState, unit);
	}

	/**
	 * 检查单位位置的特殊效果
	 * @param gameState 游戏状态
	 * @param unit 单位
	 */
	private void checkPositionEffects(GameState gameState, Unit unit) {
		// 这里可以实现位置特殊效果的逻辑
		// 在当前游戏中可能不需要，但为了扩展性添加了此方法
	}

	/**
	 * 检查单位移动触发的其他单位能力
	 * @param gameState 游戏状态
	 * @param movedUnit 移动的单位
	 */
	private void checkMovementTriggeredAbilities(GameState gameState, Unit movedUnit) {
		// 遍历所有单位，检查是否有单位的能力被移动触发
		for (Unit unit : gameState.getAllUnits()) {
			// 如果单位有"敌方单位移动时"的触发能力
			// 这里需要根据实际的游戏能力系统来实现

			// 示例：如果有单位拥有"反应"能力（当敌方单位移动到附近时可以攻击）
			if (unit.hasAbility("Provoke") && unit.getOwner() != movedUnit.getOwner()) {
				// 检查移动的单位是否进入了嘲讽范围
				if (isAdjacent(unit, movedUnit)) {
					// 设置该单位被嘲讽
					movedUnit.setStatus(Unit.UnitStatus.PROVOKED, true);
				}
			}
		}
	}

	/**
	 * 检查两个单位是否相邻
	 * @param unit1 第一个单位
	 * @param unit2 第二个单位
	 * @return 如果相邻则返回true
	 */
	private boolean isAdjacent(Unit unit1, Unit unit2) {
		int x1 = unit1.getPosition().getTilex();
		int y1 = unit1.getPosition().getTiley();
		int x2 = unit2.getPosition().getTilex();
		int y2 = unit2.getPosition().getTiley();

		// 检查是否在相邻的8个方向之一
		return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1 && !(x1 == x2 && y1 == y2);
	}
}