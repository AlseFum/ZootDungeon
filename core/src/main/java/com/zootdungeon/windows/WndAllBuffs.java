package com.zootdungeon.windows;

import com.zootdungeon.actors.Char;
import com.zootdungeon.actors.buffs.Buff;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.Icons;

public class WndAllBuffs extends WndTitledMessage {

	private static final String NO_BUFFS = "No buffs.";

	public WndAllBuffs(Char ch) {
		super(Icons.get(Icons.BUFFS), Messages.titleCase(ch.name()) + " Buffs", describeAllBuffs(ch));
	}

	private static String describeAllBuffs(Char ch) {
		StringBuilder sb = new StringBuilder();

		for (Buff buff : ch.buffs()) {
			if (sb.length() > 0) {
				sb.append("\n\n");
			}

			sb.append(Messages.titleCase(buff.name()));
			sb.append(" [").append(buff.getClass().getSimpleName()).append("]");
			if (buff.reifyFrom() != null) {
				sb.append(" from: ").append(buff.reifyFrom().getSimpleName());
			}

			String desc = buff.desc();
			if (desc != null && !desc.isEmpty()) {
				sb.append("\n").append(desc);
			}
		}

		return sb.length() == 0 ? NO_BUFFS : sb.toString();
	}

	@Override
	protected boolean useHighlighting() {
		return false;
	}
}
