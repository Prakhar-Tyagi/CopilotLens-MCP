package chs.caplets.logic.actions.ui;

import chs.cof.logical.IAbstractMulticore;
import chs.cof.logical.cable.IBackshell;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDeviceConnector;
import chs.cof.logical.cable.IDeviceOwned;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.utilities.CollectionUtils;
import chs.utilities.SetMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author chandras on 09-03-2018.
 */
public abstract class FacetConflictResolutionModel<S, T> implements IFacetConflictResolutionModel,
		IFacetConflictResolutionControl<T>
{

	@NotNull protected final S m_source;
	protected final SetMap<IFacetConflictNode, IFacetConflictNode> m_conflictObjectTree =
			SetMap.createShallowSetMap(true);
	protected final Set<IFacetConflictNode> m_topNodes = new LinkedHashSet<>();
	private final Set<IFacetConflictResolutionListener<T>> m_listeners = new LinkedHashSet<>(2);

	protected FacetConflictResolutionModel(@NotNull S source)
	{
		m_source = source;
	}

	protected void addConflictTreeObject(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			@NotNull ILogicObject target)
	{
		IFacetConflictNode targetConflictNode = conflictNodes.get(target);
		if (target instanceof IGenericPin) {
			addConflictTreeObject(conflictNodes, targetConflictNode, ((IGenericPin) target).getOwner());
		}
		else if (target instanceof IBackshell) {
			addConflictTreeObject(conflictNodes, targetConflictNode, ((IBackshell) target).getOwner());
		}
		else if (target instanceof IDeviceConnector) {
			addConflictTreeObject(conflictNodes, targetConflictNode, ((IDeviceOwned) target).getOwner());
		}
		else if (target instanceof IConductor && ((IConductor) target).getMulticore() != null) {
			addConflictTreeObject(conflictNodes, targetConflictNode, ((IConductor) target).getMulticore());
		}
		else if (target instanceof IMulticore && ((IAbstractMulticore) target).getParent() != null) {
			addConflictTreeObject(conflictNodes, targetConflictNode, ((IMulticore) target).getParent());
		}
		else {
			m_conflictObjectTree.add(targetConflictNode);
			m_topNodes.add(targetConflictNode);
		}
	}

	private void addConflictTreeObject(@NotNull Map<ILogicObject, IFacetConflictNode> conflictNodes,
			IFacetConflictNode targetConflictNode, @Nullable ILogicObject owner)
	{
		if (owner != null) {
			IFacetConflictNode ownerFacetConflictNode = conflictNodes.get(owner);
			if (ownerFacetConflictNode != null) {
				addConflictTreeObject(conflictNodes, owner);
				m_conflictObjectTree.add(ownerFacetConflictNode, targetConflictNode);
			}
		}
	}

	protected void reset()
	{
		m_topNodes.clear();
		m_conflictObjectTree.clear();
	}

	@NotNull public Set<IFacetConflictNode> getTopNodes()
	{
		return Collections.unmodifiableSet(m_topNodes);
	}

	@NotNull public Set<IFacetConflictNode> getChildNodes(@NotNull IFacetConflictNode node)
	{
		return m_conflictObjectTree.pullReadOnlySafeSet(node);
	}

	@Override public void apply()
	{
		for (IFacetConflictNode node : getTopNodes()) {
			doApply(node);
		}
	}

	private void doApply(@NotNull IFacetConflictNode node)
	{
		for (IFacetConflictNode child : getChildNodes(node)) {
			doApply(child);
		}
		node.apply();
	}

	protected abstract void doTargetChanged(@NotNull T target);

	@Override public final void targetChanged(@Nullable T target)
	{
		reset();
		if (target != null) {
			doTargetChanged(target);
		}
		notify(target);
	}

	private void notify(@Nullable T target)
	{
		m_listeners.forEach((l) -> l.targetChanged(target));
	}

	@Override public void register(@NotNull IFacetConflictResolutionListener<T> listener)
	{
		m_listeners.add(listener);
	}

	@Override public void unregister(@NotNull IFacetConflictResolutionListener<T> listener)
	{
		m_listeners.remove(listener);
	}

	@NotNull @Override public Set<IFacetConflictInfo> getRelatedFacets(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info)
	{
		return node.getRelatedFacets(info);
	}

	@Nullable protected IConnector deriveInlineMate(@NotNull IGenericInlineConnector connector)
	{
		Set<IConnector> mates = CollectionUtils.getSafeSet(connector.getMates());
		return mates.isEmpty() ? null : mates.iterator().next();
	}
}
