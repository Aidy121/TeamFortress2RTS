// Author: Aidan Fisher

import java.io.Serializable;
import java.util.ArrayList;

public class Building extends Entity implements Serializable {
	// Building and Unit do not share the same class as to set a "forced" distinction between them in code. This does not cause a performance dropoff.

	private static final long serialVersionUID = -1082224572906664999L;

	public static final float DISPENSER_HEAL_DIST = 7f;
	public static final float REPAIR_DIST = 6f;
	public static final float MAX_SENTRY_DIST = 50f; // Fairly far.

	public double sentryRotation; // Only used for sentries.
	public boolean rotationDirection;
	public Gun gun;
	public Gun rocketGun;

	public byte type;
	public int level;

	public int upgradeMetal = 0;

	public double health;
	public int maxHealth;

	public transient Entity engagedUnit = null; // Can engage another sentry, of course.

	public Building(double x, double y, double z, double rotation, byte type, int team) {
		super(x, y, z, team);
		this.rotation = rotation;
		this.type = type;
		this.level = 0;
		if (this.type == PathfindingNode.DISPENSER_BUILD) {
			setOriginalHealth(150);
			this.width = 1.8;
			this.length = 1.8;
			this.height = 4;
		} else if (this.type == PathfindingNode.SENTRY_BUILD) {
			setOriginalHealth(150);
			this.width = 2.2;
			this.length = 2.2;
			this.height = 5;
			Gun.sentryGunL1.assignToAndCopy(this, false);
		}
		this.isCylinder = true;
		this.upDownRotation = 0;
		this.sentryRotation = 0;
	}

	public int wrenchHit(int amount, int maxMetal) {
		if (amount > maxMetal) {
			amount = maxMetal;
		}
		// Check if damaged first:
		int total = 0;
		if (health < maxHealth - amount * 5) {
			health += (amount * 5);
			return amount;
		} else if (health < maxHealth) {
			total += (maxHealth / health) / 5;
			amount -= (maxHealth / health) / 5;
			health = maxHealth;
		}

		upgradeMetal += amount;
		if (upgradeMetal >= 200) {
			level++;
			if (type == PathfindingNode.SENTRY_BUILD) {
				if (level == 1) {
					setOriginalHealth(180);
					Gun.sentryGunL2.assignToAndCopy(this, false);
				} else if (level == 2) {
					setOriginalHealth(216);
					Gun.sentryRockets.assignToAndCopy(this, true);
				}
			} else if (type == PathfindingNode.DISPENSER_BUILD) {
				if (level == 1) {
					setOriginalHealth(180);
				} else if (level == 2) {
					setOriginalHealth(216);
				}
			}
			int extra = upgradeMetal - 200;
			upgradeMetal = 0;
			return total + amount - extra;
		}
		return total + amount;
	}

	public void sentryAI() {
		// Sentries appear to have linear looking movement
		if (Game.game.currentTick % 15 == 0) {
			engagedUnit = null;
			detectUnitEngagement(MAX_SENTRY_DIST);
		}

		if (engagedUnit != null) {
			lookTowardsPoint(engagedUnit.x, engagedUnit.y, engagedUnit.z + engagedUnit.height / 2);
		} else {
			// Turn:
			if (rotationDirection) {
				setSentryRotation(this.sentryRotation + 0.005);
				if (this.sentryRotation + 0.005 > Math.PI / 4 && this.sentryRotation < Math.PI / 4) {
					rotationDirection = !rotationDirection;
				}
			} else {
				setSentryRotation(this.sentryRotation - 0.005);
				if (this.sentryRotation - 0.005 < 7 * Math.PI / 4 && this.sentryRotation > 7 * Math.PI / 4) {
					rotationDirection = !rotationDirection;
				}
			}
		}
	}

	public void setSentryRotation(double sentryRotation) {
		this.sentryRotation = sentryRotation % (Math.PI * 2);
		if (this.sentryRotation < 0) {
			this.sentryRotation += (Math.PI * 2);
		}
	}

	public void lookTowardsPoint(double lX, double lY, double lZ) {
		// For now, sentries can't look up / down.
		double thisTotalRot = rotation + sentryRotation;
		if (thisTotalRot >= Math.PI * 2) {
			thisTotalRot -= Math.PI * 2;
		}

		double angle = Math.atan2(lY - this.y, lX - this.x);
		double angleDifference = Math.min(Math.abs(thisTotalRot - angle), Math.PI * 2 - Math.abs(thisTotalRot - angle));
		double newRot;
		if ((angle - thisTotalRot > 0 && angle - thisTotalRot <= Math.PI) || angle - thisTotalRot <= -Math.PI) {
			newRot = sentryRotation + 0.01;
			if (angleDifference < 0.01) {
				newRot = angle - rotation;
			}
		} else {
			newRot = sentryRotation - 0.01;
			if (angleDifference < 0.01) {
				newRot = angle - rotation;
			}
		}
		setSentryRotation(newRot);
	}

	public void detectUnitEngagement(double maxDistance) {
		// Goes through every enemy unit, and determines if engagable:
		Entity closestEnemyUnit = null;
		double bestDistance = maxDistance;
		for (Entity enemy : Game.game.units.get(getEnemy())) {
			Entity enemyUnit = enemy;
			if (this.distancexy(enemyUnit) < bestDistance) {
				if (new PathfindingNode(x, y, Game.game.map).lineTo(new PathfindingNode(enemyUnit.x, enemyUnit.y, Game.game.map), false) >= 0) {
					closestEnemyUnit = enemyUnit;
					bestDistance = this.distancexy(enemyUnit);
				}
			}
		}

		if (closestEnemyUnit != null) {
			engagedUnit = closestEnemyUnit;
		}
	}

	public void tick() {

		if (type == PathfindingNode.DISPENSER_BUILD) {
			if (Game.game.currentTick % 10 == 0) {
				double healthGive = 1;
				if (level == 1) {
					healthGive = 1.5;
				} else if (level == 2) {
					healthGive = 2;
				}
				give(healthGive, 0);
			}
			if (Game.game.currentTick % 250 == 0) {
				int metalGive = 20;
				if (level == 1) {
					metalGive = 25;
				} else if (level == 2) {
					metalGive = 30;
				}
				give(0, metalGive);
			}
		} else if (type == PathfindingNode.SENTRY_BUILD) {
			sentryAI();
			gun.tick();
			if (level == 2) {
				rocketGun.tick();
			}
		}
		collisionCheck();
		super.tick();
	}

	// Must never do health & metal at the same time.
	public void give(double health, int metal) {
		ArrayList<Unit> unitsInRange = new ArrayList<Unit>();
		for (int t = 0; t < Game.game.units.size(); t++) {
			for (int u = 0; u < Game.game.units.get(t).size(); u++) {
				if (Game.game.units.get(t).get(u).getClass() == Unit.class) { // Buildings handle building - unit collision.
					Unit other = (Unit) Game.game.units.get(t).get(u);
					if (this.distancexy(other) < DISPENSER_HEAL_DIST) {
						if ((health > 0 && other.health < other.maxHealth) || (metal > 0 && other.classNumber == 6 && other.metal < 200)) {
							unitsInRange.add(other);
						}
					}
				}
			}
		}

		for (int i = 0; i < unitsInRange.size(); i++) {
			unitsInRange.get(i).damage(-health, false); // Health can be givent to any # of players.
			unitsInRange.get(i).giveMetal(metal / unitsInRange.size());
		}
	}

	public void die() {
		// Time until death = 3
		if (dieTime < 0) {
			dieTime = 3;
		}
	}

	public void damage(double amount) {
		health -= amount;
		if (health <= 0) {
			die();
		} else if (health > maxHealth) {
			health = maxHealth;
		}
	}

	public void setOriginalHealth(int amount) {
		this.maxHealth = amount;
		this.health = amount;
	}

	// Buildings do not collide with buildings:
	public void collisionCheck() {
		// All collidables are cylinders
		for (int t = 0; t < Game.game.units.size(); t++) {
			for (int u = 0; u < Game.game.units.get(t).size(); u++) {
				if (Game.game.units.get(t).get(u) == this) { // Other units in the list will do this collision. (Note this can have a failure if a unit is added / removed while the unit ticks are happening)
					return;
				} else if (Game.game.units.get(t).get(u).getClass() == Unit.class) { // Buildings handle building - unit collision.
					int collideReducer = 1;
					if (this.team == Game.game.units.get(t).get(u).team) {
						collideReducer = 64;
					}
					double distance = Game.game.units.get(t).get(u).distancexy(this);
					double both = (width + Game.game.units.get(t).get(u).width) * 0.65;
					if (Game.game.units.get(t).get(u).z <= this.z + this.height && Game.game.units.get(t).get(u).z + Game.game.units.get(t).get(u).height >= this.z && distance < both) {
						// Collide:
						collidedWithUnit(both, distance, collideReducer, t, u);
					}
				}
			}
		}
	}

	public void collidedWithUnit(double both, double distance, int collideReducer, int t, int u) {
		double rotationBetween = Math.atan2(this.y - Game.game.units.get(t).get(u).y, this.x - Game.game.units.get(t).get(u).x);
		Game.game.units.get(t).get(u).x -= Math.cos(rotationBetween) * (both - distance) / (collideReducer);
		Game.game.units.get(t).get(u).y -= Math.sin(rotationBetween) * (both - distance) / (collideReducer);
	}
}
