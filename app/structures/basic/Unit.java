/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id (this is used by the front-end.
 * Each unit has a current UnitAnimationType, e.g. move,
 * or attack. The position is the physical position on the
 * board. UnitAnimationSet contains the underlying information
 * about the animation frames, while ImageCorrection has
 * information for centering the unit on the tile. 
 * 
 * @author Dr. Richard McCreadie
 *
 */
package structures.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This is a representation of a Unit on the game board.
 * A unit has a unique id, animation data, position on the board,
 * and combat attributes like health and attack.
 *
 * @author Dr. Richard McCreadie
 *
 */
public class Unit {

	@JsonIgnore
	protected static ObjectMapper mapper = new ObjectMapper(); // Jackson Java Object Serializer

	// 单位类型枚举
	public enum UnitType {
		AVATAR,    // 玩家头像单位
		MINION,    // 普通随从单位
		TOKEN      // 衍生物单位（如Wraithling）
	}

	// 单位状态枚举
	public enum UnitStatus {
		MOVED,       // 已经移动
		ATTACKED,    // 已经攻击
		STUNNED,     // 被晕眩
		SILENCED,    // 被沉默
		PROVOKED     // 被嘲讽
	}

	// 现有字段（保持与JSON反序列化兼容）
	int id;
	UnitAnimationType animation;
	Position position;
	UnitAnimationSet animations;
	ImageCorrection correction;

	// 新增战斗属性
	@JsonIgnore
	private int attackValue;           // 攻击力
	@JsonIgnore
	private int health;                // 当前生命值
	@JsonIgnore
	private int maxHealth;             // 最大生命值
	@JsonIgnore
	private UnitType unitType = UnitType.MINION; // 单位类型
	@JsonIgnore
	private Player owner;              // 所属玩家
	@JsonIgnore
	private Set<Ability> abilities = new HashSet<>(); // 特殊能力
	@JsonIgnore
	private Map<UnitStatus, Boolean> statusFlags = new HashMap<>(); // 状态标记
	@JsonIgnore
	private List<Artifact> equippedArtifacts = new ArrayList<>();   // 装备的神器（仅用于Avatar）

	/**
	 * 默认构造函数（保持原有反序列化兼容性）
	 */
	public Unit() {}

	/**
	 * 构造函数（保持原有构造函数兼容性）
	 */
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;

		position = new Position(0, 0, 0, 0);
		this.correction = correction;
		this.animations = animations;
	}

	/**
	 * 构造函数（保持原有构造函数兼容性）
	 */
	public Unit(int id, UnitAnimationSet animations, ImageCorrection correction, Tile currentTile) {
		super();
		this.id = id;
		this.animation = UnitAnimationType.idle;

		position = new Position(currentTile.getXpos(), currentTile.getYpos(), currentTile.getTilex(), currentTile.getTiley());
		this.correction = correction;
		this.animations = animations;
	}

	/**
	 * 构造函数（保持原有构造函数兼容性）
	 */
	public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
				ImageCorrection correction) {
		super();
		this.id = id;
		this.animation = animation;
		this.position = position;
		this.animations = animations;
		this.correction = correction;
	}

	/**
	 * 完整构造函数（包含新增属性）
	 */
	public Unit(int id, UnitAnimationType animation, Position position, UnitAnimationSet animations,
				ImageCorrection correction, int attackValue, int health, UnitType unitType, Player owner) {
		this(id, animation, position, animations, correction);
		this.attackValue = attackValue;
		this.health = health;
		this.maxHealth = health;
		this.unitType = unitType;
		this.owner = owner;
	}

	/**
	 * 重置单位的回合状态（回合开始时调用）
	 */
	@JsonIgnore
	public void resetTurnState() {
		statusFlags.put(UnitStatus.MOVED, false);
		statusFlags.put(UnitStatus.ATTACKED, false);

		// 清除一些可能会在回合结束时消失的状态
		if (statusFlags.containsKey(UnitStatus.STUNNED)) {
			statusFlags.put(UnitStatus.STUNNED, false);
		}
	}

	/**
	 * 设置单位的战斗属性
	 * @param attackValue 攻击力
	 * @param health 生命值
	 */
	@JsonIgnore
	public void setCombatStats(int attackValue, int health) {
		this.attackValue = attackValue;
		this.health = health;
		this.maxHealth = health;
	}

	/**
	 * 对单位造成伤害
	 * @param damage 伤害值
	 * @return 实际造成的伤害
	 */
	@JsonIgnore
	public int takeDamage(int damage) {
		if (damage <= 0) return 0;

		int previousHealth = health;
		health = Math.max(0, health - damage);

		// 如果是Avatar，需要同步到玩家
		if (unitType == UnitType.AVATAR && owner != null) {
			owner.syncHealthWithAvatar();
		}

		return previousHealth - health;
	}

	/**
	 * 治疗单位
	 * @param amount 治疗量
	 * @return 实际治疗量
	 */
	@JsonIgnore
	public int heal(int amount) {
		if (amount <= 0) return 0;

		int previousHealth = health;
		health = Math.min(maxHealth, health + amount);

		// 如果是Avatar，需要同步到玩家
		if (unitType == UnitType.AVATAR && owner != null) {
			owner.syncHealthWithAvatar();
		}

		return health - previousHealth;
	}

	/**
	 * 检查单位是否已死亡
	 * @return 如果生命值为0则返回true
	 */
	@JsonIgnore
	public boolean isDead() {
		return health <= 0;
	}

	/**
	 * 增加单位攻击力
	 * @param amount 增加量
	 */
	@JsonIgnore
	public void buffAttack(int amount) {
		if (amount > 0) {
			attackValue += amount;
		}
	}

	/**
	 * 增加单位最大生命值和当前生命值
	 * @param amount 增加量
	 */
	@JsonIgnore
	public void buffHealth(int amount) {
		if (amount > 0) {
			maxHealth += amount;
			health += amount;

			// 如果是Avatar，需要同步到玩家
			if (unitType == UnitType.AVATAR && owner != null) {
				owner.syncHealthWithAvatar();
			}
		}
	}

	/**
	 * 添加能力到单位
	 * @param ability 能力对象
	 */
	@JsonIgnore
	public void addAbility(Ability ability) {
		abilities.add(ability);
	}

	/**
	 * 删除单位的能力
	 * @param abilityName 能力名称
	 * @return 如果成功移除则返回true
	 */
	@JsonIgnore
	public boolean removeAbility(String abilityName) {
		return abilities.removeIf(a -> a.getName().equals(abilityName));
	}

	/**
	 * 检查单位是否有特定能力
	 * @param abilityName 能力名称
	 * @return 如果有该能力则返回true
	 */
	@JsonIgnore
	public boolean hasAbility(String abilityName) {
		for (Ability ability : abilities) {
			if (ability.getName().equals(abilityName)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 为单位装备神器（仅用于Avatar）
	 * @param artifact 神器对象
	 * @return 如果成功装备则返回true
	 */
	@JsonIgnore
	public boolean equipArtifact(Artifact artifact) {
		if (unitType != UnitType.AVATAR) {
			return false; // 只有Avatar才能装备神器
		}
		equippedArtifacts.add(artifact);
		return true;
	}

	/**
	 * 移除单位装备的神器
	 * @param artifactName 神器名称
	 * @return 如果成功移除则返回true
	 */
	@JsonIgnore
	public boolean removeArtifact(String artifactName) {
		return equippedArtifacts.removeIf(a -> a.getName().equals(artifactName));
	}

	/**
	 * 设置单位状态
	 * @param status 状态类型
	 * @param value 状态值
	 */
	@JsonIgnore
	public void setStatus(UnitStatus status, boolean value) {
		statusFlags.put(status, value);
	}

	/**
	 * 检查单位是否有特定状态
	 * @param status 状态类型
	 * @return 如果有该状态则返回true
	 */
	@JsonIgnore
	public boolean hasStatus(UnitStatus status) {
		return statusFlags.getOrDefault(status, false);
	}

	/**
	 * 获取单位在特定回合是否可以移动
	 * 受到晕眩或已经移动的单位不能移动
	 * @return 如果可以移动则返回true
	 */
	@JsonIgnore
	public boolean canMove() {
		return !hasStatus(UnitStatus.MOVED) && !hasStatus(UnitStatus.STUNNED);
	}

	/**
	 * 获取单位在特定回合是否可以攻击
	 * 受到晕眩或已经攻击的单位不能攻击
	 * @return 如果可以攻击则返回true
	 */
	@JsonIgnore
	public boolean canAttack() {
		return !hasStatus(UnitStatus.ATTACKED) && !hasStatus(UnitStatus.STUNNED);
	}

	/**
	 * 单位移动后的处理
	 */
	@JsonIgnore
	public void onUnitMoved() {
		setStatus(UnitStatus.MOVED, true);
	}

	/**
	 * 单位攻击后的处理
	 */
	@JsonIgnore
	public void onUnitAttacked() {
		setStatus(UnitStatus.ATTACKED, true);
	}

	// 原有的getter和setter方法（保持兼容性）
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public UnitAnimationType getAnimation() {
		return animation;
	}

	public void setAnimation(UnitAnimationType animation) {
		this.animation = animation;
	}

	public ImageCorrection getCorrection() {
		return correction;
	}

	public void setCorrection(ImageCorrection correction) {
		this.correction = correction;
	}

	public Position getPosition() {
		return position;
	}

	public void setPosition(Position position) {
		this.position = position;
	}

	public UnitAnimationSet getAnimations() {
		return animations;
	}

	public void setAnimations(UnitAnimationSet animations) {
		this.animations = animations;
	}

	/**
	 * 设置单位位置为指定tile的位置
	 * @param tile
	 */
	@JsonIgnore
	public void setPositionByTile(Tile tile) {
		position = new Position(tile.getXpos(), tile.getYpos(), tile.getTilex(), tile.getTiley());
	}

	// 新增的getter和setter方法

	@JsonIgnore
	public int getAttackValue() {
		return attackValue;
	}

	@JsonIgnore
	public void setAttackValue(int attackValue) {
		this.attackValue = attackValue;
	}

	@JsonIgnore
	public int getHealth() {
		return health;
	}

	@JsonIgnore
	public void setHealth(int health) {
		this.health = health;
		// 如果是Avatar，需要同步到玩家
		if (unitType == UnitType.AVATAR && owner != null) {
			owner.syncHealthWithAvatar();
		}
	}

	@JsonIgnore
	public int getMaxHealth() {
		return maxHealth;
	}

	@JsonIgnore
	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
		if (health > maxHealth) {
			health = maxHealth;
		}
	}

	@JsonIgnore
	public UnitType getUnitType() {
		return unitType;
	}

	@JsonIgnore
	public void setUnitType(UnitType unitType) {
		this.unitType = unitType;
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
	public Set<Ability> getAbilities() {
		return abilities;
	}

	@JsonIgnore
	public List<Artifact> getEquippedArtifacts() {
		return equippedArtifacts;
	}

	/**
	 * 能力接口，定义单位特殊能力
	 */
	@JsonIgnore
	public interface Ability {
		String getName();
		void applyEffect(Unit unit);
	}

	/**
	 * 神器类，表示可装备在Avatar上的物品
	 */
	@JsonIgnore
	public static class Artifact {
		private String name;
		private int robustness;
		private Map<String, Object> properties = new HashMap<>();

		public Artifact(String name, int robustness) {
			this.name = name;
			this.robustness = robustness;
		}

		public String getName() {
			return name;
		}

		public int getRobustness() {
			return robustness;
		}

		public void reduceRobustness(int amount) {
			robustness = Math.max(0, robustness - amount);
		}

		public void setProperty(String key, Object value) {
			properties.put(key, value);
		}

		public Object getProperty(String key) {
			return properties.get(key);
		}
	}
}
