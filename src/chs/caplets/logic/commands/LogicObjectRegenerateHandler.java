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

import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.ILogicObjectRegenerateHandler;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemOtherComponent;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.symbol.ISymbolRef;
import chs.cofUtils.parameterized.BackshellGraphicsRebuilder;
import chs.cofUtils.parameterized.DefaultGeneratorDCFeedback;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.CHSConstants;
import chs.utilities.CommonUtils;
import chs.utility.DiagramHelper;
import chs.utility.logic.LogicUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * logic object regeneration handler
 */
public class LogicObjectRegenerateHandler implements ILogicObjectRegenerateHandler
{

	public void regenerate(@NotNull ILogicObject logObj, boolean preserveIncludeOnBOM,
			@NotNull Function<IDevice, Boolean> rebuildFootPrintForDeviceConnectors,
			@Nullable IUIDObject schemObject,
			@Nullable ISchemDiagram diagram)
	{
		regenerate(logObj, preserveIncludeOnBOM, rebuildFootPrintForDeviceConnectors, null, schemObject, diagram);
	}

	public void regenerate(@NotNull ILogicObject logObj, boolean preserveIncludeOnBOM,
			@NotNull Function<IDevice, Boolean> rebuildFootPrintForDeviceConnectors,
			@Nullable DefaultGeneratorDCFeedback feedback,
			@Nullable IUIDObject schemObject,
			@Nullable ISchemDiagram diagram)
	{
		Generator generator = Generator.getGenerator();
		generator.setPreserveIncOnBOM(preserveIncludeOnBOM);
		GeneratorParameters gp = setupGeneratorParameters(diagram);

		ISharedObject spl = logObj.getSharedObject();
		if (spl instanceof ISharedDevice) {
			ISharedDevice sd = (ISharedDevice) spl;
			IDevice device = CommonUtils.cast(logObj, IDevice.class);
			assert device != null;
			//
			// Regenerate as they are now no longer there...
			// Call this even if all we need is to clear away the old footprint...
			//
			generator.regenerateSharedDeviceConnectors(device, sd, FactoryMgr.getCommonFactory());
		}

		if (logObj instanceof IDevice) {
			LogicUtils.ensureDeviceConnectorConsistency(((IDevice) logObj), generator, gp,
					rebuildFootPrintForDeviceConnectors.apply((IDevice) logObj), feedback);
		}
		else if (logObj instanceof IConnector) {
			if (schemObject instanceof IPinList) {
				generator.generateConnector((IPinList) schemObject, gp);
			}
		}
		else {
			if (logObj instanceof IBackshell) {
				IBackshell bs = (IBackshell) logObj;
				ISymbolRef sref = bs.getSymbolRef();
				if (diagram != null) {
					for (IDiagramObjectIterator doiter = diagram.getRepresentations(bs.getOwner().getUID());
							doiter.hasNext(); ) {
						IDiagramObject dobj = doiter.next();
						if (dobj instanceof IPinList) {
							new BackshellGraphicsRebuilder().rebuildAllBackshellGraphics((IPinList) dobj, sref);
						}
					}
				}
			}
		}
		if (schemObject instanceof ISchemOtherComponent) {
			generator.generateOtherComponent((ISchemOtherComponent) schemObject, gp);
		}
	}

	@NotNull private GeneratorParameters setupGeneratorParameters(@Nullable ISchemDiagram diagram)
	{
		//
		// Try to get the preferences and use those for initialization...
		//
		GeneratorParameters gp;
		if (diagram == null) {
			gp = new GeneratorParameters(CHSConstants.PIN_SPACING);
		}
		else {
			gp = DiagramHelper.createGeneratorParameters(diagram);
		}
		return gp;
	}
}
