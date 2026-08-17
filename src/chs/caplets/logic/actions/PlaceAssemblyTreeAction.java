/*
 * Copyright 2015-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.IActionable;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.PlaceAssemblyTreeActionHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeContainer;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IAssembly;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.parts.ILibraryAssembly;
import chs.cof.parts.ILibraryAssemblyDetails;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.common.IUID;
import chs.utilities.AppInfo;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LibraryHelper;
import chs.utility.logic.ILogicModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 8/1/15
 */
public class PlaceAssemblyTreeAction extends ControllerActionRT
{

	protected ILibraryPartSelection m_libAssembly;
	private IDesign m_design;
	private IAssembly m_assembly;
	private boolean m_isSuccess = false;

	public PlaceAssemblyTreeAction(ICapletController controller)
	{
		super(controller);
		m_design = ((ILogicModel) getController().getCapletModel()).getDesign();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_libAssembly = getSelectedLibraryAssembly();

		if (m_libAssembly == null) {
			return IActionEnum.eCanceled;
		}
		return isPartSelectionValid() ? IActionEnum.eCompleted : IActionEnum.eCanceled;
	}

	private boolean isPartSelectionValid()
	{
		boolean isValid = false;
		ILibraryAssembly libraryAssembly = (ILibraryAssembly) m_libAssembly.getSelectedObject();
		IProject project = m_design.getProject();
		if (libraryAssembly != null && project != null) {
			isValid = isLibraryAssemblyChildValid(project, libraryAssembly, libraryAssembly,
					error -> displaceMessageInOutputWindow(error));
		}
		return isValid;
	}

	private boolean isLibraryAssemblyChildValid(IProject project, ILibraryAssembly topLevelAssembly,
			ILibraryObject child,
			Consumer<String> errorDisplay)
	{
		if (!LibraryHelper.isPartUsableForProject(project, child)) {
			errorDisplay.accept(getMessageForLibraryAssemblyHasChildWhichIsNotCurrent(topLevelAssembly, child));
			return false;
		}
		if (child instanceof ILibraryAssembly) {
			for (ILibraryAssemblyDetails assemblyDetail : ((ILibraryAssembly) child).getAssemblyDetails()) {
				if (!isLibraryAssemblyChildValid(project, topLevelAssembly, assemblyDetail.getSubComponent(),
						errorDisplay)) {
					return false;
				}
			}
		}
		return true;
	}

	private void displaceMessageInOutputWindow(String message)
	{
		CAFUtils.getInstance().getOutputWindow().sendMessage(message, AppInfo.getAppInfo().getApplicationTitle(), true);
		CAFUtils.getInstance().getOutputWindow().setActivePane(AppInfo.getAppInfo().getApplicationTitle());
	}

	private String getMessageForLibraryAssemblyHasChildWhichIsNotCurrent(ILibraryAssembly libraryAssembly,
			ILibraryObject child)
	{
		return ResourceMgr.getString(PlaceAssemblyTreeAction.class,
				"PlaceAssemblyTreeAction.InvalidLibraryAssembly.child.invalidStatus", libraryAssembly.getPartNumber(),
				child.getPartNumber());
	}

	@Nullable protected ILibraryPartSelection getSelectedLibraryAssembly()
	{
		return PartBrowserActionHelper.getSelectedBrowserPart();
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			PlaceAssemblyTreeActionHelper helper = PlaceAssemblyTreeActionHelper.instance();
			final IConnectivity connectivity = m_design.getConnectivity();
			assert connectivity != null;
			final ILibraryAssembly selectedObject = (ILibraryAssembly) m_libAssembly.getSelectedObject();
			assert selectedObject != null;
			m_assembly = helper.createLogicAssembly(selectedObject, connectivity);
		}
		m_isSuccess = successful;
		return successful;
	}

	private void activateHomeTab()
	{
		JComponent browser = getController().getBrowser();
		if (browser instanceof IBrowserTreeContainer) {
			final IBrowserTreeContainer broserTree = (IBrowserTreeContainer) browser;
			broserTree.activateHomeTab();
		}
	}

	private void expandAssemblyNode(LogicBrowserTree logicBrowserTree)
	{
		if (getCreatedAssembly() != null) {
			Set<IUID> newUIDs = new HashSet<>();
			newUIDs.add(getCreatedAssembly().getUID());
			logicBrowserTree.setObjectSelected(getCreatedAssembly().getUID());
			logicBrowserTree.rebuildTree(newUIDs);
			logicBrowserTree.collapseFirstLevelChildren(logicBrowserTree.getSelectionPath());
		}
	}

	public boolean onPostTerminate(boolean onTerminateResult)
	{
		if (isActionSuccess()) {
			enableDesignTreeExpansion(true);
			IActionable browserTreee = getController().getActionableBrowser("Diagram");
			if (browserTreee instanceof LogicBrowserTree) {
				LogicBrowserTree logicBrowserTree = (LogicBrowserTree) browserTreee;

				expandAssemblyNode(logicBrowserTree);
			}
			activateHomeTab();
			enableDesignTreeExpansion(false);
		}
		return true;
	}

	protected boolean isActionSuccess()
	{
		return m_isSuccess;
	}

	protected IAssembly getCreatedAssembly()
	{
		return m_assembly;
	}

	public String getActionUIClass()
	{
		return PlaceAssemblyTreeActionUI.class.getName();
	}

	private void enableDesignTreeExpansion(boolean enabled)
	{
		JComponent browser = getController().getBrowser();
		if (browser instanceof IBrowserTreeContainer) {
			((IBrowserTreeContainer) browser).setHomeTreeExpansionEnabled(enabled);
		}
	}
}


