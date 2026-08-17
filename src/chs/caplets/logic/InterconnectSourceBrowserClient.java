/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2005-2025 Siemens
 */
package chs.caplets.logic;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IAction;
import chs.caf.caplet.action.IActionMgr;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.caplets.logic.actions.AddInterconnectWireAction;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductorIterator;
import chs.cof.logical.cable.IInterconnectMember;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IMulticoreIterator;
import chs.cof.logical.cable.IOverbraid;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibrary;
import chs.cof.parts.ILibraryBaseObject;
import chs.cof.parts.ILibraryInnerCore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.ILibraryObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.system.FactoryMgr;
import chs.utilities.AlphaNumComparator;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.helpers.LibraryHelper;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.UtilsHelper;
import chs.utility.logic.ILogicModel;
import chs.utility.transmodel.TransientNamedObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InterconnectSourceBrowserClient extends BrowserClientHelper
{

	private IDesign m_design;
	private ISchemDiagram m_diagram;

	private BrowserFolder m_conductorsFolder;
	private BrowserFolder m_multicoresFolder;
	private BrowserFolder m_overbraidsFolder;

	private final Map<IUID, BrowserFolder> m_folders = new LinkedHashMap<IUID, BrowserFolder>();

	private static final String CONDUCTORS_TEXT =
			ResourceMgr.getString(BrowserClient.class, "BrowserClient.Conductors.text");
	private static final String MULTICORES_TEXT =
			ResourceMgr.getString(BrowserClient.class, "BrowserClient.Multicores.text");
	private static final String OVERBRAIDS_TEXT =
			ResourceMgr.getString(BrowserClient.class, "BrowserClient.Overbraids.text");
	private IUIDObject m_rootObject;
	private IInterconnectSourceInfo m_interconnectSourceInfo;

	public InterconnectSourceBrowserClient(ICapletController controller)
	{
		super(controller);
		m_design = ((ILogicModel) controller.getCapletModel()).getDesign();
		m_diagram = ((ILogicModel) controller.getCapletModel()).getDiagram();
		m_design.getInterconnectSourceInfo();
		m_rootObject = new TransientNamedObject("", FactoryMgr.createUID());
		setRootObject(m_rootObject);

		// setup the static children of this browser tree
		m_conductorsFolder = createFolder(CONDUCTORS_TEXT);
		m_multicoresFolder = createFolder(MULTICORES_TEXT);
		m_overbraidsFolder = createFolder(OVERBRAIDS_TEXT);
		m_folders.put(m_conductorsFolder.getUID(), m_conductorsFolder);
		m_folders.put(m_multicoresFolder.getUID(), m_multicoresFolder);
		m_folders.put(m_overbraidsFolder.getUID(), m_overbraidsFolder);
	}

	protected IUIDObject getRootObject()
	{
		m_interconnectSourceInfo = m_design.getInterconnectSourceInfo();
		return m_rootObject;
	}

	public List<IUID> getChildren(IUID uid)
	{
		if (uid == getRoot()) {
			if (m_diagram.getUID().isEquiv(m_design.getInterconnectSourceInfo().getDiagramUID())) {
				return CollectionUtils.getObjectList(m_folders.keySet(), IUID.class);
			}
			else {
				return Collections.emptyList();
			}
		}
		IUIDObject obj = getObject(uid);
		if (obj == m_conductorsFolder) {
			return getLibraryWires();
		}
		else if (obj == m_multicoresFolder) {
			return getLibraryMulticores();
		}
		else if (obj == m_overbraidsFolder) {
			return getLibraryOverbraids();
		}
		else if (obj instanceof IInterconnectToDoItem) {
			return getInterconnectToDoItemChildren((IInterconnectToDoItem) obj);
		}
		else if (obj instanceof IMulticore) {
			return getMulticoreChildren((IMulticore) obj);
		}
		else if (obj instanceof ILibraryMulticore) {
			return getLibraryMulticoreChildren((ILibraryMulticore) obj);
		}
		else if (obj instanceof ILibraryInnerCore) {
			return getLibraryInnercoreChildren((ILibraryInnerCore) obj);
		}
		else {
			return Collections.emptyList();
		}
	}

	public String getToolTipText(IUID uid, IUID parentUID)
	{
		return null;
	}

	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{
		Icon icon = super.getIcon(uid);
		IUIDObject obj = getObject(uid);
		if (icon == null) {
			if (obj instanceof IInterconnectToDoItem) {
				icon = MulticoreLibraryHelper.getIcon(((IInterconnectToDoItem) obj).getLibraryRef());
			}
			else if (obj instanceof ILibraryObject || obj instanceof ILibraryInnerCore) {
				icon = MulticoreLibraryHelper.getIcon(uid);
			}
		}
		return icon;
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		if (uid == getRoot()) {
			return m_diagram.getUID().isEquiv(m_design.getInterconnectSourceInfo().getDiagramUID());
		}
		IUIDObject obj = getObject(uid);
		if (obj == m_conductorsFolder) {
			return !getLibraryWires().isEmpty();
		}
		else if (obj == m_multicoresFolder) {
			return !getLibraryMulticores().isEmpty();
		}
		else if (obj == m_overbraidsFolder) {
			return !getLibraryOverbraids().isEmpty();
		}
		else if (obj instanceof IInterconnectToDoItem) {
			return !getInterconnectToDoItemChildren((IInterconnectToDoItem) obj).isEmpty();
		}
		else if (obj instanceof IMulticore) {
			return !getMulticoreChildren((IMulticore) obj).isEmpty();
		}
		else if (obj instanceof ILibraryMulticore) {
			return !getLibraryMulticoreChildren((ILibraryMulticore) obj).isEmpty();
		}
		else if (obj instanceof ILibraryInnerCore) {
			return !getLibraryInnercoreChildren((ILibraryInnerCore) obj).isEmpty();
		}
		else {
			return false;
		}
	}

	@Nullable
	public IUIDObject getObject(IUID uid)
	{
		if (uid == getRoot()) {
			return getRootObject();
		}
		IUIDObject uidObj = (IUIDObject) m_folders.get(uid);
		if (uidObj != null) {
			return uidObj;
		}

		uidObj = m_uidMgr.getObject(uid);
		if (uidObj != null) {
			return uidObj;
		}
		uidObj = LibraryHelper.getLibraryInnerCore(uid);
		return uidObj;
	}

	public void activateObject(IUID uid)
	{
		IAction action = getController().getAction(AddInterconnectWireAction.class);

		if (action != null && action.isEnabled()) {
			ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "doubleclick", 0);
			IActionMgr activeActionMgr = CAFUtils.getInstance().getActiveActionMgr();
			Objects.requireNonNull(activeActionMgr).actionPerformed(action, ae);
		}
	}

	@Override @Nullable public String doGetPresentationName(IUID uid)
	{
		IUIDObject uidObj = getObject(uid);
		if (uidObj instanceof IInterconnectToDoItem) {
			return ((IInterconnectToDoItem) uidObj).getPartNumber();
		}
		else if (uidObj instanceof IMulticore && ((IMulticore) uidObj).getLibraryRef() != null) {
			IMulticore mc = (IMulticore) uidObj;
			ILibraryBaseObject libObj = LibraryHelper.getLibraryBaseObject(mc);
			if (libObj instanceof ILibraryObject) {
				return ((ILibraryObject) libObj).getPartNumber() + " (" + mc.getName() + ")";
			}
			else if (libObj instanceof ILibraryInnerCore) {
				return MulticoreLibraryHelper.toString((ILibraryInnerCore) libObj) + " (" + mc.getName() + ")";
			}
		}
		else if (uidObj instanceof ILibraryInnerCore) {
			return MulticoreLibraryHelper.toString((ILibraryInnerCore) uidObj);
		}

		return super.doGetPresentationName(uid);
	}

	private List<IUID> getToDoItemsForType(int type)
	{
		// Go through the to do items.
		Collection toDoItems = m_interconnectSourceInfo.getToDoItems();
		List<IUID> children = new ArrayList<IUID>(toDoItems.size());
		for (Object toDoItem : toDoItems) {
			IInterconnectToDoItem item = (IInterconnectToDoItem) toDoItem;
			// Look at the ones of the right type.
			if (item.getPartClass() == type) {
				ILogicObject logicObject = m_interconnectSourceInfo.getDerivedObject(item);

				// If an item has not had its part created yet, put it on the list.
				if (logicObject == null) {
					children.add(item.getUID());
				}

				// If the part has been created, but it's a multicores or overbraid, it may need to stay
				// on the to to list until more work has been done on it.
				if (logicObject instanceof IMulticore) {
					IMulticore mc = (IMulticore) logicObject;
					if (mc instanceof IOverbraid) {
						// Overbraids need to stay on the to do list until something has been added to it
						if (mc.getNumConductors() + mc.getNumMulticores() == 0) {
							children.add(logicObject.getUID());
						}
					}
					else {
						// Multicores need to stay on the to do list until all of their members have been added.
						if (!LibraryHelper.isFullyImplementedLibraryMulticore(mc)) {
							children.add(logicObject.getUID());
						}
					}
				}
			}
		}

		// The sort will be by part uid.
		Collections.sort(children, NamedObjectComparator.caseSensitiveComparator());
		return children;
	}

	private List<IUID> getLibraryWires()
	{
		return getToDoItemsForType(IInterconnectMember.TYPE_WIRE);
	}

	private List<IUID> getLibraryMulticores()
	{
		return getToDoItemsForType(IInterconnectMember.TYPE_MULTICORE);
	}

	private List<IUID> getLibraryOverbraids()
	{
		return getToDoItemsForType(IInterconnectMember.TYPE_OVERBRAID);
	}

	private List<IUID> getInterconnectToDoItemChildren(IInterconnectToDoItem item)
	{
		final ILibrary library = UtilsHelper.getCHSSystem().getPartsLibrary();
		final ILibraryMulticore libObj = library.getLibraryObject(ILibraryMulticore.class, item.getLibraryRef());
		if (libObj != null) {
			return getLibraryMulticoreChildren(libObj);
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<IUID> getMulticoreChildren(IMulticore mc)
	{
		List<IUID> children = new ArrayList<IUID>();

		if (!(mc instanceof IOverbraid)) {
			ILibraryBaseObject libMC = LibraryHelper.getLibraryBaseObject(mc);
			if (libMC instanceof ILibraryMulticore) {
				for (ILibraryInnerCore liwIt :
						LibraryHelper.getInnerCores(libMC, mc)) {
					children.add(liwIt.getUID());
				}
			}
			else {
				ILibraryInnerCore libIW;
				if (libMC instanceof ILibraryInnerCore) {
					libIW = (ILibraryInnerCore) libMC;
				}
				else {
					libIW = LibraryHelper.getLibraryInnerCore(mc);
				}
				if (libIW != null) {
					for (ILibraryInnerCore liwIt : LibraryHelper.getInnerCores(libIW, mc)) {
						children.add(liwIt.getUID());
					}
				}
			}
		}

		IMulticoreIterator iter = mc.getMulticores();
		while (iter.hasNext()) {
			IMulticore child = iter.getNext();
			children.remove(child.getInnercoreRef());
			// If the multicore is not finished, keep it on the to do list.
			if (!MulticoreLibraryHelper.isFullyImplemented(child)) {
				children.add(child.getUID());
			}
		}
		IConductorIterator condIter = mc.getConductorsIncludingShields();
		while (condIter.hasNext()) {
			chs.cof.logical.cable.IConductor child = condIter.getNext();
			children.remove(child.getInnercoreRef());
		}

		Collections.sort(children, COMPARATOR);
		return children;
	}

	private List<IUID> getLibraryInnercoreChildren(ILibraryInnerCore ic)
	{
		List<IUID> children = Collections.EMPTY_LIST;
		if (!LibraryHelper.isInnerCoreLeaf(ic)) {
			Collection<ILibraryInnerCore> liwCol = LibraryHelper.getInnerCores(ic);
			children = new ArrayList<IUID>(liwCol.size());
			for (ILibraryInnerCore liwIt : liwCol) {
				children.add(liwIt.getUID());
			}
			Collections.sort(children, COMPARATOR);
		}
		return children;
	}

	private List<IUID> getLibraryMulticoreChildren(ILibraryMulticore ic)
	{
		Collection<ILibraryInnerCore> liwCol = LibraryHelper.getInnerCores(ic);
		List<IUID> children = new ArrayList<IUID>(liwCol.size());
		for (ILibraryInnerCore liwIt : liwCol) {
			children.add(liwIt.getUID());
		}
		Collections.sort(children, COMPARATOR);
		return children;
	}

	private final Comparator COMPARATOR = new AlphaNumComparator()
	{
		public int compare(Object o1, Object o2)
		{
			assert (o1 instanceof IUID && o2 instanceof IUID);
			//noinspection ConstantConditions
			if (o1 instanceof IUID && o2 instanceof IUID) {
				return super.compare(getPresentationName((IUID) o1), getPresentationName((IUID) o2));
			}
			else {
				return 0;
			}
		}
	};

	private final Comparator LibraryInnercoreComparator = new AlphaNumComparator()
	{
		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof ILibraryInnerCore && o2 instanceof ILibraryInnerCore) {
				return super.compare(MulticoreLibraryHelper.toString((ILibraryInnerCore) o1),
						MulticoreLibraryHelper.toString((ILibraryInnerCore) o2));
			}
			else {
				return 0;
			}
		}
	};

	private final Comparator MulticoreChildComparator = new AlphaNumComparator()
	{
		public int compare(Object o1, Object o2)
		{
			if (o1 instanceof ILibraryInnerCore) {
				o1 = MulticoreLibraryHelper.toString((ILibraryInnerCore) o1);
			}
			if (o2 instanceof ILibraryInnerCore) {
				o2 = MulticoreLibraryHelper.toString((ILibraryInnerCore) o2);
			}
			return super.compare(o1, o2);
		}
	};
}
