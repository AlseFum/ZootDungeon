package com.zootdungeon.arknights.MainTheme.HourOfAnAwakening;

import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.material.Gold;
import com.zootdungeon.sprites.MobSprite;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Random;
import com.zootdungeon.journal.Document;
import com.zootdungeon.Badges;
import com.zootdungeon.scenes.GameScene;
public final class AgileOriginumSlug extends Mob {

	{
		spriteClass = AgileOriginumSlugSprite.class;

		HP = HT = 4;
		defenseSkill = 22;

		maxLvl = 7;

		loot = Gold.class;
		lootChance = 0.5f;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(1, 4);
	}
	private static int dodges = 0;
	@Override
	public String defenseVerb() {
		dodges++;
		if ((dodges >= 2 && !Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_SURPRISE_ATKS))
				|| (dodges >= 4 && !Badges.isUnlocked(Badges.Badge.BOSS_SLAIN_1))){
			GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_SURPRISE_ATKS);
			dodges = 0;
		}
		return super.defenseVerb();
	}

	public static class AgileOriginumSlugSprite extends MobSprite {
		public AgileOriginumSlugSprite() {
			super();
			Object texHandle = com.zootdungeon.Assets.getTexture("cola/originum_slug.png");
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
			scale.set(0.5f);
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
