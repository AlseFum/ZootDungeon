package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;

public class MobileShieldSoldier extends Mob {

	static {
		SpriteRegistry.registerMob("mod:shielded",
				new SpriteRegistry.MobDef("cola/Shielded.png", 32, 32));
	}

	{
		spriteClass = MobileShieldSoldierSprite.class;

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

	public static class MobileShieldSoldierSprite extends MobSprite {
		public MobileShieldSoldierSprite() {
			super();
			scale.set(0.7f);
			TextureFilm frames = textureWithFallback("mod:shielded", Assets.Sprites.RAT, 32, 32);

			idle = new Animation(1, true);
			idle.frames(frames, 23);

			run = new Animation(10, true);
			run.frames(frames, 0, 1, 2, 3, 4, 5, 6);

			attack = new Animation(10, false);
			attack.frames(frames, 8, 9, 10, 11, 12);

			die = new Animation(9, false);
			die.frames(frames, 13, 14, 15, 16, 17, 18, 19, 20, 21);

			play(idle);
		}
	}
}
