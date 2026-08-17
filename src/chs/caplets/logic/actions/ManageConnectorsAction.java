/*
 * Copyright 2016 Mentor Graphics Corporation
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
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.actions.shared.SharedObjectAvailabilityReporter;
import chs.caplets.logic.actions.ui.IConductorConnectionChangeSavePredicate;
import chs.caplets.logic.actions.ui.ManageConnectorsDialog;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedLockableUpdateableObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.logical.shared.ISharedPinList;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IDesignDescriptor;
import chs.ctf.caf.utils.LockUpdateHelper;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import chs.utility.logic.SharedObjectAvailabilityChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ManageConnectorsAction extends ControllerActionRT implements ICtxMenuProvider
{

	protected IPinList m_pinList;

	private ConductorConnectionChanger conductorConnectionChanger;

	public ManageConnectorsAction(ICapletController controller)
	{
		super(controller);
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{

		if (getOperand(selections) != null) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		m_pinList = null;
		m_pinList = getOperand(getController().getSelectMgr().getPreSelections());

		if (m_pinList != null) {
			ISharedObject sharedObject = m_pinList.getSharedObject();
			if(sharedObject != null) {
				final IDesign logicDesign = m_pinList.getLogicDesign();
				// Cancel 'Manage connections' action on a shared object (all types) instance which is restricted to current user
				final ISharedObjectAvailabilityReporter reporter = new SharedObjectAvailabilityReporter();
				if (!new SharedObjectAvailabilityChecker().check(sharedObject, logicDesign, reporter, false)) {
					return IActionEnum.eCanceled;
				}
			}
			return IActionEnum.eCompleted;
		}
		return IActionEnum.eCanceled;
	}

	protected void reset()
	{
		conductorConnectionChanger = null;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		if(!successful){
			return false;
		}

		ManageConnectorDesignScope manageConnectorDesignScope = null;
		try {

			reset();
			ISharedPinList sharedPinList = m_pinList.getSharedPinList();

			if (sharedPinList != null) {
				IProject project = sharedPinList.getProject();
				IBuildList activeBuildlist = project.getBuildListMgr().getActiveBuildList();
				if (activeBuildlist != null) {
					manageConnectorDesignScope = new ManageConnectorDesignScope(activeBuildlist, sharedPinList, false,
							new Predicate<Collection<IDesignDescriptor>>()
							{
								@Override public boolean test(Collection<IDesignDescriptor> designDescriptors)
								{
									return designDescriptors.contains(m_pinList.getLogicDesign());
								}
							});
				}
				else {
					manageConnectorDesignScope = new ManageConnectorDesignScope(sharedPinList);
				}
			}
			else {
				manageConnectorDesignScope =
						new ManageConnectorDesignScope(Collections.singleton(m_pinList.getLogicDesign()), true);
			}
			manageConnectorDesignScope.addEditableDesigns(Collections.singleton(m_pinList.getLogicDesign()));

			try (LockSharedObjects lockHelper = new LockSharedObjects(m_pinList)) {
				if (lockHelper.areAllSharedObjectsLocked()) {

					createConductorConnectionChanger(manageConnectorDesignScope);
					ManageConnectorsDialog okCancelDialog;
					if (m_pinList.getSharedPinList() == null) {
						okCancelDialog =
								createDialog(m_pinList, conductorConnectionChanger, manageConnectorDesignScope);
					}
					else {
						okCancelDialog = createDialog(m_pinList.getSharedPinList(), conductorConnectionChanger,
								manageConnectorDesignScope, lockHelper);
					}

					if (showDialog(okCancelDialog)) {
						return applyChanges();
					}
				}
			}

			return false;
		}
		finally {
			if (manageConnectorDesignScope != null) {
				manageConnectorDesignScope.releaseDesignLocks();
			}
			reset();
			if (manageConnectorDesignScope != null) {
				manageConnectorDesignScope.releaseDesignLocks();
			}
		}
	}

	private boolean applyChanges()
	{
		Cursor cursor = null;
		try {
			cursor = getCursor();
			setCursor(new Cursor(Cursor.WAIT_CURSOR));
			return conductorConnectionChanger.changeConnections();
		}
		finally {
			if (cursor != null) {
				setCursor(cursor);
			}
		}
	}

	private void createConductorConnectionChanger(ManageConnectorDesignScope designScope)
	{
		ISharedPinList sharedPinList = m_pinList.getSharedPinList();
		if (sharedPinList != null) {
			conductorConnectionChanger = new ConductorConnectionChanger(sharedPinList,
					getConnectionChangePredicate(designScope));
		}
		else {
			conductorConnectionChanger =
					new ConductorConnectionChanger(m_pinList, getConnectionChangePredicate(designScope));
		}
	}

	private IConductorConnectionChangeSavePredicate getConnectionChangePredicate(
			ManageConnectorDesignScope manageConnectorDesignScope)
	{
		return new IConductorConnectionChangeSavePredicate()
		{
			@Override public boolean shouldSaveForeignDesigns()
			{
				return true;
			}

			@Override public boolean isCurrentDesign(IDesignDescriptor designDescriptor)
			{
				IDesign design = m_pinList.getLogicDesign();
				return design != null && design.getUID() == designDescriptor.getUID();
			}

			@Override public Collection<ILogicDesign> getOpenedDesignsToBeSaved()
			{
				Collection<ILogicDesign> designsToBeSaved = new ArrayList<>();
				Collection<IDesignDescriptor> designScope = manageConnectorDesignScope.getDesignsInScope();
				for (ILogicDesign openedDesign : CAFUtils.getInstance().getOpenedDesigns(ILogicDesign.class)) {
					if (designScope.contains(openedDesign)) {
						designsToBeSaved.add(openedDesign);
					}
				}
				return designsToBeSaved;
			}

			@Override public void doPostSave()
			{
				CAFUtils.getInstance().getWindowMgr().tickleUI();
			}
		};
	}

	public static class LockSharedObjects implements AutoCloseable
	{

		private Collection<ISharedPinList> sharedPinLists;
		private Collection<ISharedPinList> sharedObjectsLocked = new LinkedHashSet<>();

		public LockSharedObjects(@NotNull IPinList givenPinList)
		{
			Set<IPinList> allPinLists = new LinkedHashSet<>();
			allPinLists.add(givenPinList);
			allPinLists.addAll(givenPinList.getConnectedPinLists());

			sharedPinLists = new LinkedHashSet<>();
			for (IPinList pinList : allPinLists) {
				if (pinList.getSharedPinList() != null) {
					sharedPinLists.add(pinList.getSharedPinList());
				}
			}
		}

		boolean areAllSharedObjectsLocked()
		{
			for (ISharedPinList aSharedPinlist : sharedPinLists) {
				if (!aSharedPinlist.isLocked()) {
					ISharedLockableUpdateableObject sharedObjectToLock = aSharedPinlist.getLockableUpdateableRoot();
					LockUpdateHelper luh = new LockUpdateHelper(sharedObjectToLock);

					if (!luh.lockAndRefresh()) {
						return false;
					}

					sharedObjectsLocked.add(aSharedPinlist);
				}
			}

			return true;
		}

		public void lockAdditionalSharedPinLists(@NotNull Set<String> uids){
		    Set<ISharedPinList> sharedPinListsToBeLocked = new HashSet<>();
            uids.forEach(uid -> {
                ISharedPinList sharedPinList = UIDMgr.getObjectOfType(uid, ISharedPinList.class);
                if(sharedPinList != null && !sharedPinList.isLocked()) {
                    sharedPinListsToBeLocked.add(sharedPinList);
                }
            });
            UtilsHelper.getPersistenceSession().batchLock(sharedPinListsToBeLocked);
            sharedPinListsToBeLocked.forEach(sharedPinList ->
            {
                if(sharedPinList.isLocked()){
                    sharedObjectsLocked.add(sharedPinList);
                }
            });
        }

		@Override public void close()
		{
			if (sharedObjectsLocked != null) {
                UtilsHelper.getPersistenceSession().batchUnlock(sharedObjectsLocked);
			}
		}
	}

	protected boolean shouldDisableUnderConcurrentEdit()
	{
		return true;
	}

	@Override public boolean isEnabled()
	{
		return (getOperand(getController().getSelectMgr().getPreSelections()) != null) && super.isEnabled();
	}

	@Override public String getActionUIClass()
	{
		return ManageConnectorsActionUI.class.getName();
	}

	protected ManageConnectorsDialog createDialog(IPinList givenPinList,
			ConductorConnectionChanger connectionChanger, ManageConnectorDesignScope manageConnectorDesignScope)
	{

		return new ManageConnectorsDialog(CAFUtils.getInstance().getDialogFrame(), ResourceMgr
				.getString(ManageConnectorsAction.class, "ManageConnectorsAction.dialog.title",
						givenPinList.getName()),
				true, givenPinList, connectionChanger, manageConnectorDesignScope);
	}

	protected ManageConnectorsDialog createDialog(ISharedPinList sharedPinList,
			ConductorConnectionChanger connectionChanger, ManageConnectorDesignScope designsInScope, LockSharedObjects lockHelper)
	{

		String readOnlyTitle =
				designsInScope.isReadonly() ? ResourceMgr
						.getString(ManageConnectorsAction.class,
								"ManageConnectorsAction.dialog.currentdesignnotinbuildlist") : "";
		return new ManageConnectorsDialog(CAFUtils.getInstance().getDialogFrame(), ResourceMgr
				.getString(ManageConnectorsAction.class, "ManageConnectorsAction.dialog.title",
						sharedPinList.getName() + readOnlyTitle),
				true, sharedPinList, connectionChanger, designsInScope, lockHelper);
	}

	protected boolean showDialog(@NotNull ManageConnectorsDialog dialog)
	{
		return dialog.showDialog();
	}

	@Nullable protected IPinList getOperand(SelectSet selections)
	{

        Set<IPinList> pinLists =
				selections.getSelectedUIDS()
						.stream()
						.map(ReferenceHelper::reduceToConnectivityObject)
						.filter(connectivityObject -> {
							if(connectivityObject instanceof IConnector) {
								return !((IConnector) connectivityObject).isRingTerminal()
										&& !(connectivityObject instanceof IDeviceConnector);
							}
							return connectivityObject instanceof IDevice;
						})
						.map(pinList -> (IPinList) pinList)
						.collect(Collectors.toSet());

		if (pinLists.size() == 1) {
			return pinLists.iterator().next();
		}
		return null;
	}
}
