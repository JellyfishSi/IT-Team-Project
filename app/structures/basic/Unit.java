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

import structures.GameState;

/**
 * 单位类，代表游戏棋盘上的一个单位。
 * 包含单位的位置、动画、战斗属性和特殊能力等。
 *
 * @author Dr. Richard McCreadie and Your Team
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
	@JsonIgnore
	private boolean isSummonedThisTurn = false; // 是否在本回合被召唤

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

		// 清除可能会在回合结束时消失的状态
		if (statusFlags.containsKey(UnitStatus.STUNNED)) {
			statusFlags.put(UnitStatus.STUNNED, false);
		}

		// 清除被召唤回合标记
		isSummonedThisTurn = false;
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

		// 如果单位有热诚能力，并且是单位所有者的头像受到伤害，则触发热诚效果
		if (owner != null && unitType != UnitType.AVATAR) {
			Unit avatar = owner.getAvatar();
			if (avatar != null && avatar.getHealth() < avatar.getPreviousHealth()) {
				triggerAbility("Zeal");
			}
		}

		return previousHealth - health;
	}

	@JsonIgnore
	private int previousHealth = -1; // 用于跟踪上一次的生命值，便于检测变化

	/**
	 * 更新上一次的生命值记录
	 * 通常在回合开始或状态更新时调用
	 */
	@JsonIgnore
	public void updatePreviousHealth() {
		this.previousHealth = this.health;
	}

	/**
	 * 获取上一次记录的生命值
	 * @return 上一次的生命值
	 */
	@JsonIgnore
	public int getPreviousHealth() {
		return previousHealth == -1 ? health : previousHealth;
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
	 * 单位死亡时的处理
	 * 触发死亡相关效果
	 * @param gameState 当前游戏状态
	 */
	@JsonIgnore
	public void onDeath(GameState gameState) {
		// 触发场上所有单位的死亡监视效果
		for (Unit unit : gameState.getAllUnits()) {
			if (unit != this && !unit.isDead()) {
				unit.triggerAbility("Deathwatch");
			}
		}
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
	 * 触发指定能力的效果
	 * @param abilityName 能力名称
	 */
	@JsonIgnore
	public void triggerAbility(String abilityName) {
		for (Ability ability : abilities) {
			if (ability.getName().equals(abilityName)) {
				ability.applyEffect(this);
			}
		}
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
		// 晕眩单位不能移动
		if (hasStatus(UnitStatus.STUNNED)) {
			return false;
		}

		// 已经移动过的单位不能再移动
		if (hasStatus(UnitStatus.MOVED)) {
			return false;
		}

		// 本回合被召唤的单位通常不能移动，除非有冲锋能力
		if (isSummonedThisTurn && !hasAbility("Rush")) {
			return false;
		}

		return true;
	}

	/**
	 * 获取单位在特定回合是否可以攻击
	 * 受到晕眩或已经攻击的单位不能攻击
	 * @return 如果可以攻击则返回true
	 */
	@JsonIgnore
	public boolean canAttack() {
		// 晕眩单位不能攻击
		if (hasStatus(UnitStatus.STUNNED)) {
			return false;
		}

		// 已经攻击过的单位不能再攻击
		if (hasStatus(UnitStatus.ATTACKED)) {
			return false;
		}

		// 本回合被召唤的单位通常不能攻击，除非有冲锋能力
		if (isSummonedThisTurn && !hasAbility("Rush")) {
			return false;
		}

		return true;
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

	/**
	 * 单位召唤时的处理
	 * @param gameState 当前游戏状态
	 */
	@JsonIgnore
	public void onSummon(GameState gameState) {
		// 标记为本回合召唤
		isSummonedThisTurn = true;

		// 触发入场效果（如果有）
		if (hasAbility("OpeningGambit")) {
			triggerAbility("OpeningGambit");
		}
	}

	/**
	 * 获取单位是否在本回合被召唤
	 * @return 如果是本回合召唤则返回true
	 */
	@JsonIgnore
	public boolean isSummonedThisTurn() {
		return isSummonedThisTurn;
	}

	/**
	 * 设置单位是否在本回合被召唤
	 * @param isSummonedThisTurn 是否在本回合被召唤
	 */
	@JsonIgnore
	public void setSummonedThisTurn(boolean isSummonedThisTurn) {
		this.isSummonedThisTurn = isSummonedThisTurn;
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
		return new HashSet<>(abilities);
	}

	@JsonIgnore
	public List<Artifact> getEquippedArtifacts() {
		return new ArrayList<>(equippedArtifacts);
	}

	@JsonIgnore
	public Map<UnitStatus, Boolean> getStatusFlags() {
		return new HashMap<>(statusFlags);
	}

	/**
	 * 单位能力接口，定义单位特殊能力
	 */

	public interface Ability {
		/**
		 * 获取能力名称
		 * @return 能力名称
		 */
		String getName();

		/**
		 * 应用能力效果
		 * @param unit 目标单位
		 */
		void applyEffect(Unit unit);
	}

	/**
	 * 嘲讽能力实现
	 */

	public static class ProvokeAbility implements Ability {
		@Override
		public String getName() {
			return "Provoke";
		}

		@Override
		public void applyEffect(Unit unit) {
			// 嘲讽效果主要在GameState中处理移动和攻击逻辑时使用
		}
	}

	/**
	 * 飞行能力实现
	 */

	public static class FlyingAbility implements Ability {
		@Override
		public String getName() {
			return "Flying";
		}

		@Override
		public void applyEffect(Unit unit) {
			// 飞行效果主要在GameState中处理移动逻辑时使用
		}
	}

	/**
	 * 冲锋能力实现
	 */

	public static class RushAbility implements Ability {
		@Override
		public String getName() {
			return "Rush";
		}

		@Override
		public void applyEffect(Unit unit) {
			// 冲锋效果在canMove和canAttack中直接处理
		}
	}

	/**
	 * 死亡监视能力基类
	 * 具体效果由子类实现
	 */

	public static abstract class DeathwatchAbility implements Ability {
		@Override
		public String getName() {
			return "Deathwatch";
		}

		@Override
		public abstract void applyEffect(Unit unit);
	}

	/**
	 * 属性增益死亡监视能力
	 * 用于Shadow Watcher类型的单位
	 */

	public static class StatBuffDeathwatchAbility extends DeathwatchAbility {
		private int attackBuff;
		private int healthBuff;

		public StatBuffDeathwatchAbility(int attackBuff, int healthBuff) {
			this.attackBuff = attackBuff;
			this.healthBuff = healthBuff;
		}

		@Override
		public void applyEffect(Unit unit) {
			unit.buffAttack(attackBuff);
			unit.buffHealth(healthBuff);
		}
	}

	/**
	 * 只增加攻击力的死亡监视能力
	 * 用于Bad Omen类型的单位
	 */

	public static class AttackBuffDeathwatchAbility extends DeathwatchAbility {
		private int attackBuff;

		public AttackBuffDeathwatchAbility(int attackBuff) {
			this.attackBuff = attackBuff;
		}

		@Override
		public void applyEffect(Unit unit) {
			unit.buffAttack(attackBuff);
		}
	}

	/**
	 * 伤害和治疗死亡监视能力
	 * 用于Shadowdancer类型的单位
	 */

	public static class DamageHealDeathwatchAbility extends DeathwatchAbility {
		private int damage;
		private int heal;

		public DamageHealDeathwatchAbility(int damage, int heal) {
			this.damage = damage;
			this.heal = heal;
		}

		@Override
		public void applyEffect(Unit unit) {
			Player owner = unit.getOwner();
			if (owner != null) {
				Player enemy = owner.isHuman() ?
						GameState.getInstance().getAiPlayer() :
						GameState.getInstance().getHumanPlayer();

				if (enemy != null && enemy.getAvatar() != null) {
					enemy.getAvatar().takeDamage(damage);
				}

				owner.heal(heal);
			}
		}
	}

	/**
	 * 召唤死亡监视能力
	 * 用于Bloodmoon Priestess类型的单位
	 */

	public static class SummonDeathwatchAbility extends DeathwatchAbility {
		private String unitToSummon;

		public SummonDeathwatchAbility(String unitToSummon) {
			this.unitToSummon = unitToSummon;
		}

		@Override
		public void applyEffect(Unit unit) {
			GameState gameState = GameState.getInstance();
			gameState.summonAdjacentRandomUnit(unit, unitToSummon);
		}
	}

	/**
	 * 入场效果能力基类
	 * 具体效果由子类实现
	 */

	public static abstract class OpeningGambitAbility implements Ability {
		@Override
		public String getName() {
			return "OpeningGambit";
		}

		@Override
		public abstract void applyEffect(Unit unit);
	}

	/**
	 * 召唤单位入场效果
	 * 用于Gloom Chaser类型的单位
	 */

	public static class SummonOpeningGambitAbility extends OpeningGambitAbility {
		private String unitToSummon;
		private Position relativePosition; // 相对位置

		public SummonOpeningGambitAbility(String unitToSummon, Position relativePosition) {
			this.unitToSummon = unitToSummon;
			this.relativePosition = relativePosition;
		}

		@Override
		public void applyEffect(Unit unit) {
			GameState gameState = GameState.getInstance();
			Position currentPos = unit.getPosition();
			int targetX = currentPos.getTilex() + relativePosition.getTilex();
			int targetY = currentPos.getTiley() + relativePosition.getTiley();

			if (gameState.isValidPosition(targetX, targetY)) {
				Tile targetTile = gameState.getTile(targetX, targetY);
				if (targetTile != null && gameState.getUnitAtTile(targetTile) == null) {
					gameState.summonUnitAt(unitToSummon, targetTile, unit.getOwner());
				}
			}
		}
	}

	/**
	 * 消灭敌方单位入场效果
	 * 用于Nightsorrow Assassin类型的单位
	 */

	public static class DestroyOpeningGambitAbility extends OpeningGambitAbility {
		@Override
		public void applyEffect(Unit unit) {
			GameState gameState = GameState.getInstance();
			List<Unit> validTargets = gameState.getAdjacentEnemyUnitsWithLowHealth(unit);

			if (!validTargets.isEmpty()) {
				// 这里可以实现选择目标的逻辑，现在简单地选择第一个
				Unit target = validTargets.get(0);
				gameState.removeUnit(target);
			}
		}
	}

	/**
	 * 增益友方单位入场效果
	 * 用于Silverguard Squire类型的单位
	 */

	public static class BuffOpeningGambitAbility extends OpeningGambitAbility {
		private int attackBuff;
		private int healthBuff;

		public BuffOpeningGambitAbility(int attackBuff, int healthBuff) {
			this.attackBuff = attackBuff;
			this.healthBuff = healthBuff;
		}

		@Override
		public void applyEffect(Unit unit) {
			GameState gameState = GameState.getInstance();
			List<Unit> validTargets = gameState.getAdjacentFriendlyUnitsInLine(unit);

			for (Unit target : validTargets) {
				target.buffAttack(attackBuff);
				target.buffHealth(healthBuff);
			}
		}
	}

	/**
	 * 热诚能力实现
	 * 当玩家头像受到伤害时触发
	 */

	public static class ZealAbility implements Ability {
		private int attackBuff;

		public ZealAbility(int attackBuff) {
			this.attackBuff = attackBuff;
		}

		@Override
		public String getName() {
			return "Zeal";
		}

		@Override
		public void applyEffect(Unit unit) {
			unit.buffAttack(attackBuff);
		}
	}

	/**
	 * 空投能力实现
	 * 允许单位被放置在棋盘上的任何位置
	 */

	public static class AirdropAbility implements Ability {
		@Override
		public String getName() {
			return "Airdrop";
		}

		@Override
		public void applyEffect(Unit unit) {
			// 空投效果主要在卡牌使用时的目标选择中处理
		}
	}

	/**
	 * 神器类，表示可装备在Avatar上的物品
	 */

	public static class Artifact {
		private String name;
		private int robustness;
		private Map<String, Object> properties = new HashMap<>();
		private ArtifactEffect effect;

		public Artifact(String name, int robustness, ArtifactEffect effect) {
			this.name = name;
			this.robustness = robustness;
			this.effect = effect;
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

		public void triggerEffect(Unit unit, GameEvent event) {
			if (effect != null) {
				effect.onTrigger(unit, event);
			}
		}

		/**
		 * 神器效果接口
		 */
		public interface ArtifactEffect {
			void onTrigger(Unit unit, GameEvent event);
		}
	}

	/**
	 * 游戏事件枚举
	 * 用于触发神器效果
	 */

	public enum GameEvent {
		ON_DAMAGE,      // 受到伤害时
		ON_ATTACK,      // 攻击时
		ON_KILL,        // 击杀单位时
		ON_TURN_START,  // 回合开始时
		ON_TURN_END     // 回合结束时
	}

	// 静态辅助方法，从GameState获取当前游戏状态实例（临时添加，实际实现中需要修改）
	@JsonIgnore
	private static GameState getInstance() {
		// 这里只是为了编译通过，实际应该有获取GameState实例的方法
		return null;
	}
}
