// Author: Aidan Fisher

import java.awt.Graphics2D;
import java.io.Serializable;

public class Weapon implements Serializable {
	private static final long serialVersionUID = 902372815712362713L;
	public short parent;

	public Weapon(Entity parent) {
		if (parent != null) {
			this.parent = parent.id;
		}
	}

	public void tick() {

	}

	public void render(Graphics2D g) {

	}
}
