package chs.caplets.logic.commands;

import chs.cof.logical.ICopyableAttributes;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.cable.IPhysicalConductor;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.wdg.IGeneratedConductor;
import chs.cof.logical.shared.ISharedConductor;
import chs.cof.logical.shared.ISharedConductorIterator;
import chs.cof.logical.shared.ISharedMulticore;
import chs.cof.logical.shared.ISharedMulticoreIterator;
import chs.common.attr.custom.ICustomAttributesProvider;
import chs.system.FactoryMgr;
import chs.utilities.StringUtils;
import chs.utility.ICDUtils;
import chs.utility.Replicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: nagamani Date: 14 Sep, 201
 * <p>
 * ConvertNetsToWires related replicator functions are placed here
 */
public class ConvertNetsToWiresCmdReplicator extends Replicator
{

	public ConvertNetsToWiresCmdReplicator(Mode copy, boolean b)
	{
		super(copy, b);
	}

	public static void setupTopoSignal(@NotNull IConductor oldConductor, @NotNull IGeneratedConductor newConductor)
	{
		//setting the topo signal attribute.
		String topoSignalNameTemp = null;
		if ((oldConductor instanceof IPhysicalConductor) && (oldConductor instanceof IGeneratedConductor)) {
			topoSignalNameTemp = ((IGeneratedConductor) oldConductor).getTopoSignalName();
		}
		setupTopoSignal(oldConductor.getName(), topoSignalNameTemp, oldConductor, newConductor);
	}

	public static void setupTopoSignal(String name, @Nullable String topoSignalNameTemp,
			@NotNull ICustomAttributesProvider oldConductor, @NotNull IGeneratedConductor newConductor)
	{
		String topoSignalName = name;
		if (topoSignalNameTemp != null && !StringUtils.isBlank(topoSignalNameTemp)) {
			topoSignalName = topoSignalNameTemp;
		}
		newConductor.setTopoSignalName(topoSignalName);
		ICDUtils.setSourceICDSignal(newConductor, ICDUtils.getSourceICDSignal(oldConductor));
	}

	public static void setupTopoSignal(@NotNull ISharedConductor oldSharedShield,
			@NotNull IGeneratedConductor newShield)
	{
		//setting the topo signal attribute.
		setupTopoSignal(oldSharedShield.getName(), oldSharedShield.getTopoSignalName(), oldSharedShield, newShield);
	}

	@Override public void replicateCopyableObject(ICopyableAttributes src, ICopyableAttributes dest)
	{
		super.replicateCopyableObject(src, dest);
		if (dest instanceof IGeneratedConductor) {
			if (src instanceof IConductor) {
				setupTopoSignal((IConductor) src, (IGeneratedConductor) dest);
			}
			else if (src instanceof ISharedConductor) {
				setupTopoSignal((ISharedConductor) src, (IGeneratedConductor) dest);
			}
		}
	}

	@Nullable public IMulticore replicateShardMulticoredAsNonShared(@NotNull IMulticore orig, @NotNull IDesign design)
	{
		ISharedMulticore sharedMC = orig.getSharedMulticore();
		if (sharedMC == null) {
			return null;
		}

		//create a new multicore
		IMulticore newMulticore = replicateMulticoreOrOverbraid(orig, false, false, design);

		//replicate shield
		replicateShieldForMulticore(orig, newMulticore, sharedMC, design);

		//replicate placed & unplaced conductors
		replicatePlacedAndUnPlacedlCondsOfSharedMC(orig, newMulticore, sharedMC, design);

		//replicate placed & unplaced child multicores
		replicatePlacedAndUnPlacedChildMulticoresOfSharedMC(orig, newMulticore, sharedMC, design);
		return newMulticore;
	}

	private void replicateShieldForMulticore(IMulticore orig, IMulticore newMulticore, ISharedMulticore sharedMC,
			IDesign design)
	{
		if (orig != null && orig.getShield() != null) {
			replicateShield(orig.getShield(), newMulticore, design);
		}
		else if (sharedMC != null && sharedMC.getShield() != null) {
			ISharedConductor sharedShieldCond = sharedMC.getShield();
			IShieldConductor newShield =
					FactoryMgr.getCableFactory().createShieldConductor(FactoryMgr.getCommonFactory().createUID());
			newMulticore.setShield(newShield);
			replicateCopyableObject(sharedShieldCond, newShield);
			if (design != null) {
				design.getConnectivity().addShieldConductor(newShield);
			}
		}
	}

	private void replicatePlacedAndUnPlacedChildMulticoresOfSharedMC(@Nullable IMulticore oldMulticore,
			@NotNull IMulticore newMulticore, @NotNull ISharedMulticore sharedMC, @NotNull IDesign design)
	{
		//1. Get all shared child multicores
		List<ISharedMulticore> sharedMCs = new ArrayList<ISharedMulticore>();
		for (ISharedMulticoreIterator shdMultIt = sharedMC.getMulticores(); shdMultIt.hasNext(); ) {
			sharedMCs.add(shdMultIt.getNext());
		}
		//2.Replicate all cable child multicores
		if (oldMulticore != null) {
			for (IMulticore oldChildMC : oldMulticore.getMulticores()) {
				IMulticore newChildMC = replicateMulticoreOrOverbraid(oldChildMC, false, false, design);
				newMulticore.addMulticore(newChildMC);
				ISharedMulticore sharedChildMC = oldChildMC.getSharedMulticore();
				sharedMCs.remove(sharedChildMC);
				replicateShieldForMulticore(oldChildMC, newChildMC, sharedChildMC, design);
				replicatePlacedAndUnPlacedlCondsOfSharedMC(oldChildMC, newChildMC, sharedChildMC, design);
				replicatePlacedAndUnPlacedChildMulticoresOfSharedMC(oldChildMC, newChildMC, sharedChildMC, design);
			}
		}
		//3. Replicate the remaining unplaced child multicores
		for (ISharedMulticore unplacedSharedMC : sharedMCs) {
			IMulticore newChildMC;
			newChildMC = getEmptyMulticore(design, oldMulticore);
			design.getConnectivity().addMulticore(newChildMC);
			newMulticore.addMulticore(newChildMC);
			//When we create a shield body for the empty multicore created above that new multicore does not have
			//connectivity at that moment. As a result, we do not set connectivity on the shield body when we assign
			//the shield body to the new multicore.
			//We must set the connectivity explictly to avoid possible data corruption.
			newChildMC.setShieldBodyConnectivity();
			replicateCopyableObject(unplacedSharedMC, newChildMC);
			replicateShieldForMulticore(null, newChildMC, unplacedSharedMC, design);
			replicatePlacedAndUnPlacedlCondsOfSharedMC(null, newChildMC, unplacedSharedMC, design);
			replicatePlacedAndUnPlacedChildMulticoresOfSharedMC(null, newChildMC, unplacedSharedMC, design);
		}
	}

	private IMulticore getEmptyMulticore(IDesign design, IMulticore oldMulticore)
	{
		IMulticore newChildMC;
		if (oldMulticore instanceof IOverbraid) {
			newChildMC =
					FactoryMgr.getCableFactory().createOverbraid(FactoryMgr.getCommonFactory().createUID());
		}
		else {
			newChildMC =
					FactoryMgr.getCableFactory().createMulticore(FactoryMgr.getCommonFactory().createUID());
		}
		Replicator.ensureShieldBodyOnLogicMulticore(design, newChildMC);
		return newChildMC;
	}

	private void replicatePlacedAndUnPlacedlCondsOfSharedMC(@Nullable IMulticore oldMulticore,
			@NotNull IMulticore newMulticore, @NotNull ISharedMulticore sharedMC, @NotNull IDesign design)
	{
		//1. Get all shared conductors
		List<ISharedConductor> sharedConds = new ArrayList<ISharedConductor>();
		for (ISharedConductorIterator shdCondIt = sharedMC.getConductors(); shdCondIt.hasNext(); ) {
			sharedConds.add(shdCondIt.getNext());
		}
		//2.Replicate all cable conductors
		if (oldMulticore != null) {
			for (IConductor cond : oldMulticore.getConductors()) {
				IConductor newCond = replicateConductor(cond);
				newMulticore.addConductor(newCond);
				ISharedConductor sharedCond = cond.getSharedConductor();
				sharedConds.remove(sharedCond);
				replicateCopyableObject(sharedCond, newCond);
				design.getConnectivity().addConductor(newCond);
			}
		}
		//3. Replicate the remaining unplaced shared conductors
		for (ISharedConductor unplacedSharedCond : sharedConds) {
			IConductor newLogicCond =
					FactoryMgr.getCableFactory().createWireConductor(FactoryMgr.getCommonFactory().createUID());
			replicateCopyableObject(unplacedSharedCond, newLogicCond);
			newMulticore.addConductor(newLogicCond);
			design.getConnectivity().addConductor(newLogicCond);
		}
	}
}
