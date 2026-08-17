/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.cof.logical.cable.IInlineJackConnector;
import chs.utility.NamedObjectListUtils;
import chs.utility.ui.NamedObjectRenderer;

import javax.swing.JList;
import java.awt.Component;

public class SharedObjectListCellRenderer extends NamedObjectRenderer
{

	public Component getListCellRendererComponent(
			JList list,
			Object value,
			int index,
			boolean isSelected,
			boolean cellHasFocus)
	{
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof IInlineJackConnector) {
			IInlineJackConnector pl = (IInlineJackConnector) value;
			setText(pl.getName() + '|' + NamedObjectListUtils.convertNamedObjectListToString(pl.getMates()));
		}
		return this;
	}
}
