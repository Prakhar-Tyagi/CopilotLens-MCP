package chs.caplets.logic.actions;

import chs.cof.logical.schem.IPinList;
import chs.common.IExtent;
import chs.common.ILocation;
import chs.system.FactoryMgr;
import chs.utility.helpers.PinListHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The behavior of this class is dependent upon the assumption that the corner are treated as either left or right. the
 * corner side is never top or bottom. this assumption is enforced here because the expansion of pinlist after
 * termination of action behaves according to this rule.
 */
public class PinPlacementValidityEvaluator
{

	@NotNull private IExtent m_effectivePinListExtent;
	private boolean m_leftClosed = false;
	private boolean m_rightClosed = false;
	private boolean m_topClosed = false;
	private boolean m_bottomClosed = false;

	private boolean m_ltCornerPlaced = false;
	private boolean m_rtCornerPlaced = false;
	private boolean m_lbCornerPlaced = false;
	private boolean m_rbCornerPlaced = false;
	@NotNull private IPinList m_pinList;

	public PinPlacementValidityEvaluator(@NotNull IPinList pinList,
			@NotNull IExtent pinListExtent)
	{
		m_pinList = pinList;
		m_effectivePinListExtent = pinListExtent;
	}

	public PinPlacementValidityEvaluator getClone()
	{
		PinPlacementValidityEvaluator clone =
				new PinPlacementValidityEvaluator(m_pinList, m_effectivePinListExtent.getClone());

		clone.m_leftClosed = m_leftClosed;
		clone.m_rightClosed = m_rightClosed;
		clone.m_topClosed = m_topClosed;
		clone.m_bottomClosed = m_bottomClosed;

		clone.m_ltCornerPlaced = m_ltCornerPlaced;
		clone.m_rtCornerPlaced = m_rtCornerPlaced;
		clone.m_lbCornerPlaced = m_lbCornerPlaced;
		clone.m_rbCornerPlaced = m_rbCornerPlaced;
		return clone;
	}

	public boolean tryPlace(@NotNull Collection<Point> absPoints)
	{
		if (absPoints.isEmpty()) {
			return false;
		}
		int left = m_effectivePinListExtent.getLeft();
		int right = m_effectivePinListExtent.getRight();
		int top = m_effectivePinListExtent.getTop();
		int bottom = m_effectivePinListExtent.getBottom();

		for (Point absPoint : absPoints) {
			Point point = PinListHelper.getRelativeToPinList(m_pinList, absPoint);

			int x = point.x;
			int y = point.y;

			//check if the point is inside the extent.
			if (x > left && x < right && y < top && y > bottom) {
				return false;
			}

			//check the left side fidelity.
			if (m_leftClosed && x < left) {
				return false;
			}

			//check the right side fidelity.
			if (m_rightClosed && x > right) {
				return false;
			}

			//check the top side fidelity.
			if (m_topClosed && y > top) {
				return false;
			}

			//check the bottom side fidelity.
			if (m_bottomClosed && y < bottom) {
				return false;
			}

			//check the left-top corner fidelity
			if (m_ltCornerPlaced && x < left && y > top) {
				return false;
			}

			//check the left-bottom corner fidelity
			if (m_lbCornerPlaced && x < left && y < bottom) {
				return false;
			}

			//check the right-top corner fidelity
			if (m_rtCornerPlaced && x > right && y > top) {
				return false;
			}

			//check the right-bottom corner fidelity
			if (m_rbCornerPlaced && x > right && y < bottom) {
				return false;
			}
		}

		//do a temporary placement and check for corners.
		PinPlacementValidityEvaluator clone = getClone();
		clone.placed(absPoints);

		boolean leftClosed = clone.m_leftClosed;
		boolean rightClosed = clone.m_rightClosed;
		boolean topClosed = clone.m_topClosed;
		boolean bottomClosed = clone.m_bottomClosed;
		boolean ltCornerPlaced = clone.m_ltCornerPlaced;
		boolean rtCornerPlaced = clone.m_rtCornerPlaced;
		boolean lbCornerPlaced = clone.m_lbCornerPlaced;
		boolean rbCornerPlaced = clone.m_rbCornerPlaced;

		//check the left-top corner fidelity
		if (ltCornerPlaced && topClosed && leftClosed) {
			return false;
		}

		//check the left-bottom corner fidelity
		if (lbCornerPlaced && bottomClosed && leftClosed) {
			return false;
		}

		//check the right-top corner fidelity
		if (rtCornerPlaced && topClosed && rightClosed) {
			return false;
		}

		//check the right-bottom corner fidelity
		if (rbCornerPlaced && bottomClosed && rightClosed) {
			return false;
		}

		//otherwise its free to place.
		return true;
	}

	public void placed(@NotNull Collection<Point> absPoints)
	{
		if (absPoints.isEmpty()) {
			return;
		}
		int left = m_effectivePinListExtent.getLeft();
		int right = m_effectivePinListExtent.getRight();
		int top = m_effectivePinListExtent.getTop();
		int bottom = m_effectivePinListExtent.getBottom();

		List<Point> impactedPoints = new ArrayList<>();

		if (m_ltCornerPlaced) {
			impactedPoints.add(new Point(left, top));
		}
		if (m_rtCornerPlaced) {
			impactedPoints.add(new Point(right, top));
		}
		if (m_lbCornerPlaced) {
			impactedPoints.add(new Point(left, bottom));
		}
		if (m_rbCornerPlaced) {
			impactedPoints.add(new Point(right, bottom));
		}

		//first update the effective extent before detrmining the closed boundaries.
		for (Point absPoint : absPoints) {
			Point point = PinListHelper.getRelativeToPinList(m_pinList, absPoint);
			impactedPoints.add(point);
			ILocation loc = FactoryMgr.getCommonFactory().constructLocation(point.x, point.y);
			m_effectivePinListExtent.addUnionLocation(loc);
		}

		left = m_effectivePinListExtent.getLeft();
		right = m_effectivePinListExtent.getRight();
		top = m_effectivePinListExtent.getTop();
		bottom = m_effectivePinListExtent.getBottom();

		m_ltCornerPlaced = false;
		m_rtCornerPlaced = false;
		m_lbCornerPlaced = false;
		m_rbCornerPlaced = false;
		for (Point impactedPoint : impactedPoints) {
			int x = impactedPoint.x;
			int y = impactedPoint.y;
			//check the left-top corner fidelity
			if (x == left && y == top) {
				m_ltCornerPlaced = true;
			}
			//check the left-bottom corner fidelity
			else if (x == left && y == bottom) {
				m_lbCornerPlaced = true;
			}
			//check the right-top corner fidelity
			else if (x == right && y == top) {
				m_rtCornerPlaced = true;
			}
			//check the right-bottom corner fidelity
			else if (x == right && y == bottom) {
				m_rbCornerPlaced = true;
			}
			else if (x == left) {
				m_leftClosed = true;
			}
			else if (x == right) {
				m_rightClosed = true;
			}
			else if (y == top) {
				m_topClosed = true;
			}
			else if (y == bottom) {
				m_bottomClosed = true;
			}
		}
	}
}
