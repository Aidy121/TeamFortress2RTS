// Author: Aidan Fisher

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Explosion extends Entity implements Serializable {

	private static final long serialVersionUID = 3619998093427673624L;

	public static int ticksPerFrame = 3;

	public short stuckTo = Short.MAX_VALUE;

	public Explosion(Point.Double p, double z, int team, short stuckTo, double damage) {
		super(p.x, p.y, z, team);
		this.width = Math.pow(damage / 4.0, 0.25);
		this.length = Math.pow(damage / 4.0, 0.25);
		this.height = Math.pow(damage / 4.0, 0.25);

		if (stuckTo != Short.MAX_VALUE) {
			Entity entity = Game.game.getEntity(stuckTo);
			if (entity != null) {
				this.stuckTo = stuckTo;
				this.x = this.x - entity.x;
				this.y = this.y - entity.y;
				this.z = this.z - entity.z;
			}
		}
	}

	public void tick() {
		super.tick();
	}
}
