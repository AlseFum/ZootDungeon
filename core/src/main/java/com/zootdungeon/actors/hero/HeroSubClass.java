package com.zootdungeon.actors.hero;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.blobs.MiseryShadowBlob;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Preparation;
import com.zootdungeon.ui.HeroIcon;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.watabou.noosa.Game;

import java.util.function.Consumer;

public enum HeroSubClass {

		NONE(HeroIcon.NONE),

		BERSERKER(HeroIcon.BERSERKER),
		GLADIATOR(HeroIcon.GLADIATOR),

		BATTLEMAGE(HeroIcon.BATTLEMAGE),
		WARLOCK(HeroIcon.WARLOCK),

		ASSASSIN(HeroIcon.ASSASSIN, hero -> {
			if (hero.invisible > 0) Buff.affect(hero, Preparation.class);
		}),
		FREERUNNER(HeroIcon.FREERUNNER),

		SNIPER(HeroIcon.SNIPER),
		WARDEN(HeroIcon.WARDEN),

		CHAMPION(HeroIcon.CHAMPION),
		MONK(HeroIcon.MONK),

		PRIEST(HeroIcon.PRIEST),
		PALADIN(HeroIcon.PALADIN),

		OP_SHARP(HeroIcon.OP_SHARP),
		ACE(HeroIcon.ACE),
		BLAZE(HeroIcon.BLAZE),
		STORMEYE(HeroIcon.STORMEYE),
		ROSMONTIS(HeroIcon.ROSMONTIS),
		OUTCAST(HeroIcon.OUTCAST),
		PITH(HeroIcon.PITH),
		LOGOS(HeroIcon.LOGOS),
		MANTRA(HeroIcon.MANTRA),
		MISERY(HeroIcon.MISERY, hero -> MiseryShadowBlob.generateForLevel()),
		SCOUT(HeroIcon.SCOUT),
		RADIAN(HeroIcon.RADIAN);

		int icon;
		Consumer<Hero> onChoose;

		HeroSubClass(int icon){
			this(icon, null);
		}

		HeroSubClass(int icon, Consumer<Hero> onChoose){
			this.icon = icon;
			this.onChoose = onChoose;
		}

		public String title() {
			return Messages.get(this, name());
		}

		public String shortDesc() {
			return Messages.get(this, name() + "_short_desc");
		}

		public String desc() {
			if (this == BATTLEMAGE){
				String desc = Messages.get(this, name() + "_desc");
				if (Game.scene() instanceof GameScene){
					MagesStaff staff = Dungeon.hero.belongings.getItem(MagesStaff.class);
					if (staff != null && staff.wandClass() != null){
						desc += "\n\n" + Messages.get(staff.wandClass(), "bmage_desc");
						desc = desc.replaceAll("_", "");
					}
				}
				return desc;
			} else {
				return Messages.get(this, name() + "_desc");
			}
		}

		public int icon(){
			return icon;
		}

		public void init(Hero hero) {
			if (onChoose != null) {
				onChoose.accept(hero);
			}
		}

}
