/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.shared;

import chs.caf.CAFUtils;
import chs.cof.logical.shared.ISharedPinList;
import chs.system.FactoryMgr;
import chs.utility.ui.PinSelectionCommonPanel;
import chs.utility.ui.PinSelectionConfigurationParams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.List;
import java.util.function.Consumer;

/**
 * Class to create add shared port dialog in Capture/Architect
 */
public class AddSharedPortDialog extends AddSharedPinDialog
{

	public AddSharedPortDialog(Frame frame, String title,
			@Nullable ISharedPinList sharedPinList)
	{
		super(frame, title, sharedPinList);
	}

	@NotNull @Override protected PinSelectionCommonPanel getPinSelectionCommonPanel()
	{
		if (getSharedPinList() != null) {
			return new PinSelectionCommonPanel(this, FactoryMgr.getDrawFactory(),
					CAFUtils.getInstance().getCommonFactory(),
					new SharedDeviceSymbolTreeController(),
					new Consumer<List<?>>()
					{
						@Override public void accept(List<?> t)
						{
							getOkButton().setEnabled(!t.isEmpty());
							placeAsStackButtonStatusUpdate(t);
						}
					}, getEscapeListener())
			{
				@NotNull @Override protected PinSelectionConfigurationParams.PinType getPinType()
				{
					return PinSelectionConfigurationParams.PinType.PORT;
				}
			};
		}
		return super.getPinSelectionCommonPanel();
	}
}
