/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IConnectivityInfo;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import javafx.scene.image.Image;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Single ended filter
 */
public class OneEndedConductorFilter implements IBatchShareCustomFilter
{

	public static final String ID = "one-ended";
	public static final String TOOLTIP =
			ResourceMgr.getString(OneEndedConductorFilter.class, "OneEndedConductorFilter.tooltip");
	public static final String ICON_PATH = "chs/images/javafx_ui/one-ended-conductor-small.png";

	@NotNull @Override public String getId()
	{
		return ID;
	}

	@NotNull @Override public String getTooltipString()
	{
		return TOOLTIP;
	}

	@NotNull @Override public Predicate<? super IBatchShareRow> getPredicate()
	{
		return (batchShareRow) -> {
			IConnectivityInfo connectivityInfo = batchShareRow.getConnectivityInfo();
			return connectivityInfo != null && connectivityInfo.getConnectedPinUIDs().size() == 1;
		};
	}

	@Nullable @Override public Image getImage()
	{
		return CHSImageLoader.loadJFXImage(ICON_PATH);
	}
}
