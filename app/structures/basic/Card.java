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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import structures.GameState;
import utils.BasicObjectBuilders;

/**
 * 卡牌基类，是游戏中所有卡牌的基础。
 * 提供卡牌通用属性和行为，支持不同类型的卡牌实现。
 *
 * @author Dr. Richard McCreadie and Your Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {

	// 卡牌类型枚举
	public enum CardType {
		CREATURE,    // 生物卡
		SPELL,       // 法术卡
		ARTIFACT     // 神器卡
	}

	// 目标类型枚举
	public enum TargetType {
		NONE,             // 无目标
		FRIENDLY_UNIT,    // 友方单位
		ENEMY_UNIT,       // 敌方单位
		ANY_UNIT,         // 任意单位
		FRIENDLY_AVATAR,  // 友方头像
		ENEMY_AVATAR,     // 敌方头像
		ANY_AVATAR,       // 任意头像
		EMPTY_TILE,       // 空白格子
		ADJACENT_TILE     // 相邻格子
	}

	// 卡牌效果时机枚举
	public enum EffectTiming {
		ON_PLAY,           // 打出卡牌时
		ON_SUMMON,         // 单位被召唤时（生物卡）
		ON_DEATH,          // 单位死亡时
		ON_ATTACK,         // 单位攻击时
		ON_DAMAGE,         // 单位受到伤害时
		ON_TURN_START,     // 回合开始时
		ON_TURN_END        // 回合结束时
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
	private CardType cardType;                                 // 卡牌类型
	@JsonIgnore
	private Player owner;                                      // 所属玩家
	@JsonIgnore
	private String cardDescription;                            // 卡牌描述
	@JsonIgnore
	private List<String> abilityNames = new ArrayList<>();     // 能力名称列表
	@JsonIgnore
	private Map<String, CardAbility> abilities = new HashMap<>();  // 卡牌能力映射表
	@JsonIgnore
	private TargetRequirement targetRequirement;               // 目标选择要求
	@JsonIgnore
	private int handPosition = -1;                             // 在手牌中的位置

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

		// 从卡牌规则文本解析能力
		parseAbilitiesFromRulesText();
	}

	// 扩展构造函数
	public Card(int id, String cardname, int manacost, MiniCard miniCard, BigCard bigCard,
				boolean isCreature, String unitConfig, CardType cardType, String cardDescription) {
		this(id, cardname, manacost, miniCard, bigCard, isCreature, unitConfig);
		this.cardType = cardType;
		this.cardDescription = cardDescription;
	}

	/**
	 * 从卡牌规则文本解析能力
	 * 识别并设置卡牌文本中描述的能力
	 */
	@JsonIgnore
	protected void parseAbilitiesFromRulesText() {
		String rulesText = getFullRulesText().toLowerCase();

		// 识别常见能力
		if (rulesText.contains("provoke")) {
			addAbilityName("Provoke");
		}
		if (rulesText.contains("flying")) {
			addAbilityName("Flying");
		}
		if (rulesText.contains("rush")) {
			addAbilityName("Rush");
		}
		if (rulesText.contains("deathwatch")) {
			addAbilityName("Deathwatch");
		}
		if (rulesText.contains("opening gambit")) {
			addAbilityName("OpeningGambit");
		}
		if (rulesText.contains("airdrop")) {
			addAbilityName("Airdrop");
		}
		if (rulesText.contains("zeal")) {
			addAbilityName("Zeal");
		}

		// 识别目标要求
		if (isCreature) {
			// 生物卡默认放置在相邻格子
			setupTargetRequirement(TargetType.ADJACENT_TILE);
		} else {
			// 法术卡的目标要求需要根据规则文本确定
			if (rulesText.contains("enemy unit")) {
				setupTargetRequirement(TargetType.ENEMY_UNIT);
			} else if (rulesText.contains("friendly unit")) {
				setupTargetRequirement(TargetType.FRIENDLY_UNIT);
			} else if (rulesText.contains("any unit") || rulesText.contains("target unit")) {
				setupTargetRequirement(TargetType.ANY_UNIT);
			} else if (rulesText.contains("enemy avatar")) {
				setupTargetRequirement(TargetType.ENEMY_AVATAR);
			} else if (rulesText.contains("your avatar") || rulesText.contains("friendly avatar")) {
				setupTargetRequirement(TargetType.FRIENDLY_AVATAR);
			} else {
				// 默认为无目标
				setupTargetRequirement(TargetType.NONE);
			}
		}
	}

	/**
	 * 设置卡牌目标要求
	 * @param targetType 目标类型
	 */
	@JsonIgnore
	protected void setupTargetRequirement(TargetType targetType) {
		GameState gameState = GameState.getInstance();

		switch (targetType) {
			case NONE:
				targetRequirement = null;  // 无目标
				break;
			case FRIENDLY_UNIT:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getOwner() == owner && unit.getUnitType() != Unit.UnitType.AVATAR;
				});
				break;
			case ENEMY_UNIT:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getOwner() != owner && unit.getUnitType() != Unit.UnitType.AVATAR;
				});
				break;
			case ANY_UNIT:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getUnitType() != Unit.UnitType.AVATAR;
				});
				break;
			case FRIENDLY_AVATAR:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getOwner() == owner && unit.getUnitType() == Unit.UnitType.AVATAR;
				});
				break;
			case ENEMY_AVATAR:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getOwner() != owner && unit.getUnitType() == Unit.UnitType.AVATAR;
				});
				break;
			case ANY_AVATAR:
				targetRequirement = new BasicTargetRequirement(tile -> {
					Unit unit = gameState.getUnitAtTile(tile);
					return unit != null && unit.getUnitType() == Unit.UnitType.AVATAR;
				});
				break;
			case EMPTY_TILE:
				targetRequirement = new BasicTargetRequirement(tile -> gameState.getUnitAtTile(tile) == null);
				break;
			case ADJACENT_TILE:
				targetRequirement = new AdjacentTileTargetRequirement(owner);
				break;
		}
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
		// 但可以提供一些通用的处理逻辑

		if (isCreature) {
			// 如果是生物卡，创建并放置单位
			Unit unit = createUnit(targetTile);
			if (unit != null) {
				unit.setOwner(owner);
				gameState.addUnit(unit, targetTile);

				// 触发入场效果
				triggerAbility("OpeningGambit", gameState, unit, targetTile);
			}
		} else {
			// 如果是法术卡，执行法术效果
			for (CardAbility ability : abilities.values()) {
				ability.applyEffect(gameState, targetTile);
			}
		}
	}

	/**
	 * 创建此卡牌对应的单位
	 * @param targetTile 目标位置
	 * @return 创建的单位实例
	 */
	@JsonIgnore
	protected Unit createUnit(Tile targetTile) {
		if (!isCreature || unitConfig == null) {
			return null;
		}

		// 使用BasicObjectBuilders加载单位
		Unit unit = BasicObjectBuilders.loadUnit(unitConfig, id, Unit.class);

		// 设置战斗属性
		if (bigCard != null) {
			unit.setCombatStats(bigCard.getAttack(), bigCard.getHealth());
		}

		// 为单位添加能力
		for (String abilityName : abilityNames) {
			addAbilityToUnit(unit, abilityName);
		}

		return unit;
	}

	/**
	 * 为单位添加能力
	 * @param unit 目标单位
	 * @param abilityName 能力名称
	 */
	@JsonIgnore
	protected void addAbilityToUnit(Unit unit, String abilityName) {
		// 创建对应的单位能力实例
		Unit.Ability ability = createUnitAbility(abilityName);
		if (ability != null) {
			unit.addAbility(ability);
		}
	}

	/**
	 * 创建单位能力实例
	 * @param abilityName 能力名称
	 * @return 单位能力实例
	 */
	@JsonIgnore
	protected Unit.Ability createUnitAbility(String abilityName) {
		// 基于能力名称创建相应的能力实例
		switch (abilityName) {
			case "Provoke":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Provoke";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 嘲讽能力的效果在GameState的移动和攻击逻辑中处理
					}
				};

			case "Flying":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Flying";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 飞行能力的效果在GameState的移动逻辑中处理
					}
				};

			case "Rush":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Rush";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 冲锋能力允许单位在被召唤的回合就可以行动
					}
				};

			case "Deathwatch":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Deathwatch";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 死亡监视效果在GameState中处理单位死亡时触发
					}
				};

			case "Zeal":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Zeal";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 热诚效果在玩家的头像受到伤害时触发
					}
				};

			case "Airdrop":
				return new Unit.Ability() {
					@Override
					public String getName() {
						return "Airdrop";
					}

					@Override
					public void applyEffect(Unit unit) {
						// 空投能力允许单位被放置在棋盘上的任何位置
					}
				};

			default:
				return null;
		}
	}

	/**
	 * 触发指定的能力
	 * @param abilityName 能力名称
	 * @param gameState 当前游戏状态
	 * @param unit 触发能力的单位
	 * @param tile 目标位置
	 */
	@JsonIgnore
	public void triggerAbility(String abilityName, GameState gameState, Unit unit, Tile tile) {
		if (abilities.containsKey(abilityName)) {
			abilities.get(abilityName).applyEffect(gameState, tile);
		}
	}

	/**
	 * 添加卡牌能力名称
	 * @param abilityName 能力名称
	 */
	@JsonIgnore
	public void addAbilityName(String abilityName) {
		if (!abilityNames.contains(abilityName)) {
			abilityNames.add(abilityName);
		}
	}

	/**
	 * 添加卡牌能力
	 * @param ability 能力对象
	 */
	@JsonIgnore
	public void addAbility(CardAbility ability) {
		abilities.put(ability.getName(), ability);
		addAbilityName(ability.getName());
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
		parseAbilitiesFromRulesText();
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
	public List<String> getAbilityNames() {
		return new ArrayList<>(abilityNames);
	}

	@JsonIgnore
	public Map<String, CardAbility> getAbilities() {
		return new HashMap<>(abilities);
	}

	@JsonIgnore
	public TargetRequirement getTargetRequirement() {
		return targetRequirement;
	}

	@JsonIgnore
	public int getHandPosition() {
		return handPosition;
	}

	@JsonIgnore
	public void setHandPosition(int handPosition) {
		this.handPosition = handPosition;
	}

	@JsonIgnore
	public boolean hasAbility(String abilityName) {
		return abilityNames.contains(abilityName);
	}

	/**
	 * 卡牌能力接口，定义卡牌的特殊能力
	 */
	public interface CardAbility {
		String getName();
		void applyEffect(GameState gameState, Tile targetTile);
	}

	/**
	 * 目标要求接口，定义卡牌目标选择的要求
	 */
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

			// 遍历所有棋盘位置
			for (int x = 0; x < gameState.getBoardWidth(); x++) {
				for (int y = 0; y < gameState.getBoardHeight(); y++) {
					Tile tile = gameState.getTile(x, y);
					if (tile != null && targetFilter.test(tile)) {
						validTargets.add(tile);
					}
				}
			}

			return validTargets;
		}
	}

	/**
	 * 相邻格子目标要求实现类，筛选出与友方单位相邻的空格子
	 */
	public static class AdjacentTileTargetRequirement implements TargetRequirement {
		private Player owner;

		public AdjacentTileTargetRequirement(Player owner) {
			this.owner = owner;
		}

		@Override
		public boolean isValidTarget(GameState gameState, Tile targetTile) {
			// 检查目标格子是否为空
			if (gameState.getUnitAtTile(targetTile) != null) {
				return false;
			}

			// 检查是否与友方单位相邻
			int tilex = targetTile.getTilex();
			int tiley = targetTile.getTiley();

			// 检查相邻的8个方向
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					if (dx == 0 && dy == 0) continue; // 跳过自身

					int adjacentX = tilex + dx;
					int adjacentY = tiley + dy;

					// 检查坐标是否在棋盘范围内
					if (adjacentX >= 0 && adjacentX < gameState.getBoardWidth() &&
							adjacentY >= 0 && adjacentY < gameState.getBoardHeight()) {

						Tile adjacentTile = gameState.getTile(adjacentX, adjacentY);
						Unit adjacentUnit = gameState.getUnitAtTile(adjacentTile);

						// 如果相邻格子有己方单位，则目标有效
						if (adjacentUnit != null && adjacentUnit.getOwner() == owner) {
							return true;
						}
					}
				}
			}

			return false;
		}

		@Override
		public List<Tile> getValidTargets(GameState gameState) {
			List<Tile> validTargets = new ArrayList<>();

			// 遍历所有棋盘位置
			for (int x = 0; x < gameState.getBoardWidth(); x++) {
				for (int y = 0; y < gameState.getBoardHeight(); y++) {
					Tile tile = gameState.getTile(x, y);
					if (tile != null && isValidTarget(gameState, tile)) {
						validTargets.add(tile);
					}
				}
			}

			return validTargets;
		}
	}
}