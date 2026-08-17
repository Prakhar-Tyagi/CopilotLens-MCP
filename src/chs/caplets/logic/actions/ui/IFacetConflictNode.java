package chs.caplets.logic.actions.ui;

import chs.common.INamedPropertiedObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * @author chandras on 25-03-2018.
 */
public interface IFacetConflictNode
{

	boolean hasConflicts();

	@NotNull Collection<IFacetConflictInfo> getConflicts();

	@NotNull INamedPropertiedObject getNodeObject();

	@NotNull String getSourceName();

	@NotNull String getTargetName();

	@Nullable IFacetConflictInfo getAttributeInfo(@NotNull String attr);

	@NotNull Set<IFacetConflictInfo> getRelatedFacets(@NotNull IFacetConflictInfo info);

	void apply();
}
