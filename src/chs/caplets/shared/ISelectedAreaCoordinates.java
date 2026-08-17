package chs.caplets.shared;

import chs.utilities.Pair;

import java.awt.Point;

public interface ISelectedAreaCoordinates
{

	Pair<Point, Point> getStartAndEndOfAreaSelection();
}