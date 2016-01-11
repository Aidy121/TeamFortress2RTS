// Author: Aidan Fisher

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

public class Unit extends Entity implements Serializable {

	private static final long serialVersionUID = -9046331796902423546L;

	public static float IDEAL_DISTANCE = 5f; // Different for different classes. 

	public static float HEAD_HEIGHT = 0.8f;

	public static final double AFTER_BURN_DAMAGE = 0.06;

	public transient double normalSpeed = 0.1; // Used for speed modifiers from weapons.
	public double speed = 0.1;
	public int maxHealth = 125;
	public double health = 125;

	public int metal = 200; // Only used for engineer (and medic)
	public int uber = -1;
	public int fireTicksLeft = 0;

	public short classNumber;
	public transient int unitNumber; // Assigned to keep track of the unit's death so the unit can respawn.

	public float engageDistance = 24.5f;

	public transient int timeSinceLastStrafe = 0;

	public transient int[] movementKeys = { 0, 0 };

	public transient double randomLookingMultiplier = 0;

	public transient Entity engagedUnit = null; // Transient, so its okay.
	public transient Entity wantedTargetUnit = null;
	public transient int ticksLeft = 0; // For wanted target unit.

	public Weapon weapon;

	public ArrayList<PathfindingNode> currentPath = new ArrayList<PathfindingNode>(); // Generally just 1 node. Last node is goal location. First node is location going to right now.

	// Current path forces the ai to walk in a relatively straight line to its destination. It will not look around while travelling, but will do checks at corners & when it finishes. 
	// It takes priority over preferredLocation, and the last node is set to the prefferedLocation. While travelling, the bot will engage with other enemies, but unlike preferred location,
	// will give a *much* larger priority to moving towards its final destination. Preferred Location almost doesn't matter when the unit is engaged.

	//public Point.Double[] preferredLocation;

	public Unit(double x, double y, double z, double rotation, int team, int unitNumber, String className) {
		super(x, y, z, team);
		classNumber = 1; // default.
		this.unitNumber = unitNumber;
		if (className.equals("Scout")) {
			Gun.scatterGun.assignToAndCopy(this, false);
			normalSpeed = 0.133;
			speed = 0.133;
			setOriginalHealth(125);
			classNumber = 1;
			this.width = 2.2;
			this.length = 2.2;
			engageDistance = 30f;
		} else if (className.equals("Soldier")) {
			Gun.rocketLauncher.assignToAndCopy(this, false);
			normalSpeed = 0.08;
			speed = 0.08;
			setOriginalHealth(200);
			classNumber = 2;
			this.width = 2.8;
			this.length = 2.8;
			engageDistance = 80f;
		} else if (className.equals("Medic")) {
			this.weapon = new HealingBeam(this);
			normalSpeed = 0.107;
			speed = 0.107;
			setOriginalHealth(150);
			classNumber = 7;
			this.width = 2.5;
			this.length = 2.5;
			engageDistance = 12f;
			this.metal = 0; // Ubercharge.
		} else if (className.equals("Heavy")) {
			Gun.miniGun.assignToAndCopy(this, false);
			normalSpeed = 0.077;
			speed = 0.077;
			setOriginalHealth(300);
			classNumber = 5;
			this.width = 3.1;
			this.length = 3.1;
			engageDistance = 60f;
		} else if (className.equals("Sniper")) {
			Gun.sniperRifle.assignToAndCopy(this, false);
			normalSpeed = 0.1;
			speed = 0.1;
			setOriginalHealth(125);
			classNumber = 8;
			this.width = 2.4;
			this.length = 2.4;
			engageDistance = 140f;
		} else if (className.equals("Pyro")) {
			Gun.flameThrower.assignToAndCopy(this, false);
			normalSpeed = 0.1;
			speed = 0.1;
			setOriginalHealth(175);
			classNumber = 3;
			this.width = 2.6;
			this.length = 2.6;
			engageDistance = 15f;
		} else if (className.equals("Engineer")) {
			Gun.shotGun.assignToAndCopy(this, false);
			normalSpeed = 0.1;
			speed = 0.1;
			setOriginalHealth(125);
			classNumber = 6;
			this.width = 2.4;
			this.length = 2.4;
			engageDistance = 24f; // Less than the scout.
		} else if (className.equals("Demoman")) {
			Gun.grenadeLauncher.assignToAndCopy(this, false);
			normalSpeed = 0.093;
			speed = 0.093;
			setOriginalHealth(175);
			classNumber = 4;
			this.width = 2.5;
			this.length = 2.5;
			engageDistance = 50f;
		}
		this.height = 8;
		this.rotation = Math.random() * Math.PI * 2; // Should have a proper direction.
		this.isCylinder = true;
		this.rotation = rotation;
		this.upDownRotation = 0;
	}

	public void setOriginalHealth(int amount) {
		this.maxHealth = amount;
		this.health = amount;
	}

	// For stuff like building sentries / dispensers, or specific types of attacks.
	public void addOrder(byte order, double atX, double atY, boolean stacked) {
		if (classNumber == 6) { // Make sure its the engineer
			this.pathFind(atX, atY, atX, atY, stacked);
			// Set last to order:
			if (currentPath.size() != 0) {
				currentPath.get(currentPath.size() - 1).order = order;
			} else {
				// Add order to path
				pathFind(atX, atY, atX, atY, stacked);
				// Not ready for stacked
				if (currentPath.size() > 0) {
					currentPath.get(currentPath.size() - 1).order = order;
				}
			}
		}
	}

	public void targetedAI() {
		if (classNumber != 7) {
			if (ticksLeft > 0) {
				ticksLeft--;
				if (ticksLeft == 0) {
					wantedTargetUnit = null;
				}
			}
			if (wantedTargetUnit != null && Game.game.currentTick % 15 == 0 && this.distancexy(wantedTargetUnit) <= engageDistance) {
				if (new PathfindingNode(x, y, Game.game.map).lineTo(new PathfindingNode(wantedTargetUnit.x, wantedTargetUnit.y, Game.game.map), false) >= 0) {
					engagedUnit = wantedTargetUnit;
				}
			}
		}
	}

	/** Currently doesn't consider stacked. */
	public void setTarget(Entity enemy, boolean stacked) {
		wantedTargetUnit = enemy;
		ticksLeft = 1200; // 12 seconds.
		pathFind(enemy.x, enemy.y, enemy.x, enemy.y, stacked); // Doesn't care for formation.
	}

	/** Currently doesn't consider stacked. */
	public void pathFind(double cX, double cY, double x, double y, boolean stacked) {
		currentPath.clear();
		currentPath = Game.game.map.pathFind(cX, cY, this.x, this.y, x, y);
	}

	public void executeOrder(byte order) {
		if ((order == PathfindingNode.DISPENSER_BUILD && this.metal >= 100 && Game.game.dispenser[team][unitNumber] == null)
				|| (order == PathfindingNode.SENTRY_BUILD && this.metal >= 130 && Game.game.sentry[team][unitNumber] == null)) {
			Building building = new Building(this.x, this.y, this.z, this.rotation, order, this.team);
			for (int t = 0; t < Game.game.units.size(); t++) {
				for (int u = 0; u < Game.game.units.get(t).size(); u++) {
					if (Game.game.units.get(t).get(u).getClass() == Building.class) {
						if (Game.game.units.get(t).get(u).distancexy(building) < Game.game.units.get(t).get(u).width + building.width) {
							return;
						}
					}
				}
			}
			if (order == PathfindingNode.DISPENSER_BUILD) {
				this.metal -= 100;
				Game.game.dispenser[team][unitNumber] = building;
			} else if (order == PathfindingNode.SENTRY_BUILD) {
				this.metal -= 130;
				Game.game.sentry[team][unitNumber] = building;
			}
			Game.game.addEntity(building, true);
		}
	}

	// Update strafe pattern every 150ms.
	public void randomStrafe(int strafeChange, int wantToStayStill) {
		if (timeSinceLastStrafe > new Random().nextInt(strafeChange)) {
			timeSinceLastStrafe = 0;
			if (movementKeys[0] == -1) {
				movementKeys[0] = new Random().nextInt(wantToStayStill) == 0 ? 0 : 1;
			} else if (movementKeys[0] == 1) {
				movementKeys[0] = new Random().nextInt(wantToStayStill) == 0 ? 0 : -1;
			} else {
				if (new Random().nextInt(wantToStayStill * 2) == 0) { // Twice as less likely to stay still if already still.
					movementKeys[0] = 0;
				} else {
					movementKeys[0] = (int) ((new Random().nextInt(2) - 0.5) * 2.1); // Just move.
				}
			}
		}
	}

	public void uber(boolean stacked) {
		if (classNumber == 7 && this.metal == 8000 && !((HealingBeam) weapon).ubering) {
			((HealingBeam) weapon).ubering = true;
		}
	}

	public void engineerAI() {
		attackClassAI();

		// Upgrade / Repair dispenser / sentry:
		if (Game.game.currentTick % 80 == 0) {
			Building sentry = Game.game.sentry[team][unitNumber];
			Building dispenser = Game.game.dispenser[team][unitNumber];
			if (sentry != null && (sentry.level != 2 || sentry.health < sentry.maxHealth) && this.distancexy(sentry) < Building.REPAIR_DIST) {
				metal -= sentry.wrenchHit(25, metal);
			} else if (dispenser != null && (dispenser.level != 2 || dispenser.health < dispenser.maxHealth) && this.distancexy(dispenser) < Building.REPAIR_DIST
					&& (metal >= 155 || (sentry != null && this.metal >= 50))) { // Needs enough to build a sentry. [has at least 25 metal in reserve, because this is an RTS - sentry is more important]
				metal -= dispenser.wrenchHit(25, metal);
			}
		}
	}

	public void attackClassAI() {
		if (Game.game.currentTick % 15 == 0) {
			engagedUnit = null;
			detectUnitEngagement(engageDistance);
			timeSinceLastStrafe++;
			if (engagedUnit != null) {
				randomStrafe(12, 5);
			}
			movementKeys[1] = 0;
		}

		if (engagedUnit != null) {
			if (this.weapon.getClass() == Gun.class) {
				if (((Gun) this.weapon).gunName.equals("Rocket Launcher")) {
					lookTowardsPoint(engagedUnit.x, engagedUnit.y, engagedUnit.z + engagedUnit.height / 10);
				} else {
					lookTowardsPoint(engagedUnit.x, engagedUnit.y, engagedUnit.z + engagedUnit.height / 2);
				}
			}
		} else {
			movementKeys[0] = 0;
			movementKeys[1] = 0;
		}
	}

	public void sniperAI() {
		Gun sniper = (Gun) weapon;
		if (sniper.coolDown < Gun.SNIPER_CHARGE_TIME) {
			// This is more extreme than in the game, but there is a slow down, so this is not purely AI.
			speed = normalSpeed / 20;
		} else {
			speed = normalSpeed;
		}
		if (Game.game.currentTick % 15 == 0) {
			engagedUnit = null;
			detectUnitEngagement(engageDistance);
			timeSinceLastStrafe++;
			if (engagedUnit != null) {
				randomStrafe(12, 5);
			}
			movementKeys[1] = 0;
		}

		if (engagedUnit != null) {
			lookTowardsPoint(engagedUnit.x, engagedUnit.y, engagedUnit.z + engagedUnit.height / 2);
		} else {
			movementKeys[0] = 0;
			movementKeys[1] = 0;
		}
	}

	public void pyroAI() {
		attackClassAI();
		if (engagedUnit != null) {
			movementKeys[1] = 1; // WM1!
		}
	}

	public void medicAI() {
		HealingBeam medigun = (HealingBeam) weapon;
		if (Game.game.currentTick % 15 == 0) {
			engagedUnit = null;
			detectUnitEngagement(engageDistance);
			timeSinceLastStrafe++;
			if (medigun.unitHealing != Short.MAX_VALUE) {
				randomStrafe(12, 3);
			}
			movementKeys[1] = 0;
			if (engagedUnit != null) {
				// Run away. (This is overrided by orders)
				//double dir = Math.atan2(engagedUnit.y - this.y, engagedUnit.x - this.x);
				movementKeys[1] = -1;
			}
		}

		if (engagedUnit != null) {
			lookTowardsPoint(engagedUnit.x, engagedUnit.y, engagedUnit.z + engagedUnit.height / 2);
		} else if (medigun.unitHealing != Short.MAX_VALUE) {
			Unit unitHealing = (Unit) Game.game.getEntity(medigun.unitHealing);
			lookTowardsPoint(unitHealing.x, unitHealing.y, unitHealing.z + unitHealing.height / 2);
		} else {
			movementKeys[0] = 0;
			movementKeys[1] = 0;
		}
	}

	public void pathfindingAI() {
		if (currentPath.size() != 0) {
			if ((currentPath.get(0).x - this.x) * (currentPath.get(0).x - this.x) + (currentPath.get(0).y - this.y) * (currentPath.get(0).y - this.y) < 0.25) { // 0.25 is 0.5^2
				// Do order:
				this.executeOrder(currentPath.get(0).order);
				currentPath.remove(0);
			}
			if (currentPath.size() == 0) {
				movementKeys[0] = 0;
				movementKeys[1] = 0;
			} else {
				if (engagedUnit == null) {
					lookAt(currentPath.get(0).x, currentPath.get(0).y, Game.game.map.getMinZ(currentPath.get(0).x, currentPath.get(0).y));
					movementKeys[0] = 0;
					movementKeys[1] = 1;
				} else {
					// Move with fastest direction possible:
					double dir = Math.atan2(currentPath.get(0).y - this.y, currentPath.get(0).x - this.x);
					movementKeys[0] = (int) (Math.sin(rotation - dir) * -1.9);
					movementKeys[1] = (int) (Math.cos(rotation - dir) * 1.9);
					if (Game.game.currentTick % 15 == 0) {
						engagedUnit = null;
						detectUnitEngagement(engageDistance);
					}
				}
			}
		}
	}

	public void unitVelocity() {
		if (onGround) { // Can't change velocity in air.
			move(movementKeys[0], movementKeys[1]);
		} else {
			// Air resistance:
			dX = dX * 0.98;
			dY = dY * 0.98;
			dZ = dZ * 0.98; // Terminal velocity can be reached.
		}
	}

	public void tick() {
		if (this.classNumber == 3) {
			pyroAI();
		} else if (this.classNumber == 6) {
			engineerAI();
		} else if (this.classNumber == 7) {
			medicAI();
		} else if (this.classNumber == 8) {
			sniperAI();
		} else {
			attackClassAI();
		}

		if (uber >= 0) {
			uber--;
		}

		pathfindingAI();

		targetedAI();

		unitVelocity();

		if (fireTicksLeft > 0) {
			fireTicksLeft--;
			this.damage(AFTER_BURN_DAMAGE, false);
		}

		// HEALTH:
		if (health > maxHealth) {
			health -= (maxHealth / 3000.0); // 15 seconds to lose all of extra health. (which is 150%)
			if (health < maxHealth) {
				health = maxHealth;
			}
		} else if (classNumber == 7) {
			health += 0.03;
		}

		weapon.tick();

		collisionCheck();
		super.tick();

	}

	public void detectUnitEngagement(double maxDistance) {
		// Goes through every enemy unit, and determines if engagable:
		Entity closestEnemyUnit = null;
		double bestDistance = maxDistance;
		for (Entity enemy : Game.game.units.get(getEnemy())) {
			Entity enemyUnit = enemy;
			if (this.distancexy(enemyUnit) < bestDistance) {
				if (new PathfindingNode(x, y, Game.game.map).lineTo(new PathfindingNode(enemyUnit.x, enemyUnit.y, Game.game.map), false) >= 0) {
					//double angle = Math.atan2(enemyUnit.y - this.y, enemyUnit.x - this.x); // Looking up / down isn't considered.
					//double angleDifference = Math.min(Math.abs(rotation - angle), Math.PI * 2 - Math.abs(rotation - angle));
					//if (angleDifference < Math.PI / 4) { // 45 degrees * 2 = 90 degrees of vision. (Doesnt' care about angle now
					closestEnemyUnit = enemyUnit;
					bestDistance = this.distancexy(enemyUnit);
				}
			}
		}

		if (closestEnemyUnit != null) {
			engagedUnit = closestEnemyUnit;
		}
	}

	public void lookTowardsPoint(double lX, double lY, double lZ) {

		double angle = Math.atan2(lY - this.y, lX - this.x);
		double angleDifference = Math.min(Math.abs(rotation - angle), Math.PI * 2 - Math.abs(rotation - angle));
		double newRot;
		if ((angle - rotation > 0 && angle - rotation <= Math.PI) || angle - rotation <= -Math.PI) {
			if (classNumber == 8) {
				Gun sniper = (Gun) weapon;
				if (sniper.coolDown < Gun.SNIPER_CHARGE_TIME) {
					newRot = rotation + 0.001;
					if (angleDifference < 0.001) {
						newRot = angle;
					}
				} else if (sniper.coolDown > Gun.SNIPER_PAUSE_TIME) {
					// Don't even change angle AT ALL.
					newRot = rotation;
				} else {
					newRot = rotation + 0.01;
					if (angleDifference < 0.01) {
						newRot = angle;
					}
				}
			} else {
				newRot = (rotation + angleDifference * 0.055);
			}
		} else {
			if (classNumber == 8) {
				Gun sniper = (Gun) weapon;
				if (sniper.coolDown < Gun.SNIPER_CHARGE_TIME) {
					newRot = rotation - 0.001;
					if (angleDifference < 0.001) {
						newRot = angle;
					}
				} else if (sniper.coolDown > Gun.SNIPER_PAUSE_TIME) {
					// Don't even change angle AT ALL.
					newRot = rotation;
				} else {
					newRot = rotation - 0.01;
					if (angleDifference < 0.01) {
						newRot = angle;
					}
				}
			} else {
				newRot = (rotation - angleDifference * 0.055);
			}
		}

		double upDownAngle = Math.atan((lZ - (this.z + this.height * HEAD_HEIGHT)) / Math.sqrt((lX - this.x) * (lX - this.x) + (lY - this.y) * (lY - this.y)));
		double upDownAngleDifference = Math.min(Math.abs(upDownRotation - upDownAngle), Math.PI * 2 - Math.abs(upDownRotation - upDownAngle));
		double newUpDownRot;
		if ((upDownAngle - upDownRotation > 0 && upDownAngle - upDownRotation <= Math.PI) || upDownAngle - upDownRotation <= -Math.PI) {
			newUpDownRot = (upDownRotation + upDownAngleDifference * 0.03);
		} else {
			newUpDownRot = (upDownRotation - upDownAngleDifference * 0.03);
		}
		setRotation(newRot, newUpDownRot);
	}

	public void collisionCheck() {
		// All collidables are cylinders
		for (int t = 0; t < Game.game.units.size(); t++) {
			for (int u = 0; u < Game.game.units.get(t).size(); u++) {
				if (Game.game.units.get(t).get(u) == this) { // Other units in the list will do this collision. (Note this can have a failure if a unit is added / removed while the unit ticks are happening)
					return;
				} else { // Buildings handle building - unit collision.
					int collideReducer = 1;
					if (this.team == Game.game.units.get(t).get(u).team) {
						collideReducer = 64;
					}
					double distance = Game.game.units.get(t).get(u).distancexy(this);
					double both = (width + Game.game.units.get(t).get(u).width) * 0.65;
					if (Game.game.units.get(t).get(u).z <= this.z + this.height && Game.game.units.get(t).get(u).z + Game.game.units.get(t).get(u).height >= this.z && distance < both) {
						// Collide:
						if (Game.game.units.get(t).get(u).getClass() == Unit.class) {
							unitCollided(both, distance, collideReducer, t, u);
						} else { // Its a building
							((Building) Game.game.units.get(t).get(u)).collidedWithUnit(both, distance, collideReducer, this.team, Game.game.units.get(this.team).indexOf(this));
						}
					}
				}
			}
		}
	}

	public void unitCollided(double both, double distance, int collideReducer, int t, int u) {
		double rotationBetween = Math.atan2(this.y - Game.game.units.get(t).get(u).y, this.x - Game.game.units.get(t).get(u).x);
		this.move(Math.cos(rotationBetween) * (both - distance) / (2 * collideReducer), Math.sin(rotationBetween) * (both - distance) / (2 * collideReducer), 0);
		Game.game.units.get(t).get(u).move(-Math.cos(rotationBetween) * (both - distance) / (2 * collideReducer), -Math.sin(rotationBetween) * (both - distance) / (2 * collideReducer), 0);
	}

	public void die() {
		// Time until death = 3
		if (dieTime < 0) {
			dieTime = 3;
		}
	}

	public void move(int x, int y) {
		if (x == 0 && y == 0) {
			this.dX = 0;
			this.dY = 0;
		} else {
			double dir = this.rotation + Math.atan2(x, y);
			this.dX = Math.cos(dir) * speed;
			this.dY = Math.sin(dir) * speed;
		}
	}

	public void jump() {
		this.dZ = 0.3;
	}

	public void giveMetal(int amount) {
		if (classNumber == 6) {
			metal += amount;
			if (metal > 200) {
				metal = 200;
			}
		}
	}

	public void damage(double amount, boolean overHeal) {
		if (amount > 0 && uber != -1) {
			// Invulnerable
			return;
		}
		health -= amount;
		if (amount < 0) {
			// Healing:
			if (fireTicksLeft > 0) {
				fireTicksLeft += (int) (amount * 40); // Note that amount is negative.
			}
			if (!overHeal) {
				// Anything that calls this requires the recipient to be under full health.
				if (health > maxHealth) {
					health = maxHealth;
				}
			}
		}
		if (health <= 0) {
			die();
		} else if (health > maxHealth * 1.5) {
			health = maxHealth * 1.5;
		}
	}

	public void lookAt(double x, double y, double z) {
		this.setRotation(Math.atan2(y - this.y, x - this.x), Math.atan2(z - this.z, Math.sqrt((x - this.x) * (x - this.x) + (y - this.y) * (y - this.y))));
	}
}
