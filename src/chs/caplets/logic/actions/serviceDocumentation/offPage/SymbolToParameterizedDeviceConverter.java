/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

package chs.caplets.logic.actions.serviceDocumentation.offPage;

import chs.caf.caplet.action.IActionEnum;
import chs.caplets.logic.actions.ConverSymbolToParamCommand;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IGuard;
import chs.system.ICreationDeletionHelper;
import chs.utility.helpers.CreationDeletionHelper;

import java.util.List;

/**
 * Converts symbol devices into parameterized devices
 * Works on Fetched objects, converts only symbol devices, excludes grounds
 * Converts only if at least one pin is not there in the signal path, if all the pins are part of the signal then we do not convert to parameterized device
 */
class SymbolToParameterizedDeviceConverter implements ISymbolToParameterizedConverter
{

	private ISchemDiagram targetDiagram;

	SymbolToParameterizedDeviceConverter(ISchemDiagram targetDiagram)
	{
		this.targetDiagram = targetDiagram;
	}

	public boolean convert(List<IPinList> pinListsToConvert)
	{
		pinListsToConvert
				.forEach(this::convert);
		return true;
	}

	private boolean convert(IPinList pinList)
	{
		ICreationDeletionHelper newCDH = CreationDeletionHelper.getTheCreationHelper().createEmptyCDH();
//		try (IGuard ignore = CreationDeletionHelper.createNullDisableUndoGuard()) {
		try (IGuard ignore = CreationDeletionHelper.createReplacementCreationDeletionHelperGuard(newCDH)) {
			return doConvert(pinList);
		}
	}

	private boolean doConvert(IPinList pinList)
	{
		ConverSymbolToParamCommand command = new ConverSymbolToParamCommand(targetDiagram);
		IActionEnum actionEnum = command.doOnActivate(pinList);
		if (actionEnum != IActionEnum.eCanceled) {
			command.doOnTerminate();
		}
		else {
			return false;
		}
		command.cleanUp();
		return true;
	}
}
