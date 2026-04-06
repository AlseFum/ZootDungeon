package com.zootdungeon.arknights.ascalon;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.blobs.SmokeScreen;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.buffs.FlavourBuff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.Select;

public class AscalonAOE extends MeleeWeapon {
    
    static {
        SpriteRegistry.texture("sheet.cola.ascalon_weapon", "cola/ascalon_weapon.png")
                .grid(64, 64)
                .label("ascalon_aoe");
    }
    
    {
        image = SpriteRegistry.itemByName("ascalon_aoe");
        tier = 0;
        bones = false;
        RCH=3;
    }
    
    private static final int AOE_RADIUS = 3; // 范围半径
    private static final float AOE_DAMAGE_MULT = 0.3f; // 范围伤害倍率（30%）
    private static final float EVASION_BONUS = 1.5f; // 闪避加成（50%）
    
    @Override
    public String name(){
        return "“复仇者”";
    }

    @Override
    public String desc(){
        return "在阿斯卡纶第一次为军事委员会完成任务后，由特雷西斯亲手赠送，特蕾西娅为她安装的第一把武器。\n\n" +
               "这把武器的每次攻击都会对大范围内的敌人造成伤害并附加持续伤害效果。装备时提供50%的闪避加成。";
    }
    
    @Override
    public boolean doEquip(Hero hero) {
        if (super.doEquip(hero)) {
            // 装备时添加闪避加成 buff
            Buff.affect(hero, EvasionBonus.class);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (super.doUnequip(hero, collect, single)) {
            // 卸下时移除闪避加成 buff
            Buff.detach(hero, EvasionBonus.class);
            return true;
        }
        return false;
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        
        // 每次攻击时触发范围伤害
        if (attacker instanceof Hero && defender.isAlive()) {
            Hero hero = (Hero) attacker;
            boolean inFogPrimary = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0
                    || Blob.volumeAt(defender.pos, SmokeScreen.class) > 0;
            AscalonWound.applyFrom(hero, defender, damage, inFogPrimary);

            for (Char ch : Select.chars().all()
                    .at(Select.placePathRing(defender.pos, Dungeon.level.passable, AOE_RADIUS))
                    .except(Select.chars().of(hero))
                    .except(Select.chars().of(defender))
                    .except(Select.chars().ally())
                    .that(Char::isAlive)
                    .query()) {
                int aoeDamage = Math.max(1, Math.round(damage * AOE_DAMAGE_MULT));
                proc(hero, ch, aoeDamage);
                boolean inFog = Blob.volumeAt(hero.pos, SmokeScreen.class) > 0
                        || Blob.volumeAt(ch.pos, SmokeScreen.class) > 0;
                AscalonWound.applyFrom(hero, ch, aoeDamage, inFog);
            }
        }
        
        return damage;
    }
    
    // 闪避加成 buff
    public static class EvasionBonus extends FlavourBuff {
        
        {
            type = buffType.POSITIVE;
            announced = false; // 不显示在 buff 栏
        }
        
        @Override
        public int icon() {
            return BuffIndicator.DUEL_EVASIVE;
        }
    }
    
    // 静态方法：获取闪避倍率
    public static float evasionMultiplier(Char target) {
        if (target.buff(EvasionBonus.class) != null) {
            return EVASION_BONUS;
        }
        return 1f;
    }
}
