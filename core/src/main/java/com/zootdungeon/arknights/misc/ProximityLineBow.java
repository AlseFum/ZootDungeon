package com.zootdungeon.arknights.misc;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.Splash;
import com.zootdungeon.items.EquipableItem;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 短程灵弓：交互与 {@link com.zootdungeon.items.weapon.SpiritBow} 相同（选格射击），射程不超过 {@link #MAX_SHOOT_DISTANCE}；
 * 目标在纯上下左右方向且距离 1～2 格时伤害 +60%。
 */
public class ProximityLineBow extends Weapon {

	public static final String AC_SHOOT = "SHOOT";

	/** 与本层 {@link com.zootdungeon.levels.Level#distance} 一致（切比雪夫距离） */
	public static final int MAX_SHOOT_DISTANCE = 3;

	private static final float ORTHO_BONUS = 1.6f;

	private int targetPos;
	static{
		SpriteRegistry.texture("mod:proximitylinebow", "cola/province_bow.png").setXY("province_bow",32,32);
	}
	{
		image = SpriteRegistry.byLabel("province_bow");
		defaultAction = AC_SHOOT;
		usesTargeting = true;

		DLY = 1f;
		RCH = 1;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.remove(EquipableItem.AC_EQUIP);
		actions.add(AC_SHOOT);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (AC_SHOOT.equals(action)) {
			return Messages.get(this, "ac_shoot");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_SHOOT)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(shooter);
		}
	}

	@Override
	public int targetingPos(Hero user, int dst) {
		return knockBolt().targetingPos(user, dst);
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(2, lvl);
	}

	@Override
	public int min(int lvl) {
		return 3 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 10 + 2 * lvl;
	}

	@Override
	public int damageRoll(Char owner) {
		int dmg = augment.damageFactor(super.damageRoll(owner));
		if (owner != null && Dungeon.level != null && hasOrthogonalShortLineBonus(owner.pos, targetPos)) {
			dmg = Math.round(dmg * ORTHO_BONUS);
		}
		return dmg;
	}

	/**
	 * 与英雄同一行或同一列，且曼哈顿距离为 1 或 2（不含斜向）。
	 */
	public static boolean hasOrthogonalShortLineBonus(int from, int to) {
		int w = Dungeon.level.width();
		int fx = from % w, fy = from / w;
		int tx = to % w, ty = to / w;
		int dx = tx - fx, dy = ty - fy;
		if (dx != 0 && dy != 0) {
			return false;
		}
		int steps = Math.abs(dx) + Math.abs(dy);
		return steps == 1 || steps == 2;
	}

	public ProximityBolt knockBolt() {
		return new ProximityBolt();
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target != null) {
				knockBolt().cast(curUser, target);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(ProximityLineBow.class, "prompt");
		}
	};

	public class ProximityBolt extends MissileWeapon {

		{
			image = ItemSpriteSheet.SPIRIT_ARROW;
			hitSound = Assets.Sounds.HIT_ARROW;
			spawnedForEffect = true;
			sticky = false;
		}

		@Override
		public int damageRoll(Char owner) {
			return ProximityLineBow.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return ProximityLineBow.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return ProximityLineBow.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return ProximityLineBow.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return ProximityLineBow.this.STRReq();
		}

		@Override
		protected void onThrow(int cell) {
			Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == curUser) {
				parent = null;
				Splash.at(cell, 0xCC99FFFF, 1);
			} else {
				if (!curUser.shoot(enemy, this)) {
					Splash.at(cell, 0xCC99FFFF, 1);
				}
			}
		}

		@Override
		public void throwSound() {
			Sample.INSTANCE.play(Assets.Sounds.ATK_SPIRITBOW, 1, Random.Float(0.87f, 1.15f));
		}

		@Override
		public void cast(Hero user, int dst) {
			if (Dungeon.level.distance(user.pos, dst) > MAX_SHOOT_DISTANCE) {
				GLog.w(Messages.get(ProximityLineBow.this, "out_of_range"));
				return;
			}
			ProximityLineBow.this.targetPos = throwPos(user, dst);
			super.cast(user, dst);
		}
	}
}
