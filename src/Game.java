// Author: Aidan Fisher

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

public class Game implements Serializable, Runnable, Cloneable {

	private static final long serialVersionUID = -6842270011743467561L;

	public static boolean blockAdding = false;
	public static ArrayList<Command> commandsToRun = new ArrayList<Command>();

	public static Game game = null;

	public final static float GRAVITY = 0.005f; // This value is added every tick.

	public transient double ticksPerSecond = 100;
	public transient long lastTick = 0;

	public transient boolean isRunning = true;
	public transient Map map;

	public int[] ticksLeftToWin = new int[2]; // The game is set to king of the hill only now.

	public int currentTick = 0;
	public ArrayList<Entity> entities = new ArrayList<Entity>(); // Units include buildings.
	public ArrayList<ArrayList<Entity>> units = new ArrayList<ArrayList<Entity>>();

	public static String[] classNames = { "Scout", "Soldier", "Pyro", "Demoman", "Heavy", "Engineer", "Medic", "Sniper", "Spy" }; // Corresponds to the classes that spawn
	public transient int[][] respawnTimer = new int[2][9]; // One for each class currently. [Either Comp HL TF2 or completely redesigned]
	// Only used for Engineer:
	public transient Building[][] dispenser = new Building[2][9];
	public transient Building[][] sentry = new Building[2][9];

	public Game() {
		game = this;
		currentTick = 0;
		units.add(new ArrayList<Entity>()); // BLUE
		units.add(new ArrayList<Entity>()); // RED
		ticksLeftToWin[0] = 18000;
		ticksLeftToWin[1] = 18000;
		// Since all respawn timers are set to 0, the units will spawn.
	}

	public void start() {
		try {
			BufferedImage map = ImageIO.read(new File("res/MapData.png"));
			this.map = new Map(map, this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//Starting game loop

		new Thread(this).start();
	}

	public void tick() {

		if (game.currentTick % 1000 == 0) {
			System.out.println("Entities: " + game.entities.size() + " " + System.currentTimeMillis());
		}

		// Run actions!
		runTasks();

		for (int t = 0; t < 2; t++) {
			for (int c = 0; c < 9; c++) {
				if (c + 1 != 9) {
					if (respawnTimer[t][c] == 0) {
						addEntity(new Unit(map.spawnRooms[t].x + new Random().nextInt(map.spawnRooms[t].width), map.spawnRooms[t].y + new Random().nextInt(map.spawnRooms[t].height), 0, 0, t, c,
								classNames[c]), true);
						respawnTimer[t][c] = -1;
					} else if (respawnTimer[t][c] > 0) {
						respawnTimer[t][c]--;
					}
				}
			}
		}

		lastTick = System.nanoTime();
		for (int i = 0; i < game.entities.size(); i++) {
			game.entities.get(i).beforeTick();
		}
		for (int i = 0; i < game.entities.size(); i++) {
			game.entities.get(i).tick();
			if (game.entities.get(i).getClass() == ControlPoint.class) {
				if (game.entities.get(i).team != -1 && ticksLeftToWin[game.entities.get(i).team] < -3000) {
					// Reset the game.
					System.out.println("Game restarting...");
					Game.game = new Game();
					game.map = this.map;
					return;
				}

			}
		}
		for (int i = 0; i < game.entities.size(); i++) {
			game.entities.get(i).afterTick();
		}
		game.currentTick++;

		for (int i = 0; i < game.entities.size(); i++) {
			if (game.entities.get(i).dieTime == 0) {
				Entity entity = game.entities.get(i);
				game.entities.remove(i);
				game.units.get(entity.team).remove(entity);
				if (entity.getClass() == Unit.class) {
					Unit unit = (Unit) entity;
					respawnTimer[unit.team][unit.unitNumber] = 1000; // Ten seconds.
				}
				i--;
				// Go through existing entities and remove any ties:
				for (int t = 0; t < game.units.size(); t++) {
					for (int u = 0; u < game.units.get(t).size(); u++) {
						if (game.units.get(t).get(u).getClass() == Unit.class) {
							Unit unit = ((Unit) game.units.get(t).get(u));
							if (unit.engagedUnit == entity) {
								unit.engagedUnit = null;
							}
							if (unit.wantedTargetUnit == entity) {
								unit.wantedTargetUnit = null;
							}
							if (unit.weapon != null && unit.weapon.getClass() == HealingBeam.class) {
								if (((HealingBeam) unit.weapon).unitHealing == entity.id) {
									((HealingBeam) unit.weapon).unitHealing = Short.MAX_VALUE;
								}
							}
						}
					}
				}
			}
		}
		Collections.sort(game.entities);

		Server.sendOutGame();
	}

	public void runTasks() {
		// Much better than reflection:
		while (commandsToRun.size() > 0) {
			commandsToRun.get(0).execute();
			commandsToRun.remove(0);
		}
	}

	public Entity getEntity(short id) {
		if (id > -16000) {
			System.out.println("Resources QUARTER used!!");
		}
		for (Entity entity : entities) {
			if (entity.id == id) {
				return entity;
			}
		}
		return null;
	}

	public void addEntity(Entity entity, boolean collidable) {
		entities.add(entity);
		if (collidable) {
			units.get(entity.team).add(entity);
		}
	}

	public Game clone() throws CloneNotSupportedException {
		return (Game) super.clone();
	}

	public void run() {
		long lastTime = System.nanoTime();
		double unprocessed = 0;
		double nsPerTick = 1000000000.0 / /*Just in case*/(double) ticksPerSecond;
		long lastTimer1 = System.currentTimeMillis();
		// Just in case:
		lastTick = System.nanoTime();
		while (isRunning) {
			long now = System.nanoTime();
			unprocessed += (now - lastTime) / nsPerTick;
			lastTime = now;
			while (unprocessed >= 1) {
				game.tick(); // This forces the game to tick the actual game that is being sent out. (Otherwise resets would simply refer back to the old game)
				unprocessed -= 1;
			}
			{
				// Nothing.
			}

			if (System.currentTimeMillis() - lastTimer1 > 1000) {
				lastTimer1 += 1000;
			}
		}
	}
}
