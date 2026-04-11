package com.zootdungeon.arknights.misc;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mimic;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.Splash;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.scrolls.ScrollOfTeleportation;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.MissileSprite;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 机弩：与 {@link ProximityLineBow} 相同，射击距离上限 3；纯上下左右且曼哈顿 1～2 格时伤害 +60%。
 * 可将弩台部署到视野内任意空格；朝向在部署时由「英雄→落点」位移的主轴取正四向（投掷大方向）。
 * 可在相邻格交互再次「调整朝向」。每回合沿朝向射击最多 3 格，带 zap + 弹道动画。
 */
public class DeployedLineBlade extends Weapon {

	public static final String AC_SHOOT = "SHOOT";
	public static final String AC_DEPLOY = "DEPLOY";

	public static final int MAX_SHOOT_DISTANCE = 3;

	private static final float ORTHO_BONUS = 1.6f;

	private int targetPos;

	static {
		SpriteRegistry.texture("mod:deployedlineblade", "cola/redknife.png").grid(32, 32).label("deployed_line_blade");
	}

	{
		image = SpriteRegistry.byLabel("deployed_line_blade");
		defaultAction = AC_SHOOT;
		usesTargeting = true;
		DLY = 1f;
		RCH = 1;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) {
			actions.add(AC_SHOOT);
			actions.add(AC_DEPLOY);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (AC_SHOOT.equals(action)) {
			return Messages.get(this, "ac_shoot");
		}
		if (AC_DEPLOY.equals(action)) {
			return Messages.get(this, "ac_deploy");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (AC_SHOOT.equals(action)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(shooter);
		} else if (AC_DEPLOY.equals(action)) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(deployer);
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
		if (owner != null && Dungeon.level != null && ProximityLineBow.hasOrthogonalShortLineBonus(owner.pos, targetPos)) {
			dmg = Math.round(dmg * ORTHO_BONUS);
		}
		return dmg;
	}

	public LineBolt knockBolt() {
		return new LineBolt();
	}

	LineBolt lineBolt() {
		return new LineBolt();
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	/**
	 * 投掷大方向：从 {@code from} 指向 {@code to} 的位移取主轴，映射为单层正四向一步（水平位移更大则东/西，否则南/北）。
	 */
	private static int snapDeployFacing(int from, int to) {
		int w = Dungeon.level.width();
		int fx = from % w, fy = from / w;
		int tx = to % w, ty = to / w;
		int dx = tx - fx, dy = ty - fy;
		if (dx == 0 && dy == 0) {
			return 0;
		}
		if (Math.abs(dx) >= Math.abs(dy)) {
			if (dx > 0) {
				return 1;
			}
			if (dx < 0) {
				return -1;
			}
			return dy > 0 ? w : -w;
		}
		if (dy > 0) {
			return w;
		}
		return -w;
	}

	private static int cardinalStep(int from, int to) {
		int w = Dungeon.level.width();
		int fx = from % w, fy = from / w;
		int tx = to % w, ty = to / w;
		int dx = tx - fx, dy = ty - fy;
		if (dx != 0 && dy != 0) {
			return 0;
		}
		if (dx == 0 && dy == 0) {
			return 0;
		}
		if (dx > 0) {
			return 1;
		}
		if (dx < 0) {
			return -1;
		}
		if (dy > 0) {
			return w;
		}
		return -w;
	}

	private static boolean isBoltHostile(Char ch, Hero owner) {
		return ch != null && ch != owner && ch.isAlive()
				&& (ch.alignment == Char.Alignment.ENEMY
				|| (ch instanceof Mimic && ch.alignment == Char.Alignment.NEUTRAL));
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target == null) {
				return;
			}
			Char ch = Actor.findChar(target);
			if (ch == null || !ch.isAlive() || ch == curUser) {
				GLog.w(Messages.get(DeployedLineBlade.this, "bad_target"));
				return;
			}
			if (Dungeon.level.distance(curUser.pos, target) > MAX_SHOOT_DISTANCE) {
				GLog.w(Messages.get(DeployedLineBlade.this, "out_of_range"));
				return;
			}
			knockBolt().cast(curUser, target);
		}

		@Override
		public String prompt() {
			return Messages.get(DeployedLineBlade.class, "prompt_shoot");
		}
	};

	private final CellSelector.Listener deployer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target == null) {
				return;
			}
			if (!Dungeon.level.heroFOV[target]) {
				GLog.w(Messages.get(DeployedLineBlade.this, "not_visible"));
				return;
			}
			if (!Dungeon.level.passable[target] && !Dungeon.level.avoid[target]) {
				GLog.w(Messages.get(DeployedLineBlade.this, "bad_deploy"));
				return;
			}
			if (Actor.findChar(target) != null) {
				GLog.w(Messages.get(DeployedLineBlade.this, "blocked_spawn"));
				return;
			}

			int facing = snapDeployFacing(curUser.pos, target);
			if (facing == 0) {
				GLog.w(Messages.get(DeployedLineBlade.this, "bad_deploy"));
				return;
			}

			DeployedLineBlade blade = DeployedLineBlade.this;
			Hero hero = curUser;

			DeployedTurretBuff oldB = hero.buff(DeployedTurretBuff.class);
			if (oldB != null && oldB.turret != null && oldB.turret.isAlive()) {
				oldB.turret.dismissTurret();
			}

			boolean equipped = blade.isEquipped(hero);
			if (equipped) {
				if (!blade.doUnequip(hero, true, true)) {
					return;
				}
			}

			Item detached = blade.detach(hero.belongings.backpack);
			if (detached == null) {
				GLog.w(Messages.get(DeployedLineBlade.this, "deploy_failed"));
				return;
			}

			if (!equipped) {
				hero.spendAndNext(1f);
			}

			Avatar mob = new Avatar(hero, (DeployedLineBlade) detached, target, facing);
			mob.pos = target;
			GameScene.add(mob);
			ScrollOfTeleportation.appear(mob, target);
			Dungeon.level.occupyCell(mob);

			DeployedTurretBuff nb = Buff.affect(hero, DeployedTurretBuff.class);
			nb.turret = mob;
		}

		@Override
		public String prompt() {
			return Messages.get(DeployedLineBlade.class, "prompt_deploy");
		}
	};

	public class LineBolt extends MissileWeapon {

		{
			image = ItemSpriteSheet.THROWING_SPIKE;
			hitSound = Assets.Sounds.HIT_STAB;
			spawnedForEffect = true;
			sticky = false;
		}

		@Override
		public int damageRoll(Char owner) {
			return DeployedLineBlade.this.damageRoll(owner);
		}

		@Override
		public boolean hasEnchant(Class<? extends Enchantment> type, Char owner) {
			return DeployedLineBlade.this.hasEnchant(type, owner);
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			return DeployedLineBlade.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return DeployedLineBlade.this.delayFactor(user);
		}

		@Override
		public int STRReq(int lvl) {
			return DeployedLineBlade.this.STRReq();
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
			Sample.INSTANCE.play(Assets.Sounds.ATK_CROSSBOW, 1, Random.Float(0.87f, 1.15f));
		}

		@Override
		public void cast(Hero user, int dst) {
			DeployedLineBlade.this.targetPos = throwPos(user, dst);
			super.cast(user, dst);
		}
	}

	/** 标记当前英雄有一座本武器的部署弩台（用于读档后由 Avatar 重新关联）。 */
	public static class DeployedTurretBuff extends Buff {

		public Avatar turret;

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return com.zootdungeon.ui.BuffIndicator.NONE;
		}
	}

	public static class Avatar extends Mob {

		private static final String TAG_BLADE = "dlb_blade";
		private static final String TAG_OWNER = "dlb_owner";
		private static final String TAG_STEP = "dlb_step";

		private DeployedLineBlade blade;
		private Hero owner;
		private int ownerId = -1;
		private int stepDelta;

		private final CellSelector.Listener aimListener = new CellSelector.Listener() {
			@Override
			public void onSelect(Integer target) {
				if (target == null) {
					return;
				}
				int st = cardinalStep(Avatar.this.pos, target);
				if (st == 0) {
					GLog.w(Messages.get(DeployedLineBlade.class, "bad_aim"));
					return;
				}
				Avatar.this.stepDelta = st;
				GLog.p(Messages.get(DeployedLineBlade.class, "aim_set"));
				if (Avatar.this.owner != null) {
					Avatar.this.owner.spendAndNext(1f);
				}
			}

			@Override
			public String prompt() {
				return Messages.get(DeployedLineBlade.class, "prompt_aim");
			}
		};

		public Avatar() {
			super();
		}

		public Avatar(Hero owner, DeployedLineBlade blade, int pos, int stepDelta) {
			this.owner = owner;
			this.blade = blade;
			this.pos = pos;
			this.stepDelta = stepDelta;
		}

		{
			spriteClass = AvatarSprite.class;
			alignment = Alignment.ALLY;
			state = PASSIVE;
			EXP = 0;
			maxLvl = -1;
			HT = HP = 15;
			defenseSkill = 4;
			properties.add(Property.IMMOVABLE);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(TAG_BLADE, blade);
			bundle.put(TAG_STEP, stepDelta);
			if (owner != null) {
				bundle.put(TAG_OWNER, owner.id());
			} else {
				bundle.put(TAG_OWNER, -1);
			}
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			blade = (DeployedLineBlade) bundle.get(TAG_BLADE);
			stepDelta = bundle.getInt(TAG_STEP);
			ownerId = bundle.getInt(TAG_OWNER);
		}

		@Override
		public void restoreEnemy() {
			super.restoreEnemy();
			if (owner == null && ownerId != -1) {
				owner = (Hero) Actor.findById(ownerId);
			}
			if (owner != null) {
				DeployedTurretBuff b = owner.buff(DeployedTurretBuff.class);
				if (b == null) {
					b = Buff.affect(owner, DeployedTurretBuff.class);
				}
				b.turret = this;
			}
		}

		@Override
		public boolean canInteract(Char c) {
			return c == owner && owner != null && owner.isAlive()
					&& Dungeon.level.adjacent(pos, c.pos);
		}

		@Override
		public boolean interact(Char c) {
			if (c != owner || owner == null || !owner.isAlive()) {
				return true;
			}
			final Hero who = owner;
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndOptions(sprite(),
							Messages.get(DeployedLineBlade.class, "turret_menu_title"),
							Messages.get(DeployedLineBlade.class, "turret_menu_body"),
							Messages.get(DeployedLineBlade.class, "opt_pickup"),
							Messages.get(DeployedLineBlade.class, "opt_aim")) {
						@Override
						protected void onSelect(int index) {
							if (index == 0) {
								GLog.i(Messages.get(DeployedLineBlade.class, "recalled"));
								dismissTurret();
								who.spendAndNext(1f);
							} else if (index == 1) {
								blade.setCurrent(who);
								GameScene.selectCell(Avatar.this.aimListener);
							}
						}
					});
				}
			});
			return true;
		}

		@Override
		public void die(Object cause) {
			returnBladeToOwner();
			super.die(cause);
		}

		void dismissTurret() {
			die(null);
		}

		private void returnBladeToOwner() {
			Hero h = owner;
			if (h != null) {
				DeployedTurretBuff tb = h.buff(DeployedTurretBuff.class);
				if (tb != null && tb.turret == this) {
					tb.turret = null;
					tb.detach();
				}
			}
			DeployedLineBlade b = blade;
			int dropPos = pos;
			blade = null;
			owner = null;
			if (b != null) {
				if (h != null && h.isAlive()) {
					if (!b.collect(h.belongings.backpack)) {
						Dungeon.level.drop(b, h.pos).sprite.drop();
					}
				} else {
					Dungeon.level.drop(b, dropPos).sprite.drop();
				}
			}
		}

		@Override
		protected boolean act() {
			if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
				fieldOfView = new boolean[Dungeon.level.length()];
			}
			Dungeon.level.updateFieldOfView(this, fieldOfView);

			if (blade == null || owner == null || !owner.isAlive()) {
				returnBladeToOwner();
				if (isAlive()) {
					die(null);
				} else {
					spend(TICK);
				}
				return true;
			}

			spend(TICK);

			if (stepDelta == 0) {
				return true;
			}

			Char shootTarget = null;
			for (int k = 1; k <= MAX_SHOOT_DISTANCE; k++) {
				int c = pos + k * stepDelta;
				if (!Dungeon.level.insideMap(c)) {
					break;
				}
				if (Dungeon.level.solid[c]) {
					break;
				}
				Char occ = Actor.findChar(c);
				if (occ != null) {
					if (isBoltHostile(occ, owner)) {
						shootTarget = occ;
					}
					break;
				}
			}

			if (shootTarget != null && sprite != null) {
				final Char tgt = shootTarget;
				final LineBolt bolt = blade.lineBolt();
				Sample.INSTANCE.play(Assets.Sounds.ATK_CROSSBOW, 1, Random.Float(0.87f, 1.15f));
				sprite.zap(tgt.pos, new Callback() {
					@Override
					public void call() {
						sprite.idle();
						if (sprite.parent == null) {
							blade.targetPos = tgt.pos;
							owner.shoot(tgt, bolt);
							return;
						}
						Callback onHit = new Callback() {
							@Override
							public void call() {
								blade.targetPos = tgt.pos;
								owner.shoot(tgt, bolt);
							}
						};
						MissileSprite ms = (MissileSprite) sprite.parent.recycle(MissileSprite.class);
						if (tgt.sprite != null) {
							ms.reset(sprite, tgt.sprite, bolt, onHit);
						} else {
							ms.reset(sprite, tgt.pos, bolt, onHit);
						}
					}
				});
			}

			return true;
		}
	}

	public static class AvatarSprite extends MobSprite {

		public AvatarSprite() {
			super();
			texture(Assets.getTexture(Assets.Sprites.ITEMS));
			TextureFilm frames = ItemSpriteSheet.film;
			idle = new Animation(0, true);
			idle.frames(frames, ItemSpriteSheet.CROSSBOW);
			run = idle;
			attack = new Animation(10, false);
			attack.frames(frames, ItemSpriteSheet.CROSSBOW);
			zap = new Animation(8, false);
			zap.frames(frames, ItemSpriteSheet.CROSSBOW);
			die = new Animation(8, false);
			die.frames(frames, ItemSpriteSheet.CROSSBOW);
			play(idle);
		}
	}
}
