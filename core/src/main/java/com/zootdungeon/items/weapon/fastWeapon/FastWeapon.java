package com.zootdungeon.items.weapon.fastWeapon;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Combo;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;

public class FastWeapon extends MeleeWeapon {

    {
        image = ItemSpriteSheet.GLOVES;
        hitSound = Assets.Sounds.HIT;
        hitSoundPitch = 1.3f;

        tier = 1;
        DLY = 0.5f; //2x speed

        bones = false;
    }

    @Override
    public int hitCount() {
        return 2; // 快速武器普通攻击连击 2 段
    }

    @Override
    public int max(int lvl) {
        return Math.round(2.5f * (tier + 1)) +     //5 base, down from 10
                lvl * Math.round(0.5f * (tier + 1));  //+1 per level, down from +2
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        //+(3+0.75*lvl) damage, roughly +100% base damage, +100% scaling
        int dmgBoost = augment.damageFactor(3 + buffedLvl());
        comboStrikeAbility(hero, target, 0, dmgBoost, this);
    }

    @Override
    public String abilityInfo() {
        int dmgBoost = levelKnown ? 3 + buffedLvl() : 3;
        if (levelKnown) {
            return Messages.get(this, "ability_desc", augment.damageFactor(dmgBoost));
        } else {
            return Messages.get(this, "typical_ability_desc", augment.damageFactor(dmgBoost));
        }
    }

    public String upgradeAbilityStat(int level) {
        return "+" + augment.damageFactor(3 + level);
    }


    public static void comboStrikeAbility(Hero hero, Integer target, float multiPerHit, int boostPerHit, MeleeWeapon wep) {
        if (target == null) {
            return;
        }

        Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(wep, "ability_no_target"));
            return;
        }

        hero.belongings.abilityWeapon = wep;
        if (!hero.canAttack(enemy)) {
            GLog.w(Messages.get(wep, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                wep.beforeAbilityUsed(hero, enemy);
                AttackIndicator.target(enemy);

                int recentHits = 0;
                Sai.ComboStrikeTracker buff = hero.buff(Sai.ComboStrikeTracker.class);
                if (buff != null) {
                    recentHits = buff.hits;
                    buff.detach();
                }

                boolean hit = hero.attack(enemy, 1f + multiPerHit * recentHits, boostPerHit * recentHits, Char.INFINITE_ACCURACY);
                if (hit && !enemy.isAlive()) {
                    wep.onAbilityKill(hero, enemy);
                }

                Invisibility.dispel();
                hero.spendAndNext(hero.attackDelay());
                if (recentHits >= 2 && hit) {
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                }

                wep.afterAbilityUsed(hero);
            }
        });
    }

    public static class ComboStrikeTracker extends Buff {

        {
            type = buffType.POSITIVE;
        }

        public static int DURATION = 5;
        private float comboTime = 0f;
        public int hits = 0;

        @Override
        public int icon() {
            if (Dungeon.hero.belongings.weapon() instanceof Gloves
                    || Dungeon.hero.belongings.weapon() instanceof Sai
                    || Dungeon.hero.belongings.weapon() instanceof Gauntlet
                    || Dungeon.hero.belongings.secondWep() instanceof Gloves
                    || Dungeon.hero.belongings.secondWep() instanceof Sai
                    || Dungeon.hero.belongings.secondWep() instanceof Gauntlet) {
                return BuffIndicator.DUEL_COMBO;
            } else {
                return BuffIndicator.NONE;
            }
        }

        @Override
        public boolean act() {
            comboTime -= TICK;
            spend(TICK);
            if (comboTime <= 0) {
                detach();
            }
            return true;
        }

        public void addHit() {
            hits++;
            comboTime = 5f;

            if (hits >= 2 && icon() != BuffIndicator.NONE) {
                GLog.p(Messages.get(Combo.class, "combo", hits));
            }
        }

        @Override
        public float iconFadePercent() {
            return Math.max(0, (DURATION - comboTime) / DURATION);
        }

        @Override
        public String iconTextDisplay() {
            return Integer.toString((int) comboTime);
        }

        @Override
        public String desc() {
            return Messages.get(this, "desc", hits, dispTurns(comboTime));
        }

        private static final String TIME = "combo_time";
        public static String RECENT_HITS = "recent_hits";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(TIME, comboTime);
            bundle.put(RECENT_HITS, hits);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            if (bundle.contains(TIME)) {
                comboTime = bundle.getInt(TIME);
                hits = bundle.getInt(RECENT_HITS);
            } else {
                //pre-2.4.0 saves
                comboTime = 5f;
                hits = 0;
                if (bundle.contains(RECENT_HITS)) {
                    for (int i : bundle.getIntArray(RECENT_HITS)) {
                        hits += i;
                    }
                }
            }
        }
    }

}