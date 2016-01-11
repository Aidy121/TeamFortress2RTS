// Author: Aidan Fisher

import java.awt.Rectangle;
import java.io.Serializable;

public class ControlPoint extends Entity implements Serializable {
	private static final long serialVersionUID = -6232864999078160124L;

	public static final int CAP_PROGRESS_REQUIRED = 10000;

	public int whoCapping = -1;
	public int capProgress = 0;
	public Rectangle capZone;

	public ControlPoint(double x, double y, double z, int width, int length) {
		super(x, y, z, -1);
		this.width = width;
		this.length = length;
		this.effectedByGravity = false;
	}

	public void tick() {
		if (this.team != -1) {
			Game.game.ticksLeftToWin[team]--;
			if (Game.game.ticksLeftToWin[team] <= 0) {
				return;
			}
		}

		int team = -1;
		int count = 0;
		for (int t = 0; t < Game.game.units.size(); t++) {
			for (int u = 0; u < Game.game.units.get(t).size(); u++) {
				if (Game.game.units.get(t).get(u).getClass() == Unit.class) {
					Unit other = (Unit) Game.game.units.get(t).get(u);
					if (other.x > capZone.x && other.y > capZone.y && other.x < capZone.x + capZone.width && other.y < capZone.y + capZone.height) {
						if (team == -1) {
							team = t;
							count++;
							if (other.classNumber == 1) {
								count++; // Scout has double cap.
							}
						} else if (team == t) {
							count++;
							if (other.classNumber == 1) {
								count++; // Scout has double cap.
							}
						} else if (team != t) {
							team = -1; // Contested.
							count = 0;
						}
					}
				}
			}
		}

		if (whoCapping == team || capProgress == 0) {
			whoCapping = team;
			if (count == 1) {
				capProgress += 6; // 16.7s
			} else if (count == 2) {
				capProgress += 9; // 11.1s
			} else if (count == 3) {
				capProgress += 11; // 9.1s
			} else if (count >= 4) {
				capProgress += 12; // 8.3s
			}
			if (capProgress > CAP_PROGRESS_REQUIRED) {
				capProgress = 0;
				this.team = whoCapping;
				whoCapping = -1;
			}
		} else {
			// Decay:
			capProgress -= 3; // 33.3s 
			if (capProgress < 0) {
				capProgress = 0;
			}
		}
	}
}
