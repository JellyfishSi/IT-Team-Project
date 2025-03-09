package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import structures.GameState;

/**
 * Indicates that a unit instance has started a move. 
 * The event reports the unique id of the unit.
 *
 * {
 *   messageType = "unitMoving"
 *   id = <unit id>
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class UnitMoving implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		// 获取正在移动的单位ID
		int unitId = message.get("id").asInt();

		// 检查游戏是否已初始化
		if (!gameState.isGameInitialized()) {
			return;
		}

		// 查找对应的单位
		structures.basic.Unit movingUnit = findUnitById(gameState, unitId);

		if (movingUnit != null) {
			// 设置单位为移动状态
			gameState.setUnitMoving(movingUnit, true);

			// 在GameState中记录这个单位正在移动
			// 这可以用于防止在单位移动过程中进行其他操作
			gameState.setUnitInMotion(movingUnit);

			// 记录单位开始移动的时间（用于计算移动时长）
			// 这在某些游戏机制中可能有用
			gameState.setUnitMoveStartTime(System.currentTimeMillis());
		}
	}

	/**
	 * 通过ID查找单位
	 * @param gameState 游戏状态
	 * @param unitId 单位ID
	 * @return 找到的单位，如果未找到则返回null
	 */
	private structures.basic.Unit findUnitById(GameState gameState, int unitId) {
		for (structures.basic.Unit unit : gameState.getAllUnits()) {
			if (unit.getId() == unitId) {
				return unit;
			}
		}
		return null;
	}
}