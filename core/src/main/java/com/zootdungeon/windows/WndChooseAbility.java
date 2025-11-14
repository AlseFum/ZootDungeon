package com.zootdungeon.windows;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.abilities.ArmorAbility;
import com.zootdungeon.actors.hero.abilities.cleric.Trinity;
import com.zootdungeon.items.KingsCrown;
import com.zootdungeon.items.armor.Armor;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.HeroIcon;
import com.zootdungeon.ui.IconButton;
import com.zootdungeon.ui.Icons;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.Window;

public class WndChooseAbility extends Window {

	private static final int WIDTH		= 130;
	private static final float GAP		= 2;

	public WndChooseAbility(final KingsCrown crown, final Armor armor, final Hero hero){

		super();

		//crown can be null if hero is choosing from armor
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( crown == null ? armor.image() : crown.image(), null ) );
		titlebar.label( Messages.titleCase(crown == null ? armor.name() : crown.name()) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock body = PixelScene.renderTextBlock( 6 );
		if (crown != null) {
			body.text(Messages.get(this, "message"), WIDTH);
		} else {
			body.text(Messages.get(this, "message_no_crown"), WIDTH);
		}
		body.setPos( titlebar.left(), titlebar.bottom() + GAP );
		add( body );

		float pos = body.bottom() + 3*GAP;
		for (ArmorAbility ability : hero.heroClass.armorAbilities()) {

			String warn;
			if (Dungeon.initialVersion < 821 && ability instanceof Trinity){
				warn = "_WARNING, code to track which items you have found for use in trinity was added in BETA-2.2. This run was started before that, and so some items you have encountered may not be usable with Trinity. Any items you currently hold can be made selectable by dropping and picking them back up._\n\n";
			} else {
				warn = "";
			}
			RedButton abilityButton = new RedButton(ability.shortDesc(), 6){
				@Override
				protected void onClick() {
					GameScene.show(new WndOptions( new HeroIcon( ability ),
							Messages.titleCase(ability.name()),
							warn + Messages.get(WndChooseAbility.this, "are_you_sure"),
							Messages.get(WndChooseAbility.this, "yes"),
							Messages.get(WndChooseAbility.this, "no")){

						@Override
						protected void onSelect(int index) {
							hide();
							if (index == 0 && WndChooseAbility.this.parent != null){
								WndChooseAbility.this.hide();
								if (crown != null) {
									crown.upgradeArmor(hero, armor, ability);
								} else {
									new KingsCrown().upgradeArmor(hero, null, ability);
								}
							}
						}
					});
				}
			};
			abilityButton.leftJustify = true;
			abilityButton.multiline = true;
			abilityButton.setSize(WIDTH-20, abilityButton.reqHeight()+2);
			abilityButton.setRect(0, pos, WIDTH-20, abilityButton.reqHeight()+2);
			add(abilityButton);

			IconButton abilityInfo = new IconButton(Icons.get(Icons.INFO)){
				@Override
				protected void onClick() {
					GameScene.show(new WndInfoArmorAbility(Dungeon.hero.heroClass, ability));
				}
			};
			abilityInfo.setRect(WIDTH-20, abilityButton.top() + (abilityButton.height()-20)/2, 20, 20);
			add(abilityInfo);

			pos = abilityButton.bottom() + GAP;
		}

		RedButton cancelButton = new RedButton(Messages.get(this, "cancel")){
			@Override
			protected void onClick() {
				hide();
			}
		};
		cancelButton.setRect(0, pos, WIDTH, 18);
		add(cancelButton);
		pos = cancelButton.bottom() + GAP;

		resize(WIDTH, (int)pos);

	}


}
