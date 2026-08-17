/*
 * Copyright 2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletLifecycle;
import chs.caplets.shared.BaseCaplet;
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.logic.VeSysDesignDRCDomainManager;
import chs.utilities.AppInfo;
import chs.utilities.FeatureLicense;
import chs.utilities.ILicenseNumbers;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;

import javax.swing.Icon;
import java.util.Collection;

/**
 * FEAT13132 - VeSys Packaging.
 * <p/>
 * This is the caplet used for VeSys Design.
 * <p/>
 *
 * @author rjoseph
 */
@ApplicationSpecification(includeIn = {Application.CapitalEssentialsDesign})

public class VeSysDesignCaplet extends Caplet
{

	public VeSysDesignCaplet()
	{
	}

	public boolean isLicensed()
	{
		return FeatureLicense.checkLicensePresent(ILicenseNumbers.VESYS2_DESIGN_FEATURE_ID);
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new VeSysDesignLifecycle(this);
	}

	@Override protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(VeSysDesignDRCDomainManager.getInstance());
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	public String getDesignType()
	{
		return ResourceMgr.getString(BaseCaplet.class, "Caplet.WiringDesign.Type");
	}

	@Override public String getUnlocalizedDesignType()
	{
		return "Wiring";
	}

	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.CAPITAL_ESSENTIALS);
		return brandingImagery.getSmallIcon();
	}

	@Override public Type getType()
	{
		return Type.VESYS_DESIGN;
	}
}
