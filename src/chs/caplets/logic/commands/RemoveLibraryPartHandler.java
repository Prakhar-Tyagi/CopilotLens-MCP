/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.commands;

import chs.caf.caplet.helpers.PropertiedSetHelper;
import chs.cof.library.IFootprintable;
import chs.cof.logical.IRemoveLibraryPartHandler;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.logical.shared.SharedPinListHelper;
import chs.cof.parts.ILibraryCavity;
import chs.cof.parts.ILibraryObject;
import chs.common.IProperty;
import chs.common.IPropertyIterator;
import chs.common.IUIDObject;
import chs.utilities.AppInfo;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.PropertyHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * remove library part logic
 */

public class RemoveLibraryPartHandler implements IRemoveLibraryPartHandler
{

	@Override public void removeLibraryPart(@NotNull ILogicObject logObj, boolean retainFootprintIfNotChanged,
			@NotNull Function<IDevice, Boolean> isFootPrintIntact, @Nullable IUIDObject schemObject)
	{
		ILibraryObject existingLibraryObject = null;
		if (logObj instanceof chs.cof.logical.cable.IPinList) {
			existingLibraryObject = (ILibraryObject) logObj.getLibraryObject();
		}
		// this will clear the libraryref & part number (& customer/supplier info)
		logObj.assignLibraryDetails(null);

		// copy across any analysis model attribute that is set on the library
		// part
		if (!AppInfo.isLogic()) {
			logObj.setAnalysisModel(null);
		}

		if (logObj instanceof chs.cof.logical.cable.IPinList) {
			//
			// Got to clean properties off the pins too. This will not be explicit.
			//
			chs.cof.logical.cable.IPinList pl = (chs.cof.logical.cable.IPinList) logObj;
			if (logObj instanceof IDevice) {
				boolean retainFootprint = retainFootprintIfNotChanged && isFootPrintIntact.apply((IDevice) logObj);
				if (!retainFootprint) {
					((IFootprintable) logObj).removeFootprint();
				}
			}
			if (existingLibraryObject != null) {
				Map<String, ILibraryCavity> cavityMap = new HashMap<String, ILibraryCavity>();
				for (ILibraryCavity lcav : LibraryHelper.getCavities(existingLibraryObject)) {
					cavityMap.put(lcav.getName(), lcav);
				}
				if (schemObject instanceof IPinList && !retainFootprintIfNotChanged) {
					for (IAbstractPin pin : pl.getPins()) {
						ILibraryCavity lc = cavityMap.get(pin.getName());
						if (lc != null) {
							for (IPropertyIterator ipitr = lc.getProperties(); ipitr.hasNext(); ) {
								IProperty prop = ipitr.next();
								IProperty pinprop = pin.findPropertyByName(prop.getName());
								PropertiedSetHelper.removeProperty(pin, pinprop);
							}
						}
					}
				}
				// Clear the library derived attributes
				PropertyHelper.clearCavityAttributes(pl, existingLibraryObject);
			}
			//dts0100972050
			ISharedPinList spl = pl.getSharedPinList();
			if (pl instanceof IDevice && spl != null) {
				SharedPinListHelper.removeLibraryAssociatedSymbolFromSPL(existingLibraryObject, spl);
			}
		}
	}
}
