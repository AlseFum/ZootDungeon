package com.zootdungeon.arknights.tragodia;

import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.mobs.Mob;
import com.zootdungeon.effects.CellEmitter;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.effects.Speck;
import com.zootdungeon.effects.particles.ShadowParticle;
import com.zootdungeon.items.wands.Wand;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.items.weapon.melee.MagesStaff;
import com.zootdungeon.levels.Level;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.utils.GLog;
import com.watabou.utils.Callback;

import java.util.ArrayList;

public class TragodiaWand extends Wand {

    private static final String CHARGE = "charge";
    private static final String CHARGE_CAP = "chargeCap";
    private static final String AC_RELEASE_ALL = "RELEASE_ALL";

    private int charge = 0;
    private int chargeCap = 10;
    static {
		SpriteRegistry.registerItemTexture("cola/tragodia_wand.png", 64)
				.label("tragodia_wand");
	}

    {
        image = SpriteRegistry.itemByName("tragodia_wand");

        collisionProperties = Ballistica.MAGIC_BOLT;
    }

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String desc() {
        String d = Messages.get(this, "desc");
        if (charge > 0) {
            d += "\n\n" + Messages.get(this, "desc_charge", charge, chargeCap);
        }
        return d;
    }

    @Override
    public ArrayList<String> actions(com.zootdungeon.actors.hero.Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        // 确保AC_RELEASE_ALL在AC_ZAP之后添加
        actions.add(AC_RELEASE_ALL);
        return actions;
    }

    @Override
    public String actionName(String action, com.zootdungeon.actors.hero.Hero hero) {
        if (action.equals(AC_RELEASE_ALL)) {
            int currentCharge = this.charge;
            return Messages.get(this, "ac_release_all", currentCharge);
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(com.zootdungeon.actors.hero.Hero hero, String action) {
        super.execute(hero, action);

        if (action.equals(AC_RELEASE_ALL)) {
            if (charge > 0) {
                releaseAllCages(hero);
            } else {
                GLog.w(Messages.get(this, "msg_no_charge"));
            }
        }
    }

    private void releaseAllCages(com.zootdungeon.actors.hero.Hero hero) {
        ArrayList<Mob> enemies = new ArrayList<>();
        for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
            if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()) {
                enemies.add(mob);
            }
        }

        if (enemies.isEmpty()) {
            GLog.w(Messages.get(this, "msg_no_enemies"));
            return;
        }

        int usedCharge = charge;
        charge = 0;
        updateQuickslot();

        GLog.p(Messages.get(this, "msg_spent_all", usedCharge));

        for (Mob enemy : enemies) {
            createPrisonCage(enemy.pos, hero);
        }

        hero.spendAndNext(1f);
    }

    @Override
    public void onZap(Ballistica bolt) {
        int targetPos = bolt.collisionPos;
        Char target = Actor.findChar(targetPos);

        if (target != null && target.alignment == Char.Alignment.ENEMY) {
            createPrisonCage(targetPos, curUser);
            wandProc(target, chargesPerCast());
        }
    }

    @Override
    public void fx(Ballistica bolt, Callback callback) {
        MagicMissile.boltFromChar(
                curUser.sprite.parent,
                MagicMissile.MAGIC_MISSILE,
                curUser.sprite,
                bolt.collisionPos,
                callback);
        Sample.INSTANCE.play(Assets.Sounds.ZAP);
    }

    @Override
    public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
        if (defender.alignment == Char.Alignment.ENEMY && defender.isAlive()) {
            // 在敌人附近创建囚笼
            createPrisonCage(defender.pos, (com.zootdungeon.actors.hero.Hero) attacker);

            // 充能
            if (charge < chargeCap) {
                charge++;
                updateQuickslot();
                GLog.p(Messages.get(this, "msg_charge_up", charge, chargeCap));
            }
        }
    }

    private void createPrisonCage(int targetPos, com.zootdungeon.actors.hero.Hero hero) {
        // 找到敌人附近的空位置
        int cagePos = findNearbyEmptyCell(targetPos);
        if (cagePos == -1) {
            // 如果没有空位置，就放在敌人位置
            cagePos = targetPos;
        }

        // 计算持续时间：基础回合数 = tier * 3 + level * 2
        int duration = tier() * 3 + buffedLvl() * 2+10;
        if (duration < 5) duration = 5; // 最少5回合

        // 创建囚笼
        PrisonCage cage = Blob.seed(cagePos, duration, PrisonCage.class);
        cage.setTargetPos(targetPos); // 设置目标位置（敌人位置）
        GameScene.add(cage);

        // 特效
        CellEmitter.get(cagePos).burst(Speck.factory(Speck.STAR), 8);
        Sample.INSTANCE.play(Assets.Sounds.MELD);
    }

    /**
     * 在敌人周围找可放置囚笼的空格：先 8 邻格，再同一方向上的第二格（{@code off * 2}），顺序与 {@link PathFinder#NEIGHBOURS8} 一致。
     */
    private int findNearbyEmptyCell(int centerPos) {
        for (int ring = 1; ring <= 2; ring++) {
            for (int off : PathFinder.NEIGHBOURS8) {
                int p = centerPos + off * ring;
                if (isValidCageCell(p)) {
                    return p;
                }
            }
        }
        return -1;
    }

    private static boolean isValidCageCell(int p) {
        return p >= 0 && p < Dungeon.level.length()
                && Dungeon.level.passable[p]
                && Actor.findChar(p) == null;
    }

    public int tier() {
        // 法杖的tier，基础值为1，每3级提升1个tier
        // 这样可以让高等级法杖的囚笼持续时间更长
        return 1 + (buffedLvl() / 3);
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(CHARGE, charge);
        bundle.put(CHARGE_CAP, chargeCap);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        if (bundle.contains(CHARGE)) {
            charge = bundle.getInt(CHARGE);
        } else {
            charge = 0;
        }
        if (bundle.contains(CHARGE_CAP)) {
            chargeCap = bundle.getInt(CHARGE_CAP);
        } else {
            chargeCap = 10; // 默认值
        }
    }

    // 囚笼Blob类
    public static class PrisonCage extends Blob {

        private int targetPos = -1; // 目标位置（敌人位置）

        {
            actPriority = BLOB_PRIO - 1; // 在普通Blob之前执行
        }

        @Override
        protected void evolve() {
            int cell;
            Level l = Dungeon.level;

            for (int i = area.left; i < area.right; i++) {
                for (int j = area.top; j < area.bottom; j++) {
                    cell = i + j * l.width();
                    if (cur[cell] > 0) {
                        off[cell] = cur[cell] - 1;
                        volume += off[cell];

                        // 如果囚笼还存在，尝试拉取敌人
                        if (off[cell] > 0 && targetPos >= 0) {
                            pullEnemyTowards(cell);
                        }
                    } else {
                        off[cell] = 0;
                    }
                }
            }
        }

        private void pullEnemyTowards(int cagePos) {
            // 寻找附近的敌人
            Char target = null;
            int closestDist = Integer.MAX_VALUE;

            for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
                if (mob.alignment == Char.Alignment.ENEMY && mob.isAlive()) {
                    int dist = Dungeon.level.distance(mob.pos, cagePos);
                    // 只拉取距离囚笼3格以内的敌人
                    if (dist <= 3 && dist < closestDist) {
                        target = mob;
                        closestDist = dist;
                    }
                }
            }

            if (target != null && closestDist > 0) {
                // 计算拉取方向
                int dx = (cagePos % Dungeon.level.width()) - (target.pos % Dungeon.level.width());
                int dy = (cagePos / Dungeon.level.width()) - (target.pos / Dungeon.level.width());

                // 尝试移动到更靠近囚笼的位置
                int newPos = -1;

                // 优先选择直接朝向囚笼的方向
                if (Math.abs(dx) > Math.abs(dy)) {
                    // 水平方向优先
                    if (dx > 0) {
                        newPos = target.pos + 1; // 向右
                    } else {
                        newPos = target.pos - 1; // 向左
                    }
                } else {
                    // 垂直方向优先
                    if (dy > 0) {
                        newPos = target.pos + Dungeon.level.width(); // 向下
                    } else {
                        newPos = target.pos - Dungeon.level.width(); // 向上
                    }
                }

                // 检查新位置是否有效
                if (newPos >= 0 && newPos < Dungeon.level.length()
                        && Dungeon.level.passable[newPos]
                        && Actor.findChar(newPos) == null) {
                    // 移动敌人
                    target.pos = newPos;
                    target.sprite.place(newPos);
                    Dungeon.level.occupyCell(target);

                    // 特效
                    CellEmitter.get(newPos).burst(Speck.factory(Speck.STAR), 3);
                } else {
                    // 如果直接方向不可行，尝试对角线方向
                    for (int offset : PathFinder.NEIGHBOURS8) {
                        newPos = target.pos + offset;
                        if (newPos >= 0 && newPos < Dungeon.level.length()
                                && Dungeon.level.passable[newPos]
                                && Actor.findChar(newPos) == null) {
                            // 检查是否更靠近囚笼
                            int newDist = Dungeon.level.distance(newPos, cagePos);
                            if (newDist < closestDist) {
                                target.pos = newPos;
                                target.sprite.place(newPos);
                                Dungeon.level.occupyCell(target);
                                CellEmitter.get(newPos).burst(Speck.factory(Speck.STAR), 3);
                                break;
                            }
                        }
                    }
                }
            }
        }

        public void setTargetPos(int pos) {
            targetPos = pos;
        }

        @Override
        public void use(com.zootdungeon.effects.BlobEmitter emitter) {
            super.use(emitter);
            emitter.pour(ShadowParticle.UP, 0.1f);
        }

        @Override
        public String tileDesc() {
            return Messages.get(this, "desc");
        }

        private static final String TARGET_POS = "targetPos";

        @Override
        public void storeInBundle(Bundle bundle) {
            super.storeInBundle(bundle);
            bundle.put(TARGET_POS, targetPos);
        }

        @Override
        public void restoreFromBundle(Bundle bundle) {
            super.restoreFromBundle(bundle);
            targetPos = bundle.getInt(TARGET_POS);
        }
    }
}
