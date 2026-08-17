package chs.caplets.logic.actions;

/**
 * Created with IntelliJ IDEA. User: nagamani Date: 6/2/14
 */

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICaplet;
import chs.caf.caplet.helpers.ActionUI;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner, Application.CapitalEssentialsDesign, Application.SEElectricalDesign},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class ConvertPinTypeActionUI extends ActionUI
{

	private String m_name;

	public ConvertPinTypeActionUI(ICaplet caplet)
	{
		super(caplet);
	}

	public void setName(String name)
	{
		m_name = name;
	}

	@Override public void setupUI()
	{
		putValue(NAME, m_name);
	}

	@Override public String getActionClass()
	{
		return ConvertPinTypeAction.class.getName();
	}
}
