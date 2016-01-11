// Author: Aidan Fisher

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.*;
import java.io.Serializable;
import java.util.ArrayList;

public class Map {

	public transient BufferedImage mapImage;

	public static Color[] spawnRoomColors = { new Color(0, 200, 250), new Color(0, 250, 200) };

	public static Color[] controlPointColors = { new Color(0, 143, 143), new Color(0, 203, 203) }; // Control Points only work for one control point currently

	public transient Rectangle[] spawnRooms = new Rectangle[2];
	public transient ArrayList<PathfindingNode> nodes = new ArrayList<PathfindingNode>();

	public Map(BufferedImage mapImage, Game game) {
		this.mapImage = mapImage;
		createNodesKitsSpawns();
		System.out.println("Map loaded, " + nodes.size() + " nodes. (" + mapImage.getWidth() + "x" + mapImage.getHeight() + ")");
	}

	public void createNodesKitsSpawns() {
		Rectangle controlPointRect = null;
		ControlPoint controlPoint = null;
		for (int x = 0; x < mapImage.getWidth(); x++) {
			for (int y = 0; y < mapImage.getHeight(); y++) {
				Color color = new Color(mapImage.getRGB(x, y));
				if (color.getGreen() == 255 && color.getBlue() == 0 && color.getRed() == 0) {
					nodes.add(new PathfindingNode(this, x + 0.5, y + 0.5)); // Center the node.
				} else {
					for (int i = 0; i < Kit.COLOR_CODES.length; i++) {
						if (color.equals(Kit.COLOR_CODES[i])) {
							Game.game.addEntity(new Kit(x + 0.5, y + 0.5, getMinZ(x, y), i), false); // Center the kit
						}
					}
					for (int i = 0; i < 2; i++) {
						if (color.equals(spawnRoomColors[i])) {
							if (spawnRooms[i] == null) {
								spawnRooms[i] = new Rectangle(x, y, 0, 0); // Top Left corner.
							} else {
								spawnRooms[i].width = x - spawnRooms[i].x; // Bottom Right corner.
								spawnRooms[i].height = y - spawnRooms[i].y;
							}
						}
					}
					if (color.equals(controlPointColors[0])) {
						if (controlPointRect == null) {
							controlPointRect = new Rectangle(x, y, 0, 0);
						} else {
							controlPointRect.width = x - controlPointRect.x;
							controlPointRect.height = y - controlPointRect.y;
							// Assign to control point:
							controlPoint.capZone = controlPointRect;
						}
					}
					if (color.equals(controlPointColors[1])) {
						controlPoint = new ControlPoint(x + 0.5, y + 0.5, getMinZ(x, y), 12, 12); // Center the control point
						Game.game.addEntity(controlPoint, false);
					}
				}
			}
		}
	}

	public boolean blocked(int x, int y) {
		if (inBounds(x, y)) {
			Color color = new Color(mapImage.getRGB((int) x, (int) y));
			if (color.getBlue() == 255 && color.getRed() == 255 && color.getGreen() == 255) {
				return true;
			}
			return false;
		}
		return true;
	}

	public boolean blockedForPathfinding(int x, int y) {
		if (inBounds(x, y)) {
			Color color = new Color(mapImage.getRGB((int) x, (int) y));
			if (color.getBlue() == 255) {
				return true;
			}
			return false;
		}
		return true;
	}

	public boolean inBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < mapImage.getWidth() && y < mapImage.getHeight();
	}

	public double getDifference(int x1, int y1, int x2, int y2) {
		return getMinZ(x2, y2) - getMinZ(x1, y1);
	}

	public double getMinZ(Point2D p) {
		return getMinZ(p.getX(), p.getY());
	}

	public double getMinZ(double x, double y) {
		if (inBounds((int) x, (int) y)) {
			Color color = new Color(mapImage.getRGB((int) x, (int) y));
			return color.getRed() / 20.0;
		} else {
			return 0;
		}
	}

	// Always makes the final destination a valid location.
	public ArrayList<PathfindingNode> pathFind(double xCB, double yCB, double xA, double yA, double xB, double yB) {

		// Check if xB && yB are valid:
		if (blockedForPathfinding((int) Math.floor(xB), (int) Math.floor(yB))) {
			// Brint xB / yB to a valid location.
			xB = Math.floor(xB);
			yB = Math.floor(yB);
			for (int range = 1; range <= 20; range++) {
				xB += range;
				if (!blockedForPathfinding((int) xB, (int) yB)) {
					xB += 0.5;
					yB += 0.5;
					break;
				}
				xB -= range;
				yB += range;
				if (!blockedForPathfinding((int) xB, (int) yB)) {
					xB += 0.5;
					yB += 0.5;
					break;
				}
				xB -= range;
				yB -= range;
				if (!blockedForPathfinding((int) xB, (int) yB)) {
					xB += 0.5;
					yB += 0.5;
					break;
				}
				xB += range;
				yB -= range;
				if (!blockedForPathfinding((int) xB, (int) yB)) {
					xB += 0.5;
					yB += 0.5;
					break;
				}
				yB += range;
				if (range == 20) {
					return new ArrayList<PathfindingNode>();
				}

			}
		}

		// Actual pathfinding:

		// These nodes *need* to be deleted after this method is done.
		PathfindingNode startNode = new PathfindingNode(this, xA, yA);
		PathfindingNode endNode = new PathfindingNode(this, xB, yB);

		double startToLast = startNode.lineTo(endNode, true);
		if (startToLast >= 0) {
			ArrayList<PathfindingNode> returnPath = new ArrayList<PathfindingNode>();
			returnPath.add(endNode);
			startNode.delete();
			endNode.delete();
			return returnPath;
		}

		double currentBestDistance = Double.MAX_VALUE;

		ArrayList<ArrayList<PathfindingNode>> openList = new ArrayList<ArrayList<PathfindingNode>>();
		ArrayList<ArrayList<Double>> openListDistance = new ArrayList<ArrayList<Double>>();

		ArrayList<PathfindingNode> existingNodes = (ArrayList<PathfindingNode>) nodes.clone();
		existingNodes.add(endNode);

		openList.add(new ArrayList<PathfindingNode>());
		openListDistance.add(new ArrayList<Double>());
		openList.get(0).add(startNode);
		openListDistance.get(0).add(0.0);

		while (true) {
			if (openList.size() == 0) {
				startNode.delete();
				endNode.delete();
				return new ArrayList<PathfindingNode>();
			}
			if (openListDistance.get(0).get(openListDistance.get(0).size() - 1) == currentBestDistance) {
				openList.get(0).remove(0); // 0 is start pos.
				startNode.delete();
				endNode.delete();
				return openList.get(0);
			}
			PathfindingNode lastNode = openList.get(0).get(openList.get(0).size() - 1);
			for (int n = 0; n < lastNode.connected.size(); n++) {
				double distance = lastNode.distances.get(n) + openListDistance.get(0).get(openListDistance.get(0).size() - 1);
				boolean success = true;
				for (int i = 0; i < openList.size(); i++) {
					for (int j = 0; j < openList.get(i).size(); j++) {
						if (lastNode.connected.get(n).equals(openList.get(i).get(j))) {
							if (openListDistance.get(i).get(j) > distance) {
								// this try wins over the other try:
								// Remove other try:
								openList.remove(i);
								openListDistance.remove(i);
								i--;
								break;
							} else {
								// This check has failed!
								success = false;
								break;
							}
						}
					}
					if (!success) {
						break;
					}
				}
				if (!success) {
					continue;
				}
				if (lastNode.connected.get(n) == endNode) {
					currentBestDistance = distance;
				}
				// Add this try:
				ArrayList<PathfindingNode> nodes = (ArrayList<PathfindingNode>) openList.get(0).clone();
				ArrayList<Double> distances = (ArrayList<Double>) openListDistance.get(0).clone();
				nodes.add(lastNode.connected.get(n));
				distances.add(distance);
				// sort it:
				boolean add = true;
				for (int z = 1; z < openList.size(); z++) {
					// distance physically can't be under its predecessor
					if (distance < openListDistance.get(z).get(openListDistance.get(z).size() - 1)) {
						openList.add(z, nodes);
						openListDistance.add(z, distances);
						add = false;
						break;
					}
				}
				if (add) {
					openList.add(nodes);
					openListDistance.add(distances);
				}
			}
			// remove the openList entry:
			openList.remove(0);
			openListDistance.remove(0);
		}
		// Unreachable.
	}
}
