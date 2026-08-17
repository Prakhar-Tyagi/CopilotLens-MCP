/*
 * Copyright 2005-2013 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared.autoshare;

import chs.caplets.logic.actions.shared.EditSharedPinListModel;
import chs.caplets.logic.actions.shared.helper.IShareMessageContextReporter;
import chs.caplets.logic.actions.shared.helper.PinMappingHandler;
import chs.cof.logical.ILogicDesign;
import org.jetbrains.annotations.NotNull;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jan 24, 2005 Time: 1:13:27 PM
 */
public class AutoMapView
{

	@NotNull private PinMappingHandler mHandler;
	private boolean isEnabled;

	public AutoMapView(@NotNull EditSharedPinListModel model, @NotNull ILogicDesign design,
			@NotNull IShareMessageContextReporter reporter, boolean extendedPinMatch, boolean mateCompatibilityCheck,
			boolean isBulkShare)
	{
		mHandler = new PinMappingHandler(model, design, reporter, extendedPinMatch, mateCompatibilityCheck, false,
				isBulkShare);
		isEnabled = mHandler.isMapperValid();
	}

	public void init()
	{
		mHandler.init();
	}

	public void setEnabled(boolean enabled)
	{
		isEnabled = enabled;
	}

	private boolean isEnabled()
	{
		return isEnabled;
	}

	public void associateAll()
	{
		// Needed for share into.
		if (isEnabled()) {
			if (mHandler.allowGenerateMapping()) {
				mHandler.associateAll(() -> false); // Do not create pins automatically
			}
		}
	}
}
