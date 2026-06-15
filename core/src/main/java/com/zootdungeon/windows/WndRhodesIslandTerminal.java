package com.zootdungeon.windows;

import com.watabou.noosa.ui.Component;
import com.watabou.noosa.PointerArea;

import com.zootdungeon.Dungeon;
import com.zootdungeon.actors.hero.Belongings;
import com.zootdungeon.actors.hero.Hero;
import com.zootdungeon.actors.hero.Talent;
import com.zootdungeon.arknights.RhodesIslandTerminal;
import com.zootdungeon.arknights.SkillRecord;
import com.zootdungeon.arknights.plugins.SkillSheet.SkillDef;
import com.zootdungeon.items.Item;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.scenes.GameScene;
import com.zootdungeon.scenes.PixelScene;
import com.zootdungeon.sprites.ItemSprite;
import com.zootdungeon.ui.RedButton;
import com.zootdungeon.ui.RenderedTextBlock;
import com.zootdungeon.ui.ScrollPane;
import com.zootdungeon.ui.Window;
import com.zootdungeon.utils.GLog;

import java.util.ArrayList;
import java.util.List;

public class WndRhodesIslandTerminal extends Window {

	private static final int WIDTH_MAX = 176;
	private static final int LAYOUT_MIN_W = 104;
	private static final int SCREEN_EDGE_MARGIN = 24;
	private static final int GAP = 2;
	private static final int MARGIN = 3;
	private static final int BTN_HEIGHT = 16;
	private static final int MIN_VIEW_H = 96;

	private static final int BTN_INFO_W = 22;
	private static final int BTN_ACTIVATE_W = 32;
	private static final int BTN_UNINSTALL_W = 28;
	private static final int BTN_EXIT_W = 34;
	private static final int EMPTY_TEXT_H = 14;

	private static WndRhodesIslandTerminal openInstance;

	private int layoutWidth = WIDTH_MAX;

	private final RhodesIslandTerminal terminal;
	private IconTitle title;
	private RedButton btnExit;
	private RedButton btnInstall;
	private ScrollPane contentPane;
	private transient boolean relayoutInProgress = false;

	@Override
	public void offset(int xOffset, int yOffset) {
		super.offset(xOffset, yOffset);
		if (!relayoutInProgress) {
			relayout();
		}
	}

	@Override
	public void boundOffsetWithMargin(int margin) {
		super.boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
	}

	public WndRhodesIslandTerminal(RhodesIslandTerminal terminal) {
		this.terminal = terminal;
		if (openInstance != null && openInstance.parent != null) {
			openInstance.hide();
		}
		openInstance = this;
		buildWindow();
	}

	private void buildWindow() {
		title = new IconTitle(new ItemSprite(terminal), Messages.get(RhodesIslandTerminal.class, "name"));
		add(title);

		btnExit = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_exit"), 7) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		add(btnExit);

		syncLayoutWidth();
		relayout();
		rebuildContent();
	}

	private int maxInnerWidth() {
		return Math.max(LAYOUT_MIN_W,
				(int) PixelScene.uiCamera.width - 2 * SCREEN_EDGE_MARGIN - chrome.marginHor());
	}

	private int maxInnerHeight() {
		return (int) PixelScene.uiCamera.height - 2 * SCREEN_EDGE_MARGIN - chrome.marginVer();
	}

	private void syncLayoutWidth() {
		int screenW = (int) PixelScene.uiCamera.width;
		int screenH = (int) PixelScene.uiCamera.height;
		int maxByScreen = maxInnerWidth();
		boolean portrait = screenH > screenW;
		if (portrait) {
			layoutWidth = maxByScreen;
		} else {
			layoutWidth = Math.min(WIDTH_MAX, maxByScreen);
		}
		layoutWidth = Math.max(LAYOUT_MIN_W, Math.min(layoutWidth, maxByScreen));
	}

	private void relayout() {
		if (relayoutInProgress) return;
		relayoutInProgress = true;
		try {
			syncLayoutWidth();
			float y = MARGIN;
			float titleW = layoutWidth - BTN_EXIT_W - GAP;
			title.setRect(0, y, titleW, 0);
			btnExit.setRect(titleW + GAP, y, BTN_EXIT_W, BTN_HEIGHT);
			y = Math.max(title.bottom(), btnExit.bottom()) + GAP;

			int viewH = getAvailableViewHeight(y);
			if (contentPane != null) {
				int maxWindowHeight = maxInnerHeight();
				int finalH = (int) (y + viewH + MARGIN);
				if (finalH > maxWindowHeight) {
					finalH = maxWindowHeight;
				}
				viewH = Math.max(0, finalH - (int) y - MARGIN);
				contentPane.setRect(0, y, layoutWidth, viewH);
				resize(layoutWidth, finalH);
				boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
				return;
			}
			int maxWindowHeight = maxInnerHeight();
			int finalH = (int) (y + viewH + MARGIN);
			if (finalH > maxWindowHeight) finalH = maxWindowHeight;
			resize(layoutWidth, finalH);
			boundOffsetWithMargin(SCREEN_EDGE_MARGIN);
		} finally {
			relayoutInProgress = false;
		}
	}

	private int getAvailableViewHeight(float contentTop) {
		int maxWindowHeight = maxInnerHeight();
		int bySpace = (int) (maxWindowHeight - contentTop - MARGIN);
		return Math.max(MIN_VIEW_H, bySpace);
	}

	public void refresh() {
		rebuildContent();
	}

	private void rebuildContent() {
		if (contentPane != null) {
			contentPane.destroy();
			remove(contentPane);
			contentPane = null;
		}
		if (btnInstall != null) {
			remove(btnInstall);
			btnInstall = null;
		}
		syncLayoutWidth();
		Component content = buildActiveContent();
		contentPane = new InteractiveScrollPane(content);
		contentPane.scrollTo(0, 0);
		add(contentPane);
		relayout();
	}

	private Component buildActiveContent() {
		Component root = new Component();
		ColumnLayout col = new ColumnLayout(root, 0, 0, layoutWidth, GAP);

		// COST 资源条
		RenderedTextBlock costLabel = PixelScene.renderTextBlock(
				Messages.get(RhodesIslandTerminal.class, "cost_display", terminal.cost(),
						RhodesIslandTerminal.effectiveCostCap(Dungeon.hero)), 8);
		costLabel.maxWidth(layoutWidth);
		col.addAutoHeight(costLabel);

		// 槽位信息
		int used = terminal.installedSkills().size();
		int max = RhodesIslandTerminal.maxSkillSlots(Dungeon.hero);
		RenderedTextBlock slotsInfo = PixelScene.renderTextBlock(
				Messages.get(RhodesIslandTerminal.class, "skill_slot_display", used, max), 7);
		slotsInfo.maxWidth(layoutWidth);
		col.addAutoHeight(slotsInfo);

		// surge 按钮（仅在点过天赋时）
		if (Dungeon.hero != null && Dungeon.hero.pointsInTalent(Talent.RESERVED_OP_COST_SURGE) > 0) {
			RedButton surgeBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_cost_surge"), 8) {
				@Override
				protected void onClick() {
					RhodesIslandTerminal.surgeAllCostIntoMagicalGear(Dungeon.hero);
					refresh();
				}
			};
			col.addFixedHeight(surgeBtn, BTN_HEIGHT);
		}

		// 已安装技能列表
		for (SkillDef skill : terminal.installedSkills()) {
			addSkillRow(root, col, skill);
		}

		// 槽位未满时显示"安装新技能"按钮（放在 content 里也滚得动；放在 contentPane 外可避免布局错位——这里放在 content 内以便随滚动）
		Hero hero = Dungeon.hero;
		if (hero != null && used < max) {
			btnInstall = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_install_skill"), 8) {
				@Override
				protected void onClick() {
					openSkillRecordSelector();
				}
			};
			col.addFixedHeight(btnInstall, BTN_HEIGHT);
		}

		root.setSize(layoutWidth, col.bottom());
		return root;
	}

	private void addSkillRow(Component root, ColumnLayout col, SkillDef skill) {
		float rowY = col.y;
		int rightW = BTN_ACTIVATE_W + GAP + BTN_UNINSTALL_W + GAP + BTN_INFO_W;
		int titleW = layoutWidth - rightW - GAP;

		String titleText = skill.name;
		if (skill.cost > 0) {
			titleText += " (" + Messages.get(RhodesIslandTerminal.class, "cost_use", skill.cost) + ")";
		}

		RenderedTextBlock titleBlock = PixelScene.renderTextBlock(titleText, 8);
		titleBlock.maxWidth(titleW);
		titleBlock.setRect(0, rowY, titleW, 12);
		root.add(titleBlock);

		float btnX = layoutWidth - rightW;
		RedButton infoBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_info"), 7) {
			@Override
			protected void onClick() {
				GameScene.show(new WndMessage(skill.desc));
			}
		};
		infoBtn.setRect(btnX, rowY, BTN_INFO_W, BTN_HEIGHT);
		root.add(infoBtn);
		btnX += BTN_INFO_W + GAP;

		final SkillDef target = skill;
		RedButton activateBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_activate"), 7) {
			@Override
			protected void onClick() {
				if (!terminal.spendCost(target.cost)) {
					GLog.w(Messages.get(RhodesIslandTerminal.class, "cost_not_enough", target.cost));
					return;
				}
				target.execute(Dungeon.hero);
				hide();
			}
		};
		activateBtn.setRect(btnX, rowY, BTN_ACTIVATE_W, BTN_HEIGHT);
		activateBtn.enable(terminal.cost() >= skill.cost);
		root.add(activateBtn);
		btnX += BTN_ACTIVATE_W + GAP;

		RedButton uninstallBtn = new RedButton(Messages.get(RhodesIslandTerminal.class, "btn_uninstall"), 7) {
			@Override
			protected void onClick() {
				terminal.uninstallSkill(target);
				refresh();
			}
		};
		uninstallBtn.setRect(btnX, rowY, BTN_UNINSTALL_W, BTN_HEIGHT);
		root.add(uninstallBtn);

		col.y = Math.max(titleBlock.bottom(), rowY + BTN_HEIGHT) + GAP;
	}

	private void openSkillRecordSelector() {
		WndBag.ItemSelector selector = new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(RhodesIslandTerminal.class, "install_skill_prompt");
			}

			@Override
			public Class<? extends com.zootdungeon.items.bags.Bag> preferredBag() {
				return Belongings.Backpack.class;
			}

			@Override
			public boolean itemSelectable(Item item) {
				return item instanceof SkillRecord si && si.skill() != null && !terminal.isInstalled(si.skill());
			}

			@Override
			public void onSelect(Item item) {
				if (item instanceof SkillRecord && Dungeon.hero != null) {
					terminal.installSkillFromItem(Dungeon.hero, item);
					if (openInstance != null) openInstance.refresh();
				}
			}
		};
		GameScene.selectItem(selector);
	}

	@Override
	public void destroy() {
		super.destroy();
		if (openInstance == this) openInstance = null;
	}

	/**
	 * ScrollPane 的默认滚动热区会 block pointer 输入，导致滚动区域里的 Button 点不到。
	 * 这里把滚动热区改成 NEVER_BLOCK，让按钮仍然能收到点击事件，同时保留滚动能力。
	 */
	private static class InteractiveScrollPane extends ScrollPane {
		public InteractiveScrollPane(Component content) {
			super(content);
			if (controller != null) {
				controller.blockLevel = PointerArea.NEVER_BLOCK;
			}
		}
	}

	private static class ColumnLayout {
		public final Component parent;
		public final float x;
		public float y;
		public final float width;
		public final float gap;

		public ColumnLayout(Component parent, float x, float y, float width, float gap) {
			this.parent = parent;
			this.x = x;
			this.y = y;
			this.width = width;
			this.gap = gap;
		}

		public void addAutoHeight(Component c) {
			c.setRect(x, y, width, 0);
			parent.add(c);
			y = c.bottom() + gap;
		}

		public void addFixedHeight(Component c, float height) {
			c.setRect(x, y, width, height);
			parent.add(c);
			y = c.bottom() + gap;
		}

		public int bottom() {
			return (int) Math.ceil(y);
		}
	}
}
