package com.zootdungeon.items.weapon.blockWeapon;

import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.levels.Terrain;
import com.zootdungeon.levels.features.Door;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;

public class BlockWeapon extends MeleeWeapon {
    {
        image = ItemSpriteSheet.ROUND_SHIELD;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.1f;

        tier = 4;

    }

    public int drBase = 3;
    public int drPerLevel = 1;

    @Override
    public int max(int lvl) {
        return Math.round(3f * (tier + 1)) +   //12 base, down from 20
                lvl * (tier - 1);               //+2 per level, down from +4
    }

    @Override
    public int defenseFactor(Char owner) {
        return DRMax();
    }

    public int DRMax() {
        return DRMax(buffedLvl());
    }

    //4 extra defence, plus 1 per level
    public int DRMax(int lvl) {
        return drBase + drPerLevel * lvl;
    }
// statsInfo方法已在BlockWeapon中实现

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        RoundShield.guardAbility(hero, 5 + buffedLvl(), this);
    }

    @Override
    public String abilityInfo() {
        if (levelKnown) {
            return Messages.get(this, "ability_desc", 5 + buffedLvl());
        } else {
            return Messages.get(this, "typical_ability_desc", 5);
        }
    }

    @Override
    public String upgradeAbilityStat(int level) {
        return Integer.toString(5 + level);
    }

    public static void guardAbility(Hero hero, int duration, MeleeWeapon wep) {
        wep.beforeAbilityUsed(hero, null);
        Buff.prolong(hero, RoundShield.GuardTracker.class, duration).hasBlocked = false;
        hero.sprite.operate(hero.pos);
        hero.spendAndNext(Actor.TICK);
        wep.afterAbilityUsed(hero);
    }

    public static class GuardTracker extends FlavourBuff {

        {
            announced = true;
            type = buffType.POSITIVE;
        }

        public boolean hasBlocked = false;

        @Override
        public int icon() {
            return BuffIndicator.DUEL_GUARD;
        }

        @Override
        public void tintIcon(Image icon) {
            if (hasBlocked) {
                icon.tint(0x651f66, 0.5f);
            } else {
                icon.resetColor();
            }
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (5 - visualcooldown()) / 5);
        }

        private static final String BLOCKED = "blocked";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(BLOCKED, hasBlocked);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            hasBlocked = bundle.getBoolean(BLOCKED);
        }


    }
    public static void lungeAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep){
        if (target == null){
            return;
        }

        Char enemy = Actor.findChar(target);
        //duelist can lunge out of her FOV, but this wastes the ability instead of cancelling if there is no target
        if (Dungeon.level.heroFOV[target]) {
            if (enemy == null || enemy == hero || hero.isCharmedBy(enemy)) {
                GLog.w(Messages.get(wep, "ability_no_target"));
                return;
            }
        }

        if (hero.rooted || Dungeon.level.distance(hero.pos, target) < 2
                || Dungeon.level.distance(hero.pos, target)-1 > wep.reachFactor(hero)){
            GLog.w(Messages.get(wep, "ability_target_range"));
            if (hero.rooted) PixelScene.shake( 1, 1f );
            return;
        }

        int lungeCell = -1;
        for (int i : PathFinder.NEIGHBOURS8){
            if (Dungeon.level.distance(hero.pos+i, target) <= wep.reachFactor(hero)
                    && Actor.findChar(hero.pos+i) == null
                    && (Dungeon.level.passable[hero.pos+i] || (Dungeon.level.avoid[hero.pos+i] && hero.flying))){
                if (lungeCell == -1 || Dungeon.level.trueDistance(hero.pos + i, target) < Dungeon.level.trueDistance(lungeCell, target)){
                    lungeCell = hero.pos + i;
                }
            }
        }

        if (lungeCell == -1){
            GLog.w(Messages.get(wep, "ability_target_range"));
            return;
        }

        final int dest = lungeCell;

        hero.busy();
        Sample.INSTANCE.play(Assets.Sounds.MISS);
        hero.sprite.jump(hero.pos, dest, 0, 0.1f, new Callback() {
            @Override
            public void call() {
                if (Dungeon.level.map[hero.pos] == Terrain.OPEN_DOOR) {
                    Door.leave( hero.pos );
                }
                hero.pos = dest;
                Dungeon.level.occupyCell(hero);
                Dungeon.observe();

                hero.belongings.abilityWeapon = wep; //set this early to we can check canAttack
                if (enemy != null && hero.canAttack(enemy)) {
                    hero.sprite.attack(enemy.pos, new Callback() {
                        @Override
                        public void call() {

                            wep.beforeAbilityUsed(hero, enemy);
                            AttackIndicator.target(enemy);
                            if (hero.attack(enemy, dmgMulti, dmgBoost, Char.INFINITE_ACCURACY)) {
                                Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                                if (!enemy.isAlive()) {
                                    wep.onAbilityKill(hero, enemy);
                                }
                            }
                            Invisibility.dispel();
                            hero.spendAndNext(hero.attackDelay());
                            wep.afterAbilityUsed(hero);
                        }
                    });
                } else {
                    //spends charge but otherwise does not count as an ability use
                    Charger charger = Buff.affect(hero, Charger.class);
                    charger.partialCharge -= 1;
                    while (charger.partialCharge < 0 && charger.charges > 0) {
                        charger.charges--;
                        charger.partialCharge++;
                    }
                    updateQuickslot();
                    GLog.w(Messages.get(Rapier.class, "ability_no_target"));
                    hero.spendAndNext(1/hero.speed());
                }
            }
        });
    }
}