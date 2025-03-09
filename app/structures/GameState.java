/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
 *
 */

package structures;

/**
 * This class can be used to hold information about the on-going game.
 * Its created with the GameActor.
 *
 * @author Dr. Richard McCreadie
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import structures.basic.Card;
import structures.basic.EffectAnimation;
import structures.basic.Player;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * This class holds information about the on-going game.
 * It acts as the central repository for all game state data.
 * 集成了Board功能，直接管理棋盘。
 *
 * @author Dr. Richard McCreadie
 *
 */
public class GameState {

	// 单例模式实现
	private static GameState instance;

	/**
	 * 获取GameState实例
	 * @return GameState的单例实例
	 */
	public static GameState getInstance() {
		if (instance == null) {
			instance = new GameState();
		}
		return instance;
	}

	/**
	 * 重置GameState实例
	 * 主要用于测试或开始新游戏
	 */
	public static void resetInstance() {
		instance = new GameState();
	}

	// 保留原有字段以保持兼容性
	public boolean gameInitalised = false;
	public boolean something = false;

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
	private GamePhase currentPhase = GamePhase.INITIALIZING; // 当前游戏阶段
	private boolean gameEnded = false;           // 游戏是否结束
	private Player winner = null;                // 获胜玩家

	// 当前回合状态
	private boolean hasMoved = false;            // 当前单位是否已移动
	private boolean hasAttacked = false;         // 当前单位是否已攻击
	private Unit selectedUnit = null;            // 当前选中的单位
	private Card selectedCard = null;            // 当前选中的卡牌

	// 棋盘管理 (集成Board功能)
	private Tile[][] tiles;                      // 棋盘格子数组(9x5)
	private static final int BOARD_WIDTH = 9;    // 棋盘宽度
	private static final int BOARD_HEIGHT = 5;   // 棋盘高度
	private Map<Position, Unit> unitPositions;   // 存储单位位置的映射

	// 存储所有单位的列表，便于快速访问
	private List<Unit> allUnits = new ArrayList<>();

	// 随机数生成器，用于随机决策
	private Random random = new Random();

	/**
	 * 默认构造函数
	 */
	public GameState() {
		// 保持空构造函数以便向后兼容
		unitPositions = new HashMap<>();
	}

	/**
	 * 初始化游戏状态
	 * @param humanPlayer 人类玩家
	 * @param aiPlayer AI玩家
	 */
	public void initializeGame(Player humanPlayer, Player aiPlayer) {
		this.humanPlayer = humanPlayer;
		this.aiPlayer = aiPlayer;
		this.turnNumber = 1;
		this.activePlayer = humanPlayer; // 人类玩家先手
		this.currentPhase = GamePhase.PLAYER_TURN;
		this.gameInitalised = true;
		this.gameEnded = false;
		this.winner = null;

		// 初始化棋盘
		initializeBoard();
	}

	/**
	 * 初始化棋盘
	 */
	private void initializeBoard() {
		tiles = new Tile[BOARD_WIDTH][BOARD_HEIGHT];
		unitPositions = new HashMap<>();

		// 在实际实现中，这里需要创建每个位置的Tile对象
		for (int x = 0; x < BOARD_WIDTH; x++) {
			for (int y = 0; y < BOARD_HEIGHT; y++) {
				tiles[x][y] = BasicObjectBuilders.loadTile(x, y);
			}
		}
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
		activePlayer.drawCard();

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
	 * @param tile 单位的位置
	 */
	public void addUnit(Unit unit, Tile tile) {
		allUnits.add(unit);
		unit.setPositionByTile(tile);
		Position position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
		unitPositions.put(position, unit);
	}

	/**
	 * 从游戏中移除单位
	 * @param unit 要移除的单位
	 */
	public void removeUnit(Unit unit) {
		allUnits.remove(unit);
		Position position = unit.getPosition();
		unitPositions.remove(position);
	}

	/**
	 * 移动单位到新位置
	 * @param unit 要移动的单位
	 * @param newTile 新位置
	 * @return 如果成功移动返回true
	 */
	public boolean moveUnit(Unit unit, Tile newTile) {
		// 检查移动是否有效
		if (!isValidMove(unit, newTile)) {
			return false;
		}

		// 从旧位置移除
		Position oldPosition = unit.getPosition();
		unitPositions.remove(oldPosition);

		// 设置新位置
		unit.setPositionByTile(newTile);
		Position newPosition = unit.getPosition();
		unitPositions.put(newPosition, unit);

		// 设置单位已移动状态
		unit.onUnitMoved();

		return true;
	}

	/**
	 * 检查移动是否有效
	 * @param unit 要移动的单位
	 * @param targetTile 目标位置
	 * @return 如果移动有效返回true
	 */
	public boolean isValidMove(Unit unit, Tile targetTile) {
		// 检查单位是否可以移动
		if (!unit.canMove()) {
			return false;
		}

		// 检查目标位置是否已有单位
		if (getUnitAtTile(targetTile) != null) {
			return false;
		}

		// 检查目标位置是否在有效移动范围内
		List<Tile> validMoves = getValidMoves(unit);
		return validMoves.contains(targetTile);
	}

	/**
	 * 获取单位的所有有效移动位置
	 * @param unit 要检查的单位
	 * @return 有效移动位置列表
	 */
	public List<Tile> getValidMoves(Unit unit) {
		List<Tile> validMoves = new ArrayList<>();
		Position pos = unit.getPosition();
		int tilex = pos.getTilex();
		int tiley = pos.getTiley();

		// 获取该单位是否有飞行能力
		boolean hasFlying = unit.hasAbility("Flying");

		// 如果单位有飞行能力，可以移动到任何空位置
		if (hasFlying) {
			for (int x = 0; x < BOARD_WIDTH; x++) {
				for (int y = 0; y < BOARD_HEIGHT; y++) {
					Tile tile = tiles[x][y];
					if (getUnitAtTile(tile) == null) {
						validMoves.add(tile);
					}
				}
			}
			return validMoves;
		}

		// 普通单位的移动规则：可以移动到相邻的空位置
		// 可以在四个基本方向上移动最多2格，或在斜角方向上移动1格

		// 检查基本方向（上下左右）
		int[][] directions = {
				{-1, 0}, {1, 0}, {0, -1}, {0, 1} // 左右上下
		};

		for (int[] dir : directions) {
			// 检查1格距离
			int x1 = tilex + dir[0];
			int y1 = tiley + dir[1];
			if (isValidPosition(x1, y1)) {
				Tile tile = tiles[x1][y1];
				if (getUnitAtTile(tile) == null) {
					validMoves.add(tile);

					// 检查2格距离
					int x2 = x1 + dir[0];
					int y2 = y1 + dir[1];
					if (isValidPosition(x2, y2)) {
						Tile tile2 = tiles[x2][y2];
						if (getUnitAtTile(tile2) == null) {
							validMoves.add(tile2);
						}
					}
				}
			}
		}

		// 检查斜角方向
		int[][] diagonals = {
				{-1, -1}, {-1, 1}, {1, -1}, {1, 1} // 左上、左下、右上、右下
		};

		for (int[] diag : diagonals) {
			int x = tilex + diag[0];
			int y = tiley + diag[1];
			if (isValidPosition(x, y)) {
				Tile tile = tiles[x][y];
				if (getUnitAtTile(tile) == null) {
					validMoves.add(tile);
				}
			}
		}

		return validMoves;
	}

	/**
	 * 检查位置是否在棋盘范围内
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @return 如果在范围内返回true
	 */
	public boolean isValidPosition(int x, int y) {
		return x >= 0 && x < BOARD_WIDTH && y >= 0 && y < BOARD_HEIGHT;
	}

	/**
	 * 根据位置获取单位
	 * @param tile 棋盘位置
	 * @return 位于该位置的单位，如果没有则返回null
	 */
	public Unit getUnitAtTile(Tile tile) {
		if (tile == null) return null;

		for (Position pos : unitPositions.keySet()) {
			if (pos.getTilex() == tile.getTilex() && pos.getTiley() == tile.getTiley()) {
				return unitPositions.get(pos);
			}
		}
		return null;
	}

	/**
	 * 获取指定坐标的Tile
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @return 位于该坐标的Tile
	 */
	public Tile getTile(int x, int y) {
		if (isValidPosition(x, y)) {
			return tiles[x][y];
		}
		return null;
	}

	/**
	 * 处理单位攻击
	 * @param attacker 攻击单位
	 * @param defender 防御单位
	 * @return 如果攻击成功返回true
	 */
	public boolean attackUnit(Unit attacker, Unit defender) {
		// 检查是否可以攻击
		if (!canAttack(attacker, defender)) {
			return false;
		}

		// 进行攻击
		int damageDealt = defender.takeDamage(attacker.getAttackValue());
		attacker.onUnitAttacked();

		// 如果防御单位未死亡且在攻击范围内，进行反击
		if (!defender.isDead() && isInAttackRange(defender, attacker)) {
			attacker.takeDamage(defender.getAttackValue());
		}

		// 检查是否有任何单位死亡
		if (defender.isDead()) {
			// 触发死亡监视效果
			triggerDeathwatchEffects(defender);
			removeUnit(defender);
		}

		if (attacker.isDead()) {
			triggerDeathwatchEffects(attacker);
			removeUnit(attacker);
		}

		return true;
	}

	/**
	 * 检查是否可以攻击
	 * @param attacker 攻击单位
	 * @param defender 防御单位
	 * @return 如果可以攻击返回true
	 */
	public boolean canAttack(Unit attacker, Unit defender) {
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
		if (isAffectedByProvoke(attacker) && !hasProvoke(defender)) {
			return false;
		}

		return true;
	}

	/**
	 * 检查单位是否在攻击范围内
	 * @param attacker 攻击单位
	 * @param defender 防御单位
	 * @return 如果在攻击范围内返回true
	 */
	private boolean isInAttackRange(Unit attacker, Unit defender) {
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
	 * @return 如果受到嘲讽影响返回true
	 */
	private boolean isAffectedByProvoke(Unit unit) {
		Position pos = unit.getPosition();
		int tilex = pos.getTilex();
		int tiley = pos.getTiley();

		// 检查相邻位置是否有带嘲讽的敌方单位
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue;

				int x = tilex + dx;
				int y = tiley + dy;

				if (isValidPosition(x, y)) {
					Tile tile = tiles[x][y];
					Unit adjacentUnit = getUnitAtTile(tile);

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
	 * 检查单位是否有嘲讽能力
	 * @param unit 要检查的单位
	 * @return 如果有嘲讽能力返回true
	 */
	private boolean hasProvoke(Unit unit) {
		return unit.hasAbility("Provoke");
	}

	/**
	 * 触发死亡监视效果
	 * @param deadUnit 死亡的单位
	 */
	private void triggerDeathwatchEffects(Unit deadUnit) {
		// 触发所有拥有死亡监视能力的单位的效果
		for (Unit unit : allUnits) {
			if (unit.hasAbility("Deathwatch")) {
				// 这里应该调用具体的死亡监视效果
				// 在实际实现中，每个单位的死亡监视效果可能不同
				unit.triggerAbility("Deathwatch");
			}
		}
	}

	/**
	 * 从指定位置获取相邻的空位置
	 * @param unit 中心单位
	 * @return 相邻的空位置列表
	 */
	private List<Tile> getAdjacentEmptyTiles(Unit unit) {
		List<Tile> emptyTiles = new ArrayList<>();
		Position pos = unit.getPosition();
		int tilex = pos.getTilex();
		int tiley = pos.getTiley();

		// 检查相邻的8个方向
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue; // 跳过自身

				int adjacentX = tilex + dx;
				int adjacentY = tiley + dy;

				if (isValidPosition(adjacentX, adjacentY)) {
					Tile adjacentTile = tiles[adjacentX][adjacentY];
					if (getUnitAtTile(adjacentTile) == null) {
						emptyTiles.add(adjacentTile);
					}
				}
			}
		}

		return emptyTiles;
	}

	/**
	 * 在指定单位周围随机位置召唤一个单位
	 * @param sourceUnit 源单位
	 * @param unitType 要召唤的单位类型(配置文件路径)
	 * @return 召唤的单位，如果无法召唤则返回null
	 */
	public Unit summonAdjacentRandomUnit(Unit sourceUnit, String unitType) {
		List<Tile> emptyTiles = getAdjacentEmptyTiles(sourceUnit);
		if (emptyTiles.isEmpty()) {
			return null; // 没有空位可以召唤
		}

		// 随机选择一个空位
		Tile targetTile = emptyTiles.get(random.nextInt(emptyTiles.size()));

		// 召唤单位
		return summonUnitAt(unitType, targetTile, sourceUnit.getOwner());
	}

	/**
	 * 在指定位置召唤单位
	 * @param unitType 单位类型(配置文件路径)
	 * @param tile 目标位置
	 * @param owner 单位所有者
	 * @return 召唤的单位
	 */
	public Unit summonUnitAt(String unitType, Tile tile, Player owner) {
		// 在实际实现中，应该使用BasicObjectBuilders加载单位
		// 这里简化为返回null
		// Unit unit = BasicObjectBuilders.loadUnit(unitType, nextUnitId(), Unit.class);
		return null;
	}

	/**
	 * 获取单位相邻的已受伤的敌方单位
	 * @param unit 中心单位
	 * @return 相邻的已受伤敌方单位列表
	 */
	public List<Unit> getAdjacentEnemyUnitsWithLowHealth(Unit unit) {
		List<Unit> targetUnits = new ArrayList<>();
		Position pos = unit.getPosition();
		int tilex = pos.getTilex();
		int tiley = pos.getTiley();

		// 检查相邻的8个方向
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0) continue; // 跳过自身

				int adjacentX = tilex + dx;
				int adjacentY = tiley + dy;

				if (isValidPosition(adjacentX, adjacentY)) {
					Tile adjacentTile = tiles[adjacentX][adjacentY];
					Unit adjacentUnit = getUnitAtTile(adjacentTile);

					// 检查是否是敌方单位且已受伤
					if (adjacentUnit != null &&
							adjacentUnit.getOwner() != unit.getOwner() &&
							adjacentUnit.getHealth() < adjacentUnit.getMaxHealth()) {
						targetUnits.add(adjacentUnit);
					}
				}
			}
		}

		return targetUnits;
	}

	/**
	 * 获取单位前方和后方的友方单位
	 * @param unit 中心单位
	 * @return 前方和后方的友方单位列表
	 */
	public List<Unit> getAdjacentFriendlyUnitsInLine(Unit unit) {
		List<Unit> lineUnits = new ArrayList<>();
		Position pos = unit.getPosition();
		int tilex = pos.getTilex();
		int tiley = pos.getTiley();

		// 获取单位所属玩家的头像位置，确定方向
		Unit avatar = unit.getOwner().getAvatar();
		if (avatar == null) return lineUnits;

		Position avatarPos = avatar.getPosition();

		// 根据头像位置确定前进方向
		int dx = (tilex > avatarPos.getTilex()) ? 1 : -1;

		// 检查前方和后方单位
		int frontX = tilex + dx;
		int backX = tilex - dx;

		// 检查前方
		if (isValidPosition(frontX, tiley)) {
			Tile frontTile = tiles[frontX][tiley];
			Unit frontUnit = getUnitAtTile(frontTile);
			if (frontUnit != null && frontUnit.getOwner() == unit.getOwner()) {
				lineUnits.add(frontUnit);
			}
		}

		// 检查后方
		if (isValidPosition(backX, tiley)) {
			Tile backTile = tiles[backX][tiley];
			Unit backUnit = getUnitAtTile(backTile);
			if (backUnit != null && backUnit.getOwner() == unit.getOwner()) {
				lineUnits.add(backUnit);
			}
		}

		return lineUnits;
	}

	/**
	 * 播放效果动画
	 * @param effect 效果动画
	 * @param tile 目标位置
	 */
	public void playEffectAnimation(EffectAnimation effect, Tile tile) {
		// 此方法应该调用BasicCommands.playEffectAnimation
		// 在实际实现中需要有对应的实现
	}

	/**
	 * 生成下一个单位ID
	 * @return 新的单位ID
	 */
	private int nextUnitId() {
		// 简单实现，实际应该有更好的ID生成策略
		return allUnits.size() + 10;
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

	public int getBoardWidth() {
		return BOARD_WIDTH;
	}

	public int getBoardHeight() {
		return BOARD_HEIGHT;
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