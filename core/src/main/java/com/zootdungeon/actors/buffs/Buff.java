package com.zootdungeon.actors.buffs;

import com.zootdungeon.actors.Actor;
import com.zootdungeon.actors.Char;
import com.zootdungeon.messages.Messages;
import com.zootdungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Reflection;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Buff extends Actor {

	public Char target;

	//whether this buff was already extended by the mnemonic prayer spell
	public boolean mnemonicExtended = false;

	{
		actPriority = BUFF_PRIO; //low priority, towards the end of a turn
	}

	public enum buffType {POSITIVE, NEGATIVE, NEUTRAL}
	public buffType type = buffType.NEUTRAL;

	//whether or not the buff announces its name
	public boolean announced = false;

	//whether a buff should persist through revive effects or similar (e.g. transmogrify)
	public boolean revivePersists = false;

	protected HashSet<Class> resistances = new HashSet<>();

	public HashSet<Class> resistances() {
		return new HashSet<>(resistances);
	}

	protected HashSet<Class> immunities = new HashSet<>();

	public HashSet<Class> immunities() {
		return new HashSet<>(immunities);
	}

	private transient boolean detachingFromRoot = false;
	private Class<? extends Buff> runtimeReifyFrom = null;

	// Non-null means this buff is a reified child of another buff.
	public Class<? extends Buff> reifyFrom() {
		return runtimeReifyFrom;
	}

	public void setReifiedFrom(Class<? extends Buff> rootClass) {
		runtimeReifyFrom = rootClass;
	}

	// Non-empty means this buff is composite and should materialize these child buffs.
	public Set<Class<? extends Buff>> reifiesTo() {
		return Collections.emptySet();
	}

	public final boolean isReified() {
		return reifyFrom() != null;
	}

	public final boolean hasReifiedChildren() {
		return !reifiesTo().isEmpty();
	}

	// By default every Buff is atomic unless it explicitly reifies child buffs.
	public final boolean isAtomicBuff() {
		return !hasReifiedChildren();
	}

	public final boolean isCompositeBuff() {
		return hasReifiedChildren();
	}

	public float resistFactor(Char target) {
		LinkedHashSet<Class<? extends Buff>> classes = reifiedLeafClassesOf(getClass());
		if (classes.isEmpty()) {
			return target.resist(getClass());
		}

		// Composite buffs use the strictest factor among themselves and all leaf children.
		float factor = target.resist(getClass());
		for (Class<? extends Buff> childClass : classes) {
			factor = Math.min(factor, target.resist(childClass));
		}
		return factor;
	}

	public boolean attachTo( Char target ) {

		if (target.isImmune( getClass() ) || !canAttachReifiedTo(target)) {
			return false;
		}

		this.target = target;

		if (target.add(this)){
			if (!target.isRestoringBuffs() && !syncReifiedChildren()) {
				detach();
				this.target = null;
				return false;
			}
			if (target.sprite != null) fx( true );
			return true;
		} else {
			this.target = null;
			return false;
		}
	}

	public void detach() {
		if (detachingFromRoot || target == null || !isReified()) {
			detachReifiedChildren();
			if (target != null && target.remove(this) && target.sprite != null) fx(false);
			return;
		}

		Class<? extends Buff> rootClass = reifyFrom();
		if (rootClass != null) {
			Buff root = target.buff(rootClass);
			if (root != null) {
				// Reified children never outlive their root buff.
				root.detach();
				return;
			}
		}

		detachReifiedChildren();
		if (target.remove(this) && target.sprite != null) fx(false);
	}

	public final void detachFromRoot() {
		detachingFromRoot = true;
		try {
			detachReifiedChildren();
			if (target != null && target.remove(this) && target.sprite != null) fx(false);
		} finally {
			detachingFromRoot = false;
		}
	}

	protected boolean canAttachReifiedTo(Char target) {
		if (isReified() && !target.isRestoringBuffs()) {
			Class<? extends Buff> rootClass = reifyFrom();
			if (rootClass != null && target.buff(rootClass) == null) {
				// Child buffs are not meant to be attached manually outside restore/sync flows.
				return false;
			}
		}

		if (!hasReifiedChildren()) {
			return true;
		}

		if (target.buff((Class<? extends Buff>) getClass()) != null) {
			return false;
		}

		for (Class<? extends Buff> childClass : reifiesTo()) {
			Buff child = Reflection.newInstance(childClass);
			if (child == null) {
				return false;
			}
			child.setReifiedFrom((Class<? extends Buff>) getClass());
			if (!target.canAddBuff(child) || target.isImmune(childClass)) {
				return false;
			}
		}

		return true;
	}

	public boolean syncReifiedChildren() {
		if (target == null || !hasReifiedChildren()) {
			return true;
		}

		// A composite buff is responsible for ensuring all of its children exist.
		for (Class<? extends Buff> childClass : reifiesTo()) {
			Buff child = findReifiedChild(childClass);
			if (child == null) {
				child = Reflection.newInstance(childClass);
				if (child == null) {
					return false;
				}
				child.setReifiedFrom((Class<? extends Buff>) getClass());
				if (child.reifyFrom() != getClass() || !child.attachTo(target)) {
					return false;
				}
			} else if (child.reifyFrom() != getClass()) {
				return false;
			}
			syncReifiedChildDuration(child);
		}
		return true;
	}

	protected void detachReifiedChildren() {
		if (target == null || !hasReifiedChildren()) {
			return;
		}

		for (Class<? extends Buff> childClass : reifiesTo()) {
			Buff child = findReifiedChild(childClass);
			if (child != null) {
				child.detachFromRoot();
			}
		}
	}

	protected Buff findReifiedChild(Class<? extends Buff> childClass) {
		if (target == null) {
			return null;
		}
		for (Buff buff : target.buffs(childClass)) {
			if (buff.getClass() == childClass && buff.reifyFrom() == getClass()) {
				return buff;
			}
		}
		return null;
	}

	protected void syncReifiedChildDuration(Buff child) {
		if (!(this instanceof FlavourBuff) || !(child instanceof FlavourBuff)) {
			return;
		}

		float rootCooldown = cooldown();
		float childCooldown = child.cooldown();
		if (childCooldown < rootCooldown) {
			child.postpone(rootCooldown - childCooldown);
		}
	}

	@Override
	public boolean act() {
		deactivate();
		return true;
	}

	public int icon() {
		return BuffIndicator.NONE;
	}

	//some buffs may want to tint the base texture color of their icon
	public void tintIcon( Image icon ){
		//do nothing by default
	}

	//percent (0-1) to fade out out the buff icon, usually if buff is expiring
	public float iconFadePercent(){
		return 0;
	}

	//text to display on large buff icons in the desktop UI
	public String iconTextDisplay(){
		return "";
	}

	//visual effect usually attached to the sprite of the character the buff is attacked to
	public void fx(boolean on) {
		//do nothing by default
	}

	public String heroMessage(){
		String msg = Messages.get(this, "heromsg");
		if (msg.isEmpty()) {
			return null;
		} else {
			return msg;
		}
	}

	public String name() {
		return Messages.get(this, "name");
	}

	public String desc(){
		return Messages.get(this, "desc");
	}

	//to handle the common case of showing how many turns are remaining in a buff description.
	protected String dispTurns(float input){
		return Messages.decimalFormat("#.##", input);
	}

	//buffs act after the hero, so it is often useful to use cooldown+1 when display buff time remaining
	public float visualcooldown(){
		return cooldown()+1f;
	}

	private static final String MNEMONIC_EXTENDED = "mnemonic_extended";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (mnemonicExtended) bundle.put(MNEMONIC_EXTENDED, mnemonicExtended);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains(MNEMONIC_EXTENDED)) {
			mnemonicExtended = bundle.getBoolean(MNEMONIC_EXTENDED);
		}
	}

	private static final Map<Class<?>, LinkedHashSet<Class<? extends Buff>>> REIFIED_LEAF_CACHE = new HashMap<>();

	public static boolean classMatches(Class<?> candidate, Class<?> effect) {
		if (candidate == null || effect == null) {
			return false;
		}
		if (candidate.isAssignableFrom(effect)) {
			return true;
		}
		for (Class<? extends Buff> childClass : reifiedLeafClassesOf(candidate)) {
			if (childClass.isAssignableFrom(effect)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public static LinkedHashSet<Class<? extends Buff>> reifiedLeafClassesOf(Class<?> buffClass) {
		LinkedHashSet<Class<? extends Buff>> cached = REIFIED_LEAF_CACHE.get(buffClass);
		if (cached != null) {
			return new LinkedHashSet<>(cached);
		}

		// Flatten nested composite buffs so resist/immune logic can reason over leaf effects.
		LinkedHashSet<Class<? extends Buff>> result = new LinkedHashSet<>();
		if (buffClass != null && Buff.class.isAssignableFrom(buffClass)) {
			Buff buff = Reflection.newInstance((Class<? extends Buff>) buffClass);
			if (buff != null) {
				for (Class<? extends Buff> childClass : buff.reifiesTo()) {
					LinkedHashSet<Class<? extends Buff>> nested = reifiedLeafClassesOf(childClass);
					if (nested.isEmpty()) {
						result.add(childClass);
					} else {
						result.addAll(nested);
					}
				}
			}
		}

		REIFIED_LEAF_CACHE.put(buffClass, new LinkedHashSet<>(result));
		return result;
	}

	//creates a fresh instance of the buff and attaches that, this allows duplication.
	public static<T extends Buff> T append( Char target, Class<T> buffClass ) {
		T buff = Reflection.newInstance(buffClass);
		buff.attachTo( target );
		return buff;
	}

	public static<T extends FlavourBuff> T append( Char target, Class<T> buffClass, float duration ) {
		T buff = append( target, buffClass );
		buff.spend( duration * buff.resistFactor(target) );
		return buff;
	}

	//same as append, but prevents duplication.
	public static<T extends Buff> T affect( Char target, Class<T> buffClass ) {
		T buff = target.buff( buffClass );
		if (buff != null) {
			return buff;
		} else {
			return append( target, buffClass );
		}
	}
	
	public static<T extends FlavourBuff> T affect( Char target, Class<T> buffClass, float duration ) {
		T buff = affect( target, buffClass );
		buff.spend( duration * buff.resistFactor(target) );
		return buff;
	}

	//postpones an already active buff, or creates & attaches a new buff and delays that.
	public static<T extends FlavourBuff> T prolong( Char target, Class<T> buffClass, float duration ) {
		T buff = affect( target, buffClass );
		buff.postpone( duration * buff.resistFactor(target) );
		return buff;
	}

	public static<T extends CounterBuff> T count( Char target, Class<T> buffclass, float count ) {
		T buff = affect( target, buffclass );
		buff.countUp( count );
		return buff;
	}
	
	public static void detach( Char target, Class<? extends Buff> cl ) {
		for ( Buff b : target.buffs( cl )){
			b.detach();
		}
	}
}
