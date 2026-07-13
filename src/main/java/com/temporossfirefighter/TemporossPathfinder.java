package com.temporossfirefighter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

final class TemporossPathfinder
{
	private static final int[][] DIRECTIONS = {
		{-1, -1}, {-1, 0}, {-1, 1},
		{0, -1},           {0, 1},
		{1, -1},  {1, 0},  {1, 1}
	};
	private static final int MAX_VISITED_TILES = 12_000;

	private TemporossPathfinder()
	{
	}

	static List<WorldPoint> findPath(WorldView worldView, WorldPoint start, WorldPoint target)
	{
		if (worldView == null || start == null || target == null
			|| start.getPlane() != target.getPlane()
			|| LocalPoint.fromWorld(worldView, start) == null
			|| LocalPoint.fromWorld(worldView, target) == null)
		{
			return Collections.emptyList();
		}

		PriorityQueue<Node> open = new PriorityQueue<>();
		Map<WorldPoint, Integer> costs = new HashMap<>();
		Map<WorldPoint, WorldPoint> previous = new HashMap<>();
		open.add(new Node(start, 0, heuristic(start, target)));
		costs.put(start, 0);

		int visited = 0;
		while (!open.isEmpty() && visited++ < MAX_VISITED_TILES)
		{
			Node current = open.poll();
			Integer bestCost = costs.get(current.point);
			if (bestCost == null || current.cost != bestCost)
			{
				continue;
			}
			if (current.point.equals(target))
			{
				return reconstructPath(previous, target);
			}

			WorldArea currentArea = new WorldArea(current.point, 1, 1);
			for (int[] direction : DIRECTIONS)
			{
				int dx = direction[0];
				int dy = direction[1];
				if (!currentArea.canTravelInDirection(worldView, dx, dy))
				{
					continue;
				}

				WorldPoint next = current.point.dx(dx).dy(dy);
				if (LocalPoint.fromWorld(worldView, next) == null)
				{
					continue;
				}
				int nextCost = current.cost + 1;
				if (nextCost >= costs.getOrDefault(next, Integer.MAX_VALUE))
				{
					continue;
				}

				costs.put(next, nextCost);
				previous.put(next, current.point);
				open.add(new Node(next, nextCost, nextCost + heuristic(next, target)));
			}
		}
		return Collections.emptyList();
	}

	private static int heuristic(WorldPoint point, WorldPoint target)
	{
		return Math.max(Math.abs(point.getX() - target.getX()),
			Math.abs(point.getY() - target.getY()));
	}

	private static List<WorldPoint> reconstructPath(
		Map<WorldPoint, WorldPoint> previous,
		WorldPoint target)
	{
		List<WorldPoint> path = new ArrayList<>();
		WorldPoint current = target;
		while (current != null)
		{
			path.add(current);
			current = previous.get(current);
		}
		Collections.reverse(path);
		return path;
	}

	private static final class Node implements Comparable<Node>
	{
		private final WorldPoint point;
		private final int cost;
		private final int score;

		private Node(WorldPoint point, int cost, int score)
		{
			this.point = point;
			this.cost = cost;
			this.score = score;
		}

		@Override
		public int compareTo(Node other)
		{
			return Integer.compare(score, other.score);
		}
	}
}
