package chs.caplets.logic.actions;

import chs.utilities.IUpdateableDataUnit;
import chs.utilities.StringUtils;
import chs.utilities.UpdateableDataUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chandras on 01-07-2018.
 */
public class FootprintDevicePinKey implements IPrivilegedFootprintDevicePinKey
{

	@NotNull private IUpdateableDataUnit<String> m_data;

	public FootprintDevicePinKey(@Nullable String name)
	{
		m_data = new UpdateableDataUnit<>(name);
	}

	@NotNull public String getName()
	{
		return StringUtils.nonNull(m_data.getValue());
	}

	public void setName(@Nullable String name)
	{
		m_data = m_data.getUpdated(name);
	}
}
