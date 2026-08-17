/*
 * Copyright 2006 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.   
 */
package chs.caplets.logic;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.cof.logical.shared.IDesignWideUsageMgr;
import chs.common.IDesignContainer;
import chs.common.IObjectFilter;
import chs.common.IUID;
import chs.utilities.CollectionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MCShieldSchemFinder
{
	private final IShieldConductor shield;

	public MCShieldSchemFinder(IShieldConductor shield)
	{
		this.shield = shield;
	}

	public Iterator<IDiagramObject> getRepresentations()
	{
		IDesignWideUsageMgr dwum = getDWUM();

		assert dwum != null : "Usage manager is null";

		if(dwum != null){
			Collection<IDiagramObject> representations = dwum.getRepresentations(shield);

			Set<IAbstractPin> pins = getSchemReachablePins(representations);
			getPinsInConnectivityOnly(pins);
			// Shall we have pins available from connectivity only, they may be reachable through a daisy chain.
			if(!pins.isEmpty()){
				Set<IUID> diagrams = new HashSet<IUID>();
				dwum.getMulticoreDiagrams(shield.getMulticore(), diagrams);
				Set<IDiagramObject> shieldBodies = new HashSet<IDiagramObject>();
				ILogicDesign design = (ILogicDesign)shield.getDesignContainer();
				for(IUID diagramuid : diagrams){
					CollectionUtils.add(design.getDiagram(diagramuid).getRepresentations(shield.getMulticore().getShieldBody().getUID()), shieldBodies);
				}
				representations.addAll(getShieldsFromDaisyChains(shieldBodies));
			}
			return representations.iterator();
		}
		return Collections.<IDiagramObject>emptySet().iterator();
	}

	public Collection<IDiagramObject> getShieldsFromDaisyChains(Collection<IDiagramObject> representations)
	{
		Set<IDiagramObject> shields = new HashSet<IDiagramObject>();
		for (IDiagramObject rep : representations) {
			IShieldBody shBody = (IShieldBody) rep;
			for (IShieldBodyHookup hookup : shBody.getShieldBodyHookups()) {
				Collection<IPin> indirectlyConnectedPins = new ArrayList<>();
				hookup.getIndirectlyConnectPins(indirectlyConnectedPins);
				for (IPin pin : indirectlyConnectedPins) {
					if (pin.getJoint() != null) {
						Set<ISegment> connectedShields = pin.getJoint().getAssociations(
								new IObjectFilter()
								{
									public boolean accept(Object obj)
									{
										return obj != null && obj instanceof ISegment &&
												((ISegment)obj).getConductor().getConnectivity() instanceof IShieldConductor;
									}
								});
						for (ISegment segment : connectedShields) {
							shields.add(segment.getParent());
						}
					}
				}
			}
		}
		removeShieldsWithPinsNotInConnectivity(shields);
		return shields;
	}

	private void removeShieldsWithPinsNotInConnectivity(Set<IDiagramObject> shields)
	{
		Set<IConductor> invalidShields = new HashSet<IConductor>();
		for(IDiagramObject obj : shields){
			IConductor shield = (IConductor) obj;
			boolean valid = false;
			for(IPin p : shield.getPins()){
				if(this.shield.getPinSet().contains(p.getConnectivity())){
					valid = true;
				}
			}
			if(!valid){
				invalidShields.add(shield);
			}
		}
		shields.removeAll(invalidShields);
	}

	private Set<IAbstractPin> getSchemReachablePins(Collection<IDiagramObject> representations)
	{
		Set<IAbstractPin> pins = new HashSet<IAbstractPin>();
		for(IDiagramObject rep : representations){
			if (rep instanceof IConductor) {
				IConductor conductor = (IConductor) rep;
				for (IPin p : conductor.getPins()) {
					pins.add(p.getConnectivity());
				}
			}
		}
		return pins;
	}

	private void getPinsInConnectivityOnly(Set<IAbstractPin> schemReachablePins)
	{
		for(IAbstractPin pin : shield.getPins()){
			if(schemReachablePins.contains(pin)){
				schemReachablePins.remove(pin);
			}
			else{
				schemReachablePins.add(pin);
			}
		}
	}

	@Nullable public IDesignWideUsageMgr getDWUM()
	{
		IConnectivity connectivity = shield.getConnectivity();
		if(connectivity != null){
			IDesignContainer dContainer = connectivity.getDesign();
			if(dContainer != null && dContainer instanceof ILogicDesign){
				return ((ILogicDesign)dContainer).getDesignWideUsageMgr();
			}
		}
		return null;
	}
}
