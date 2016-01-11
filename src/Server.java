// Author: Aidan Fisher
// This is meant to be run off some sort of command line. (Otherwise, to end it, simply end the process)
// Port 7788.

import java.awt.Point;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;

public class Server {

	// Potential way to send out
	private static Game sendOutGame = null;
	private static boolean sending = false;

	public static ArrayList<Player> players = new ArrayList<Player>();

	private static ServerSocket serverSocket;
	private static Socket socket;
	private static ObjectOutputStream out;
	private static ObjectInputStream in;
	public static User[] user = new User[10];

	public static void main(String args[]) {
		Game.game = new Game();
		Game.game.start();
		startServer();
	}

	public static void startServer() {
		try {
			System.out.println("Starting server...");
			serverSocket = new ServerSocket(7788);
			System.out.println("Server Started... Address: " + serverSocket.getInetAddress());
			while (true) {
				socket = serverSocket.accept();
				for (int i = 0; i < 10; i++) {
					if (user[i] == null) {
						System.out.println("User " + (i + 1) + " connected from " + socket.getInetAddress());
						socket.setTcpNoDelay(false);
						out = new ObjectOutputStream(socket.getOutputStream());
						in = new ObjectInputStream(socket.getInputStream());
						out.writeInt(i);
						out.flush();
						User theUser = new User(out, in, i);
						user[i] = theUser;
						Thread thread = new Thread(user[i]);
						thread.start();
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void preRunOperations(User user) {
		// Not needed to give users the game just yet. (Its sent every tick anyways) Also, this throws a concurrent modification sometimes. Needs to be done with the game tick.
		/*try {
			// Give connecting users the map, (if exists)
			user.out.writeInt(-1); // Code for MAP writing.
			user.out.writeObject(Game.game); // This is proper. (Unlike current tick implementation)
			user.out.reset();
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}

	public static void sendOutGame() {
		// Create a copy of "game" for send out.

		for (int i = 0; i < Server.user.length; i++) {
			if (Server.user[i] != null) {
				if (Server.user[i].player != null && Server.user[i].out != null) {
					try {
						Server.user[i].out.writeInt(-1); // Code for MAP writing.
						Server.user[i].out.writeObject(Game.game); // This is proper. (Unlike current tick implementation)
						Server.user[i].out.reset(); // This is key, as to send the same object multiple times.
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}

class User implements Runnable {
	ObjectOutputStream out;
	ObjectInputStream in;
	int userID;
	Player player = null;

	public User(ObjectOutputStream out, ObjectInputStream in, int userID) {
		this.out = out;
		this.in = in;
		this.userID = userID;
	}

	public void run() {
		Server.preRunOperations(this);
		while (true) {
			try {
				String type = in.readUTF();
				if (type.equals("newObject")) {
					Object object = in.readObject();
					if (object instanceof String) {
						// Adding player..
						this.player = new Player((String) object, Entity.BLUE);
						boolean addPlayer = true;
						for (int i = 0; i < Server.players.size(); i++) {
							if (this.player.name.equals(Server.players.get(i).name)) {
								// Set to online!
								if (Server.players.get(i).online) {
									System.out.println("Player " + player.name + " is already connected! " + (userID + 1));
									this.player = null;
									addPlayer = false;
								} else {
									this.player = Server.players.get(i);
									System.out.println("Returning User: " + this.player.name + ", " + (userID + 1));
									this.player.online = true;
									addPlayer = false;
								}
								break;
							}
						}
						if (addPlayer) {
							System.out.println("New User: " + this.player.name + ", " + (userID + 1));
							Server.players.add(this.player);
						}
					}
				} else if (type.equals("MoveUnit")) { // this needs to require the player to be the owner of the unit
					short unit = in.readShort();
					double cX = in.readDouble();
					double cY = in.readDouble();
					double x = in.readDouble();
					double y = in.readDouble();
					boolean stacked = in.readBoolean();
					// Pretty simple: (If the unit doesn't "exist", game will take care of that.) [Keep in mind, this currently could easily let the player control enemy units]
					new PathFind(unit, cX, cY, x, y, stacked); // All commands take the liberty of adding themselves to the game stack.
				} else if (type.equals("SetTarget")) { // this needs to require the player to be the owner of the unit
					short unit = in.readShort();
					short enemy = in.readShort();
					boolean stacked = in.readBoolean();
					// Pretty simple:
					new SetTarget(unit, enemy, stacked);
				} else if (type.equals("AddOrder")) {
					short unit = in.readShort();
					byte order = in.readByte();
					double atX = in.readDouble();
					double atY = in.readDouble();
					boolean stacked = in.readBoolean();
					new AddOrder(unit, order, atX, atY, stacked);
				} else if (type.equals("Uber")) {
					short unit = in.readShort();
					boolean stacked = in.readBoolean();
					new Uber(unit, stacked);
				} else if (type.equals("Exiting")) {
					disconnect();
					return;
				}
			} catch (IOException e) {
				disconnect();
				return;
			} catch (Exception e) {
				System.out.println("Alternate error.");
				e.printStackTrace();
			}
		}
	}

	public void disconnect() {
		this.out = null;
		this.in = null;
		// Free up slot:
		int i = 0;
		for (; i < 10; i++) {
			if (Server.user[i] == this) {
				Server.user[i] = null;
				break;
			}
		}
		System.out.println("User " + (i + 1) + " has disconnected.");
		try {
			if (this.player != null) {
				this.player.online = false;
			}
		} catch (Exception e1) {

		}
	}

	public static void queueMethod(Command command) {
		while (Game.blockAdding) {
			// Hmm.
		}
		Game.blockAdding = true;
		Game.commandsToRun.add(command);
		Game.blockAdding = false;
	}
}