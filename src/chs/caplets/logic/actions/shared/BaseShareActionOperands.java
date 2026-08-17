/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions.shared;

import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IGenericInlineConnector;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.schem.IHighwaySchematic;
import chs.common.IUIDObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class BaseShareActionOperands
{

	// TODO jacobt FEAT13040 : remove these public fields once all the old clients no longer use them
	public IUIDObject target = null;
	public chs.cof.logical.schem.IPinList mate = null;

	private ILogicObject logicObject = null;
	private Set<IRepresentedObject> representations = new HashSet<IRepresentedObject>();
	@NotNull private OperandShareabilityStatus shareabilityStatus = OperandShareabilityStatus.Undetermined;

	@NotNull public OperandShareabilityStatus getShareabilityStatus()
	{
		return shareabilityStatus;
	}

	public void setShareabilityStatus(@NotNull OperandShareabilityStatus shareabilityStatus)
	{
		this.shareabilityStatus = shareabilityStatus;
	}

	/**
	 * Set the schematic objects that were selected when the share/unshare action is performed.
	 * <p/>
	 * Some actions affect all schems (e.g.), while some only the affect the specifed schems (e.g. Unshare)
	 *
	 * @param reps The schematic representations directly included in the action
	 */
	void setRepresentations(Set<IRepresentedObject> reps)
	{
		representations = reps;
	}

	/**
	 * Get the representations as above.
	 * <p/>
	 * Note that mate representions are not directly specified on this object, but are implied within the actions.
	 *
	 * @return The possibly empty set of schematic representations directly included in the action
	 */
	Set<IRepresentedObject> getRepresentations()
	{
		return representations;
	}

	/**
	 * Share can now work directly on a logic object rather than a single schem representation
	 *
	 * @return The logic object, if one was explicitly set via setLogicMate
	 */
	@Nullable public ILogicObject getLogicObject()
	{
		return logicObject;
	}

	/**
	 * The target uid object for a shared-based action. This can either be schem or connectivity object
	 *
	 * @return The target uid object
	 */
	@Nullable public IUIDObject getTarget()
	{
		return target;
	}

	/**
	 * Explicitly specify the logic object to use for a shared-based action
	 *
	 * @param logicObject The logic object
	 */
	public void setLogicObject(ILogicObject logicObject)
	{
		this.logicObject = logicObject;
	}

	/**
	 * Get the cable pinlist specified as target for these operands, or null if a pinlist was not specified
	 *
	 * @return The cable pinlist or null if not specified
	 */
	@Nullable public IPinList getCablePinList()
	{
		if (target instanceof chs.cof.logical.schem.IPinList) {
			// don't trust "weaken overly strong" cast messages
			//noinspection OverlyStrongTypeCast
			return ((chs.cof.logical.schem.IPinList) target).getConnectivity();
		}
		if (logicObject instanceof IPinList) {
			return (IPinList) logicObject;
		}
		return null;
	}

	/**
	 * Get the inline mate of the target for these operands, or null if the target is not an inline
	 *
	 * @return The cable inline mate or null if not specified or non-existent
	 */
	@Nullable public IGenericInlineConnector getCablePinListMate()
	{
		IPinList pinlist = getCablePinList();
		if (pinlist instanceof IGenericInlineConnector) {
			IGenericInlineConnector inline = (IGenericInlineConnector) pinlist;
			IConnector inlineMate = inline.getMate();
			if (inlineMate instanceof IGenericInlineConnector) {
				return (IGenericInlineConnector) inlineMate;
			}
		}
		return null;
	}

	/**
	 * Get the cable conductor specified as target for these operands, or null if a conductor was not specified
	 *
	 * @return The cable condcutor or null if not specified
	 */
	@Nullable public IConductor getCableConductor()
	{
		if (target instanceof chs.cof.logical.schem.IConductor) {
			// don't trust "weaken overly strong" cast messages
			//noinspection OverlyStrongTypeCast
			return ((chs.cof.logical.schem.IConductor) target).getConnectivity();
		}
		if (logicObject instanceof IConductor) {
			return (IConductor) logicObject;
		}
		return null;
	}

	/**
	 * Get the cable highway specified as target for these operands, or null if a highway was not specified
	 *
	 * @return The cable highway or null if not specified
	 */
	@Nullable public IHighway getHighway()
	{
		if (target instanceof IHighwaySchematic) {
			// don't trust "weaken overly strong" cast messages
			//noinspection OverlyStrongTypeCast
			return ((IHighwaySchematic) target).getConnectivity();
		}
		if (logicObject instanceof IHighway) {
			return (IHighway) logicObject;
		}
		return null;
	}

	/**
	 * Get the representations specified for the Share/Unshare operation as schem conductors.
	 *
	 * @return The possibly empty collection of schem conductors
	 */
	Collection<chs.cof.logical.schem.IConductor> getConductorRepresentations()
	{
		Collection<chs.cof.logical.schem.IConductor> condReps = new ArrayList<chs.cof.logical.schem.IConductor>();
		for (IRepresentedObject rep : representations) {
			if (rep instanceof chs.cof.logical.schem.IConductor) {
				condReps.add((chs.cof.logical.schem.IConductor) rep);
			}
		}
		return condReps;
	}

	/**
	 * Get the representations specified for the Share/Unshare operation as schem pinlists
	 *
	 * @return The possibly empty collection of schem pinlists
	 */
	public Collection<chs.cof.logical.schem.IPinList> getPinListRepresentations()
	{
		Collection<chs.cof.logical.schem.IPinList> plReps = new ArrayList<chs.cof.logical.schem.IPinList>();
		for (IRepresentedObject rep : representations) {
			if (rep instanceof chs.cof.logical.schem.IPinList) {
				plReps.add((chs.cof.logical.schem.IPinList) rep);
			}
		}
		return plReps;
	}

	/**
	 * Get the representations specified for the Share/Unshare operation as schem highways.
	 *
	 * @return The possibly empty collection of schem highway
	 */
	Collection<IHighwaySchematic> getHighwayRepresentations()
	{
		Collection<IHighwaySchematic> highwayReps = new ArrayList<IHighwaySchematic>();
		for (IRepresentedObject rep : representations) {
			if (rep instanceof IHighwaySchematic) {
				highwayReps.add((IHighwaySchematic) rep);
			}
		}
		return highwayReps;
	}
}
