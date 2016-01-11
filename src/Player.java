// Author: Aidan Fisher

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;

public class Player implements Serializable {

	// Client recieves player

	private static final long serialVersionUID = 7100857761457340534L;

	public boolean online = false;
	public String name; // This is the player's unique Identifier! (?)
	public int teamControlling;

	public Player(String name, int teamControlling) {
		this.name = name;
		this.online = true; // When it's added.
		this.teamControlling = teamControlling;
	}
}
