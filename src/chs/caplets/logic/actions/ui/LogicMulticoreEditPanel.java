package chs.caplets.logic.actions.ui;

import chs.caf.caplet.helpers.MCProxy;
import chs.caf.caplet.helpers.MulticoreEditPanel;
import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.utility.helpers.LogicObjectLockFinder;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LogicMulticoreEditPanel extends MulticoreEditPanel
{

	public LogicMulticoreEditPanel(MCProxy root, COFTypeEnum type, boolean shieldEdit,
			int editScope, Set<IUID> selectedUIDS)
	{
		super(root, type, shieldEdit, editScope, selectedUIDS);
		m_avList.setCellRenderer(new CreateLogicMulticoreRenderer());
		m_mcTree.setCellRenderer(new CreateLogicMulticoreTreeRenderer());
	}

	protected void updateButtons()
	{
		super.updateButtons();
		Object[] selLeft = getLeftSideSelection();
		TreePath[] selRight = getRightSelections();

		if (selRight != null) {
			if (m_addButton.isEnabled()) {
				if (selLeft != null) {
					m_addButton.setEnabled(canAddInMultiUserSession(selLeft, selRight));
				}
			}

			if (m_removeButton.isEnabled()) {
				m_removeButton.setEnabled(canRemoveInMultiUserSession(selRight));
			}

			if (m_deleteButton.isEnabled()) {
				m_deleteButton.setEnabled(canDeleteInMultiUserSession(selRight));
			}

			if (m_modifyButton.isEnabled()) {
				m_modifyButton.setEnabled(canModifyInMultiUserSession(selRight));
			}
		}
	}

	private boolean canAddInMultiUserSession(@NotNull Object[] selLeft, @NotNull TreePath[] selRight)
	{
		// disable even if one of the selected values on left side is locked in another session
		// disable if the target proxy is locked in another session
		return isAvailableForEdit(selLeft) && isAvailableForEdit(selRight);
	}

	private boolean isAvailableForEdit(Object[] selLeft)
	{
		for (Object aSelLeft : selLeft) {
			MCProxy editCandidate = (MCProxy) aSelLeft;
			if (isMCProxyLockedInAnotherSession(editCandidate)) {
				return false;
			}
		}
		return true;
	}

	protected boolean isRemoveOKWhenParentIsOverbraid(boolean removeOK, MCProxy targetProxy)
	{
		if (targetProxy.isLibraryMulticoreRef()) {
			TreeNode node = targetProxy.getParent();
			MCProxy parentProxy = node instanceof MCProxy ? ((MCProxy) node) : null;
			if (parentProxy != null && parentProxy.isOverbraidRef()) {
				return true;
			}
		}
		return removeOK;
	}

	private boolean isAvailableForEdit(TreePath[] selRight)
	{
		for (TreePath treePath : selRight) {
			MCProxy proxyToBeEdited = (MCProxy) treePath.getLastPathComponent();
			MCProxy rootProxy = getRootProxy(proxyToBeEdited);
			if (isMCProxyLockedInAnotherSession(rootProxy)) {
				return false;
			}
		}
		return true;
	}

	private boolean canRemoveInMultiUserSession(@NotNull TreePath[] selRight)
	{
		return isAvailableForEdit(selRight);
	}

	private boolean canDeleteInMultiUserSession(@NotNull TreePath[] selRight)
	{
		return isAvailableForEdit(selRight);
	}

	private boolean canModifyInMultiUserSession(@NotNull TreePath[] selRight)
	{
		return isAvailableForEdit(selRight);
	}

	private boolean isMCProxyLockedInAnotherSession(@NotNull MCProxy mcProxy)
	{
		Set<ILogicObject> logicObjectsToBeCheckedForLock = new HashSet<ILogicObject>();
		IUIDObject logicRef = mcProxy.getRef();
		if (logicRef instanceof ILogicObject) {
			logicObjectsToBeCheckedForLock.add((ILogicObject) logicRef);
		}

		if (logicRef instanceof IMulticore) {
			logicObjectsToBeCheckedForLock.addAll(((IMulticore) logicRef).getAllConductorsInHierarchy(true));
			logicObjectsToBeCheckedForLock.addAll(((IMulticore) logicRef).getAllMulticoresInHierarchy());
		}

		return isAnyObjectLockedInAnotherSession(logicObjectsToBeCheckedForLock);
	}

	protected boolean isAnyObjectLockedInAnotherSession(Collection<ILogicObject> logicObjects)
	{
		return LogicObjectLockFinder.isAnyLockedInOtherSession(logicObjects);
	}

	private class CreateLogicMulticoreRenderer extends MulticoreEditPanel.CreateMulticoreRenderer
	{

		protected boolean shouldEnable()
		{
			return shouldEnableThisObject(mcp.getRef());
		}
	}

	private class CreateLogicMulticoreTreeRenderer extends MulticoreEditPanel.CreateMulticoreTreeRenderer
	{

		protected boolean shouldEnable()
		{
			return shouldEnableThisObject(mcp.getRef());
		}
	}

	private boolean shouldEnableThisObject(IUIDObject logicRef)
	{
		boolean enabled = true;
		if (logicRef instanceof ILogicObject && LogicObjectLockFinder.isLogicObjectLockedInOtherSession(
				(ILogicObject) logicRef)) { // see if logic object is locked in another session
			enabled = false;
		}
		return enabled;
	}
}
