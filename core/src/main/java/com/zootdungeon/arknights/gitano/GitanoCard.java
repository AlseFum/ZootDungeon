package com.zootdungeon.arknights.gitano;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Adrenaline;
import com.zootdungeon.actors.buffs.Barrier;
import com.zootdungeon.actors.buffs.Barkskin;
import com.zootdungeon.actors.buffs.Bless;
import com.zootdungeon.actors.buffs.Blindness;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Cripple;
import com.zootdungeon.actors.buffs.Daze;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Haste;
import com.zootdungeon.actors.buffs.Hex;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.buffs.Regeneration;
import com.zootdungeon.actors.buffs.Recharging;
import com.zootdungeon.actors.buffs.Roots;
import com.zootdungeon.actors.buffs.Slow;
import com.zootdungeon.actors.buffs.Vulnerable;
import com.zootdungeon.actors.buffs.Weakness;
import com.zootdungeon.actors.buffs.WellFed;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.SpellSprite;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.scrolls.ScrollOfTeleportation;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class GitanoCard extends Item {

	public static final String AC_DRAW = "DRAW";

	private static final String BUNDLE_DECK_TYPE = "deck_type";

	/**
	 * 本局固定牌组：{@code -1} 尚未掷骰；{@code 0} 扑克；{@code 1} 大阿卡纳；{@code 2} 小阿卡纳。
	 * 通过 {@link #storeInBundle}/{@link #restoreFromBundle} 与存档同步。
	 */
	public static int deckType = -1;

	/** 新开局或未从存档恢复时清空牌组类型（例如读档前会先 reset 再写入 bundle）。 */
	public static void resetDeckType() {
		deckType = -1;
	}

	private static final String[] PLAYING_SUITS = {"spades", "hearts", "diamonds", "clubs"};
	private static final String[] PLAYING_RANKS = {"ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king"};
	private static final String[] MINOR_SUITS = {"wands", "cups", "swords", "pentacles"};
	private static final String[] MINOR_RANKS = {"ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "page", "knight", "queen", "king"};
	private static final String[] MAJOR_ARCANA = {
			"fool", "magician", "high_priestess", "empress", "emperor", "hierophant",
			"lovers", "chariot", "strength", "hermit", "wheel_of_fortune", "justice",
			"hanged_man", "death", "temperance", "devil", "tower", "star", "moon",
			"sun", "judgement", "world"
	};

	private static final ArrayList<BuffInfo> FORTUNE_BUFFS = new ArrayList<>();

	static {
		SpriteRegistry.texture("sheet.cola.gitano_card", "cola/gitano_card.png")
				.grid(32, 32)
				.label("gitano_card");

		FORTUNE_BUFFS.add(new BuffInfo(Haste.class, Haste.DURATION));
		FORTUNE_BUFFS.add(new BuffInfo(Invisibility.class, Invisibility.DURATION));
		FORTUNE_BUFFS.add(new BuffInfo(Regeneration.class, 20f));
		FORTUNE_BUFFS.add(new BuffInfo(Bless.class, Bless.DURATION));
		FORTUNE_BUFFS.add(new BuffInfo(Barkskin.class, 20f));
		FORTUNE_BUFFS.add(new BuffInfo(Adrenaline.class, Adrenaline.DURATION));
		FORTUNE_BUFFS.add(new BuffInfo(WellFed.class, 300f));
		FORTUNE_BUFFS.add(new BuffInfo(Recharging.class, Recharging.DURATION));
	}

	{
		image = SpriteRegistry.byLabel("gitano_card");
		stackable = true;
		defaultAction = AC_DRAW;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(BUNDLE_DECK_TYPE, deckType);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		resetDeckType();
		super.restoreFromBundle(bundle);
		if (bundle.contains(BUNDLE_DECK_TYPE)) {
			deckType = bundle.getInt(BUNDLE_DECK_TYPE);
		}
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_DRAW);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (AC_DRAW.equals(action)) {
			return Messages.get(this, "ac_draw");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (AC_DRAW.equals(action)) {
			use(hero);
		}
	}

	public void use(Hero hero) {
		DeckType deck = resolveDeck();
		CardOutcome outcome;

		switch (deck) {
			case PLAYING_CARDS:
				outcome = drawPlayingCard(hero);
				break;
			case MAJOR_ARCANA:
				outcome = drawMajorArcana(hero);
				break;
			default:
				outcome = drawMinorArcana(hero);
				break;
		}

		GLog.p(Messages.get(this, "draw", outcome.cardName));
		if (outcome.effectMessage != null && !outcome.effectMessage.isEmpty()) {
			GLog.i(outcome.effectMessage);
		}
		SpellSprite.show(hero, outcome.sprite, 1, 1, 0);
		consume(hero);
	}

	/** 本局首次抽牌时掷出并写入 {@link #deckType}，之后固定。 */
	private DeckType resolveDeck() {
		if (deckType < 0) {
			deckType = Random.Int(DeckType.values().length);
		}
		return DeckType.values()[Math.floorMod(deckType, DeckType.values().length)];
	}

	private CardOutcome drawPlayingCard(Hero hero) {
		int suit = Random.Int(PLAYING_SUITS.length);
		int rank = Random.Int(PLAYING_RANKS.length) + 1;
		String cardName = playingCardName(suit, rank);

		switch (PLAYING_SUITS[suit]) {
			case "hearts": {
				int heal = 3 + rank * 2;
				healHero(hero, heal);
				String bless = "";
				if (rank >= 11) {
					applyFlavourBuff(hero, Bless.class, 8f + rank);
					bless = Messages.get(this, "eff_part_bless");
				}
				String barrier = "";
				if (rank == 1 || rank == 13) {
					int sh = 6 + rank;
					grantBarrier(hero, sh);
					barrier = Messages.get(this, "eff_part_barrier_you", sh);
				}
				String effect = Messages.get(this, "eff_play_hearts", heal, bless, barrier);
				return new CardOutcome(cardName, SpellSprite.HASTE, effect);
			}

			case "diamonds": {
				int gold = 8 + rank * 4;
				Dungeon.gold += gold;
				String recharge = "";
				if (rank >= 7) {
					applyBuff(hero, Recharging.class);
					recharge = Messages.get(this, "eff_part_recharging");
				}
				String barrier = "";
				if (rank >= 11) {
					int sh = 4 + rank;
					grantBarrier(hero, sh);
					barrier = Messages.get(this, "eff_part_barrier_you", sh);
				}
				String effect = Messages.get(this, "eff_play_diamonds", gold, recharge, barrier);
				return new CardOutcome(cardName, SpellSprite.CHARGE, effect);
			}

			case "clubs": {
				Mob clubTarget = randomVisibleEnemy(hero);
				if (clubTarget == null) {
					applyFlavourBuff(hero, Adrenaline.class, 6f);
					return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_play_clubs_no_foe"));
				}
				int dmg = 4 + rank * 2;
				damageTarget(clubTarget, dmg);
				String vuln = "";
				if (rank >= 8) {
					applyFlavourBuff(clubTarget, Vulnerable.class, 6f + rank / 2f);
					vuln = Messages.get(this, "eff_part_vulnerable_foe");
				}
				String daze = "";
				if (rank >= 11) {
					applyFlavourBuff(clubTarget, Daze.class, Daze.DURATION);
					daze = Messages.get(this, "eff_part_daze_foe");
				}
				String effect = Messages.get(this, "eff_play_clubs_hit", clubTarget.name(), dmg, vuln, daze);
				return new CardOutcome(cardName, SpellSprite.HASTE, effect);
			}

			default: {
				Mob spadeTarget = randomVisibleEnemy(hero);
				if (rank == 1) {
					ScrollOfTeleportation.teleportChar(hero);
					applyFlavourBuff(hero, Invisibility.class, 5f);
					return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_spade_ace"));
				}
				if (spadeTarget == null) {
					applyFlavourBuff(hero, Invisibility.class, 4f + rank / 2f);
					return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_spade_no_foe"));
				}
				if (rank <= 4) {
					applyFlavourBuff(spadeTarget, Blindness.class, 3f + rank);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_blind", spadeTarget.name()));
				} else if (rank <= 8) {
					applyFlavourBuff(spadeTarget, Slow.class, 4f + rank / 2f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_slow", spadeTarget.name()));
				} else if (rank <= 10) {
					applyFlavourBuff(spadeTarget, Weakness.class, 5f + rank);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_weak", spadeTarget.name()));
				} else if (rank == 11) {
					applyFlavourBuff(spadeTarget, Hex.class, 10f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_hex", spadeTarget.name()));
				} else if (rank == 12) {
					applyFlavourBuff(spadeTarget, Cripple.class, 8f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_cripple", spadeTarget.name()));
				} else {
					applyFlavourBuff(spadeTarget, Roots.class, 4f);
					applyFlavourBuff(spadeTarget, Vulnerable.class, 10f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_spade_king", spadeTarget.name()));
				}
			}
		}
	}

	private CardOutcome drawMajorArcana(Hero hero) {
		int index = Random.Int(MAJOR_ARCANA.length);
		String key = MAJOR_ARCANA[index];
		String cardName = Messages.get(this, "major_" + key);

		switch (index) {
			case 0:
				ScrollOfTeleportation.teleportChar(hero);
				applyFlavourBuff(hero, Invisibility.class, 8f);
				return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_0"));
			case 1:
				applyBuff(hero, Recharging.class);
				applyFlavourBuff(hero, Bless.class, 20f);
				return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_major_1"));
			case 2:
				healHero(hero, 14);
				applyBuff(hero, Regeneration.class);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_2"));
			case 3:
				healHero(hero, 18);
				applyBuff(hero, Barkskin.class);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_3"));
			case 4:
				grantBarrier(hero, 20);
				applyFlavourBuff(hero, Bless.class, 15f);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_4"));
			case 5:
				healHero(hero, 10);
				applyBuff(hero, WellFed.class);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_5"));
			case 6:
				healHero(hero, 12);
				applyFlavourBuff(hero, Haste.class, 8f);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_6"));
			case 7:
				applyFlavourBuff(hero, Haste.class, 12f);
				applyFlavourBuff(hero, Adrenaline.class, 10f);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_7"));
			case 8:
				applyBuff(hero, Barkskin.class);
				applyFlavourBuff(hero, Adrenaline.class, 12f);
				grantBarrier(hero, 10);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_8"));
			case 9:
				applyFlavourBuff(hero, Invisibility.class, 10f);
				healHero(hero, 8);
				return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_9"));
			case 10: {
				String fortune = applyRandomFortuneWithMessage(hero);
				return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_major_10", fortune));
			}
			case 11:
				Mob justiceTarget = randomVisibleEnemy(hero);
				if (justiceTarget == null) {
					applyFlavourBuff(hero, Bless.class, 8f);
					return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_11_no_foe"));
				}
				damageTarget(justiceTarget, 18);
				applyFlavourBuff(justiceTarget, Vulnerable.class, 10f);
				return new CardOutcome(cardName, SpellSprite.VISION,
						Messages.get(this, "eff_major_11_hit", justiceTarget.name()));
			case 12:
				Mob hangedTarget = randomVisibleEnemy(hero);
				if (hangedTarget != null) {
					applyFlavourBuff(hangedTarget, Roots.class, 4f);
				}
				healHero(hero, 14);
				if (hangedTarget == null) {
					return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_12_no_foe"));
				}
				return new CardOutcome(cardName, SpellSprite.VISION,
						Messages.get(this, "eff_major_12", hangedTarget.name()));
			case 13: {
				int n = damageAllVisibleEnemies(hero, 20, true);
				return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_13", n));
			}
			case 14:
				applyBuff(hero, Regeneration.class);
				grantBarrier(hero, 12);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_14"));
			case 15:
				applyFlavourBuff(hero, Adrenaline.class, 14f);
				grantBarrier(hero, 14);
				applyFlavourBuff(hero, Weakness.class, 6f);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_15"));
			case 16: {
				int n = damageAllVisibleEnemies(hero, 12, false);
				applyToAllVisibleEnemies(hero, Daze.class, 4f);
				return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_16", n));
			}
			case 17:
				healHero(hero, 20);
				applyBuff(hero, Recharging.class);
				return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_major_17"));
			case 18:
				applyFlavourBuff(hero, Invisibility.class, 8f);
				Mob moonTarget = randomVisibleEnemy(hero);
				if (moonTarget != null) {
					applyFlavourBuff(moonTarget, Hex.class, 12f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_major_18", moonTarget.name()));
				}
				return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_major_18_no_foe"));
			case 19:
				healHero(hero, 16);
				applyFlavourBuff(hero, Bless.class, 20f);
				applyFlavourBuff(hero, Haste.class, 10f);
				return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_major_19"));
			case 20: {
				int judged = damageAllVisibleEnemies(hero, 16, true);
				int heal = judged > 0 ? judged * 3 : 0;
				if (judged > 0) {
					healHero(hero, heal);
				}
				return new CardOutcome(cardName, SpellSprite.VISION,
						Messages.get(this, "eff_major_20", judged, heal));
			}
			default:
				healHero(hero, 12);
				grantBarrier(hero, 16);
				applyFlavourBuff(hero, Bless.class, 20f);
				applyFlavourBuff(hero, Haste.class, 10f);
				applyBuff(hero, Recharging.class);
				return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_major_21"));
		}
	}

	private CardOutcome drawMinorArcana(Hero hero) {
		int suit = Random.Int(MINOR_SUITS.length);
		int rank = Random.Int(MINOR_RANKS.length) + 1;
		String cardName = minorCardName(suit, rank);

		switch (MINOR_SUITS[suit]) {
			case "wands": {
				Mob wandTarget = randomVisibleEnemy(hero);
				if (wandTarget == null) {
					applyBuff(hero, Recharging.class);
					return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_min_wands_no_foe"));
				}
				if (rank <= 10) {
					int dmg = 5 + rank * 2;
					damageTarget(wandTarget, dmg);
					if (rank == 1) {
						applyFlavourBuff(wandTarget, Daze.class, 3f);
						return new CardOutcome(cardName, SpellSprite.CHARGE,
								Messages.get(this, "eff_min_wands_ace", wandTarget.name(), dmg));
					}
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_wands_num", wandTarget.name(), dmg));
				} else if (rank == 11) {
					damageTarget(wandTarget, 18);
					applyFlavourBuff(wandTarget, Daze.class, 4f);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_wands_page", wandTarget.name()));
				} else if (rank == 12) {
					damageTarget(wandTarget, 20);
					applyFlavourBuff(wandTarget, Vulnerable.class, 10f);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_wands_knight", wandTarget.name()));
				} else if (rank == 13) {
					int n = damageAllVisibleEnemies(hero, 10, false);
					return new CardOutcome(cardName, SpellSprite.CHARGE, Messages.get(this, "eff_min_wands_queen", n));
				} else {
					damageTarget(wandTarget, 26);
					applyFlavourBuff(wandTarget, Daze.class, 5f);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_wands_king", wandTarget.name()));
				}
			}

			case "cups": {
				if (rank <= 10) {
					int heal = 4 + rank * 2;
					healHero(hero, heal);
					if (rank >= 6) {
						grantBarrier(hero, rank);
						return new CardOutcome(cardName, SpellSprite.HASTE,
								Messages.get(this, "eff_min_cups_num_barrier", heal, rank));
					}
					return new CardOutcome(cardName, SpellSprite.HASTE,
							Messages.get(this, "eff_min_cups_num", heal));
				} else if (rank == 11) {
					applyFlavourBuff(hero, Bless.class, 12f);
					return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_min_cups_page"));
				} else if (rank == 12) {
					applyFlavourBuff(hero, Haste.class, 8f);
					return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_min_cups_knight"));
				} else if (rank == 13) {
					healHero(hero, 12);
					applyBuff(hero, Regeneration.class);
					applyFlavourBuff(hero, Bless.class, 10f);
					return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_min_cups_queen"));
				} else {
					healHero(hero, 18);
					grantBarrier(hero, 14);
					applyBuff(hero, WellFed.class);
					return new CardOutcome(cardName, SpellSprite.HASTE, Messages.get(this, "eff_min_cups_king"));
				}
			}

			case "swords": {
				Mob swordTarget = randomVisibleEnemy(hero);
				if (swordTarget == null) {
					applyFlavourBuff(hero, Invisibility.class, 5f);
					return new CardOutcome(cardName, SpellSprite.VISION, Messages.get(this, "eff_min_swords_no_foe"));
				}
				if (rank <= 4) {
					int dmg = 4 + rank;
					damageTarget(swordTarget, dmg);
					applyFlavourBuff(swordTarget, Blindness.class, 2f + rank);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_blind", swordTarget.name(), dmg));
				} else if (rank <= 10) {
					int dmg = 6 + rank;
					damageTarget(swordTarget, dmg);
					applyFlavourBuff(swordTarget, Slow.class, 3f + rank / 2f);
					if (rank >= 8) {
						applyFlavourBuff(swordTarget, Weakness.class, 8f);
						return new CardOutcome(cardName, SpellSprite.VISION,
								Messages.get(this, "eff_min_swords_mid_weak", swordTarget.name(), dmg));
					}
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_mid", swordTarget.name(), dmg));
				} else if (rank == 11) {
					damageTarget(swordTarget, 14);
					applyFlavourBuff(swordTarget, Cripple.class, 8f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_page", swordTarget.name()));
				} else if (rank == 12) {
					damageTarget(swordTarget, 16);
					applyFlavourBuff(swordTarget, Cripple.class, 8f);
					applyFlavourBuff(swordTarget, Vulnerable.class, 8f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_knight", swordTarget.name()));
				} else if (rank == 13) {
					damageTarget(swordTarget, 18);
					applyFlavourBuff(swordTarget, Hex.class, 12f);
					applyFlavourBuff(swordTarget, Weakness.class, 12f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_queen", swordTarget.name()));
				} else {
					damageTarget(swordTarget, 20);
					applyFlavourBuff(swordTarget, Roots.class, 4f);
					applyFlavourBuff(swordTarget, Vulnerable.class, 12f);
					return new CardOutcome(cardName, SpellSprite.VISION,
							Messages.get(this, "eff_min_swords_king", swordTarget.name()));
				}
			}

			default: {
				int gold = 6 + rank * 3;
				Dungeon.gold += gold;
				if (rank <= 10) {
					int sh = 3 + rank;
					grantBarrier(hero, sh);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_pent_num", gold, sh));
				} else if (rank == 11) {
					applyBuff(hero, Barkskin.class);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_pent_page", gold));
				} else if (rank == 12) {
					applyBuff(hero, Recharging.class);
					grantBarrier(hero, 8);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_pent_knight", gold));
				} else if (rank == 13) {
					applyFlavourBuff(hero, Bless.class, 14f);
					grantBarrier(hero, 12);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_pent_queen", gold));
				} else {
					applyBuff(hero, WellFed.class);
					applyBuff(hero, Recharging.class);
					grantBarrier(hero, 16);
					return new CardOutcome(cardName, SpellSprite.CHARGE,
							Messages.get(this, "eff_min_pent_king", gold));
				}
			}
		}
	}

	private String playingCardName(int suitIndex, int rank) {
		return Messages.get(this, "playing_format",
				Messages.get(this, "rank_" + PLAYING_RANKS[rank - 1]),
				Messages.get(this, "playing_suit_" + PLAYING_SUITS[suitIndex]));
	}

	private String minorCardName(int suitIndex, int rank) {
		return Messages.get(this, "minor_format",
				Messages.get(this, "minor_rank_" + MINOR_RANKS[rank - 1]),
				Messages.get(this, "minor_suit_" + MINOR_SUITS[suitIndex]));
	}

	private void consume(Hero hero) {
		quantity--;
		if (quantity <= 0) {
			detach(hero.belongings.backpack);
		} else {
			updateQuickslot();
		}
	}

	private String applyRandomFortuneWithMessage(Hero hero) {
		BuffInfo buffInfo = Random.element(FORTUNE_BUFFS);
		applyBuff(hero, buffInfo.buffClass, buffInfo.duration);
		return Messages.get(buffInfo.buffClass, "name");
	}

	private void applyBuff(Char target, Class<? extends Buff> buffClass) {
		Buff.affect(target, buffClass);
	}

	private void applyBuff(Char target, Class<? extends Buff> buffClass, float duration) {
		if (FlavourBuff.class.isAssignableFrom(buffClass)) {
			@SuppressWarnings("unchecked")
			Class<? extends FlavourBuff> flavourBuffClass = (Class<? extends FlavourBuff>) buffClass;
			Buff.affect(target, flavourBuffClass, duration);
		} else {
			Buff.affect(target, buffClass);
		}
	}

	private void applyFlavourBuff(Char target, Class<? extends FlavourBuff> buffClass, float duration) {
		Buff.affect(target, buffClass, duration);
	}

	private Mob randomVisibleEnemy(Hero hero) {
		ArrayList<Mob> enemies = hero.getVisibleEnemies();
		return enemies.isEmpty() ? null : Random.element(enemies);
	}

	private void damageTarget(Mob mob, int damage) {
		if (mob != null && mob.isAlive()) {
			mob.damage(damage, this);
		}
	}

	private int damageAllVisibleEnemies(Hero hero, int damage, boolean scaled) {
		int affected = 0;
		for (Mob mob : hero.getVisibleEnemies()) {
			if (mob != null && mob.isAlive()) {
				damageTarget(mob, scaled ? damage + hero.lvl / 2 : damage);
				affected++;
			}
		}
		return affected;
	}

	private void applyToAllVisibleEnemies(Hero hero, Class<? extends FlavourBuff> buffClass, float duration) {
		for (Mob mob : hero.getVisibleEnemies()) {
			if (mob != null && mob.isAlive()) {
				applyFlavourBuff(mob, buffClass, duration);
			}
		}
	}

	private void grantBarrier(Hero hero, int shield) {
		Buff.affect(hero, Barrier.class).incShield(shield);
	}

	private void healHero(Hero hero, int amount) {
		hero.HP = Math.min(hero.HT, hero.HP + amount);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	private enum DeckType {
		PLAYING_CARDS,
		MAJOR_ARCANA,
		MINOR_ARCANA
	}

	private static class CardOutcome {
		private final String cardName;
		private final int sprite;
		private final String effectMessage;

		private CardOutcome(String cardName, int sprite, String effectMessage) {
			this.cardName = cardName;
			this.sprite = sprite;
			this.effectMessage = effectMessage;
		}
	}

	private static class BuffInfo {
		private final Class<? extends Buff> buffClass;
		private final float duration;

		private BuffInfo(Class<? extends Buff> buffClass, float duration) {
			this.buffClass = buffClass;
			this.duration = duration;
		}
	}
}
