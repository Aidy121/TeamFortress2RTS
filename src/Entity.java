// Author: Aidan Fisher

import java.awt.*;
import java.util.*;
import java.awt.geom.*;
import java.io.Serializable;

public class Entity implements Comparable<Entity>, Serializable {

	public static final double SMALLEST_DISTANCE = 0.001;

	private static final long serialVersionUID = 7979875947947389831L;

	public static short nextID = Short.MIN_VALUE;

	public final static int BLUE = 0;
	public final static int RED = 1;

	public short id;

	// This location is the center bottom of the player. Players are cylinders.
	public double x;
	public double y;
	public double z;

	public transient double dX;
	public transient double dY;
	public transient double dZ;

	public transient double oX;
	public transient double oY;
	public transient double oZ;

	public double interpX;
	public double interpY;
	public double interpZ;

	public double rotation; // xy rot
	public double upDownRotation; // up/down rot: From -Math.PI/2 to +Math.PI/2. 0 is horizontal.
	public double width;
	public double length;
	public double height;

	public boolean onGround = false;
	public transient boolean isCylinder = false;

	public int team;

	public int dieTime = -1;

	public boolean effectedByGravity = true;

	public Entity(double x, double y, double z, int team) {
		this.x = x;
		this.y = y;
		this.z = z; // Ground, for now.
		this.dX = 0;
		this.dY = 0;
		this.dZ = 0;
		this.team = team;
		this.id = nextID++;
	}

	// It should be noted that the game only handles 2 teams right now.
	public int getEnemy() {
		if (this.team == 0) {
			return 1;
		} else {
			return 0;
		}
	}

	public void beforeTick() {
		this.oX = this.x;
		this.oY = this.y;
		this.oZ = this.z;
	}

	public void afterTick() {
		this.interpX = this.x - this.oX;
		this.interpY = this.y - this.oY;
		this.interpZ = this.z - this.oZ;
	}

	public void tick() {
		onGround = false;
		if (dieTime > 0) {
			dieTime--;
		}

		move(dX, dY, dZ);

		if (effectedByGravity) {
			this.dZ -= Game.GRAVITY;
		}
		if (this.z <= Game.game.map.getMinZ(x, y)) {
			// collision. [Grenades just roll]
			this.z = Game.game.map.getMinZ(x, y);
			onGround = true;
			if (effectedByGravity) {
				if (this.getClass() == Bullet.class) {
					this.dZ = -this.dZ * 0.8;
					((Bullet) this).speed *= 0.75; // Cripples the speed of it.
					this.dX = Math.cos(rotation) * ((Bullet) this).speed * Math.cos(upDownRotation);
					this.dY = Math.sin(rotation) * ((Bullet) this).speed * Math.cos(upDownRotation);
					((Bullet) this).fullDamage *= 0.85; // Up to 9/10 the damage each bounce as well. (Bonus to direct hit, essentially)
				} else {
					this.dZ = 0;
				}
			} else { // Collide
				die();
			}
		}
	}

	public void move(double dX, double dY, double dZ) {
		this.x += dX;
		this.y += dY;
		this.z += dZ;
		if (this.getClass() == Unit.class) {
			wallCollision(dX, dY);
		} else if (this.getClass() == Bullet.class && effectedByGravity) {
			// This indicates its a bouncing bullet, so it doesn't die on contact. (Its a grenade)
			bounceWallCollision(); // ALWAYS is moved by dX / dY.
		}
	}

	// Overrided, generally.
	public void die() {
		if (this.dieTime < 0) {
			this.dieTime = 0;
			if (this.getClass() == Bullet.class) {
				this.dieTime = -1; // Die is taken by bullet method. Overrided, essentially.
				((Bullet) this).die(null, ((Bullet) this).splashDistance != 0);
			}
		}
	}

	// In x / y coordinate plane
	public double distancexy(Entity other) {
		return Math.sqrt((this.x - other.x) * (this.x - other.x) + (this.y - other.y) * (this.y - other.y));
	}

	// Uses center of mass:
	public double getDistance(Entity other) {
		return Math.sqrt((this.x - other.x) * (this.x - other.x) + (this.y - other.y) * (this.y - other.y) + (this.z + this.height / 2 - other.z - other.height / 2)
				* (this.z + this.height / 2 - other.z - other.height / 2));
	}

	public double distanceFromGrid(double thisX, double thisY, int gridX, int gridy) {
		double x = Math.min(Math.abs(thisX - gridX), Math.abs(thisX - (gridX + 1)));
		double y = Math.min(Math.abs(thisX - gridX), Math.abs(thisX - (gridX + 1)));
		return Math.sqrt(x * x + y * y);
	}

	// The unit has already moved the dx and dy!
	public void wallCollision(double dX, double dY) {
		// (*Unit only currently)
		// Test if in "No pathfinding":
		// If so, Find closest point out of "No pathfinding":
		if (Game.game.map.blockedForPathfinding((int) this.x, (int) this.y)) { // x && y will be positive.
			// The units can *never* be in this zone.
			// However, the checks are done in a cross pattern: 
			double fX = -1;
			double fY = -1;
			for (int range = 0; range < 40; range++) {
				double bestDist = Double.MAX_VALUE;
				double distance = distanceFromGrid(this.x, this.y, (int) this.x + range, (int) this.y);
				if (!Game.game.map.blockedForPathfinding((int) this.x + range, (int) this.y) && distance < bestDist) {
					bestDist = distance;
					fX = (int) this.x + range;
					fY = this.y;
				}
				distance = distanceFromGrid(this.x, this.y, (int) this.x - range, (int) this.y);
				if (!Game.game.map.blockedForPathfinding((int) this.x - range, (int) this.y) && distance < bestDist) {
					bestDist = distance;
					fX = (int) this.x - range + 1;
					fY = this.y;
				}
				distance = distanceFromGrid(this.x, this.y, (int) this.x, (int) this.y + range);
				if (!Game.game.map.blockedForPathfinding((int) this.x, (int) this.y + range) && distance < bestDist) {
					bestDist = distance;
					fX = this.x;
					fY = (int) this.y + range;
				}
				distance = distanceFromGrid(this.x, this.y, (int) this.x, (int) this.y - range);
				if (!Game.game.map.blockedForPathfinding((int) this.x, (int) this.y - range) && distance < bestDist) {
					bestDist = distance;
					fX = this.x;
					fY = (int) this.y - range + 1;
				}
				if (bestDist != Double.MAX_VALUE) {
					break;
				}
			}
			if (fX != -1 || fY != -1) {
				this.x = fX;
				this.y = fY;
			}
		}
	}

	// The entity has already moved the dx and dy.
	public void bounceWallCollision() {
		// Tests the rectangle against the walls in the game.
		// Essentially if the z is too low, for its current dX / dY, it flips direction.
		int deltaX = (int) this.x - (int) (this.x - dX); // Assumed to be 1 or -1. (or 0, of course)
		int deltaY = (int) this.y - (int) (this.y - dY);
		if (Game.game.map.getMinZ(this.x, this.y) > this.z - dZ) { // [Currently grenades can't go up at all.]
			boolean xDir = true;
			boolean yDir = true;
			if (deltaX != 0 && deltaY != 0) {
				if (Game.game.map.getMinZ(this.x, this.y - deltaY) <= this.z - dZ) {
					xDir = false;
				} else if (Game.game.map.getMinZ(this.x - deltaX, this.y) <= this.z - dZ) {
					yDir = false;
				}
			}
			// Bounce: [If very close to corner, it will essentially bounce off the diagonal]
			if (deltaX != 0 && xDir) {
				int wallX = Math.max((int) this.x, (int) (this.x - dX));
				// Set new x:
				this.x = 2 * wallX - this.x;
				// Flip the dX:
				dX = -dX;
			}
			if (deltaY != 0 && yDir) {
				int wallY = Math.max((int) this.y, (int) (this.y - dY));
				// Set new y:
				this.y = 2 * wallY - this.y;
				// Flip the dY:
				dY = -dY;
			}
			// Recalculate rotation:
			this.rotation = Math.atan2(dY, dX);
		}
	}

	public boolean contains(Point.Double point, double z) {
		if (this.isCylinder) {
			if (z >= this.z && z <= this.z + height) {
				if ((point.x - this.x) * (point.x - this.x) + (point.y - this.y) * (point.y - this.y) <= (this.width / 2) * (this.width / 2)) {
					return true;
				}
			}
			return false;
		} else {
			double distance = Math.sqrt((point.x - (this.x)) * (point.x - (this.x)) + (point.y - (this.y)) * (point.y - (this.y)));

			Point.Double newPoint = new Point.Double(this.x + Math.cos(-rotation) * distance, this.y + Math.sin(-rotation) * distance); // New point on new coord system.
			return newPoint.x >= this.x - width / 2 && newPoint.y >= this.y - height / 2 && newPoint.x <= this.x + width / 2 && newPoint.y <= this.y + height && z >= this.z && z <= this.z + height;
		}
	}

	public void setRotation(double rotation, double upDownRotation) {
		this.rotation = rotation % (Math.PI * 2);
		if (this.rotation < 0) {
			this.rotation += (Math.PI * 2);
		}
		if (upDownRotation >= Math.PI / 2 - 0.001) {
			this.upDownRotation = Math.PI / 2 - 0.001;
		} else if (upDownRotation <= -Math.PI / 2 + 0.001) {
			this.upDownRotation = -Math.PI / 2 + 0.001;
		} else {
			this.upDownRotation = upDownRotation;
		}
	}

	public double getZValue() {
		double z = 0;
		if (this.getClass() == Explosion.class) {
			z += 10000;
		}
		z += this.z + this.height;
		return z;
	}

	public void render(Graphics2D g) {
	}

	@Override
	public int compareTo(Entity e) {
		if (getZValue() < e.getZValue()) {
			return -1;
		} else if (getZValue() > e.getZValue()) {
			return 1;
		} else {
			return 0;
		}
	}
}
