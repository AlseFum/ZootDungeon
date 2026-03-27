/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2025 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.zootdungeon.items.weapon.configurable;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 双刀：单把可投掷，两把可组合为一把；组合后每次攻击会攻击两次。
 */
public class TwinBlade extends MeleeWeapon {

    public static final String AC_COMBINE = "COMBINE";
    public static final String AC_THROW = "THROW";

    {
        image = ItemSpriteSheet.THROWING_KNIFE;
        tier = 2;
        bones = false;
    }

    /** 是否为组合状态（两把合为一把） */
    private boolean combined = false;
    private float doubleStrikeChance = 1f;

    @Override
    public String name() {
        return combined ? "双刀（组合）" : "双刀";
    }

    @Override
    public String desc() {
        if (combined) {
            return "两把刀组合而成。有概率触发第二次挥击。\n\n可拆分回两把单刀（拆分后需重新拾取）。";
        }
        return "一把轻便的短刀，可近战也可投掷。\n\n拥有两把时可以选择「组合」，合为一把双刀，攻击时会连续挥击两次。";
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (!combined) {
            actions.add(AC_THROW);
            if (countSingleBlades(hero) >= 2) {
                actions.add(AC_COMBINE);
            }
        }
        return actions;
    }

    /** 英雄身上（背包+装备）的单刀数量 */
    private int countSingleBlades(Hero hero) {
        int n = 0;
        if (hero.belongings.weapon instanceof TwinBlade && !((TwinBlade) hero.belongings.weapon).combined) {
            n++;
        }
        for (Item i : hero.belongings.backpack) {
            if (i instanceof TwinBlade && !((TwinBlade) i).combined) {
                n += i.quantity();
            }
        }
        return n;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_COMBINE)) return "组合";
        if (action.equals(AC_THROW)) return "投掷";
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_COMBINE)) {
            doCombine(hero);
            return;
        }
        if (action.equals(AC_THROW)) {
            if (isEquipped(hero)) {
                doUnequip(hero, true, false);
            }
            curUser = hero;
            curItem = this;
            doThrow(hero);
            return;
        }
        super.execute(hero, action);
    }

    private void doCombine(Hero hero) {
        if (countSingleBlades(hero) < 2) {
            GLog.w("需要至少两把双刀才能组合。");
            return;
        }
        // 先确保两把都在背包里：若装备了单刀则卸下
        if (hero.belongings.weapon instanceof TwinBlade && !((TwinBlade) hero.belongings.weapon).combined) {
            hero.belongings.weapon.doUnequip(hero, true, false);
        }
        TwinBlade first = null;
        for (Item i : hero.belongings.backpack) {
            if (i instanceof TwinBlade && !((TwinBlade) i).combined) {
                first = (TwinBlade) i;
                break;
            }
        }
        if (first == null) {
            GLog.w("需要至少两把双刀才能组合。");
            return;
        }
        int lvl = first.level();
        first.detach(hero.belongings.backpack);
        for (Item i : hero.belongings.backpack) {
            if (i instanceof TwinBlade && !((TwinBlade) i).combined) {
                lvl = Math.max(lvl, ((TwinBlade) i).level());
                ((TwinBlade) i).detach(hero.belongings.backpack);
                break;
            }
        }
        TwinBlade combinedBlade = new TwinBlade();
        combinedBlade.combined = true;
        combinedBlade.level(lvl);
        combinedBlade.identify();
        if (!combinedBlade.collect(hero.belongings.backpack)) {
            Dungeon.level.drop(combinedBlade, hero.pos).sprite.drop();
        }
        GLog.p("两把刀组合成双刀！");
        updateQuickslot();
    }

    @Override
    public void doThrow(Hero hero) {
        GameScene.selectCell(thrower);
    }

    private static CellSelector.Listener thrower = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target != null && curItem instanceof TwinBlade) {
                ((TwinBlade) curItem).cast(curUser, target);
            }
        }
        @Override
        public String prompt() {
            return "选择投掷目标";
        }
    };

    private static Hero curUser;
    private static Item curItem;

    public void cast(final Hero user, final int dst) {
        curUser = user;
        curItem = this;
        final int cell = throwPos(user, dst);
        user.sprite.zap(cell);
        user.busy();
        Char enemy = Actor.findChar(cell);
        if (enemy != null && enemy != user) {
            user.belongings.thrownWeapon = this;
            boolean hit = user.attack(enemy);
            user.belongings.thrownWeapon = null;
            if (!hit) {
                user.spendAndNext(1f);
                Item i = detach(user.belongings.backpack);
                if (i != null) Dungeon.level.drop(i, cell).sprite.drop();
                curUser = null;
                curItem = null;
            } else {
                onThrowHit(user, enemy, cell);
            }
        } else {
            user.spendAndNext(1f);
            Item i = detach(user.belongings.backpack);
            if (i != null) Dungeon.level.drop(i, cell).sprite.drop();
            curUser = null;
            curItem = null;
        }
    }

    public int throwPos(Hero user, int dst) {
        return new Ballistica(user.pos, dst, Ballistica.PROJECTILE).collisionPos;
    }

    private void onThrowHit(Hero user, Char enemy, int cell) {
        Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH, 1f, 1.2f);
        Item i = detach(user.belongings.backpack);
        if (i != null) Dungeon.level.drop(i, cell).sprite.drop();
        user.spendAndNext(1f);
        curUser = null;
        curItem = null;
    }

    @Override
    public int min(int lvl) {
        return tier + lvl;
    }

    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }

    public TwinBlade randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        combined = Random.Int(5) == 0;
        doubleStrikeChance = Random.Float(0.35f, 1f);
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }

    // --------------- 组合态：攻击两次 ---------------
    @Override
    public float delayFactor(Char owner) {
        if (!combined) return super.delayFactor(owner);
        if (!(owner instanceof Hero)) return super.delayFactor(owner);
        Hero hero = (Hero) owner;
        TwinStrikeBuff b = hero.buff(TwinStrikeBuff.class);
        if (b != null && b.secondTarget != null) {
            return 0f; // 第一击后不消耗时间，由 SecondStrikeActor 触发第二击
        }
        return super.delayFactor(owner);
    }

    @Override
    public void activate(Char ch) {
        super.activate(ch);
        if (ch instanceof Hero && combined) {
            Buff.affect(ch, TwinStrikeBuff.class);
        }
    }

    @Override
    public boolean doUnequip(Hero hero, boolean collect, boolean single) {
        if (super.doUnequip(hero, collect, single)) {
            TwinStrikeBuff b = hero.buff(TwinStrikeBuff.class);
            if (b != null) b.detach();
            return true;
        }
        return false;
    }

    @Override
    public int proc(Char attacker, Char defender, int damage) {
        damage = super.proc(attacker, defender, damage);
        if (combined && attacker instanceof Hero && Random.Float() < doubleStrikeChance) {
            Hero hero = (Hero) attacker;
            TwinStrikeBuff b = hero.buff(TwinStrikeBuff.class);
            if (b != null) {
                b.secondTarget = defender;
                Actor.add(new SecondStrikeActor(b));
            }
        }
        return damage;
    }

    public static class TwinStrikeBuff extends Buff {
        public Char secondTarget = null;
        { actPriority = BUFF_PRIO; }
        @Override
        public boolean act() {
            spend(TICK);
            return true;
        }
    }

    private static class SecondStrikeActor extends Actor {
        private final TwinStrikeBuff buff;
        SecondStrikeActor(TwinStrikeBuff buff) {
            this.buff = buff;
            actPriority = 5; // 比英雄稍高，紧接在英雄之后执行
        }
        @Override
        protected boolean act() {
            Hero hero = Dungeon.hero;
            if (hero == null || !hero.isAlive() || buff.secondTarget == null) {
                Actor.remove(this);
                return true;
            }
            Char target = buff.secondTarget;
            if (!target.isAlive() || !Dungeon.level.adjacent(hero.pos, target.pos)) {
                buff.secondTarget = null;
                Actor.remove(this);
                return true;
            }
            // 第二击立即结算（不重复播攻击动画，避免与回合调度冲突）
            hero.attack(target);
            buff.secondTarget = null;
            hero.spend(hero.attackDelay());
            hero.next();
            Actor.remove(this);
            return true;
        }
    }

    // --------------- 序列化 ---------------
    private static final String COMBINED = "combined";
    private static final String DOUBLE_STRIKE_CHANCE = "doubleStrikeChance";

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(COMBINED, combined);
        bundle.put(DOUBLE_STRIKE_CHANCE, doubleStrikeChance);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        combined = bundle.getBoolean(COMBINED);
        if (bundle.contains(DOUBLE_STRIKE_CHANCE)) {
            doubleStrikeChance = bundle.getFloat(DOUBLE_STRIKE_CHANCE);
        }
    }
}
