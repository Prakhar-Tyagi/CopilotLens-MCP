package chs.caplets.logic.shared;

import chs.caplets.logic.actions.shared.MulticoreSharedPanel;
import chs.caplets.logic.actions.ui.MCNode;
import chs.caplets.logic.actions.ui.MCNodeCreator;
import chs.caplets.logic.actions.ui.MCSharedMatcher;
import chs.caplets.logic.actions.ui.MCSharedNode;
import chs.caplets.logic.actions.ui.MCSharedNodeCreator;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedObject;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.tree.IObjectUIFilterOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SharedMulticoreStructureMatchFilter implements IObjectUIFilterOption
{

	@Nullable private SharedAbstractableTree m_sharedAbstractableTree;
	private MCNode multicoreNode;
	private boolean m_StrictMatch;
	private Set<ISharedObject> m_structureMatched = new HashSet<>();

	public void setSharedAbstractableTree(@NotNull SharedAbstractableTree sharedAbstractableTree)
	{
		m_sharedAbstractableTree = sharedAbstractableTree;
	}

	public void setLocalMulticore(@NotNull IMulticore multicore)
	{
		multicoreNode = Objects.requireNonNull(new MCNodeCreator(multicore).execute());
	}

	@Nullable @Override public Icon getIcon(@NotNull Object obj)
	{
		return null;
	}

	@Override public boolean filterIn(@NotNull Object obj)
	{
		return !m_structureMatched.contains(obj);
	}

	public void setMatched(ISharedObject sharedObject, boolean structureChanged)
	{
		if (structureChanged) {
			m_structureMatched.add(sharedObject);
		}
		else {
			m_structureMatched.remove(sharedObject);
		}
	}

	@NotNull @Override public String getDescription(@NotNull Object obj)
	{
		return ResourceMgr.getString(MulticoreSharedPanel.class, "MulticoreSharedPanel.structure.mismatch.text");
	}

	public boolean selected(@NotNull Object obj)
	{
		return doFilter(obj);
	}

	private boolean doFilter(@NotNull Object obj)
	{
		ISharedMulticore newSharedMulticore = CommonUtils.cast(obj, ISharedMulticore.class);
		if (newSharedMulticore == null) {
			return true;
		}
		MCSharedMatcher mcSharedMatcher = new MCSharedMatcher();
		MCSharedNode sharedMulticoreNode = Objects.requireNonNull(
				new MCSharedNodeCreator(newSharedMulticore).execute());
		if (mcSharedMatcher.isMatched(m_StrictMatch, multicoreNode, sharedMulticoreNode)) {
			setMatched(newSharedMulticore, false);
			return true;
		}
		else {
			Message.show(PromptSeverity.INFORMATION, MulticoreSharedPanel.class, "MulticoreSharedPanel.structure.mismatch");
			if (m_sharedAbstractableTree != null) {
				TreePath path = m_sharedAbstractableTree.getSelectionPath();
				if (path != null) {
					DefaultTreeModel model = (DefaultTreeModel) m_sharedAbstractableTree.getModel();
					MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
					model.removeNodeFromParent(node);
				}
			}
			setMatched(newSharedMulticore, true);
			return false;
		}
	}

	public void setStrictMatch(boolean isStrictMatch)
	{
		m_StrictMatch = isStrictMatch;
	}
}
