package chs.caplets.logic.icd;

import chs.cof.icd.IDeviceICD;
import chs.cof.library.PSDLibraryPartSelection;
import chs.cof.logical.IDesign;
import chs.cof.parts.partselector.IICDSelection;
import chs.common.IUID;
import chs.system.UIDMgr;

/**
 * Created with IntelliJ IDEA. User: ksistla Date: 3/6/14 Time: 5:41 PM To change this template use File | Settings |
 * File Templates.
 */

public class ICDSelection extends PSDLibraryPartSelection implements IICDSelection
{

	private IDeviceICD m_icd;
	private IUID m_design;

	ICDSelection(IDesign design)
	{
		m_design = design.getUID();
	}

	public ICDSelection(IDeviceICD icd, IDesign design)
	{
		m_icd = icd;
		m_design = design.getUID();
	}

	@Override public String getSelectedDeviceName()
	{
		IDeviceICD icd = getICD();
		return icd != null ? icd.getRole() : "";
	}

	@Override public IDeviceICD getICD()
	{
		return m_icd;
	}

	@Override public IDesign getDesign()
	{
		return (IDesign) UIDMgr.getObject(m_design);
	}
}
