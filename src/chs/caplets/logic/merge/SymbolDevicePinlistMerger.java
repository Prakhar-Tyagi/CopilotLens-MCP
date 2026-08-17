/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

package chs.caplets.logic.merge;

import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlock;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.IInternalPinIterator;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.ISymbolRef;
import chs.common.IUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by IntelliJ IDEA. User: melmorsy Date: 17-Mar-2010 Time: 14:27:53
 */

public class SymbolDevicePinlistMerger extends DevicePinlistMerger
{

	private Set<ILogicObject> deferredDeleteInternalConnectivityObject = null;

	protected SymbolDevicePinlistMerger(ILogicObject sourceLogicObject, ILogicObject targetLogicObject,
			@NotNull IMergeActionChangeReporter reporter)
	{
		super(sourceLogicObject, targetLogicObject, reporter);
		deferredDeleteInternalConnectivityObject = new HashSet<ILogicObject>();
	}

	protected Set<ILogicObject> getDeferredDeleteInternalConnectivityObjects()
	{
		return Collections.unmodifiableSet(deferredDeleteInternalConnectivityObject);
	}

	@Override protected void mergeConnectivity(ILogicObject sourceLogicObject, ILogicObject targetLogicObject)
	{
		// merge the links first because removing an internal pin will remove its reference from the links.
		// when removing an internal pin, it loops over all links in the device and remove its reference from the link
		// so if the links are moved first we will preserve its references to internal pins.
		if (sourceLogicObject instanceof IDevice && targetLogicObject instanceof IDevice) {
			mergInternalLinks((IDevice) sourceLogicObject, (IDevice) targetLogicObject);

			mergeInternalPins((IDevice) sourceLogicObject, (IDevice) targetLogicObject);
		}

		super.mergeConnectivity(sourceLogicObject, targetLogicObject);
		// merge the symbol refs here.
		IDevice source = (IDevice) sourceLogicObject;
		IDevice target = (IDevice) targetLogicObject;

		mergeSymbolReferences(source, target);

		mergeInternalAnalysisModels(source, target);
	}

	@Override protected void fixupConnections(IAbstractPin pin, IAbstractPin targetPin)
	{
		super.fixupConnections(pin, targetPin);
		if (pin instanceof IDevicePin && targetPin instanceof IDevicePin) {
			fixupInternalLinks(pin, targetPin);
		}
	}

	protected void fixupInternalLinks(IGenericPin pin, IGenericPin targetPin)
	{
		// if we are merging pin into targetPin, we need to make sure that there is no links still referencing
		// pin. if there is, set it to reference targetPin
		IDevice targetDevice = (IDevice) getTargetLogicObject();
		for (IInternalLink lnk : targetDevice.getInternalLinkCollection()) {
			if (lnk.getStartPin() == pin) {
				lnk.setStartPin(targetPin);
			}
			if (lnk.getEndPin() == pin) {
				lnk.setEndPin(targetPin);
			}

			// This is primarily to fix dts0100691868
			// Merging to devices having exactly the same symbol definition would move the internal connections from source to target
			// we would still need to set the link's block ref to the target pin's one, if they are not teh same already
			if (lnk.getBlockRef() != targetPin.getBlockRef()) {
				lnk.setBlockRef(targetPin.getBlockRef());
			}
		}
	}

	@Override protected void mergeSchematic(IConnectivityRef sourceSchemObject, ILogicObject targetlogicObject)
	{
		super.mergeSchematic(sourceSchemObject,	targetlogicObject);
		for (IInternalSchemPin internalSchemPin : ((IPinList) sourceSchemObject).getInternalPins()) {
			internalSchemPin.setConnectivity((IGenericPin) getMappedValue(internalSchemPin.getConnectivity()));
		}
		for (ISchemInternalLink internalSchemLink : ((IPinList) sourceSchemObject).getInternalLinks()) {
			internalSchemLink.setConnectivity((IInternalLink) getMappedValue(internalSchemLink.getConnectivity()));
		}
	}

	private void mergeInternalAnalysisModels(IDevice source, IDevice target)
	{
		// if the source is composite symbol or has a composite symbol. it will be ignored by merging symbol references
		Collection<IBlock> blocks = new HashSet<IBlock>(target.getBlocks());

		for (IBlock blk : source.getBlocks()) {
			source.removeBlock(blk);
			target.addBlock(blk);
		}

		removeUnnecessaryAnalysisModels(target, blocks);
	}

	private void removeUnnecessaryAnalysisModels(IDevice target, Collection<IBlock> targetBlocks)
	{
		Collection<IBlock> blocks = new HashSet<IBlock>(target.getBlocks());
		// first filter referenced blocks
		// we have to keep all blocks that were on target in the first place. 
		blocks.removeAll(targetBlocks);
		for (IAbstractPin p : target.getPins()) {
			removeBlockFromList(new PinBlockRefAdapter(p), blocks);
		}
		for (IInternalPin internalPin : target.getInternalPins()) {
			removeBlockFromList(new PinBlockRefAdapter(internalPin), blocks);
		}
		for (IInternalLink internalLink : target.getInternalLinkCollection()) {
			removeBlockFromList(new InternalLinkBlockRefAdapter(internalLink), blocks);
		}
		// the remaining blocks are the ones unreferenced by external pins, we want to delete them,
		// and also delete internal connectivity that reference them.
		addDeferredInternalConnectivityObjectsForDelete(blocks, target);

		for (Iterator<IBlock> itr = blocks.iterator(); itr.hasNext();) {
			IBlock blk = itr.next();
			target.removeBlock(blk);
			blk.delete();
			itr.remove();
		}

		// we can't do the same with internal links and pins, because they may have different attributes.
	}

	private void addDeferredInternalConnectivityObjectsForDelete(Collection<IBlock> blocks, IDevice target)
	{
//		deferredDeleteInternalConnectivityObject = new HashSet<ILogicObject>();
		for (IBlock blk : blocks) {
			addInternalConnectivityForBlock(blk, target, deferredDeleteInternalConnectivityObject);
		}
	}

	private void addInternalConnectivityForBlock(IBlock blk, IDevice dev, Set<ILogicObject> connectivity)
	{
		for (IInternalPin ipin : dev.getInternalPins()) {
			if (ipin.getBlockRef() == blk.getUID()) {
				connectivity.add(ipin);

				// get the links that reference this pin also.
				for (IInternalLink lnk : dev.getInternalLinkCollection()) {
					if (lnk.getStartPin() == ipin || lnk.getEndPin() == ipin) {
						connectivity.add(lnk);
					}
				}
			}
		}
	}

	private void removeBlockFromList(BlockRefContainerAdapter p, Collection<IBlock> blocks)
	{
		IUID blockUID = p.getBlockRef();
		if (blockUID != null) {
			IBlock blk = getBlock((IDevice) p.getOwner(), blockUID);
			if (blk != null) {
				blocks.remove(blk);
			}
		}
	}

	@Nullable private IBlock getBlock(IDevice dev, IUID blockUID)
	{
		for (IBlock blk : dev.getBlocks()) {
			if (blk.getUID() == blockUID) {
				return blk;
			}
		}
		return null;
	}

	protected void mergeSymbolReferences(IDevice source, IDevice target)
	{
		Set<ISymbolRef> refs = new HashSet<ISymbolRef>(source.getSymbolReferences());
		for (ISymbolRef ref : refs) {
			source.removeSymbolRefIfCanMaintainMultipleSymbols(ref);
			target.addSymbolRefIfCanMaintainMultipleSymbols(ref);
		}
	}

	public static Mergeable areMergable(IDevice source, IDevice target)
	{
		boolean multiSymbol_source = source.canMaintainMultipleSymbols();
		boolean multiSymbol_targer = target.canMaintainMultipleSymbols();

		if (!(multiSymbol_targer && multiSymbol_source)) {
			return Mergeable.DifferentObjectTypes;
		}

		// both devices can hold multiple symbols.
		// okay lets see if there is a collision or not.
		Map<String, IAbstractPin> targetPins = new TreeMap<String, IAbstractPin>();
		for (IAbstractPin tpin : target.getPins()) {
			// we are interested only in symboled pins.
			if (tpin.getReference() != null) {
				targetPins.put(tpin.getName(), tpin);
			}
		}

		// now for each pin in the source, if the tree contains the pin name. this means a collision. which is not acceptable
		// note that the tree set uses the equals method to compare (it is valid to use the tree since Strings are Comparable objects)

		for (IAbstractPin spin : source.getPins()) {
			if (spin.getReference() != null && targetPins.containsKey(spin.getName()) &&
					spin.getReference() != targetPins.get(spin.getName()).getReference()) {
				// two symboled pins, and they don't reference the same symbol pin UID.
				return Mergeable.SymbolCollision;
			}
		}

		return Mergeable.Possible;
	}

	@Override protected void mergePin(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent,
			IAbstractPin sourcePin, IAbstractPin targetPin)
	{
		super.mergePin(sourceParent, targetParent, sourcePin, targetPin);
		// if there is no symbol reference on the target pin, and the source pin has a one, then move the symbol reference
		// to the target
		if (targetPin != null && targetPin.getReference() == null && sourcePin.getReference() != null) {
			((IDevicePin) targetPin).setReference(sourcePin.getReference());
		}
	}

	@NotNull @Override protected List<IAbstractPin> mergeMatchingPins(chs.cof.logical.cable.IPinList sourceParent,
			chs.cof.logical.cable.IPinList targetParent)
	{
		List<IAbstractPin> pinsToBeMoved = new LinkedList<IAbstractPin>();
		List<IAbstractPin> unMergedPins = super.mergeMatchingPins(sourceParent, targetParent);
		for (IAbstractPin sourcePin : unMergedPins) {
			IInternalPin targetInternalPin =
					chs.cof.logical.cable.IPinList.Statics.findInternalPinByName(targetParent, sourcePin.getName());
			if (targetInternalPin != null && !targetInternalPin.isFromSymbol() &&
					sourcePin.getReference() == targetInternalPin.getReference()) {
				deferredDeleteInternalConnectivityObject.add(targetInternalPin);
				sourceParent.removePin(sourcePin);
				addMapping(sourcePin, sourcePin);
				targetParent.addPin(sourcePin);
				fixupInternalLinks(targetInternalPin, sourcePin);
			}
			else {
				pinsToBeMoved.add(sourcePin);
			}
		}

		return pinsToBeMoved;
	}

	protected void doPostSchematicMerge(IConnectivityRef schemObject)
	{
		if (schemObject.getConnectivity() instanceof IDevice && deferredDeleteInternalConnectivityObject != null) {
			IPinList pinlist = (IPinList) schemObject;
			for (IInternalSchemPin ispin : pinlist.getInternalPins()) {
				if (deferredDeleteInternalConnectivityObject.contains(ispin.getConnectivity())) {
					ispin.delete();
				}
			}
			for (ISchemInternalLink islnk : pinlist.getInternalLinks()) {
				if (deferredDeleteInternalConnectivityObject.contains(islnk.getConnectivity())) {
					islnk.delete();
				}
			}
		}

		super.doPostSchematicMerge(schemObject);
	}

	@Override protected void postMergingComplete()
	{

		for (ILogicObject internalConnectivityObject : deferredDeleteInternalConnectivityObject) {
			internalConnectivityObject.delete();
		}

		deferredDeleteInternalConnectivityObject.clear();
		deferredDeleteInternalConnectivityObject = null;

		super.postMergingComplete();
	}

	protected void mergInternalLinks(IDevice sourceDevice, IDevice targetDevice)
	{
		for (IInternalLink internalLink : sourceDevice.getInternalLinkCollection()) {
			IInternalLink targetLink = getMatchingTargetLink(targetDevice, internalLink);
			if (targetLink != null) {
				addMapping(internalLink, targetLink);
			}
			else {
				sourceDevice.removeInternalLink(internalLink);
				targetDevice.addInternalLink(internalLink);
				addMapping(internalLink, internalLink);
			}
		}
	}

	private IInternalLink getMatchingTargetLink(IDevice targetDevice, IInternalLink internalLink)
	{
		for (IInternalLink targetInternalLink : targetDevice.getInternalLinkCollection()) {
			if (targetInternalLink.getReference() == internalLink.getReference()) {
				return targetInternalLink;
			}
		}
		return null;
	}

	protected void mergeInternalPins(IDevice sourceDevice, IDevice targetDevice)
	{
		IInternalPinIterator internalpinsIterator = sourceDevice.getInternalPins();
		while (internalpinsIterator.hasNext()) {
			IInternalPin internalPin = internalpinsIterator.getNext();
			//Merge only internal pins which are internal because of unplaced blocks.
			//Merge only when they refer to same symbol pin
			//Dont merge actual internal pins created in CSymbol
			IGenericPin targetPin = getMatchingTargetPin(targetDevice, internalPin);
			if (targetPin != null) {
				//dts0100791334 Bash6:S1 - MergeInto - Duplicate pins are created on device when we merge blocks of composite symbol
				addMapping(internalPin, targetPin);
				fixupInternalLinks(internalPin, targetPin);
			}
			else {
				sourceDevice.removeInternalPin(internalPin);         
				targetDevice.addInternalPin(internalPin);
				addMapping(internalPin, internalPin);
			}
		}
	}

	@Nullable private IGenericPin getMatchingTargetPin(IDevice targetDevice, IInternalPin internalPin)
	{
		IInternalPin targetInternalPin =
				chs.cof.logical.cable.IPinList.Statics.findInternalPinByName(targetDevice, internalPin.getName());
		if (targetInternalPin != null && internalPin.getReference() == targetInternalPin.getReference() &&
				targetInternalPin.isFromSymbol() == internalPin.isFromSymbol()) {
			return targetInternalPin;
		}

		if (!internalPin.isFromSymbol()) {
			IAbstractPin targetPin =
					chs.cof.logical.cable.IPinList.Statics.findPinByName(targetDevice, internalPin.getName());
			if (targetPin != null && internalPin.getReference() == targetPin.getReference()) {
				return targetPin;
			}
		}
		return null;
	}

	private interface BlockRefContainerAdapter
	{

		IUID getBlockRef();

		@Nullable chs.cof.logical.cable.IPinList getOwner();
	}

	private static class PinBlockRefAdapter implements BlockRefContainerAdapter
	{

		private IGenericPin m_pin;

		private PinBlockRefAdapter(IGenericPin pin)
		{
			m_pin = pin;
		}

		@Override public IUID getBlockRef()
		{
			return m_pin.getBlockRef();
		}

		@Override public chs.cof.logical.cable.IPinList getOwner()
		{
			return m_pin.getOwner();
		}
	}

	private static class InternalLinkBlockRefAdapter implements BlockRefContainerAdapter
	{

		private IInternalLink m_link;

		private InternalLinkBlockRefAdapter(IInternalLink link)
		{
			m_link = link;
		}

		@Override public IUID getBlockRef()
		{
			return m_link.getBlockRef();
		}

		@Override public chs.cof.logical.cable.IPinList getOwner()
		{
			return m_link.getOwner();
		}
	}
}
