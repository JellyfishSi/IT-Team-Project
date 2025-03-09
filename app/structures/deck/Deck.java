package structures.deck;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import structures.basic.Card;
import structures.basic.Player;

/**
 * 牌库类，管理玩家的卡牌集合。
 * 包含洗牌、抽卡等基本功能。
 *
 * @author Your Team
 */
public class Deck {

    private List<Card> cards;        // 牌库中的卡牌列表
    private Player owner;            // 牌库所属玩家
    private boolean isShuffled;      // 标记牌库是否已洗牌

    /**
     * 默认构造函数
     */
    public Deck() {
        this.cards = new ArrayList<>();
        this.isShuffled = false;
    }

    /**
     * 带所属玩家的构造函数
     * @param owner 牌库所属玩家
     */
    public Deck(Player owner) {
        this();
        this.owner = owner;
    }

    /**
     * 带初始卡牌列表的构造函数
     * @param cards 初始卡牌列表
     * @param owner 牌库所属玩家
     */
    public Deck(List<Card> cards, Player owner) {
        this.cards = new ArrayList<>(cards);
        this.owner = owner;
        this.isShuffled = false;

        // 设置每张卡牌的所属玩家
        for (Card card : this.cards) {
            card.setOwner(owner);
        }
    }

    /**
     * 洗牌
     */
    public void shuffle() {
        Collections.shuffle(cards);
        isShuffled = true;
    }

    /**
     * 抽取牌库顶部的一张卡牌
     * @return 抽到的卡牌，如果牌库为空则返回null
     */
    public Card drawCard() {
        if (cards.isEmpty()) {
            return null;
        }

        return cards.remove(0);
    }

    /**
     * 抽取指定数量的卡牌
     * @param count 要抽取的卡牌数量
     * @return 抽到的卡牌列表，如果牌库为空则返回空列表
     */
    public List<Card> drawCards(int count) {
        List<Card> drawnCards = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Card card = drawCard();
            if (card == null) {
                break;
            }
            drawnCards.add(card);
        }

        return drawnCards;
    }

    /**
     * 添加一张卡牌到牌库顶部
     * @param card 要添加的卡牌
     */
    public void addCardToTop(Card card) {
        if (card != null) {
            card.setOwner(owner); // 确保卡牌所属玩家正确设置
            cards.add(0, card);
        }
    }

    /**
     * 添加一张卡牌到牌库底部
     * @param card 要添加的卡牌
     */
    public void addCardToBottom(Card card) {
        if (card != null) {
            card.setOwner(owner); // 确保卡牌所属玩家正确设置
            cards.add(card);
        }
    }

    /**
     * 添加多张卡牌到牌库
     * @param newCards 要添加的卡牌列表
     */
    public void addCards(List<Card> newCards) {
        for (Card card : newCards) {
            if (card != null) {
                card.setOwner(owner); // 确保卡牌所属玩家正确设置
                cards.add(card);
            }
        }
    }

    /**
     * 随机打乱牌库并添加多张卡牌
     * @param newCards 要添加的卡牌列表
     */
    public void addCardsAndShuffle(List<Card> newCards) {
        addCards(newCards);
        shuffle();
    }

    /**
     * 查找牌库中的特定卡牌（根据名称）
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
     * 移除牌库中的特定卡牌
     * @param card 要移除的卡牌
     * @return 如果成功移除返回true
     */
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    /**
     * 检查牌库是否为空
     * @return 如果牌库为空则返回true
     */
    public boolean isEmpty() {
        return cards.isEmpty();
    }

    /**
     * 获取牌库中剩余的卡牌数量
     * @return 牌库中的卡牌数量
     */
    public int size() {
        return cards.size();
    }

    /**
     * 获取牌库所属玩家
     * @return 牌库所属玩家
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * 设置牌库所属玩家
     * @param owner 牌库所属玩家
     */
    public void setOwner(Player owner) {
        this.owner = owner;

        // 更新所有卡牌的所属玩家
        for (Card card : cards) {
            card.setOwner(owner);
        }
    }

    /**
     * 获取牌库中的所有卡牌
     * @return 牌库中的卡牌列表（防御性复制）
     */
    public List<Card> getCards() {
        return new ArrayList<>(cards);
    }

    /**
     * 设置牌库中的卡牌
     * @param cards 新的卡牌列表
     */
    public void setCards(List<Card> cards) {
        this.cards = new ArrayList<>(cards);

        // 更新所有卡牌的所属玩家
        for (Card card : this.cards) {
            card.setOwner(owner);
        }
    }

    /**
     * 检查牌库是否已洗牌
     * @return 如果已洗牌则返回true
     */
    public boolean isShuffled() {
        return isShuffled;
    }

    /**
     * 重置牌库
     * 清空牌库并重置洗牌状态
     */
    public void reset() {
        cards.clear();
        isShuffled = false;
    }

    /**
     * 从JSON配置文件加载牌库
     * 这个方法在实际实现中可以从配置文件加载预定义的牌库
     * @param deckConfigFile 牌库配置文件路径
     * @param owner 牌库所属玩家
     * @return 创建的牌库对象
     */
    public static Deck loadFromConfig(String deckConfigFile, Player owner) {
        // 在实际实现中，这里应该实现从配置文件加载牌库的逻辑
        // 现在只是一个占位的实现
        return new Deck(owner);
    }
}