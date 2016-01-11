// Author: Aidan Fisher

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;

public class PathfindingNode implements Serializable {

	// Also lists orders: (All include pathfinding, even if already there)
	public static final byte PATH_FINDING = 0;
	public static final byte DISPENSER_BUILD = 1;
	public static final byte SENTRY_BUILD = 2;

	private static final long serialVersionUID = 394100569780330688L;

	public transient Map map;

	public transient byte order;
	public double x;
	public double y;
	public transient ArrayList<PathfindingNode> connected = new ArrayList<PathfindingNode>();
	public transient ArrayList<Double> distances = new ArrayList<Double>();

	// Connects to existing node network, (assumed to be a main node)
	public PathfindingNode(Map map, double x, double y) {
		// Connect node to existing nodes:
		this.map = map;
		this.x = x;
		this.y = y;
		this.order = PATH_FINDING;
		for (PathfindingNode node : this.map.nodes) {
			double distance = node.lineTo(this, true);
			if (distance >= 0) {
				node.connected.add(this);
				node.distances.add(distance);
			}
			//Note: If distance is recalculated here, (to account for ledges,) remove method has to consider this.
			if (distance >= 0) {
				connected.add(node);
				distances.add(distance);
			}
		}
	}

	// Does not connect to existing node network.
	public PathfindingNode(double x, double y, Map map) {
		this.x = x;
		this.y = y;
		this.map = map;
		this.order = PATH_FINDING; // Default.
	}

	// Does not connect to existing node network.
	public PathfindingNode(double x, double y, Map map, byte order) {
		this.x = x;
		this.y = y;
		this.map = map;
		this.order = order; // Default.
	}

	// The node is assumed to be not be apart of map nodes.
	public void delete() {
		// Opposite of initial constructor: (But only has to remove references to)
		for (PathfindingNode node : connected) {
			int index = node.connected.lastIndexOf(this); // Its either the last or 2nd last reference.
			node.connected.remove(index);
			node.distances.remove(index);
		}
	}

	public double lineTo(PathfindingNode other, boolean pathfinding) {
		if (map.blockedForPathfinding((int) Math.floor(other.x), (int) Math.floor(other.y))) {
			return -1; // Causes issues.
		}

		boolean blocked = map.blockedForPathfinding((int) Math.floor(this.x), (int) Math.floor(this.y));

		if (this.y == other.y && other.x == this.x) {
			return 0;
		}

		double rotation = Math.atan2(other.y - this.y, other.x - this.x);

		// Basically, the goal is to acquire a list of grid squares that are passed through.
		// note 0, 90, special cases, do happen.
		int xSign;
		int xFix;
		if (Math.cos(rotation) >= 0) {
			xSign = 1;
			xFix = 0;
		} else {
			xSign = -1;
			xFix = -1;
		}
		int ySign;
		int yFix;
		if (Math.sin(rotation) >= 0) {
			ySign = 1;
			yFix = 0;
		} else {
			ySign = -1;
			yFix = -1;
		}
		if ((rotation >= Math.PI / 4 && rotation < 3 * Math.PI / 4) || (rotation >= 5 * Math.PI / 4 && rotation < 7 * Math.PI / 4)) {
			double m = Math.cos(rotation) / Math.sin(rotation);
			double b = this.x - m * this.y;
			Point checkPoint = new Point((int) Math.floor(this.x), (int) Math.floor(this.y));
			if (xSign == -1) {
				checkPoint.x += 1;
			}
			if (ySign == -1) {
				checkPoint.y += 1;
			}
			while (true) {
				// Check if this point is "done"

				if (pathfinding) {
					if (map.blockedForPathfinding(checkPoint.x + xFix, checkPoint.y + yFix)) {
						if (!blocked || map.blocked(checkPoint.x + xFix, checkPoint.y + yFix)) {
							return -1;
						}
					} else if (blocked) {
						blocked = false;
					}
				} else if (map.blocked(checkPoint.x + xFix, checkPoint.y + yFix)) {
					return -1;
				}

				if (Math.abs(checkPoint.x + 0.5 - other.x) <= 1.6 && Math.abs(checkPoint.y + 0.5 - other.y) <= 1.6) {
					return Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y));
				}

				// Check point to right:
				checkPoint = new Point(checkPoint.x, checkPoint.y + ySign);
				double x0 = m * checkPoint.y + b;
				if ((xSign == 1 && x0 >= checkPoint.x && x0 <= checkPoint.x + 1) || (xSign == -1 && x0 <= checkPoint.x && x0 >= checkPoint.x - 1)) {
					continue;
				}

				// Check point to above, (it MUST intersect, but, we need "lastInterX, lastInterY"
				checkPoint = new Point(checkPoint.x + xSign, checkPoint.y - ySign /* reverted last check*/);
				// Intersection check is pointless.
			}
		} else {

			double m = Math.sin(rotation) / Math.cos(rotation);
			double b = this.y - m * this.x;
			Point checkPoint = new Point((int) Math.floor(this.x), (int) Math.floor(this.y));
			if (xSign == -1) {
				checkPoint.x += 1;
			}
			if (ySign == -1) {
				checkPoint.y += 1;
			}
			while (true) {
				// Check if this point is "done"
				if (pathfinding) {
					if (map.blockedForPathfinding(checkPoint.x + xFix, checkPoint.y + yFix)) {
						if (!blocked || map.blocked(checkPoint.x + xFix, checkPoint.y + yFix)) {
							return -1;
						}
					} else if (blocked) {
						blocked = false;
					}
				} else if (map.blocked(checkPoint.x + xFix, checkPoint.y + yFix)) {
					return -1;
				}

				if (Math.abs(checkPoint.x + 0.5 - other.x) <= 1.6 && Math.abs(checkPoint.y + 0.5 - other.y) <= 1.6) {
					return Math.sqrt((other.x - this.x) * (other.x - this.x) + (other.y - this.y) * (other.y - this.y));
				}

				// Check point to right:
				checkPoint = new Point(checkPoint.x + xSign, checkPoint.y);
				double y0 = m * checkPoint.x + b;
				if ((ySign == 1 && y0 >= checkPoint.y && y0 <= checkPoint.y + 1) || (ySign == -1 && y0 <= checkPoint.y && y0 >= checkPoint.y - 1)) {
					continue;
				}

				// Check point to above, (it MUST intersect, but, we need "lastInterX, lastInterY"
				checkPoint = new Point(checkPoint.x - xSign /* reverted last check*/, checkPoint.y + ySign);
				// Intersection check is pointless.
			}
		}

		// Basically, the goal is to acquire a list of grid squares that are passed through.
		// note 0, 45, 90, special cases, do happen.

		/*int xDist = other.x - this.x;
		int yDist = other.y - this.y;

		if (xDist == 0 && yDist == 0) {
			return 0;
		}

		int xSign;
		if (xDist >= 0) {
			xSign = 1;
		} else {
			xSign = -1;
		}
		int ySign;
		if (yDist >= 0) {
			ySign = 1;
		} else {
			ySign = -1;
		}

		// ratio:
		double ratio;
		if (xDist == 0) {
			ratio = 170000000.0 * ySign;
		} else {
			ratio = yDist / (double) xDist;
		}

		double currentX = this.x + 0.5;
		double currentY = this.y + 0.5;

		while (true) {
			//System.out.println(ratio + " " + currentX + " " + currentY + " (" + this.x + ", " + this.y + ") -> (" + other.x + ", " + other.y + ")");
			// Move to next grid point.
			int nextX;
			if (xSign == 1) {
				nextX = (int) Math.floor(currentX + 1);
			} else {
				nextX = (int) Math.ceil(currentX - 1);
			}
			int nextY;
			if (ySign == 1) {
				nextY = (int) Math.floor(currentY + 1);
			} else {
				nextY = (int) Math.ceil(currentY - 1);
			}

			double nextRatio = (nextY - currentY) / (double) (nextX - currentX);
			//System.out.println(ratio + " / " + nextRatio);
			if ((ratio >= nextRatio && xSign / ySign == 1) || (ratio <= nextRatio && xSign / ySign == -1)) {
				//System.out.println(xSign + " " + ySign + " " + (nextY - currentY));
				currentX += ((nextY - currentY) / ratio);
				currentY = nextY;
			} else {
				currentY += ((nextX - currentX) * ratio);
				currentX = nextX;
			}
			Point p = new Point((int) Math.floor(currentX), (int) Math.floor(currentY));
			if (p.x == other.x && p.y == other.y) {
				// done
				return Math.sqrt(xDist * xDist + yDist * yDist);
			}
			if (map.blocked(p.x, p.y)) {
				break;
			}
			// other checks.
		}
		return -1;*/
	}
}
