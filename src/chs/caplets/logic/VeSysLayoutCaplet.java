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
import chs.ctf.drc.IDRCDomainManager;
import chs.ctf.drc.logic.VesysLayoutDRCDomainManager;

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

public class VeSysLayoutCaplet extends LayoutCaplet
{

	public VeSysLayoutCaplet()
	{
	}

	@Override protected void addDRCDomainManagers(Collection<IDRCDomainManager> drcs)
	{
		drcs.add(VesysLayoutDRCDomainManager.getInstance());
	}
}
