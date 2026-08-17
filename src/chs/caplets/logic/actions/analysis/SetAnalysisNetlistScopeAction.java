/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2008-2024 Siemens
 */
package chs.caplets.logic.actions.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.IAnalysisNetlistScope;
import chs.analysis.scope.IAnalysisNetlistScopeChangeListener;
import chs.caf.CAFUtils;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.CapletLifecycleHelper;
import chs.caplets.logic.analysis.ui.AnalysisBrowserPanel;
import chs.caplets.shared.BaseController;
import chs.cof.logical.IDesign;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IAnalysisBuildList;
import chs.cof.project.buildlist.IBuildList;
import chs.cof.project.buildlist.IBuildListMgr;
import chs.cof.topology.IBaseTopologyDesign;
import chs.common.DesignUtils;
import chs.common.IDesignContainer;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.system.FactoryMgr;
import chs.utilities.ui.tree.Drawer;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import java.awt.Cursor;
import java.awt.event.ActionEvent;

@SuppressWarnings({"ThisEscapedInObjectConstruction"}) @ApplicationSpecification(
		includeIn = {Application.CapitalLogicDesigner, Application.CapitalSystemsIntegrator, Application.CapitalCapture,
				Application.CapitalArchitect, Application.CapitalEssentialsDesign, Application.SEElectricalDesign})
public class SetAnalysisNetlistScopeAction extends AbstractAction implements IAnalysisNetlistScopeChangeListener
{

	// /////////////// //
	// Class variables //
	// /////////////// //

	protected static final String NAME_PREFIX = "<html>Set Scope: <b>";
	protected static final String NAME_SUFFIX = "</b></html>";

	// ///////////// //
	// Class methods //
	// ///////////// //

	@Nullable private static String getName(IUIDObject obj)
	{
		String name = null;
		if (obj instanceof IDesignContainer) {
			IDesignContainer desCont = (IDesignContainer) obj;
			name = desCont.getFullName();
		}
		else if (obj instanceof IReadOnlyNamedObject) {
			name = ((IReadOnlyNamedObject) obj).getName();
		}
		return name;
	}

	// ////////////////// //
	// Instance Variables //
	// ////////////////// //

	/**
	 * The scope object
	 */
//	private IUIDObject scopeObject;
	private String scopeObjectId = null;

	/**
	 * The selected icon
	 */
	private Icon selectedIcon = null;

	/**
	 * The unselected icon
	 */
	private Icon unselectedIcon = null;

	// //////////// //
	// Constructors //
	// //////////// //
	public SetAnalysisNetlistScopeAction(IUIDObject obj)
	{
		String name = getName(obj);
		putValue(NAME, name);
		setDescription(name);

		putValue(LONG_DESCRIPTION, "Set Scope to " + name);
		scopeObjectId = obj == null ? null : obj.getUID().getString();

		// load the icons...
		loadIcons(obj);
		putValue(SMALL_ICON, unselectedIcon);

		AnalysisServices.addScopeChangeListener(this);
	}

	private void setDescription(String name)
	{
		putValue(SHORT_DESCRIPTION, NAME_PREFIX + name + NAME_SUFFIX);
	}

	@SuppressWarnings({"ThisEscapedInObjectConstruction"}) private void loadIcons(IUIDObject obj)
	{
		if (obj instanceof IBuildList) {
			unselectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/scope_build_list.gif");
			selectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/current_scope_buildlist.gif");
		}
		else if (obj instanceof IBaseTopologyDesign) {
			unselectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/scope_plane.gif");
			selectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/current_scope_topoplane.gif");
		}
		else if (obj instanceof IDesign) {
			unselectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/scope_design.gif");
			selectedIcon = CHSImageLoader.loadImageIcon("chs/images/app/current_scope_design.gif");
		}
	}

	public boolean isEnabled()
	{
		// We need to ensure we have the most up to date name in the title bar. Placing this check
		// here ensures we're always up to date before the menu is shown. There should be little
		// performance hit as all items are local.
		IUIDObject scopeObject = getScopeObject();
		if (scopeObject != null) {
			String name = getName(scopeObject);
			if (name != null) {
				setDescription(name);
			}
		}
		return true;
	}

	public void actionPerformed(ActionEvent e)
	{
		CAFUtils.getInstance().loadCursor(Cursor.WAIT_CURSOR);
		try {
			IUIDObject scopeObject = getScopeObject();
			IUIDObject initialScopedObject = null;
			if (AnalysisServices.getCurrentAnalysisNetlistScope() != null) {
				initialScopedObject = AnalysisServices.getCurrentAnalysisNetlistScope().getScopedObject();
			}

//			If design is deleted from other session then warn user about that and update the Panel accordingly
			if (scopeObject instanceof IDesign) {
				IProject project = ((IDesign) scopeObject).getProject();
				if (CapletLifecycleHelper.warnAndCloseIfProjectIsDeleted(project) ||
						CapletLifecycleHelper.warnAndRefreshIfDesignIsDeleted(project,
								(IDesign) scopeObject)) {
					//((Project)project).fireDesignRemoved((IDesign) scopeObject) ;
					AnalysisBrowserPanel analysisBrowserPanel =
							((BaseController) CAFUtils.getInstance().getActiveCapletController())
									.getAnalysisBrowserPanel();
					if (analysisBrowserPanel != null) {
						analysisBrowserPanel.updateToolBarForChangedScopeCandidates();
					}
					CAFUtils.getInstance().projectChanged(project);
					setActiveBuildList(scopeObject);
					return;
				}
			}
			AnalysisServices.changeScopeWithProgressDialog(scopeObject,
					CAFUtils.getInstance().getCurrentProject().getUID().toString(), true,
					CAFUtils.getInstance().getWindowMgr().getDialogFrame());
			if (AnalysisServices.getCurrentAnalysisNetlistScope() == null) {
				return;
			}
			if (CAFUtils.getInstance().getActiveCapletController() != null) {
				ICapletController controller = CAFUtils.getInstance().getActiveCapletController();
				controller.addAuditTab();
				CAFUtils.getInstance().tickleUI(controller.getCaplet().getFIB());
			}

			setActiveBuildList(scopeObject);
			//We have to repaint project tree explicitly to display scope changes correctly.
			//Only selected node of project tree will be repainted on its own.
			//PDVC-238 / dts0101226085
			if (initialScopedObject != scopeObject) {
				CAFUtils.getInstance().getCAFProjectMgr().repaintCurrentProjectTree();
			}
		}
		finally {
			CAFUtils.getInstance().loadCursor(Cursor.DEFAULT_CURSOR);
		}
	}

	private void setActiveBuildList(@Nullable IUIDObject scopeObject)
	{
		boolean isActiveAnalysisBuildList = false;
		IBuildList activeBuildList =
				FactoryMgr.getCAFUtils().getCurrentProject().getBuildListMgr().getActiveBuildList();
		if (activeBuildList != null && activeBuildList instanceof IAnalysisBuildList) {
			isActiveAnalysisBuildList = true;
		}

		if (scopeObject instanceof IAnalysisBuildList && (isActiveAnalysisBuildList || activeBuildList == null)) {
			//Change the active build list to scopeobject if scope object is analysis buildlist and
			// if current active build list is an analysis build list or no active build list present now.
			CAFUtils.getInstance().getCurrentProject().getBuildListMgr()
					.setActiveBuildList((IBuildList) scopeObject);
		}
		else if (isActiveAnalysisBuildList) {
			//If current active build list is analysis buildlist and scope object is a design
			// then deactivate the analysis build list.
			unsetActiveBuildList();
		}
	}

	protected void unsetActiveBuildList()
	{
		IBuildListMgr mgr = CAFUtils.getInstance().getCurrentProject().getBuildListMgr();
		mgr.setActiveBuildList(null);
		CAFUtils.getInstance().getCAFProjectMgr().projectChanged(CAFUtils.getInstance().getCurrentProject());
	}

	public void updateUI()
	{
		boolean enableState = isEnabled();
		setEnabled(!enableState);
		setEnabled(enableState);
	}

	public void scopeChanged(IAnalysisNetlistScope newScope)
	{
		Icon icon = unselectedIcon;
		if (newScope != null) {
			IUIDObject obj = newScope.getScopedObject();
			IUIDObject scopeObject = getScopeObject();
			if (scopeObject != null && scopeObject.equals(obj)) {
				// they're the same, we're selected
				icon = selectedIcon;
				setSelectedComponent(this);
			}
		}
		putValue(SMALL_ICON, icon);
	}

	public boolean isActive()
	{
		Icon icon = unselectedIcon;
		boolean rVal = false;
		IUIDObject scopeObject = getScopeObject();
		if (AnalysisServices.getCurrentAnalysisNetlistScope() != null && scopeObject != null &&
				scopeObject.equals(AnalysisServices.getCurrentAnalysisNetlistScope().getScopedObject())) {
			icon = selectedIcon;
			rVal = true;
		}
		putValue(SMALL_ICON, icon);
		return rVal;
	}

	private static SetAnalysisNetlistScopeAction selectedAction = null;

	public static void setSelectedComponent(SetAnalysisNetlistScopeAction a)
	{

		if (selectedAction != null && a != null) {
			selectedAction.firePropertyChange(Drawer.DRAWER_SELECTED, selectedAction, a);
		}
		selectedAction = a;
	}

	@Nullable private IUIDObject getScopeObject()
	{
		if (scopeObjectId != null) {
			IUID uid = FactoryMgr.getCommonFactory().constructUID(scopeObjectId);
			return DesignUtils.getLoadedObject(uid);
		}
		return null;
	}
}
