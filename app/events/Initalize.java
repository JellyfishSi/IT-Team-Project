package events;

import com.fasterxml.jackson.databind.JsonNode;

import akka.actor.ActorRef;
import commands.BasicCommands;
import structures.GameState;
import structures.basic.Player;
import structures.basic.Tile;
import structures.basic.Unit;
import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

/**
 * Indicates that both the core game loop in the browser is starting, meaning
 * that it is ready to receive commands from the back-end.
 *
 * {
 *   messageType = "initalize"
 * }
 *
 * @author Dr. Richard McCreadie
 *
 */
public class Initalize implements EventProcessor{

	@Override
	public void processEvent(ActorRef out, GameState gameState, JsonNode message) {
		// 标记游戏已初始化
		gameState.gameInitalised = true;
		gameState.something = true;

		// 初始化棋盘所有格子
		initializeBoard(out, gameState);

		// 在此处可以添加更多初始化代码
		// 例如初始化玩家头像、初始牌库等
	}

	/**
	 * 初始化棋盘所有格子
	 * @param out ActorRef，用于向前端发送命令
	 * @param gameState 游戏状态对象
	 */
	private void initializeBoard(ActorRef out, GameState gameState) {
		// 棋盘大小为9x5
		int boardWidth = gameState.getBoardWidth();
		int boardHeight = gameState.getBoardHeight();

		// 创建并绘制每个格子
		for (int x = 0; x < boardWidth; x++) {
			for (int y = 0; y < boardHeight; y++) {
				// 加载格子
				Tile tile = BasicObjectBuilders.loadTile(x, y);

				// 绘制格子，模式0表示普通未高亮状态
				BasicCommands.drawTile(out, tile, 0);

				// 添加适当的延迟，避免一次性发送过多命令
				try {Thread.sleep(5);} catch (InterruptedException e) {e.printStackTrace();}
			}
		}

		// 在初始化后添加一条通知
		BasicCommands.addPlayer1Notification(out, "棋盘已初始化", 2);
		try {Thread.sleep(2000);} catch (InterruptedException e) {e.printStackTrace();}
	}
}