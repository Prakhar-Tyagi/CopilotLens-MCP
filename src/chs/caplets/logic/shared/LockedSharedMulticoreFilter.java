package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.MulticoreSharedPanel;
import chs.cof.logical.shared.ISharedMulticore;
import chs.common.RefreshStatusEnum;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utility.logic.ISharedObjectAvailabilityReporter;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

public class LockedSharedMulticoreFilter extends LockedSharedObjectFilter
{
	@NotNull @Override public String getDescription(@NotNull Object obj)
	{
		return ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.DisableReason.Restricted.text");
	}
	public boolean selected(@NotNull Object obj)
	{
		ISharedMulticore newSharedMulticore = CommonUtils.cast(obj, ISharedMulticore.class);
		if (newSharedMulticore == null) {
			return true;
		}
		if (!newSharedMulticore.lock()) {
			setLockedOut(newSharedMulticore, true);
			return false;
		}
		RefreshStatusEnum rs = newSharedMulticore.refresh();
		if (RefreshStatusEnum.eObjectDoesNotExist.equals(rs)) {
			Message.show(PromptSeverity.WARNING, MulticoreSharedPanel.class, "MulticoreSharedPanel.sharedObject.deleted");
			if (m_sharedPinListTree != null) {
				TreePath path = m_sharedPinListTree.getSelectionPath();
				if (path != null) {
					DefaultTreeModel model = (DefaultTreeModel) m_sharedPinListTree.getModel();
					MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
					model.removeNodeFromParent(node);
				}
			}
			return false;
		}
		final ISharedObjectAvailabilityReporter nullReporter = ISharedObjectAvailabilityReporter.NULL_REPORTER;
		if (!isSharedObjectAvailable(newSharedMulticore, m_design, nullReporter)) {
			newSharedMulticore.unlock();
			setLockedOut(newSharedMulticore, true);
			return false;
		}
		if (!isSharedObjectEditable(newSharedMulticore, m_design, nullReporter)) {
			newSharedMulticore.unlock();
			setLockedOut(newSharedMulticore, true);
			return false;
		}
		setLockedOut(newSharedMulticore, false);
		return true;
	}
}
