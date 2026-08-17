/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.symbol.analysis;

import chs.analysis.IAnalysisAttachmentTargetProvider;
import chs.caplets.symbol.Model;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author rharring
 */

public class SymbolAnalysisAttachmentTargetProvider implements IAnalysisAttachmentTargetProvider
{

	/**
	 * The model
	 */
	protected Model m_model;

	/**
	 * Creates a new instance of LogicAnalysisAttachmentTargetProvider
	 */
	public SymbolAnalysisAttachmentTargetProvider(Model model)
	{
		m_model = model;
	}

	/**
	 * This method gets a collection containing the objects ( which should be IAnalysable ) which this attachment operation
	 * targets, typically the selection.
	 * <p/>
	 * These targets may be multiple instances of the same component / device or a single instance. Mixtures of instances
	 * should NOT be provided.
	 *
	 * @return Collection, the objects selected.
	 */
	public Collection getTargets()
	{
		ArrayList v = new ArrayList();

		v.add(m_model.getSymbolDef());

		return v;
	}

	/**
	 * This method gets a string representation of the design's uid
	 *
	 * @return String, the design's uid.
	 */
	public String getUID()
	{
		return m_model.getSymbolDef().getUID().getString();
	}

	/**
	 * This method gets the symbol definition of the given targets. This should return null to disable symbol attachment.
	 *
	 * @param v - the selected targets
	 *
	 * @return IUIDObject, a fullyLoaded ISymbolDef or null to indicate no symbol attachment possible
	 */
	public IUIDObject getSymbolTarget(Collection v)
	{
		// we return null as we are in CapitalSymbol so don't have a differentiation
		// between instance and symbol.
		return null;
	}

	/**
	 * This method provides the id of the subsystem desriptor attached to the project. I.e. get's the design's project,
	 * queries the analysis manager for the subsystem id.
	 *
	 * @return int, the attached subsystem -- always -1 for a symbol
	 */
	public int getProjectSubsystemId()
	{
		return -1;
	}

	@Override public boolean isReadOnlyTarget(IUIDObject object)
	{
		return false;
	}

	@Override public boolean isPromotionTabEnabled()
	{
		return true;
	}

    @Nullable @Override public IProject getProject()
    {
        return null;
    }

    @Override public boolean doesContainerSupportActionInMUMode()
    {
        return true;
    }

	@Nullable @Override public String getDisabledTooltip()
	{
		return null;
	}
}