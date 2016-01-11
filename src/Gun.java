// Author: Aidan Fisher

import java.awt.Graphics2D;
import java.io.Serializable;

public class Gun extends Weapon implements Serializable {

	public final static int SNIPER_CHARGE_TIME = 20;
	public final static int SNIPER_PAUSE_TIME = 160;

	/** Add Muzzle Flash! */

	private static final long serialVersionUID = -5801926527271533028L;
	public static Gun revolver = new Gun(null, "Revolver", 80, 65, 1, Math.toRadians(1.5), 1.2, 60, 0, 1);
	public static Gun scatterGun = new Gun(null, "Scattergun", 135, 6, 20, Math.toRadians(15), 0.5, 35, 0, 1); // Note: 90 degrees is 180 degrees, essentially.
	public static Gun rocketLauncher = new Gun(null, "Rocket Launcher", 150, 80, 1, Math.toRadians(1), 0.2, 100, 5, 1);
	public static Gun grenadeLauncher = new Gun(null, "Grenade Launcher", 100, 80, 1, Math.toRadians(7), 0.5, 80, 4, 1);
	public static Gun miniGun = new Gun(null, "Minigun", 10, 10, 1, Math.toRadians(8), 0.5, 60, 0, 2);
	public static Gun sniperRifle = new Gun(null, "Sniper Rifle", 200, 100, 1, 0, 1.5, 200, 0, 1);
	public static Gun flameThrower = new Gun(null, "Flame Thrower", 2, 0.1, 6, Math.toRadians(12), 0.5, 11, 0, 2.4);
	public static Gun shotGun = new Gun(null, "Shotgun", 135, 5, 20, Math.toRadians(14), 0.5, 35, 0, 1); // Slightly less damage than the scattergun, a bit more accuracy.
	public static Gun sentryGunL1 = new Gun(null, "Sentry L1", 25, 16, 1, Math.toRadians(1), 1, 60, 0, 1);
	public static Gun sentryGunL2 = new Gun(null, "Sentry L2", 12, 16, 1, Math.toRadians(1), 1, 65, 0, 1);
	public static Gun sentryRockets = new Gun(null, "Sentry Rockets", 300, 80, 1, Math.toRadians(1), 0.2, 70, 4, 1);

	// Guns shoot projectiles; generally fast-> and *always* have tracers behind them [Projectiles can be "hitscan" weapons, "splash" damage: rockets, pills] 

	String gunName;

	double bulletSpeed;
	boolean tryingToFire = false;
	int fireRate; // ticks to fire
	int coolDown; // ticks until can fire again

	double maxMissAngle; // Will be between 0 and this angle.
	int numBullets;
	double damage;
	int distance;

	double forwardOffset;

	int maxDistance;
	double splashDistance;

	public Gun(Entity parent, String gunName, int fireRate, double damage, int numBullets, double maxMissAngle, double bulletSpeed, int maxDistance, double splashDistance, double forwardOffset) {
		super(parent);
		this.gunName = gunName;
		this.bulletSpeed = bulletSpeed;
		this.fireRate = fireRate;
		this.coolDown = fireRate; // Can't fire yet.
		this.damage = damage;
		this.numBullets = numBullets;
		this.maxDistance = maxDistance;
		this.maxMissAngle = maxMissAngle;
		this.splashDistance = splashDistance;
		this.forwardOffset = forwardOffset;
	}

	public void assignToAndCopy(Entity entity, boolean rockets) {
		if (entity.getClass() == Unit.class) {
			((Unit) entity).weapon = new Gun(entity, this.gunName, this.fireRate, this.damage, this.numBullets, this.maxMissAngle, this.bulletSpeed, this.maxDistance, this.splashDistance,
					this.forwardOffset);
		} else { // Must be building.
			if (!rockets) {
				((Building) entity).gun = new Gun(entity, this.gunName, this.fireRate, this.damage, this.numBullets, this.maxMissAngle, this.bulletSpeed, this.maxDistance, this.splashDistance,
						this.forwardOffset);
			} else {
				((Building) entity).rocketGun = new Gun(entity, this.gunName, this.fireRate, this.damage, this.numBullets, this.maxMissAngle, this.bulletSpeed, this.maxDistance, this.splashDistance,
						this.forwardOffset);
			}
		}
	}

	public void tick() {
		Entity parentEntity = Game.game.getEntity(parent);
		if (parentEntity.getClass() == Unit.class) {
			tryingToFire = ((Unit) parentEntity).engagedUnit != null;
		} else { // Must be building
			tryingToFire = ((Building) parentEntity).engagedUnit != null;
		}
		if (this.gunName.equals("Sniper Rifle")) {
			if (coolDown > Gun.SNIPER_CHARGE_TIME) {
				coolDown--;
			} else if (coolDown == Gun.SNIPER_CHARGE_TIME) {
				if (tryingToFire) {
					coolDown--;
				}
			} else if (coolDown > 0) {
				coolDown--;
			} else {
				// Fire anyways
				fire();
			}
		} else if (coolDown > 0) {
			coolDown--;
		} else {
			if (tryingToFire) {
				fire();
			}
		}
		if (this.gunName.equals("Minigun")) {
			Unit parentUnit = ((Unit) parentEntity);
			if (tryingToFire) {
				parentUnit.speed = parentUnit.normalSpeed * 0.47;
			} else {
				parentUnit.speed = parentUnit.normalSpeed;
			}
		}
	}

	public void fire() {
		Entity parent = Game.game.getEntity(this.parent);
		// Determine miss angle:
		for (int i = 0; i < numBullets; i++) {
			double missAngleHorizontal = Math.random() * maxMissAngle * 2 - maxMissAngle;
			double missAngleVertical = Math.random() * maxMissAngle * 2 - maxMissAngle;
			//double missAngleVertical = 0; // For now, at least.
			Bullet bullet;
			if (parent.getClass() == Unit.class) {
				bullet = new Bullet(parent.x + forwardOffset * Math.cos(parent.rotation), parent.y + forwardOffset * Math.sin(parent.rotation), parent.z + parent.height * 0.8, parent.team,
						parent.upDownRotation + missAngleVertical, parent.rotation + missAngleHorizontal, bulletSpeed, damage, maxDistance, splashDistance, gunName); // Oddly enough, projectiles don't travel faster when fired from moving targets..			
			} else {
				bullet = new Bullet(parent.x + forwardOffset * Math.cos(parent.rotation + ((Building) parent).sentryRotation), parent.y + forwardOffset
						* Math.sin(parent.rotation + ((Building) parent).sentryRotation), parent.z + parent.height * 0.8, parent.team, parent.upDownRotation + missAngleVertical, parent.rotation
						+ ((Building) parent).sentryRotation + missAngleHorizontal, bulletSpeed, damage, maxDistance, splashDistance, gunName); // Oddly enough, projectiles don't travel faster when fired from moving targets..

			}
			Game.game.addEntity(bullet, false);
		}
		coolDown = fireRate;
	}
}
