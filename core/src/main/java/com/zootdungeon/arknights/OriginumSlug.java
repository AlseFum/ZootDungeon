package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

public class OriginumSlug extends Mob {

	static {
		SpriteRegistry.registerMob("mod:originum_slug",
				new SpriteRegistry.MobDef("cola/originum_slug.png", 32, 32));
	}

	{
		spriteClass = OriginumSlugSprite.class;

		HP = HT = 8;
		defenseSkill = 2;

		maxLvl = 5;

		loot = Gold.class;
		lootChance = 0.4f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(1, 4);
	}

	@Override
	public int attackSkill(Char target) {
		return 8;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 1);
	}

	public static class OriginumSlugSprite extends MobSprite {
		public OriginumSlugSprite() {
			super();
			TextureFilm frames = textureWithFallback("mod:originum_slug", Assets.Sprites.RAT, 32, 32);

			idle = new Animation(2, true);
			idle.frames(frames, 0, 0, 0, 1);

			run = new Animation(10, true);
			run.frames(frames, 6, 7, 8, 9, 10);

			attack = new Animation(15, false);
			attack.frames(frames, 2, 3, 4, 5, 0);

			die = new Animation(10, false);
			die.frames(frames, 11, 12, 13, 14);

			play(idle);
		}
	}
}
