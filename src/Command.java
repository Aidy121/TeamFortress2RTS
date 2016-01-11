// Author: Aidan Fisher

import java.awt.Component;

public interface Command {
	void execute();
}

class PathFind implements Command {
	short unit;
	double cX;
	double cY;
	double x;
	double y;
	boolean stacked;

	public PathFind(short unit, double cX, double cY, double x, double y, boolean stacked) {
		this.unit = unit;
		this.cX = cX;
		this.cY = cY;
		this.x = x;
		this.y = y;
		this.stacked = stacked;
		Game.commandsToRun.add(this);
	}

	public void execute() {
		// For whatever reason, the unit could "not exist", so it is good to check it:
		Unit unit = (Unit) Game.game.getEntity(this.unit);
		if (unit != null) {
			unit.pathFind(cX, cY, x, y, stacked);
		}
	}
}

class SetTarget implements Command {
	short unit;
	short enemy;
	boolean stacked;

	public SetTarget(short unit, short enemy, boolean stacked) {
		this.unit = unit;
		this.enemy = enemy;
		this.stacked = stacked;
		Game.commandsToRun.add(this);
	}

	public void execute() {
		// For whatever reason, the unit could "not exist", so it is good to check it:
		Unit unit = (Unit) Game.game.getEntity(this.unit);
		if (unit != null) {
			Entity enemy = Game.game.getEntity(this.enemy);
			if (enemy != null) {
				unit.setTarget(enemy, stacked);
			}
		}
	}
}

class AddOrder implements Command {
	short unit;
	byte order;
	double atX;
	double atY;
	boolean stacked;

	public AddOrder(short unit, byte order, double atX, double atY, boolean stacked) {
		this.unit = unit;
		this.order = order;
		this.atX = atX;
		this.atY = atY;
		this.stacked = stacked;
		Game.commandsToRun.add(this);
	}

	public void execute() {
		// For whatever reason, the unit could "not exist", so it is good to check it:
		Unit unit = (Unit) Game.game.getEntity(this.unit);
		if (unit != null) {
			unit.addOrder(order, atX, atY, stacked);
		}
	}
}

class Uber implements Command {
	short unit;
	boolean stacked;

	public Uber(short unit, boolean stacked) {
		this.unit = unit;
		this.stacked = stacked;
		Game.commandsToRun.add(this);
	}

	public void execute() {
		// For whatever reason, the unit could "not exist", so it is good to check it:
		Unit unit = (Unit) Game.game.getEntity(this.unit);
		if (unit != null) {
			unit.uber(stacked);
		}
	}
}