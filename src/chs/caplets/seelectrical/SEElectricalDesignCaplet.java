package chs.caplets.seelectrical;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletLifecycle;
import chs.caplets.logic.Caplet;
import chs.caplets.logic.SEElectricalDesignLifeCycle;
import chs.caplets.shared.BaseCaplet;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.logic.SEElectricalDesignDRCDomainManager;
import chs.utilities.AppInfo;
import chs.utilities.FeatureLicense;
import chs.utilities.ILicenseNumbers;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;

import javax.swing.Icon;
import java.util.Collection;

@ApplicationSpecification(includeIn = {Application.SEElectricalDesign})

public class SEElectricalDesignCaplet extends Caplet
{

	public SEElectricalDesignCaplet()
	{
	}

	public boolean isLicensed()
	{
		return FeatureLicense.checkLicensePresent(ILicenseNumbers.VESYS2_DESIGN_FEATURE_ID);
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new SEElectricalDesignLifeCycle(this);
	}

	@Override protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(SEElectricalDesignDRCDomainManager.getInstance());
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(BaseCaplet.class, "Caplet.SEElectricalDesign.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Wiring";
	}

	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.SEELECTRICAL);
		return brandingImagery.getSmallIcon();
	}

	@Override public Type getType()
	{
		return Type.SEELECTRICAL_DESIGN;
	}
}
