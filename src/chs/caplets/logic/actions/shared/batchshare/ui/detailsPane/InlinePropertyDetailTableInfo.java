/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.utilities.CommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

/**
 * @author rmahato
 */
public class InlinePropertyDetailTableInfo extends PropertyDetailTableInfo
{

	@NotNull @Override public Collection<DetailsTableInfo> getTableData(@Nullable ILogicObject selectedObject)
	{
		Collection<DetailsTableInfo> rows = super.getTableData(selectedObject);
		IInlineJackConnector jackConnector = CommonUtils.cast(selectedObject, IInlineJackConnector.class);
		if (jackConnector != null) {
			IConnector plugConnector = jackConnector.getMates().iterator().next();
			addPropertyDetailRows(Objects.requireNonNull(plugConnector), (Set<DetailsTableInfo>) rows);
		}

		return rows;
	}
}
