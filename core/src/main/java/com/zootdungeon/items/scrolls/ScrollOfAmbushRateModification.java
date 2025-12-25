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
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.zootdungeon.items.scrolls;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Belongings;
import com.zootdungeon.items.Item;
import com.zootdungeon.items.weapon.ambushWeapon.AmbushWeapon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.sprites.ItemSpriteSheet;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.windows.WndTextInput;

public class ScrollOfAmbushRateModification extends InventoryScroll {

    {
        icon = ItemSpriteSheet.Icons.SCROLL_UPGRADE; // 使用升级卷轴的图标，或者可以自定义
        preferredBag = Belongings.Backpack.class;

        unique = true;
        anonymous = true; // 始终已识别，不需要鉴定
    }
    
    @Override
    public void doRead() {
        // 不消耗卷轴，直接显示选择界面
        identifiedByUse = false;
        GameScene.selectItem(itemSelector);
    }

    @Override
    protected boolean usableOnItem(Item item) {
        return item instanceof AmbushWeapon;
    }

    @Override
    protected void onItemSelected(Item item) {
        if (item instanceof AmbushWeapon) {
            AmbushWeapon weapon = (AmbushWeapon) item;
            // 如果卷轴被itemSelector消耗了，需要恢复它
            // itemSelector会在第115行消耗非ScrollOfUpgrade的卷轴
            if (!curUser.belongings.backpack.items.contains(this)) {
                // 卷轴被消耗了，需要恢复
                this.collect(curUser.belongings.backpack);
            }
            showRateInputDialog(weapon);
        }
    }

    private void showRateInputDialog(AmbushWeapon weapon) {
        String currentRate = String.format("%.2f", weapon.ambushRate);
        
        GameScene.show(new WndTextInput(
                Messages.get(this, "input_rate_title"),
                Messages.get(this, "input_rate_message", weapon.name(), currentRate),
                currentRate,
                10,
                false,
                Messages.get(this, "confirm"),
                Messages.get(this, "cancel")) {
            @Override
            public void onSelect(boolean positive, String text) {
                if (positive) {
                    try {
                        float newRate = Float.parseFloat(text.trim());
                        // 允许任何数值，包括负数
                        weapon.ambushRate = newRate;
                        GLog.p(Messages.get(ScrollOfAmbushRateModification.this, "rate_modified", weapon.name(), String.format("%.2f", newRate)));
                        
                        // 不消耗卷轴，只播放动画
                        readAnimation();
                        com.watabou.noosa.audio.Sample.INSTANCE.play(com.zootdungeon.Assets.Sounds.READ);
                    } catch (NumberFormatException e) {
                        GLog.w(Messages.get(ScrollOfAmbushRateModification.this, "invalid_number"));
                    }
                }
            }
        });
    }

    @Override
    public int value() {
        return isKnown() ? 30 * quantity : super.value();
    }

    @Override
    public int energyVal() {
        return isKnown() ? 6 * quantity : super.energyVal();
    }
}

