package com.zootdungeon.actors;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import com.zootdungeon.Dungeon;
import com.zootdungeon.Statistics;
import com.zootdungeon.actors.blobs.Blob;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.actors.mobs.Mob;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

/**
 * Actor时间调度系统
 * 
 * <h2>核心概念</h2>
 * Actor是所有需要参与时间调度的游戏实体的基类，包括角色(Char)、状态效果(Buff)、环境效果(Blob)等。
 * 每个Actor维护一个时间值(time)，表示距离下次行动的时间。当time <= now时，Actor可以行动。
 * 
 * <h2>调度机制</h2>
 * 系统通过process()方法持续运行，每轮选择时间最早且已到达行动时间的Actor执行其act()方法。
 * 调度算法支持两种模式：
 * <ul>
 *   <li><b>线性搜索模式</b>：O(n)遍历所有Actor，适用于少量Actor场景（默认）</li>
 *   <li><b>优先队列模式</b>：O(log n)从优先队列获取，当Actor数量>20时自动启用</li>
 * </ul>
 * 
 * <h2>优先级系统</h2>
 * 当多个Actor的时间相同时，通过actPriority决定执行顺序（数值越大优先级越高）：
 * <ul>
 *   <li>VFX_PRIO (100)：视觉效果，最高优先级</li>
 *   <li>HERO_PRIO (0)：英雄基准</li>
 *   <li>BLOB_PRIO (-10)：环境效果</li>
 *   <li>MOB_PRIO (-20)：怪物行动</li>
 *   <li>BUFF_PRIO (-30)：状态效果，最低优先级</li>
 * </ul>
 * 
 * <h2>时间消耗</h2>
 * Actor提供两种时间消耗方法：
 * <ul>
 *   <li><b>spendConstant(time)</b>：精确消耗时间，不受任何时间影响因子影响，用于需要精确控制的场景</li>
 *   <li><b>spend(time)</b>：可被子类重写，Char类会应用时间缩放（slow/haste等效果）</li>
 * </ul>
 * 系统通过normalize()方法处理浮点精度误差，将接近整数的浮点数舍入为整数。
 * 
 * <h2>时间管理</h2>
 * <ul>
 *   <li><b>fixTime()</b>：定期调用，将所有Actor的时间值归一化，防止时间值过大</li>
 *   <li><b>clearTime()</b>：清空Actor时间使其立即行动，Char类会同时清空所有buff的时间</li>
 *   <li><b>timeToNow()</b>：将Actor时间设置为当前时间，使其在下个调度周期行动</li>
 *   <li><b>postpone(time)</b>：延后Actor的行动时间到指定时间点</li>
 * </ul>
 * 
 * <h2>线程安全</h2>
 * 系统通过synchronized保护关键操作，支持中断处理避免阻塞UI线程。
 * 在Char的sprite动画期间会等待动画完成，确保视觉连贯性。
 * 
 * <h2>生命周期</h2>
 * <ul>
 *   <li><b>add()</b>：将Actor添加到调度系统，自动注册ID并设置初始时间</li>
 *   <li><b>remove()</b>：从调度系统中移除Actor，清理所有相关引用</li>
 *   <li><b>onAdd()</b>：Actor添加时的回调，子类可重写执行初始化</li>
 *   <li><b>onRemove()</b>：Actor移除时的回调，子类可重写执行清理</li>
 * </ul>
 * 
 * <h2>特殊处理</h2>
 * <ul>
 *   <li>Char类型的Actor会自动递归添加其所有buff到调度系统</li>
 *   <li>系统会等待Char的sprite动画完成后再执行行动</li>
 *   <li>当Hero死亡时，系统会自动停止处理</li>
 * </ul>
 */
public abstract class Actor implements Bundlable {

    public static final float TICK = 1f;

    private float time;

    private int id = 0;

    public int id() {
        if (id > 0) {
            return id;
        } else {
            return (id = nextID++);
        }
    }

    // 优先级常量：数值越大优先级越高
    protected static final int VFX_PRIO = 100;    // 视觉效果
    protected static final int HERO_PRIO = 0;     // 英雄基准
    protected static final int BLOB_PRIO = -10;   // 环境效果
    protected static final int MOB_PRIO = -20;    // 怪物行动
    protected static final int BUFF_PRIO = -30;   // Buff效果
    private static final int DEFAULT = -100;      // 默认优先级

    /** 行动优先级，时间相同时数值大的先执行 */
    public int actPriority = DEFAULT;

    /**
     * Actor的核心行动方法，需要由子类实现
     * @return true继续处理下一个Actor，false暂停Actor系统（通常玩家需要输入时返回false）
     */
    protected abstract boolean act();

    /** 精确消耗时间，不受时间影响因子影响 */
    protected void spendConstant(float time) {
        this.time += time;
        this.time = normalize(this.time);
    }

    /** 消耗时间，可被子类重写以应用时间修正（如slow/haste） */
    protected void spend(float time) {
        spendConstant(time);
    }

    /** 将行动时间提升到下一个整数 */
    public void spendToWhole() {
        time = (float) Math.ceil(time);
    }

    /** 时间标准化，避免浮点精度误差 */
    private static float normalize(float time) {
        float ex = Math.abs(time % 1f);
        if (ex < .001f) {
            return Math.round(time);
        }
        return time;
    }

    /** 延后Actor的行动时间到指定时间点 */
    protected void postpone(float time) {
        if (this.time < now + time) {
            this.time = normalize(now + time);
        }
    }

    /** 获取距离下次行动的剩余时间，负值表示已可行动 */
    public float cooldown() {
        return time - now;
    }

    /** 清空Actor的时间使其立即行动，子类可重写添加额外逻辑 */
    public void clearTime() {
        spendConstant(-Actor.now());
    }

    /** 将Actor时间设置为当前时间，使其在下个调度周期行动 */
    public void timeToNow() {
        time = now;
    }

    /** 停用Actor，不再参与调度 */
    protected void deactivate() {
        time = Float.MAX_VALUE;
    }

    /** Actor添加时的回调，子类可重写 */
    protected void onAdd() {
    }

    /** Actor移除时的回调，子类可重写 */
    protected void onRemove() {
    }

    private static final String TIME = "time";
    private static final String ID = "id";

    @Override
    public void storeInBundle(Bundle bundle) {
        bundle.put(TIME, time);
        bundle.put(ID, id);
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        time = bundle.getFloat(TIME);
        int incomingID = bundle.getInt(ID);
        if (Actor.findById(incomingID) == null) {
            id = incomingID;
        } else {
            id = nextID++;
        }
    }

    // 静态成员变量
    private static final HashSet<Actor> all = new HashSet<>();
    private static final HashSet<Char> chars = new HashSet<>();
    private static volatile Actor current;
    private static final SparseArray<Actor> ids = new SparseArray<>();
    private static int nextID = 1;

    // 性能优化相关
    private static PriorityQueue<Actor> actorQueue = null;
    private static boolean usePriorityQueue = false;
    private static float now = 0;

    public static float now() {
        return now;
    }

    /** 启用优先队列优化 */
    public static void enablePriorityQueue() {
        if (!usePriorityQueue) {
            usePriorityQueue = true;
            actorQueue = new PriorityQueue<>(new ActorComparator());
            if (!all.isEmpty()) {
                actorQueue.addAll(all);
            }
        }
    }

    /** 禁用优先队列优化，回退到线性搜索 */
    public static void disablePriorityQueue() {
        usePriorityQueue = false;
        if (actorQueue != null) {
            actorQueue.clear();
            actorQueue = null;
        }
    }

    public static boolean isPriorityQueueEnabled() {
        return usePriorityQueue;
    }

    /** Actor比较器，用于优先队列排序：先按时间，时间相同时按优先级 */
    private static class ActorComparator implements Comparator<Actor> {
        @Override
        public int compare(Actor a1, Actor a2) {
            int timeCompare = Float.compare(a1.time, a2.time);
            if (timeCompare != 0) {
                return timeCompare;
            }
            return Integer.compare(a2.actPriority, a1.actPriority);
        }
    }

    public static synchronized void clear() {

        now = 0;

        all.clear();
        chars.clear();
        if (actorQueue != null) {
            actorQueue.clear();
        }

        ids.clear();
    }

    public static synchronized void fixTime() {

        if (all.isEmpty()) {
            return;
        }

        float min = Float.MAX_VALUE;
        for (Actor a : all) {
            if (a.time < min) {
                min = a.time;
            }
        }

        //Only pull everything back by whole numbers
        //So that turns always align with a whole number
        min = (int) min;
        for (Actor a : all) {
            a.time -= min;
        }

        // Rebuild priority queue with updated times
        if (usePriorityQueue && actorQueue != null) {
            actorQueue.clear();
            actorQueue.addAll(all);
        }

        if (Dungeon.hero != null && all.contains(Dungeon.hero)) {
            Statistics.duration += min;
        }
        now -= min;
    }

    /** 初始化Actor系统，按顺序添加英雄、怪物、环境效果 */
    public static void init() {
        add(Dungeon.hero);

        for (Mob mob : Dungeon.level.mobs) {
            add(mob);
        }

        // 怪物需要在所有Actor添加完毕后再恢复目标
        for (Mob mob : Dungeon.level.mobs) {
            mob.restoreEnemy();
        }

        for (Blob blob : Dungeon.level.blobs.values()) {
            add(blob);
        }

        current = null;

        if (!usePriorityQueue && all.size() > 20) {
            enablePriorityQueue();
        }
    }

    private static final String NEXTID = "nextid";

    public static void storeNextID(Bundle bundle) {
        bundle.put(NEXTID, nextID);
    }

    public static void restoreNextID(Bundle bundle) {
        nextID = bundle.getInt(NEXTID);
    }

    public static void resetNextID() {
        nextID = 1;
    }

    /*protected*/
    public void next() {
        if (current == this) {
            current = null;
        }
    }

    public static boolean processing() {
        return current != null;
    }

    public static int curActorPriority() {
        return current != null ? current.actPriority : HERO_PRIO;
    }

    /** 控制Actor处理线程是否保持活跃 */
    public static boolean keepActorThreadAlive = true;

    /** Actor系统的核心处理方法，持续选择并执行Actor */
    public static void process() {

        boolean doNext;
        boolean interrupted = false;

        do {

            current = null;
            if (!interrupted && !Game.switchingScene()) {
                if (usePriorityQueue && actorQueue != null) {
                    // 优先队列模式：O(log n)
                    if (!actorQueue.isEmpty()) {
                        current = actorQueue.poll();
                    }
                } else {
                    // 线性搜索模式：O(n) - 原始稳定实现
                    float earliest = Float.MAX_VALUE;
                    for (Actor actor : all) {
                        //some actors will always go before others if time is equal.
                        if (actor.time < earliest
                                || actor.time == earliest && (current == null || actor.actPriority > current.actPriority)) {
                            earliest = actor.time;
                            current = actor;
                        }
                    }
                }
            }

            if (current != null) {

                now = current.time;
                Actor acting = current;

                if (acting instanceof Char && ((Char) acting).sprite != null) {
                    // If it's character's turn to act, but its sprite
                    // is moving, wait till the movement is over
                    try {
                        Object spriteLock = ((Char) acting).sprite;
                        synchronized (spriteLock) {
                            if (((Char) acting).sprite.isMoving) {
                                spriteLock.wait();
                            }
                        }
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }

                interrupted = interrupted || Thread.interrupted();

                if (interrupted) {
                    doNext = false;
                    current = null;
                } else {
                    doNext = acting.act();
                    if (doNext && (Dungeon.hero == null || !Dungeon.hero.isAlive())) {
                        doNext = false;
                        current = null;
                    }

                    // 重新加入队列（仅在优先队列模式下）
                    if (usePriorityQueue && actorQueue != null && all.contains(acting)) {
                        actorQueue.offer(acting);
                    }
                }
            } else {
                doNext = false;
            }

            if (!doNext) {
                synchronized (Thread.currentThread()) {

                    interrupted = interrupted || Thread.interrupted();

                    if (interrupted) {
                        current = null;
                        interrupted = false;
                    }

                    //signals to the gamescene that actor processing is finished for now
                    Thread.currentThread().notify();

                    try {
                        Thread.currentThread().wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            }

        } while (keepActorThreadAlive);
    }

    /** 立即将Actor添加到调度系统 */
    public static void add(Actor actor) {
        add(actor, now);
    }

    /** 延迟将Actor添加到调度系统 */
    public static void addDelayed(Actor actor, float delay) {
        add(actor, now + Math.max(delay, 0));
    }

    /** Actor添加的核心实现 */
    private static synchronized void add(Actor actor, float time) {

        if (all.contains(actor)) {
            return;
        }

        ids.put(actor.id(), actor);

        all.add(actor);
        actor.time += time;

        // 添加到优先队列（如果启用）
        if (usePriorityQueue && actorQueue != null) {
            actorQueue.offer(actor);
        }

        actor.onAdd();

        if (actor instanceof Char ch) {
            chars.add(ch);
            // 递归添加角色的所有buff
            for (Buff buff : ch.buffs()) {
                add(buff);
            }
        }
    }

    /** 从调度系统中移除Actor，注意不会自动移除角色的buff */
    public static synchronized void remove(Actor actor) {

        if (actor != null) {
            all.remove(actor);
            if (actor instanceof Char ch) {
                chars.remove(ch);
            }

            // 从优先队列移除（如果启用）
            if (usePriorityQueue && actorQueue != null) {
                actorQueue.remove(actor);
            }

            actor.onRemove();

            if (actor.id > 0) {
                ids.remove(actor.id);
            }
        }
    }

    /** 冻结角色及其所有buff的时间，常用于时间暂停、眩晕等效果 */
    public static void delayChar(Char ch, float time) {
        ch.spendConstant(time);
        for (Buff b : ch.buffs()) {
            b.spendConstant(time);
        }
    }

    /** 在指定位置查找角色 */
    public static synchronized Char findChar(int pos) {
        for (Char ch : chars) {
            if (ch.pos == pos) {
                return ch;
            }
        }
        return null;
    }

    /** 通过ID查找Actor */
    public static synchronized Actor findById(int id) {
        return ids.get(id);
    }

    /** 获取所有活跃Actor的副本集合 */
    public static synchronized HashSet<Actor> all() {
        return new HashSet<>(all);
    }

    /** 获取所有角色Actor的副本集合 */
    public static synchronized HashSet<Char> chars() {
        return new HashSet<>(chars);
    }

    /** 确保Actor已正确添加并调用了onAdd()方法 */
    public static boolean ensureActorAdded(Actor actor) {
        if (actor != null && all.contains(actor)) {
            if (actor instanceof Mob && ((Mob) actor).firstAdded) {
                actor.onAdd();
                return true;
            }
        }
        return false;
    }
}
