/*
 * Copyright 2006-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.icd;

import chs.api.IXObjectNames;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.IModelChangeListener;
import chs.caf.caplet.ModelChangeEvent;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.cof.icd.IActiveBuildlistChangeListner;
import chs.cof.icd.IApplicableICDChangeListner;
import chs.cof.icd.ICDApplicability;
import chs.cof.icd.IDeviceICD;
import chs.cof.icd.IICD;
import chs.cof.icd.IICDAssociatedSignal;
import chs.cof.icd.IICDBackshellTermination;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.shared.ISharedFullyLoadedPinListMgr;
import chs.cof.logical.shared.PinListTypeEnum;
import chs.cof.parts.partselector.IICDSelection;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.project.IProject;
import chs.cof.project.buildlist.IBuildList;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.attr.IAttributeTypes;
import chs.common.reporting.query.ITagBasedAttributeValuesQueryHandler;
import chs.common.reporting.query.ReporterQueryHandlerFactory;
import chs.utilities.CollectionUtils;
import chs.utilities.CommonUtils;
import chs.utilities.KeySeparatedStringBuilder;
import chs.utilities.ResourceMgr;
import chs.utilities.SetMap;
import chs.utilities.SortedList;
import chs.utilities.StringUtils;
import chs.utility.DesignInfo;
import chs.utility.ICDApplicabilityChecker;
import chs.utility.ICDSignalDetailsFinder;
import chs.utility.ICDUtils;
import chs.utility.helpers.UIDComparator;
import chs.utility.ui.CHSSwingUtils;
import chs.utility.ui.IconUtils;
import chs.utility.ui.UIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ICDBrowserClient extends BrowserClientHelper
		implements IModelChangeListener, IActiveBuildlistChangeListner,
		IApplicableICDChangeListner
{

	private ILogicDesign m_design;
	protected Collection<String> m_placedInOtherDesignsDeviceNames = new ArrayList<String>();
	protected Set<String> m_sharedDeviceNames = new HashSet<>();
	protected Set<IUID> m_connectedICDs = new TreeSet<>();

	private BrowserFolder m_placedFolder;
	private BrowserFolder m_unplacedFolder;
	private BrowserFolder m_unallocatedFolder;
	private BrowserFolder m_placedInBuildlistFolder;

	protected List<IUID> m_unallocatedICDs = new ArrayList<IUID>();
	protected List<IUID> m_placedICDs = new ArrayList<IUID>();
	protected List<IUID> m_unplacedICDs = new ArrayList<IUID>();
	protected List<IUID> m_placedInBuildlistICDs = new ArrayList<IUID>();
	protected HashSet<IUID> m_variantICDs = new HashSet<>();
	@Nullable private Set<String> m_allSharedObjectNames;

	private ICDBrowserPanel m_browserPanel;

	public static final String REVISION_TIP = ResourceMgr
			.getString(ICDBrowserClient.class, "ICDBrowserClient.tip.Revisions.text");
	public static final String EFFECTIVITY_TIP = ResourceMgr
			.getString(ICDBrowserClient.class, "ICDBrowserClient.tip.Effectivity.text");
	public static final String NAME = ResourceMgr
			.getString(ICDBrowserClient.class, "ICDBrowserClient.Type");
	public static final String VARIANT = ResourceMgr
			.getString(ICDBrowserClient.class, "ICDBrowserClient.Variant");

	private static final String PLACED_TEXT =
			ResourceMgr.getString(ICDBrowserClient.class, "ICDBrowserClient.Placed.text");
	private static final String UNPLACED_TEXT =
			ResourceMgr.getString(ICDBrowserClient.class, "ICDBrowserClient.UnPlaced.text");
	private static final String UNALLOCATED_TEXT =
			ResourceMgr.getString(ICDBrowserClient.class, "ICDBrowserClient.Unallocated.text");
	private static final String PLACEDINBUILDLIST_TEXT =
			ResourceMgr.getString(ICDBrowserClient.class, "ICDBrowserClient.PlacedInBuildlist.text");

	protected ICDTreeComparator m_comp = null;

	public static final String HTML_HEADER = "<html><body>";
	public static final String HTML_FOOTER = "</body></html>";

	public ICDBrowserClient(@NotNull ILogicDesign design, ICapletController controller, ICDBrowserPanel browser)
	{
		super(controller);
		m_browserPanel = browser;
		m_design = design;
		m_comp = new ICDTreeComparator();
		setRootObject(m_design);
		addFolders();
		adjustPlacedInBuildListMap();
		registerListners();
	}

	private void registerListners()
	{
		m_design.getDesignICDContainer().addActiveBuildlistChangeListner(this);
		m_design.getDesignICDContainer().addApplicableICDChangeListner(this);
	}

	@Override public String getToolTipText(IUID uid, IUID parentUID)
	{
		IUIDObject uidObj = getObject(uid);
		StringBuilder toolTipContent = new StringBuilder();
		if (uidObj instanceof IICD) {
			IICD iicd = (IICD) uidObj;
			IDeviceICD deviceICD = m_design.getDesignICDContainer().constructDeviceICD(iicd);
			if (deviceICD.hasVariants()) {
				Set<String> fullNameStrs = new TreeSet<>();
				for (IICD icd : ICDUtils.getAllICDs(deviceICD)) {
					StringBuilder builder = new StringBuilder();
					builder.append(icd.getName());
					String revision = icd.getRevision();
					if (!StringUtils.isBlank(revision)) {
						builder.append("<B>:</B>").append(revision);
					}
					String effectivityValue = icd.getEffectivityValue();
					if (!StringUtils.isBlank(effectivityValue)) {
						builder.append("<B>:(</B>").append(effectivityValue).append("<B>)</B>");
					}
					fullNameStrs.add(builder.toString());
				}

				KeySeparatedStringBuilder keySepBuilder = new KeySeparatedStringBuilder("<B>,</B> ");
				fullNameStrs.forEach((s) -> keySepBuilder.append(s));

				toolTipContent.append("<B>").append(VARIANT).append(":</B> ").append(keySepBuilder.toString());
			}
			else {
				String revision = iicd.getRevision();
				toolTipContent.append("<B>").append(NAME).append(":</B> ").append(iicd.getName());

				if (!StringUtils.isBlank(revision)) {
					toolTipContent.append("<BR>").append("<B>").append(REVISION_TIP).append(":</B> ").append(revision);
				}
				String effectivityValue = iicd.getEffectivityValue();
				if (!StringUtils.isBlank(effectivityValue)) {
					toolTipContent.append("<BR>").append("<B>").append(EFFECTIVITY_TIP).append(":</B> ")
							.append(effectivityValue);
				}
			}
			toolTipContent.append(StringUtils.HTML_SPACE).append(StringUtils.HTML_SPACE);
		}

		String toolTip = toolTipContent.toString();
		if (!StringUtils.isBlank(toolTip)) {
			return HTML_HEADER + toolTip + HTML_FOOTER;
		}
		else {
			return null;
		}
	}

	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{
		Icon icon = determineICDIcon(uid);
		if (icon != null && m_variantICDs.contains(uid)) {
			icon = IconUtils.decorateVariantItem(icon);
		}
		return icon;
	}

	@Nullable protected final Icon determineICDIcon(IUID uid)
	{
		String role = getRoleForICD(uid);
		boolean shared = false;
		if (role != null) {
			if (m_sharedDeviceNames.contains(role)) {
				shared = true;
			}
		}
		boolean connected = false;
		if (m_connectedICDs.contains(uid)) {
			connected = true;
		}
		if (shared && connected) {
			return IconUtils.getSharedConnectedICDIcon();
		}
		if (shared) {
			return IconUtils.getIconForSharedPinList(PinListTypeEnum.TypeDevice, IconUtils.ACTIVE);
		}
		if (connected) {
			return IconUtils.getNonSharedConnectedICDIcon();
		}
		return super.getIcon(uid);
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		if (m_placedICDs.isEmpty() && m_unplacedICDs.isEmpty() && m_unallocatedICDs.isEmpty() &&
				m_placedInBuildlistICDs.isEmpty()) {
			createChildFoldersAndDistributeICDs();
		}

		IUIDObject nodeObj = getObject(uid);
		if (nodeObj instanceof BrowserFolder) {
			if (uid == super.getRoot()) {
				return true;
			}
			if (nodeObj == m_placedFolder) {
				return !m_placedICDs.isEmpty();
			}
			if (nodeObj == m_unplacedFolder) {
				return !m_unplacedICDs.isEmpty();
			}
			if (nodeObj == m_unallocatedFolder) {
				return !m_unallocatedICDs.isEmpty();
			}
			if (nodeObj == m_placedInBuildlistFolder) {
				return !m_placedInBuildlistICDs.isEmpty();
			}
		}
		return false;
	}

	@Override public List<IUID> getChildren(IUID uid)
	{
		if (uid == super.getRoot()) {
			List<IUID> topLevelFolders = new ArrayList<IUID>();
			topLevelFolders.add(m_unplacedFolder.getUID());
			topLevelFolders.add(m_placedFolder.getUID());
			if (m_placedInOtherDesignsDeviceNames.isEmpty()) {
				topLevelFolders.remove(m_placedInBuildlistFolder);
			}
			else {
				topLevelFolders.add(m_placedInBuildlistFolder.getUID());
			}
			topLevelFolders.add(m_unallocatedFolder.getUID());
			return topLevelFolders;
		}
		if (m_placedICDs.isEmpty() && m_unplacedICDs.isEmpty() && m_unallocatedICDs.isEmpty() &&
				m_placedInBuildlistICDs.isEmpty()) {
			createChildFoldersAndDistributeICDs();
		}
		IUIDObject obj = getObject(uid);
		if (obj instanceof BrowserFolder) {
			if (obj == m_placedFolder) {
				return m_placedICDs;
			}
			if (obj == m_unplacedFolder) {
				return m_unplacedICDs;
			}
			if (obj == m_unallocatedFolder) {
				return m_unallocatedICDs;
			}
			if (obj == m_placedInBuildlistFolder) {
				return m_placedInBuildlistICDs;
			}
		}
		return Collections.emptyList();
	}

	public void addFolders()
	{
		m_placedFolder = createFolder(PLACED_TEXT);
		m_placedInBuildlistFolder = createFolder(PLACEDINBUILDLIST_TEXT);
		m_unplacedFolder = createFolder(UNPLACED_TEXT);
		m_unallocatedFolder = createFolder(UNALLOCATED_TEXT);
	}

	protected void createChildFoldersAndDistributeICDs()
	{

		clearCaches();

		IConnectivity connectivity = m_design.getConnectivity();
		IProject project = m_design.getProject();

		List<String> placedDevices = new SortedList<String>();
		if (connectivity != null) {
			for (IDevice device : connectivity.getAllDevices()) {
				placedDevices.add(ICDUtils.toICDComparableKey(ICDUtils.getICDMatchName(device)));
				if (device.getSharedObject() != null) {
					m_sharedDeviceNames.add(device.getName());
				}
			}
		}

		Set<IDeviceICD> allAvailableICDs = m_design.getDesignICDContainer().getApplicableICDsWithDesignAssociation();
		determineVariantICDs(allAvailableICDs);
		Collection<IDeviceICD> allICDs = new HashSet<>();
		for (IDeviceICD icd : allAvailableICDs) {
			if (StringUtils.isBlank(icd.getRole())) {
				continue;
			}
			allICDs.add(icd);
		}

		IBuildList bl = null;
		if (project != null) {
			bl = project.getBuildListMgr().getActiveBuildList();
		}
		ICDApplicabilityChecker icdApplicabilityChecker =
				new ICDApplicabilityChecker(allICDs, Collections.singleton(new DesignInfo(m_design, bl)));

		bucketICDs(icdApplicabilityChecker, ICDApplicability.ALLOCATED, placedDevices, m_placedICDs,
				m_placedInBuildlistICDs, m_unplacedICDs);

		bucketICDs(icdApplicabilityChecker, ICDApplicability.UNALLOCATED, placedDevices, m_placedICDs,
				m_placedInBuildlistICDs, m_unallocatedICDs);

		Set<String> sigSet = new TreeSet<>();
		Map<IUID, Set<String>> placedSigMap = new HashMap<>();
		updatePlacedSignals(m_placedICDs, sigSet, placedSigMap);
		updateConnectedICDs(CollectionUtils.coalesce(m_placedInBuildlistICDs, m_unplacedICDs, m_unallocatedICDs),
				sigSet);
		updateConnectedICDsForPlaced(m_placedICDs, sigSet, placedSigMap);
		if (project != null) {
			updateSharedDeviceNames(project, allICDs);
		}

		Collections.sort(m_placedICDs, m_comp);
		Collections.sort(m_placedInBuildlistICDs, m_comp);
		Collections.sort(m_unplacedICDs, m_comp);
		Collections.sort(m_unallocatedICDs, m_comp);
	}

	private void updateSharedDeviceNames(@NotNull IProject project, @NotNull Collection<IDeviceICD> allICDs)
	{
		if (allICDs.isEmpty()) {
			return;
		}
		Set<String> sharedDeviceNames = getAllSharedObjectNames(project);

		Set<IDeviceICD> unplacedSharedICDs = allICDs.stream()
				.filter(icd -> !m_placedICDs.contains(icd.getICD().getUID()))
				.filter(icd -> !ICDUtils.isICDApplicableForJustOneDesign(icd))
				.filter(icd -> sharedDeviceNames.contains(icd.getRole()))
				.collect(Collectors.toSet());

		for (IDeviceICD icd : unplacedSharedICDs) {
			m_sharedDeviceNames.add(icd.getRole());
		}
	}

	@NotNull private Set<String> getAllSharedObjectNames(@NotNull IProject project)
	{
		if (m_allSharedObjectNames == null) {
			m_allSharedObjectNames = ((ISharedFullyLoadedPinListMgr)project.getSharedPinListMgr()).getSharedPinLists(PinListTypeEnum.TypeDevice)
					.stream().map(shDevice -> shDevice.getName())
					.collect(Collectors.toSet());
		}
		return m_allSharedObjectNames;
	}

	@Override public void startCreation()
	{
		m_allSharedObjectNames = null;
	}

	@Override public void endCreation()
	{
		m_allSharedObjectNames = null;
		super.endCreation();
	}

	private void determineVariantICDs(Set<IDeviceICD> variantICDs)
	{
		m_variantICDs.clear();
		for (IDeviceICD entry : variantICDs) {
			if (entry.hasVariants()) {
				for (IICD variant : ICDUtils.getAllICDs(entry)) {
					m_variantICDs.add(variant.getUID());
				}
			}
		}
	}

	private void bucketICDs(
			ICDApplicabilityChecker icdApplicabilityChecker,
			ICDApplicability applicabilityFilter,
			List<String> placedDevices,
			List<IUID> placedICDs, List<IUID> placedInBuildlistICDs, List<IUID> unplacedICDs)
	{
		for (IDeviceICD deviceICD : icdApplicabilityChecker.getApplicableICDs(m_design, applicabilityFilter)) {
			IICD icd = deviceICD.getICD();
			if (placedDevices.contains(ICDUtils.toICDComparableKey(icd.getRole()))) {
				placedICDs.add(icd.getUID());
			}
			else {
				if (m_placedInOtherDesignsDeviceNames.contains(ICDUtils.toICDComparableKey(icd.getRole()))) {
					placedInBuildlistICDs.add(icd.getUID());
				}
				else {
					unplacedICDs.add(icd.getUID());
				}
			}
		}
	}

	private void updateConnectedICDsForPlaced(List<IUID> placedICDs, Set<String> sigSet,
			Map<IUID, Set<String>> placedSigMap)
	{
		for (IUID uid : placedICDs) {
			IUIDObject obj = getObject(uid);
			if (obj instanceof IICD) {
				IICD iicd = (IICD) obj;
				Set<String> dup = ICDUtils.getSignalNames(iicd);
				dup.removeAll(placedSigMap.get(uid));
				if (!Collections.disjoint(dup, sigSet)) {
					m_connectedICDs.add(uid);
				}
			}
		}
	}

	private void updatePlacedSignals(List<IUID> placedICDs, Set<String> sigSet, Map<IUID, Set<String>> placedSigMap)
	{
		for (IUID uid : placedICDs) {
			IUIDObject obj = getObject(uid);
			if (obj instanceof IICD) {
				IICD iicd = (IICD) obj;
				IDeviceICD deviceICD = m_design.getDesignICDContainer().constructDeviceICD(iicd);
				IPinList device = ICDUtils.getMatchingDevice(deviceICD, m_design);
				if (device != null) {
					Set<String> sigs = new TreeSet<String>();
					for (IAbstractPin pin : device.getPins()) {
						sigs.addAll(ICDUtils.getSignalNames(iicd, pin.getName()));
					}
					collectPlacedBackshellSignals(deviceICD, device, sigs);
					sigSet.addAll(sigs);
					placedSigMap.put(uid, sigs);
				}
			}
		}
	}

	private void collectPlacedBackshellSignals(IDeviceICD icd, IPinList pinlist, Set<String> placedSignals)
	{
		IDevice device = CommonUtils.cast(pinlist, IDevice.class);
		if (device != null) {
			final List<IDeviceICD> icdsAssociatedWithDevice = Collections.singletonList(icd);
			final SetMap<IICDBackshellTermination, IICDAssociatedSignal> icdBSTermToSignalMap =
					ICDSignalDetailsFinder.getICDSignalsAssociatedWithTerm(icdsAssociatedWithDevice);
			ICDUtils.processMatchingBSTerminals(device, icdsAssociatedWithDevice, (icdBSTerm, logicBSTerm) -> {
				for (IICDAssociatedSignal signal : icdBSTermToSignalMap.pullReadOnlySafeSet(icdBSTerm)) {
					placedSignals.add(signal.getNetName());
				}
			});
		}
	}

	private void updateConnectedICDs(Set<IUID> icdColl, Set<String> sigNames)
	{
		for (IUID uid : icdColl) {
			IUIDObject obj = getObject(uid);
			if (obj instanceof IICD) {
				IICD iicd = (IICD) obj;
				if (!Collections.disjoint(sigNames, ICDUtils.getSignalNames(iicd))) {
					m_connectedICDs.add(uid);
				}
			}
		}
	}

	private void clearCaches()
	{
		CHSSwingUtils.invokeBlockingInEdt(() ->
		{
			m_unallocatedICDs.clear();
			m_placedICDs.clear();
			m_unplacedICDs.clear();
			m_placedInBuildlistICDs.clear();
			m_connectedICDs.clear();
			m_variantICDs.clear();
			m_sharedDeviceNames.clear();
		});
	}

	public void invalidate()
	{
		clearCaches();
	}

	@Override public void modelPreChanged(ModelChangeEvent e)
	{
		clearCaches();
	}

	@Override public void modelChanged(ModelChangeEvent e)
	{
		clearCaches();
	}

	@Override public void destroy()
	{
		unRegisterListners();
		clearCaches();
		super.destroy();
	}

	private void unRegisterListners()
	{
		m_design.getDesignICDContainer().removeActiveBuildlistChangeListner(this);
		m_design.getDesignICDContainer().removeApplicableICDChangeListner(this);
	}

	@Override protected String doGetPresentationName(IUID uid)
	{
		String role = getRoleForICD(uid);
		if (role != null) {
			return role;
		}
		return super.doGetPresentationName(uid);
	}

	@Nullable private String getRoleForICD(IUID uid)
	{
		IUIDObject obj = getObject(uid);

		if (obj instanceof IICD) {
			IICD iicd = (IICD) obj;
			return iicd.getRole();
		}
		return null;
	}

	public void activateObject(IUID uid)
	{
		if (getObject(uid) instanceof IICD) {
			m_browserPanel.doubleClicked();
		}
	}

	@Override public void selectObject(IUID uid)
	{
			IUIDObject object = getObject(uid);
			if (object instanceof IICD) {
				m_browserPanel.selectObject((IICD) object);
			}
			else {
				m_browserPanel.unselectCurrentSelection();
			}
	}

	@Override public void mouseReleased(MouseEvent e)
	{
			if (e.isPopupTrigger()) {
				selectTheLatestLibraryPartFromICD();
				m_browserPanel.displayPopupMenu(e);
			}
	}

	private void selectTheLatestLibraryPartFromICD()
	{
		ILibraryPartSelection partSelection = m_browserPanel.getPartSelection();
		IICDSelection iicdSelection = CommonUtils.cast(partSelection, IICDSelection.class);
		if (iicdSelection != null) {
			IDeviceICD icd = iicdSelection.getICD();
			if (icd != null) {
				m_browserPanel.selectLibraryObjects(icd);
			}
		}
	}

	@Override public void activeBuildListChanged()
	{
		reBuildTheTree();
	}

	private void reBuildTheTree()
	{
		CHSSwingUtils.invokeBlockingInEdt(() ->
		{
			clearCaches();

			adjustPlacedInBuildListMap();

			rebuildICDTree();
		});
	}

	protected void rebuildICDTree()
	{
		m_browserPanel.reBuildICDTree();
	}

	private void adjustPlacedInBuildListMap()
	{
		assert m_design.getProject() != null;
		IBuildList activeBL = m_design.getProject().getBuildListMgr().getActiveBuildList();
		Set<IDeviceICD> applicableICDs = m_design.getDesignICDContainer().getApplicableICDs();
		if (activeBL != null && activeBL.containsDesignUID(m_design.getUID()) &&
				activeBL.getDesignDescriptors().getSize() > 1 && applicableICDs != null && !applicableICDs.isEmpty()) {
			ReporterQueryHandlerFactory queryHandlerFactory = new ReporterQueryHandlerFactory();
			ITagBasedAttributeValuesQueryHandler queryHandler =
					queryHandlerFactory.getTagBasedAttributeValuesQueryHandler(
							ICDUtils.toICDComparableKey(IAttributeTypes.NAME));
			List<IUID> desUIDs = new ArrayList<IUID>();
			desUIDs.addAll(activeBL.getDesignsList());
			desUIDs.remove(m_design.getUID());
			m_placedInOtherDesignsDeviceNames =
					queryHandler.getDistinctAttributeValues(UIDUtils.convertToStringArray(desUIDs),
							new String[]{IXObjectNames.IDevice_HtmlName}).stream()
							.map(s -> ICDUtils.toICDComparableKey(s)).collect(Collectors.toCollection(HashSet::new));
		}
		else {
			m_placedInOtherDesignsDeviceNames.clear();
		}
	}

	@Override public void applicableICDsChanged()
	{
		reBuildTheTree();
	}

	protected static class ICDTreeComparator extends UIDComparator
	{

		private ICDTreeComparator()
		{
		}

		@Override public int compare(Object o1, Object o2)
		{
			Object obj1 = getObject(o1);
			Object obj2 = getObject(o2);

			// now make sure they're (really) ICD objects
			if (!(obj1 instanceof IICD) || !(obj2 instanceof IICD)) {
				return 0;
			}

			String n1 = ((IICD) obj1).getRole();
			String n2 = ((IICD) obj2).getRole();
			if (n1 == null) {
				if (n2 == null) {
					return 0;
				}
				return 1;
			}
			if (n2 == null) {
				return -1;
			}
			// use the system wide comparator
			return UIUtils.compareAlphaNumStrings(n1, n2);
		}
	}
}
