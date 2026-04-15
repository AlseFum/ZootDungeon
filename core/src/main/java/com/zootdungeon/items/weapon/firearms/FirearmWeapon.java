package com.zootdungeon.items.weapon.firearms;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.effects.Splash;
import com.zootdungeon.items.wands.WandOfBlastWave;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.levels.Level;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.watabou.utils.DeviceCompat;

import java.util.ArrayList;
import java.util.List;

public abstract class FirearmWeapon extends Weapon {

	public static final String AC_SHOOT = "SHOOT";
	public static final String AC_RELOAD = "RELOAD";
	public static final String AC_AIM = "AIM";
	public static final String AC_CYCLE_MODE = "CYCLE_MODE";

	private static final String KEY_TRIGGER_MODE = "firearm_trigger_mode";
	private static final String KEY_MAGAZINE = "firearm_magazine";

	public enum TriggerMode {
		SINGLE,
		BURST,
		AUTO
	}

	protected TriggerMode triggerMode = TriggerMode.SINGLE;
	protected int tier = 3;

	protected int maxRange = 8;
	protected int burstCount = 3;
	protected int autoCount = 5;
	/** Gun-side multiplier applied to bullet base damage. */
	protected float gunDamageMult = 1f;

	/** Ammo behavior is fully delegated to this, if present. */
	protected FirearmMagazine magazine = null;

	{
		image = ItemSpriteSheet.CROSSBOW;
		defaultAction = AC_SHOOT;
		usesTargeting = true;
		DLY = 1.05f;
		RCH = 1;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)) {
			actions.add(AC_SHOOT);
			if (supportsAimAction()) {
				actions.add(AC_AIM);
			}
		}
		if (supportsReloadAction()) {
			actions.add(AC_RELOAD);
		}
		if (supportsModeCycle()) {
			actions.add(AC_CYCLE_MODE);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (AC_SHOOT.equals(action)) {
			return "射击";
		}
		if (AC_RELOAD.equals(action)) {
			return reloadActionName();
		}
		if (AC_AIM.equals(action)) {
			return "瞄准";
		}
		if (AC_CYCLE_MODE.equals(action)) {
			return "切换射击模式";
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
		} else if (AC_RELOAD.equals(action)) {
			if (!supportsReloadAction()) {
				GLog.w("该武器不支持" + reloadActionName() + "。");
				return;
			}
			reload(hero);
		} else if (AC_AIM.equals(action)) {
			if (!supportsAimAction()) {
				GLog.w("该武器没有瞄准动作。");
				return;
			}
			((FirearmTraitAimable) this).aim(hero);
			hero.spendAndNext(0.8f);
		} else if (AC_CYCLE_MODE.equals(action)) {
			cycleTriggerMode();
			hero.spendAndNext(0.2f);
		}
	}

	protected boolean supportsReloadAction() {
		return magazine != null && magazine.supportsReloadAction();
	}

	protected String reloadActionName() {
		return "换弹";
	}

	protected boolean supportsAimAction() {
		return this instanceof FirearmTraitAimable;
	}

	protected boolean supportsModeCycle() {
		// opt-in: rifles set burst/auto counts and want UI action
		return burstCount > 1 || autoCount > 1;
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer target) {
			if (target == null || curUser == null) {
				return;
			}
			fireAt(curUser, target);
		}

		@Override
		public String prompt() {
			return "选择射击目标";
		}
	};

	private void fireAt(Hero user, int dst) {
		if (!hasAmmoForTrigger(user)) {
			GLog.w("弹药不足。");
			// Still spend a small amount of time; "failed shot" shouldn't be free.
			user.spendAndNext(0.2f);
			return;
		}

		if (!Dungeon.level.heroFOV[dst]) {
			// Allow blind fire; accuracy/damage are handled by bullet/gun mechanics.
			GLog.w("目标不在视野中（盲射）。");
		}

		// Clamp target to maxRange along the ballistic line instead of cancelling the action.
		if (Dungeon.level.distance(user.pos, dst) > maxRange) {
			GLog.w("超出有效射程（已按射程截断）。");
			Ballistica clamp = new Ballistica(user.pos, dst, Ballistica.PROJECTILE);
			int idx = Math.min(maxRange, clamp.path.size() - 1);
			if (idx >= 0) {
				dst = clamp.path.get(idx);
			}
		}

		int aimLayers = (this instanceof FirearmTraitAimable)
				? ((FirearmTraitAimable) this).consumeAimLayers(user)
				: 0;
		int shots = shotsPerTrigger();
		int fired = 0;

		for (int i = 0; i < shots; i++) {
			FirearmBullet bullet = consumeBulletForShot(user);
			if (bullet == null) {
				break;
			}
			if (this instanceof FirearmTraitShotgun) {
				fireShotgunShell(user, dst, bullet, aimLayers);
			} else {
				fireSingleRound(user, dst, bullet, aimLayers);
			}
			fired++;
		}

		if (fired <= 0) {
			GLog.w("没有可发射的子弹。");
			return;
		}

		user.spendAndNext(delayFactor(user));
		afterTriggerFired(user, fired);
	}

	protected void afterTriggerFired(Hero user, int firedShots) {
	}

	protected int shotsPerTrigger() {
		switch (triggerMode) {
			case BURST:
				return Math.max(1, burstCount);
			case AUTO:
				return Math.max(1, autoCount);
			case SINGLE:
			default:
				return 1;
		}
	}

	protected void cycleTriggerMode() {
		if (!supportsModeCycle()) {
			return;
		}
		switch (triggerMode) {
			case SINGLE:
				triggerMode = TriggerMode.BURST;
				GLog.i("切换到三发模式。");
				break;
			case BURST:
				triggerMode = TriggerMode.AUTO;
				GLog.i("切换到连发模式。");
				break;
			case AUTO:
			default:
				triggerMode = TriggerMode.SINGLE;
				GLog.i("切换到单发模式。");
				break;
		}
	}

	protected void reload(Hero hero) {
		doReload(hero);
		hero.spendAndNext(0.8f);
	}

	protected void doReload(Hero hero) {
		if (magazine != null) magazine.doReload(hero);
	}

	protected boolean hasAmmoForTrigger(Hero hero) {
		return magazine != null && magazine.hasAmmo(hero);
	}

	protected FirearmBullet consumeBulletForShot(Hero hero) {
		return magazine == null ? null : magazine.consumeBullet(hero);
	}

	// Aim logic is provided by FirearmTraitAimable (optional).

	private void fireSingleRound(Hero user, int dst, FirearmBullet bullet, int aimLayers) {
		fireLine(user, user.pos, dst, bullet, aimLayers, false);
	}

	private void fireShotgunShell(Hero user, int dst, FirearmBullet bullet, int aimLayers) {
		// Shotgun-only: pellet count is determined by bullet.pellet; gun may add bonuses.
		FirearmTraitShotgun sg = (FirearmTraitShotgun) this;
		int pellets = bullet.pellet > 0 ? bullet.pellet : sg.pelletCount();
		pellets += sg.pelletBonus(bullet);
		pellets = Math.max(1, pellets);
		final float spreadDeg = Math.max(0f, sg.pelletSpreadDeg());
		for (int i = 0; i < pellets; i++) {
			int pelletTarget = spreadDeg <= 0f ? dst : shotgunPelletTarget(user.pos, dst, spreadDeg);
			fireLine(user, user.pos, pelletTarget, bullet, aimLayers, true);
		}
	}

	/**
	 * Picks a pellet aim cell using an angular cone around the aim direction.
	 * This keeps pellets broadly aimed at {@code dst} instead of randomly shifting the target cell.
	 */
	private int shotgunPelletTarget(int from, int dst, float spreadDeg) {
		int w = Dungeon.level.width();
		int h = Dungeon.level.height();
		int fx = from % w;
		int fy = from / w;
		int tx = dst % w;
		int ty = dst / w;

		int dx = tx - fx;
		int dy = ty - fy;
		// If aiming at self (should be rare), pick an arbitrary direction.
		if (dx == 0 && dy == 0) {
			dx = 1;
		}

		float base = (float) Math.atan2(dy, dx);
		float half = (float) (Math.toRadians(spreadDeg) * 0.5);
		float ang = base + Random.Float(-half, half);
		int dist = Math.max(1, Dungeon.level.distance(from, dst));

		int nx = Math.round(fx + (float) Math.cos(ang) * dist);
		int ny = Math.round(fy + (float) Math.sin(ang) * dist);
		nx = Math.max(0, Math.min(w - 1, nx));
		ny = Math.max(0, Math.min(h - 1, ny));
		return nx + ny * w;
	}

	private void fireLine(Hero user, int from, int to, FirearmBullet bullet, int aimLayers, boolean shotgunPellet) {
		int params = Ballistica.STOP_TARGET | Ballistica.STOP_CHARS | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID;
		Ballistica line = new Ballistica(from, to, params);
		applyDoorBreaching(line.path, bullet);

		Char target = firstHostileOnPath(user, line.path);
		if (target != null) {
			int distance = Dungeon.level.distance(user.pos, target.pos);
			GunRound round = new GunRound(bullet, distance, aimLayers, shotgunPellet, target.pos);
			if (DeviceCompat.isDebug()) {
				// Lightweight runtime diagnostics for hit issues.
				user.belongings.thrownWeapon = round;
				int atk = user.attackSkill(target);
				user.belongings.thrownWeapon = null;
				float accMulti = round.accuracyFactor(user, target);
				GLog.i("[Firearm] atkSkill=%d accMulti=%.3f dist=%d bullet=%s",
						atk, accMulti, distance, bullet.displayName);
			}
			if (!user.shoot(target, round)) {
				Splash.at(target.pos, 0xCCCCCC, 1);
				applyImpactEffect(user, null, target.pos, bullet, 0);
				return;
			}
			if (shotgunPellet && bullet.knockbackPower > 0 && target.isAlive()) {
				Ballistica push = new Ballistica(user.pos, target.pos, Ballistica.STOP_TARGET);
				if (push.path.size() > push.dist + 1) {
					Ballistica behind = new Ballistica(push.collisionPos, push.path.get(push.path.size() - 1), Ballistica.PROJECTILE);
					WandOfBlastWave.throwChar(target, behind, bullet.knockbackPower, true, false, user);
				}
			}
		} else {
			Splash.at(line.collisionPos, 0x99AAAAAA, 1);
			applyImpactEffect(user, null, line.collisionPos, bullet, 0);
			// Ricochet is bullet-driven and not limited to shotgun pellets.
			if (bullet.ricochet > 0) {
				ricochet(user, line, bullet, aimLayers, bullet.ricochet);
			}
		}
	}

	private void applyImpactEffect(Char attacker, Char defender, int cell, FirearmBullet bullet, int damage) {
		if (bullet == null) {
			return;
		}
		if (bullet.onHitEffect != null) {
			bullet.onHitEffect.apply(attacker, defender, cell, bullet, damage);
		} else if (bullet.hasFlag(FirearmBullet.FLAG_ON_HIT)) {
			onHitEffect(attacker, defender, cell, bullet, damage);
		}
	}

	private void ricochet(Hero user, Ballistica previous, FirearmBullet bullet, int aimLayers, int remain) {
		if (remain <= 0 || previous.path.size() < 2) {
			return;
		}
		int hit = previous.collisionPos;
		if (!Dungeon.level.insideMap(hit) || !Dungeon.level.solid[hit]) {
			return;
		}

		int prev = previous.path.get(Math.max(0, previous.dist - 1));
		int w = Dungeon.level.width();
		int px = prev % w;
		int py = prev / w;
		int hx = hit % w;
		int hy = hit / w;
		int dx = hx - px;
		int dy = hy - py;
		if (dx == 0 && dy == 0) {
			return;
		}

		// 简化反弹：沿主轴反向。
		if (Math.abs(dx) >= Math.abs(dy)) {
			dx = -dx;
		} else {
			dy = -dy;
		}

		int bounceTo = hit + dx * maxRange + dy * maxRange * w;
		if (!Dungeon.level.insideMap(bounceTo)) {
			return;
		}
		FirearmBullet bounced = bullet.copy();
		bounced.ricochet = remain - 1;
		fireLine(user, hit, bounceTo, bounced, aimLayers, true);
	}

	private void applyDoorBreaching(List<Integer> path, FirearmBullet bullet) {
		if (!bullet.hasFlag(FirearmBullet.FLAG_BREAKS_DOORS)) {
			return;
		}
		for (int i = 1; i < path.size(); i++) {
			int c = path.get(i);
			if (Dungeon.level.map[c] == Terrain.DOOR) {
				Level.set(c, Terrain.EMPTY);
				GameScene.updateMap(c);
			}
		}
	}

	private Char firstHostileOnPath(Hero user, List<Integer> path) {
		for (int i = 1; i < path.size(); i++) {
			Char ch = Actor.findChar(path.get(i));
			if (ch != null && ch != user && ch.isAlive()
					&& (ch.alignment == Char.Alignment.ENEMY || ch.alignment == Char.Alignment.NEUTRAL)) {
				return ch;
			}
		}
		return null;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KEY_TRIGGER_MODE, triggerMode);
		bundle.put(KEY_MAGAZINE, magazine);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		triggerMode = bundle.getEnum(KEY_TRIGGER_MODE, TriggerMode.class);
		if (triggerMode == null) {
			triggerMode = TriggerMode.SINGLE;
		}
		magazine = (FirearmMagazine) bundle.get(KEY_MAGAZINE);
	}

	@Override
	public String status() {
		String s = ammoStatusShort();
		// keep status short; UI often truncates this string
		return s;
	}

	protected String ammoStatus() {
		return magazine == null ? "无弹匣" : magazine.ammoStatus();
	}

	/** Short status string for quickslot/backpack display. */
	protected String ammoStatusShort() {
		return magazine == null ? "-" : magazine.ammoStatusShort();
	}

	@Override
	public String info() {
		String info = super.info();
		info += "\n\n" + "弹药: " + ammoStatus();
		if (supportsModeCycle()) {
			info += "\n" + "模式: " + triggerMode;
		}
		return info;
	}

	@Override
	public int STRReq(int lvl) {
		return STRReq(tier, lvl);
	}

	@Override
	public int min(int lvl) {
		return tier + lvl;
	}

	@Override
	public int max(int lvl) {
		return 5 * (tier + 1) + (tier + 1) * lvl;
	}

	public static void onHeroMoved(Hero hero, int oldPos, int newPos) {
		FirearmTraitAimable.onHeroMoved(hero, oldPos, newPos);
	}

	private class GunRound extends MissileWeapon {

		private final FirearmBullet bullet;
		private final int distance;
		private final int aimLayers;
		private final boolean shotgunPellet;
		private final int impactCell;

		private GunRound(FirearmBullet bullet, int distance, int aimLayers, boolean shotgunPellet, int impactCell) {
			this.bullet = bullet.copy();
			this.distance = distance;
			this.aimLayers = aimLayers;
			this.shotgunPellet = shotgunPellet;
			this.impactCell = impactCell;
			image = ItemSpriteSheet.THROWING_SPIKE;
			spawnedForEffect = true;
			sticky = false;
			hitSound = Assets.Sounds.HIT_ARROW;
		}

		@Override
		public int damageRoll(Char owner) {
			// Bullet carries base damage numbers; gun provides multipliers and behavior.
			int baseMin = Math.max(0, bullet.baseMin);
			int baseMax = Math.max(baseMin, bullet.baseMax);
			int dmg = Random.IntRange(baseMin, baseMax);
			float mult = gunDamageMult;
			if (shotgunPellet && FirearmWeapon.this instanceof FirearmTraitShotgun) {
				mult *= ((FirearmTraitShotgun) FirearmWeapon.this).pelletDamageFactor();
			}
			if (FirearmWeapon.this instanceof FirearmTraitPistol) {
				mult *= ((FirearmTraitPistol) FirearmWeapon.this).pistolDamageMult(distance);
			}
			if (FirearmWeapon.this instanceof FirearmTraitShotgun) {
				mult *= ((FirearmTraitShotgun) FirearmWeapon.this).shotgunDamageMult(distance);
			}
			if (aimLayers > 0 && FirearmWeapon.this instanceof FirearmTraitAimable) {
				mult *= (1f + ((FirearmTraitAimable) FirearmWeapon.this).aimDamagePerLayer() * aimLayers);
			}
			return Math.max(0, Math.round(dmg * mult));
		}

		@Override
		public float accuracyFactor(Char owner, Char target) {
			float acc = FirearmWeapon.this.accuracyFactor(owner, target) * bullet.accuracyMult;

			if (FirearmWeapon.this instanceof FirearmTraitPistol) {
				acc *= ((FirearmTraitPistol) FirearmWeapon.this).pistolAccuracyMult(distance);
			}
			if (FirearmWeapon.this instanceof FirearmTraitShotgun) {
				acc *= ((FirearmTraitShotgun) FirearmWeapon.this).shotgunAccuracyMult(distance);
			}
			if (aimLayers > 0 && FirearmWeapon.this instanceof FirearmTraitAimable) {
				acc *= (1f + ((FirearmTraitAimable) FirearmWeapon.this).aimAccuracyPerLayer() * aimLayers);
			}
			return acc;
		}

		@Override
		public int proc(Char attacker, Char defender, int damage) {
			applyImpactEffect(attacker, defender, impactCell, bullet, damage);
			return FirearmWeapon.this.proc(attacker, defender, damage);
		}

		@Override
		public float delayFactor(Char user) {
			return 0f;
		}

		@Override
		public int STRReq(int lvl) {
			return FirearmWeapon.this.STRReq();
		}
	}

	/** Optional extra effect that triggers on impact, gated by {@link FirearmBullet#FLAG_ON_HIT}. */
	protected void onHitEffect(Char attacker, Char defender, int cell, FirearmBullet bullet, int damage) {
	}

	public interface FirearmTraitPistol {
		default float pistolDamageMult(int distance) {
			if (distance <= 2) return 1.2f;
			return Math.max(0.35f, 1f - 0.2f * (distance - 2));
		}

		default float pistolAccuracyMult(int distance) {
			if (distance <= 3) return 1f;
			return Math.max(0.35f, 1f - 0.2f * (distance - 3));
		}
	}

	public interface FirearmTraitShotgun {
		default int pelletCount() {
			return 7;
		}

		/** Cone angle in degrees that pellets can deviate within. */
		default float pelletSpreadDeg() {
			return 18f;
		}

		/** Optional extra pellets from gun/skills, applied on top of {@link FirearmBullet#pellet}. */
		default int pelletBonus(FirearmBullet bullet) {
			return 0;
		}

		default float pelletDamageFactor() {
			return 0.35f;
		}

		default float shotgunDamageMult(int distance) {
			return Math.max(0.2f, 1f - 0.32f * Math.max(0, distance - 1));
		}

		default float shotgunAccuracyMult(int distance) {
			if (distance <= 2) return 2.25f;
			return Math.max(0.45f, 1f - 0.15f * (distance - 2));
		}
	}

	public interface FirearmTraitAimable {
		int aimCap();

		float aimDamagePerLayer();

		float aimAccuracyPerLayer();

		boolean keepHalfAimOnMove();

		default void aim(Hero hero) {
			AimTracker tracker = Buff.affect(hero, AimTracker.class);
			tracker.maxLayers = Math.max(1, aimCap());
			tracker.keepHalfOnMove = keepHalfAimOnMove();
			tracker.layers = Math.min(tracker.maxLayers, tracker.layers + 1);
		}

		default int consumeAimLayers(Hero hero) {
			AimTracker tracker = hero.buff(AimTracker.class);
			if (tracker == null || tracker.layers <= 0) return 0;
			int layers = tracker.layers;
			tracker.detach();
			return layers;
		}

		static void onHeroMoved(Hero hero, int oldPos, int newPos) {
			if (hero == null || oldPos == newPos) return;
			AimTracker tracker = hero.buff(AimTracker.class);
			if (tracker == null) return;
			if (tracker.keepHalfOnMove) {
				tracker.layers = Math.max(0, tracker.layers / 2);
				if (tracker.layers <= 0) tracker.detach();
			} else {
				tracker.detach();
			}
		}
	}

	public static class AimTracker extends Buff {
		private static final String KEY_LAYERS = "layers";
		private static final String KEY_MAX = "max";
		private static final String KEY_HALF = "half_on_move";

		public int layers = 0;
		public int maxLayers = 6;
		public boolean keepHalfOnMove = true;

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}

		@Override
		public String toString() {
			return "瞄准";
		}

		@Override
		public String desc() {
			return "当前瞄准层数: " + layers + "/" + maxLayers;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(KEY_LAYERS, layers);
			bundle.put(KEY_MAX, maxLayers);
			bundle.put(KEY_HALF, keepHalfOnMove);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			layers = bundle.getInt(KEY_LAYERS);
			maxLayers = bundle.getInt(KEY_MAX);
			keepHalfOnMove = bundle.getBoolean(KEY_HALF);
		}
	}
}

