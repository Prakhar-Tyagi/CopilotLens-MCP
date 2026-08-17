/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.IConnectivityInfo;
import chs.caplets.logic.actions.shared.batchshare.IPropertyRow;
import chs.caplets.logic.actions.shared.batchshare.IShareRow;
import chs.utilities.AlphaNumComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Row element for batch share table
 */
public interface IBatchShareRow extends IShareRow, IPropertyRow, Comparable<IBatchShareRow>
{
	@NotNull String getMatchBy();

	@NotNull Action getAction();

	void setAction(Action action);

	@Nullable IConnectivityInfo getConnectivityInfo();

	@NotNull IBatchShareGroup getGroup();

	@Override
	default int compareTo(@NotNull IBatchShareRow o)
	{
		int result = AlphaNumComparator.compare(getMatchBy(), o.getMatchBy(), false);
		if (result == 0) {
			result = getAction().compareTo(o.getAction());
		}
		if (result == 0) {
			result = AlphaNumComparator.compare(getName(), o.getName(), false);
		}
		if (result == 0) {
			result = AlphaNumComparator.compare(getDesignName(), o.getDesignName(), false);
		}
		return result;
	}
}