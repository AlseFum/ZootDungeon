package com.zootdungeon.arknights.gitano;

/**
 * 全局卡牌状态：本局牌组类型由 {@link GitanoCard} 在首次抽牌时掷出，
 * 并通过物品的 {@code storeInBundle} / {@code restoreFromBundle} 与存档同步到该静态字段。
 */
public final class GlobalCard {

	private GlobalCard() {
	}

	/**
	 * 本局固定牌组：{@code -1} 尚未掷骰；{@code 0} 扑克；{@code 1} 大阿卡纳；{@code 2} 小阿卡纳。
	 */
	public static int deckType = -1;

	public static void reset() {
		deckType = -1;
	}
}
