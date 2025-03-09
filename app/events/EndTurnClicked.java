package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
import structures.basic.Unit;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case
 * the end-turn button.
 *
 * {
 *   messageType = "endTurnClicked"
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class EndTurnClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		// 检查游戏是否已初始化
		if (!gameState.isGameInitialized()) {
			return;
		}

		// 检查是否是游戏结束状态
		if (gameState.isGameEnded()) {
			return;
		}

		// 检查是否是玩家回合
		if (gameState.getCurrentPhase() != GameState.GamePhase.PLAYER_TURN) {
			BasicCommands.addPlayer1Notification(out, "不是您的回合", 2);
			return;
		}

		// 清除所有选择和高亮
		clearSelections(out, gameState);

		// 处理玩家回合结束
		handlePlayerTurnEnd(out, gameState);

		// 检查游戏是否结束
		if (checkGameOver(out, gameState)) {
			return;
		}

		// 转换到AI回合
		startAITurn(out, gameState);

		// AI执行动作
		performAIActions(out, gameState);

		// 处理AI回合结束
		handleAITurnEnd(out, gameState);

		// 再次检查游戏是否结束
		if (checkGameOver(out, gameState)) {
			return;
		}

		// 开始玩家的新回合
		startPlayerTurn(out, gameState);
	}

	/**
	 * 处理玩家回合结束
	 */
	private void handlePlayerTurnEnd(ActorRef out, GameState gameState) {
		BasicCommands.addPlayer1Notification(out, "回合结束", 1);

		// 获取人类玩家
		Player humanPlayer = gameState.getHumanPlayer();
		if (humanPlayer != null) {
			// 触发回合结束效果
			humanPlayer.endTurn();
		}

		// 更新游戏状态
		gameState.endTurn();
	}

	/**
	 * 开始AI回合
	 */
	private void startAITurn(ActorRef out, GameState gameState) {
		// 切换到AI回合
		Player aiPlayer = gameState.startNewTurn();

		// 确保正确获取了AI玩家
		if (aiPlayer != gameState.getAiPlayer()) {
			return;
		}

		// 显示AI回合通知
		BasicCommands.addPlayer1Notification(out, "AI回合", 1);

		// 更新AI玩家的法力值显示
		BasicCommands.setPlayer2Mana(out, aiPlayer);

		// 给AI一点思考时间
		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
	}

	/**
	 * 执行AI动作
	 */
	private void performAIActions(ActorRef out, GameState gameState) {
		// 获取AI玩家
		Player aiPlayer = gameState.getAiPlayer();

		// 这里应该有AI决策逻辑
		// 在冲刺周期二，我们可以实现一个简单的AI
		// 在后续冲刺中可以增强AI能力

		// 示例：AI单位移动和攻击
		for (Unit unit : gameState.getAllUnits()) {
			// 只处理AI的单位
			if (unit.getOwner() != aiPlayer) {
				continue;
			}

			// 简单的AI逻辑：如果单位可以攻击玩家的单位，就攻击
			tryAIUnitAttack(out, gameState, unit);

			// 如果单位还可以移动，尽量向玩家单位靠近
			tryAIUnitMove(out, gameState, unit);
		}

		// 示例：AI使用卡牌
		tryAIPlayCards(out, gameState, aiPlayer);

		// 给AI动作一些执行时间
		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
	}

	/**
	 * 尝试AI单位攻击
	 */
	private void tryAIUnitAttack(ActorRef out, GameState gameState, Unit unit) {
		// 这里应该有AI单位攻击逻辑
		// 简单实现：找到范围内最接近的玩家单位进行攻击

		// 在实际实现中，应该有更复杂的目标选择逻辑
		// 这里只是一个占位实现
	}

	/**
	 * 尝试AI单位移动
	 */
	private void tryAIUnitMove(ActorRef out, GameState gameState, Unit unit) {
		// 这里应该有AI单位移动逻辑
		// 简单实现：向最近的玩家单位移动

		// 在实际实现中，应该有更复杂的路径规划逻辑
		// 这里只是一个占位实现
	}

	/**
	 * 尝试AI使用卡牌
	 */
	private void tryAIPlayCards(ActorRef out, GameState gameState, Player aiPlayer) {
		// 这里应该有AI使用卡牌的逻辑
		// 简单实现：尽可能使用手中的卡牌

		// 在实际实现中，应该有更复杂的卡牌选择和目标选择逻辑
		// 这里只是一个占位实现
	}

	/**
	 * 处理AI回合结束
	 */
	private void handleAITurnEnd(ActorRef out, GameState gameState) {
		// 获取AI玩家
		Player aiPlayer = gameState.getAiPlayer();

		if (aiPlayer != null) {
			// 触发回合结束效果
			aiPlayer.endTurn();
		}

		// 更新游戏状态
		gameState.endTurn();

		// 通知AI回合结束
		BasicCommands.addPlayer1Notification(out, "AI回合结束", 1);
		try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
	}

	/**
	 * 开始玩家新回合
	 */
	private void startPlayerTurn(ActorRef out, GameState gameState) {
		// 开始新回合
		Player player = gameState.startNewTurn();

		// 确保正确获取了人类玩家
		if (player != gameState.getHumanPlayer()) {
			return;
		}

		// 显示玩家回合通知
		BasicCommands.addPlayer1Notification(out, "你的回合", 1);

		// 更新玩家法力值显示
		BasicCommands.setPlayer1Mana(out, player);

		// 绘制新抽的卡牌
		if (player.getHand() != null) {
			int handSize = player.getHand().size();
			if (handSize > 0) {
				// 假设最后一张卡是新抽的
				java.util.List<structures.basic.Card> cards = player.getHand().getCards();
				if (!cards.isEmpty()) {
					structures.basic.Card newCard = cards.get(cards.size() - 1);
					BasicCommands.drawCard(out, newCard, handSize, 0);
				}
			}
		}
	}

	/**
	 * 检查游戏是否结束
	 * @return 如果游戏结束返回true
	 */
	private boolean checkGameOver(ActorRef out, GameState gameState) {
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
	 * 清除所有选择和高亮
	 */
	private void clearSelections(ActorRef out, GameState gameState) {
		// 清除单位选择
		gameState.setSelectedUnit(null);

		// 清除卡牌选择
		gameState.setSelectedCard(null);

		// 清除所有高亮
		clearTileHighlights(out, gameState);
	}

	/**
	 * 清除所有格子的高亮
	 */
	private void clearTileHighlights(ActorRef out, GameState gameState) {
		for (int x = 0; x < gameState.getBoardWidth(); x++) {
			for (int y = 0; y < gameState.getBoardHeight(); y++) {
				structures.basic.Tile tile = gameState.getTile(x, y);
				if (tile != null) {
					BasicCommands.drawTile(out, tile, 0); // 0表示不高亮
				}
			}
		}
	}
}