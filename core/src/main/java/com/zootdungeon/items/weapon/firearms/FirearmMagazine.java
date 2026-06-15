package com.zootdungeon.items.weapon.firearms;

import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import com.zootdungeon.utils.GLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;

public class FirearmMagazine extends Item {

	private static final String KEY_ID = "id";
	private static final String KEY_BULLET = "bullet";
	private static final String KEY_CAPACITY = "capacity";
	private static final String KEY_REMAINING = "remaining";

	public String id = "mag";
	public FirearmBullet bullet = new FirearmBullet();
	public int capacity = 10;
	public int remaining = 10;

	public FirearmMagazine() {
		stackable = false;
		image = ItemSpriteSheet.MISSILE_HOLDER;
	}

	public FirearmMagazine(String id, FirearmBullet bullet, int capacity, int remaining) {
		stackable = false;
		image = ItemSpriteSheet.MISSILE_HOLDER;
		this.id = id;
		this.bullet = bullet == null ? new FirearmBullet() : bullet.copy();
		// capacity <= 0 means "unlimited" magazine.
		if (capacity <= 0) {
			this.capacity = 0;
			this.remaining = 0;
		} else {
			this.capacity = Math.max(1, capacity);
			this.remaining = Math.max(0, Math.min(this.capacity, remaining));
		}
	}

	public boolean hasRounds() {
		return capacity <= 0 || remaining > 0;
	}

	public FirearmBullet takeRound() {
		if (capacity <= 0) {
			return bullet.copy();
		}
		if (remaining <= 0) {
			return null;
		}
		remaining--;
		return bullet.copy();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KEY_ID, id);
		bundle.put(KEY_BULLET, bullet);
		bundle.put(KEY_CAPACITY, capacity);
		bundle.put(KEY_REMAINING, remaining);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		id = bundle.getString(KEY_ID);
		bullet = (FirearmBullet) bundle.get(KEY_BULLET);
		int cap = bundle.getInt(KEY_CAPACITY);
		if (cap <= 0) {
			capacity = 0;
			remaining = 0;
		} else {
			capacity = Math.max(1, cap);
			remaining = Math.max(0, Math.min(capacity, bundle.getInt(KEY_REMAINING)));
		}
		if (bullet == null) {
			bullet = new FirearmBullet();
		}
	}

	public boolean supportsReloadAction() {
		return false;
	}

	public void doReload(Hero hero) {
	}

	public boolean hasAmmo(Hero hero) {
		return hasRounds();
	}

	public FirearmBullet consumeBullet(Hero hero) {
		return takeRound();
	}

	public boolean loadRound(FirearmBullet round) {
		if (round == null) return false;
		if (capacity <= 0) {
			bullet = round.copy();
			return true;
		}
		if (remaining >= capacity) return false;
		if (remaining > 0 && bullet != null && bullet.id != null && round.id != null && !bullet.id.equals(round.id)) {
			return false;
		}
		bullet = round.copy();
		remaining++;
		return true;
	}

	public String ammoStatus() {
		String b = bullet == null ? "无" : bullet.displayName;
		return "弹匣:" + remaining + "/" + capacity + " | " + b;
	}

	public String ammoStatusShort() {
		return remaining + "/" + capacity;
	}

	/** A-type magazine: unlimited primary feed + limited special ammo pool. */
	public static class SwitchMagazine extends FirearmMagazine {

		private static final String KEY_SPECIAL_MODE = "special_mode";
		private static final String KEY_SPECIAL_AMMO = "special_ammo";
		private static final String KEY_DEFAULT_BULLET = "default_bullet";
		private static final String KEY_SPECIAL_BULLET = "special_bullet";
		private static final String KEY_PRIMARY_MAG = "primary_mag";

		public FirearmBullet defaultBullet = new FirearmBullet("a_default", "常规弹");
		public FirearmBullet specialBullet = new FirearmBullet("a_special", "技能弹");
		public FirearmMagazine primaryMagazine = new FirearmMagazine("a_unlimited", defaultBullet, 0, 0);
		public boolean specialMode = false;
		public int specialAmmo = 0;

		public void setSpecialAmmo(int amount) { specialAmmo = Math.max(0, amount); }
		public int specialAmmo() { return specialAmmo; }
		public void setSpecialMode(boolean enabled) { specialMode = enabled; }
		public boolean specialMode() { return specialMode; }
		public void addSpecialAmmo(int add) { if (add > 0) specialAmmo += add; }
		public void setSpecialBullet(FirearmBullet bullet) { if (bullet != null) specialBullet = bullet.copy(); }
		public FirearmBullet specialBullet() { return specialBullet; }

		public boolean hasAmmo() {
			return (specialMode && specialAmmo > 0) || primaryMagazine.hasRounds();
		}

		public FirearmBullet consumeRound() {
			if (specialMode && specialAmmo > 0) {
				specialAmmo--;
				if (specialAmmo <= 0) {
					// When the last special round is spent, automatically return to normal mode.
					specialMode = false;
				}
				return specialBullet.copy();
			}
			primaryMagazine.bullet = defaultBullet.copy();
			return primaryMagazine.takeRound();
		}

		public String ammoStatus() {
			String sp = specialBullet != null ? specialBullet.displayName : "无";
			String normal = defaultBullet != null ? defaultBullet.displayName : "无";
			return "A型 弹匣:∞(" + normal + ") | 特弹:" + specialAmmo + " | " + sp + (specialMode ? " [启用]" : " [关闭]");
		}

		public String ammoStatusShort() {
			return specialMode?"SA:" + specialAmmo:" ";
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(KEY_SPECIAL_MODE, specialMode);
			bundle.put(KEY_SPECIAL_AMMO, specialAmmo);
			bundle.put(KEY_DEFAULT_BULLET, defaultBullet);
			bundle.put(KEY_SPECIAL_BULLET, specialBullet);
			bundle.put(KEY_PRIMARY_MAG, primaryMagazine);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			specialMode = bundle.getBoolean(KEY_SPECIAL_MODE);
			specialAmmo = bundle.getInt(KEY_SPECIAL_AMMO);
			defaultBullet = (FirearmBullet) bundle.get(KEY_DEFAULT_BULLET);
			specialBullet = (FirearmBullet) bundle.get(KEY_SPECIAL_BULLET);
			primaryMagazine = (FirearmMagazine) bundle.get(KEY_PRIMARY_MAG);
			if (defaultBullet == null) defaultBullet = new FirearmBullet("a_default", "常规弹");
			if (specialBullet == null) specialBullet = new FirearmBullet("a_special", "技能弹");
			if (primaryMagazine == null) primaryMagazine = new FirearmMagazine("a_unlimited", defaultBullet, 0, 0);
			primaryMagazine.capacity = 0;
			primaryMagazine.remaining = 0;
			primaryMagazine.bullet = defaultBullet.copy();
		}

		@Override
		public boolean supportsReloadAction() {
			return false;
		}

		@Override
		public void doReload(Hero hero) {
		}

		@Override
		public boolean hasAmmo(Hero hero) {
			return hasAmmo();
		}

		@Override
		public FirearmBullet consumeBullet(Hero hero) {
			return consumeRound();
		}
	}

	/** B-type magazine: small FIFO queue with per-round loading. */
	public static class QueueMagazine extends FirearmMagazine {

		private static final String KEY_CAPACITY = "queue_capacity";
		private static final String KEY_RELOAD_TEMPLATE = "reload_template";
		private static final String KEY_QUEUE = "round_queue";

		public int queueCapacity = 4;
		public FirearmBullet reloadTemplate = new FirearmBullet("b_default", "常规弹");
		public final ArrayDeque<FirearmBullet> rounds = new ArrayDeque<>();

		public boolean loadRound(FirearmBullet bullet) {
			if (bullet == null || rounds.size() >= queueCapacity) return false;
			rounds.addLast(bullet.copy());
			return true;
		}

		public boolean reloadOne() {
			return loadRound(reloadTemplate);
		}

		public boolean hasAmmo() {
			return !rounds.isEmpty();
		}

		public FirearmBullet consumeRound() {
			FirearmBullet b = rounds.pollFirst();
			return b == null ? null : b.copy();
		}

		public String ammoStatus() {
			FirearmBullet next = rounds.peekFirst();
			String nextName = next != null ? next.displayName : "无";
			return "B型 弹匣(队列):" + rounds.size() + "/" + queueCapacity + " | 下一发:" + nextName;
		}

		public String ammoStatusShort() {
			return rounds.size() + "/" + queueCapacity;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(KEY_CAPACITY, queueCapacity);
			bundle.put(KEY_RELOAD_TEMPLATE, reloadTemplate);
			bundle.put(KEY_QUEUE, new ArrayList<>(rounds));
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			queueCapacity = Math.max(1, bundle.getInt(KEY_CAPACITY));
			reloadTemplate = (FirearmBullet) bundle.get(KEY_RELOAD_TEMPLATE);
			if (reloadTemplate == null) reloadTemplate = new FirearmBullet("b_default", "常规弹");

			rounds.clear();
			Collection<Bundlable> collection = bundle.getCollection(KEY_QUEUE);
			if (collection != null) {
				for (Bundlable b : collection) {
					if (b instanceof FirearmBullet) loadRound((FirearmBullet) b);
				}
			}
		}

		@Override
		public boolean supportsReloadAction() {
			return true;
		}

		@Override
		public void doReload(Hero hero) {
			reloadOne();
		}

		@Override
		public boolean hasAmmo(Hero hero) {
			return hasAmmo();
		}

		@Override
		public FirearmBullet consumeBullet(Hero hero) {
			return consumeRound();
		}
	}

	/** C-type magazine model: swap magazines only, inconsistent single-round insert is stored by the gun. */
	public static class SwapMagazine extends FirearmMagazine {

		private static final String KEY_ACTIVE_MAG = "active_mag";
		private static final String KEY_NO_ACTIVE_MAG = "c_no_active_mag";
		private static final String KEY_SPARE_MAGS = "spare_mags";

		public FirearmMagazine activeMagazine = new FirearmMagazine(
				"default_mag",
				new FirearmBullet("c_default", "常规弹"),
				10,
				10
		);
		public final ArrayList<FirearmMagazine> spareMagazines = new ArrayList<>();

		public void addSpareMagazine(FirearmMagazine mag) {
			if (mag != null) spareMagazines.add(mag);
		}

		public void swapMagazine() {
			if (spareMagazines.isEmpty()) {
				GLog.w("没有可切换的备用弹匣。");
				return;
			}
			FirearmMagazine old = activeMagazine;
			activeMagazine = spareMagazines.remove(0);
			if (old != null) spareMagazines.add(old);
			GLog.i("已即时切换弹匣。");
		}

		public boolean hasAmmo() {
			return activeMagazine != null && activeMagazine.hasRounds();
		}

		public FirearmBullet consumeRound() {
			if (activeMagazine == null) return null;
			return activeMagazine.takeRound();
		}

		public String ammoStatus() {
			if (activeMagazine == null) {
				return "C型 弹匣:无 | 备用:" + spareMagazines.size();
			}
			int cap = activeMagazine.capacity;
			int rem = activeMagazine.remaining;
			String magAmmo = activeMagazine.bullet != null ? activeMagazine.bullet.displayName : "无";
			return "C型 弹匣:" + rem + "/" + cap + " | " + magAmmo + " | 备用:" + spareMagazines.size();
		}

		public String ammoStatusShort() {
			int cap = activeMagazine == null ? 0 : activeMagazine.capacity;
			int rem = activeMagazine == null ? 0 : activeMagazine.remaining;
			return rem + "/" + cap;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(KEY_NO_ACTIVE_MAG, activeMagazine == null);
			if (activeMagazine != null) {
				bundle.put(KEY_ACTIVE_MAG, activeMagazine);
			}
			bundle.put(KEY_SPARE_MAGS, spareMagazines);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			if (bundle.getBoolean(KEY_NO_ACTIVE_MAG)) {
				activeMagazine = null;
			} else {
				activeMagazine = (FirearmMagazine) bundle.get(KEY_ACTIVE_MAG);
				if (activeMagazine == null) {
					activeMagazine = new FirearmMagazine("default_mag", new FirearmBullet("c_default", "常规弹"), 10, 10);
				}
			}
			spareMagazines.clear();
			Collection<Bundlable> mags = bundle.getCollection(KEY_SPARE_MAGS);
			if (mags != null) {
				for (Bundlable b : mags) {
					if (b instanceof FirearmMagazine) spareMagazines.add((FirearmMagazine) b);
				}
			}
		}

		@Override
		public boolean supportsReloadAction() {
			return true;
		}

		@Override
		public void doReload(Hero hero) {
			swapMagazine();
		}

		@Override
		public boolean hasAmmo(Hero hero) {
			return hasAmmo();
		}

		@Override
		public FirearmBullet consumeBullet(Hero hero) {
			return consumeRound();
		}
	}
}

