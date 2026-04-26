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
import com.zootdungeon.ui.Chrome;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;
import com.zootdungeon.utils.LuaScriptManager;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.TextInput;
import org.luaj.vm2.LuaValue;
import com.zootdungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import java.util.ArrayList;
import java.util.List;

public class WndLuaScript extends Window {

	private static final int WIDTH = 200;
	private static final int W_LAND = 360;
	private static final int W_LAND_WIDE = 480;
	private static final int MARGIN = 1;
	private static final int BUTTON_HEIGHT = 16;
	private static final int INPUT_HEIGHT_PORT = 100;
	private static final int INPUT_HEIGHT_LAND = 80;
	private static final int INPUT_HEIGHT_WIDE = 60;

	protected TextInput textBox;
	protected RenderedTextBlock resultText;

	protected RedButton btnCopy;
	protected RedButton btnPaste;
	protected RedButton btnClear;
	protected RedButton btnSave;
	protected RedButton btnLoad;

	public WndLuaScript() {
		super();

		final boolean landscape = PixelScene.landscape();
		final int width;
		if (landscape) {
			float aspectRatio = PixelScene.uiCamera.width / PixelScene.uiCamera.height;
			width = aspectRatio > 1.2f ? W_LAND_WIDE : W_LAND;
		} else {
			width = WIDTH;
		}

		//need to offset to give space for the soft keyboard
		if (landscape) {
			offset(0, -45);
		} else {
			offset(0, -60);
		}

		float pos = 2;

		// 标题
		final RenderedTextBlock txtTitle = PixelScene.renderTextBlock("Lua脚本执行器", 9);
		txtTitle.maxWidth(width);
		txtTitle.hardlight(Window.TITLE_COLOR);
		txtTitle.setPos((width - txtTitle.width()) / 2, 2);
		add(txtTitle);

		pos = txtTitle.bottom() + 4 * MARGIN;

		// 说明文字
		final RenderedTextBlock txtBody = PixelScene.renderTextBlock("输入Lua脚本代码，点击执行按钮运行", 6);
		txtBody.maxWidth(width);
		txtBody.setPos(0, pos);
		add(txtBody);

		pos = txtBody.bottom() + 2 * MARGIN;

		// 文本输入框（多行）
		int textSize = (int)PixelScene.uiCamera.zoom * 6;
		textBox = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), true, textSize){
			@Override
			public void onChanged() {
				super.onChanged();
				if (btnCopy != null) btnCopy.enable(!getText().isEmpty());
				if (btnClear != null) btnClear.enable(!getText().isEmpty());
				if (btnSave != null) btnSave.enable(!getText().isEmpty());
			}

			@Override
			public void onClipBoardUpdate() {
				super.onClipBoardUpdate();
				if (btnPaste != null) {
					try {
						btnPaste.enable(Gdx.app.getClipboard().hasContents());
					} catch (Exception e) {
						// 剪贴板访问失败时禁用粘贴按钮
						btnPaste.enable(false);
					}
				}
			}
		};
		textBox.setMaxLength(2048); // 允许较长的脚本

		final int inputHeight = landscape ? (width >= W_LAND_WIDE ? INPUT_HEIGHT_WIDE : INPUT_HEIGHT_LAND) : INPUT_HEIGHT_PORT;
		float textBoxWidth = width - 3 * MARGIN - BUTTON_HEIGHT;

		add(textBox);
		textBox.setRect(MARGIN, pos, textBoxWidth, inputHeight);

		// 复制按钮
		btnCopy = new RedButton(""){
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
				textBox.copyToClipboard();
			}
		};
		btnCopy.icon(Icons.COPY.get());
		btnCopy.enable(false);
		add(btnCopy);

		// 粘贴按钮
		btnPaste = new RedButton(""){
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
				try {
					if (Gdx.app.getClipboard().hasContents()) {
						textBox.pasteFromClipboard();
					} else {
						enable(false);
					}
				} catch (Exception e) {
					// 剪贴板访问失败
					enable(false);
				}
			}
		};
		btnPaste.icon(Icons.PASTE.get());
		try {
			btnPaste.enable(Gdx.app.getClipboard().hasContents());
		} catch (Exception e) {
			// 剪贴板访问失败时禁用粘贴按钮
			btnPaste.enable(false);
		}
		add(btnPaste);

		// 清空按钮
		btnClear = new RedButton(""){
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
				textBox.setText("");
				if (resultText != null) {
					resultText.text("");
					resultText.visible = false;
				}
			}
		};
		btnClear.icon(Icons.CLOSE.get());
		btnClear.enable(false);
		add(btnClear);

		btnCopy.setRect(textBoxWidth + 2*MARGIN, pos, BUTTON_HEIGHT, BUTTON_HEIGHT);
		btnPaste.setRect(textBoxWidth + 2*MARGIN, btnCopy.bottom() + MARGIN, BUTTON_HEIGHT, BUTTON_HEIGHT);
		btnClear.setRect(textBoxWidth + 2*MARGIN, btnPaste.bottom() + MARGIN, BUTTON_HEIGHT, BUTTON_HEIGHT);
		
		// 保存按钮
		btnSave = new RedButton(""){
			@Override
			protected void onClick() {
				saveScript();
			}
		};
		btnSave.icon(Icons.get(Icons.DISPLAY));
		btnSave.enable(false);
		add(btnSave);
		
		// 加载按钮
		btnLoad = new RedButton(""){
			@Override
			protected void onClick() {
				showScriptList();
			}
		};
		btnLoad.icon(Icons.get(Icons.INFO));
		add(btnLoad);
		
		btnSave.setRect(textBoxWidth + 2*MARGIN, btnClear.bottom() + MARGIN, BUTTON_HEIGHT, BUTTON_HEIGHT);
		btnLoad.setRect(textBoxWidth + 2*MARGIN, btnSave.bottom() + MARGIN, BUTTON_HEIGHT, BUTTON_HEIGHT);

		pos += inputHeight + MARGIN;

		// 执行按钮
		final RedButton executeBtn = new RedButton("执行") {
			@Override
			protected void onClick() {
				executeScript();
			}
		};
		executeBtn.icon(Icons.get(Icons.ENTER));
		executeBtn.setRect(MARGIN, pos, (textBoxWidth - MARGIN) / 2, BUTTON_HEIGHT);
		add(executeBtn);

		// 关闭按钮
		final RedButton closeBtn = new RedButton("关闭") {
			@Override
			protected void onClick() {
				hide();
			}
		};
		closeBtn.setRect(executeBtn.right() + MARGIN, pos, (textBoxWidth - MARGIN) / 2, BUTTON_HEIGHT);
		add(closeBtn);

		pos += BUTTON_HEIGHT + MARGIN;

		// 结果显示区域
		resultText = PixelScene.renderTextBlock("", 6);
		resultText.maxWidth(width - 2 * MARGIN);
		resultText.setPos(MARGIN, pos);
		resultText.visible = false;
		add(resultText);

		pos += 20; // 为结果预留空间

		//need to resize first before laying out the text box, as it depends on the window's camera
		resize(width, (int) pos);

		textBox.setRect(MARGIN, textBox.top(), textBoxWidth, inputHeight);

		PointerEvent.clearKeyboardThisPress = false;
	}

	private void executeScript() {
		String script = textBox.getText().trim();
		if (script.isEmpty()) {
			showResult("错误：脚本为空", 0xFF4444); // 红色表示错误
			return;
		}

		try {
			LuaScriptManager lua = LuaScriptManager.getInstance();
			LuaValue result = lua.loadString("temp_script_" + System.currentTimeMillis(), script);
			
			if (result.isnil()) {
				showResult("执行完成（无返回值）", Window.TITLE_COLOR);
			} else {
				String resultStr = formatLuaValue(result);
				showResult("结果: " + resultStr, Window.TITLE_COLOR);
			}
		} catch (Exception e) {
			showResult("错误: " + e.getMessage(), 0xFF4444); // 红色表示错误
			GLog.w("Lua脚本执行错误: " + e.getMessage());
		}
	}

	private String formatLuaValue(LuaValue value) {
		if (value.isnil()) {
			return "nil";
		} else if (value.isboolean()) {
			return String.valueOf(value.toboolean());
		} else if (value.isint()) {
			return String.valueOf(value.toint());
		} else if (value.isnumber()) {
			return String.valueOf(value.todouble());
		} else if (value.isstring()) {
			return "\"" + value.tojstring() + "\"";
		} else {
			return value.toString();
		}
	}

	private void showResult(String text, int color) {
		if (resultText != null) {
			resultText.text(text);
			resultText.hardlight(color);
			resultText.visible = true;
		}
	}

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (textBox != null){
			textBox.setRect(textBox.left(), textBox.top(), textBox.width(), textBox.height());
		}
	}

	@Override
	public void onBackPressed() {
		//Do nothing, prevents accidentally losing writing
	}
	
	private void saveScript() {
		String script = textBox.getText().trim();
		if (script.isEmpty()) {
			showResult("错误：脚本为空，无法保存", 0xFF4444);
			return;
		}
		
		// 显示输入脚本名称的窗口
		GameScene.show(new WndTextInput("保存脚本", "输入脚本名称：", "script_" + System.currentTimeMillis(), 50, false, "保存", "取消") {
			@Override
			public void onSelect(boolean positive, String text) {
				if (positive && text != null && !text.trim().isEmpty()) {
					String scriptName = text.trim();
					if (saveScriptToFile(scriptName, script)) {
						showResult("脚本已保存: " + scriptName, Window.TITLE_COLOR);
					} else {
						showResult("保存失败", 0xFF4444);
					}
				}
			}
		});
	}
	
	private boolean saveScriptToFile(String name, String content) {
		try {
			// 从全局数据中获取脚本
			Bundle globalData = com.zootdungeon.SaveManager.loadGlobal();
			if (globalData == null) {
				globalData = new Bundle();
			}
			
			// 获取或创建脚本Bundle
			Bundle scriptsBundle = globalData.getBundle("lua_scripts");
			if (scriptsBundle == null || scriptsBundle.isNull()) {
				scriptsBundle = new Bundle();
			}
			
			// 保存脚本内容
			scriptsBundle.put(name, content);
			
			// 更新脚本名称列表
			ArrayList<String> scriptNames = new ArrayList<>();
			if (globalData.contains("lua_script_names")) {
				String[] existing = globalData.getStringArray("lua_script_names");
				if (existing != null) {
					for (String n : existing) {
						if (!n.equals(name)) {
							scriptNames.add(n);
						}
					}
				}
			}
			if (!scriptNames.contains(name)) {
				scriptNames.add(name);
			}
			globalData.put("lua_script_names", scriptNames.toArray(new String[0]));
			
			// 保存回全局数据
			globalData.put("lua_scripts", scriptsBundle);
			com.zootdungeon.SaveManager.saveGlobal(globalData);
			
			return true;
		} catch (Exception e) {
			GLog.w("Failed to save script: " + e.getMessage());
			return false;
		}
	}
	
	private void showScriptList() {
		List<String> scripts = getSavedScripts();
		if (scripts.isEmpty()) {
			showResult("没有保存的脚本", 0xFF4444);
			return;
		}
		
		// 创建脚本列表窗口
		Window listWindow = new Window();
		
		int width = 150;
		float pos = 2;
		
		RenderedTextBlock title = PixelScene.renderTextBlock("选择脚本", 9);
		title.maxWidth(width);
		title.hardlight(Window.TITLE_COLOR);
		title.setPos((width - title.width()) / 2, pos);
		listWindow.add(title);
		
		pos = title.bottom() + 4;
		
		// 添加脚本按钮
		for (final String scriptName : scripts) {
			RedButton btn = new RedButton(scriptName) {
				@Override
				protected void onClick() {
					hide();
					loadScript(scriptName);
				}
			};
			btn.setRect(2, pos, width - 4, BUTTON_HEIGHT);
			listWindow.add(btn);
			pos += BUTTON_HEIGHT + 2;
		}
		
		listWindow.resize(width, (int)pos + 2);
		GameScene.show(listWindow);
	}
	
	private List<String> getSavedScripts() {
		List<String> scripts = new ArrayList<>();
		try {
			Bundle globalData = com.zootdungeon.SaveManager.loadGlobal();
			if (globalData != null && globalData.contains("lua_script_names")) {
				String[] scriptNames = globalData.getStringArray("lua_script_names");
				if (scriptNames != null) {
					for (String name : scriptNames) {
						scripts.add(name);
					}
				}
			}
		} catch (Exception e) {
			GLog.w("Failed to list scripts: " + e.getMessage());
		}
		return scripts;
	}
	
	private void loadScript(String name) {
		try {
			Bundle globalData = com.zootdungeon.SaveManager.loadGlobal();
			if (globalData != null) {
				Bundle scriptsBundle = globalData.getBundle("lua_scripts");
				if (scriptsBundle != null && scriptsBundle.contains(name)) {
					String content = scriptsBundle.getString(name);
					textBox.setText(content);
					showResult("已加载脚本: " + name, Window.TITLE_COLOR);
					return;
				}
			}
			showResult("脚本不存在: " + name, 0xFF4444);
		} catch (Exception e) {
			showResult("加载失败: " + e.getMessage(), 0xFF4444);
			GLog.w("Failed to load script: " + e.getMessage());
		}
	}
}

