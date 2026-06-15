package com.zootdungeon.items.weapon.firearms;

import com.zootdungeon.items.Item;
import com.watabou.utils.Bundle;
// 实际上这里可以优化
public class FirearmBullet extends Item {

	@FunctionalInterface
	public interface OnHitEffect {
		void apply(com.zootdungeon.actors.Char attacker, com.zootdungeon.actors.Char defender,
				   int cell, FirearmBullet bullet, int damage);
	}

	private static final String KEY_ID = "id";
	private static final String KEY_NAME = "name";
	private static final String KEY_BASE_MIN = "base_min";
	private static final String KEY_BASE_MAX = "base_max";
	private static final String KEY_ACCURACY_MULT = "accuracy_mult";
	private static final String KEY_PELLET = "pellet";
	private static final String KEY_RICOCHET = "ricochet";
	// Legacy key for backward compatibility with existing saves.
	private static final String KEY_SHOTGUN_BOUNCES = "shotgun_bounces";
	private static final String KEY_FLAGS = "flags";
	// Legacy key for backward compatibility with existing saves.
	private static final String KEY_BREAKS_DOORS = "breaks_doors";
	private static final String KEY_KNOCKBACK = "knockback";

	public static final int FLAG_BREAKS_DOORS = 1 << 0;
	public static final int FLAG_ON_HIT = 1 << 1;

	public String id = "default";
	public String displayName = "Default Round";
	public int baseMin = 1;
	public int baseMax = 2;
	public float accuracyMult = 1f;
	/** Shotgun-only: base pellet trajectory count for this shell. <=0 means "use gun default". */
	public int pellet = 0;
	/** Remaining ricochet count for this bullet type. */
	public int ricochet = 0;
	public int flags = 0;
	public int knockbackPower = 0;
	/** Runtime-only on-hit effect (not serialized). */
	public transient OnHitEffect onHitEffect = null;

	public FirearmBullet() {
		stackable = true;
	}

	public FirearmBullet(String id, String displayName) {
		stackable = true;
		this.id = id;
		this.displayName = displayName;
	}

	public FirearmBullet copy() {
		FirearmBullet c = new FirearmBullet(id, displayName);
		c.baseMin = baseMin;
		c.baseMax = baseMax;
		c.accuracyMult = accuracyMult;
		c.pellet = pellet;
		c.ricochet = ricochet;
		c.flags = flags;
		c.knockbackPower = knockbackPower;
		c.bindRuntimeEffect();
		return c;
	}

	public boolean hasFlag(int flag) {
		return (flags & flag) != 0;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KEY_ID, id);
		bundle.put(KEY_NAME, displayName);
		bundle.put(KEY_BASE_MIN, baseMin);
		bundle.put(KEY_BASE_MAX, baseMax);
		bundle.put(KEY_ACCURACY_MULT, accuracyMult);
		bundle.put(KEY_PELLET, pellet);
		bundle.put(KEY_RICOCHET, ricochet);
		bundle.put(KEY_FLAGS, flags);
		bundle.put(KEY_KNOCKBACK, knockbackPower);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		id = bundle.getString(KEY_ID);
		displayName = bundle.getString(KEY_NAME);
		baseMin = bundle.getInt(KEY_BASE_MIN);
		baseMax = bundle.getInt(KEY_BASE_MAX);
		accuracyMult = bundle.getFloat(KEY_ACCURACY_MULT);
		pellet = Math.max(0, bundle.getInt(KEY_PELLET));
		ricochet = Math.max(bundle.getInt(KEY_RICOCHET), bundle.getInt(KEY_SHOTGUN_BOUNCES));
		flags = bundle.getInt(KEY_FLAGS);
		if (bundle.getBoolean(KEY_BREAKS_DOORS)) {
			flags |= FLAG_BREAKS_DOORS;
		}
		knockbackPower = bundle.getInt(KEY_KNOCKBACK);
		bindRuntimeEffect();
	}

	private void bindRuntimeEffect() {
		onHitEffect = null;
		if ("inc".equals(id)) {
			onHitEffect = (attacker, defender, cell, bullet, damage) -> {
				if (cell < 0) return;
				com.zootdungeon.scenes.GameScene.add(
						com.zootdungeon.actors.blobs.Blob.seed(cell, 4, com.zootdungeon.actors.blobs.Fire.class)
				);
			};
		}
	}

	@Override
	public String name() {
		return displayName != null ? displayName : super.name();
	}

	@Override
	public String desc() {
		return "子弹";
	}

	/** Built-in bullet presets used by debug firearms. Keep this intentionally small. */
	public static final class Presets {
		private Presets() {}

		public static FirearmBullet fmj() {
			FirearmBullet b = new FirearmBullet("fmj", "FMJ");
			b.baseMin = 2;
			b.baseMax = 4;
			return b;
		}

		public static FirearmBullet buckshot() {
			FirearmBullet b = new FirearmBullet("buck", "鹿弹");
			b.baseMin = 1;
			b.baseMax = 2;
			b.accuracyMult = 0.9f;
			b.pellet = 7;
			b.knockbackPower = 1;
			b.ricochet = 1;
			return b;
		}

		public static FirearmBullet incendiary() {
			FirearmBullet b = new FirearmBullet("inc", "燃烧弹");
			b.baseMin = 2;
			b.baseMax = 4;
			b.accuracyMult = 0.95f;
			b.flags |= FLAG_ON_HIT;
			b.bindRuntimeEffect();
			return b;
		}
	}
}

