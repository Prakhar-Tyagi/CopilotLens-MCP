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

import chs.cof.logical.cable.IInlineJackConnector;
import chs.cof.logical.cable.ILogicObject;
import chs.utilities.ResourceMgr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author rmahato
 */
public class AttributeDetailPane extends BaseDetailPane
{

	private static final String PANE_TITLE =
			ResourceMgr.getString(AttributeDetailPane.class, "AttributeDetailPane.tableTitle.text");
	private static final String TABLE_ID = "attribute_details_table";

	public AttributeDetailPane(@Nullable ILogicObject selectedObject)
	{
		super(PANE_TITLE, TABLE_ID);
		setId("AttributeDetailPane");
		updateContent(selectedObject);
	}

	@NotNull @Override protected DetailsTableInfo createTableInfo(@Nullable ILogicObject selectedObject)
	{
		if (selectedObject instanceof IInlineJackConnector) {
			return new InlineAttributeDetailsInfo();
		}
		else {
			return new AttributeDetailsTableInfo();
		}
	}
}
