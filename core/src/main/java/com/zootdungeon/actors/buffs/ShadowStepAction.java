package com.zootdungeon.actors.buffs;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.blobs.MiseryShadowBlob;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.CellSelector;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.ui.ActionIndicator;
import com.zootdungeon.utils.GLog;

/** MISERY shadow step: ActionIndicator button singleton for teleporting to shadow cells. */
public class ShadowStepAction implements ActionIndicator.Action {

	public static final ShadowStepAction INSTANCE = new ShadowStepAction();

	@Override
	public String actionName() {
		return Messages.get(this, "name");
	}

	@Override
	public int indicatorColor() {
		return 0x8844AA;
	}

	@Override
	public void doAction() {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				if (!MiseryShadowBlob.isShadowCell(cell)) {
					GLog.w(Messages.get(ShadowStepAction.class, "not_shadow"));
					return;
				}
				MiseryShadowBlob blob = (MiseryShadowBlob) Dungeon.level.blobs.get(MiseryShadowBlob.class);
				if (blob != null && Dungeon.hero != null) {
					blob.teleportTo(Dungeon.hero, cell);
				}
			}

			@Override
			public String prompt() {
				return Messages.get(ShadowStepAction.class, "prompt");
			}
		});
	}
}
