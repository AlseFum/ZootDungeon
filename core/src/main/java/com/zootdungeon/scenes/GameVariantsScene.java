package com.zootdungeon.scenes;

import com.zootdungeon.Badges;
import com.zootdungeon.CDSettings;
import com.zootdungeon.ColaDungeon;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.Chrome;
import com.zootdungeon.ui.CheckBox;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.StyledButton;
import com.zootdungeon.ui.Window;
import com.zootdungeon.windows.WndChallenges;
import com.zootdungeon.windows.WndTitledMessage;
import com.watabou.noosa.Camera;
import com.watabou.noosa.ColorBlock;
import com.watabou.utils.DeviceCompat;

public class GameVariantsScene extends PixelScene {

	@Override
	public void create() {
		super.create();

		int w = Camera.main.width;
		int h = Camera.main.height;

		ColorBlock bg = new ColorBlock(w, h, 0xFF2d2f31);
		add(bg);

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 12);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos((w - title.width()) / 2f, 8);
		PixelScene.align(title);
		add(title);

		float top = title.bottom() + 12;

		StyledButton challengeBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR,
				Messages.get(WndChallenges.class, "title"), 8) {
			@Override
			protected void onClick() {
				super.onClick();
				if (!Badges.isUnlocked(Badges.Badge.VICTORY) && !DeviceCompat.isDebug()) {
					ColaDungeon.scene().addToFront(new WndTitledMessage(
							Icons.get(Icons.CHALLENGE_GREY),
							Messages.get(WndChallenges.class, "title"),
							Messages.get(HeroSelectScene.class, "challenges_nowin")));
					return;
				}
				ColaDungeon.scene().addToFront(new WndChallenges(CDSettings.challenges(), true));
			}
		};
		challengeBtn.icon(Icons.get(CDSettings.challenges() > 0 ? Icons.CHALLENGE_COLOR : Icons.CHALLENGE_GREY));
		challengeBtn.setRect((w - 120) / 2f, top, 120, 21);
		PixelScene.align(challengeBtn);
		add(challengeBtn);
		top = challengeBtn.bottom() + 6;

		CheckBox trampleChainCb = new CheckBox(Messages.get(this, "rule_trample_chain_grass")) {
			@Override
			protected void onClick() {
				super.onClick();
				CDSettings.trampleChainGrass(checked());
			}
		};
		trampleChainCb.checked(CDSettings.trampleChainGrass());
		trampleChainCb.setRect(16, top, w - 32, 18);
		PixelScene.align(trampleChainCb);
		add(trampleChainCb);
		top = trampleChainCb.bottom() + 8;

		RenderedTextBlock hint = PixelScene.renderTextBlock(Messages.get(this, "hint"), 6);
		hint.maxWidth((int) (w - 24));
		hint.setPos((w - hint.width()) / 2f, top);
		PixelScene.align(hint);
		add(hint);

		StyledButton backBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "back"), 8) {
			@Override
			protected void onClick() {
				super.onClick();
				ColaDungeon.switchNoFade(HeroSelectScene.class);
			}
		};
		backBtn.icon(Icons.get(Icons.LEFTARROW));
		backBtn.setRect((w - 80) / 2f, h - 28, 80, 21);
		PixelScene.align(backBtn);
		add(backBtn);
	}

	@Override
	protected void onBackPressed() {
		ColaDungeon.switchNoFade(HeroSelectScene.class);
	}
}
