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

package com.zootdungeon;

import java.io.IOException;
import java.util.LinkedHashMap;

import com.badlogic.gdx.Input;
import com.watabou.input.ControllerHandler;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;
// this file handles key binding
public class CDKeyBinding extends GameAction {

	protected CDKeyBinding(String name ){
		super( name );
	}

	//--New references to existing actions from GameAction
	public static final GameAction NONE  = GameAction.NONE;
	public static final GameAction BACK  = GameAction.BACK;

	public static final GameAction LEFT_CLICK   = GameAction.LEFT_CLICK;
	public static final GameAction RIGHT_CLICK  = GameAction.RIGHT_CLICK;
	public static final GameAction MIDDLE_CLICK = GameAction.MIDDLE_CLICK;
	//--

	public static final GameAction N            = new CDKeyBinding("n");
	public static final GameAction W            = new CDKeyBinding("w");
	public static final GameAction S            = new CDKeyBinding("s");
	public static final GameAction E            = new CDKeyBinding("e");
	public static final GameAction NW           = new CDKeyBinding("nw");
	public static final GameAction NE           = new CDKeyBinding("ne");
	public static final GameAction SW           = new CDKeyBinding("sw");
	public static final GameAction SE           = new CDKeyBinding("se");
	public static final GameAction WAIT_OR_PICKUP   = new CDKeyBinding("wait_or_pickup");

	public static final GameAction INVENTORY    = new CDKeyBinding("inventory");
	public static final GameAction INVENTORY_SELECTOR   = new CDKeyBinding("inventory_selector");
	public static final GameAction QUICKSLOT_SELECTOR   = new CDKeyBinding("quickslot_selector");
	public static final GameAction QUICKSLOT_1  = new CDKeyBinding("quickslot_1");
	public static final GameAction QUICKSLOT_2  = new CDKeyBinding("quickslot_2");
	public static final GameAction QUICKSLOT_3  = new CDKeyBinding("quickslot_3");
	public static final GameAction QUICKSLOT_4  = new CDKeyBinding("quickslot_4");
	public static final GameAction QUICKSLOT_5  = new CDKeyBinding("quickslot_5");
	public static final GameAction QUICKSLOT_6  = new CDKeyBinding("quickslot_6");

	public static final GameAction BAG_1        = new CDKeyBinding("bag_1");
	public static final GameAction BAG_2        = new CDKeyBinding("bag_2");
	public static final GameAction BAG_3        = new CDKeyBinding("bag_3");
	public static final GameAction BAG_4        = new CDKeyBinding("bag_4");
	public static final GameAction BAG_5        = new CDKeyBinding("bag_5");

	public static final GameAction EXAMINE      = new CDKeyBinding("examine");
	public static final GameAction WAIT         = new CDKeyBinding("wait");
	public static final GameAction REST         = new CDKeyBinding("rest");

	public static final GameAction TAG_ATTACK   = new CDKeyBinding("tag_attack");
	public static final GameAction TAG_ACTION   = new CDKeyBinding("tag_action");
	public static final GameAction TAG_LOOT     = new CDKeyBinding("tag_loot");
	public static final GameAction TAG_RESUME   = new CDKeyBinding("tag_resume");

	public static final GameAction CYCLE        = new CDKeyBinding("cycle");

	public static final GameAction HERO_INFO    = new CDKeyBinding("hero_info");
	public static final GameAction JOURNAL      = new CDKeyBinding("journal");

	public static final GameAction ZOOM_IN      = new CDKeyBinding("zoom_in");
	public static final GameAction ZOOM_OUT     = new CDKeyBinding("zoom_out");

	// custom: open in-game command console
	public static final GameAction CONSOLE      = new CDKeyBinding("console");

	private static final LinkedHashMap<Integer, GameAction> defaultBindings = new LinkedHashMap<>();
	static {
		defaultBindings.put( Input.Keys.ESCAPE,         CDKeyBinding.BACK );
		defaultBindings.put( Input.Keys.BACKSPACE,      CDKeyBinding.BACK );

		defaultBindings.put( Input.Keys.W,              CDKeyBinding.N );
		defaultBindings.put( Input.Keys.A,              CDKeyBinding.W );
		defaultBindings.put( Input.Keys.S,              CDKeyBinding.S );
		defaultBindings.put( Input.Keys.D,              CDKeyBinding.E );
		defaultBindings.put( Input.Keys.SPACE,          CDKeyBinding.WAIT_OR_PICKUP);

		defaultBindings.put( Input.Keys.UP,             CDKeyBinding.N );
		defaultBindings.put( Input.Keys.LEFT,           CDKeyBinding.W );
		defaultBindings.put( Input.Keys.DOWN,           CDKeyBinding.S );
		defaultBindings.put( Input.Keys.RIGHT,          CDKeyBinding.E );

		defaultBindings.put( Input.Keys.NUMPAD_8,       CDKeyBinding.N );
		defaultBindings.put( Input.Keys.NUMPAD_4,       CDKeyBinding.W );
		defaultBindings.put( Input.Keys.NUMPAD_2,       CDKeyBinding.S );
		defaultBindings.put( Input.Keys.NUMPAD_6,       CDKeyBinding.E );
		defaultBindings.put( Input.Keys.NUMPAD_7,       CDKeyBinding.NW );
		defaultBindings.put( Input.Keys.NUMPAD_9,       CDKeyBinding.NE );
		defaultBindings.put( Input.Keys.NUMPAD_1,       CDKeyBinding.SW );
		defaultBindings.put( Input.Keys.NUMPAD_3,       CDKeyBinding.SE );
		defaultBindings.put( Input.Keys.NUMPAD_5,       CDKeyBinding.WAIT_OR_PICKUP );

		defaultBindings.put( Input.Keys.F,              CDKeyBinding.INVENTORY );
		defaultBindings.put( Input.Keys.I,              CDKeyBinding.INVENTORY );
		defaultBindings.put( Input.Keys.NUM_1,          CDKeyBinding.QUICKSLOT_1 );
		defaultBindings.put( Input.Keys.NUM_2,          CDKeyBinding.QUICKSLOT_2 );
		defaultBindings.put( Input.Keys.NUM_3,          CDKeyBinding.QUICKSLOT_3 );
		defaultBindings.put( Input.Keys.NUM_4,          CDKeyBinding.QUICKSLOT_4 );
		defaultBindings.put( Input.Keys.NUM_5,          CDKeyBinding.QUICKSLOT_5 );
		defaultBindings.put( Input.Keys.NUM_6,          CDKeyBinding.QUICKSLOT_6 );

		defaultBindings.put( Input.Keys.F1,             CDKeyBinding.BAG_1 );
		defaultBindings.put( Input.Keys.F2,             CDKeyBinding.BAG_2 );
		defaultBindings.put( Input.Keys.F3,             CDKeyBinding.BAG_3 );
		defaultBindings.put( Input.Keys.F4,             CDKeyBinding.BAG_4 );
		defaultBindings.put( Input.Keys.F5,             CDKeyBinding.BAG_5 );

		defaultBindings.put( Input.Keys.E,              CDKeyBinding.EXAMINE );
		defaultBindings.put( Input.Keys.Z,              CDKeyBinding.REST );

		defaultBindings.put( Input.Keys.Q,              CDKeyBinding.TAG_ATTACK );
		defaultBindings.put( Input.Keys.TAB,            CDKeyBinding.CYCLE);
		defaultBindings.put( Input.Keys.X,              CDKeyBinding.TAG_ACTION );
		defaultBindings.put( Input.Keys.C,              CDKeyBinding.TAG_LOOT );
		defaultBindings.put( Input.Keys.ENTER,          CDKeyBinding.TAG_LOOT );
		defaultBindings.put( Input.Keys.R,              CDKeyBinding.TAG_RESUME );

		defaultBindings.put( Input.Keys.H,              CDKeyBinding.HERO_INFO );
		defaultBindings.put( Input.Keys.J,              CDKeyBinding.JOURNAL );

		defaultBindings.put( Input.Keys.PLUS,           CDKeyBinding.ZOOM_IN );
		defaultBindings.put( Input.Keys.EQUALS,         CDKeyBinding.ZOOM_IN );
		defaultBindings.put( Input.Keys.MINUS,          CDKeyBinding.ZOOM_OUT );

		// default binding for opening the console on desktop keyboards (the ` key)
		defaultBindings.put( Input.Keys.GRAVE,          CDKeyBinding.CONSOLE );
	}

	public static LinkedHashMap<Integer, GameAction> getDefaults() {
		return new LinkedHashMap<>(defaultBindings);
	}

	private static final LinkedHashMap<Integer, GameAction> defaultControllerBindings = new LinkedHashMap<>();
	static {
		defaultControllerBindings.put( Input.Keys.BUTTON_START,     CDKeyBinding.BACK );
		defaultControllerBindings.put( Input.Keys.BUTTON_SELECT,    CDKeyBinding.JOURNAL );

		defaultControllerBindings.put( Input.Keys.BUTTON_R2,        CDKeyBinding.LEFT_CLICK );
		defaultControllerBindings.put( Input.Keys.BUTTON_THUMBR,    CDKeyBinding.LEFT_CLICK );
		defaultControllerBindings.put( Input.Keys.BUTTON_L2,        CDKeyBinding.RIGHT_CLICK );

		defaultControllerBindings.put( Input.Keys.DPAD_UP+1000,     CDKeyBinding.TAG_ACTION );
		defaultControllerBindings.put( Input.Keys.DPAD_LEFT+1000,   CDKeyBinding.TAG_LOOT );
		defaultControllerBindings.put( Input.Keys.DPAD_DOWN+1000,   CDKeyBinding.TAG_RESUME );
		defaultControllerBindings.put( Input.Keys.DPAD_RIGHT+1000,  CDKeyBinding.CYCLE );

		defaultControllerBindings.put( Input.Keys.BUTTON_THUMBL,    CDKeyBinding.WAIT_OR_PICKUP );

		defaultControllerBindings.put( Input.Keys.BUTTON_R1,        CDKeyBinding.ZOOM_IN );
		defaultControllerBindings.put( Input.Keys.BUTTON_L1,        CDKeyBinding.ZOOM_OUT );

		defaultControllerBindings.put( Input.Keys.BUTTON_A,         CDKeyBinding.TAG_ATTACK );
		defaultControllerBindings.put( Input.Keys.BUTTON_B,         CDKeyBinding.EXAMINE );
		defaultControllerBindings.put( Input.Keys.BUTTON_X,         CDKeyBinding.QUICKSLOT_SELECTOR );
		defaultControllerBindings.put( Input.Keys.BUTTON_Y,         CDKeyBinding.INVENTORY_SELECTOR );
	}

	public static LinkedHashMap<Integer, GameAction> getControllerDefaults() {
		return new LinkedHashMap<>(defaultControllerBindings);
	}

	static {
		//hard bindings for android devices
		KeyBindings.addHardBinding( Input.Keys.BACK, CDKeyBinding.BACK );
		KeyBindings.addHardBinding( Input.Keys.MENU, CDKeyBinding.INVENTORY );

		//hard bindings for desktop fullscreen toggle
		//not bound to specific game actions, see PixelScene
		//Note that user-entered bindings can override these individually, and that's fine.
		KeyBindings.addHardBinding( Input.Keys.ALT_RIGHT, CDKeyBinding.NONE );
		KeyBindings.addHardBinding( Input.Keys.ENTER, CDKeyBinding.NONE );
	}

	//we only save/loads keys which differ from the default configuration.
	private static final String BINDINGS_FILE = "keybinds.dat";

	public static void loadBindings(){

		if (!KeyBindings.getAllBindings().isEmpty()){
			return;
		}

		try {
			Bundle b = FileUtils.bundleFromFile(BINDINGS_FILE);

			Bundle firstKeys = b.getBundle("first_keys");
			Bundle secondKeys = b.getBundle("second_keys");
			Bundle thirdKeys = b.getBundle("third_keys");

			LinkedHashMap<Integer, GameAction> defaults = getDefaults();
			LinkedHashMap<Integer, GameAction> merged = new LinkedHashMap<>();

			for (GameAction a : allActions()) {
				if (firstKeys.contains(a.name()) && KeyEvent.isKeyboardKey(firstKeys.getInt(a.name()))) {
					if (firstKeys.getInt(a.name()) == 0){
						continue; //we have no keys assigned to this action, move to the next one
					} else {
						merged.put(firstKeys.getInt(a.name()), a);
						//remove whatever the first default key was for this action, if any
						for (int i : defaults.keySet()) {
							if (defaults.get(i) == a) {
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the first one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

				if (secondKeys.contains(a.name()) && KeyEvent.isKeyboardKey(secondKeys.getInt(a.name()))) {
					if (secondKeys.getInt(a.name()) == 0){
						continue; //we have no more keys assigned to this action, move to the next one
					} else {
						merged.put(secondKeys.getInt(a.name()), a);
						//remove whatever the second default key was for this action, if any
						for (int i : defaults.keySet()){
							if (defaults.get(i) == a){
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the next one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

				if (thirdKeys.contains(a.name()) && KeyEvent.isKeyboardKey(thirdKeys.getInt(a.name()))) {
					if (thirdKeys.getInt(a.name()) == 0){
						continue; //we have no more keys assigned to this action, move to the next one
					} else {
						merged.put(thirdKeys.getInt(a.name()), a);
						//remove whatever the third default key was for this action, if any
						for (int i : defaults.keySet()){
							if (defaults.get(i) == a){
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the next one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

			}

			KeyBindings.setAllBindings(merged);

			defaults = getControllerDefaults();
			merged.clear();

			Bundle firstButtons = b.getBundle("first_keys_controller");
			Bundle secondButtons = b.getBundle("second_keys_controller");
			Bundle thirdButtons = b.getBundle("third_keys_controller");

			for (GameAction a : allActions()) {
				if (firstButtons.contains(a.name()) && ControllerHandler.icControllerKey(firstButtons.getInt(a.name()))) {
					if (firstButtons.getInt(a.name()) == 0){
						continue; //we have no keys assigned to this action, move to the next one
					} else {
						merged.put(firstButtons.getInt(a.name()), a);
						//remove whatever the first default button was for this action, if any
						for (int i : defaults.keySet()) {
							if (defaults.get(i) == a) {
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the first one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

				if (secondButtons.contains(a.name()) && ControllerHandler.icControllerKey(secondButtons.getInt(a.name()))) {
					if (secondButtons.getInt(a.name()) == 0){
						continue; //we have no more keys assigned to this action, move to the next one
					} else {
						merged.put(secondButtons.getInt(a.name()), a);
						//remove whatever the second default button was for this action, if any
						for (int i : defaults.keySet()) {
							if (defaults.get(i) == a) {
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the next one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

				if (thirdButtons.contains(a.name()) && ControllerHandler.icControllerKey(thirdButtons.getInt(a.name()))) {
					if (thirdButtons.getInt(a.name()) == 0){
						continue; //we have no more keys assigned to this action, move to the next one
					} else {
						merged.put(thirdButtons.getInt(a.name()), a);
						//remove whatever the third default button was for this action, if any
						for (int i : defaults.keySet()) {
							if (defaults.get(i) == a) {
								defaults.remove(i);
								break;
							}
						}
					}
				} else {
					//if we have no custom key here, find the next one from defaults and merge it
					for (int i : defaults.keySet()){
						if (defaults.get(i) == a){
							merged.put(i, defaults.remove(i));
							break;
						}
					}
				}

			}

			KeyBindings.setAllControllerBindings(merged);

		} catch (Exception e){
			KeyBindings.setAllBindings(getDefaults());
			KeyBindings.setAllControllerBindings(getControllerDefaults());
		}

	}

	public static void saveBindings(){

		Bundle b = new Bundle();

		Bundle firstKeys = new Bundle();
		Bundle secondKeys = new Bundle();
		Bundle thirdKeys = new Bundle();

		for (GameAction a : allActions()){
			int firstCur = 0;
			int secondCur = 0;
			int thirdCur = 0;
			int firstDef = 0;
			int secondDef = 0;
			int thirdDef = 0;

			for (int i : defaultBindings.keySet()){
				if (defaultBindings.get(i) == a){
					if (firstDef == 0) {
						firstDef = i;
					} else if (secondDef == 0) {
						secondDef = i;
					} else {
						thirdDef = i;
					}
				}
			}

			LinkedHashMap<Integer, GameAction> curBindings = KeyBindings.getAllBindings();
			for (int i : curBindings.keySet()){
				if (curBindings.get(i) == a){
					if (firstCur == 0) {
						firstCur = i;
					} else if (secondCur == 0) {
						secondCur = i;
					} else {
						thirdCur = i;
					}
				}
			}

			if (firstCur != firstDef){
				firstKeys.put(a.name(), firstCur);
			}
			if (secondCur != secondDef){
				secondKeys.put(a.name(), secondCur);
			}
			if (thirdCur != thirdDef){
				thirdKeys.put(a.name(), thirdCur);
			}

		}

		b.put("first_keys", firstKeys);
		b.put("second_keys", secondKeys);
		b.put("third_keys", thirdKeys);

		Bundle firstButtons = new Bundle();
		Bundle secondButtons = new Bundle();
		Bundle thirdButtons = new Bundle();

		for (GameAction a : allActions()){
			int firstCur = 0;
			int secondCur = 0;
			int thirdCur = 0;
			int firstDef = 0;
			int secondDef = 0;
			int thirdDef = 0;

			for (int i : defaultControllerBindings.keySet()){
				if (defaultControllerBindings.get(i) == a){
					if (firstDef == 0) {
						firstDef = i;
					} else if (secondDef == 0) {
						secondDef = i;
					} else {
						thirdDef = i;
					}
				}
			}

			LinkedHashMap<Integer, GameAction> curBindings = KeyBindings.getAllControllerBindings();
			for (int i : curBindings.keySet()){
				if (curBindings.get(i) == a){
					if (firstCur == 0) {
						firstCur = i;
					} else if (secondCur == 0) {
						secondCur = i;
					} else {
						thirdCur = i;
					}
				}
			}

			if (firstCur != firstDef){
				firstButtons.put(a.name(), firstCur);
			}
			if (secondCur != secondDef){
				secondButtons.put(a.name(), secondCur);
			}
			if (thirdCur != thirdDef){
				thirdButtons.put(a.name(), thirdCur);
			}

		}

		b.put("first_keys_controller", firstButtons);
		b.put("second_keys_controller", secondButtons);
		b.put("third_keys_controller", thirdButtons);

		try {
			FileUtils.bundleToFile(BINDINGS_FILE, b);
		} catch (IOException e) {
			ColaDungeon.reportException(e);
		}

	}

}
