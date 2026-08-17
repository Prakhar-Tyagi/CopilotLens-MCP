package chs.caplets.logic.actions;

import chs.cof.draw.IGfxObject;
import chs.cof.draw.ILine;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.schem.IAbstractSchemPin;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISegment;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.common.Location;
import chs.utilities.SetMap;
import chs.utility.helpers.BaseDeviceConnectionHelper;
import chs.utility.helpers.ConnectionHelper;
import chs.utility.helpers.PinListConnectionHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConvertSymbolToParamHelper
{

	public static final int SIDE_TOP = 1;
	public static final int SIDE_RIGHT = 2;
	public static final int SIDE_BOTTOM = 4;
	public static final int SIDE_LEFT = 8;

	private ConvertSymbolToParamHelper()
	{

	}

	@Nullable
	public static Point getSingleIntersectionPoint(IExtent absExtent, IConductor conductor,
			AffineTransform transform)
	{
		Set<ISegment> segments = new LinkedHashSet<>();
		segments.addAll(conductor.getSegmentsOfType(ISegment.class));
		Point intersection = null;
		for (ISegment segment : segments) {
			ILine line = segment.getLineHolder();
			boolean intersects = line.reallyIntersects(absExtent);
			if (intersects && intersection != null) {
				return null;
			}
			if (intersects) {
				intersection = findIntersectionPoint(line, absExtent, transform);
				if (intersection != null && Line2D.relativeCCW(line.getStartPoint().getX(), line.getStartPoint().getY(),
						line.getEndPoint().getX(), line.getEndPoint().getY(), intersection.getX(),
						intersection.getY()) != 0) {
					intersection = null;
				}
			}
		}
		return intersection;
	}

	@Nullable private static Point findIntersectionPoint(ILine line, IExtent absExtent, AffineTransform transform)
	{

		int x1 = line.getStartPoint().getX();
		int x2 = line.getEndPoint().getX();
		int y1 = line.getStartPoint().getY();
		int y2 = line.getEndPoint().getY();

		Point lineStart = new Point(x1, y1);
		Point lineEnd = new Point(x2, y2);

		transform.transform(lineStart, lineStart);
		transform.transform(lineEnd, lineEnd);

		int eX = absExtent.getX();
		int eY = absExtent.getY();
		int eW = absExtent.getWidth();
		int eH = absExtent.getHeight();

		Point line2Start = new Point(eX, eY);
		Point line2End = new Point();

		Rectangle bounds = new Rectangle(eX, eY, eW, eH);
		Point outerPoint = new Point();
		Point innerPoint = new Point();

		if (bounds.contains(lineStart)) {
			outerPoint.setLocation(lineEnd);
			innerPoint.setLocation(lineStart);
		}
		else {
			outerPoint.setLocation(lineStart);
			innerPoint.setLocation(lineEnd);
		}

		int side = determineIntersectionSide(bounds, innerPoint, outerPoint);

		switch (side) {
			case SIDE_TOP:
				line2Start.setLocation(eX, eY + eH);
				line2End.setLocation(eX + eW, eY + eH);
				break;
			case SIDE_RIGHT:
				line2Start.setLocation(eX + eW, eY);
				line2End.setLocation(eX + eW, eY + eH);
				break;
			case SIDE_BOTTOM:
				line2End.setLocation(eX + eW, eY);
				break;
			case SIDE_LEFT:
				line2End.setLocation(eX, eY + eH);
				break;
			default:
				break;
		}
		return findIntersectionOfTwoLines(innerPoint, outerPoint, line2Start, line2End);
	}

	private static int determineIntersectionSide(Rectangle bounds, Point innerPoint, Point outerPoint)
	{
		double x = bounds.getX();
		double y = bounds.getY();
		double w = bounds.getWidth();
		double h = bounds.getHeight();
		if (Line2D.linesIntersect(x, y, x, y + h, innerPoint.getX(), innerPoint.getY(), outerPoint.getX(),
				outerPoint.getY())) {
			return SIDE_LEFT;
		}
		if (Line2D.linesIntersect(x, y + h, x + w, y + h, innerPoint.getX(), innerPoint.getY(), outerPoint.getX(),
				outerPoint.getY())) {
			return SIDE_TOP;
		}
		if (Line2D.linesIntersect(x + w, y, x + w, y + h, innerPoint.getX(), innerPoint.getY(), outerPoint.getX(),
				outerPoint.getY())) {
			return SIDE_RIGHT;
		}
		return SIDE_BOTTOM;
	}

	//returns first intersection point of the line segments if there is any
	private static Point findIntersectionOfTwoLines(Point lineStart, Point lineEnd, Point line2Start, Point line2End)
	{
		double x;
		double y;
		Double slope1 = getSlope(lineStart, lineEnd);
		Double slope2 = getSlope(line2Start, line2End);

		if (slope1.isInfinite()) {
			x = getXintercept(lineStart, lineEnd);
			y = (slope2 * x) + getYintercept(line2Start, line2End);
		}
		else if (slope2.isInfinite()) {
			x = getXintercept(line2Start, line2End);
			y = (slope1 * x) + getYintercept(lineStart, lineEnd);
		}
		else if (slope1 == 0) {
			y = getYintercept(lineStart, lineEnd);
			x = (y - getYintercept(line2Start, line2End)) / slope2;
		}
		else if (slope2 == 0) {
			y = getYintercept(line2Start, line2End);
			x = (y - getYintercept(lineStart, lineEnd)) / slope1;
		}
		else {
			double yintercept = getYintercept(lineStart, lineEnd);
			x = (yintercept - getYintercept(line2Start, line2End)) / (slope2 - slope1);
			y = (slope1 * x) + yintercept;
		}
		return new Point((int) x, (int) y);
	}

	private static double getYintercept(Point lineStart, Point lineEnd)
	{
		double a1 = lineStart.getX();
		double b1 = lineStart.getY();
		double a2 = lineEnd.getX();
		double b2 = lineEnd.getY();
		return (b1 * a2 - a1 * b2) / (a2 - a1);
	}

	private static double getXintercept(Point lineStart, Point lineEnd)
	{
		double a1 = lineStart.getX();
		double b1 = lineStart.getY();
		double a2 = lineEnd.getX();
		double b2 = lineEnd.getY();
		return (b1 * a2 - a1 * b2) / (b1 - b2);
	}

	private static Double getSlope(Point lineStart, Point lineEnd)
	{
		double denominator = lineEnd.getX() - lineStart.getX();
		double numerator = lineEnd.getY() - lineStart.getY();
		return (numerator / denominator);
	}

	public static Point getPointonSchem(Point src, Point dest, Point point, int pinspacing)
	{
		double xDelta = dest.x - src.x;
		double yDelta = dest.y - src.y;
		double delta = ((point.x - src.x) * xDelta + (point.y - src.y) * yDelta) / (xDelta * xDelta + yDelta * yDelta);
		Point intersection = new Point(Math.floorDiv((int) Math.round(src.x + delta * xDelta), pinspacing) * pinspacing,
				Math.floorDiv((int) Math.round(src.y + delta * yDelta), pinspacing) * pinspacing);
		if (intersection == src && xDelta == 0) {
			intersection.translate(0, pinspacing);
		}
		else if (intersection == src && yDelta == 0) {
			intersection.translate(pinspacing, 0);
		}
		return intersection;
	}

	public static boolean determineMatedConnectorMaps(IPinList symDev,
			Set<IPinList> atachedPinLists, Map<IDevicePin, IAbstractSchemPin> pintoPinMap,
			SetMap<IConnector, IDevicePin> pintoConnectorMap)
	{
		boolean success = true;
		for (IPinList connector : atachedPinLists) {
			chs.cof.logical.cable.IPinList connectivity = connector.getConnectivity();
			if ((connectivity instanceof IConnector) && !(connectivity instanceof IDeviceConnector)) {
				PinListConnectionHelper helper =
						ConnectionHelper.createInstance(symDev, connector);
				if (helper == null) {
					success = false;
					break;
				}
				IConnector attachedConnector = (IConnector) connectivity;
				pintoConnectorMap.create(attachedConnector);
				Set<IGfxObject> pins = ((BaseDeviceConnectionHelper) helper).determinePossibleDevicePins();
				for (IGfxObject obj : pins) {
					if (obj instanceof IPin) {
						IGfxObject matchingPin = helper.getMatchingPin((IAbstractSchemPin) obj);
						if (matchingPin != null && matchingPin instanceof IPin) {
							IDevicePin devicePin = (IDevicePin) ((IConnectivityRef) obj).getConnectivity();
							pintoPinMap.put(devicePin, (IAbstractSchemPin) matchingPin);
							pintoConnectorMap.add(attachedConnector, devicePin);
						}
					}
				}
			}
		}
		return success;
	}

	public static boolean isCornerPin(IExtent nonTextExtent, ILocation pinAbsLocation)
	{
		int x = nonTextExtent.getX();
		int y = nonTextExtent.getY();
		int w = nonTextExtent.getWidth();
		int h = nonTextExtent.getHeight();
		return pinAbsLocation.orthogonalDistance(new Location(x, y)) == 0 ||
				pinAbsLocation.orthogonalDistance(new Location(x + w, y)) == 0 ||
				pinAbsLocation.orthogonalDistance(new Location(x + w, y + h)) == 0 ||
				pinAbsLocation.orthogonalDistance(new Location(x, y + h)) == 0;
	}

	public static Point findNearestPlaceHolder(Point point, List<Point> availLocations)
	{
		Point required = point;
		Point current = availLocations.get(0);
		double mindist = getDistance(required, current);
		int choice = 0;
		for (int index = 1; index < availLocations.size(); index++) {
			current = availLocations.get(index);
			double distance = getDistance(required, current);
			if (mindist > distance) {
				choice = index;
				mindist = distance;
			}
		}
		required = availLocations.get(choice);
		return required;
	}

	public static double getDistance(Point p1, Point p2)
	{
		return Point2D.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
	}

	public static int findClosetAvailPoint(int index, List<Point> availPoints, Set<Point> usedLocations)
	{
		int front;
		int back;
		for (front = index; front >= 0; --front) {
			if (!usedLocations.contains(availPoints.get(front))) {
				break;
			}
		}
		for (back = index; back < availPoints.size(); ++back) {
			if (!usedLocations.contains(availPoints.get(back))) {
				break;
			}
		}
		if (front < 0 || front == index) {
			return back;
		}
		if (back >= availPoints.size() || back == index) {
			return front;
		}
		return index - front > back - index ? back : front;
	}
}
