package structures;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 * 
 * @author Dr. Richard McCreadie
 *
 */

import java.util.ArrayList;
import java.util.List;

import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import structures.board.Board;

/**
 * This class holds information about the on-going game.
 * It acts as the central repository for all game state data.
 *
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {

	// 保留原有字段以保持兼容性
	public boolean gameInitalised = false;

	// 游戏阶段枚举
	public enum GamePhase {
		INITIALIZING,    // 游戏初始化阶段
		PLAYER_TURN,     // 人类玩家回合
		AI_TURN,         // AI玩家回合
		GAME_OVER        // 游戏结束
	}

	// 游戏状态字段
	private int turnNumber = 1;                  // 当前回合计数
	private Player humanPlayer;                  // 人类玩家
	private Player aiPlayer;                     // AI玩家
	private Player activePlayer;                 // 当前行动玩家
	private Board gameBoard;                     // 游戏棋盘
	private GamePhase currentPhase = GamePhase.INITIALIZING; // 当前游戏阶段
	private boolean gameEnded = false;           // 游戏是否结束
	private Player winner = null;                // 获胜玩家

	// 当前回合状态
	private boolean hasMoved = false;            // 当前单位是否已移动
	private boolean hasAttacked = false;         // 当前单位是否已攻击
	private Unit selectedUnit = null;            // 当前选中的单位
	private Card selectedCard = null;            // 当前选中的卡牌

	// 存储所有单位的列表，便于快速访问
	private List<Unit> allUnits = new ArrayList<>();

	/**
	 * 默认构造函数
	 */
	public GameState() {
		// 保持空构造函数以便向后兼容
	}

	/**
	 * 初始化游戏状态
	 * @param humanPlayer 人类玩家
	 * @param aiPlayer AI玩家
	 * @param gameBoard 游戏棋盘
	 */
	public void initializeGame(Player humanPlayer, Player aiPlayer, Board gameBoard) {
		this.humanPlayer = humanPlayer;
		this.aiPlayer = aiPlayer;
		this.gameBoard = gameBoard;
		this.turnNumber = 1;
		this.activePlayer = humanPlayer; // 人类玩家先手
		this.currentPhase = GamePhase.PLAYER_TURN;
		this.gameInitalised = true;
		this.gameEnded = false;
		this.winner = null;
	}

	/**
	 * 开始新回合
	 * @return 新回合的玩家
	 */
	public Player startNewTurn() {
		// 切换活跃玩家
		activePlayer = (activePlayer == humanPlayer) ? aiPlayer : humanPlayer;

		// 如果是新的人类玩家回合，增加回合计数
		if (activePlayer == humanPlayer) {
			turnNumber++;
		}

		// 设置当前阶段
		currentPhase = (activePlayer == humanPlayer) ? GamePhase.PLAYER_TURN : GamePhase.AI_TURN;

		// 重置回合状态
		resetTurnState();

		// 为活跃玩家增加法力值
		int newMana = Math.min(turnNumber, 9); // 最大9点法力值
		activePlayer.setMana(newMana);

		// 活跃玩家抽一张牌
		// 这里假设Player类有drawCard方法
		// activePlayer.drawCard();

		// 重置所有单位的行动状态
		for (Unit unit : allUnits) {
			if (unit.getOwner() == activePlayer) {
				unit.resetTurnState();
			}
		}

		return activePlayer;
	}

	/**
	 * 结束当前回合
	 */
	public void endTurn() {
		resetTurnState(); // 重置回合状态
	}

	/**
	 * 重置当前回合状态
	 */
	private void resetTurnState() {
		hasMoved = false;
		hasAttacked = false;
		selectedUnit = null;
		selectedCard = null;
	}

	/**
	 * 检查游戏是否结束
	 * @return 如果游戏结束返回true，否则返回false
	 */
	public boolean checkGameOver() {
		// 检查人类玩家的Avatar是否死亡
		if (humanPlayer.getAvatar().getHealth() <= 0) {
			gameEnded = true;
			winner = aiPlayer;
			currentPhase = GamePhase.GAME_OVER;
			return true;
		}

		// 检查AI玩家的Avatar是否死亡
		if (aiPlayer.getAvatar().getHealth() <= 0) {
			gameEnded = true;
			winner = humanPlayer;
			currentPhase = GamePhase.GAME_OVER;
			return true;
		}

		return false;
	}

	/**
	 * 添加单位到游戏中
	 * @param unit 要添加的单位
	 */
	public void addUnit(Unit unit) {
		allUnits.add(unit);
	}

	/**
	 * 从游戏中移除单位
	 * @param unit 要移除的单位
	 */
	public void removeUnit(Unit unit) {
		allUnits.remove(unit);
	}

	/**
	 * 根据位置获取单位
	 * @param tile 棋盘位置
	 * @return 位于该位置的单位，如果没有则返回null
	 */
	public Unit getUnitAtTile(Tile tile) {
		return gameBoard.getUnitAt(tile);
	}

	// Getters and Setters

	public int getTurnNumber() {
		return turnNumber;
	}

	public Player getHumanPlayer() {
		return humanPlayer;
	}

	public Player getAiPlayer() {
		return aiPlayer;
	}

	public Player getActivePlayer() {
		return activePlayer;
	}

	public Board getGameBoard() {
		return gameBoard;
	}

	public GamePhase getCurrentPhase() {
		return currentPhase;
	}

	public void setCurrentPhase(GamePhase phase) {
		this.currentPhase = phase;
	}

	public boolean isGameEnded() {
		return gameEnded;
	}

	public Player getWinner() {
		return winner;
	}

	public boolean hasUnitMoved() {
		return hasMoved;
	}

	public void setHasMoved(boolean hasMoved) {
		this.hasMoved = hasMoved;
	}

	public boolean hasUnitAttacked() {
		return hasAttacked;
	}

	public void setHasAttacked(boolean hasAttacked) {
		this.hasAttacked = hasAttacked;
	}

	public Unit getSelectedUnit() {
		return selectedUnit;
	}

	public void setSelectedUnit(Unit selectedUnit) {
		this.selectedUnit = selectedUnit;
	}

	public Card getSelectedCard() {
		return selectedCard;
	}

	public void setSelectedCard(Card selectedCard) {
		this.selectedCard = selectedCard;
	}

	public List<Unit> getAllUnits() {
		return allUnits;
	}

	/**
	 * 检查指定玩家是否是当前活动玩家
	 * @param player 要检查的玩家
	 * @return 如果是当前活动玩家则返回true
	 */
	public boolean isActivePlayer(Player player) {
		return activePlayer == player;
	}

	/**
	 * 检查游戏是否已初始化
	 * @return 如果游戏已初始化则返回true
	 */
	public boolean isGameInitialized() {
		return gameInitalised;
	}
}