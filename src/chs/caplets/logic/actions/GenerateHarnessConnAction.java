/*
 * Copyright 2006-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnActionHelper;
import chs.caplets.logic.actions.ghc.GenerateHarnessConnUtils;
import chs.cof.library.FootprintSource;
import chs.cof.library.IFootprintable;
import chs.cof.logical.FootprintUtils;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.HarnessConnectorGenerationEnum;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cofUtils.parameterized.GeneratorHCFeedback;
import chs.common.IUIDObject;
import chs.common.RefreshStatusEnum;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.AssemblyUtils;
import chs.utility.helpers.LibraryHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.Cursor;
import java.awt.event.ActionEvent;

public class GenerateHarnessConnAction extends ControllerActionRT
		implements ICtxMenuProvider
{

	private static Cursor m_cursor = CAFUtils.getInstance().loadCursor(Cursor.WAIT_CURSOR);
	protected GenerateHarnessConnActionHelper addHarnConnActionHelper;
	// Delegate action after shared device has been created
	private String m_ctxCommand;
	private final IOutputWindow output;
	protected GeneratorHCFeedback feedback;
	private ICapletController controller;
	private IPinList mPinList;

	public GenerateHarnessConnAction(ICapletController aController)
	{
		super(aController);
		controller = aController;
		// set up error message object
		output = CAFUtils.getInstance().getOutputWindow();
		feedback = new GeneratorHCFeedback()
		{
			public void outputMessage(String s, boolean writtenSomething)
			{
				output.sendMessage(s, getOutputTabName(), writtenSomething);
			}
		};

		addHarnConnActionHelper = new GenerateHarnessConnActionHelper(
				((ILogicModel) getController().getCapletModel()).getDiagram(), feedback,
				HarnessConnectorGenerationEnum.TypeManuallyGenerated);
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		IActionEnum result = IActionEnum.eCanceled;
		// clear the tab if it exists
		output.clearPane(feedback.getOutputTabName());

		IPinList pinList = getOperand(getController().getSelectMgr().getPreSelections());
		mPinList = pinList;


		// dts0100595263 Rejection: For some unknown reason SOMETIMES mDiagram of GHCHelper
		// is not null but does not have any valid values in it.
		// So setting the diagram for GHCHelper
		// This may not be the best solution but it works. May be GHCHelper should take
		// care of this issue in setUp. But GHCHelper::setUp is invoked in multiple paths
		// So I thought this is a safe solution for now.
		// Yet to investigate why mDiagram is not correctly populated in the first place
		// This can be reproduced on 2008.2 base version also. Not a side effect of earlier
		// code changes done for dts0100595263
		if (pinList != null) {
			if (pinList.getParent() instanceof ISchemDiagram) {
				addHarnConnActionHelper.setDiagram((ISchemDiagram) pinList.getParent());
			}
		}
		result = IActionEnum.eCompleted;
		return result;
	}


	protected void showDomainAccessInfoDialog()
	{
		String resourceKeyRoot = "GenerateHarnessConnAction.noDomainAccess";
		ResourceBasedMessageContent content = new ResourceBasedMessageContent(this, resourceKeyRoot);
		Message.show(PromptSeverity.ERROR, content);
	}

	public boolean onTerminate(boolean successful)
	{
		try {
			if (successful) {
				// lock pinlists of existing shared connectors that are in play
				// go do the work
				//addHarnConnActionHelper.generateHarnessConnectorsForPinlist(mPinList);
				if (mPinList.getSharedObject() == null) {
					addHarnConnActionHelper.generateHarnessConnectorsForDevice((IDevice) mPinList.getConnectivity());
				}
				else {
					addHarnConnActionHelper.generateHarnessConnectorsForSharedDevice(
							(ISharedPinList) mPinList.getSharedObject(), mPinList.getConnectivity());
				}
			}
		}
		finally {
			if (successful) {
				ICapletModel model = controller.getCapletModel();
				model.setModified(true);
				if (model instanceof ILogicModel) {
					((ILogicModel) model).getDesign().getProject().getSharedPinListMgr().fireChangeEvent();
				}
			}
		}

		return successful;
	}

	/**
	 * Return our matching ActionUI class
	 */
	public String getActionUIClass()
	{
		return GenerateHarnessConnActionUI.class.getName();
	}

	// Enabled if there are any IParameterized objects selected.
	public boolean isEnabled()
	{
		final IPinList operand = getOperand(getController().getSelectMgr().getPreSelections());
		boolean isEnabled = getController().getCapletModel().isEditable() && operand != null && super.isEnabled();

		chs.cof.logical.cable.IPinList device = operand != null ? operand.getConnectivity() : null;
		if (isEnabled && AssemblyUtils.getCOTSAssembly(device) != null) {
			m_disabledReason = ResourceMgr.getString(GenerateHarnessConnActionUI.class,
					"GenerateHarnessConnAction.notPossibleOnCOTSAssembly.text");
			isEnabled = false;
		}
		ILogicDesign design = device != null ? device.getLogicDesign() : null;
		if (isEnabled && design != null && ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(design)) {
			m_disabledReason = ResourceMgr.getString(GenerateHarnessConnActionUI.class,
					"GenerateHarnessConnAction.notPossibleUnderConcurrentEdit.text");
			isEnabled = false;
		}
		if (isEnabled && device != null && FootprintUtils.determineFootprintSource((IFootprintable) device) == FootprintSource.LIBRARY &&
				device.getLibraryObject() == null) {
			m_disabledReason = ResourceMgr.getString(GenerateHarnessConnActionUI.class,
					"GenerateHarnessConnAction.notPossibleInValidUID.text");
			isEnabled = false;
		}
		if(isEnabled && device != null && design != null && addHarnConnActionHelper.isUnsupportedAbstractionForGHC(design)){
			isEnabled = false;
		}
		return isEnabled;
	}

	// Put ourselves in the context menu if there are
	// any IParameterized objects selected.
	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (getOperand(selections) != null) {
			String shortDesc = (String) getActionUI().getValue(Action.SHORT_DESCRIPTION);
			if (m_ctxCommand == null || !m_ctxCommand.equalsIgnoreCase(shortDesc)) {
				// Make a private copy for command name
				m_ctxCommand = shortDesc;
			}
			container.add(new ActionEntry(getActionUI(), m_ctxCommand));
		}
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Nullable
	private IPinList getOperand(SelectSet selections)
	{
		// resrtict operation to a single IDevice pinlist

		IPinList operand = null;

		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {

			IUIDObject uidObj = iter.getNext();
			if (uidObj instanceof IPinList) {
				IPinList pl = (IPinList) uidObj;
				if (pl.getConnectivity() instanceof IDevice) {
					// this is a second candidate - we can't choose so bail out
					if (operand != null) {
						return null;
					}
					operand = pl;
				}
			}
		}

		// device found - further qualification....
		if (operand != null) {

			IDevice dev = (IDevice) operand.getConnectivity();

			// Device must have a harness connector footprint
			//device connector type of footprint will also be performing ghc now.
			if (FootprintUtils.determineFootprintType(dev) == ILibraryDeviceFootprint.FootprintType.UNDEFINED) {
				if (!LibraryHelper.hasRingTerminalHousingDefined(dev)) {
					return null;
				}
			}
			// eliminate 0-pin devices and devices with all pins already connected
			if (!GenerateHarnessConnUtils.hasConnectablePins(operand)) {
				return null;
			}
		}
		return operand;
	}

	public String getStatusbarText()
	{
		return addHarnConnActionHelper.getStatusbarText();
	}

	public boolean isPostTerminateValidationRequired()
	{
		return true;
	}

	@Override public Cursor getCursor()
	{
		return m_cursor;
	}
}
