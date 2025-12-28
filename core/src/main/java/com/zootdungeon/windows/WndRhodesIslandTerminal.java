/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
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

package com.zootdungeon.windows;

import com.zootdungeon.Dungeon;
import com.zootdungeon.arknights.skills.Skill;
import com.zootdungeon.arknights.skills.SkillSheet;
import com.zootdungeon.items.artifacts.RhodesIslandTerminal;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;

public class WndRhodesIslandTerminal extends Window {

	private static final int WIDTH		= 120;
	private static final int BTN_HEIGHT	= 20;
	private static final int GAP		= 2;
	
	private int pos;
	
	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		
		super();

		IconTitle title = new IconTitle(new ItemSprite(terminal), Messages.titleCase(terminal.name()));
		title.setRect(0, 0, WIDTH, 0);
		add(title);
		pos = (int)title.bottom() + GAP;

		// 显示所有注册的技能
		for (Skill skill : SkillSheet.values()) {
			final Skill finalSkill = skill;
			String buttonText = skill.id;
			if (skill.cost > 0) {
				buttonText += " (消耗 " + skill.cost + " cost)";
			}
			
			RedButton btn = new RedButton(buttonText) {
				@Override
				protected void onClick() {
					// 检查cost是否足够
					if (Dungeon.cost < finalSkill.cost) {
						GLog.w("cost不足，需要 " + finalSkill.cost + " cost");
						return;
					}
					
					// 消耗cost
					Dungeon.cost -= finalSkill.cost;
					
					// 执行技能
					finalSkill.execute(Dungeon.hero);
					
					hide();
				}
			};
			
			// 如果cost不足，禁用按钮
			if (Dungeon.cost < skill.cost) {
				btn.enable(false);
			}
			
			addButton(btn);
		}

		resize(WIDTH, pos);
	}
	
	private void addButton(RedButton btn) {
		add(btn);
		btn.setRect(0, pos > 0 ? pos += GAP : 0, WIDTH, BTN_HEIGHT);
		pos += BTN_HEIGHT;
	}
}

