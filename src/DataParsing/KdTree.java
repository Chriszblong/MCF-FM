package DataParsing;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import COMSETsystem.*;

/**
 * Modified from Michael <GrubenM@GMail.com>'s code (https://github.com/mgruben/Kd-Trees) to index 
 * 2D line segments (i.e., links) instead of 2D points. 
 * @author Bo <boxu08@gmail.com>
 *
 */

/*
 * Copyright (C) 2019 Bo <boxu08@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * 
 * Below is the original license notice from Michael <GrubenM@GMail.com> 
 *
 */
/*
 * Copyright (C) 2016 Michael <GrubenM@GMail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class KdTree {
	private Node root;
	private int size;

	/**
	 * Construct an empty set of points.
	 */
	public KdTree() {
		size = 0;
	}

	/**
	 * Is the group empty?
	 * 
	 * @return {@code true} if this group is empty;
	 *         {@code false} otherwise
	 */
	public boolean isEmpty() {
		return root == null;
	}

	/**
	 * @return the number of links in the group.
	 */
	public int size() {
		return size;
	}

	/**
	 * Add the link to the group.
	 * 
	 * At the root (and every second level thereafter), the x-coordinate is
	 * used as the key. The root link partitions the space using a vertical band
	 * the width of which is determined by the minX and maxX of the link. Let L be
	 * the next link to be added. 
	 * 
	 * If L.maxX<=root.minX, then L is added to the left branch of the root;
	 * If L.minX>=root.maxX, then L is added to the right branch of the root; 
	 * Otherwise (i.e., L intersects the root's partition band), the root link's partition
	 * band is expanded to contain L, and L is added to the right branch of the root.  
	 *  
	 * @param link the link to add
	 * @throws NullPointerException if {@code link} is {@code null}
	 */
	public void insert(Link link) {
		if (link == null) throw new java.lang.NullPointerException(
				"called insert() with a null Point2D");

		root = insert(root, link, true);
	}

	private Node insert(Node n, Link link, boolean evenLevel) {
		if (n == null) {
			size++;
			return new Node(link);
		}

		double cmp = directionLinkToBand(link, n, evenLevel);

		/**
		 * Traverse down the BST.
		 * 
		 * In subsequent levels, the orientation is orthogonal
		 * to the current orientation.
		 * 
		 * Place the link in the left or right nodes accordingly.
		 */
		
		/**
		 * If the comparison is affirmatively left or right, then we're considering a link
		 * that is either entirely to the left/bottom (may touch) or entirely to the right/top 
		 * (may touch) of the node's partition band.
		 */

		// Handle Nodes which should be inserted to the left
		if (cmp < 0 && evenLevel) {
			n.lb = insert(n.lb, link, !evenLevel);
		}

		// Handle Nodes which should be inserted to the bottom
		else if (cmp < 0 && !evenLevel) {
			n.lb = insert(n.lb, link, !evenLevel);
		}

		// Handle Nodes which should be inserted to the right
		else if (cmp > 0 && evenLevel) {
			n.rt = insert(n.rt, link, !evenLevel);
		}

		// Handle Nodes which should be inserted to the top
		else if (cmp > 0 && !evenLevel) {
			n.rt = insert(n.rt, link, !evenLevel);
		}

		/**
		 * The link intersects the node's partition band. 
		 * This is considered a tie and resolved in favor of the right subtree.
		 */
		else { 
			// extend the node's partition band
		    n.extendRange(link);
		    n.rt = insert(n.rt, link, !evenLevel);
		}	

		return n;
	}

	/**
	 * A nearest neighbor in the group to point p; null if the group is empty.
	 * 	  
	 * @param link the link from which to search for a neighbor
	 * @return the nearest neighbor to the given link,
	 *         {@code null} otherwise.
	 * @throws NullPointerException if {@code link} is {@code null}
	 */
	public Link nearest(Point2D p) {
		if (p == null) throw new java.lang.NullPointerException(
				"called contains() with a null Point2D");
		if (isEmpty()) return null;
		return nearest(root, p, root.link, true);
	}

	private Link nearest(Node n, Point2D p, Link champion,
			boolean evenLevel) {

		// Handle reaching the end of the tree
		if (n == null) return champion;

		// Determine if the current Node's link beats the existing champion
		if (n.link.distanceSq(p) < champion.distanceSq(p))
			champion = n.link;

		/**
		 * Calculate the distance from the search point to the current
		 * Node's partition band.
		 * 
		 * Primarily, the sign of this calculation is useful in determining
		 * which side of the Node to traverse next.
		 * 
		 * Additionally, the magnitude to toPartitionLine is useful for pruning.
		 * 
		 * Specifically, if we find a champion whose distance is shorter than
		 * to a previous partition band, then we know we don't have to check any
		 * of the links on the other side of that partition band, because none
		 * can be closer.
		 */
		double toPartitionLine = distancePointToBand(p, n, evenLevel);

		/**
		 * Handle the search point being to the left of or below
		 * the current Node's partition band.
		 */
		if (toPartitionLine < 0) {
			champion = nearest(n.lb, p, champion, !evenLevel);

			// Since champion may have changed, recalculate distance
			if (champion.distanceSq(p) >=
					toPartitionLine * toPartitionLine) {
				champion = nearest(n.rt, p, champion, !evenLevel);
			}
		}

		/**
		 * Handle the search link being to the right of or above
		 * the current Node's partition band.
		 * 
		 * Note that, since insert() above breaks link comparison ties
		 * by placing the inserted link on the right branch of the current
		 * Node, traversal must also break ties by going to the right branch
		 * of the current Node (i.e. to the right or top, depending on
		 * the level of the current Node).
		 */
		else {
			champion = nearest(n.rt, p, champion, !evenLevel);

			// Since champion may have changed, recalculate distance
			if (champion.distanceSq(p) >=
					toPartitionLine * toPartitionLine) {
				champion = nearest(n.lb, p, champion, !evenLevel);
			}
		}

		return champion;
	}

	/**
	 * The direction from the given link to the given Node's partition band.
	 * 
	 * If the sign of the returned integer is -1, then the given link
	 * lies or should lie on the left branch of the given Node.
	 * 
	 * Otherwise, the given link lies or should lie on the right branch of the given Node.
	 * 
	 * @param link the link in question
	 * @param n the Node in question
	 * @param evenLevel is the current level even?  If so, then the Node's
	 *        partition band is vertical, so the link will be to the left
	 *        or right of the Node's partition band. If not, then the Node's 
	 *        partition band is horizontal, so the link will be above or 
	 *        below the Node's partition band.
	 * @return -1 if the link is to the left of the node's partition band; 
	 *         1 right; 
	 *         0 if the link intersects the node's partition band.
	 */
	private int directionLinkToBand(Link link, Node n, boolean evenLevel) {
		if (evenLevel) { // compare x coordinates
			if (link.maxX <= n.minX)
				return -1;
			if (link.minX >= n.maxX)
				return 1;
			else 
				return 0;
		}
		else { // compare y coordinates
			if (link.maxY <= n.minY)
				return -1;
			if (link.minY >= n.maxY)
				return 1;
			else 
				return 0;		
		}
	}
	
	/**
	 * The distance and direction from the given point to the given Node's partition band.
	 * 
	 * If the sign of the returned double is negative, then the given point
	 * lies or should lie on the left branch of the given Node.
	 * 
	 * Otherwise (including where the difference is exactly 0), then the
	 * given point lies or should lie on the right branch of the given Node.
	 * 
	 * @param p the point in question
	 * @param n the Node in question
	 * @param evenLevel is the current level even?  If so, then the Node's
	 *        partition band is vertical, so the point will be to the left
	 *        or right of the Node's partition band. If not, then the Node's 
	 *        partition band is horizontal, so the point will be above or 
	 *        below the Node's partition band.
	 * @return -1 if the point is to the left of the node's partition band; 
	 *         1 right; 
	 *         0 if the point is inside the node's partition band.
	 */	
	private double distancePointToBand(Point2D p, Node n, boolean evenLevel) {
		if (evenLevel) { // compare x coordinates
			if (p.getX() <= n.minX || p.getX() >= n.maxX) 
				return p.getX() - n.minX;
			else 
				return 0;
		}
		else { // compare y coordinates
			if (p.getY() <= n.minY || p.getY() >= n.maxY) 
				return p.getY() - n.minY;
			else 
				return 0;
		}		
	}

	/**
	 * The data structure from which a KdTree is created.
	 */
	private static class Node {

		private final Link link;
		private double minX;
		private double minY;
		private double maxX;
		private double maxY;

		// the left/bottom subtree
		private Node lb;

		// the right/top subtree
		private Node rt;

		private Node(Link link) {
		
			this.link = link;
			minX = link.minX;
			minY = link.minY;
			maxX = link.maxX;
			maxY = link.maxY;
			lb = null;
			rt = null;
			
		}

		public void extendRange(Link link) {
			minX = Math.min(minX, link.minX);
			minY = Math.min(minY, link.minY);
			maxX = Math.max(maxX, link.maxX);
			maxY = Math.max(maxY, link.maxY);			
		}
	}
}