/*
 * Copyright 2016 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.shared;

import chs.cof.logical.ILogicDesign;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.ISharedPinReservationView;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * This interface defines behaviour for objects that are providing information about shared pinlist.
 */
public interface ISharedPinListInfoProvider
{

	boolean selectPinList(ILogicDesign design, @Nullable ISharedPinList spl, ISharedPinReservationView sharedpinview,
			@NotNull IPlacementOptionParams params);

	boolean selectPinList(ILogicDesign design, @Nullable PinListTypeEnum pltype, @NotNull IPlacementOptionParams params);

	ISharedPinList getSharedPinList();

	boolean getAutoGenerate();

	int getNumUsedPins();

	boolean getReference();

	Collection<IPinProxy> getUsedPins();

	void cleanUp();

	boolean getPlaceAsStack();

	boolean getPlaceAsGroup();

	boolean getWithConductor();
}
