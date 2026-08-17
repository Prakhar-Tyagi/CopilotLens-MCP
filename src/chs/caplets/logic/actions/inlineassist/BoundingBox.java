/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.inlineassist;

import chs.utilities.CHSConstants;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;

/**
 * The `BoundingBox` class represents a rectangular bounding box defined by four points.
 * It provides methods to expand the bounding box vertically or horizontally.
 */
public class BoundingBox
{
    private static int pinSpacing = CHSConstants.PIN_SPACING;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;

    public BoundingBox(@NotNull Point firstPoint, @NotNull Point secondPoint) {
        minX = Math.min(firstPoint.x, secondPoint.x);
        maxX = Math.max(firstPoint.x, secondPoint.x);
        minY = Math.min(firstPoint.y, secondPoint.y);
        maxY = Math.max(firstPoint.y, secondPoint.y);
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public void expandVertical(boolean isGrowingBottom)
    {
        if (isGrowingBottom ) {
            minY -= pinSpacing;
        } else {
            maxY += pinSpacing;
        }
    }

    public void expandHorizontal( boolean isGrowingLeft)
    {
        if (isGrowingLeft) {
            minX -= pinSpacing;
        } else {
            maxX += pinSpacing;
        }
    }
}
