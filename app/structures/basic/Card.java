/**
 * This is the base representation of a Card which is rendered in the player's hand.
 * A card has an id, a name (cardname) and a manacost. A card then has a large and mini
 * version. The mini version is what is rendered at the bottom of the screen. The big
 * version is what is rendered when the player clicks on a card in their hand.
 * 
 * @author Dr. Richard McCreadie
 *
 */
package structures.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This is the base representation of a Card which is rendered in the player's hand.
 * A card has an id, a name (cardname) and a manacost. A card then has a large and mini
 * version. The mini version is what is rendered at the bottom of the screen. The big
 * version is what is rendered when the player clicks on a card in their hand.
 *
 * @author Dr. Richard McCreadie
 *
 */
public class Card {

	// 卡牌类型枚举
	public enum CardType {
		CREATURE,    // 生物卡
		SPELL,       // 法术卡
		ARTIFACT     // 神器卡
	}

	// 原始字段（保持与JSON反序列化兼容）
	int id;
	String cardname;
	int manacost;
	MiniCard miniCard;
	BigCard bigCard;
	boolean isCreature;
	String unitConfig;

	// 新增字段
	@JsonIgnore
	private CardType cardType;          // 卡牌类型
	@JsonIgnore
	private Player owner;               // 所属玩家
	@JsonIgnore
	private String cardDescription;     // 卡牌描述
	@JsonIgnore
	private List<CardAbility> abilities = new ArrayList<>(); // 卡牌能力列表
	@JsonIgnore
	private TargetRequirement targetRequirement; // 目标选择要求

	// 默认构造函数
	public Card() {};

	// 原始构造函数
	public Card(int id, String cardname, int manacost, MiniCard miniCard, BigCard bigCard, boolean isCreature, String unitConfig) {
		super();
		this.id = id;
		this.cardname = cardname;
		this.manacost = manacost;
		this.miniCard = miniCard;
		this.bigCard = bigCard;
		this.isCreature = isCreature;
		this.unitConfig = unitConfig;

		// 根据isCreature设置cardType
		this.cardType = isCreature ? CardType.CREATURE : CardType.SPELL;
	}

	// 扩展构造函数
	public Card(int id, String cardname, int manacost, MiniCard miniCard, BigCard bigCard,
				boolean isCreature, String unitConfig, CardType cardType, String cardDescription) {
		this(id, cardname, manacost, miniCard, bigCard, isCreature, unitConfig);
		this.cardType = cardType;
		this.cardDescription = cardDescription;
	}

	/**
	 * 在游戏中使用此卡牌
	 * @param gameState 当前游戏状态
	 * @param targetTile 目标位置（如有）
	 * @return 是否成功使用
	 */
	@JsonIgnore
	public boolean play(GameState gameState, Tile targetTile) {
		// 检查目标是否有效
		if (!isValidTarget(gameState, targetTile)) {
			return false;
		}

		// 执行卡牌效果
		executeCardEffect(gameState, targetTile);

		return true;
	}

	/**
	 * 检查目标是否有效
	 * @param gameState 当前游戏状态
	 * @param targetTile 目标位置
	 * @return 如果目标有效则返回true
	 */
	@JsonIgnore
	public boolean isValidTarget(GameState gameState, Tile targetTile) {
		if (targetRequirement == null) {
			return true; // 无目标要求
		}

		return targetRequirement.isValidTarget(gameState, targetTile);
	}

	/**
	 * 获取卡牌的所有有效目标位置
	 * @param gameState 当前游戏状态
	 * @return 有效目标位置列表
	 */
	@JsonIgnore
	public List<Tile> getValidTargets(GameState gameState) {
		if (targetRequirement == null) {
			return new ArrayList<>(); // 无目标要求
		}

		return targetRequirement.getValidTargets(gameState);
	}

	/**
	 * 执行卡牌效果
	 * @param gameState 当前游戏状态
	 * @param targetTile 目标位置
	 */
	@JsonIgnore
	protected void executeCardEffect(GameState gameState, Tile targetTile) {
		// 基类中不实现具体效果，由子类覆盖
	}

	/**
	 * 添加卡牌能力
	 * @param ability 能力对象
	 */
	@JsonIgnore
	public void addAbility(CardAbility ability) {
		abilities.add(ability);
	}

	/**
	 * 设置卡牌目标要求
	 * @param targetRequirement 目标要求
	 */
	@JsonIgnore
	public void setTargetRequirement(TargetRequirement targetRequirement) {
		this.targetRequirement = targetRequirement;
	}

	/**
	 * 初始化卡牌效果和能力
	 * 这个方法应该在卡牌加载后调用，以设置不能通过JSON直接加载的效果
	 */
	@JsonIgnore
	public void initializeCardEffects() {
		// 默认实现为空，由子类覆盖
	}

	/**
	 * 从BigCard获取完整规则文本
	 * @return 规则文本
	 */
	@JsonIgnore
	public String getFullRulesText() {
		if (bigCard == null || bigCard.getRulesTextRows() == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		for (String line : bigCard.getRulesTextRows()) {
			sb.append(line).append(" ");
		}
		return sb.toString().trim();
	}

	// 原始getter和setter方法
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCardname() {
		return cardname;
	}

	public void setCardname(String cardname) {
		this.cardname = cardname;
	}

	public int getManacost() {
		return manacost;
	}

	public void setManacost(int manacost) {
		this.manacost = manacost;
	}

	public MiniCard getMiniCard() {
		return miniCard;
	}

	public void setMiniCard(MiniCard miniCard) {
		this.miniCard = miniCard;
	}

	public BigCard getBigCard() {
		return bigCard;
	}

	public void setBigCard(BigCard bigCard) {
		this.bigCard = bigCard;
	}

	public boolean getIsCreature() {
		return isCreature;
	}

	public void setIsCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}

	public void setCreature(boolean isCreature) {
		this.isCreature = isCreature;
	}

	public boolean isCreature() {
		return isCreature;
	}

	public String getUnitConfig() {
		return unitConfig;
	}

	public void setUnitConfig(String unitConfig) {
		this.unitConfig = unitConfig;
	}

	// 新增getter和setter方法
	@JsonIgnore
	public CardType getCardType() {
		return cardType;
	}

	@JsonIgnore
	public void setCardType(CardType cardType) {
		this.cardType = cardType;
	}

	@JsonIgnore
	public Player getOwner() {
		return owner;
	}

	@JsonIgnore
	public void setOwner(Player owner) {
		this.owner = owner;
	}

	@JsonIgnore
	public String getCardDescription() {
		return cardDescription;
	}

	@JsonIgnore
	public void setCardDescription(String cardDescription) {
		this.cardDescription = cardDescription;
	}

	@JsonIgnore
	public List<CardAbility> getAbilities() {
		return abilities;
	}

	@JsonIgnore
	public TargetRequirement getTargetRequirement() {
		return targetRequirement;
	}

	/**
	 * 卡牌能力接口，定义卡牌的特殊能力
	 */
	@JsonIgnore
	public interface CardAbility {
		String getName();
		void applyEffect(GameState gameState, Tile targetTile);
	}

	/**
	 * 目标要求接口，定义卡牌目标选择的要求
	 */
	@JsonIgnore
	public interface TargetRequirement {
		/**
		 * 检查目标是否有效
		 * @param gameState 当前游戏状态
		 * @param targetTile 目标位置
		 * @return 如果目标有效则返回true
		 */
		boolean isValidTarget(GameState gameState, Tile targetTile);

		/**
		 * 获取所有有效目标位置
		 * @param gameState 当前游戏状态
		 * @return 有效目标位置列表
		 */
		List<Tile> getValidTargets(GameState gameState);
	}

	/**
	 * 基本目标要求实现类，使用谓词过滤目标
	 */
	@JsonIgnore
	public static class BasicTargetRequirement implements TargetRequirement {
		private Predicate<Tile> targetFilter;

		public BasicTargetRequirement(Predicate<Tile> targetFilter) {
			this.targetFilter = targetFilter;
		}

		@Override
		public boolean isValidTarget(GameState gameState, Tile targetTile) {
			return targetFilter.test(targetTile);
		}

		@Override
		public List<Tile> getValidTargets(GameState gameState) {
			List<Tile> validTargets = new ArrayList<>();
			Board board = gameState.getGameBoard();

			// 遍历所有棋盘位置
			for (int x = 0; x < board.getWidth(); x++) {
				for (int y = 0; y < board.getHeight(); y++) {
					Tile tile = board.getTile(x, y);
					if (targetFilter.test(tile)) {
						validTargets.add(tile);
					}
				}
			}

			return validTargets;
		}
	}
}
