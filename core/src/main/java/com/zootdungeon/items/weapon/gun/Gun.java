package com.zootdungeon.items.weapon.gun;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.zootdungeon.Assets;
import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.weapon.Weapon;
import com.zootdungeon.items.weapon.ammo.Ammo;
import com.zootdungeon.items.weapon.ammo.Cartridge;
import com.zootdungeon.items.weapon.ammo.CartridgeAltFire;
import com.zootdungeon.effects.MagicMissile;
import com.zootdungeon.items.weapon.ammo.CartridgeEffect;
import com.zootdungeon.mechanics.Ballistica;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.SpriteRegistry;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndOptions;
import com.watabou.noosa.Group;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Gun extends Weapon {

    /** Action keys for fire/reload; display text via Messages (ac_fire, ac_reload). */
    protected static final String AC_RELOAD = "reload";
    protected static final String AC_FIRE = "fire";

    protected int ammo = 8;
    protected int maxAmmo = 8;
    public CartridgeEffect car_effect = CartridgeEffect.Normal;
    public Cartridge cartridge;
    protected float reloadTime = 1f;
    public int tier=1;
    {
        image = SpriteRegistry.itemByName("gun");
        cartridge = new Cartridge(maxAmmo, car_effect);
        usesTargeting = true;
        defaultAction = AC_FIRE;
    }
    
    private Gun _this = this;

    @Override
    public String name() {
        return Messages.get(this, "name");
    }

    @Override
    public String status() {
        return ammo + "/" + maxAmmo;
    }

    @Override
    public String desc() {
        String cartridgeTypeStr = cartridge != null ? cartridge.cartridgeType.name() : Messages.get(Gun.class, "no_cartridge");
        String effectStr = (cartridge != null && cartridge.onHit != null && cartridge.onHit != CartridgeEffect.Normal)
                ? cartridge.onHit.name() : Messages.get(Gun.class, "no_effect");
        return Messages.get(this, "desc", min(0), max(0), cartridgeTypeStr, ammo, maxAmmo, effectStr);
    }

    protected void execute_ac_fire(Hero hero, String action) {
        if (ammo <= 0) {
            GLog.w(Messages.get(Gun.class, "no_ammo"));
            return;
        }
        GameScene.selectCell(shooter);
    }
    public void fire(int targetPos){
        fire(targetPos,true);
    }
    public void fire(int targetPos,boolean shouldSpend) {
        HitResult[] hitResults = fire_hits(curUser, targetPos, Ballistica.PROJECTILE);
        
        // 创建子弹轨迹粒子效果 - 使用更快的速度
        final int finalTargetPos = hitResults.length > 0 ? hitResults[0].where() : targetPos;
        MagicMissile missile = MagicMissile.boltFromChar(
            curUser.sprite.parent, 
            MagicMissile.LIGHT_MISSILE, 
            curUser.sprite, 
            finalTargetPos,
            new Callback() {
                @Override
                public void call() {
                    // 子弹命中后处理伤害和效果
                    processFireResults(hitResults, shouldSpend);
                }
            }
        );
        
        missile.setSpeed(800f);
        curUser.sprite.parent.add(missile);
    }
    
    protected void processFireResults(HitResult[] hitResults, boolean shouldSpend) {
        for (HitResult hitResult : hitResults) {
            Object target = hitResult.who();
            if (target instanceof Char targetChar) {
                boolean hit = Random.Float() < accuracyFactor(curUser, targetChar);
                if (hit) {
                    int baseDamage = fire_damage(curUser, targetChar);
                    int actualDamage = fire_proc(curUser, targetChar, baseDamage);
                    targetChar.damage(actualDamage, this);
                    Sample.INSTANCE.play(Assets.Sounds.HIT);
                }else{
                }
            }else{
            }
            // Apply the cartridge's effect
            if(cartridge.onHit!=null){
            cartridge.onHit.onHit.apply(curUser, hitResult.where(), (int) (cartridge.power * getAmmoPowerMultiplier()), 0);
            }
        }
        if(shouldSpend){
            curUser.spendAndNext(1f);
        }
    }

    protected int fire_proc(Char shooter, Char target, int damage) {
        // Subtract the target's defense from the damage
        int defense = target != null && target instanceof Char targetChar ? targetChar.drRoll() : 0;
        int actualDamage = Math.max(damage - defense, 0);
        if (actualDamage <= 0) {
            GLog.w(Messages.get(Gun.class, "scrape"));
        }
        return actualDamage;
    }

    protected CellSelector.Listener shooter = new CellSelector.Listener() {
        @Override
        public void onSelect(Integer target) {
            if (target != null) {
                consumeAmmo(1);
                if (cartridge instanceof CartridgeAltFire catf) {
                    catf.fire(curUser, target, catf, _this);
                } else {
                    fire(target);
                }
                updateQuickslot();
            }
        }

        @Override
        public String prompt() {
            return aiming_prompt();
        }
    };

    protected void execute_ac_reload(Hero hero, String action) {
        if (ammo >= maxAmmo) {
            GLog.w(Messages.get(Gun.class, "ammo_full"));
            return;
        }
        List<Ammo> availableAmmo = findAmmo();
        if (availableAmmo.isEmpty()) {
            GLog.w(Messages.get(Gun.class, "no_ammo_available"));
            return;
        }
        if (availableAmmo.size() == 1) {
            reload(availableAmmo.get(0));
        } else {
            showAmmoSelectionWindow(availableAmmo);
        }
        updateQuickslot();
    }

    protected void showAmmoSelectionWindow(List<Ammo> availableAmmo) {
        ArrayList<String> ammoNames = new ArrayList<>();
        for (Ammo _ammo : availableAmmo) {
            ammoNames.add(_ammo.name() + " (" + _ammo.quantity() + ")");
        }
        GameScene.show(new WndOptions(Messages.get(Gun.class, "select_ammo_title"),
                Messages.get(Gun.class, "select_ammo_prompt"),
                ammoNames.toArray(new String[0])) {
            @Override
            protected void onSelect(int index) {
                if (index >= 0 && index < availableAmmo.size()) {
                    reload(availableAmmo.get(index));
                }
                updateQuickslot();
            }
        });
    }

    protected List<Ammo> findAmmo() {
        return Dungeon.hero.belongings.getAllItems(Ammo.class)
                .stream()
                .filter(this::canLoad)
                .collect(Collectors.toList());
    }

    public boolean canLoad(Ammo ammo) {
        return true;
    }

    public void reload(Ammo ammoItem) {
        cartridge
                = ammoItem.cartridge instanceof Cartridge
                && ammoItem.cartridge.onHit != CartridgeEffect.Supply
                        ? ammoItem.cartridge
                        : new Cartridge(maxAmmo, car_effect);
        ammo = ammoItem.full_reload
                ? maxAmmo
                : ammoItem.amount;
        ammoItem.quantity(ammoItem.quantity() - 1);
        if (ammoItem.quantity() <= 0) {
            ammoItem.detach(Dungeon.hero.belongings.backpack);
        }

        curUser.spend(reloadTime);
        curUser.busy();
        curUser.sprite.attack(curUser.pos);
        Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
    }

    public String aiming_prompt() {
        return Messages.get(Gun.class, "prompt_aim");
    }

    @Override
    public float accuracyFactor(Char owner, Char target) {
        return 1f;
    }

    protected float getAmmoPowerMultiplier() {
        return 1.0f + 0.4f * level();
    }

    protected void consumeAmmo(int amount) {
        ammo = Math.max(0, ammo - amount);
        if (ammo <= 0) {
            GLog.w(Messages.get(Gun.class, "ammo_depleted"));
        }
    }

    private static final String AMMO = "ammo";
    private static final String MAX_AMMO = "maxAmmo";

    @Override
    public int min(int lvl) {
        return 2 + lvl;
    }

    @Override
    public int max(int lvl) {
        return 8 + 2 * lvl;
    }

    @Override
    public int STRReq(int lvl) {
        return 8 + lvl;
    }

    public int fire_damage(Char hero, Char target) {
        return (int) (getAmmoPowerMultiplier() * cartridge.power);
    }

    public HitResult[] fire_hits(Char shooter, int targetPos, int projectileType) {
        Ballistica shot = new Ballistica(shooter.pos, targetPos, projectileType);
        return new HitResult[]{new HitResult(shot.collisionPos, Actor.findChar(shot.collisionPos))};
    }

    public static record HitResult(int where, Char who) {

    }

    public static class ShotResult {

        public final boolean hit;           // 是否命中
        public final boolean blocked;       // 是否被阻挡
        public final int damage;            // 造成的伤害
        public final int distance;          // 射击距离
        public final Char target;           // 目标
        public final int collisionPos;      // 碰撞位置
        public final float hitChance;       // 命中率

        public ShotResult(boolean hit, boolean blocked, int damage, int distance,
                Char target, int collisionPos, float hitChance) {
            this.hit = hit;
            this.blocked = blocked;
            this.damage = damage;
            this.distance = distance;
            this.target = target;
            this.collisionPos = collisionPos;
            this.hitChance = hitChance;
        }
    }
    /** @deprecated Use {@link #fire(int)} / {@link #fire_hits} and {@link #processFireResults} instead. */
    @Deprecated
    public static ShotResult shoot(Gun gun, Char shooter, int targetPos, Cartridge cartridge, int projectileType) {
        Ballistica shot = new Ballistica(shooter.pos, targetPos, projectileType);

        // 检查是否被阻挡
        boolean blocked = shot.collisionPos != targetPos;
        int collisionPos = shot.collisionPos;
        Char target = Actor.findChar(collisionPos);

        boolean hit = !blocked
                && Random.Float() < gun.accuracyFactor(shooter, target);

        // 计算伤害
        int damage = gun.fire_damage(shooter, target);

        // 应用伤害
        //FIXME 应有能复杂的机制
        if (target != null) {
            target.damage(damage, gun);
        }

        if (shooter instanceof Hero shooterHero) {
            cartridge.onHit.onHit.apply(shooterHero, collisionPos, (int) (cartridge.power * gun.getAmmoPowerMultiplier()), damage);
        }
        shooter.sprite.attack(shooter.pos);
        Sample.INSTANCE.play(gun.hitSound, 1, gun.hitSoundPitch);
        return new ShotResult(hit, blocked, damage, shot.dist, target, collisionPos, gun.accuracyFactor(shooter, target));
    }

    @Override
    public void storeInBundle(Bundle bundle) {
        super.storeInBundle(bundle);
        bundle.put(AMMO, ammo);
        bundle.put(MAX_AMMO, maxAmmo);
        // if (cartridge != null && car) {
        //     bundle.put("cartridgeEffect", cartridge.onHit.name);
        //     bundle.put("cartridgePower", cartridge.power);
        //     // Add more fields if needed
        // }
    }

    @Override
    public void restoreFromBundle(Bundle bundle) {
        super.restoreFromBundle(bundle);
        ammo = bundle.getInt(AMMO);
        maxAmmo = bundle.getInt(MAX_AMMO);
        // Retrieve cartridgeType from cartridgeEffect
        // String cartridgeType = bundle.getString("cartridgeEffect");
        // int cartridgePower = bundle.getInt("cartridgePower");
        // cartridge = new Cartridge(cartridgePower, CartridgeEffect.valueOf(cartridgeType));
        // Initialize more fields if needed
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_FIRE);
        actions.add(AC_RELOAD);
        addSubActions(hero, actions);
        return actions;
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_FIRE)) {
            return Messages.get(Gun.class, "ac_fire");
        }
        if (action.equals(AC_RELOAD)) {
            return Messages.get(Gun.class, "ac_reload");
        }
        if (subActionName(action, hero) != null) {
            return subActionName(action, hero);
        }
        return super.actionName(action, hero);
    }

    @Override
    public void execute(Hero hero, String action) {
        super.execute(hero, action);
        switch (action) {
            case AC_FIRE ->
                execute_ac_fire(hero, action);
            case AC_RELOAD ->
                execute_ac_reload(hero, action);
            default ->
                executeSubAction(hero, action);
        }
    }

    public String subActionName(String action, Hero hero) {
        return null;
    }

    protected void addSubActions(Hero hero, ArrayList<String> actions) {
    }

    protected void executeSubAction(Hero hero, String action) {
    }

}
