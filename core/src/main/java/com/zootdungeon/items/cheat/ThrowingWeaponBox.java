package com.zootdungeon.items.cheat;

import com.zootdungeon.Assets;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.missiles.Bolas;
import com.zootdungeon.items.weapon.missiles.FishingSpear;
import com.zootdungeon.items.weapon.missiles.ForceCube;
import com.zootdungeon.items.weapon.missiles.HeavyBoomerang;
import com.zootdungeon.items.weapon.missiles.Javelin;
import com.zootdungeon.items.weapon.missiles.Kunai;
import com.zootdungeon.items.weapon.missiles.MissileWeapon;
import com.zootdungeon.items.weapon.missiles.Shuriken;
import com.zootdungeon.items.weapon.missiles.ThrowingClub;
import com.zootdungeon.items.weapon.missiles.ThrowingHammer;
import com.zootdungeon.items.weapon.missiles.ThrowingKnife;
import com.zootdungeon.items.weapon.missiles.ThrowingSpear;
import com.zootdungeon.items.weapon.missiles.ThrowingSpike;
import com.zootdungeon.items.weapon.missiles.ThrowingStone;
import com.zootdungeon.items.weapon.missiles.Tomahawk;
import com.zootdungeon.items.weapon.missiles.Trident;
import com.zootdungeon.arknights.necrass.SummoningThrowingWeapon;
import com.zootdungeon.items.weapon.missiles.darts.AdrenalineDart;
import com.zootdungeon.items.weapon.missiles.darts.BlindingDart;
import com.zootdungeon.items.weapon.missiles.darts.ChillingDart;
import com.zootdungeon.items.weapon.missiles.darts.CleansingDart;
import com.zootdungeon.items.weapon.missiles.darts.Dart;
import com.zootdungeon.items.weapon.missiles.darts.DisplacingDart;
import com.zootdungeon.items.weapon.missiles.darts.HealingDart;
import com.zootdungeon.items.weapon.missiles.darts.HolyDart;
import com.zootdungeon.items.weapon.missiles.darts.IncendiaryDart;
import com.zootdungeon.items.weapon.missiles.darts.ParalyticDart;
import com.zootdungeon.items.weapon.missiles.darts.PoisonDart;
import com.zootdungeon.items.weapon.missiles.darts.RotDart;
import com.zootdungeon.items.weapon.missiles.darts.ShockingDart;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndTabbedIconGrid;
import com.watabou.noosa.Image;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

public class ThrowingWeaponBox extends Item {

    private Class<? extends MissileWeapon> selectedWeapon;
    private MissileWeapon curWeapon; // 内置的武器实例
    
    public static final String AC_SELECT = "SELECT";
    public static final String AC_GENERATE = "GENERATE";
    public static final String AC_THROW = Item.AC_THROW;

    {
        image = ItemSpriteSheet.MISSILE_HOLDER;
        defaultAction = AC_THROW;
        usesTargeting = true;
    }

    @Override
    public void execute(Hero hero, String action) {
        if (action.equals(AC_SELECT)) {
            hero.sprite.operate(hero.pos);

            // 使用WndTabbedIconGrid构建器创建分标签页窗口
            WndTabbedIconGrid.Builder builder = new WndTabbedIconGrid.Builder()
                    .setTitle("选择投掷武器")
                    .setColumns(4);
            
            // 添加标准投掷武器标签页
            builder.addTab("标准投掷武器", Icons.get(Icons.WAND_HOLSTER));
            
            // 添加标准投掷武器到第一个标签页（索引0）
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingStone.class),
                    getWeaponDescription(ThrowingStone.class),
                    () -> selectedWeapon = ThrowingStone.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingKnife.class),
                    getWeaponDescription(ThrowingKnife.class),
                    () -> selectedWeapon = ThrowingKnife.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingSpike.class),
                    getWeaponDescription(ThrowingSpike.class),
                    () -> selectedWeapon = ThrowingSpike.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Shuriken.class),
                    getWeaponDescription(Shuriken.class),
                    () -> selectedWeapon = Shuriken.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingClub.class),
                    getWeaponDescription(ThrowingClub.class),
                    () -> selectedWeapon = ThrowingClub.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(FishingSpear.class),
                    getWeaponDescription(FishingSpear.class),
                    () -> selectedWeapon = FishingSpear.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingSpear.class),
                    getWeaponDescription(ThrowingSpear.class),
                    () -> selectedWeapon = ThrowingSpear.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Bolas.class),
                    getWeaponDescription(Bolas.class),
                    () -> selectedWeapon = Bolas.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Kunai.class),
                    getWeaponDescription(Kunai.class),
                    () -> selectedWeapon = Kunai.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Javelin.class),
                    getWeaponDescription(Javelin.class),
                    () -> selectedWeapon = Javelin.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Tomahawk.class),
                    getWeaponDescription(Tomahawk.class),
                    () -> selectedWeapon = Tomahawk.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(HeavyBoomerang.class),
                    getWeaponDescription(HeavyBoomerang.class),
                    () -> selectedWeapon = HeavyBoomerang.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(Trident.class),
                    getWeaponDescription(Trident.class),
                    () -> selectedWeapon = Trident.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ThrowingHammer.class),
                    getWeaponDescription(ThrowingHammer.class),
                    () -> selectedWeapon = ThrowingHammer.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(ForceCube.class),
                    getWeaponDescription(ForceCube.class),
                    () -> selectedWeapon = ForceCube.class
            );
            builder.addItemToTab(0,
                    createMissileIcon(SummoningThrowingWeapon.class),
                    getWeaponDescription(SummoningThrowingWeapon.class),
                    () -> selectedWeapon = SummoningThrowingWeapon.class
            );
            
            // 添加飞镖标签页
            builder.addTab("飞镖", Icons.get(Icons.WAND_HOLSTER));
            
            // 添加飞镖到第二个标签页（索引1）
            builder.addItemToTab(1,
                    createMissileIcon(Dart.class),
                    getWeaponDescription(Dart.class),
                    () -> selectedWeapon = Dart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(PoisonDart.class),
                    getWeaponDescription(PoisonDart.class),
                    () -> selectedWeapon = PoisonDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(CleansingDart.class),
                    getWeaponDescription(CleansingDart.class),
                    () -> selectedWeapon = CleansingDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(ParalyticDart.class),
                    getWeaponDescription(ParalyticDart.class),
                    () -> selectedWeapon = ParalyticDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(AdrenalineDart.class),
                    getWeaponDescription(AdrenalineDart.class),
                    () -> selectedWeapon = AdrenalineDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(HealingDart.class),
                    getWeaponDescription(HealingDart.class),
                    () -> selectedWeapon = HealingDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(ChillingDart.class),
                    getWeaponDescription(ChillingDart.class),
                    () -> selectedWeapon = ChillingDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(ShockingDart.class),
                    getWeaponDescription(ShockingDart.class),
                    () -> selectedWeapon = ShockingDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(IncendiaryDart.class),
                    getWeaponDescription(IncendiaryDart.class),
                    () -> selectedWeapon = IncendiaryDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(RotDart.class),
                    getWeaponDescription(RotDart.class),
                    () -> selectedWeapon = RotDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(HolyDart.class),
                    getWeaponDescription(HolyDart.class),
                    () -> selectedWeapon = HolyDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(DisplacingDart.class),
                    getWeaponDescription(DisplacingDart.class),
                    () -> selectedWeapon = DisplacingDart.class
            );
            builder.addItemToTab(1,
                    createMissileIcon(BlindingDart.class),
                    getWeaponDescription(BlindingDart.class),
                    () -> selectedWeapon = BlindingDart.class
            );

            // 显示窗口
            GameScene.show(builder.build());
        } else if (action.equals(AC_GENERATE)) {
            if (selectedWeapon == null) {
                GLog.w("请先选择要生成的投掷武器！");
            } else {
                MissileWeapon weapon = Reflection.newInstance(selectedWeapon);
                weapon.identify();
                if (weapon.collect(hero.belongings.backpack)) {
                    GLog.p("生成了 " + weapon.name() + "！");
                } else {
                    GLog.w("背包已满，无法生成物品！");
                }
            }
        } else if (action.equals(AC_THROW)) {
            if (selectedWeapon == null) {
                GLog.w("请先选择要投掷的武器！");
            } else {
                // 调用父类的 execute 方法处理 AC_THROW，它会调用 doThrow，然后调用 cast
                super.execute(hero, AC_THROW);
            }
        }
    }

    @Override
    public ArrayList<String> actions(Hero hero) {
        ArrayList<String> actions = super.actions(hero);
        actions.add(AC_SELECT);
        actions.add(AC_GENERATE);
        return actions;
    }

    @Override
    public void cast(final Hero user, final int dst) {
        if (selectedWeapon == null) {
            // 如果没有选择武器，使用默认的扔出行为
            super.cast(user, dst);
            return;
        }
        
        // 如果内置武器不存在或类型不匹配，创建新的武器
        if (curWeapon == null || curWeapon.getClass() != selectedWeapon) {
            curWeapon = Reflection.newInstance(selectedWeapon);
            curWeapon.identify();
        }
        
        // 将武器添加到背包，然后调用它的 cast 方法
        // 注意：每次投掷都会创建一个新的实例添加到背包，这样武器会被正常 detach 和投掷
        // 而内置的 curWeapon 保持不变，ThrowingWeaponBox 不会被移除
        MissileWeapon weaponToThrow = Reflection.newInstance(selectedWeapon);
        weaponToThrow.identify();
        
        if (weaponToThrow.collect(user.belongings.backpack)) {
            // 临时保存当前物品
            final Item originalItem = curItem;
            curItem = weaponToThrow;
            
            try {
                // 调用武器的 cast 方法
                weaponToThrow.cast(user, dst);
            } finally {
                // 恢复 curItem
                curItem = originalItem;
            }
        } else {
            // 背包已满，使用默认行为
            super.cast(user, dst);
        }
    }

    @Override
    public String actionName(String action, Hero hero) {
        if (action.equals(AC_SELECT)) {
            return "选择武器";
        } else if (action.equals(AC_GENERATE)) {
            return "生成武器";
        } else if (action.equals(AC_THROW)) {
            return "投掷武器";
        }
        return super.actionName(action, hero);
    }

    @Override
    public String name() {
        return "投掷武器箱";
    }

    @Override
    public String desc() {
        return "这是一个可以让你选择生成各种投掷武器的物品。";
    }

    @Override
    public boolean isUpgradable() {
        return false;
    }

    @Override
    public boolean isIdentified() {
        return true;
    }

    @Override
    public int value() {
        return 1000;
    }
    
    // 创建投掷武器图标的辅助方法
    private static Image createMissileIcon(Class<? extends MissileWeapon> weaponClass) {
        MissileWeapon weapon = Reflection.newInstance(weaponClass);
        return new ItemSprite(weapon.image);
    }

    // 获取武器描述的辅助方法
    private static String getWeaponDescription(Class<? extends MissileWeapon> weaponClass) {
        String name = Messages.get(weaponClass, "name");
        String desc = Messages.get(weaponClass, "desc");
        
        // 检查是否找到了有效的文本
        if (name == null || name.equals(Messages.NO_TEXT_FOUND)) {
            name = weaponClass.getSimpleName();
        }
        if (desc == null || desc.equals(Messages.NO_TEXT_FOUND)) {
            desc = "";
        }
        
        // 如果描述太长，可以截取第一句话
        if (!desc.isEmpty() && desc.length() > 100) {
            int firstPeriod = desc.indexOf('。');
            int firstPeriodEn = desc.indexOf('.');
            int cutPoint = firstPeriod > 0 ? firstPeriod : (firstPeriodEn > 0 ? firstPeriodEn : Math.min(100, desc.length()));
            desc = desc.substring(0, cutPoint);
        }
        
        return desc.isEmpty() ? name : (name + "：" + desc);
    }

}

