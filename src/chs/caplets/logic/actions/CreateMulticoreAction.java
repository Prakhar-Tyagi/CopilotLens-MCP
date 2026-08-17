/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2002-2023 Siemens
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ActionRT;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.MCProxy;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.caf.helpers.ui.std.UIManager;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.UserSessionException;
import chs.caplets.logic.Model;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.common.DesignAbstractionType;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utilities.Environment;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.UtilsHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CreateMulticoreAction extends ControllerActionRT
{

	private Model m_model;
	private CreateLogicMulticoreDialog m_dialog = null;
	@NotNull private CreateMulticoreContext m_context;
	private boolean m_shieldEdit = false;
	private Collection<IUID> m_alreadyLockedInThisSession = new HashSet<IUID>();

	public CreateMulticoreAction(ICapletController controller)
	{
		super(controller);
		m_model = (Model) controller.getCapletModel();
		m_context = createMulticoreActionContext();
		m_context.setProxyRoot(new MCProxy("root", null));
		m_context.setEditScope(MulticoreEditPanel.LOCAL_SCOPE);
		m_context.setEditType(COFTypeEnum.Multicore);
	}

	protected CreateMulticoreContext createMulticoreActionContext()
	{
		return new CreateMulticoreContext(m_model.getDesign(), m_model.getDiagram());
	}

	public String getActionUIClass()
	{
		return CreateMulticoreActionUI.class.getName();
	}

	public void setEditScope(int scope)
	{
		m_context.setEditScope(scope);
	}

	public void setEditType(COFTypeEnum type)
	{
		m_context.setEditType(type);
	}

	protected Model getModel()
	{
		return m_model;
	}

	public CreateMulticoreContext getContext()
	{
		return m_context;
	}

	/*
	 * Determines whether the given multicore is directly or indirectly inside an overbraid
	 */

	public IActionEnum onActivate(ActionEvent e)
	{
		m_context.init(m_model.getDesign(), m_model.getDiagram());
		m_model.getDesign().refresh(); // is the next refresh required
		if (!CreateMulticoreActionHelper.refreshManager(m_context)) {
			return IActionEnum.eCanceled;
		}

		m_context.buildProxyTree();
		m_alreadyLockedInThisSession =
				new HashSet<IUID>(LogicObjectLockFinder.getAllTheLockedObjects(m_model.getDesign()));

		if (showDialog(getHiddenNames())) {
			return IActionEnum.eCanceled;
		}

		return IActionEnum.eCompleted;
	}

	private boolean showDialog(Set<String> hiddenMulticoreNames)
	{
		HashSet<IUID> selectedUIDS = new HashSet<IUID>();
		//
		// Bring up a dialog, asking for the name & the members.
		//
		Frame owner = getController().getCaplet().getFIB().getWindowMgr().getDialogFrame();

		SelectSet sset = getController().getSelectMgr().getCurrentSelections();
		for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject obj = iter.getNext();
			IUID uid = getIUID(obj);

			if (uid != null) {
				selectedUIDS.add(uid);
			}
		}

		m_dialog = createMulticoreCreationDialog(owner, m_context, selectedUIDS);
		m_dialog.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (m_dialog != null) {
					m_dialog.setVisible(false);
					m_dialog.setCancelled(true);
				}
			}
		});

		m_dialog.setAlreadyLockedInThisSession(m_alreadyLockedInThisSession);

		ILibraryPartSelection libObj = PartBrowserActionHelper.getSelectedBrowserPart();
		m_context.setLibObj(libObj);

		if (libObj != null) {
			m_dialog.getPanel().onNew(m_context.isOverbraidEditType());
		}
		//
		// Select all the things which are selected on the diagram
		//
		if (sset.getSelectCount() > 0) {
			// First, squirrel away the UIDs of the objects in the list.
			//
			ListModel lm = m_dialog.getAvailableNetOrMulticoresList().getModel();
			ListSelectionModel lsm = m_dialog.getAvailableNetOrMulticoresList().getSelectionModel();
			List<IUID> lmv = new ArrayList<IUID>(lm.getSize());
			//
			for (int i = 0; i < lm.getSize(); i++) {
				MCProxy mcp = (MCProxy) lm.getElementAt(i);
				if (mcp.getRef() instanceof ILogicObject) {
					lmv.add(mcp.getRef().getUID());
				}
			}
			//
			// Now go through the selected objects...
			//
			for (SelectedUIDObjectIterator iter = sset.getSelectedUIDObjects(); iter.hasNext(); ) {
				IUIDObject obj = iter.getNext();
				IUID uid = getIUID(obj);

				if (uid != null) {
					int idx = lmv.indexOf(uid);
					if (idx != -1) {
						//
						// In the list.
						//
						lsm.addSelectionInterval(idx, idx);
					}
				}
			}
		}
		//
		m_dialog.getPanel().setShieldEdit(canEditShield());
		m_dialog.getPanel().setHiddenMulticoreNames(hiddenMulticoreNames);
		m_dialog.setVisible(true);

		return m_dialog.isCancelled();
	}

	protected boolean canEditShield()
	{
		return true;
	}

	@Nullable private IUID getIUID(IUIDObject obj)
	{
		IUID uid = null;
		if (obj instanceof chs.cof.logical.schem.IConductor) {
			IUIDObject connObject = ((IRepresentedObject) obj).getRawConnectivity();
			assert connObject != null;
			uid = connObject.getUID();
		}
		else if (obj instanceof chs.cof.logical.schem.IShieldBody) {
			chs.cof.logical.schem.IShieldBody sb = (chs.cof.logical.schem.IShieldBody) obj;
			uid = sb.getConnectivity().getMulticore().getUID();
		}
		else if (obj instanceof IMulticore) {
			IMulticore mc = (IMulticore) obj;
			uid = mc.getUID();
		}

		return uid;
	}

	@NotNull protected CreateLogicMulticoreDialog createMulticoreCreationDialog(@Nullable Frame owner,
			@NotNull CreateMulticoreContext context, @NotNull HashSet<IUID> selectedUIDS)
	{
		return new CreateLogicMulticoreDialog(owner, CAFUtils.getInstance().getDialogTitleByAction(this, true), context,
				selectedUIDS);
	}

	/**
	 * Obtains all multicores or overbraids in the tree.
	 *
	 * @param proxy - root node in the tree to search for multicores.
	 * @param multicores - where to add the multicores found
	 */
	private void getAllMulticores(MCProxy proxy, Set<IMulticore> multicores)
	{
		for (int i = 0; i < proxy.getChildCount(); i++) {
			MCProxy childProxy = proxy.getChildProxyAt(i);
			getAllMulticores(childProxy, multicores);
		}

		IUIDObject ref = proxy.getRef();
		if (ref instanceof IMulticore) {
			IMulticore multicore = (IMulticore) ref;
			multicores.add(multicore);
		}
	}

	/**
	 * Collect names of multicores and overbraids excluded from this edit but which should be used when checking for
	 * duplicate names.
	 *
	 * @return return hidden names
	 */
	private Set<String> getHiddenNames()
	{
		// Find the multicores that are visible in the dialog.
		Set<IMulticore> visibleMulticores = new HashSet<IMulticore>();
		getAllMulticores(m_context.getProxyRoot(), visibleMulticores);

		Set<String> hiddenMulticoreNames = new HashSet<String>();

		// For all multicores in the design.
		IConnectivity connectivity = m_context.getConnectivity();
		assert connectivity != null;
		boolean topLevelOnly = false;
		for (IMulticore multicore : connectivity.getMulticores(topLevelOnly)) {
			if (!visibleMulticores.contains(multicore)) {
				hiddenMulticoreNames.add(multicore.getName());
			}
		}
		return hiddenMulticoreNames;
	}

	protected boolean onTerminate(boolean successful)
	{
		boolean rvalue = false;
		try {
			if (successful) {
				rvalue = editModel();
			}
		}
		finally {
			CreateMulticoreActionHelper.unlockManager(m_context);
			if (m_dialog != null) {
				m_dialog.getPanel().releaseLocks();
				m_dialog.dispose();
				m_dialog = null;
				m_context.destroy();
			}
		}
		return rvalue;
	}

	protected boolean editModel()
	{

		Set<MCProxy> removedMCs = m_dialog.getPanel().getRemovedMulticores();
		Set<MCProxy> addedMCs = m_dialog.getPanel().getAddedMulticores();
		Set<MCProxy> changedIndicators = m_dialog.getPanel().getAddedIndicators();
		Set<MCProxy> modifiedMCs = m_dialog.getPanel().getModifiedMulticores();
		m_context.setProxyLibMCmap(m_dialog.getPanel().getLibraryMCMap());
		m_context.setProxylibMCInnerCoreMap(m_dialog.getPanel().getProxylibMCInnerCoreMap());

		Set<ILogicDesign> designsToBeUnlocked = new HashSet<ILogicDesign>();

		IUserSession session = UtilsHelper.getCHSSystem().getUserSession();
		boolean bStatus = false;
		try {
			startClientTransaction(session);
			Set<ISharedObject> sharedChanges = m_dialog.getPanel().getSharedChanges();
			Set<ILogicDesign> designsToBeUpdated = new HashSet<ILogicDesign>();
			boolean bLockedAllDesigns =
					m_context.lockImpactedDesigns(designsToBeUnlocked, sharedChanges, designsToBeUpdated);
			if (bLockedAllDesigns) {
				m_context.processChanges(designsToBeUpdated, addedMCs, removedMCs, modifiedMCs, changedIndicators,
						sharedChanges);
				//
				commitClientTransaction(session);
				bStatus = true;
			}
		}
		catch (UserSessionException sessionException) {
			Environment.getExceptionDisplay().displayException(sessionException, "CreateMulticoreAction failed");
		}
		catch (SharedObjectLockException e) {
			// Do nothing here, as error message already displayed
		}
		catch (Exception e) {
			Environment.getExceptionDisplay().displayException(e, "CreateMulticoreAction failed");
		}
		finally {
			m_context.releaseDesignLocks(designsToBeUnlocked);
			if (!bStatus && session != null) {
				session.rollbackClientTransaction();
			}
		}
		return bStatus;
	}

	protected void startClientTransaction(IUserSession session)
	{
		session.startClientTransaction();
	}

	protected void commitClientTransaction(IUserSession session) throws UserSessionException
	{
		session.commitClientTransaction();
	}

	public boolean isEnabled()
	{
		DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();
		Set<DesignAbstractionType> abstractionTypes =
				new LinkedHashSet<>(
						Arrays.asList(DesignAbstractionType.SYTEM_BLOCK,
								DesignAbstractionType.SMART_FLOWS,DesignAbstractionType.FLUID));
		if (designAbstraction != null && abstractionTypes.contains(designAbstraction)) {
			return false;
		}

		if (m_context.isSharedEditScope() && ActionRT.isDesignUnderConcurrentEdit()) {
			setDisabledReason(ResourceMgr.getString(ActionRT.class, "ActionRT.LogicMUMode"));
			return false;
		}
		return getController().getCapletModel().isEditable() && super.isEnabled();
	}

	/**
	 * @see ActionRT#destroy()
	 */
	public void destroy()
	{
		super.destroy();
		m_model = null;
		m_context.destroy();
	}

	public static class SharedObjectLockException extends RuntimeException
	{

		public SharedObjectLockException(Throwable cause)
		{
			super(cause);
		}
	}
}