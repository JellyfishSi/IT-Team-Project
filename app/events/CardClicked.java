package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.hand.Hand;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a card.
 * The event returns the position in the player's hand the card resides within.
 *
 * {
 *   messageType = "cardClicked"
 *   position = <hand index position [1-6]>
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class CardClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {

		// 获取点击的卡牌在手牌中的位置（1-6）
		int handPosition = message.get("position").asInt();

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

		// 获取人类玩家
		Player humanPlayer = gameState.getHumanPlayer();
		if (humanPlayer == null || humanPlayer.getHand() == null) {
			return;
		}

		// 清除先前的选择
		clearPreviousSelections(out, gameState);

		// 获取点击的卡牌
		Hand hand = humanPlayer.getHand();
		Card clickedCard = hand.getCardByGamePosition(handPosition);

		if (clickedCard == null) {
			return; // 该位置没有卡牌
		}

		// 检查法力值是否足够
		if (humanPlayer.getMana() < clickedCard.getManacost()) {
			BasicCommands.addPlayer1Notification(out, "法力值不足", 2);
			return;
		}

		// 设置为当前选择的卡牌
		gameState.setSelectedCard(clickedCard);

		// 设置卡牌的手牌位置（方便后续使用）
		clickedCard.setHandPosition(handPosition);

		// 高亮显示卡牌
		BasicCommands.drawCard(out, clickedCard, handPosition, 1); // 1表示高亮

		// 高亮显示有效的目标位置
		highlightValidTargets(out, gameState, clickedCard);
	}

	/**
	 * 高亮显示卡牌的有效目标位置
	 */
	private void highlightValidTargets(ActorRef out, GameState gameState, Card card) {
		// 获取卡牌的所有有效目标位置
		java.util.List<Tile> validTargets = card.getValidTargets(gameState);

		// 高亮显示这些位置
		for (Tile tile : validTargets) {
			// 对于生物卡，使用普通高亮
			if (card.isCreature()) {
				BasicCommands.drawTile(out, tile, 1); // 1表示高亮
			}
			// 对于法术卡，根据目标类型使用不同的高亮
			else {
				// 如果目标是敌方单位，使用红色高亮
				if (gameState.getUnitAtTile(tile) != null &&
						gameState.getUnitAtTile(tile).getOwner() != card.getOwner()) {
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
	 * 清除先前的选择
	 */
	private void clearPreviousSelections(ActorRef out, GameState gameState) {
		// 清除先前选择的单位
		if (gameState.getSelectedUnit() != null) {
			gameState.setSelectedUnit(null);
		}

		// 清除先前选择的卡牌
		Card previousCard = gameState.getSelectedCard();
		if (previousCard != null) {
			BasicCommands.drawCard(out, previousCard, previousCard.getHandPosition(), 0); // 0表示不高亮
			gameState.setSelectedCard(null);
		}

		// 清除所有格子高亮
		clearTileHighlights(out, gameState);
	}

	/**
	 * 清除所有格子的高亮
	 */
	private void clearTileHighlights(ActorRef out, GameState gameState) {
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