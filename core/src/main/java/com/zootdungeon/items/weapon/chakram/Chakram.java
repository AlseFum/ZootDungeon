package com.zootdungeon.items.weapon.chakram;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.mechanics.Damage;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.MissileSprite;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;

/**
 * 回旋武器：向选定方向沿直线飞出，穿透单位与可通行地形，在撞墙处短暂停留后再沿路径飞回英雄当前格；
 * 去程与回程各对路径上的敌人结算一次物理伤害（使用当前装备的该武器骰伤）。
 * <p>
 * 远端等待通过挂在英雄身上的 {@link ChakramReturn} 消耗 {@link Actor#TICK} 推进时间（与重回旋镖 {@code CircleBack} 同理），
 * 避免 {@code Actor.addDelayed} 在英雄尚未 {@code spend} 时卡住全局 Actor 调度。
 */
public class Chakram extends Weapon {

	/** 抵达远端后，再经过多少个全局时间单位（每次 {@link Actor#TICK}）开始折返 */
	private static final int HOVER_TICKS = 3;

	private transient boolean throwing = false;

	{
		image = ItemSpriteSheet.THROWING_KNIFE;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.2f;

		defaultAction = Item.AC_THROW;
		usesTargeting = true;

		DLY = 1f;
		RCH = 1;
		ACC = 1f;
	}

	@Override
	public void cast(final Hero user, final int dst) {
		if (throwing) {
			return;
		}
		if (!isEquipped(user)) {
			GLog.w("回旋镖必须装备在主手或副手后才能掷出。");
			return;
		}
		if (dst == user.pos) {
			return;
		}

		final int heroPos = user.pos;
		final Ballistica outbound = new Ballistica(heroPos, dst, Ballistica.STOP_SOLID);
		final int endCell = outbound.collisionPos;
		if (outbound.dist <= 0 || endCell == heroPos) {
			GLog.w("无法朝这个方向掷出。");
			return;
		}

		throwing = true;
		user.sprite.zap(endCell);
		user.busy();
		throwSound();

		((MissileSprite) user.sprite.parent.recycle(MissileSprite.class)).reset(
				user.sprite,
				endCell,
				this,
				new Callback() {
					@Override
					public void call() {
						hitAlongPath(user, heroPos, outbound);
						Sample.INSTANCE.play(hitSound, 1f, hitSoundPitch);

						final ItemSprite hover = new ItemSprite(Chakram.this);
						hover.place(endCell);
						user.sprite.parent.add(hover);

						// 先结算去程的「一投」时间，让全局 now 与怪物回合能前进；否则 addDelayed / 调度会僵死
						user.spendAndNext(delayFactor(user));

						Buff.append(user, ChakramReturn.class).setup(Chakram.this, hover, endCell);
					}
				});
	}

	/**
	 * 对 ballistica 路径上从起点下一格到碰撞格（含）之间的每个敌人各打一次。
	 *
	 * @param refPos 用于点射天赋等的参照格（通常掷出时为英雄掷出时的位置）
	 */
	private void hitAlongPath(Hero hero, int refPos, Ballistica b) {
		if (b.path.isEmpty() || b.dist < 1) {
			return;
		}
		for (int i = 1; i <= b.dist && i < b.path.size(); i++) {
			int cell = b.path.get(i);
			Char ch = Actor.findChar(cell);
			if (ch != null && ch != hero && ch.isAlive()) {
				float acc = throwAcc(hero, refPos, ch);
				Damage.physical(hero, ch, 1f, 0f, acc);
			}
		}
	}

	/** 近似投掷武器的远近距命中修正，再乘武器 {@link #accuracyFactor}。 */
	private float throwAcc(Hero hero, int refPos, Char target) {
		float f = accuracyFactor(hero, target);
		if (Dungeon.level.adjacent(refPos, target.pos)) {
			f *= 0.5f + 0.2f * hero.pointsInTalent(Talent.POINT_BLANK);
		} else {
			f *= 1.5f;
		}
		return f;
	}

	@Override
	public float castDelay(Char user, int dst) {
		return delayFactor(user) * 2f;
	}

	@Override
	public int min(int lvl) {
		return 5 + 3 * lvl;
	}

	@Override
	public int max(int lvl) {
		return 10 + 5 * lvl;
	}

	@Override
	public int STRReq(int lvl) {
		return 10 + lvl;
	}

	@Override
	public String name() {
		return "巨型回旋镖";
	}

	@Override
	public String desc() {
		return "沿直线掷出，穿透路径上的敌人，在撞墙后会短暂停留再折返飞回手中；去程与回程都会各造成一次伤害。\n\n"
				+ "_必须装备后才能投掷。_\n\n"
				+ "力量需求：" + STRReq(buffedLvl()) + "\n"
				+ "伤害：" + min() + "-" + max();
	}

	/**
	 * 与 {@link com.zootdungeon.items.weapon.missiles.HeavyBoomerang.CircleBack} 相同思路：用 Buff 每次 {@code spend(TICK)}
	 * 推进时间线，再播放回程导弹。
	 */
	public static class ChakramReturn extends Buff {

		private Chakram chakram;
		private ItemSprite hover;
		private int endCell;
		private int ticksLeft;

		private static final String CHAKRAM = "chakram";
		private static final String END = "end_cell";
		private static final String TICKS = "ticks_left";

		{
			type = buffType.NEUTRAL;
			announced = false;
		}

		public void setup(Chakram weapon, ItemSprite hoverSprite, int end) {
			chakram = weapon;
			hover = hoverSprite;
			endCell = end;
			ticksLeft = HOVER_TICKS;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}

		@Override
		public boolean act() {
			if (chakram == null || target == null || !target.isAlive()) {
				killHover();
				if (chakram != null) {
					chakram.throwing = false;
				}
				detach();
				return true;
			}

			ticksLeft--;
			if (ticksLeft > 0) {
				spend(TICK);
				return true;
			}

			killHover();

			Hero hero = (Hero) target;
			if (!hero.isAlive()) {
				chakram.throwing = false;
				detach();
				return true;
			}

			int returnTo = hero.pos;
			final Ballistica inbound = new Ballistica(endCell, returnTo, Ballistica.STOP_SOLID);
			((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(
					endCell,
					hero.sprite,
					chakram,
					new Callback() {
						@Override
						public void call() {
							chakram.hitAlongPath(hero, returnTo, inbound);
							Sample.INSTANCE.play(chakram.hitSound, 1f, chakram.hitSoundPitch * 0.9f);
							chakram.throwing = false;
							hero.spendAndNext(chakram.castDelay(hero, endCell));
						}
					});

			detach();
			// return false 会让 Actor 线程 wait 且 current 仍指向本 Buff，GameScene 无法 notify（与 CircleBack 导弹里 next() 不同）。
			return true;
		}

		private void killHover() {
			if (hover != null) {
				hover.killAndErase();
				hover = null;
			}
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(CHAKRAM, chakram);
			bundle.put(END, endCell);
			bundle.put(TICKS, ticksLeft);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			chakram = (Chakram) bundle.get(CHAKRAM);
			endCell = bundle.getInt(END);
			ticksLeft = bundle.getInt(TICKS);
			hover = null;
		}
	}
}
