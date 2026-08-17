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

import org.jetbrains.annotations.NotNull;

import java.awt.geom.AffineTransform;

/**
 * @author chandras on 19-10-2019.
 */
public enum PlacementAxisRotation
{
	Zero {
		public PlacementAxisRotation next()
		{
			return Ninety;
		}

		public PlacementAxisRotation prev()
		{
			return TwoSeventy;
		}
	},
	Ninety {
		@NotNull public AffineTransform getTransform()
		{
			final AffineTransform affineTransform = new AffineTransform();
			affineTransform.setToRotation(3 * Math.PI / 2);
			return affineTransform;
		}

		public boolean isYAxis()
		{
			return true;
		}

		public PlacementAxisRotation next()
		{
			return OneEighty;
		}
	},
	OneEighty {
		@NotNull public AffineTransform getTransform()
		{
			final AffineTransform affineTransform = new AffineTransform();
			affineTransform.setToRotation(Math.PI);
			return affineTransform;
		}

		public PlacementAxisRotation next()
		{
			return TwoSeventy;
		}

		public PlacementAxisRotation prev()
		{
			return Ninety;
		}
	},
	TwoSeventy {
		@NotNull public AffineTransform getTransform()
		{
			final AffineTransform affineTransform = new AffineTransform();
			affineTransform.setToRotation(Math.PI / 2);
			return affineTransform;
		}

		public boolean isYAxis()
		{
			return true;
		}

		public PlacementAxisRotation prev()
		{
			return OneEighty;
		}
	};

	@NotNull public AffineTransform getTransform()
	{
		return new AffineTransform();
	}

	public boolean isYAxis()
	{
		return false;
	}

	public PlacementAxisRotation next()
	{
		return Zero;
	}

	public PlacementAxisRotation prev()
	{
		return Zero;
	}
}
