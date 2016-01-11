// Author: Aidan Fisher

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class Bullet extends Entity implements Serializable {

	public final static int AFTER_BURN_TICKS = 1000;

	private static final long serialVersionUID = 3185021257460712779L;
	double speed;
	double distance;
	int maxDistance;
	double fullDamage; // Check all that use this when implementing damage fall off!
	double splashDistance;
	boolean particle;

	String bulletImage;

	public Bullet(double x, double y, double z, int team, double upDownRotation, double rotation, double speed, double fullDamage, int maxDistance, double splashDistance, String gunName) {
		super(x, y, z, team);
		this.effectedByGravity = false;
		this.particle = false;
		if (gunName.equals("Rocket Launcher") || gunName.equals("Sentry Rockets")) {
			this.width = 2;
			this.length = 0.6;
			this.height = 0.6;
			this.bulletImage = "Rocket.png";
		} else if (gunName.equals("Sniper Rifle")) {
			this.width = 20;
			this.length = 0.22;
			this.height = 0.22;
			this.bulletImage = "LightBulletSniper.png";
		} else if (gunName.equals("Flame Thrower")) {
			this.width = 1.5;
			this.length = 1.5;
			this.height = 1.5;
			this.bulletImage = "Fire.png";
			this.particle = true;
		} else if (gunName.equals("Grenade Launcher")) {
			this.width = 0.8;
			this.length = 0.6;
			this.height = 0.6;
			if (this.team == Entity.BLUE) {
				this.bulletImage = "BlueGrenade.png";
			} else if (this.team == Entity.RED) {
				this.bulletImage = "RedGrenade.png";
			}
			this.effectedByGravity = true;
		} else {
			this.width = 1.5;
			this.length = 0.21;
			this.height = 0.21;
			this.bulletImage = "LightBullet.png";
		}
		this.upDownRotation = upDownRotation;
		this.rotation = rotation;
		this.speed = speed;
		this.dX = Math.cos(rotation) * speed * Math.cos(upDownRotation);
		this.dY = Math.sin(rotation) * speed * Math.cos(upDownRotation);
		this.dZ = Math.sin(upDownRotation) * speed; // 0 = down, PI = up. 
		this.fullDamage = fullDamage;
		this.maxDistance = maxDistance;
		this.splashDistance = splashDistance;
	}

	public void tick() {
		super.tick();
		if (dieTime < 0) {
			distance += speed;
			if (distance > maxDistance) {
				if (this.effectedByGravity) {
					// Grenade (blow up)
					die(null, true);
				} else {
					dieTime = 0;
				}
			} else {
				collisionCheck();
			}
		}
	}

	public void wallCollision() {

	}

	public void die(Entity entityHit, boolean explosion) {
		if (dieTime < 0) {
			if (!explosion) {
				dieTime = 0;
				return;
			} else {
				dieTime = 0; // Explosions are separate from bullets.
			}
			Explosion e;
			if (entityHit != null) {
				e = new Explosion(this.getFrontPoint(), this.z, this.team, entityHit.id, this.fullDamage);
			} else {
				e = new Explosion(this.getFrontPoint(), this.z, this.team, Short.MAX_VALUE, this.fullDamage);
			}
			e.dieTime = 16 * Explosion.ticksPerFrame - 1;
			Game.game.addEntity(e, false);
			if (splashDistance > 0) {
				for (Entity entity : Game.game.entities) {
					if (entity.getClass() == Unit.class) {
						double distance = entity.getDistance(this);
						if (distance < splashDistance) {
							if (distance > 1) {
								double dRatio = 0.07;
								entity.dX = ((entity.x - this.x) / distance) * dRatio * 3;
								entity.dY = ((entity.y - this.y) / distance) * dRatio * 3;
								entity.dZ = ((entity.z + entity.height - this.z - height) / distance) * dRatio;
							}
							if (entityHit != entity) {
								// Not direct hit.
								((Unit) entity).damage(fullDamage / 2.0, false);
							}
						}
					} else if (entity.getClass() == Building.class) {
						double distance = entity.getDistance(this);
						if (distance < splashDistance) {
							((Building) entity).damage(fullDamage / 2.0);
						}
					}
				}
			}
		}
	}

	public void collisionCheck() {
		// All collidables are units right now.
		// Only enemies can be collided with.
		for (int i = 0; i < Game.game.units.get(getEnemy()).size(); i++) {
			Entity enemyObject = Game.game.units.get(getEnemy()).get(i);
			if (enemyObject.contains(this.getFrontPoint(), this.z)) {
				if (this.particle) {
					if (enemyObject.getClass() == Unit.class) {
						Unit enemyUnit = (Unit) enemyObject;
						enemyUnit.damage(fullDamage, false);
						// Set on fire:
						if (enemyUnit.classNumber != 3) { // And not ubered.
							enemyUnit.fireTicksLeft = AFTER_BURN_TICKS;
						}
					} else { // Must be building.
						((Building) enemyObject).damage(fullDamage); // Can't go on fire.
					}
				} else {
					if (enemyObject.getClass() == Unit.class) {
						((Unit) enemyObject).damage(fullDamage, false);
					} else {
						((Building) enemyObject).damage(fullDamage);
					}
					this.die(enemyObject, true);
					return;
				}
			}
		}
	}

	public Point.Double getFrontPoint() {
		return new Point.Double(this.x, this.y);// bullet should be facing the way it is going.
	}
}
