/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.icd;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.AppAction;
import chs.caf.CAFUtils;
import chs.caf.IAppActionMgr;
import chs.caf.cafmain.actions.RefreshICDObjectsAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectEvent;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.helpers.ui.common.ResourceHolder;
import chs.caplets.logic.actions.icdbrowser.ICDObjectActionHandler;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDPartBrowser;
import chs.cof.library.IICDComponentSearchController;
import chs.cof.library.ILibraryPartViewerWindow;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IDevice;
import chs.cof.parts.ILibraryCustomerPartNumber;
import chs.cof.parts.ILibraryDevice;
import chs.cof.parts.ILibraryObject;
import chs.cof.parts.ILibrarySupplierPartNumber;
import chs.cof.parts.Library;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.PartPreferencesSelectionContext;
import chs.cof.project.IProject;
import chs.common.IPreferenceChangeEvent;
import chs.common.IPreferenceChangeListener;
import chs.common.IProjectPreferenceMgr;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.ctf.ui.form.CTFLabel;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.ui.tree.TreeUtils;
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ICDBrowserPanel extends JPanel implements IICDPartBrowser, IPreferenceChangeListener
{

	private ICDBrowserTree m_icdTree;
	private JToolBar refreshToolbar;
	private ICDObjectActionHandler m_actionHandler;
	private boolean m_actionRunning = false;
	private ILibraryPartViewerWindow m_partViewerWnd = null;
	private IICDComponentSearchController m_libraryCompSearchController;
	private ICDSelection m_partSelection;
	private IUID m_design;
	private ICDSelectionHelper m_selectionHelper;

	public ICDBrowserPanel(ILogicDesign design, ICapletController capletController,
			@NotNull ICapletModel model, IICDComponentSearchController libraryComponentSearchController)
	{

		m_icdTree = new ICDBrowserTree(new ICDBrowserClient(design, capletController, this), "ICDBrowser");
		m_actionHandler = new ICDObjectActionHandler(model, m_icdTree);
		m_libraryCompSearchController = libraryComponentSearchController;
		m_libraryCompSearchController.setSearchMode(false);
		m_design = design.getUID();
		m_partSelection = new ICDSelection(design);
		m_selectionHelper = new ICDSelectionHelper();

		m_actionHandler.setPartBrowser(this);

		setLayout(new GridBagLayout());

		JLabel refreshLabel = new CTFLabel();
		refreshLabel.setText(
				ResourceMgr.getStringForLabel(ICDBrowserPanel.class, "ICDBrowserPanel.Refresh.text"));
		GridBagConstraints gridBagConstraints =
				new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1.0, 0.0, GridBagConstraints.EAST,
						GridBagConstraints.NONE, new Insets(3, 0, 0, 4), 0, 0);

		ActionContainer refreshICDsTb = new ActionContainer("RefreshICDs");
		if (capletController != null) {

			IAppActionMgr appActionMgr = capletController.getCaplet().getFIB().getAppActionMgr();

			AppAction appAction = appActionMgr.getAction(RefreshICDObjectsAction.class.getName());
			if (appAction != null) {
				refreshICDsTb
						.add(new ActionEntry(appAction));
			}
		}
		refreshToolbar = ResourceHolder.createToolBar((String) refreshICDsTb.getValue(Action.NAME),
				refreshICDsTb.getMembers(), null, capletController);
		refreshToolbar.setBorder(null);
		if (refreshToolbar.getComponentCount() > 0){
			add(refreshLabel, gridBagConstraints);
		}
		gridBagConstraints = new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHEAST,
				GridBagConstraints.HORIZONTAL, new Insets(3, 0, 0, 5), 0, 0);
		add(refreshToolbar, gridBagConstraints);

		gridBagConstraints =
				new GridBagConstraints(0, 1, 10, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
						new Insets(0, 5, 5, 5), 0, 0);

		JPanel treePanel = m_icdTree.buildContentPanel(null);
		add(treePanel, gridBagConstraints);
		IProjectPreferenceMgr preferences = getProjectPreferenceMgr();
		if (preferences != null) {
			preferences.addPreferenceChangeListener(this);
		}
	}

	@Nullable private IProjectPreferenceMgr getProjectPreferenceMgr()
	{
		ILogicDesign logicDesign = getLogicDesign();
		IProject project = logicDesign.getProject();
		return project != null ? project.getPreferences() : null;
	}

	public void destroy()
	{
		IProjectPreferenceMgr preferences = getProjectPreferenceMgr();
		if (preferences != null) {
			preferences.removePreferenceChangeListener(this);
		}
		if (refreshToolbar != null) {
			refreshToolbar.removeAll();
			refreshToolbar = null;
		}
		if (m_icdTree != null) {
			m_icdTree.destroy();
			m_icdTree = null;
		}
		removeAll();

		m_libraryCompSearchController.exit();
		if (m_partViewerWnd != null) {
			m_partViewerWnd.disposeWindow();
			m_partViewerWnd = null;
		}
		m_actionHandler.setPartBrowser(null);
		m_actionHandler = null;
	}

	public void startAction()
	{
		m_actionRunning = true;
	}

	public void endAction()
	{
		m_actionRunning = false;
	}

	@Override public ILibraryPartSelection getPartSelection()
	{
		return m_partSelection;
	}

	@Nullable public ILibraryPartSelection getPartSelectionForCreate()
	{
		if (m_actionRunning) {
			return m_partSelection;
		}
		return null;
	}

	public void viewDetails(boolean visible)
	{
		if (m_partViewerWnd == null) {
			if (visible) {
				m_partViewerWnd =
						CAFUtils.getInstance().getCHSSystem().getPartsLibrary()
								.createLibraryPartViewerWindow(CAFUtils.getInstance().getDialogFrame(),
										m_libraryCompSearchController,
										ConfigurationTypeEnum.fromDesignType(getLogicDesign().getDesignType()));
				if (m_partViewerWnd != null) {
					m_partViewerWnd.refresh();
					m_partViewerWnd.refreshCompDetailsTabManager();
					m_partViewerWnd.makeModal(false);
				}
			}
			else {
				return;
			}
		}
		if (m_partViewerWnd != null) {
			m_partViewerWnd.makeVisible(visible);
		}
	}

	@Override public void selectDetails()
	{

		if (m_partSelection != null) {
			PartPreferencesSelectionContext context = new PartPreferencesSelectionContext(m_selectionHelper.getSelectionFilter(m_partSelection));

			ConfigurationTypeEnum configurationTypeEnum =
					ConfigurationTypeEnum.fromDesignType(getLogicDesign().getDesignType());
			Library.getInstance().getLibraryPartSelector(CAFUtils.getInstance().getDialogFrame())
					.showSelectDetailsDlg(m_partSelection, context, configurationTypeEnum);
		}
	}

	@Override public boolean shouldEnableSelectDetails()
	{
		if (m_partSelection == null || m_partSelection.getICD() == null) {
			return false;
		}
		ILibraryObject libraryObject = m_partSelection.getSelectedObject();
		if (!(libraryObject instanceof ILibraryDevice)) {
			return false;
		}
		IDeviceICD icd = m_partSelection.getICD();
		boolean multipleSuppParts = libraryObject.getSupplierPartNumbers().size() > 1;
		if (!StringUtils.isBlank(icd.getSupplierPartNumber())) {
			multipleSuppParts = false;
		}
		boolean multipleFootprints = ((ILibraryDevice) libraryObject).getDeviceFootprints().size() > 1;
		if (!StringUtils.isBlank(icd.getFootprintName())) {
			multipleFootprints = false;
		}
		boolean multipleCustParts = libraryObject.getCustomerPartNumbers().size() > 1;
		if (!StringUtils.isBlank(icd.getCustomerPartNumber())) {
			multipleCustParts = false;
		}
		boolean multipleSymbols = libraryObject.getLibraryGraphics().size() > 1;
		return multipleCustParts || multipleSymbols || multipleSuppParts || multipleFootprints;
	}

	public void doubleClicked()
	{
		m_actionHandler.executeDefaultAction(getPartSelection());
	}

	public void selectObject(@NotNull IICD icd)
	{
		IDeviceICD devICD = m_partSelection.getICD();
		if (devICD == null || !devICD.match(icd)) {
			ILogicDesign logicDesign = getLogicDesign();
			IDeviceICD deviceICD = logicDesign.getDesignICDContainer().constructDeviceICD(icd);
			m_partSelection = new ICDSelection(deviceICD, logicDesign);
			selectLibraryObjects(deviceICD);
		}
	}

	public void selectLibraryObjects(IDeviceICD icd)
	{
		m_selectionHelper.selectLibraryObjects(icd, m_partSelection, m_libraryCompSearchController);
	}

	public void unselectCurrentSelection()
	{
		m_partSelection = new ICDSelection(getLogicDesign());
		m_partSelection.setSelectedLibraryObject(null);
	}

	public static void updateCustomerPartNumber(ICDSelection partSelection, IDeviceICD icd, ILibraryDevice device)
	{
		String customerPartNumber = icd.getCustomerPartNumber();
		if (!StringUtils.isBlank(customerPartNumber)) {
			Set<ILibraryCustomerPartNumber> customerPartNumbers = device.getCustomerPartNumbers(customerPartNumber);
			if (!customerPartNumbers.isEmpty()) {
				partSelection.setSelectedCustomerPartNumber(customerPartNumbers.iterator().next());
			}
		}
	}

	public static void updateSupplierPartNumber(ICDSelection partSelection, IDeviceICD icd, ILibraryDevice device)
	{
		String supplierPartNumber = icd.getSupplierPartNumber();
		if (!StringUtils.isBlank(supplierPartNumber)) {
			Set<ILibrarySupplierPartNumber> supplierPartNumbers = device.getSupplierPartNumbers(supplierPartNumber);
			if (!supplierPartNumbers.isEmpty()) {
				partSelection.setSelectedSupplierPartNumber(supplierPartNumbers.iterator().next());
			}
		}
	}

	public void displayPopupMenu(MouseEvent e)
	{
		m_actionHandler.displayPopupMenu(getPartSelection(), e);
	}

	public void reBuildICDTree()
	{
		if (m_icdTree != null) {
			IUIDObject iuidObject = UIDMgr.getNonDeletedObject(m_design);
			IDesign design = CommonUtils.cast(iuidObject, IDesign.class);
			if (design != null) {
				m_partSelection = new ICDSelection(design);
			}
			m_icdTree.reBuildICDTree();
		}
	}

	@NotNull private ILogicDesign getLogicDesign()
	{
		ILogicDesign logicDesign = (ILogicDesign) UIDMgr.getObject(m_design);
		assert logicDesign != null;
		return logicDesign;
	}

	@Override public void preferenceChanged(IPreferenceChangeEvent e)
	{
		IDeviceICD deviceICD = m_partSelection.getICD();
		if (deviceICD != null) {
			m_partSelection = new ICDSelection(deviceICD, getLogicDesign());
			selectLibraryObjects(deviceICD);
		}
	}

	@Override public void selectICDInTree(@NotNull IDevice device)
	{
		if (m_icdTree != null) {
			IDeviceICD deviceICD = ICDUtils.getMappedICD(device);
			if (deviceICD != null) {
				TreeUtils.expandAll(m_icdTree, true);
				SelectSet selectSet = new SelectSet(deviceICD.getICD());
				ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
				if (selectMgr != null) {
					selectMgr.getPreSelections().add(selectSet);
				}
				m_icdTree.selectionChanged(new SelectEvent(selectSet, true, true));
			}
		}
	}

	@NotNull public ICDObjectActionHandler getActionHandler()
	{
		return m_actionHandler;
	}
}
