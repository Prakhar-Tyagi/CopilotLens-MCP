package chs.caplets.logic.actions.ui;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author chandras on 09-03-2018.
 */
public interface IFacetConflictResolutionModel
{

	@NotNull Set<IFacetConflictNode> getTopNodes();

	@NotNull Set<IFacetConflictNode> getChildNodes(@NotNull IFacetConflictNode node);

	@NotNull Set<IFacetConflictInfo> getRelatedFacets(@NotNull IFacetConflictNode node,
			@NotNull IFacetConflictInfo info);

	void apply();
}
