// Author: Aidan Fisher

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.io.Serializable;

public class HealingBeam extends Weapon implements Serializable {

	private static final long serialVersionUID = 5281447600197225348L;

	public static int UBER_FLASH_TIME = 5; // Needs to be more than one.

	public static float MAX_HEALING_DISTANCE = 22f;

	public boolean ubering = false;

	public short unitHealing = Short.MAX_VALUE;

	public HealingBeam(Unit parent) {
		super(parent);
	}

	public void tick() {
		Unit parent = (Unit) Game.game.getEntity(this.parent);
		if (ubering) {
			parent.uber = UBER_FLASH_TIME;
		}
		if (unitHealing != Short.MAX_VALUE) {
			if (Game.game.getEntity(unitHealing).distancexy(parent) > MAX_HEALING_DISTANCE) {
				unitHealing = Short.MAX_VALUE;
			} else {
				Unit healingUnit = (Unit) Game.game.getEntity(unitHealing);
				healingUnit.damage(-0.24, true); // This could be increased to consider out of combat healing.
				// Gain ubercharge:
				if (!ubering) {
					parent.metal += 2;
					if (parent.metal > 8000) {
						parent.metal = 8000;
					}
				} else {
					parent.metal -= 10;
					if (parent.metal < 0) {
						parent.metal = 0;
						ubering = false;
					}
					healingUnit.uber = UBER_FLASH_TIME;
				}
			}
		} else if (ubering) {
			// Still use uber:
			parent.metal -= 10;
			if (parent.metal < 0) {
				parent.metal = 0;
				ubering = false;
			}
		}
		if (unitHealing == Short.MAX_VALUE || Game.game.currentTick % 100 == 0) { // Can switch every 1 second.
			Unit worstPercentHealthFriendlyUnit = null;
			double worstPercentHealth = 1.6;
			for (Entity friendly : Game.game.units.get(parent.team)) {
				if (friendly.getClass() == Unit.class) {
					Unit friendlyUnit = (Unit) friendly;
					double percentHealth = friendlyUnit.health / friendlyUnit.maxHealth;
					if (parent.id != friendlyUnit.id && percentHealth < worstPercentHealth && parent.distancexy(friendlyUnit) < MAX_HEALING_DISTANCE) {
						worstPercentHealthFriendlyUnit = friendlyUnit;
						worstPercentHealth = percentHealth;
					}
				}
			}

			if (worstPercentHealthFriendlyUnit != null) {
				unitHealing = worstPercentHealthFriendlyUnit.id;
			}
		}
	}
}
