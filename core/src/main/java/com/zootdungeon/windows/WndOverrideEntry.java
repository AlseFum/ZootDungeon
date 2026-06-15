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
 */

package com.zootdungeon.windows;

import com.zootdungeon.Assets;
import com.zootdungeon.Assets.ResourceType;
import com.zootdungeon.CDSettings;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.Chrome;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.Window;
import com.watabou.noosa.TextInput;

public class WndOverrideEntry extends Window {

	private static final int WIDTH = 140;
	private static final int MARGIN = 2;
	private static final int BUTTON_HEIGHT = 16;
	private static final int INPUT_HEIGHT = 16;

	private ResourceType selectedType = ResourceType.TEXTURE;
	private RedButton btnLang;
	private RedButton btnTexture;
	private RedButton btnSound;
	private RedButton btnScript;
	private TextInput inputId;
	private TextInput inputPath;
	private final Runnable onAdded;

	public WndOverrideEntry(Runnable onAdded) {
		super();
		this.onAdded = onAdded;

		int width = PixelScene.landscape() ? 200 : WIDTH;
		float pos = MARGIN;

		RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(this, "title"), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((width - title.width()) / 2, pos);
		add(title);
		pos = title.bottom() + MARGIN * 2;

		RenderedTextBlock lblType = PixelScene.renderTextBlock(Messages.get(this, "type"), 7);
		lblType.setPos(0, pos);
		add(lblType);
		pos = lblType.bottom() + 1;

		float btnW = (width - 3) / 4;
		btnLang = new RedButton("LANG", 7) {
			@Override protected void onClick() { selectType(ResourceType.LANG); }
		};
		btnLang.setRect(0, pos, btnW, BUTTON_HEIGHT);
		add(btnLang);
		btnTexture = new RedButton("TEX", 7) {
			@Override protected void onClick() { selectType(ResourceType.TEXTURE); }
		};
		btnTexture.setRect(btnLang.right() + 1, pos, btnW, BUTTON_HEIGHT);
		add(btnTexture);
		btnSound = new RedButton("SND", 7) {
			@Override protected void onClick() { selectType(ResourceType.SOUND); }
		};
		btnSound.setRect(btnTexture.right() + 1, pos, btnW, BUTTON_HEIGHT);
		add(btnSound);
		btnScript = new RedButton("SCR", 7) {
			@Override protected void onClick() { selectType(ResourceType.SCRIPT); }
		};
		btnScript.setRect(btnSound.right() + 1, pos, btnW, BUTTON_HEIGHT);
		add(btnScript);
		pos = btnScript.bottom() + MARGIN;

		RenderedTextBlock lblId = PixelScene.renderTextBlock(Messages.get(this, "resource_id"), 7);
		lblId.setPos(0, pos);
		add(lblId);
		pos = lblId.bottom() + 1;

		int textSize = (int) PixelScene.uiCamera.zoom * 9;
		inputId = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize);
		inputId.setMaxLength(128);
		inputId.setRect(0, pos, width - MARGIN * 2, INPUT_HEIGHT);
		add(inputId);
		pos = inputId.bottom() + MARGIN;

		RenderedTextBlock lblPath = PixelScene.renderTextBlock(Messages.get(this, "override_path"), 7);
		lblPath.setPos(0, pos);
		add(lblPath);
		pos = lblPath.bottom() + 1;

		inputPath = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, textSize);
		inputPath.setMaxLength(256);
		inputPath.setRect(0, pos, width - MARGIN * 2, INPUT_HEIGHT);
		add(inputPath);
		pos = inputPath.bottom() + MARGIN * 2;

		RedButton btnAdd = new RedButton(Messages.get(this, "add"), 8) {
			@Override
			protected void onClick() {
				String id = inputId.getText().trim();
				String path = inputPath.getText().trim();
				if (id.isEmpty() || path.isEmpty()) {
					parent.addToFront(new WndMessage(Messages.get(WndOverrideEntry.this, "empty")));
					return;
				}
				boolean hadLang = !Assets.manualOverrideIndex.isEmpty(ResourceType.LANG);
				Assets.manualOverrideIndex.put(selectedType, id, path);
				Assets.saveManualOverrides();
				if (selectedType == ResourceType.LANG || hadLang) {
					Messages.setup(CDSettings.language());
				}
				if (onAdded != null) onAdded.run();
				hide();
			}
		};
		btnAdd.setRect(0, pos, (width - MARGIN) / 2, BUTTON_HEIGHT);
		add(btnAdd);

		RedButton btnCancel = new RedButton(Messages.get(this, "cancel"), 8) {
			@Override protected void onClick() { hide(); }
		};
		btnCancel.setRect(btnAdd.right() + MARGIN, pos, (width - MARGIN) / 2, BUTTON_HEIGHT);
		add(btnCancel);

		pos = btnCancel.bottom() + MARGIN;

		resize(width, (int) pos);
		updateTypeButtons();
	}

	private void selectType(ResourceType type) {
		selectedType = type;
		updateTypeButtons();
	}

	private void updateTypeButtons() {
		btnLang.textColor(selectedType == ResourceType.LANG ? Window.TITLE_COLOR : 0xFFFFFF);
		btnTexture.textColor(selectedType == ResourceType.TEXTURE ? Window.TITLE_COLOR : 0xFFFFFF);
		btnSound.textColor(selectedType == ResourceType.SOUND ? Window.TITLE_COLOR : 0xFFFFFF);
		btnScript.textColor(selectedType == ResourceType.SCRIPT ? Window.TITLE_COLOR : 0xFFFFFF);
	}
}
