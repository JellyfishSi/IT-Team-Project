package utils;

import java.util.ArrayList;
import java.util.List;

import structures.GameState;
import structures.basic.Card;
import structures.basic.Player;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * 目标选择工具类，处理卡牌和能力的目标选择逻辑
 * 包括目标验证、范围计算和特殊目标规则
 *
 * @author Your Team
 */
public class TargetingUtility {

    /**
     * 获取卡牌的有效目标位置
     * @param card 卡牌
     * @param gameState 当前游戏状态
     * @return 有效目标位置列表
     */
    public static List<Tile> getValidTargets(Card card, GameState gameState) {
        List<Tile> validTargets = new ArrayList<>();

        // 如果卡牌为空，返回空列表
        if (card == null) {
            return validTargets;
        }

        Player owner = card.getOwner();

        // 根据卡牌类型和效果确定有效目标
        if (card.isCreature()) {
            // 生物卡的目标是空格子
            validTargets = getValidCreatureCardTargets(card, gameState);
        } else {
            // 法术卡的目标根据效果确定
            validTargets = getValidSpellCardTargets(card, gameState);
        }

        return validTargets;
    }

    /**
     * 获取生物卡的有效目标位置
     * @param card 生物卡
     * @param gameState 当前游戏状态
     * @return 有效目标位置列表
     */
    private static List<Tile> getValidCreatureCardTargets(Card card, GameState gameState) {
        List<Tile> validTargets = new ArrayList<>();
        Player owner = card.getOwner();

        // 检查卡牌是否有空投能力
        boolean hasAirdrop = card.hasAbility("Airdrop");

        // 空投能力允许放置在任何空格子上
        if (hasAirdrop) {
            // 遍历所有格子
            for (int x = 0; x < gameState.getBoardWidth(); x++) {
                for (int y = 0; y < gameState.getBoardHeight(); y++) {
                    Tile tile = gameState.getTile(x, y);
                    // 检查是否为空格子
                    if (tile != null && gameState.getUnitAtTile(tile) == null) {
                        validTargets.add(tile);
                    }
                }
            }
        } else {
            // 普通单位只能放置在友方单位附近
            for (Unit unit : gameState.getAllUnits()) {
                // 只考虑友方单位
                if (unit.getOwner() == owner) {
                    // 获取周围的空格子
                    List<Tile> adjacentTiles = getAdjacentEmptyTiles(unit, gameState);
                    validTargets.addAll(adjacentTiles);
                }
            }
        }

        return validTargets;
    }

    /**
     * 获取法术卡的有效目标位置
     * @param card 法术卡
     * @param gameState 当前游戏状态
     * @return 有效目标位置列表
     */
    private static List<Tile> getValidSpellCardTargets(Card card, GameState gameState) {
        List<Tile> validTargets = new ArrayList<>();
        String cardName = card.getCardname();
        Player owner = card.getOwner();

        // 根据不同的法术卡名称确定目标位置
        if (cardName.equalsIgnoreCase("Truestrike") || cardName.equalsIgnoreCase("Beam Shock")) {
            // 对敌方单位的法术
            validTargets = getEnemyUnitTiles(owner, gameState);
        } else if (cardName.equalsIgnoreCase("Sundrop Elixir")) {
            // 对友方单位的治疗法术
            validTargets = getFriendlyUnitTiles(owner, gameState);
        } else if (cardName.equalsIgnoreCase("Dark Terminus")) {
            // 对敌方非头像单位的法术
            validTargets = getEnemyNonAvatarUnitTiles(owner, gameState);
        } else if (cardName.equalsIgnoreCase("Wraithling Swarm")) {
            // 召唤法术，目标是任何友方单位附近的空格子
            for (Unit unit : gameState.getAllUnits()) {
                if (unit.getOwner() == owner) {
                    validTargets.addAll(getAdjacentEmptyTiles(unit, gameState));
                }
            }
        } else if (cardName.equalsIgnoreCase("Horn of the Forsaken")) {
            // 装备法术，目标是玩家头像
            Unit avatar = owner.getAvatar();
            if (avatar != null) {
                Tile avatarTile = gameState.getTile(avatar.getPosition().getTilex(), avatar.getPosition().getTiley());
                if (avatarTile != null) {
                    validTargets.add(avatarTile);
                }
            }
        } else {
            // 默认情况，可能是无目标法术或特殊法术
            // 这里可以根据需要扩展
        }

        return validTargets;
    }

    /**
     * 获取玩家所有敌方单位所在的格子
     * @param player 玩家
     * @param gameState 当前游戏状态
     * @return 敌方单位所在的格子列表
     */
    public static List<Tile> getEnemyUnitTiles(Player player, GameState gameState) {
        List<Tile> enemyTiles = new ArrayList<>();

        for (Unit unit : gameState.getAllUnits()) {
            // 检查是否是敌方单位
            if (unit.getOwner() != player) {
                // 获取单位所在的格子
                Position pos = unit.getPosition();
                Tile tile = gameState.getTile(pos.getTilex(), pos.getTiley());
                if (tile != null) {
                    enemyTiles.add(tile);
                }
            }
        }

        return enemyTiles;
    }

    /**
     * 获取玩家所有友方单位所在的格子
     * @param player 玩家
     * @param gameState 当前游戏状态
     * @return 友方单位所在的格子列表
     */
    public static List<Tile> getFriendlyUnitTiles(Player player, GameState gameState) {
        List<Tile> friendlyTiles = new ArrayList<>();

        for (Unit unit : gameState.getAllUnits()) {
            // 检查是否是友方单位
            if (unit.getOwner() == player) {
                // 获取单位所在的格子
                Position pos = unit.getPosition();
                Tile tile = gameState.getTile(pos.getTilex(), pos.getTiley());
                if (tile != null) {
                    friendlyTiles.add(tile);
                }
            }
        }

        return friendlyTiles;
    }

    /**
     * 获取玩家所有敌方非头像单位所在的格子
     * @param player 玩家
     * @param gameState 当前游戏状态
     * @return 敌方非头像单位所在的格子列表
     */
    public static List<Tile> getEnemyNonAvatarUnitTiles(Player player, GameState gameState) {
        List<Tile> enemyTiles = new ArrayList<>();

        for (Unit unit : gameState.getAllUnits()) {
            // 检查是否是敌方非头像单位
            if (unit.getOwner() != player && unit.getUnitType() != Unit.UnitType.AVATAR) {
                // 获取单位所在的格子
                Position pos = unit.getPosition();
                Tile tile = gameState.getTile(pos.getTilex(), pos.getTiley());
                if (tile != null) {
                    enemyTiles.add(tile);
                }
            }
        }

        return enemyTiles;
    }

    /**
     * 获取单位周围的空格子
     * @param unit 中心单位
     * @param gameState 当前游戏状态
     * @return 周围的空格子列表
     */
    public static List<Tile> getAdjacentEmptyTiles(Unit unit, GameState gameState) {
        List<Tile> emptyTiles = new ArrayList<>();
        Position pos = unit.getPosition();
        int tilex = pos.getTilex();
        int tiley = pos.getTiley();

        // 检查周围8个方向
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // 跳过自身

                int newX = tilex + dx;
                int newY = tiley + dy;

                if (gameState.isValidPosition(newX, newY)) {
                    Tile tile = gameState.getTile(newX, newY);
                    if (tile != null && gameState.getUnitAtTile(tile) == null) {
                        emptyTiles.add(tile);
                    }
                }
            }
        }

        return emptyTiles;
    }

    /**
     * 检查一个位置是否在另一个位置的指定范围内
     * @param x1 第一个位置的X坐标
     * @param y1 第一个位置的Y坐标
     * @param x2 第二个位置的X坐标
     * @param y2 第二个位置的Y坐标
     * @param range 范围值
     * @return 如果在范围内则返回true
     */
    public static boolean isInRange(int x1, int y1, int x2, int y2, int range) {
        // 使用切比雪夫距离（国际象棋距离）
        int distance = Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
        return distance <= range;
    }

    /**
     * 检查两个单位是否相邻
     * @param unit1 第一个单位
     * @param unit2 第二个单位
     * @return 如果相邻则返回true
     */
    public static boolean areUnitsAdjacent(Unit unit1, Unit unit2) {
        if (unit1 == null || unit2 == null) {
            return false;
        }

        Position pos1 = unit1.getPosition();
        Position pos2 = unit2.getPosition();

        int x1 = pos1.getTilex();
        int y1 = pos1.getTiley();
        int x2 = pos2.getTilex();
        int y2 = pos2.getTiley();

        // 检查是否在相邻的8个方向之一
        return Math.abs(x1 - x2) <= 1 && Math.abs(y1 - y2) <= 1 && !(x1 == x2 && y1 == y2);
    }

    /**
     * 获取目标周围指定范围内的所有格子
     * @param centerX 中心X坐标
     * @param centerY 中心Y坐标
     * @param range 范围值
     * @param gameState 当前游戏状态
     * @return 范围内的格子列表
     */
    public static List<Tile> getTilesInRange(int centerX, int centerY, int range, GameState gameState) {
        List<Tile> tilesInRange = new ArrayList<>();

        for (int x = centerX - range; x <= centerX + range; x++) {
            for (int y = centerY - range; y <= centerY + range; y++) {
                if (gameState.isValidPosition(x, y)) {
                    Tile tile = gameState.getTile(x, y);
                    if (tile != null) {
                        tilesInRange.add(tile);
                    }
                }
            }
        }

        return tilesInRange;
    }

    /**
     * 获取单位的随机有效目标
     * @param sourceUnit 源单位
     * @param targetType 目标类型（友方、敌方等）
     * @param gameState 当前游戏状态
     * @return 随机目标，如果没有有效目标则返回null
     */
    public static Unit getRandomValidTarget(Unit sourceUnit, Card.TargetType targetType, GameState gameState) {
        List<Unit> validTargets = new ArrayList<>();
        Player owner = sourceUnit.getOwner();

        for (Unit unit : gameState.getAllUnits()) {
            // 根据目标类型过滤
            switch (targetType) {
                case FRIENDLY_UNIT:
                    if (unit.getOwner() == owner && unit.getUnitType() != Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                case ENEMY_UNIT:
                    if (unit.getOwner() != owner && unit.getUnitType() != Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                case FRIENDLY_AVATAR:
                    if (unit.getOwner() == owner && unit.getUnitType() == Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                case ENEMY_AVATAR:
                    if (unit.getOwner() != owner && unit.getUnitType() == Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                case ANY_UNIT:
                    if (unit.getUnitType() != Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                case ANY_AVATAR:
                    if (unit.getUnitType() == Unit.UnitType.AVATAR) {
                        validTargets.add(unit);
                    }
                    break;
                default:
                    break;
            }
        }

        // 如果有有效目标，随机选择一个
        if (!validTargets.isEmpty()) {
            java.util.Random random = new java.util.Random();
            int index = random.nextInt(validTargets.size());
            return validTargets.get(index);
        }

        return null;
    }
}