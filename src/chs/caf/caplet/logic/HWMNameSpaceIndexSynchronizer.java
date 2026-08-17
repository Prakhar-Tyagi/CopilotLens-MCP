package chs.caf.caplet.logic;

import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.capitalmanager.appserver.HighWaterMarkException;
import chs.capitalmanager.appserver.IUserSession;
import chs.capitalmanager.appserver.IUserSessionRemotePackage.WaterMarkInfo;
import chs.cof.logical.ILogicDesign;
import chs.cof.project.IProject;
import chs.cof.project.naming.IHWMNameSpace;
import chs.cof.project.naming.IIndexedNamedObject;
import chs.cof.project.naming.INameSpace;
import chs.common.DesignUtils;
import chs.common.IDesignContainer;
import chs.common.IUID;
import chs.common.UIDUtils;
import chs.system.FactoryMgr;
import chs.utilities.SetMap;
import chs.utility.CachedProjectExportSessionAutoClosable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author chandras on 01-07-2016.
 */
public class HWMNameSpaceIndexSynchronizer implements IModelChangeListener
{

	private IUID m_designId;

	public HWMNameSpaceIndexSynchronizer(@NotNull IDesignContainer designContainer)
	{
		m_designId = designContainer.getUID();
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
		IUserSession userSession = FactoryMgr.getCHSSystem().getUserSession();
		if (userSession == null) {
			return;
		}
		ILogicDesign loadedDesign = DesignUtils.getLoadedDesign(m_designId, ILogicDesign.class);
		if (loadedDesign == null) {
			return;
		}

		if (!loadedDesign.isUnderConcurrentEdit() || CachedProjectExportSessionAutoClosable.isWithinConnectionCaching()) {
			//the synch happens only in concurrent edit mode.
			return;
		}

		IProject project = loadedDesign.getProject();
		if (project == null) {
			return;
		}

		SetMap<IHWMNameSpace, IIndexedNamedObject> nameSpaceCategorized = new SetMap<>();
		for (IIndexedNamedObject indexedNamedObject : UIDUtils
				.convertToUIDObjectCollection(e.getNewObjectsUIDs(), IIndexedNamedObject.class)) {
			IIndexedNamedObject namedObjectHolder = indexedNamedObject.getNamedObjectHolder();
			INameSpace nameSpace = namedObjectHolder.getNameMgr().getNameSpace(namedObjectHolder);
			if (nameSpace instanceof IHWMNameSpace) {
				nameSpaceCategorized.add((IHWMNameSpace) nameSpace, namedObjectHolder);
			}
		}

		Map<String, IHWMNameSpace> nameSpacesToProcess = new HashMap<String, IHWMNameSpace>();
		Map<String, Integer> nameSpaceAllocation = new HashMap<String, Integer>();
		for (Map.Entry<IHWMNameSpace, Set<IIndexedNamedObject>> entry : nameSpaceCategorized.entrySet()) {
			int indicesToSync = 0;
			for (IIndexedNamedObject namedObject : entry.getValue()) {
				if (namedObject.getIndex() > 0) {
					++indicesToSync;
				}
			}
			if (indicesToSync > 0) {
				String nameSpaceString = entry.getKey().getNameSpaceString();
				nameSpaceAllocation.put(nameSpaceString, indicesToSync);
				nameSpacesToProcess.put(nameSpaceString, entry.getKey());
			}
		}

		if (nameSpaceAllocation.isEmpty()) {
			//don't go to DB with empty data. manager might crash.
			return;
		}

		Set<Map.Entry<String, Integer>> entries = nameSpaceAllocation.entrySet();
		int nameSpaceCount = entries.size();
		String[] componentTypes = new String[nameSpaceCount];
		int[] wmCounts = new int[nameSpaceCount];
		int idx = 0;
		for (Map.Entry<String, Integer> entry : entries) {
			componentTypes[idx] = entry.getKey();
			wmCounts[idx] = entry.getValue();
			++idx;
		}

		try {
			int allocationBlockSize = IHWMNameSpace.RANGECOUNT;
			WaterMarkInfo[] waterMarkInfos = userSession.allocateDesignLevelWaterMarks
					(project.getUID().getString(), m_designId.getString(), componentTypes, wmCounts,
							allocationBlockSize);
			assert waterMarkInfos.length == componentTypes.length;
			int loopCount = Math.min(waterMarkInfos.length, componentTypes.length);
			for (int id = 0; id < loopCount; ++id) {
				IHWMNameSpace ihwmNameSpace = nameSpacesToProcess.get(componentTypes[id]);
				if (ihwmNameSpace != null) {
					doSynchronizeIndex(ihwmNameSpace, nameSpaceCategorized.pullReadOnlySafeSet(ihwmNameSpace),
							waterMarkInfos[id], wmCounts[id]);
				}
			}
		}
		catch (HighWaterMarkException e1) {
			e1.printStackTrace();
		}
	}

	private void doSynchronizeIndex(IHWMNameSpace ihwmNameSpace, Set<IIndexedNamedObject> indexedNamedObjects,
			WaterMarkInfo waterMarkInfo, int wmCount)
	{
		int lastSynchronizedCurrentIndex = ihwmNameSpace.getLastSynchronizedCurrentIndex();
		int currentIndex = ihwmNameSpace.getCurrentIndex();
		Map<Integer, IIndexedNamedObject> orderedObjSet = new TreeMap<>();
		for (IIndexedNamedObject indexedNamedObject : indexedNamedObjects) {
			int index = indexedNamedObject.getIndex();
			if (index > 0 && index >= lastSynchronizedCurrentIndex && index < currentIndex) {
				orderedObjSet.put(index, indexedNamedObject);
			}
		}
		int iteration = 1;
		for (Map.Entry<Integer, IIndexedNamedObject> entry : orderedObjSet.entrySet()) {
			int index = entry.getKey();
			int newIndex = computeNextAllocatedIndex(iteration, waterMarkInfo, wmCount);
			if (index != newIndex) {
				resetToNewIndex(entry.getValue(), newIndex);
			}
			++iteration;
		}
		ihwmNameSpace
				.indicesSynchronized(waterMarkInfo.postAllocWMCurrIndex + 1, waterMarkInfo.postAllocWMHighIndex - 1);
	}

	private void resetToNewIndex(IIndexedNamedObject indexedNamedObject, int newIndex)
	{
		indexedNamedObject.resetName();
		indexedNamedObject.setIndex(newIndex);
		indexedNamedObject.getNameMgr().addObject(indexedNamedObject);
	}

	private int computeNextAllocatedIndex(int iteration, WaterMarkInfo waterMarkInfo, int wmCount)
	{
		//the allocation excludes preAllocWMCurrIndex but includes postAllocWMCurrIndex
		//similarly excludes both preAllocWMHighIndex but includes postAllocWMHighIndex
		//and also the iteration of allocation starts with 1. the useful indices are always > 0.
		assert iteration > 0 && iteration <= wmCount;
		int result = waterMarkInfo.preAllocWMCurrIndex + iteration;
		if (waterMarkInfo.preAllocWMCurrIndex <= 0 || result >= waterMarkInfo.preAllocWMHighIndex) {
			result = waterMarkInfo.postAllocWMCurrIndex + iteration - wmCount;
		}
		return result;
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{

	}
}
