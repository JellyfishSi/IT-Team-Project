package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * Indicates that the user has clicked an object on the game canvas, in this case a tile.
 * The event returns the x (horizontal) and y (vertical) indices of the tile that was
 * clicked. Tile indices start at 1.
 *
 * {
 *   messageType = "tileClicked"
 *   tilex = <x index of the tile>
 *   tiley = <y index of the tile>
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class TileClicked implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// 获取被点击的棋盘格子坐标
		int tilex = message.get("tilex").asInt();
		int tiley = message.get("tiley").asInt();

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

		// 获取点击的格子
		Tile clickedTile = gameState.getTile(tilex-1, tiley-1); // 转换为0-based索引
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
			handleUnitAction(out, gameState, selectedUnit, clickedTile, unitOnTile);
		} else if (selectedCard != null) {
			// 已经选中了一张卡牌，尝试使用卡牌
			handleCardPlay(out, gameState, humanPlayer, selectedCard, clickedTile);
		} else if (unitOnTile != null) {
			// 点击了一个有单位的格子，尝试选中该单位
			handleUnitSelection(out, gameState, humanPlayer, unitOnTile, clickedTile);
		} else {
			// 点击了一个空格子，取消所有选择
			clearSelections(out, gameState);
		}
	}

	/**
	 * 处理单位行动（移动或攻击）
	 */
	private void handleUnitAction(ActorRef out, GameState gameState, Unit selectedUnit, Tile clickedTile, Unit unitOnTile) {
		// 检查单位所属玩家
		if (selectedUnit.getOwner() != gameState.getHumanPlayer()) {
			clearSelections(out, gameState);
			return;
		}

		// 如果点击的格子上有单位
		if (unitOnTile != null) {
			// 如果是敌方单位，尝试攻击
			if (unitOnTile.getOwner() != selectedUnit.getOwner()) {
				if (gameState.canAttack(selectedUnit, unitOnTile)) {
					// 播放攻击动画
					BasicCommands.playUnitAnimation(out, selectedUnit, structures.basic.UnitAnimationType.attack);
					try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

					// 执行攻击
					gameState.attackUnit(selectedUnit, unitOnTile);

					// 更新UI显示
					BasicCommands.setUnitHealth(out, unitOnTile, unitOnTile.getHealth());
					BasicCommands.setUnitHealth(out, selectedUnit, selectedUnit.getHealth());

					// 检查单位是否死亡
					if (unitOnTile.isDead()) {
						BasicCommands.playUnitAnimation(out, unitOnTile, structures.basic.UnitAnimationType.death);
						try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
						BasicCommands.deleteUnit(out, unitOnTile);
					}

					if (selectedUnit.isDead()) {
						BasicCommands.playUnitAnimation(out, selectedUnit, structures.basic.UnitAnimationType.death);
						try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
						BasicCommands.deleteUnit(out, selectedUnit);
					}

					// 清除选择
					clearSelections(out, gameState);
				} else {
					BasicCommands.addPlayer1Notification(out, "无法攻击该单位", 2);
				}
			} else {
				// 如果是友方单位，切换选中的单位
				clearTileHighlights(out, gameState);
				gameState.setSelectedUnit(unitOnTile);
				highlightValidMoves(out, gameState, unitOnTile);
			}
		} else {
			// 如果点击的是空格子，尝试移动
			if (gameState.isValidMove(selectedUnit, clickedTile)) {
				// 移动单位
				BasicCommands.moveUnitToTile(out, selectedUnit, clickedTile);
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}

				// 更新游戏状态
				gameState.moveUnit(selectedUnit, clickedTile);

				// 清除高亮和选择
				clearSelections(out, gameState);
			} else {
				BasicCommands.addPlayer1Notification(out, "无法移动到该位置", 2);
			}
		}
	}

	/**
	 * 处理卡牌使用
	 */
	private void handleCardPlay(ActorRef out, GameState gameState, Player player, Card selectedCard, Tile clickedTile) {
		// 检查卡牌是否可以在该位置使用
		if (selectedCard.isValidTarget(gameState, clickedTile)) {
			// 尝试使用卡牌
			if (player.useCard(selectedCard, clickedTile, gameState)) {
				// 卡牌使用成功，更新UI
				int handPosition = selectedCard.getHandPosition();
				if (handPosition > 0) {
					BasicCommands.deleteCard(out, handPosition);
				}

				// 更新玩家法力值显示
				BasicCommands.setPlayer1Mana(out, player);

				// 如果是生物卡，处理召唤
				if (selectedCard.isCreature()) {
					// 生物单位的创建和放置已在Player.useCard中处理
					// 这里可以添加额外的UI效果
				} else {
					// 如果是法术卡，可以播放特效
					// 例如：BasicCommands.playEffectAnimation(out, effect, clickedTile);
				}

				// 清除选择
				clearSelections(out, gameState);
			} else {
				BasicCommands.addPlayer1Notification(out, "无法使用该卡牌", 2);
			}
		} else {
			BasicCommands.addPlayer1Notification(out, "无效的目标", 2);
		}
	}

	/**
	 * 处理单位选择
	 */
	private void handleUnitSelection(ActorRef out, GameState gameState, Player player, Unit unitOnTile, Tile clickedTile) {
		// 如果单位是玩家的，选中该单位
		if (unitOnTile.getOwner() == player) {
			// 检查单位是否可以行动
			if (unitOnTile.canMove() || unitOnTile.canAttack()) {
				// 设置为选中的单位
				gameState.setSelectedUnit(unitOnTile);

				// 高亮显示这个格子
				BasicCommands.drawTile(out, clickedTile, 1); // 1表示高亮

				// 高亮显示可移动的位置
				highlightValidMoves(out, gameState, unitOnTile);
			} else {
				BasicCommands.addPlayer1Notification(out, "该单位已经行动过", 2);
			}
		} else {
			// 如果是敌方单位，显示单位信息
			// 可以添加显示敌方单位详细信息的代码
			clearSelections(out, gameState);
		}
	}

	/**
	 * 高亮显示单位的有效移动位置
	 */
	private void highlightValidMoves(ActorRef out, GameState gameState, Unit unit) {
		// 获取单位可以移动到的所有位置
		java.util.List<Tile> validMoves = gameState.getValidMoves(unit);

		// 高亮显示这些位置
		for (Tile tile : validMoves) {
			BasicCommands.drawTile(out, tile, 1); // 1表示高亮
		}
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
}