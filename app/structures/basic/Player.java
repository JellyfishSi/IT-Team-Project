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
public class Player {

	// 玩家类型枚举
	public enum PlayerType {
		HUMAN,
		AI
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
	private Deck deck;                   // 玩家牌库
	private Hand hand;                   // 玩家手牌

	// 单位管理
	private Unit avatar;                 // 玩家头像单位 (在棋盘上的化身)

	/**
	 * 默认构造函数，保持原有接口兼容性
	 */
	public Player() {
		super();
		this.health = 20;
		this.mana = 0;
	}

	/**
	 * 基本构造函数，保持原有接口兼容性
	 * @param health 生命值
	 * @param mana 法力值
	 */
	public Player(int health, int mana) {
		super();
		this.health = health;
		this.mana = mana;
		this.maxMana = 9; // 最大法力值上限为9
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
			hand.addCard(card);
		}
		return card;
	}

	/**
	 * 使用一张卡牌
	 * @param card 要使用的卡牌
	 * @param manaUsed 使用的法力值
	 * @return 是否成功使用
	 */
	public boolean useCard(Card card, int manaUsed) {
		if (hand == null || !hand.containsCard(card)) {
			return false; // 手牌中不存在该卡牌
		}

		if (mana < manaUsed) {
			return false; // 法力值不足
		}

		// 扣除法力值
		mana -= manaUsed;

		// 从手牌中移除卡牌
		hand.removeCard(card);

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

		// 抽一张牌
		drawCard();
	}

	/**
	 * 结束回合时的操作
	 */
	public void endTurn() {
		// 回合结束时的处理（如有）
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

	// 原有的Getter和Setter方法
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
	}

	public Hand getHand() {
		return hand;
	}

	public void setHand(Hand hand) {
		this.hand = hand;
	}

	public Unit getAvatar() {
		return avatar;
	}

	public void setAvatar(Unit avatar) {
		this.avatar = avatar;

		// 同步生命值
		if (avatar != null) {
			avatar.setHealth(this.health);
		}
	}
}
