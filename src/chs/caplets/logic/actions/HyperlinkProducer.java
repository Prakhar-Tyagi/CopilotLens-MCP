/*
 * Copyright 2005-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.helpers.ILogicHyperlink;
import chs.caf.caplet.helpers.ILogicHyperlinkProducer;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caplets.helpers.AbstractHyperlinkProducer;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.drawplus.IXRefText;
import chs.cof.logical.IDesign;
import chs.cof.logical.ILayoutDesignMgr;
import chs.cof.logical.ILayoutLogicDesign;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.ILogicDesignIterator;
import chs.cof.logical.ISourceObjectRef;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IBlockDevice;
import chs.cof.logical.cable.IBlockDevicePin;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IConnectorPin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IHighway;
import chs.cof.logical.cable.IInterconnectConductor;
import chs.cof.logical.cable.IInterconnectObject;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.ILogicOtherComponent;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.IBaseShareableDiagramObject;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.schem.ISchemStackPin;
import chs.cof.logical.schem.ISegment;
import chs.cof.logical.shared.ICrossReferenceMonitor;
import chs.cof.logical.shared.ISharedPinListUsage;
import chs.cof.logical.shared.ISharedUsage;
import chs.cof.project.IProject;
import chs.cof.project.hierarchy.IHierarchicalConnectivityFinder;
import chs.cof.symbol.IBlock;
import chs.common.*;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.MessageHelper;
import chs.utility.NamedObjectUtils;
import chs.utility.SymbolUtils;
import chs.utility.logic.DesignHelper;
import org.jetbrains.annotations.Nullable;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Creates hyperlinks required to show Logic objects in View Related Items dialog.
 */
public abstract class HyperlinkProducer extends AbstractHyperlinkProducer<IDesign, ISchemDiagram> implements
		ILogicHyperlinkProducer
{

	protected HyperlinkProducer(IProject project, IDesign design, ISchemDiagram diagram)
	{
		super(project, design, diagram, null);
	}

	private boolean hasHyperlinks(IRepresentedObject repObj)
	{
		// FEAT13040 : Any sort of pinlist, pin or conductor can now "have hyperlinks"
		IUIDObject rawConn = repObj.getRawConnectivity();
		return rawConn instanceof IPinList || rawConn instanceof IConductor || rawConn instanceof IAbstractPin ||
				rawConn instanceof IHighway || rawConn instanceof ILogicOtherComponent;
	}

	@SuppressWarnings({"ConstantConditions", "OverlyLongMethod"})
	public List<ILogicHyperlink> createHyperlinks()
	{
		List<ILogicHyperlink> links = new ArrayList<ILogicHyperlink>(1);
		ILogicObject logicObject = (ILogicObject) m_repObj.getRawConnectivity();
		IBaseShareableDiagramObject diagramObject = (IBaseShareableDiagramObject) m_repObj;

		// Look for other usages of the object
		chs.cof.logical.schem.IPinList sourcePinList = null;
		IBlock sourceBlock = null;
		if (diagramObject instanceof chs.cof.logical.schem.IPinList) {
			sourcePinList = (chs.cof.logical.schem.IPinList) diagramObject;
			sourceBlock = sourcePinList.getBlock();
		}

		final IDesign layoutDsign = logicObject.getLogicDesign();
		if (layoutDsign instanceof ILayoutLogicDesign) {
			final ILayoutDesignMgr layoutDesignMgr = ((ILayoutLogicDesign) layoutDsign).getLayoutDesignMgr();
			for (ISourceObjectRef ref : layoutDesignMgr.getSourceObjectRefs(logicObject.getUID())) {
				final IUID sourceDesignUID = ref.getSourceDesignUID();
				final IUID sourceObjectUID = ref.getSourceObjectUID();
				final ILogicDesign sourceDesign = DesignUtils.getLoadedDesign(sourceDesignUID, ILogicDesign.class);
				if (sourceDesign != null) {
					addLink(links, new SourceSchematicHyperlink(m_currentDiagram, sourceDesign, sourceObjectUID));
				}
			}
		}

		if (logicObject instanceof IBlockDevice) {
			IDesignContainer targetDesign = ((IBlockDevice) logicObject).getAssociatedDesign(null);
			if (targetDesign instanceof ILogicDesign) {
				targetDesign.refresh();
				int diagramCount = targetDesign.getNumDiagrams();
				if (diagramCount > 0) {
					ISchemDiagram targetDiagram = ((ILogicDesign) targetDesign).getDiagrams(true).getNext();
					addLink(links, new BlockDeviceHyperlink(m_currentDiagram, (IDesign) targetDesign, targetDiagram));
				}
			}
		}

		ICrossReferenceMonitor monitor = m_currentDesign.getProject().getCrossReferenceMonitor();

		Collection<ISharedUsage> usages = new ArrayList<ISharedUsage>();

		usages.addAll(monitor.getSharedUsages((IRepresentedObject) m_repObj, m_currentDesign));

		Collection<? extends ISharedUsage> associatedPinUsages = getBlockPinAssociatedPinUsages(monitor, logicObject);
		usages.addAll(associatedPinUsages);

		for (Object o : usages) {
			ISharedUsage usage = (ISharedUsage) o;

			if (usage instanceof ISharedPinListUsage) {
				ISharedPinListUsage splu = (ISharedPinListUsage) usage;

				// Blocks link to references only if they are instances of the same block
				if (sourceBlock != null && splu.isReference() && splu.getBlock() != sourceBlock) {
					continue;
				}

				// Composites don't link to their own blocks
				chs.cof.logical.schem.IPinList targetPinList = splu.getSchemPinList();
				if (targetPinList != null && SymbolUtils.areInSameCompositeInstance(sourcePinList, targetPinList)) {
					continue;
				}
			}

			// Throw out the usage if its design is not on the active build list
			if (!DesignHelper.testOnActiveBuildList(usage.getDesignUID(), m_project)) {
				continue;
			}

			// Throw out your own usage
			if (usage.getDiagramObjectUID().isEquiv(diagramObject.getUID())) {
				continue;
			}

			addLink(links, new UsageHyperlink(m_currentDiagram, usage));
		}

		// Interconnect links
		// TODO jacobt FEAT13040 : test View Related Items for ICX
		// it's just possible that our comprehensive set of regression tests won't catch this variant of View Related Items functionality
		// are the following links OK in addition to the ones from usages above?  ICX would not previously have usages so need to check the following:
		if (logicObject instanceof IInterconnectConductor
				|| (logicObject instanceof IInterconnectObject && logicObject instanceof IConnector)) {
			// Look for diagrams that ought to have objects generated from the interconnect object.
			for (ILogicDesignIterator designItr = ((IPrivilegedDesignMgr)m_project.getDesignMgr()).getAbstractLogicDesigns();
				 designItr.hasNext(); ) {
				ILogicDesign design = designItr.getNext();
				if (!DesignHelper.testOnActiveBuildList(design.getUID(), m_project)) {
					continue;
				}
				IInterconnectSourceInfo isi = design.getInterconnectSourceInfo();
				// Make sure the design was derived from this design.
				if (isi == null || isi.getDiagramUID() == null
						|| !m_currentDesign.getUID().isEquiv(isi.getSourceDesignUID())) {
					continue;
				}
				// Make sure the derived diagram still exists
				ISchemDiagram derivedDiagram = design.getDiagram(isi.getDiagramUID());
				if (derivedDiagram == null) {
					continue;
				}
				if (logicObject instanceof IInterconnectConductor) {
					if (isi.isSourceConductor((IInterconnectConductor) logicObject)) {
						addLink(links, new GeneratedConductorHyperlink(m_currentDiagram, design, derivedDiagram,
								(IInterconnectConductor) logicObject));
					}
				}
				else {
					if (isi.isSourceConnector((IConnector) logicObject)) {
						addLink(links, new GeneratedConnectorHyperlink(m_currentDiagram, design, derivedDiagram,
								(IConnector) logicObject));
					}
				}
			}
		}
		else if (logicObject instanceof IConnector
				|| logicObject instanceof IConnectorPin
				|| logicObject instanceof IWireConductor
				|| logicObject instanceof IShieldConductor) {
			// Look for an interconnect object that was used to generate the object
			IInterconnectSourceInfo isi = m_currentDesign.getInterconnectSourceInfo();
			if (isi != null && m_currentDiagram.getUID().isEquiv(isi.getDiagramUID())) {
				ILogicDesign sourceDesign = m_project.getDesignMgr().getAbstractLogicDesign(isi.getSourceDesignUID());
				ISchemDiagram sourceDiagram = sourceDesign.getDiagram(isi.getSourceDiagramUID());

				ILogicObject candidate = logicObject;
				if (candidate instanceof IConnectorPin) {

					// Client is cross referencing a connector pin, check against the connector owning the pin.
					IConnectorPin pin = (IConnectorPin) candidate;
					candidate = pin.getOwner();
				}

				IConnector cnctr = null;
				if (candidate instanceof IConnector) {
					cnctr = (IConnector) candidate;
					if (!isi.isDerivedConnector(cnctr)) {
						// Connector but not one generated from an interconnect connector.
						cnctr = null;
					}
				}

				if (cnctr != null) {
					ILogicHyperlink link1 = null;
					if (sourceDiagram != null) {
						link1 = new InterconnectConnectorHyperlink(m_currentDiagram, sourceDesign, sourceDiagram,
								logicObject);
						addLink(links, link1);
					}
					if (link1 == null || link1.getConfidence() < 1.0) {
						ILogicHyperlink link2 =
								new InterconnectConnectorHyperlink(m_currentDiagram, sourceDesign, null, logicObject);
						addLink(links, link2);
					}
				}
				if ((logicObject instanceof IConductor && isi.isDerivedConductor((IConductor) logicObject))
						|| (logicObject instanceof IMulticore && isi.isDerivedMulticore((IMulticore) logicObject))) {
					ILogicHyperlink link1 = null;
					if (sourceDiagram != null) {
						link1 = new InterconnectConductorHyperlink(m_currentDiagram, sourceDesign, sourceDiagram,
								logicObject);
						addLink(links, link1);
					}
					if (link1 == null || link1.getConfidence() < 1.0) {
						ILogicHyperlink link2 =
								new InterconnectConductorHyperlink(m_currentDiagram, sourceDesign, null, logicObject);
						addLink(links, link2);
					}
				}
			}
		}

		return links;
	}

	private Collection<? extends ISharedUsage> getBlockPinAssociatedPinUsages(ICrossReferenceMonitor monitor,
			ILogicObject logicObject)
	{
		Collection<? extends ISharedUsage> associatedPinUsages = Collections.emptyList();
		if (logicObject instanceof IBlockDevicePin) {
			IBlockDevicePin blockDevicePin = (IBlockDevicePin) logicObject;
			IPinList blockDevice = ((IGenericPin) logicObject).getOwner();
			Set<ILogicDesign> logicDesignsInScope = Collections.emptySet();
			if (blockDevice instanceof IBlockDevice) {
				IDesignContainer design = ((IBlockDevice) blockDevice).getAssociatedDesign(null);
				if (design instanceof ILogicDesign) {
					logicDesignsInScope = Collections.singleton((ILogicDesign) design);
				}
			}
			IHierarchicalConnectivityFinder finder = FactoryMgr.getLogicalFactory().getConnectivityFinder();
			IAbstractPin childPin = finder.getChildPin(blockDevicePin, logicDesignsInScope);
			if (childPin != null) {
				associatedPinUsages = monitor.getSharedUsages(childPin, childPin.getLogicDesign());
			}
		}
		return associatedPinUsages;
	}

	private boolean addLink(List<ILogicHyperlink> links, ILogicHyperlink link)
	{
		if (link != null && link.getConfidence() > 0.0) {
			return links.add(link);
		}
		else {
			return false;
		}
	}

	@Override public boolean hasValidSelection()
	{
		return m_repObj != null;
	}

	public void handleNoLinksState()
	{
		String msg;
		IUIDObject logicObj = m_repObj.getRawConnectivity();
		if (logicObj instanceof IBlockDevice) {
			msg = ResourceMgr.
					getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.BlockDeviceMessage.text");
		}
		else if (logicObj instanceof ILogicObject && ((ILogicObject) logicObj).isShared()) {
			msg = ResourceMgr
					.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.SharedMessage.text");
		}
		else {
			msg = ResourceMgr
					.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.NonSharedMessage.text");
		}

		MessageHelper.showWarningMessage(getParentDialogFrame(),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.Title.text"),
				ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.NoLinksFound.Heading.text"),
				msg);
	}

	protected abstract SelectSet getCurrentSelections();

	public void reset()
	{
		m_repObj = getOperand(getCurrentSelections());
	}

	public Frame getParentDialogFrame()
	{
		return CAFUtils.getInstance().getWindowMgr().getDialogFrame();
	}

	@Nullable public IRepresentedObject getOperand(SelectSet selections)
	{
		IRepresentedObject repObj = null;
		SelectedUIDObjectIterator it = selections.getSelectedUIDObjects();

		// Reduce the selections to a single logic object. If this can't be done, reject.
		while (it.hasNext()) {
			IUIDObject obj = it.getNext();
			if (obj instanceof ISchemStackPin) {
				continue; // Ignore Stack Pin
			}
			IRepresentedObject currRepObj = null;
			if (obj instanceof IRepresentedObject) {
				currRepObj = (IRepresentedObject) obj;
			}
			else if (obj instanceof IXRefText) {
				currRepObj = ((IXRefText) obj).getRepObject();
			}
			else if (obj instanceof IDiagramObject) {
				IDiagramObject parent = ((IDiagramObject) obj).getParent();
				if (parent instanceof IRepresentedObject) {
					currRepObj = (IRepresentedObject) parent;
				}
				else if (parent instanceof ISegment) {
					currRepObj = ((ISegment) parent).getConductor();
				}
			}
			if (repObj == null) {
				repObj = currRepObj;
			}
			else if (currRepObj != repObj) {
				return null;
			}
		}

		if (repObj != null && hasHyperlinks(repObj)) {
			return repObj;
		}
		return null;
	}

	public String getViewRelatedDialogTitle()
	{
		return ResourceMgr.getString(CrossLinkAction.class, "CrossLinkAction.Dialog.Title.text",
				NamedObjectUtils.getName(m_repObj));
	}
}
