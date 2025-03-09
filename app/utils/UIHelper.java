package utils;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Card;
import structures.basic.Tile;
import structures.basic.Unit;

import java.util.List;

public class UIHelper {

    /**
     * 清除所有格子的高亮（静态版本，适用于没有保存状态的类）
     * @param out ActorRef用于向前端发送命令
     * @param gameState 当前游戏状态
     */
    public static void clearTileHighlights(ActorRef out, GameState gameState) {
        for (int x = 0; x < gameState.getBoardWidth(); x++) {
            for (int y = 0; y < gameState.getBoardHeight(); y++) {
                Tile tile = gameState.getTile(x, y);
                if (tile != null) {
                    BasicCommands.drawTile(out, tile, 0); // 0表示不高亮
                }
            }
        }
    }

    /**
     * 清除所有格子的高亮（实例方法版本，适用于有保存出参的控制器类）
     * @param controller 持有ActorRef和GameState的控制器
     */
    public static void clearTileHighlights(TileHighlightClearable controller) {
        ActorRef out = controller.getOut();
        GameState gameState = controller.getGameState();
        clearTileHighlights(out, gameState);
    }

    // 定义一个接口，让所有需要清除高亮的控制器实现它
    public interface TileHighlightClearable {
        ActorRef getOut();
        GameState getGameState();
    }

    /**
     * 清除所有选择和高亮
     * @param out ActorRef用于向前端发送命令
     * @param gameState 当前游戏状态
     */
    public static void clearSelections(ActorRef out, GameState gameState) {
        // 清除单位选择
        gameState.setSelectedUnit(null);

        // 清除卡牌选择
        Card previousCard = gameState.getSelectedCard();
        if (previousCard != null) {
            int position = previousCard.getHandPosition();
            if (position > 0) {
                BasicCommands.drawCard(out, previousCard, position, 0); // 0表示不高亮
            }
            gameState.setSelectedCard(null);
        }

        // 清除所有高亮
        clearTileHighlights(out, gameState);
    }

    /**
     * 高亮显示单位的有效移动位置
     * @param out ActorRef用于向前端发送命令
     * @param gameState 当前游戏状态
     * @param unit 要高亮移动范围的单位
     */
    public static void highlightValidMoves(ActorRef out, GameState gameState, Unit unit) {
        List<Tile> validMoves = gameState.getValidMoves(unit);

        for (Tile tile : validMoves) {
            BasicCommands.drawTile(out, tile, 1); // 1表示高亮
        }
    }
}