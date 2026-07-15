package com.zootdungeon.actors.mapDevice;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.ArtifactRecharge;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.mechanics.Damage;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 双极装置：受到一定次数攻击后切换极性。
 *
 * <ul>
 *   <li><b>模式 1（充能）</b>：每 {@link #INTERVAL} 回合脉冲一次，
 *       对附近 {@link Char.Alignment#ALLY} 目标施加 {@link ArtifactRecharge}，
 *       对附近 {@link Char.Alignment#ENEMY} 目标造成魔法伤害。</li>
 *   <li><b>模式 2（腐蚀）</b>：每 {@link #INTERVAL} 回合脉冲一次，
 *       对附近 {@link Char.Alignment#ALLY} 目标造成魔法伤害，
 *       对附近 {@link Char.Alignment#ENEMY} 目标恢复生命值。</li>
 *   <li>受到 {@link #HITS_TO_SWITCH} 次攻击后，模式翻转并重置计数。</li>
 * </ul>
 */
public class BipolarDevice extends MapDevice {

    private static final int RADIUS = 3;
    private static final int INTERVAL = 5;

    private static final int HITS_TO_SWITCH = 3;
    private static final int DMG_MIN = 5;
    private static final int DMG_MAX = 10;
    private static final int HEAL_AMOUNT = 5;
    private static final float RECHARGE_TURNS = 10f;

    private boolean modeFriendly = true; // true = 模式1(充能), false = 模式2(腐蚀)
    private int hitsReceived = 0;
    private int cooldown = 0;

    {
        spriteClass = MapDevice.Sprite.class;
        HP = HT = 50;
        properties.add(Property.IMMOVABLE);
        properties.add(Property.INORGANIC);
        properties.add(Property.STATIC);
    }

    public BipolarDevice() {
        super();
    }

    public boolean isModeFriendly() {
        return modeFriendly;
    }

    @Override
    protected boolean act() {
        if (cooldown <= 0) {
            for (Char ch : Actor.chars()) {
                if (ch == this || !ch.isAlive()) continue;
                if (Dungeon.level.distance(pos, ch.pos) > RADIUS) continue;

                if (ch.alignment == Alignment.ALLY) {
                    applyHeroEffect(ch);
                } else if (ch.alignment == Alignment.ENEMY) {
                    applyEnemyEffect(ch);
                }
            }
            cooldown = INTERVAL;
        } else {
            cooldown--;
        }
        spend(TICK);
        return true;
    }

    private void applyHeroEffect(Char hero) {
        if (modeFriendly) {
            // 模式 1：给英雄充能
            Buff.affect(hero, ArtifactRecharge.class).set(RECHARGE_TURNS);
        } else {
            // 模式 2：伤害英雄
            int dmg = Random.NormalIntRange(DMG_MIN, DMG_MAX);
            Damage.additional(this, hero, Damage.MAGIC, dmg, this);
        }
    }

    private void applyEnemyEffect(Char enemy) {
        if (modeFriendly) {
            // 模式 1：伤害敌人
            int dmg = Random.NormalIntRange(DMG_MIN, DMG_MAX);
            Damage.additional(this, enemy, Damage.MAGIC, dmg, this);
        } else {
            // 模式 2：缓慢恢复敌人
            enemy.HP = Math.min(enemy.HT, enemy.HP + HEAL_AMOUNT);
            if (enemy.sprite != null) {
                enemy.sprite.showStatus(com.zootdungeon.sprites.CharSprite.POSITIVE, Integer.toString(HEAL_AMOUNT));
            }
        }
    }

    @Override
    public void receiveDamage(Object src) {
        hitsReceived++;
        if (hitsReceived >= HITS_TO_SWITCH) {
            modeFriendly = !modeFriendly;
            hitsReceived = 0;
            if (sprite != null) {
                sprite.flash();
            }
        }
    }

    private static final String MODE_FRIENDLY = "mode_friendly";
    private static final String HITS_RECEIVED = "hits_received";
    private static final String COOLDOWN = "cooldown";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MODE_FRIENDLY, modeFriendly);
        bundle.put(HITS_RECEIVED, hitsReceived);
        bundle.put(COOLDOWN, cooldown);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        modeFriendly = bundle.getBoolean(MODE_FRIENDLY);
        hitsReceived = bundle.getInt(HITS_RECEIVED);
        cooldown = bundle.getInt(COOLDOWN);
    }
}
