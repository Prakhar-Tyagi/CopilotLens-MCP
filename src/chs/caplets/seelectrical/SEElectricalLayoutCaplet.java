package chs.caplets.seelectrical;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caplets.logic.LayoutCaplet;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.logic.SEElectricalLayoutDRCDomainManager;

import java.util.Collection;

@ApplicationSpecification(includeIn = {Application.SEElectricalDesign})

public class SEElectricalLayoutCaplet extends LayoutCaplet
{

	public SEElectricalLayoutCaplet()
	{
	}

	@Override protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(SEElectricalLayoutDRCDomainManager.getInstance());
	}
}
