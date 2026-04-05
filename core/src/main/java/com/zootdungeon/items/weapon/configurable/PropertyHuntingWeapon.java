package com.zootdungeon.items.weapon.configurable;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.Char;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashSet;

public class PropertyHuntingWeapon extends MeleeWeapon {
    
    // 针对指定property敌人的伤害倍率（额外增加）
    private float damageMultiplier = 1.5f;
    
    // 要检测的property列表（可以在子类中覆盖或初始化时设置）
    protected HashSet<Char.Property> targetProperties = new HashSet<>();
    private static final String TARGET_PROPERTIES = "targetProperties";
    private static final String DAMAGE_MULTIPLIER = "damageMultiplier";
    
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
    public String desc() {
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
                (int) ((damageMultiplier - 1.0f) * 100));
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
            damage = Math.round(damage * damageMultiplier);
        }
        
        return damage;
    }

    public PropertyHuntingWeapon randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        damageMultiplier = Random.Float(1.2f, 2.0f);
        targetProperties.clear();
        Char.Property[] values = Char.Property.values();
        int count = Random.IntRange(1, Math.min(3, values.length));
        for (int i = 0; i < count; i++) {
            targetProperties.add(values[Random.Int(values.length)]);
        }
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        String[] props = new String[targetProperties.size()];
        int i = 0;
        for (Char.Property p : targetProperties) {
            props[i++] = p.name();
        }
        bundle.put(TARGET_PROPERTIES, props);
        bundle.put(DAMAGE_MULTIPLIER, damageMultiplier);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(TARGET_PROPERTIES)) {
            targetProperties.clear();
            for (String name : bundle.getStringArray(TARGET_PROPERTIES)) {
                try {
                    targetProperties.add(Char.Property.valueOf(name));
                } catch (Exception ignored) {
                }
            }
            if (targetProperties.isEmpty()) {
                targetProperties.add(Char.Property.UNDEAD);
                targetProperties.add(Char.Property.DEMONIC);
            }
        }
        if (bundle.contains(DAMAGE_MULTIPLIER)) {
            damageMultiplier = bundle.getFloat(DAMAGE_MULTIPLIER);
        }
    }
}

