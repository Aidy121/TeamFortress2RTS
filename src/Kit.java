// Author: Aidan Fisher

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;

public class Kit extends Entity implements Serializable {
	private static final long serialVersionUID = 8108685360484118074L;

	public final static int RESPAWN_TIMER = 1000;

	public final static Color[] COLOR_CODES = { new Color(0, 60, 60), new Color(0, 120, 60), new Color(0, 200, 60), new Color(0, 120, 170), new Color(0, 170, 170), new Color(0, 255, 170) };
	public final static int[] METAL_CONTAINED = { 0, 0, 0, 40, 100, 200 };
	public final static int[] HEALTH_CONTAINED = { 20, 50, 100, 0, 0, 0 }; // Percent.

	public int type; // 0, 1, 2, 3, 4, 5. Can also do stuff like resupply lockers in the future.

	public int respawnTimer = -1; // Currently spawned == -1

	public Kit(double x, double y, double z, int type) {
		super(x, y, z, 0); // Not apart of either team.
		// All the same size:
		this.type = type;
		this.width = 3;
		this.length = 3;
		this.height = 3;
		this.effectedByGravity = false;
	}

	// Rotate for effect?
	public void tick() {

		// Last to happen in tick: (Return statements)
		if (respawnTimer == -1) {
			// Detect nearby players: (Similar to buildings)
			for (int t = 0; t < Game.game.units.size(); t++) {
				for (int u = 0; u < Game.game.units.get(t).size(); u++) {
					if (Game.game.units.get(t).get(u).getClass() == Unit.class) { // Buildings handle building - unit collision.
						Unit unit = (Unit) Game.game.units.get(t).get(u);
						if (this.distancexy(unit) < (this.width + unit.width) * 0.5) {
							if (this.type <= 2 && unit.health < unit.maxHealth) {
								respawnTimer = RESPAWN_TIMER;
								unit.damage(-(HEALTH_CONTAINED[this.type] * unit.maxHealth / 100.0), false);
								return;
							} else if (this.type >= 3 && unit.classNumber == 6 && unit.metal < 200) {
								respawnTimer = RESPAWN_TIMER;
								unit.giveMetal(METAL_CONTAINED[this.type]);
								return;
							}
						}
					}
				}
			}
		} else {
			respawnTimer--;
		}
	}
}
