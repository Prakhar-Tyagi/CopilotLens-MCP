/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.inlineassist;

import java.awt.Point;
import java.util.Comparator;

public class NewConnectorDataComparator implements Comparator<NewConnectorData>
{

	@Override public int compare(NewConnectorData o1, NewConnectorData o2)
	{
		final Point centerPoint1 = o1.getExtent().getCenter();
		final Point centerPoint2 = o2.getExtent().getCenter();
		final boolean vertical1 = o1.isVertical();
		final boolean vertical2 = o2.isVertical();
		if (!vertical1 && vertical2) {
			return -1;
		}
		if (vertical1 && !vertical2) {
			return 1;
		}
		if (vertical1) {
			if (centerPoint1.x < centerPoint2.x) {
				return -1;
			}
			if (centerPoint1.x > centerPoint2.x) {
				return 1;
			}
			if (centerPoint1.y < centerPoint2.y) {
				return 1;
			}
			if (centerPoint1.y > centerPoint2.y) {
				return -1;
			}
		}
		else {
			if (centerPoint1.y < centerPoint2.y) {
				return 1;
			}
			if (centerPoint1.y > centerPoint2.y) {
				return -1;
			}
			if (centerPoint1.x < centerPoint2.x) {
				return -1;
			}
			if (centerPoint1.x > centerPoint2.x) {
				return 1;
			}
		}
		return 0;
	}
}
