package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

public class Rebellist extends Mob {

	static {
		// SpriteRegistry no longer manages mob textures.
		SpriteRegistry.texture("mod:rebellist", "cola/rebellist.png");
	}

	{
		spriteClass = RebellistSprite.class;

		HP = HT = 12;
		defenseSkill = 4;

		EXP = 2;
		maxLvl = 8;

		lootTableId = "mob:infantry:loot";
		lootChance = 0.4f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(1, 6);
	}

	@Override
	public int attackSkill(Char target) {
		return 10;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 2);
	}

	public static class RebellistSprite extends MobSprite {
		public RebellistSprite() {
			super();
			scale.set(0.7f);
			boolean hasMod = SpriteRegistry.the("mod:rebellist") != null;
			TextureFilm frames = hasMod
					? textureWithFallback("mod:rebellist", Assets.Sprites.RAT, 32, 32)
					: textureWithFallback(null, Assets.Sprites.RAT, 16, 15);

			idle = new Animation(1, true);
			idle.frames(frames, hasMod ? 23 : 0);

			run = new Animation(10, true);
			if (hasMod) run.frames(frames, 0, 1, 2, 3, 4, 5, 6);
			else run.frames(frames, 0, 1, 2, 3);

			attack = new Animation(10, false);
			if (hasMod) attack.frames(frames, 8, 9, 10, 11, 12);
			else attack.frames(frames, 4, 5, 6);

			die = new Animation(9, false);
			if (hasMod) die.frames(frames, 13, 14, 15, 16, 17, 18, 19, 20, 21);
			else die.frames(frames, 7, 8, 9);

			play(idle);
		}
	}
}
