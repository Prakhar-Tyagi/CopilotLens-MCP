package chs.caplets.logic.commands;

import chs.caplets.logic.DeleteHelper;
import chs.caplets.logic.Model;
import chs.cof.logical.schem.IConductor;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.schem.IShieldBody;
import chs.cof.logical.schem.IShieldBodyHookup;
import chs.common.ILocation;
import chs.common.IUIDObject;
import chs.utilities.CollectionUtils;
import chs.utilities.SetMap;
import chs.utility.GfxUtils;
import chs.utility.logic.DaisyChainCreationHelper;
import chs.utility.logic.ShieldBodyInfo;
import chs.utility.logic.ShieldBodyLocationComparator;
import chs.utility.logic.ShieldHookupInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DaisyChainShieldDeletionCmd
{

	private Model mCapletModel;
	private int mShieldOrientation;
	// Set for storing the shield body in sorted order
	private Set<ShieldBodyInfo> mShieldBodySet;

	// List for storing the shield body selected by the user
	private List<IShieldBody> mShieldBodyList;

	// Map containg Pin as the key and the Shield terminations from that pin as values.
	private SetMap<IPin, ShieldHookupInfo> mPinShieldMap;



	public DaisyChainShieldDeletionCmd(List<IShieldBody> shieldBodyList, Model capletModel, int shieldOrientation)
	{
		mShieldBodyList = shieldBodyList;
		mPinShieldMap = new SetMap<IPin, ShieldHookupInfo>();
		mCapletModel = capletModel;
		mShieldOrientation = shieldOrientation;
		mShieldBodySet = new TreeSet<ShieldBodyInfo>(new ShieldBodyLocationComparator<ShieldBodyInfo>(mShieldOrientation));
	}

	public void connectIndicators()
	{
		sortHookups();

		if (mShieldBodySet.size() > 1) {
			ISchemDiagram currentDiagram = mCapletModel.getDiagram();
			Iterator<ShieldBodyInfo> iter = mShieldBodySet.iterator();
			Collection<IUIDObject> objectsToBeDeleted = new ArrayList<IUIDObject>();
			ShieldBodyInfo source = iter.next();
			populateShieldTermination(source);
			while (iter.hasNext()) {
				ShieldBodyInfo target = iter.next();
				if(DaisyChainCreationHelper.createDaisyChainBetweenIndicators(source.getEndHookup(),
						target.getStartHookup(), currentDiagram)){
					populateShieldTermination(target);
				}
				else {
					populateObjectsToBeDeleted(objectsToBeDeleted);
					mPinShieldMap.clear();
					populateShieldTermination(target);
				}
				source = target;
			}
			populateObjectsToBeDeleted(objectsToBeDeleted);
			DeleteHelper.getInstance().delete(currentDiagram, objectsToBeDeleted, true);
		}
	}

	private void sortHookups()
	{
		Iterator<IShieldBody> iter = mShieldBodyList.iterator();
		mShieldBodySet.clear();

		while (iter.hasNext()) {
			IShieldBody shieldBody = iter.next();
			if (shieldBody.getShieldBodyHookups().size() == 2) {
				mShieldBodySet.add(new ShieldBodyInfo(shieldBody));
			}
		}
	}

	private void populateObjectsToBeDeleted(Collection<IUIDObject> objectsToBeDeleted)
	{
		for (IPin pin : mPinShieldMap.keySet()) {
			Set<ShieldHookupInfo> shieldHookupList = mPinShieldMap.get(pin);
			// There are multi-term shield segments
			if (shieldHookupList.size() > 1) {
				IConductor shieldToBeRetained = findShortestShield(pin, shieldHookupList);
				for (ShieldHookupInfo shieldHookupInfo : shieldHookupList) {
					IConductor shieldConductor = shieldHookupInfo.getShieldConductor();
					if (!shieldConductor.equals(shieldToBeRetained)) {
						objectsToBeDeleted.add(shieldConductor);
						//need to add the segments in the deletion set otherwise delete helper doesn't delete the conductor.
						objectsToBeDeleted
								.addAll(CollectionUtils.getObjectList(shieldConductor.getObjects(), ISegment.class));
					}
				}
			}
		}
	}

	@Nullable private IConductor findShortestShield(IPin pPin, Set<ShieldHookupInfo> shieldList)
	{
		double smallestDistance = Double.MAX_VALUE;
		IConductor shortestShield = null;
		for (ShieldHookupInfo shieldHookupInfo : shieldList) {
			double distance = calculateDistance(pPin, shieldHookupInfo.getShieldBodyHookup());
			if (distance < smallestDistance) {
				smallestDistance = distance;
				shortestShield = shieldHookupInfo.getShieldConductor();
			}
		}
		return shortestShield;
	}

	private double calculateDistance(IPin pPin, IShieldBodyHookup shielBodyHookup)
	{
		ILocation pinLocation = pPin.getAbsLocation();
		ILocation hookupLocation = shielBodyHookup.getAbsLocation();
		return GfxUtils.getDesitanceBetweenPoints(pinLocation, hookupLocation);
	}

	private void populateShieldTermination(ShieldBodyInfo shieldBodyInfo)
	{
		populateShieldTermination(shieldBodyInfo.getStartHookup());
		populateShieldTermination(shieldBodyInfo.getEndHookup());
	}

	private void populateShieldTermination(IShieldBodyHookup shieldBodyHookup)
	{
		for (IConductor shieldConductor : shieldBodyHookup.getShieldConductors()) {
			for (IPin pin : shieldConductor.getPins()) {
				Set<ShieldHookupInfo> shieldHookupList = mPinShieldMap.get(pin);
				ShieldHookupInfo shieldHookupInfo = new ShieldHookupInfo(shieldConductor, shieldBodyHookup);
				shieldHookupList.add(shieldHookupInfo);
			}
		}
	}

}
