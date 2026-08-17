package chs.caplets.logic.actions.ui;

import chs.cof.logical.cable.IFunctionConductor;
import org.jetbrains.annotations.Nullable;

public class SignalData
{
	private String instanceName;
	private String dictionaryName;
	@Nullable private IFunctionConductor iFunctionConductor;
	private boolean active;
	private boolean presentInDictionary;

	SignalData(@Nullable IFunctionConductor iFunctionConductor, String instanceName,
			@Nullable Object dictionaryName, boolean active, boolean presentInDictionary)
	{
		this.instanceName = instanceName;
		this.dictionaryName = (dictionaryName == null ? "" : dictionaryName.toString());
		this.iFunctionConductor = iFunctionConductor;
		this.active = active;
		this.presentInDictionary = presentInDictionary;
	}

	public String getInstanceName()
	{
		return instanceName;
	}

	public String getDictionaryName()
	{
		return dictionaryName;
	}

	@Nullable public IFunctionConductor getiFunctionConductor()
	{
		return iFunctionConductor;
	}

	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		this.active = active;
	}

	public boolean isPresentInDictionary()
	{
		return presentInDictionary;
	}
}
