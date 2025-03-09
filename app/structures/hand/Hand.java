package structures.hand;

import java.util.ArrayList;
import java.util.List;

import structures.basic.Card;
import structures.basic.Player;

/**
 * 手牌类，管理玩家当前持有的卡牌。
 * 处理手牌上限、添加和移除卡牌等功能。
 *
 * @author Your Team
 */
public class Hand {

    public static final int MAX_HAND_SIZE = 6; // 手牌上限为6张

    private List<Card> cards;        // 手牌中的卡牌列表
    private Player owner;            // 手牌所属玩家

    /**
     * 默认构造函数
     */
    public Hand() {
        this.cards = new ArrayList<>();
    }

    /**
     * 带所属玩家的构造函数
     * @param owner 手牌所属玩家
     */
    public Hand(Player owner) {
        this();
        this.owner = owner;
    }

    /**
     * 添加卡牌到手牌
     * @param card 要添加的卡牌
     * @return 如果成功添加返回true，手牌已满则返回false
     */
    public boolean addCard(Card card) {
        if (isFull()) {
            return false;
        }

        if (card != null) {
            card.setOwner(owner); // 确保卡牌所属玩家正确设置
            cards.add(card);
            return true;
        }

        return false;
    }

    /**
     * 添加多张卡牌到手牌，直到手牌满或添加完所有卡牌
     * @param newCards 要添加的卡牌列表
     * @return 成功添加的卡牌数量
     */
    public int addCards(List<Card> newCards) {
        int addedCount = 0;

        for (Card card : newCards) {
            if (addCard(card)) {
                addedCount++;
            } else {
                break; // 手牌已满，停止添加
            }
        }

        return addedCount;
    }

    /**
     * 从手牌中移除卡牌
     * @param card 要移除的卡牌
     * @return 如果成功移除返回true
     */
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    /**
     * 从手牌中移除指定位置的卡牌
     * @param position 卡牌位置(0-5)
     * @return 被移除的卡牌，如果位置无效则返回null
     */
    public Card removeCard(int position) {
        if (position < 0 || position >= cards.size()) {
            return null;
        }

        return cards.remove(position);
    }

    /**
     * 获取指定位置的卡牌(不移除)
     * @param position 卡牌位置(0-5)
     * @return 位置上的卡牌，如果位置无效则返回null
     */
    public Card getCard(int position) {
        if (position < 0 || position >= cards.size()) {
            return null;
        }

        return cards.get(position);
    }

    /**
     * 检查手牌是否包含指定卡牌
     * @param card 要检查的卡牌
     * @return 如果手牌中包含该卡牌则返回true
     */
    public boolean containsCard(Card card) {
        return cards.contains(card);
    }

    /**
     * 根据卡牌名称查找手牌
     * @param cardName 卡牌名称
     * @return 找到的卡牌，如果不存在则返回null
     */
    public Card findCardByName(String cardName) {
        for (Card card : cards) {
            if (card.getCardname().equals(cardName)) {
                return card;
            }
        }
        return null;
    }

    /**
     * 根据卡牌ID查找手牌
     * @param cardId 卡牌ID
     * @return 找到的卡牌，如果不存在则返回null
     */
    public Card findCardById(int cardId) {
        for (Card card : cards) {
            if (card.getId() == cardId) {
                return card;
            }
        }
        return null;
    }

    /**
     * 获取卡牌在手牌中的位置
     * @param card 要查找的卡牌
     * @return 卡牌位置(0-5)，如果不存在则返回-1
     */
    public int getCardPosition(Card card) {
        return cards.indexOf(card);
    }

    /**
     * 清空手牌
     */
    public void clear() {
        cards.clear();
    }

    /**
     * 检查手牌是否为空
     * @return 如果手牌为空则返回true
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * 检查手牌是否已满
     * @return 如果手牌已达到上限则返回true
     */
    public boolean isFull() {
        return cards.size() >= MAX_HAND_SIZE;
    }

    /**
     * 获取手牌中卡牌数量
     * @return 手牌中的卡牌数量
     */
    public int size() {
        return cards.size();
    }

    /**
     * 获取当前剩余手牌空间
     * @return 还可以添加的卡牌数量
     */
    public int remainingSpace() {
        return MAX_HAND_SIZE - cards.size();
    }

    /**
     * 获取手牌中的所有卡牌
     * @return 手牌中的卡牌列表（防御性复制）
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * 获取手牌所属玩家
     * @return 手牌所属玩家
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * 设置手牌所属玩家
     * @param owner 手牌所属玩家
     */
    public void setOwner(Player owner) {
        this.owner = owner;

        // 更新所有卡牌的所属玩家
        for (Card card : cards) {
            card.setOwner(owner);
        }
    }

    /**
     * 根据法力值过滤可用的卡牌
     * @param availableMana 可用法力值
     * @return 可以使用的卡牌列表
     */
    public List<Card> getPlayableCards(int availableMana) {
        List<Card> playableCards = new ArrayList<>();

        for (Card card : cards) {
            if (card.getManacost() <= availableMana) {
                playableCards.add(card);
            }
        }

        return playableCards;
    }

    /**
     * 获取指定类型的卡牌
     * @param isCreature 是否为生物卡
     * @return 指定类型的卡牌列表
     */
    public List<Card> getCardsByType(boolean isCreature) {
        List<Card> filteredCards = new ArrayList<>();

        for (Card card : cards) {
            if (card.isCreature() == isCreature) {
                filteredCards.add(card);
            }
        }

        return filteredCards;
    }

    /**
     * 游戏显示位置转换为内部数组索引(对应PlayingHand位置0-5)
     * @param handPosition 游戏中的手牌位置(1-6)
     * @return 内部数组索引(0-5)
     */
    public static int gamePositionToIndex(int handPosition) {
        // 游戏位置从1开始，内部索引从0开始
        return handPosition - 1;
    }

    /**
     * 内部数组索引转换为游戏显示位置
     * @param index 内部数组索引(0-5)
     * @return 游戏中的手牌位置(1-6)
     */
    public static int indexToGamePosition(int index) {
        // 内部索引从0开始，游戏位置从1开始
        return index + 1;
    }

    /**
     * 根据游戏中的手牌位置获取卡牌
     * @param handPosition 游戏中的手牌位置(1-6)
     * @return 位置上的卡牌，如果位置无效则返回null
     */
    public Card getCardByGamePosition(int handPosition) {
        int index = gamePositionToIndex(handPosition);
        return getCard(index);
    }

    /**
     * 根据游戏中的手牌位置移除卡牌
     * @param handPosition 游戏中的手牌位置(1-6)
     * @return 被移除的卡牌，如果位置无效则返回null
     */
    public Card removeCardByGamePosition(int handPosition) {
        int index = gamePositionToIndex(handPosition);
        return removeCard(index);
    }
}