package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.sprites.MobSprite;
import com.zootdungeon.sprites.SpriteRegistry;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
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
			Object texHandle = SpriteRegistry.mobTextureOr(Assets.Sprites.RAT, "mod:originum_slug");
			texture(texHandle);
			SmartTexture tex = TextureCache.get(texHandle);
			int fw = 32, fh = 32;
			if (tex.width / fw == 0 || tex.height / fh == 0) {
				fw = 16;
				fh = 16;
			}
			if (tex.width / fw == 0 || tex.height / fh == 0) {
				fw = Math.max(1, tex.width);
				fh = Math.max(1, tex.height);
			}
			TextureFilm frames = new TextureFilm(texture, fw, fh);

			idle = new Animation(1, true);
			idle.frames(frames, 0);

			run = new Animation(10, true);
			run.frames(frames, 0);

			attack = new Animation(10, false);
			attack.frames(frames, 0);

			die = new Animation(10, false);
			die.frames(frames, 0);

			play(idle);
		}
	}
}
