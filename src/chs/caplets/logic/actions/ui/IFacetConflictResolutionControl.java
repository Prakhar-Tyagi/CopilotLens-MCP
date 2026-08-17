package chs.caplets.logic.actions.ui;

import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 09-03-2018.
 */
public interface IFacetConflictResolutionControl<T> extends IFacetConflictResolutionListener<T>
{

	void register(@NotNull IFacetConflictResolutionListener<T> listener);

	void unregister(@NotNull IFacetConflictResolutionListener<T> listener);
}
