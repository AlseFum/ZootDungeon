package com.zootdungeon.arknights.ascalon;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.SmokeScreen;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.sprites.SpriteRegistry;

public class AscalonAmbush extends AmbushWeapon {
    
    static {
        SpriteRegistry.registerItemTexture("cola/ascalon_weapon.png",64)
                .label("ascalon_ambush");
    }
    
    {
        image = SpriteRegistry.itemByName("ascalon_ambush");
        tier = 0;
        bones = false;
        ambushRate=1.2f;
    }
    
    @Override
    public String name(){
        return "“复仇者·伏击”";
    }

    @Override
    public String desc(){
        return "在阿斯卡纶第一次为军事委员会完成任务后，由特雷西斯亲手赠送，特蕾西娅为她安装的第一把武器。\n\n" +
               "这把武器只在伏击敌人时触发特殊效果，造成高额伤害并附加持续伤害效果。";
    }
    
    @Override
    public int damageRoll(Char owner) {
        if (owner instanceof Hero) {
            Hero hero = (Hero)owner;
            Char enemy = hero.enemy();
            if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
                // 检查是否在暗夜迷雾中
                float currentAmbushRate = ambushRate;
                if (Blob.volumeAt(hero.pos, SmokeScreen.class) > 0 
                        || (enemy != null && Blob.volumeAt(enemy.pos, SmokeScreen.class) > 0)) {
                    currentAmbushRate = 2.4f;
                }
                
                // surprise hit: 偏向高端的基础伤害，再加上 exSTR 骰
                int lvl = buffedLvl();
                int mn = min(lvl);
                int mx = max(lvl);
                int diff = mx - mn;

                // 基础部分：将 [mn, mx] 区间压缩到 [mn+currentAmbushRate*diff, mx+currentAmbushRate*diff]
                int biasedMin = mn + Math.round(diff * currentAmbushRate);
                int biasedMax = mx + Math.round(diff * currentAmbushRate);
                if (biasedMin > biasedMax) biasedMin = biasedMax;

                int damage = augment.damageFactor(com.watabou.utils.Random.NormalIntRange(biasedMin, biasedMax));
                damage = hero.heroDamageIntRange(damage, STRReq());
                return damage;
            }
        }
        return super.damageRoll(owner);
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        
        // 只在伏击时应用 Wound buff
        if (attacker instanceof Hero && defender.isAlive()) {
            Hero hero = (Hero) attacker;
            Char enemy = hero.enemy();
            
            // 检查是否是伏击攻击
            if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
                // 在攻击时判断是否在迷雾中
                boolean inFog = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0 
                        || Blob.volumeAt(defender.pos, SmokeScreen.class) > 0;
                
                AscalonWound existingWound = defender.buff(AscalonWound.class);
                AscalonWound wound;
                if (existingWound != null) {
                    wound = existingWound;
                } else {
                    wound = new AscalonWound();
                    wound.attachTo(defender);
                }
                // 如果buff刚被创建或剩余时间小于DURATION，则延长到DURATION
                float duration = AscalonWound.DURATION * defender.resist(AscalonWound.class);
                wound.extend(duration);
                // 设置hero引用、攻击时的攻击力和迷雾状态
                float attackDamage = hero.damageRoll();
                wound.set(hero, attackDamage, inFog);
            }
        }
        
        return damage;
    }
}
