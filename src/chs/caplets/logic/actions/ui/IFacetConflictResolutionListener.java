package chs.caplets.logic.actions.ui;

import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 09-03-2018.
 */
public interface IFacetConflictResolutionListener<T>
{

	void targetChanged(@Nullable T target);
}
