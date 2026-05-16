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

package com.zootdungeon.windows;

import com.badlogic.gdx.Gdx;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.Chrome;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.StyledButton;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.utils.LuaScriptManager;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.ui.Component;
import org.luaj.vm2.LuaValue;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.List;

public class WndLuaScript extends Window {

	private static final int W_PORT      = 200;
	private static final int W_LAND      = 340;
	private static final int W_LAND_WIDE = 440;

	private static final int MARGIN    = 2;
	private static final int BTN_H     = 16;
	private static final int INPUT_H_P = 110;
	private static final int INPUT_H_L = 80;

	private TextInput textBox;
	private RenderedTextBlock resultText;
	private RedButton btnCopy;
	private RedButton btnPaste;
	private RedButton btnClear;
	private RedButton btnSave;
	private RedButton btnHelp;

	private String currentScriptName = null;

	public WndLuaScript() {
		super();

		boolean landscape = PixelScene.landscape();
		float aspect = PixelScene.uiCamera.width / PixelScene.uiCamera.height;
		int w = landscape ? (aspect > 1.2f ? W_LAND_WIDE : W_LAND) : W_PORT;

		if (landscape) offset(0, -45);
		else offset(0, -60);

		float pos = MARGIN;
		int colR = 46;
		int inputH = landscape ? INPUT_H_L : INPUT_H_P;
		float leftW = w - colR - 2 * MARGIN;

		// Title
		RenderedTextBlock title = PixelScene.renderTextBlock(msg("title"), 9);
		title.maxWidth(w);
		title.hardlight(TITLE_COLOR);
		title.setPos((w - title.width()) / 2f, pos);
		add(title);
		pos = title.bottom() + MARGIN;

		// Text input (left side)
		textBox = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), true, (int)(PixelScene.uiCamera.zoom * 6)) {
			@Override
			public void onChanged() {
				super.onChanged();
				boolean has = !getText().isEmpty();
				if (btnCopy != null) btnCopy.enable(has);
				if (btnClear != null) btnClear.enable(has);
				if (btnSave != null) btnSave.enable(has);
			}
			@Override
			public void onClipBoardUpdate() {
				super.onClipBoardUpdate();
				if (btnPaste != null) {
					try {
						btnPaste.enable(Gdx.app.getClipboard().hasContents());
					} catch (Exception e) {
						btnPaste.enable(false);
					}
				}
			}
		};
		textBox.setMaxLength(2048);
		textBox.setText("");
		add(textBox);
		textBox.setRect(MARGIN, pos, leftW, inputH);

		// Right column: icon buttons
		float rx = MARGIN + leftW + MARGIN;

		btnHelp = makeIconBtn(Icons.INFO, this::showHelp);
		add(btnHelp);
		btnHelp.setRect(rx, pos, BTN_H, BTN_H);

		btnCopy = makeIconBtn(Icons.COPY, () -> textBox.copyToClipboard());
		btnCopy.enable(false);
		add(btnCopy);
		btnCopy.setRect(rx, btnHelp.bottom() + MARGIN, BTN_H, BTN_H);

		btnPaste = makeIconBtn(Icons.PASTE, () -> {
			try {
				if (Gdx.app.getClipboard().hasContents()) textBox.pasteFromClipboard();
			} catch (Exception ignored) {}
		});
		try {
			btnPaste.enable(Gdx.app.getClipboard().hasContents());
		} catch (Exception e) {
			btnPaste.enable(false);
		}
		add(btnPaste);
		btnPaste.setRect(rx, btnCopy.bottom() + MARGIN, BTN_H, BTN_H);

		btnClear = makeIconBtn(Icons.CLOSE, () -> {
			textBox.setText("");
			if (resultText != null) {
				resultText.text("");
				resultText.visible = false;
			}
			currentScriptName = null;
		});
		btnClear.enable(false);
		add(btnClear);
		btnClear.setRect(rx, btnPaste.bottom() + MARGIN, BTN_H, BTN_H);

		btnSave = makeIconBtn(Icons.DISPLAY, this::saveScript);
		btnSave.enable(false);
		add(btnSave);
		btnSave.setRect(rx, btnClear.bottom() + MARGIN, BTN_H, BTN_H);

		pos += inputH + MARGIN;

		// Run / Close buttons
		RedButton runBtn = new RedButton(msg("run")) {
			@Override
			protected void onClick() {
				executeScript();
			}
		};
		runBtn.setRect(MARGIN, pos, (leftW - MARGIN) / 2, BTN_H);
		add(runBtn);

		StyledButton closeBtn = new StyledButton(Chrome.Type.GREY_BUTTON_TR, msg("close"), 9) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		closeBtn.setRect(runBtn.right() + MARGIN, pos, (leftW - MARGIN) / 2, BTN_H);
		add(closeBtn);
		pos += BTN_H + MARGIN;

		// Result display
		resultText = PixelScene.renderTextBlock("", 6);
		resultText.maxWidth(w - 2 * MARGIN);
		resultText.setPos(MARGIN, pos);
		resultText.visible = false;
		add(resultText);
		pos += 24;

		// Quick reference button
		RedButton refBtn = new RedButton(msg("ref_toggle"), 9) {
			@Override
			protected void onClick() {
				showQuickRef();
			}
		};
		refBtn.setRect(MARGIN, pos, w - 2 * MARGIN, BTN_H);
		add(refBtn);
		pos += BTN_H + MARGIN;

		// Saved scripts list
		List<ScriptEntry> saved = loadSavedScripts();
		if (!saved.isEmpty()) {
			RenderedTextBlock listTitle = PixelScene.renderTextBlock(msg("saved"), 6);
			listTitle.hardlight(0xAAAAAA);
			listTitle.setPos(MARGIN, pos);
			add(listTitle);
			pos = listTitle.bottom() + MARGIN;

			for (ScriptEntry e : saved) {
				ScriptRow row = new ScriptRow(e.name, e.code, pos, w - 2 * MARGIN);
				add(row);
				pos = row.bottom() + MARGIN;
			}
		}

		resize(w, (int)pos);
		textBox.setRect(MARGIN, textBox.top(), leftW, inputH);
		PointerEvent.clearKeyboardThisPress = false;
	}

	private RedButton makeIconBtn(Icons type, Runnable action) {
		RedButton btn = new RedButton("") {
			@Override
			protected void onPointerDown() {
				super.onPointerDown();
				PointerEvent.clearKeyboardThisPress = false;
			}
			@Override
			protected void onPointerUp() {
				super.onPointerUp();
				PointerEvent.clearKeyboardThisPress = false;
			}
			@Override
			protected void onClick() {
				super.onClick();
				action.run();
			}
		};
		btn.icon(type.get());
		return btn;
	}

	// ── Execute ──────────────────────────────────────────────────────────────
	private void executeScript() {
		String script = textBox.getText().trim();
		if (script.isEmpty()) {
			showResult(msg("err_empty"), 0xFF4444);
			return;
		}
		try {
			LuaScriptManager lua = LuaScriptManager.getInstance();
			LuaValue result = lua.loadString("tmp_" + System.currentTimeMillis(), script);
			if (result.isnil()) {
				showResult(msg("ok_no_result"), TITLE_COLOR);
			} else {
				showResult(msg("ok_result") + " " + formatLuaValue(result), TITLE_COLOR);
			}
		} catch (Exception e) {
			showResult(msg("err_exec") + ": " + e.getMessage(), 0xFF4444);
			GLog.w("[Lua] " + e.getMessage());
		}
	}

	private String formatLuaValue(LuaValue v) {
		if (v.isnil())     return "nil";
		if (v.isboolean()) return String.valueOf(v.toboolean());
		if (v.isint())    return String.valueOf(v.toint());
		if (v.isnumber())  return String.valueOf(v.todouble());
		if (v.isstring())  return "\"" + v.tojstring() + "\"";
		if (v.istable())   return "{ ... }";
		return v.toString();
	}

	private void showResult(String text, int color) {
		if (resultText != null) {
			resultText.text(text);
			resultText.hardlight(color);
			resultText.visible = true;
		}
	}

	// ── Save / Load ──────────────────────────────────────────────────────────
	private void saveScript() {
		String script = textBox.getText().trim();
		if (script.isEmpty()) {
			showResult(msg("err_empty"), 0xFF4444);
			return;
		}
		GameScene.show(new WndTextInput(
				msg("save_title"),
				msg("save_prompt"),
				currentScriptName != null ? currentScriptName : "script_" + System.currentTimeMillis(),
				50, false,
				msg("save_confirm"),
				msg("cancel")
		) {
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) {
					String name = text.trim();
					if (doSaveScript(name, script)) {
						currentScriptName = name;
						WndLuaScript.this.showResult(msg("saved_ok") + ": " + name, TITLE_COLOR);
					} else {
						WndLuaScript.this.showResult(msg("save_fail"), 0xFF4444);
					}
				}
			}
		});
	}

	private boolean doSaveScript(String name, String code) {
		try {
			Bundle global = com.zootdungeon.SaveManager.loadGlobal();
			if (global == null) global = new Bundle();

			Bundle scripts = global.getBundle("lua_scripts");
			if (scripts == null || scripts.isNull()) scripts = new Bundle();
			scripts.put(name, code);

			ArrayList<String> names = new ArrayList<>();
			if (global.contains("lua_script_names")) {
				for (String n : global.getStringArray("lua_script_names")) {
					if (!n.equals(name)) names.add(n);
				}
			}
			names.add(name);
			global.put("lua_script_names", names.toArray(new String[0]));
			global.put("lua_scripts", scripts);
			com.zootdungeon.SaveManager.saveGlobal(global);
			return true;
		} catch (Exception e) {
			GLog.w("[Lua] save failed: " + e.getMessage());
			return false;
		}
	}

	private void deleteScript(String name) {
		try {
			Bundle global = com.zootdungeon.SaveManager.loadGlobal();
			if (global == null) return;

			Bundle scripts = global.getBundle("lua_scripts");
			if (scripts != null && !scripts.isNull()) {
				scripts.remove(name);
				global.put("lua_scripts", scripts);
			}

			ArrayList<String> names = new ArrayList<>();
			if (global.contains("lua_script_names")) {
				for (String n : global.getStringArray("lua_script_names")) {
					if (!n.equals(name)) names.add(n);
				}
			}
			global.put("lua_script_names", names.toArray(new String[0]));
			com.zootdungeon.SaveManager.saveGlobal(global);

			if (currentScriptName != null && currentScriptName.equals(name)) {
				currentScriptName = null;
			}
			showResult(msg("deleted") + ": " + name, 0xAAAAAA);
		} catch (Exception e) {
			GLog.w("[Lua] delete failed: " + e.getMessage());
		}
	}

	private List<ScriptEntry> loadSavedScripts() {
		List<ScriptEntry> result = new ArrayList<>();
		try {
			Bundle global = com.zootdungeon.SaveManager.loadGlobal();
			if (global == null || !global.contains("lua_script_names")) return result;
			String[] names = global.getStringArray("lua_script_names");
			if (names == null) return result;
			Bundle scripts = global.getBundle("lua_scripts");
			for (String name : names) {
				if (scripts != null && scripts.contains(name)) {
					result.add(new ScriptEntry(name, scripts.getString(name)));
				}
			}
		} catch (Exception ignored) {}
		return result;
	}

	// ── Help windows ────────────────────────────────────────────────────────
	private void showHelp() {
		String body = msg("help_intro") + "\n\n"
				+ msg("help_globals") + "\n"
				+ "  log(msg)\n"
				+ "  random(min, max) / randomFloat()\n"
				+ "  Dungeon.depth / .gold / .hero / .level\n"
				+ "  Dungeon:drop(\"upgrade\", cell)\n"
				+ "  hero:HP() / :heal(n) / :teleport(cell)\n"
				+ "  hero:pos() / :level() / :damage(n)\n"
				+ "  level:isPassable(cell) / :isValid(cell)\n"
				+ "\n" + msg("help_java") + "\n"
				+ "  Java.getClass(\"ClassName\")\n"
				+ "  Java.callStatic(\"Class\", \"method\", args)\n"
				+ "  obj:call(\"method\", args...)\n"
				+ "\n" + msg("help_hint");
		GameScene.show(new WndOptions(
				Icons.INFO.get(),
				msg("help_title"),
				body,
				msg("got_it")
		));
	}

	private void showQuickRef() {
		GameScene.show(new WndOptions(
				Icons.INFO.get(),
				msg("ref_title"),
				msg("ref_body"),
				"OK"
		));
	}

	// ── Script row ────────────────────────────────────────────────────────
	private class ScriptRow extends Component {
		private final String name;
		private final String code;
		private final float rowW;
		private RenderedTextBlock label;
		private RedButton loadBtn;
		private RedButton delBtn;

		ScriptRow(String name, String code, float y, float w) {
			this.name = name;
			this.code = code;
			this.rowW = w;
			setRect(0, y, w, BTN_H + MARGIN);
		}

		@Override
		protected void createChildren() {
			label = PixelScene.renderTextBlock(name, 6);
			label.maxWidth((int)(rowW - BTN_H * 2 - MARGIN * 2));
			label.setPos(0, (height() - label.height()) / 2f);
			add(label);

			loadBtn = new RedButton("", 9) {
				@Override
				protected void onClick() {
					textBox.setText(code);
					currentScriptName = name;
					WndLuaScript.this.showResult(msg("loaded") + ": " + name, TITLE_COLOR);
				}
			};
			loadBtn.icon(Icons.ENTER.get());
			add(loadBtn);

			delBtn = new RedButton("", 9) {
				@Override
				protected void onClick() {
					deleteScript(name);
					GameScene.show(new WndLuaScript());
				}
			};
			delBtn.icon(Icons.CLOSE.get());
			add(delBtn);
		}

		@Override
		protected void layout() {
			label.setPos(0, (height() - label.height()) / 2f);
			loadBtn.setRect(rowW - BTN_H * 2 - MARGIN, 0, BTN_H, BTN_H);
			delBtn.setRect(rowW - BTN_H, 0, BTN_H, BTN_H);
		}
	}

	private static class ScriptEntry {
		final String name;
		final String code;
		ScriptEntry(String name, String code) { this.name = name; this.code = code; }
	}

	// ── Messages ──────────────────────────────────────────────────────────
	private String msg(String key) {
		return Messages.get("arknights.luascript." + key);
	}

	@Override
	public void onBackPressed() {
		// Prevent accidental loss of writing
	}

	@Override
	public void offset(int xOff, int yOff) {
		super.offset(xOff, yOff);
		if (textBox != null) {
			textBox.setRect(textBox.left(), textBox.top(), textBox.width(), textBox.height());
		}
	}
}
