package chs.caplets.logic.actions.ui;

import chs.common.ValueTypeEnum;
import org.jetbrains.annotations.NotNull;

/**
 * @author chandras on 25-03-2018.
 */
public interface IFacetConflictInfo
{

	boolean isAttribute();

	@NotNull String getDisplayName();

	@NotNull String getName();

	@NotNull String getSourceValue();

	@NotNull String getTargetValue();

	@NotNull String getSourceRawValue();

	@NotNull String getTargetRawValue();

	@NotNull ValueTypeEnum getSourceType();

	@NotNull ValueTypeEnum getTargetType();

	@NotNull ValueTypeEnum getResultType();

	void setUserChoice(@NotNull ValueOption choice);

	@NotNull ValueOption getUserChoice();

	void setResult(@NotNull String result);

	@NotNull String getResult();

	@NotNull String getRawResult();
}
