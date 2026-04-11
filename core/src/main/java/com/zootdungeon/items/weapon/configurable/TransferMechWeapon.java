package com.zootdungeon.items.weapon.configurable;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.melee.MeleeWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.CharSprite;
import com.zootdungeon.sprites.GhostSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.BuffIndicator;
import com.zootdungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashSet;

public class TransferMechWeapon extends MeleeWeapon {
    
    private static final String AC_RELEASE_MECH = "RELEASE_MECH";
    private static final String MECH_COUNT = "mechCount";
    private static final String AOE_RANGE = "aoeRange";
    private static final String SEARCH_RANGE = "searchRange";
    
    // 存储所有活跃的mech（包括buff和char形式）
    private HashSet<TransferMechBuff> activeMechBuffs = new HashSet<>();
    private HashSet<TransferMechChar> activeMechChars = new HashSet<>();
    public int aoeRange = 2;
    public int searchRange = 8;
    
    {
        image = ItemSpriteSheet.WAND_WARDING;
        tier = 0;
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        if (isEquipped(hero)) {
            actions.add(AC_RELEASE_MECH);
        }
        return actions;
    }
    
    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_RELEASE_MECH)) {
            return Messages.get(this, "ac_release");
        }
        return super.actionName(action, hero);
    }
    
    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        
        if (action.equals(AC_RELEASE_MECH)) {
            GameScene.selectCell(new MechTargeter(hero));
        }
    }
    
    private class MechTargeter extends CellSelector.Listener {
        private Hero hero;
        
        public MechTargeter(Hero hero) {
            this.hero = hero;
        }
        
        @Override
        public void onSelect(Integer target) {
            if(target == null){
                return;
            }
            Char targetChar = Actor.findChar(target);
            if (targetChar != null && targetChar.alignment == Char.Alignment.ENEMY) {
                releaseMechs(targetChar, hero);
            } else {
                GLog.w(Messages.get(TransferMechWeapon.class, "msg_need_enemy"));
            }
        }
        
        @Override
        public String prompt() {
            return Messages.get(TransferMechWeapon.class, "prompt_target");
        }
    }
    
    private void releaseMechs(Char target, Hero hero) {
        // 根据 tier 和强化等级计算 mech 数量：2 + tier + level
        int mechCount = Math.max(1, 1 + tier + buffedLvl());

        // 优先追踪：点击点附近的敌人（通常就是 target，本逻辑允许“附近替换”）
        Char preferred = findNearestEnemyAround(target.pos, searchRange);
        if (preferred == null) preferred = target;

        int spawned = 0;
        for (int i = 0; i < mechCount; i++) {
            // 先释放 mech（char 形态），追到目标后再转换为 buff
            int spawnPos = findNearbyEmptyCell(hero.pos, 6);
            if (spawnPos == -1) {
                // 没有可用空位：回退为直接挂 buff，保证技能仍然生效
                TransferMechBuff mech = new TransferMechBuff();
                mech.weapon = this;
                mech.power = tier + buffedLvl();
                mech.aoeRange = aoeRange;
                if (mech.attachTo(preferred)) {
                    activeMechBuffs.add(mech);
                    spawned++;
                }
                continue;
            }

            TransferMechChar mechChar = new TransferMechChar();
            mechChar.weapon = this;
            mechChar.power = tier + buffedLvl();
            mechChar.searchRange = searchRange;
            mechChar.forcedTargetId = preferred.id();
            mechChar.pos = spawnPos;
            mechChar.initStats();
            mechChar.state = mechChar.HUNTING;

            GameScene.add(mechChar);
            activeMechChars.add(mechChar);
            spawned++;
        }
        
        // 特效
        if (spawned > 0) {
            CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STAR), spawned);
        }
        Sample.INSTANCE.play(Assets.Sounds.MELD);
        
        GLog.p(Messages.get(TransferMechWeapon.class, "msg_released", spawned));
        hero.spendAndNext(1f);
    }

    private int findNearbyEmptyCell(int centerPos, int maxSteps) {
        if (maxSteps < 1) maxSteps = 1;

        PathFinder.buildDistanceMap(centerPos, Dungeon.level.passable, maxSteps);

        int best = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < Dungeon.level.length(); i++) {
            int d = PathFinder.distance[i];
            if (d > 0 && d <= maxSteps
                    && Dungeon.level.passable[i]
                    && Actor.findChar(i) == null) {
                if (d < bestDist) {
                    best = i;
                    bestDist = d;
                }
            }
        }
        return best;
    }

    private Char findNearestEnemyAround(int centerPos, int range) {
        Char closest = null;
        int closestDist = Integer.MAX_VALUE;
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob != null && mob.isAlive() && mob.alignment == Char.Alignment.ENEMY) {
                int dist = Dungeon.level.distance(centerPos, mob.pos);
                if (dist <= range && dist < closestDist) {
                    closest = mob;
                    closestDist = dist;
                }
            }
        }
        return closest;
    }
    
    // 当敌人死亡时，将buff转换为char
    public void convertBuffToChar(TransferMechBuff buff, int deathPos) {
        activeMechBuffs.remove(buff);
        
        // 创建char形式的mech
        int spawnPos = findNearbyEmptyCell(deathPos, 6);
        if (spawnPos == -1) return;

        TransferMechChar mechChar = new TransferMechChar();
        mechChar.weapon = this;
        mechChar.power = buff.power;
        mechChar.searchRange = searchRange;
        mechChar.pos = spawnPos;
        mechChar.initStats();
        mechChar.state = mechChar.HUNTING;
        
        GameScene.add(mechChar);
        activeMechChars.add(mechChar);
        
        // 特效
        CellEmitter.get(deathPos).burst(Speck.factory(Speck.STAR), 6);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }
    
    // 当char找到新目标时，转换为buff
    public void convertCharToBuff(TransferMechChar mechChar, Char newTarget) {
        activeMechChars.remove(mechChar);
        mechChar.die(null);
        
        // 创建buff形式的mech
        TransferMechBuff mechBuff = new TransferMechBuff();
        mechBuff.weapon = this;
        mechBuff.power = mechChar.power;
        mechBuff.aoeRange = aoeRange;
        if (mechBuff.attachTo(newTarget)) {
            activeMechBuffs.add(mechBuff);
        }
        
        // 特效
        CellEmitter.get(newTarget.pos).burst(Speck.factory(Speck.STAR), 4);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }
    
    // 移除mech
    public void removeMechBuff(TransferMechBuff mech) {
        activeMechBuffs.remove(mech);
    }
    
    public void removeMechChar(TransferMechChar mech) {
        activeMechChars.remove(mech);
    }
    
    // 获取所有活跃的mech
    public HashSet<TransferMechBuff> getActiveMechBuffs() {
        return new HashSet<>(activeMechBuffs);
    }
    
    public HashSet<TransferMechChar> getActiveMechChars() {
        return new HashSet<>(activeMechChars);
    }
    
    @Override
    public int min(int lvl) {
        return tier + lvl;
    }
    
    @Override
    public int max(int lvl) {
        return 5 * (tier + 1) + lvl * (tier + 1);
    }

    public TransferMechWeapon randomize() {
        tier = Random.IntRange(1, 5);
        level(Random.IntRange(0, 3));
        aoeRange = Random.IntRange(1, 3);
        searchRange = Random.IntRange(5, 12);
        return this;
    }

    @Override
    public Item random() {
        return randomize();
    }
    
    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(MECH_COUNT, activeMechBuffs.size() + activeMechChars.size());
        bundle.put(AOE_RANGE, aoeRange);
        bundle.put(SEARCH_RANGE, searchRange);
    }
    
    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        // 注意：mech会在游戏加载时自动恢复，这里只需要清空集合
        activeMechBuffs.clear();
        activeMechChars.clear();
        if (bundle.contains(AOE_RANGE)) aoeRange = bundle.getInt(AOE_RANGE);
        if (bundle.contains(SEARCH_RANGE)) searchRange = bundle.getInt(SEARCH_RANGE);
    }
    
    // Mech Buff类
    public static class TransferMechBuff extends Buff {
        
        public TransferMechWeapon weapon;
        public int power = 1;
        public int aoeRange = 2;

        /** 跟随目标 {@link CharSprite} 的机甲附着特效，不写入存档 */
        private transient Emitter mechSpriteFx;
        
        {
            type = buffType.NEGATIVE;
        }

        @Override
        public String name() {
            return Messages.get(TransferMechBuff.class, "name");
        }
        
        @Override
        public int icon() {
            return BuffIndicator.CORRUPT;
        }
        
        @Override
        public void tintIcon(Image icon) {
            icon.hardlight(0.5f, 0.5f, 1f); // 蓝色
        }
        
        @Override
        public String iconTextDisplay() {
            return String.valueOf(power);
        }

        @Override
        public void fx(boolean on) {
            if (!on) {
                clearMechSpriteFx();
            }
        }

        @Override
        public void drawSpriteOverlay(CharSprite sprite) {
            if (target == null || !target.isAlive() || sprite == null || sprite.ch != target) {
                clearMechSpriteFx();
                return;
            }
            if (!sprite.visible) {
                clearMechSpriteFx();
                return;
            }
            if (mechSpriteFx == null || !mechSpriteFx.alive) {
                mechSpriteFx = GameScene.emitter();
                if (mechSpriteFx != null) {
                    mechSpriteFx.pos(sprite);
                    float rate = Math.min(0.2f, 0.06f + power * 0.025f);
                    mechSpriteFx.pour(Speck.factory(Speck.STAR), rate);
                }
            }
        }

        private void clearMechSpriteFx() {
            if (mechSpriteFx != null) {
                mechSpriteFx.on = false;
                mechSpriteFx.killAndErase();
                mechSpriteFx = null;
            }
        }
        
        @Override
        public boolean act() {
            if (target == null || !target.isAlive()) {
                // 目标死亡，转换为char
                if (weapon != null && target != null) {
                    int deathPos = target.pos;
                    weapon.convertBuffToChar(this, deathPos);
                }
                detach();
                return true;
            }
            
            // 对目标本身造成伤害
            int damage = Random.NormalIntRange(power, power * 2);
            target.damage(damage, this);
            
            // 对附近单位造成伤害
            damageNearbyEnemies();
            
            spend(TICK);
            return true;
        }
        
        private void damageNearbyEnemies() {
            if (target == null || !target.isAlive()) {
                return;
            }
            
            int centerPos = target.pos;
            
            // 对范围内的所有敌人造成伤害
            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob != null && mob.isAlive() && mob != target
                        && Dungeon.level.distance(centerPos, mob.pos) <= aoeRange) {
                    int aoeDamage = Random.NormalIntRange(power, power * 2);
                    mob.damage(aoeDamage, this);
                }
            }
        }
        
        @Override
        public void detach() {
            clearMechSpriteFx();
            if (weapon != null) {
                // 只有当目标还活着时才转换为char（说明是被净化等效果移除的）
                // 如果目标死亡，在act()中已经处理过转换了
                if (target != null && target.isAlive()) {
                    weapon.convertBuffToChar(this, target.pos);
                }
                weapon.removeMechBuff(this);
            }
            super.detach();
        }
        
        private static final String POWER = "power";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(POWER, power);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            power = bundle.getInt(POWER);
        }
        
        @Override
        public String desc() {
            return Messages.get(TransferMechWeapon.class, "mech_buff_desc", power);
        }
    }
    
    // Mech Char类
    public static class TransferMechChar extends Mob {
        
        public TransferMechWeapon weapon;
        public int power = 1;
        public int searchRange = 8; // 搜索敌人的范围
        public int forcedTargetId = -1; // 优先追踪的目标（释放时指定）
        
        {
            spriteClass = GhostSprite.class;
            alignment = Alignment.ALLY;
            intelligentAlly = true;
            
            lootChance = 0f;
            EXP = 0;
            
            state = HUNTING;
        }
        
        public TransferMechChar() {
            // 属性在 power 赋值后初始化
        }

        public void initStats() {
            HP = HT = 10 + power * 2;
            defenseSkill = 5 + power;
            maxLvl = Math.max(1, power);
        }
        
        @Override
        public int damageRoll() {
            return Random.NormalIntRange(power, power * 2);
        }
        
        @Override
        public int attackSkill(Char target) {
            return 10 + power * 2;
        }
        
        @Override
        public int drRoll() {
            return Random.NormalIntRange(0, power / 2);
        }
        
        @Override
        public String name() {
            return Messages.get(TransferMechChar.class, "name");
        }
        
        @Override
        public String description() {
            return Messages.get(TransferMechChar.class, "desc");
        }
        
        @Override
        protected boolean act() {
            // 如果英雄死亡，mech也消失
            if (Dungeon.hero == null || !Dungeon.hero.isAlive()) {
                if (weapon != null) {
                    weapon.removeMechChar(this);
                }
                die(null);
                return true;
            }
            
            // 初始化视野数组
            if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()) {
                fieldOfView = new boolean[Dungeon.level.length()];
            }
            
            // 更新视野
            Dungeon.level.updateFieldOfView(this, fieldOfView);
            
            // 寻找附近的敌人
            Char target = findForcedTargetOrNearbyEnemy();
            
            if (target != null) {
                // 找到敌人，移动到敌人位置并转换为buff
                if (Dungeon.level.adjacent(pos, target.pos)) {
                    // 已经在相邻位置，直接转换
                    if (weapon != null) {
                        weapon.convertCharToBuff(this, target);
                    }
                    spend(1 / speed());
                    return true;
                } else {
                    // 移动到敌人附近（getCloser 只改 pos，必须 moveSprite 才会显示位移，与 Mob.Hunting / DirectableAlly 一致）
                    int oldPos = pos;
                    if (getCloser(target.pos)) {
                        spend(1 / speed());
                        return moveSprite(oldPos, pos);
                    } else {
                        spend(1 / speed());
                        return true;
                    }
                }
            } else {
                // 没有找到敌人，等待
                spend(1 / speed());
                return true;
            }
        }
        
        private Char findForcedTargetOrNearbyEnemy() {
            if (forcedTargetId != -1) {
                Char forced = (Char) Actor.findById(forcedTargetId);
                if (forced != null && forced.isAlive() && forced.alignment == Alignment.ENEMY) {
                    // 不要求在视野内，保证“追到指定目标”
                    return forced;
                }
                forcedTargetId = -1;
            }

            // 与 Mob.chooseEnemy 类似：用路径步数选目标，优先追能走到的敌人；不再要求敌人在机甲视野内
            PathFinder.buildDistanceMap(pos, Dungeon.findPassable(this, Dungeon.level.passable, fieldOfView, true));

            Char pick = pickChaseTarget(true);
            if (pick != null) {
                return pick;
            }
            // 范围内没有敌人时，全图追击最近可路径/几何距离的敌人
            return pickChaseTarget(false);
        }

        /**
         * @param onlyWithinSearchRange 为 true 时只考虑与机甲距离 {@link #searchRange} 内的敌人
         */
        private Char pickChaseTarget(boolean onlyWithinSearchRange) {
            Char best = null;
            int bestPath = Integer.MAX_VALUE;
            int bestGeom = Integer.MAX_VALUE;

            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob == null || !mob.isAlive() || mob.alignment != Alignment.ENEMY) {
                    continue;
                }
                int geom = Dungeon.level.distance(pos, mob.pos);
                if (onlyWithinSearchRange && geom > searchRange) {
                    continue;
                }
                int p = steppingDistToAdjacent(mob);
                if (p < bestPath || (p == bestPath && geom < bestGeom)) {
                    bestPath = p;
                    bestGeom = geom;
                    best = mob;
                }
            }
            return best;
        }

        /** 从机甲当前位置走到 {@code ch} 邻格的最少步数（与 {@link Mob#chooseEnemy} 一致） */
        private static int steppingDistToAdjacent(Char ch) {
            int d = Integer.MAX_VALUE;
            for (int n : PathFinder.NEIGHBOURS8) {
                int c = ch.pos + n;
                if (!Dungeon.level.insideMap(c) || !Dungeon.level.adjacent(ch.pos, c)) {
                    continue;
                }
                int step = PathFinder.distance[c];
                if (step < d) {
                    d = step;
                }
            }
            return d;
        }
        
        @Override
        public void die(Object cause) {
            if (weapon != null) {
                weapon.removeMechChar(this);
            }
            super.die(cause);
        }
        
        @Override
        public Item createLoot() {
            return null;
        }
        
        private static final String POWER = "power";
        
        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(POWER, power);
        }
        
        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            power = bundle.getInt(POWER);
            // 重新设置属性
            initStats();
        }
    }
}
