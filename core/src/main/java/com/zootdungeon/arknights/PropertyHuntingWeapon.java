package com.zootdungeon.arknights;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;
import java.util.HashSet;

public class PropertyHuntingWeapon extends MeleeWeapon {
    
    // 针对指定property敌人的伤害倍率（额外增加）
    private static final float DAMAGE_MULTIPLIER = 1.5f;
    
    // 要检测的property列表（可以在子类中覆盖或初始化时设置）
    protected HashSet<Char.Property> targetProperties = new HashSet<>();
    
    {
        image = ItemSpriteSheet.SWORD;
        hitSound = Assets.Sounds.HIT_SLASH;
        hitSoundPitch = 1.0f;
        
        tier = 1;
        
        // 默认针对亡灵和恶魔生物
        targetProperties.add(Char.Property.UNDEAD);
        targetProperties.add(Char.Property.DEMONIC);
    }
    
    /**
     * 设置要检测的property列表
     * @param properties 要检测的property集合
     */
    public void setTargetProperties(Char.Property... properties) {
        targetProperties.clear();
        for (Char.Property prop : properties) {
            targetProperties.add(prop);
        }
    }
    
    /**
     * 添加要检测的property
     * @param property 要添加的property
     */
    public void addTargetProperty(Char.Property property) {
        targetProperties.add(property);
    }
    
    /**
     * 检查目标是否有任何指定的property
     * @param defender 目标
     * @return 如果目标有任何一个指定的property则返回true
     */
    protected boolean hasTargetProperty(Char defender) {
        if (defender == null) return false;
        
        for (Char.Property prop : targetProperties) {
            if (Char.hasProp(defender, prop)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String name() {
        return Messages.get(this, "name");
    }
    
    @Override
    public String info() {
        // 构建property列表字符串
        ArrayList<String> propNames = new ArrayList<>();
        
        for (Char.Property prop : targetProperties) {
            propNames.add(Messages.get(this, "property_" + prop.name().toLowerCase()));
        }
        
        String propList;
        if (propNames.isEmpty()) {
            propList = Messages.get(this, "property_none");
        } else {
            propList = String.join(", ", propNames);
        }
        
        return Messages.get(this, "desc", 
                propList,
                (int)((DAMAGE_MULTIPLIER - 1.0f) * 100));
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }
    
    @Override
    public int proc(Char attacker, Char defender, int damage) {
        // 先调用父类的 proc 方法处理基础逻辑
        damage = super.proc(attacker, defender, damage);
        
        // 检查目标是否有指定的property
        if (hasTargetProperty(defender)) {
            // 应用额外伤害（伤害提升会体现在战斗数字上）
            damage = Math.round(damage * DAMAGE_MULTIPLIER);
        }
        
        return damage;
    }
}

