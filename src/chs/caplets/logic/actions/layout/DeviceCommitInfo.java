/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.layout;

import chs.cof.draw.ITransform;
import chs.common.ILocation;
import org.jetbrains.annotations.NotNull;

import java.awt.Point;
import java.awt.geom.AffineTransform;

/**
 * @author chandras on 19-10-2019.
 */
public class DeviceCommitInfo implements IDeviceCommitInfo
{

	private Point m_location;
	private AffineTransform m_transform;

	public DeviceCommitInfo(@NotNull ILocation location, @NotNull ITransform transform)
	{
		m_location = new Point(location.getX(), location.getY());
		m_transform = new AffineTransform(transform.getAffineTransform());
	}

	@NotNull public Point getLocation()
	{
		return m_location;
	}

	@NotNull public AffineTransform getTransform()
	{
		return m_transform;
	}
}
