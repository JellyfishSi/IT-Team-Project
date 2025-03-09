package structures.basic;

/**
 * A basic representation of of the Player. A player
 * has health and mana.
 * 
 * @author Dr. Richard McCreadie
 *
 */
public class Player {

	int health;
	int mana;
	
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
	}
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
	}
	public int getHealth() {
		return health;
	}
	public void setHealth(int health) {
		this.health = health;
	}
	public int getMana() {
		return mana;
	}
	public void setMana(int mana) {
		this.mana = mana;
	}
	
	
	
}
package structures.basic;

import structures.deck.Deck;
import structures.hand.Hand;

/**
 * A representation of a Player in the game.
 * A player has health and mana, along with a deck, hand, avatar unit.
 * The avatar unit is the player's representation on the board.
 * Player's health is tied to their avatar - if avatar is destroyed, player loses.
 *
 * @author Dr. Richard McCreadie
 *
 */
package structures.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import structures.GameState;
import structures.deck.Deck;
import structures.hand.Hand;

/**
 * 玩家类，代表游戏中的一位玩家。
 * 管理玩家的生命值、法力值、牌库、手牌和头像单位。
 *
 * @author Dr. Richard McCreadie and Your Team
 */
public class Player {

	// 玩家类型枚举
	public enum PlayerType {
		HUMAN,  // 人类玩家
		AI      // AI玩家
	}

	// 基础属性
	private int id;                      // 玩家ID
	private PlayerType type;             // 玩家类型
	private String name;                 // 玩家名称

	// 战斗属性
	private int health;                  // 当前生命值 (与avatar同步)
	private int mana;                    // 当前法力值
	private int maxMana;                 // 最大法力值

	// 卡牌管理
	@JsonIgnore
	private Deck deck;                   // 玩家牌库
	@JsonIgnore
	private Hand hand;                   // 玩家手牌

	// 单位管理
	@JsonIgnore
	private Unit avatar;                 // 玩家头像单位 (在棋盘上的化身)

	// 状态跟踪
	@JsonIgnore
	private Map<String, Integer> statusEffects = new HashMap<>(); // 状态效果(如晕眩)及其持续回合
	@JsonIgnore
	private List<Unit.Artifact> equippedArtifacts = new ArrayList<>(); // 装备的神器
	@JsonIgnore
	private boolean hasTakenActionThisTurn = false; // 本回合是否已执行行动

	/**
	 * 默认构造函数
	 */
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
		this.maxMana = 0;
	}

	/**
	 * 基本构造函数
	 * @param health 生命值
	 * @param mana 法力值
	 */
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
		this.maxMana = mana;
	}

	/**
	 * 完整构造函数
	 * @param id 玩家ID
	 * @param type 玩家类型
	 * @param name 玩家名称
	 * @param health 生命值
	 * @param mana 法力值
	 */
	public Player(int id, PlayerType type, String name, int health, int mana) {
		this(health, mana);
		this.id = id;
		this.type = type;
		this.name = name;
	}

	/**
	 * 初始化玩家
	 * @param deck 玩家牌库
	 * @param avatar 玩家头像单位
	 */
	public void initialize(Deck deck, Unit avatar) {
		this.deck = deck;
		this.hand = new Hand(this);
		this.avatar = avatar;

		// 确保avatar与玩家健康状态同步
		if (avatar != null) {
			avatar.setHealth(this.health);
			avatar.setMaxHealth(this.health);
			avatar.setOwner(this);
			avatar.setUnitType(Unit.UnitType.AVATAR);
		}

		// 初始抽3张牌
		for (int i = 0; i < 3; i++) {
			drawCard();
		}
	}

	/**
	 * 抽一张牌
	 * @return 抽到的卡牌，如果牌库为空则返回null
	 */
	public Card drawCard() {
		if (deck == null || deck.isEmpty()) {
			return null; // 牌库为空
		}

		Card card = deck.drawCard();
		if (card != null && hand != null && !hand.isFull()) {
			card.setOwner(this);
			hand.addCard(card);
			return card;
		}
		return null;
	}

	/**
	 * 使用一张卡牌
	 * @param card 要使用的卡牌
	 * @param targetTile 目标位置
	 * @param gameState 当前游戏状态
	 * @return 是否成功使用
	 */
	public boolean useCard(Card card, Tile targetTile, GameState gameState) {
		// 检查卡牌是否在手牌中
		if (hand == null || !hand.containsCard(card)) {
			return false;
		}

		// 检查法力值是否足够
		if (mana < card.getManacost()) {
			return false;
		}

		// 检查目标是否有效
		if (!card.isValidTarget(gameState, targetTile)) {
			return false;
		}

		// 扣除法力值
		mana -= card.getManacost();

		// 从手牌中移除卡牌
		hand.removeCard(card);

		// 执行卡牌效果
		if (!card.play(gameState, targetTile)) {
			// 如果卡牌使用失败，恢复法力值和手牌
			mana += card.getManacost();
			hand.addCard(card);
			return false;
		}

		// 标记玩家已执行行动
		hasTakenActionThisTurn = true;

		return true;
	}

	/**
	 * 开始新回合时的操作
	 * @param turnNumber 当前回合数
	 */
	public void startTurn(int turnNumber) {
		// 更新法力值（回合数决定法力值上限，最大为9）
		maxMana = Math.min(turnNumber, 9);
		mana = maxMana;

		// 重置行动标记
		hasTakenActionThisTurn = false;

		// 减少状态效果持续时间
		List<String> expiredEffects = new ArrayList<>();
		for (Map.Entry<String, Integer> effect : statusEffects.entrySet()) {
			int remainingDuration = effect.getValue() - 1;
			if (remainingDuration <= 0) {
				expiredEffects.add(effect.getKey());
			} else {
				statusEffects.put(effect.getKey(), remainingDuration);
			}
		}

		// 移除过期效果
		for (String effect : expiredEffects) {
			statusEffects.remove(effect);
		}

		// 抽一张牌
		drawCard();
	}

	/**
	 * 结束回合时的操作
	 */
	public void endTurn() {
		// 触发回合结束效果
		triggerEndTurnEffects();
	}

	/**
	 * 触发回合结束效果
	 */
	private void triggerEndTurnEffects() {
		// 在这里实现回合结束时需要触发的效果
	}

	/**
	 * 受到伤害
	 * @param amount 伤害量
	 * @return 实际受到的伤害
	 */
	public int takeDamage(int amount) {
		if (amount <= 0) return 0;

		int previousHealth = health;
		health = Math.max(0, health - amount);

		// 同步头像的生命值
		if (avatar != null) {
			avatar.setHealth(health);
		}

		// 触发神器效果
		if (!equippedArtifacts.isEmpty()) {
			List<Unit.Artifact> brokenArtifacts = new ArrayList<>();
			for (Unit.Artifact artifact : equippedArtifacts) {
				artifact.reduceRobustness(1); // 每次受到伤害减少1点耐久
				if (artifact.getRobustness() <= 0) {
					brokenArtifacts.add(artifact);
				}
			}

			// 移除已损坏的神器
			equippedArtifacts.removeAll(brokenArtifacts);
		}

		return previousHealth - health;
	}

	/**
	 * 治疗生命值
	 * @param amount 治疗量
	 * @return 实际恢复的生命值
	 */
	public int heal(int amount) {
		if (amount <= 0) return 0;

		int previousHealth = health;
		int maxHealth = avatar != null ? avatar.getMaxHealth() : 20;
		health = Math.min(maxHealth, health + amount);

		// 同步头像的生命值
		if (avatar != null) {
			avatar.setHealth(health);
		}

		return health - previousHealth;
	}

	/**
	 * 同步玩家生命值与Avatar生命值
	 * 这应当在Avatar受到伤害或治疗后调用
	 */
	public void syncHealthWithAvatar() {
		if (avatar != null) {
			this.health = avatar.getHealth();
		}
	}

	/**
	 * 设置生命值并同步到Avatar
	 * @param health 新的生命值
	 */
	public void setHealthAndSync(int health) {
		this.health = health;
		if (avatar != null) {
			avatar.setHealth(health);
		}
	}

	/**
	 * 检查玩家是否已经落败（Avatar是否被消灭）
	 * @return 如果玩家已落败则返回true
	 */
	public boolean isDefeated() {
		return health <= 0 || (avatar != null && avatar.getHealth() <= 0);
	}

	/**
	 * 装备神器
	 * @param artifact 要装备的神器
	 * @return 是否成功装备
	 */
	public boolean equipArtifact(Unit.Artifact artifact) {
		if (artifact != null) {
			equippedArtifacts.add(artifact);

			// 如果avatar存在，也同步装备
			if (avatar != null) {
				avatar.equipArtifact(artifact);
			}

			return true;
		}
		return false;
	}

	/**
	 * 移除神器
	 * @param artifactName 神器名称
	 * @return 是否成功移除
	 */
	public boolean removeArtifact(String artifactName) {
		boolean removed = equippedArtifacts.removeIf(a -> a.getName().equals(artifactName));

		// 如果avatar存在，也同步移除
		if (removed && avatar != null) {
			avatar.removeArtifact(artifactName);
		}

		return removed;
	}

	/**
	 * 添加状态效果
	 * @param effectName 效果名称
	 * @param duration 持续回合数
	 */
	public void addStatusEffect(String effectName, int duration) {
		if (duration > 0) {
			statusEffects.put(effectName, duration);
		}
	}

	/**
	 * 移除状态效果
	 * @param effectName 效果名称
	 */
	public void removeStatusEffect(String effectName) {
		statusEffects.remove(effectName);
	}

	/**
	 * 检查是否有指定状态效果
	 * @param effectName 效果名称
	 * @return 如果有该效果则返回true
	 */
	public boolean hasStatusEffect(String effectName) {
		return statusEffects.containsKey(effectName) && statusEffects.get(effectName) > 0;
	}

	/**
	 * 获取状态效果的剩余持续时间
	 * @param effectName 效果名称
	 * @return 剩余持续回合数，如果没有该效果则返回0
	 */
	public int getStatusEffectDuration(String effectName) {
		return statusEffects.getOrDefault(effectName, 0);
	}

	/**
	 * 检查玩家本回合是否已执行行动
	 * @return 如果已执行行动则返回true
	 */
	public boolean hasTakenActionThisTurn() {
		return hasTakenActionThisTurn;
	}

	/**
	 * 设置玩家本回合是否已执行行动
	 * @param hasTakenAction 是否已执行行动
	 */
	public void setHasTakenActionThisTurn(boolean hasTakenAction) {
		this.hasTakenActionThisTurn = hasTakenAction;
	}

	/**
	 * 获取玩家的有效卡牌（可用法力值足够使用的卡牌）
	 * @return 可用卡牌列表
	 */
	public List<Card> getPlayableCards() {
		if (hand == null) {
			return new ArrayList<>();
		}
		return hand.getPlayableCards(mana);
	}

	// 原始的Getter和Setter方法
	public int getHealth() {
		// 确保与avatar保持同步
		if (avatar != null) {
			return avatar.getHealth();
		}
		return health;
	}

	public void setHealth(int health) {
		this.health = health;
		// 同步到avatar
		if (avatar != null) {
			avatar.setHealth(health);
		}
	}

	public int getMana() {
		return mana;
	}

	public void setMana(int mana) {
		this.mana = mana;
	}

	// 新增的Getter和Setter方法
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public PlayerType getType() {
		return type;
	}

	public void setType(PlayerType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getMaxMana() {
		return maxMana;
	}

	public void setMaxMana(int maxMana) {
		this.maxMana = maxMana;
	}

	public Deck getDeck() {
		return deck;
	}

	public void setDeck(Deck deck) {
		this.deck = deck;

		// 设置牌库所属玩家
		if (deck != null) {
			deck.setOwner(this);
		}
	}

	public Hand getHand() {
		return hand;
	}

	public void setHand(Hand hand) {
		this.hand = hand;

		// 设置手牌所属玩家
		if (hand != null) {
			hand.setOwner(this);
		}
	}

	public Unit getAvatar() {
		return avatar;
	}

	public void setAvatar(Unit avatar) {
		this.avatar = avatar;

		// 设置Avatar的所属玩家和类型
		if (avatar != null) {
			avatar.setOwner(this);
			avatar.setUnitType(Unit.UnitType.AVATAR);

			// 同步生命值
			avatar.setHealth(this.health);
			avatar.setMaxHealth(this.health);
		}
	}

	public List<Unit.Artifact> getEquippedArtifacts() {
		return new ArrayList<>(equippedArtifacts);
	}

	public Map<String, Integer> getStatusEffects() {
		return new HashMap<>(statusEffects);
	}

	/**
	 * 判断是否是人类玩家
	 * @return 如果是人类玩家则返回true
	 */
	public boolean isHuman() {
		return type == PlayerType.HUMAN;
	}

	/**
	 * 判断是否是AI玩家
	 * @return 如果是AI玩家则返回true
	 */
	public boolean isAI() {
		return type == PlayerType.AI;
	}

	/**
	 * 获取当前手牌数量
	 * @return 手牌数量
	 */
	public int getHandSize() {
		return hand != null ? hand.size() : 0;
	}

	/**
	 * 获取当前牌库剩余卡牌数量
	 * @return 牌库剩余卡牌数量
	 */
	public int getDeckSize() {
		return deck != null ? deck.size() : 0;
	}

	/**
	 * 重置玩家状态（游戏重新开始时使用）
	 */
	public void reset() {
		// 重置生命值和法力值
		this.health = 20;
		this.mana = 0;
		this.maxMana = 0;

		// 清空手牌和牌库
		if (hand != null) {
			hand.clear();
		}

		if (deck != null) {
			deck.reset();
		}

		// 清空状态效果和神器
		statusEffects.clear();
		equippedArtifacts.clear();

		// 重置行动标记
		hasTakenActionThisTurn = false;
	}
}
