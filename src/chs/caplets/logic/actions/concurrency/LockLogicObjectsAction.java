/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2018-2024 Siemens
 */

package chs.caplets.logic.actions.concurrency;

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.ICtxMenuProvider;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.action.IActionUI;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.ILogicObject;
import chs.cofUtils.logical.concurrency.ILogicDesignChildrenLockStrategy;
import chs.cog.IPrivilegedCOGManagedLockableChildrenContainer;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.images.CHSImageLoader;
import chs.system.UIDMgr;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.ReferenceHelper;
import chs.utility.helpers.SingleLineHelper;
import chs.utility.ui.HTMLHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign})
public class LockLogicObjectsAction extends ControllerActionRT implements IActionUI, ICtxMenuProvider
{

	private Map<String, Object> values;

	public LockLogicObjectsAction(@NotNull ICapletController controller)
	{
		super(controller);
		values = new HashMap<String, Object>();
		Icon icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_lock_local.png");

		putValue(NAME, ResourceMgr.getString(LockLogicObjectsAction.class, "LockLogicObjectsAction.name.decl"));
		putValue(SMALL_ICON, icon);
	}

	@Override public void updateUI()
	{

	}

	@NotNull @Override public ICaplet getCaplet()
	{
		return getController().getCaplet();
	}

	@Override public void setupUI()
	{

	}

	@Override public String getActionClass()
	{
		return getClass().getName();
	}

	@Nullable @Override public String getActionUIInstanceName()
	{
		return null;
	}

	@Override public String getActionUIClass()
	{
		return LockLogicObjectsAction.class.getName();
	}

	@Override protected IActionEnum onActivate(ActionEvent e)
	{
		return IActionEnum.eActivated;
	}

	@Override protected boolean onTerminate(boolean successful)
	{
		return true;
	}

	private boolean doAction()
	{
		SelectSet selections = getController().getSelectMgr().getPreSelections();
		final Collection<ILogicObject> logicObjects = getOperands(selections);
		final Collection<ILogicObject> candidatesToLock = new HashSet<>();
		ILogicDesign logicDesign = null;
		Collection<? extends ILogicObject> lockableLogicObjects =
				ILogicDesignChildrenLockStrategy.mapToLockable(logicObjects);
		for (ILogicObject logicObject : lockableLogicObjects) {
			if (LogicObjectLockFinder.isEditable(logicObject)) {
				if (SingleLineHelper.isSingleLineMulticore(logicObject)) {
					continue;
				}
				String message = ResourceMgr
						.getString(getClass(), "LockLogicObjectsAction.alreadyLocked.msg", logicObject.getName());
				CAFUtils.getInstance().sendApplicationMessage(message);
			}
			else {
				final ILogicDesign parentDesign = logicObject.getLogicDesign();
				if (parentDesign != null) {
					logicDesign = parentDesign;
					candidatesToLock.add(logicObject);
				}
			}
		}
		if (logicDesign != null) {
			final Set<IUID> lockFailed = LogicObjectLockFinder.tryEdit(logicDesign, candidatesToLock);
			Collection<ILogicObject> lockFailedObjects = new ArrayList<>();
			for (IUID lockFailedUID : lockFailed) {
				final ILogicObject logicObject = UIDMgr.getObjectOfType(lockFailedUID, ILogicObject.class);
				if (logicObject != null) {
					if (SingleLineHelper.isSingleLineMulticore(logicObject)) {
						continue;
					}
					lockFailedObjects.add(logicObject);
				}
			}
			if (!lockFailedObjects.isEmpty()) {
				String message = ResourceMgr.getString(getClass(), "LockLogicObjectsAction.lockFailed.msg",
						HTMLHelper.linkAll(lockFailedObjects));
				CAFUtils.getInstance().sendApplicationMessage(message);
			}

			Collection<ILogicObject> lockedObjectNames = new ArrayList<>();
			for (ILogicObject logicObject : candidatesToLock) {
				if (LogicObjectLockFinder.isEditable(logicObject)) {
					if (SingleLineHelper.isSingleLineMulticore(logicObject)) {
						continue;
					}
					lockedObjectNames.add(logicObject);
				}
			}
			if (!lockedObjectNames.isEmpty()) {
				String message = ResourceMgr.getString(getClass(), "LockLogicObjectsAction.lockSuccessful.msg",
						HTMLHelper.linkAll(lockedObjectNames));
				CAFUtils.getInstance().sendApplicationMessage(message);
			}
		}
		// todo creddy: When to return false?
		return true;
	}

	@Override public Object getValue(String key)
	{
		return values.get(key);
	}

	@Override public void putValue(String key, Object value)
	{
		values.put(key, value);
	}

	@Override public void setEnabled(boolean b)
	{

	}

	@Override public void addPropertyChangeListener(PropertyChangeListener listener)
	{

	}

	@Override public void removePropertyChangeListener(PropertyChangeListener listener)
	{

	}

	@Override public void actionPerformed(ActionEvent e)
	{
		doAction();
	}

	@Override public String getActionUIName()
	{
		return getClass().getName();
	}

	@Override public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (!getOperands(selections).isEmpty()) {
			final Action actionUI = getActionUI();
			assert actionUI != null;
			container.add(new ActionEntry(actionUI));
		}
	}

	@Override public void populateActiveCtxMenu(ActionContainer container)
	{

	}

	@Override public boolean isEnabled()
	{
		// todo: This is not working fine if multiple designs are opened in the session
		//preventive fix when model is null
		ICapletModel model = getController().getCapletModel();
		if (model == null || !model.isEditable()) {// eg. read-only model
			return false;
		}
		if (super.isEnabled()) {
			SelectSet selections = getController().getSelectMgr().getPreSelections();
			return !getOperands(selections).isEmpty();
		}
		return false;
	}

	private Collection<ILogicObject> getOperands(SelectSet selections)
	{
		Collection<ILogicObject> candidatesForLock = new HashSet<>();
		ILogicDesign logicDesign = null;
		for (SelectedUIDObjectIterator iter = selections.getSelectedUIDObjects(); iter.hasNext(); ) {
			IUIDObject uidObj = iter.getNext();
			ILogicObject logicObject = ReferenceHelper.reduceToLogicLockable(uidObj);
			if (logicObject != null) {
				final ILogicDesign parentDesign = logicObject.getLogicDesign();
				if (logicDesign == null) {
					logicDesign = parentDesign;
				}
				else if (logicDesign != parentDesign) {
					return Collections.emptySet();
				}
				candidatesForLock.add(logicObject);
			}
		}
		if (logicDesign != null && ((IPrivilegedCOGManagedLockableChildrenContainer) logicDesign).isWeakLocked()) {
			return candidatesForLock;
		}
		return Collections.emptySet();
	}
}