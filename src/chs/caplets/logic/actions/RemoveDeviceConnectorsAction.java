/*
 * Copyright 2006 Mentor Graphics Corporation
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
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.Selection;
import chs.caf.caplet.selection.SelectionIterator;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.icd.IDeviceICD;
import chs.cof.library.FootprintSource;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.concurrency.ILogicConcurrencyController;
import chs.cof.logical.footprint.user.IPrivilegedUserFootprintable;
import chs.cof.logical.footprint.user.UserDeviceFootprintFactory;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedDevice;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedObjectMgr;
import chs.cof.parts.ILibraryDeviceFootprint;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.cofUtils.parameterized.DefaultGeneratorDCFeedback;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.common.ICommonFactory;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Choice;
import chs.utilities.ui.messaging.Question;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.DiagramHelper;
import chs.utility.ICDUtils;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoveDeviceConnectorsAction extends ControllerActionRT implements ICtxMenuProvider
{

	public RemoveDeviceConnectorsAction(ICapletController controller)
	{
		super(controller);
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eCompleted;
	}

	public boolean isEnabled()
	{
		if (super.isEnabled() && getCapletModel().isEditable()) {
			Set<IDevice> operands = getOperands(getPreSelections());
			return !operands.isEmpty();
		}
		return false;
	}

	@Nullable private IDevice checkIfDevice(IUIDObject selObject)
	{
		IDevice device = CommonUtils.cast(selObject, IDevice.class);
		if (device == null) {
			IPinList pinList = CommonUtils.cast(selObject, IPinList.class);
			device = CommonUtils.cast((pinList != null ? pinList.getConnectivity() : null), IDevice.class);
		}
		return device;
	}

	@NotNull protected SelectSet getPreSelections()
	{
		return getController().getSelectMgr().getPreSelections();
	}

	protected Set<IDevice> getOperands(@NotNull SelectSet sset)
	{
		Set<IDevice> operands = new HashSet<>();
		Model model = (Model) getController().getCapletModel();
		if (!model.isEditable()) {
			return operands;
		}

		ILogicDesign logicDesign = CommonUtils.cast(model.getDesign(), ILogicDesign.class);
		IBaseDiagram diagram = getDiagram();

		if (logicDesign == null || diagram == null) {
			return operands;
		}

		if (ILogicConcurrencyController.isUnderLogicConcurrencyLimitation(logicDesign)) {
			return operands;
		}

		IProject project = logicDesign.getProject();
		if (project == null) {
			return operands;
		}

		IBuildListMgr buildListMgr = project.getBuildListMgr();
		SelectionIterator iter = sset.getSelected();
		while (iter.hasNext()) {
			Selection sel = iter.getNext();
			IUIDObject selObject = sel.getObject();
			IDevice device = checkIfDevice(selObject);
			if (device == null) {
				continue;
			}
			ISharedDevice sharedDevice = CommonUtils.cast(device.getSharedObject(), ISharedDevice.class);
			if (sharedDevice != null) {
				if (sharedDevice.getNumDeviceConnectors() < 1) {
					continue;
				}
			}
			else {
				if (device.getNumDeviceConnectors() < 1) {
					continue;
				}
			}
			ILibraryDeviceFootprint footprint = device.getFootprint();
			if (footprint != null) {
				//this action is only for non-footprinted devices only.
				continue;
			}

			Set<IDeviceICD> matchingICDs =
					ICDUtils.getMatchingICDs(device, logicDesign, buildListMgr.getActiveBuildList(), true);
			if (!matchingICDs.isEmpty()) {
				continue;
			}

			//this action is only for non-icd devices only.
			operands.add(device);
		}
		return operands;
	}

	@Nullable protected IBaseDiagram getDiagram()
	{
		return CAFUtils.getInstance().getActiveDiagram();
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		Set<IDevice> operands = getOperands(getPreSelections());
		boolean sharedDevicesSelected = false;

		final IOutputWindow output = getOutputWindow();
		DefaultGeneratorDCFeedback feedback = new DefaultGeneratorDCFeedback()
		{
			public void outputMessage(String s, boolean writtenSomething)
			{
				output.sendMessage(s, getOutputTabName(), writtenSomething);
			}
		};

		for (IDevice logicDevice : operands) {
			if (logicDevice.isShared()) {
				sharedDevicesSelected = true;
			}
		}

		if (sharedDevicesSelected) {
			ResourceBasedMessageContent messageContent =
					new ResourceBasedMessageContent(RemoveDeviceConnectorsAction.class,
							"RemoveDeviceConnectorsAction.sharedDeviceConnector");
			Choice chDelete = createChoice("RemoveDeviceConnectorsAction.sharedDeviceConnector.choice.delete");
			Choice chCancel = createChoice("RemoveDeviceConnectorsAction.sharedDeviceConnector.choice.cancel");
			Choice response = Question.show(messageContent, chDelete, chCancel);
			if (response == chCancel) {
				return false;
			}
		}

		doRemoveDeviceConnectors(operands, feedback);

		return true;
	}

	private void doRemoveDeviceConnectors(@NotNull Set<IDevice> operands, @NotNull DefaultGeneratorDCFeedback feedback)
	{
		Model model = (Model) getController().getCapletModel();
		ISchemDiagram diagram = model.getDiagram();
		ICommonFactory commonFactory = FactoryMgr.getCommonFactory();
		GeneratorParameters gp = DiagramHelper.createGeneratorParameters(diagram);
		Generator generator = Generator.getGenerator();
		Set<ISharedObject> modifiedSharedObjects = new HashSet<>();
		for (IDevice logicDevice : operands) {
			boolean shouldRebuildCableDevice = true;
			ISharedDevice sharedDevice = CommonUtils.cast(logicDevice.getSharedObject(), ISharedDevice.class);
			//implant an empty footprint otherwise it will not be cleared.
			UserDeviceFootprintFactory factory = new UserDeviceFootprintFactory();
			IPrivilegedUserFootprintable userFootprintable =
					CommonUtils.cast(logicDevice, IPrivilegedUserFootprintable.class);
			if (sharedDevice != null) {
				if (sharedDevice.lock()) {
					try {
						if (userFootprintable != null) {
							userFootprintable.setDeviceFootprint(factory.generateFootprint(sharedDevice));
						}
						generator.regenerateSharedDeviceConnectors(logicDevice, sharedDevice, commonFactory);
						modifiedSharedObjects.add(sharedDevice);
					}
					finally {
						if (userFootprintable != null) {
							userFootprintable.setDeviceFootprint(null);
						}
						sharedDevice.saveAndUnlock();
					}
				}
				else {
					shouldRebuildCableDevice = false;
					String message = ResourceMgr.getString(RemoveDeviceConnectorsAction.class,
							"RemoveDeviceConnectorsAction.lock.fail", HTMLHelper.link(logicDevice));
					feedback.outputMessage(message, true);
				}
			}
			if (shouldRebuildCableDevice) {
				try {
					if (sharedDevice == null && userFootprintable != null) {
						userFootprintable.setDeviceFootprint(factory.generateFootprint(logicDevice));
					}
					logicDevice.setDeviceSideFootprintSource(FootprintSource.getDefault());
					// Syles get applied indirectly when the device connectors are rebuilt, but only for
					// the device connectors
					generator.rebuildDeviceConnectors(logicDevice, gp, feedback, true, Generator.REGENERATE_PROPERTIES, false);
				}
				finally {
					if (sharedDevice == null && userFootprintable != null) {
						userFootprintable.setDeviceFootprint(null);
					}
				}
			}
		}
		if (!modifiedSharedObjects.isEmpty()) {
			Map<ISharedObjectMgr, Set<IUID>> modifiedSharedObjectsToMgr = modifiedSharedObjects.stream()
					.collect(Collectors.groupingBy(ISharedObject::getSharedObjectMgr,
							Collectors.mapping(ISharedObject::getUID, Collectors.toSet())));
			for (ISharedObjectMgr sharedObjectMgr : modifiedSharedObjectsToMgr.keySet()) {
				sharedObjectMgr.fireChangeEvent(modifiedSharedObjectsToMgr.get(sharedObjectMgr));
			}
			// Editing of shared objects is not undoable
			getController().getUndoableContainer().endEdit();
			getController().clearUndoQueue();
		}
	}

	@NotNull protected Choice createChoice(@NotNull String resourceKey)
	{
		return new Choice(this, resourceKey);
	}

	protected IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	@Override public String getActionUIClass()
	{
		return RemoveDeviceConnectorsActionUI.class.getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (isEnabled() && actionUI != null) {
			container.add(new ActionEntry(actionUI, null));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{
	}

	@Override public boolean shouldDisableUndoForNonUndoableChanges()
	{
		return true;
	}
}
