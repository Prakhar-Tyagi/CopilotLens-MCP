/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic;

import chs.caf.IFIB;
import chs.caf.IResource;
import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.ICapletLifecycle;
import chs.utilities.AppInfo;
import chs.utilities.ui.BrandingHelper;
import chs.utilities.ui.BrandingImagery;
import chs.utility.ui.HelpMgr;
import chs.utility.ui.IHelpMgr;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/**
 * FEAT14997 - Offline Service Documentation User: kayyagar Date: Oct 12, 2010 Time: 7:28:07 PM
 */

@ApplicationSpecification(includeIn = {Application.SvcDoc})
public class SvcDocCaplet extends Caplet
{

	public SvcDocCaplet()
	{
	}

    public boolean isLicensed()
	{
		//return FeatureLicense.checkLicensePresent(ILicenseNumbers.SVCDOC_ID);
		return true;
	}

	protected ICapletLifecycle createLifecycle()
	{
		return new SvcDocLifeCycle(this);
	}

	/**
	 * @return name of the designs created by this caplet
	 */
	/*public String getDesignType()
	{
		return ResourceMgr.getString(BaseCaplet.class, "Caplet.Design.Type");
	}
*/
	public Icon getIcon()
	{
		BrandingImagery brandingImagery = BrandingHelper.getBrandingImagery(AppInfo.App.SERVICE_DOC);
		return brandingImagery.getSmallIcon();
	}

	public String getName()
	{
		return AppInfo.getFullApplicationName(AppInfo.App.SERVICE_DOC);
	}

	protected IResource createResource()
	{
		return new SvcDocResource(this);
	}
	
	public void initializeCaplet(@NotNull IFIB fib)
	{
		super.initializeCaplet(fib);
	}

	@Override
	protected void initialiseHelpSets()
	{
		super.initialiseHelpSets();

		HelpMgr.getInstance().addHelpSet(IHelpMgr.HelpSet.PUBLISHER, false);
	}

	@Override public Type getType()
	{
		return Type.SVC_DOC;
	}

	@Override
	public boolean shouldCheckSelectionAllowedForDelete()
	{
		return false;
	}
}
