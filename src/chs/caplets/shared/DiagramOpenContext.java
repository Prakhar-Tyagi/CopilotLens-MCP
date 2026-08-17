package chs.caplets.shared;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DiagramOpenContext
{

	@NotNull private List<?> context;
	private boolean isForReadOnly;

	public DiagramOpenContext(@NotNull List<?> context, boolean isForReadOnly)
	{
		this.context = context;
		this.isForReadOnly = isForReadOnly;
	}

	@NotNull public List<?> getContext()
	{
		return context;
	}

	public boolean isForReadOnly()
	{
		return isForReadOnly;
	}
}
