/*
 * Copyright 2006-2017 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IPinList;
import chs.cof.parts.ILibraryDevice;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.ISymbolDef;
import chs.common.ISymbolledPin;
import chs.common.IUID;
import chs.ctf.caf.ui.IPlacementOptionParams;
import chs.ctf.caf.utils.IPinProxy;
import chs.utilities.CommonUtils;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.ICDUtils;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

public class CreateICDFromPlacedICDAction extends AddPinListAction
{

	public CreateICDFromPlacedICDAction(ICapletController controller)
	{
		super(controller);
	}

	@Nullable @Override protected IPinList getOperand()
	{
		IDeviceICD selectICD = getSelectICD();
		IDesign design = getDesign();
		if (selectICD != null && design != null) {
			IPinList matchingDevice = ICDUtils.getMatchingDevice(selectICD, design);
			if (matchingDevice != null && !matchingDevice.isShared()) {
				return matchingDevice;
			}
		}
		return null;
	}

	@Nullable private ILogicDesign getDesign()
	{
		ILogicModel model = getModel();
		return model != null ? CommonUtils.cast(model.getDesign(), ILogicDesign.class) : null;
	}

	@Nullable private ILogicModel getModel()
	{
		return CommonUtils.cast(getController().getCapletModel(), ILogicModel.class);
	}

	@Nullable private IDeviceICD getSelectICD()
	{
		SelectSet selections = getPreSelections();
		List<IICD> selectedObjects = selections.getSelectedObjects(IICD.class);
		if (selectedObjects.size() == 1) {
			ILogicDesign design = getDesign();
			if (design != null) {
				return design.getDesignICDContainer().constructDeviceICD(selectedObjects.iterator().next());
			}
		}
		return null;
	}

	@Override public String getActionUIClass()
	{
		return CreateICDFromPlacedICDActionUI.class.getName();
	}

	@Override protected AddSymbolledPinListAction createAddSymbolledPinListAction(ICapletController controller,
			IPinList pinlist, ISymbolDef symDef, IBlock block, Map<IUID, ISymbolledPin> map, boolean reference,
			boolean withConductor)
	{
		IDeviceICD selectICD = getSelectICD();
		if (selectICD == null) {
			throw new IllegalStateException("Selected ICD cannot be null");
		}
		return new AddSymbolledICDPinListAction(controller, pinlist, symDef, block, map, reference, selectICD, withConductor);
	}

	@Override protected CreateParameterizedObjectAction createAddParameterizedPinListAction(
			ICapletController controller, IPinList pinlist, List<IAbstractPin> pins, boolean autogenerate,
			boolean reference, boolean placeAsStack, boolean placeAsGroup, List<IPinProxy> pinProxies,
			boolean withConductor)
	{
		IDeviceICD selectICD = getSelectICD();
		if (selectICD == null) {
			throw new IllegalStateException("Selected ICD cannot be null");
		}
		return new AddParameterizedICDPinListAction(getController(), pinlist, pins, autogenerate, reference,
				placeAsStack, placeAsGroup, pinProxies, selectICD, withConductor);
	}

	@Override protected boolean pinSelectionDialogRequired(IPinList pinlist)
	{
		IDeviceICD selectICD = getSelectICD();
		return selectICD != null && !selectICD.getICDUsageDefinition().getPinSignalAssociations().isEmpty();
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		IDeviceICD selectICD = getSelectICD();
		IPinList pinList = getOperand();

		if (pinList != null && selectICD != null && !ICDUtils.arePinNamesMatching(pinList, selectICD)) {
			String resourceKeyRoot = "CreateICDFromPlacedICDAction.pinsMismatch";
			ResourceBasedMessageContent content = new ResourceBasedMessageContent(CreateICDFromPlacedICDAction.class,
					resourceKeyRoot);
			Message.show(PromptSeverity.ERROR, content);
			return IActionEnum.eCanceled;
		}

		ILibraryDevice transientLibraryDevice = null;
		try {
			transientLibraryDevice =
					PinListAddPinHelper.assignTransiantLibraryPart(selectICD, pinList);
			return super.onActivate(e);
		}
		finally {
			PinListAddPinHelper.removeTransiantLibraryPart(transientLibraryDevice, pinList);
		}
	}

	@NotNull @Override
	protected IPlacementOptionParams createPlacementOptionParams(@NotNull IPinList pinList, @Nullable ISymbolDef symDef)
	{
		IPlacementOptionParams params = super.createPlacementOptionParams(pinList, symDef);
		params.enableWithConductorOption(true, pinList.getProject());
		return params;
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
