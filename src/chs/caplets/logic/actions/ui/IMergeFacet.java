package chs.caplets.logic.actions.ui;

import chs.common.ValueTypeEnum;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 01-04-2018.
 */
public interface IMergeFacet extends IFacetStature
{

	@NotNull String getName();

	@NotNull String getDisplayName();

	@Nullable String getValue();

	@Nullable String getRawValue();

	ValueTypeEnum getType();

	boolean isEqual(@NotNull IMergeFacet otherFacet);
}
