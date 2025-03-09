package utils;

import java.util.ArrayList;
import java.util.List;

import structures.GameState;
import structures.basic.Position;
import structures.basic.Tile;
import structures.basic.Unit;

/**
 * 移动工具类，处理与单位移动相关的复杂逻辑
 * 包括移动范围计算、路径检查和移动规则验证
 *
 * @author Your Team
 */
public class MovementUtility {

    /**
     * 获取单位可以移动到的所有位置
     * @param unit 要移动的单位
     * @param gameState 当前游戏状态
     * @return 可移动位置列表
     */
    public static List<Tile> getValidMovePositions(Unit unit, GameState gameState) {
        List<Tile> validMoves = new ArrayList<>();

        // 如果单位不能移动，返回空列表
        if (!unit.canMove()) {
            return validMoves;
        }

        Position pos = unit.getPosition();
        int tilex = pos.getTilex();
        int tiley = pos.getTiley();

        // 检查单位是否有特殊移动能力（如飞行）
        if (unit.hasAbility("Flying")) {
            return getAllEmptyTiles(gameState);
        } else if (unit.hasStatus(Unit.UnitStatus.PROVOKED)) {
            // 如果单位被嘲讽，只能在原地或攻击嘲讽者
            return new ArrayList<>();
        } else {
            // 正常移动逻辑
            return getNormalMoveTiles(tilex, tiley, gameState);
        }
    }

    /**
     * 获取棋盘上所有空位置
     * @param gameState 当前游戏状态
     * @return 所有空位置列表
     */
    private static List<Tile> getAllEmptyTiles(GameState gameState) {
        List<Tile> emptyTiles = new ArrayList<>();

        for (int x = 0; x < gameState.getBoardWidth(); x++) {
            for (int y = 0; y < gameState.getBoardHeight(); y++) {
                Tile tile = gameState.getTile(x, y);
                if (tile != null && gameState.getUnitAtTile(tile) == null) {
                    emptyTiles.add(tile);
                }
            }
        }

        return emptyTiles;
    }

    /**
     * 获取普通单位的可移动位置
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param gameState 当前游戏状态
     * @return 可移动位置列表
     */
    private static List<Tile> getNormalMoveTiles(int startX, int startY, GameState gameState) {
        List<Tile> validMoves = new ArrayList<>();

        // 检查四个基本方向（上下左右）2格
        int[][] directions = {
                {-1, 0}, {1, 0}, {0, -1}, {0, 1} // 左右上下
        };

        for (int[] dir : directions) {
            // 检查1格距离
            int x1 = startX + dir[0];
            int y1 = startY + dir[1];
            if (isValidPositionForMove(x1, y1, gameState)) {
                Tile tile = gameState.getTile(x1, y1);
                validMoves.add(tile);

                // 检查2格距离
                int x2 = x1 + dir[0];
                int y2 = y1 + dir[1];
                if (isValidPositionForMove(x2, y2, gameState)) {
                    Tile tile2 = gameState.getTile(x2, y2);
                    validMoves.add(tile2);
                }
            }
        }

        // 检查斜角方向（1格）
        int[][] diagonals = {
                {-1, -1}, {-1, 1}, {1, -1}, {1, 1} // 左上、左下、右上、右下
        };

        for (int[] diag : diagonals) {
            int x = startX + diag[0];
            int y = startY + diag[1];
            if (isValidPositionForMove(x, y, gameState)) {
                Tile tile = gameState.getTile(x, y);
                validMoves.add(tile);
            }
        }

        return validMoves;
    }

    /**
     * 检查位置是否有效（在棋盘范围内且没有单位）
     * @param x X坐标
     * @param y Y坐标
     * @param gameState 当前游戏状态
     * @return 如果位置有效则返回true
     */
    private static boolean isValidPositionForMove(int x, int y, GameState gameState) {
        if (!gameState.isValidPosition(x, y)) {
            return false;
        }

        Tile tile = gameState.getTile(x, y);
        return tile != null && gameState.getUnitAtTile(tile) == null;
    }

    /**
     * 检查从起始位置到目标位置的路径是否被阻挡
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 目标X坐标
     * @param endY 目标Y坐标
     * @param gameState 当前游戏状态
     * @return 如果路径被阻挡则返回true
     */
    public static boolean isPathBlocked(int startX, int startY, int endX, int endY, GameState gameState) {
        // 如果是相邻位置，路径不会被阻挡
        if (Math.abs(startX - endX) <= 1 && Math.abs(startY - endY) <= 1) {
            return false;
        }

        // 如果是直线移动（水平、垂直）
        if (startX == endX || startY == endY) {
            return isLinearPathBlocked(startX, startY, endX, endY, gameState);
        }

        // 对于两格距离的斜角移动，默认路径被阻挡
        return true;
    }

    /**
     * 检查直线路径是否被阻挡
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 目标X坐标
     * @param endY 目标Y坐标
     * @param gameState 当前游戏状态
     * @return 如果路径被阻挡则返回true
     */
    private static boolean isLinearPathBlocked(int startX, int startY, int endX, int endY, GameState gameState) {
        // 水平移动
        if (startY == endY) {
            int step = startX < endX ? 1 : -1;
            for (int x = startX + step; x != endX; x += step) {
                Tile tile = gameState.getTile(x, startY);
                if (tile != null && gameState.getUnitAtTile(tile) != null) {
                    return true; // 路径被阻挡
                }
            }
        }
        // 垂直移动
        else if (startX == endX) {
            int step = startY < endY ? 1 : -1;
            for (int y = startY + step; y != endY; y += step) {
                Tile tile = gameState.getTile(startX, y);
                if (tile != null && gameState.getUnitAtTile(tile) != null) {
                    return true; // 路径被阻挡
                }
            }
        }

        return false; // 路径畅通
    }

    /**
     * 计算两个位置之间的曼哈顿距离
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 目标X坐标
     * @param endY 目标Y坐标
     * @return 两点间的曼哈顿距离
     */
    public static int getManhattanDistance(int startX, int startY, int endX, int endY) {
        return Math.abs(startX - endX) + Math.abs(startY - endY);
    }

    /**
     * 计算两个位置之间的切比雪夫距离（国际象棋距离）
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 目标X坐标
     * @param endY 目标Y坐标
     * @return 两点间的切比雪夫距离
     */
    public static int getChebyshevDistance(int startX, int startY, int endX, int endY) {
        return Math.max(Math.abs(startX - endX), Math.abs(startY - endY));
    }

    /**
     * 检查移动是否有效（结合单位能力和游戏规则）
     * @param unit 要移动的单位
     * @param targetTile 目标位置
     * @param gameState 当前游戏状态
     * @return 如果移动有效则返回true
     */
    public static boolean isValidMove(Unit unit, Tile targetTile, GameState gameState) {
        // 检查单位是否可以移动
        if (!unit.canMove()) {
            return false;
        }

        // 检查目标位置是否已有单位
        if (gameState.getUnitAtTile(targetTile) != null) {
            return false;
        }

        Position currentPos = unit.getPosition();
        int startX = currentPos.getTilex();
        int startY = currentPos.getTiley();
        int endX = targetTile.getTilex();
        int endY = targetTile.getTiley();

        // 检查单位是否有飞行能力
        if (unit.hasAbility("Flying")) {
            // 飞行单位可以移动到任何空位置
            return gameState.getUnitAtTile(targetTile) == null;
        }

        // 检查单位是否被嘲讽
        if (unit.hasStatus(Unit.UnitStatus.PROVOKED)) {
            return false; // 被嘲讽的单位不能移动
        }

        // 检查移动距离
        int distance = getChebyshevDistance(startX, startY, endX, endY);
        if (distance > 2) {
            return false; // 普通单位最多移动2格
        }

        // 如果是斜角移动且距离为2，则无效
        if (startX != endX && startY != endY && distance == 2) {
            return false; // 不允许2格斜角移动
        }

        // 检查路径是否被阻挡
        return !isPathBlocked(startX, startY, endX, endY, gameState);
    }

    /**
     * 获取指定单位周围的所有空位置
     * @param unit 中心单位
     * @param gameState 当前游戏状态
     * @return 周围的空位置列表
     */
    public static List<Tile> getAdjacentEmptyTiles(Unit unit, GameState gameState) {
        List<Tile> emptyTiles = new ArrayList<>();
        Position pos = unit.getPosition();
        int tilex = pos.getTilex();
        int tiley = pos.getTiley();

        // 检查周围8个方向
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // 跳过自身位置

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
     * 获取从起点到终点的移动方向
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param endX 目标X坐标
     * @param endY 目标Y坐标
     * @return 移动方向数组 [dx, dy]
     */
    public static int[] getMoveDirection(int startX, int startY, int endX, int endY) {
        int dx = 0;
        int dy = 0;

        if (startX < endX) dx = 1;
        else if (startX > endX) dx = -1;

        if (startY < endY) dy = 1;
        else if (startY > endY) dy = -1;

        return new int[] {dx, dy};
    }
}