/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2006-2026 Siemens
 */
package chs.caplets.logic.actions.icdbrowser;

import chs.caf.annotations.Application;
import chs.caf.annotations.ApplicationSpecification;
import chs.caf.caplet.helpers.browser.LinkBrowserControl;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.links.LinkType;
import chs.images.CHSImageLoader;
import chs.utilities.ResourceMgr;
import chs.utility.ICDURLHelper;
import chs.utility.ICDUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

@ApplicationSpecification(includeIn = {Application.CapitalLogicDesigner},
		immersedMode = ApplicationSpecification.ImmersedMode.ACTION_ALLOWED)
public class ViewICDDetailsAction extends AbstractAction
{

	private IDeviceICD m_icd;

	public ViewICDDetailsAction(@NotNull IDeviceICD icd)
	{
		putValue(NAME, ResourceMgr.getStringForMenu(
				ViewICDDetailsAction.class, "ViewICDDetailsAction.name.text"));
		putValue(SHORT_DESCRIPTION, ResourceMgr
				.getString(ViewICDDetailsAction.class, "ViewICDDetailsAction.shortDesc.text"));
		putValue(LONG_DESCRIPTION, ResourceMgr
				.getString(ViewICDDetailsAction.class, "ViewICDDetailsAction.longDesc.text"));
		putValue(SMALL_ICON, CHSImageLoader.loadImageIcon("chs/images/general/toggle.png"));
		m_icd = icd;
	}

	@Override public void actionPerformed(ActionEvent e)
	{
		for (IICD iicd : ICDUtils.getAllICDs(m_icd)) {
			openInBrowser(iicd);
		}
	}

	private void openInBrowser(@NotNull IICD icd)
	{
		String icdUrl = new ICDURLHelper().constructICDURL(icd.getUID().getString());
		open(icdUrl);
	}

	protected void open(String icdUrl)
	{
		LinkBrowserControl.displayURL(LinkType.ICD, icdUrl);
	}
}