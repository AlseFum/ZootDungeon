package com.zootdungeon.items.weapon.base;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.Daze;
import com.zootdungeon.actors.buffs.Invisibility;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.items.weapon.base.MeleeWeapon;
import com.zootdungeon.ui.AttackIndicator;
import com.zootdungeon.utils.GLog;
import java.util.Map;

public class AccurateWeapon  extends MeleeWeapon{
    {
        image = ItemSpriteSheet.CUDGEL;
        hitSound = Assets.Sounds.HIT_CRUSH;
        hitSoundPitch = 1.2f;

        tier = 1;
        ACC = 1.40f; //40% boost to accuracy

        bones = false;
    }
    public int dmgBoostBase=3;

    @Override
    public Map<String, Object> getConfig() {
        Map<String, Object> cfg = super.getConfig();
        cfg.put("dmgBoostBase", dmgBoostBase);
        return cfg;
    }

    @Override
    public void setConfig(String key, Object value) {
        switch (key) {
            case "dmgBoostBase": dmgBoostBase = (Integer) value; break;
            default: super.setConfig(key, value); break;
        }
    }

    @Override
    public int max(int lvl) {
        return  4*(tier+1) +    //8 base, down from 10
                lvl*(tier+1);   //scaling unchanged
    }

    @Override
    public String targetingPrompt() {
        return Messages.get(this, "prompt");
    }

    @Override
    protected void duelistAbility(Hero hero, Integer target) {
        //+(3+1.5*lvl) damage, roughly +67% base dmg, +100% scaling
        int dmgBoost = augment.damageFactor(dmgBoostBase + Math.round(1.5f*buffedLvl()));
        heavyBlowAbility(hero, target, 1, dmgBoost, this);
    }

    @Override
    public String abilityInfo() {
        int dmgBoost = levelKnown ? dmgBoostBase + Math.round(1.5f*buffedLvl()) : dmgBoostBase;
        if (levelKnown){
            return Messages.get(this, "ability_desc", augment.damageFactor(min()+dmgBoost), augment.damageFactor(max()+dmgBoost));
        } else {
            return Messages.get(this, "typical_ability_desc", min(0)+dmgBoost, max(0)+dmgBoost);
        }
    }

    public String upgradeAbilityStat(int level){
        int dmgBoost = dmgBoostBase + Math.round(1.5f*level);
        return augment.damageFactor(min(level)+dmgBoost) + "-" + augment.damageFactor(max(level)+dmgBoost);
    }
    public static void heavyBlowAbility(Hero hero, Integer target, float dmgMulti, int dmgBoost, MeleeWeapon wep){
        if (target == null) {
            return;
        }

        Char enemy = Actor.findChar(target);
        if (enemy == null || enemy == hero || hero.isCharmedBy(enemy) || !Dungeon.level.heroFOV[target]) {
            GLog.w(Messages.get(wep, "ability_no_target"));
            return;
        }

        hero.belongings.abilityWeapon = wep;
        if (!hero.canAttack(enemy)){
            GLog.w(Messages.get(wep, "ability_target_range"));
            hero.belongings.abilityWeapon = null;
            return;
        }
        hero.belongings.abilityWeapon = null;

        //no bonus damage if attack isn't a surprise
        if (enemy instanceof Mob && !((Mob) enemy).surprisedBy(hero)){
            dmgMulti = Math.min(1, dmgMulti);
            dmgBoost = 0;
        }

        float finalDmgMulti = dmgMulti;
        int finalDmgBoost = dmgBoost;
        hero.sprite.attack(enemy.pos, new Callback() {
            @Override
            public void call() {
                wep.beforeAbilityUsed(hero, enemy);
                AttackIndicator.target(enemy);
                if (hero.attack(enemy, finalDmgMulti, finalDmgBoost, Char.INFINITE_ACCURACY)) {
                    Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
                    if (enemy.isAlive()){
                        Buff.affect(enemy, Daze.class, Daze.DURATION);
                    } else {
                        wep.onAbilityKill(hero, enemy);
                    }
                }
                Invisibility.dispel();
                hero.spendAndNext(hero.attackDelay());
                wep.afterAbilityUsed(hero);
            }
        });
    }

}
