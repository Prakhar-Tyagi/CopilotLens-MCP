/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2003-2025 Siemens
 */
package chs.caplets.symbol;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.IUndoableObject;
import chs.caf.caplet.helpers.replication.IDataTransferReplicator;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.Selection;
import chs.caplets.shared.DataTransfer;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxGroup;
import chs.cof.draw.IGfxGroupable;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IImageDataSource;
import chs.cof.draw.ILine;
import chs.cof.draw.IPolyline;
import chs.cof.draw.ISheet;
import chs.cof.drawplus.IAttributeText;
import chs.cof.drawplus.IBaseDiagram;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IGridDatumRepresentation;
import chs.cof.drawplus.IPropText;
import chs.cof.drawplus.IPropertiedCurve;
import chs.cof.drawplus.IPropertiedGfxGroup;
import chs.cof.drawplus.IPropertiedGraphic;
import chs.cof.drawplus.IPropertiedImage;
import chs.cof.drawplus.IPropertiedLine;
import chs.cof.drawplus.IPropertiedPolyline;
import chs.cof.drawplus.IXRefPlaceholder;
import chs.cof.logical.cable.Device;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.IDevicePin;
import chs.cof.logical.cable.IFunction;
import chs.cof.logical.cable.IFunctionPin;
import chs.cof.logical.cable.IGenericPin;
import chs.cof.logical.cable.IInternalLink;
import chs.cof.logical.cable.IInternalPin;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.ISplicePin;
import chs.cof.logical.schem.IGenericSchemPin;
import chs.cof.logical.schem.IInternalConnectivityPinList;
import chs.cof.logical.schem.IInternalLinkPolyline;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.Border;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.IUserDefinedZone;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IEngineeringDatum;
import chs.common.ILocation;
import chs.common.IPropertiedObject;
import chs.common.IProperty;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.attr.IAttributeProvider;
import chs.common.reln.IRelatedEntityType;
import chs.ctf.print.PrintRegionHelper;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CollectionUtils;
import chs.utilities.ResourceMgr;
import chs.utility.GfxUtils;
import chs.utility.Replicator;
import chs.utility.SymbolUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.CreationDeletionHelper;
import chs.utility.helpers.LogHelper;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * * This class contains caplet specific implementations of the IDataTransfer functions used for transfering data *
 * around using the clipboard. * * The current implementation of this simply uses the Replictor functionality along with
 * the current select set * to copy/paste objects.  The first release will not have cut/copy/paste functionality outside
 * of CAF. In other words, * this implementation is not going to use the clipboard with XML. The reason for this is
 * simply speed of * implementation.  Additionally, there is not an outstanding requirement to be able to paste outside
 * of a Java * environment at this time. *
 */

public class SymbolDataTransfer extends DataTransfer
{

	/**
	 * m_PinHolder is a temporary pin list to which pins are added on copy. The original pin list from the source symbol
	 * should not be used as it is not guaranteed that the pin will be pasted to that pion list
	 */

	protected chs.cof.logical.cable.IPinList m_pinHolder = null;

	/**
	 * m_parentRelationMap holds the relationship between datum and its parent datum.
	 */
	protected Map<IDatum, IDatum> m_relatedDatumParentMap = null;
	protected Map<IAttributeDatum, IDatum> m_attributeDatumParentMap = null;
	protected Map<IDatum, IRelatedEntityType> m_relatedEntityMap = null;
	private Map<String, IProperty> m_properties;    // map prop name to actual property from the source model

	public SymbolDataTransfer()
	{
		IUID uid = FactoryMgr.getCommonFactory().createUID();
		m_pinHolder = new Device(uid);
		UIDMgr.addObject(m_pinHolder);

		m_relatedDatumParentMap = new HashMap<IDatum, IDatum>();
		m_attributeDatumParentMap = new HashMap<IAttributeDatum, IDatum>();
		m_relatedEntityMap = new HashMap<IDatum, IRelatedEntityType>();
		m_properties = new HashMap<String, IProperty>();
	}

	/**
	 * If doing a paste from a copy, this will duplicate the itesm to be copied and paste them down.
	 *
	 * @return True if successful.
	 */
	public boolean doCAFPaste(@NotNull ICapletController controller)
	{
		super.doCAFPaste(controller);
		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
		Set<ILocation> offsetSet = new HashSet<ILocation>();

		if (capView instanceof GfxView) {
			GfxView gview = (GfxView) capView;
			Point delta = new Point();
			int offset = getOffsetForView(gview);
			if(NotaRepeatedCopy || !mouseMoved){
				delta.setLocation(offset,offset);
			}
			else if (m_sourcePoint != null && m_PrevPoint != null) {
				delta.setLocation(m_PrevPoint.x - m_sourcePoint.x, m_PrevPoint.y - m_sourcePoint.y);
			}
			else {
				// -- ideally it shouldn't come here just a safe check if sourcePoint is null
				offset = super.getOffsetForView(gview);
				delta.setLocation(offset, offset);
			}

			if (m_objectBuffer != null) {
				SelectSet sset = controller.getSelectMgr().getPreSelections();
				sset.clear();
				SelectSet pastedSelectSet = new SelectSet(); // create new SelectSet to avoid notification problems
				// before proecessing the paste, put all of the objects back in the uidmgr
				addOrRemovePreviousCopiedObjectsFromUIDMgr(true);

				// get the symbol model so that we can get the name manager
				// here we go, get the symbol def
				if (!(controller.getCapletModel() instanceof Model)) {
					// this is bad
					return false;
				}

				Model model = (Model) controller.getCapletModel();
				List<IUIDObject> bufferCopy = new ArrayList<IUIDObject>(m_objectBuffer);
				NonPastableItemCollector nonPastableItemCollector = new NonPastableItemCollector();
				filterNonPastableItems(bufferCopy, nonPastableItemCollector);
				//first remove the objects which are not pastable.
				nonPastableItemCollector.processCollectedObjects();

				//now create a fresh copy of pastable objects and do paste them individually.
				bufferCopy.clear();
				bufferCopy.addAll(m_objectBuffer);
				for (IUIDObject uidObj : bufferCopy) {
					if (uidObj instanceof IGfxObject) {
						IGfxObject gfxObj = (IGfxObject) uidObj;
						if (gfxObj instanceof IPropertiedLine) {
							offsetObject(((ILine) gfxObj).getStartPoint(), delta, offsetSet);
							offsetObject(((ILine) gfxObj).getEndPoint(), delta, offsetSet);
							gfxObj.setLocation(gfxObj.getLocation());
						}
						else if (gfxObj instanceof IPropertiedCurve || gfxObj instanceof IPropertiedPolyline) {
							List<ILocation> locs = ((IPolyline) gfxObj).getPoints();
							for (ILocation loc : locs) {
								offsetObject(loc, delta, offsetSet);
							}
							gfxObj.setLocation(gfxObj.getLocation());
						}
						else if (gfxObj instanceof IPropertiedGfxGroup) {
							// Get start location
							transformGfxGroup(offsetSet, gview,delta, GfxUtils.SCALE_ONE,  gfxObj);

							// Make sure all of the groups childner as in the sheet!
							ISheet sheet = gview.getSheet();
							for (IGfxObject gObj : ((IGfxGroup) gfxObj).getGfxObjects()) {
								sheet.addObject(gObj);
							}
						}
						else {
							IStamp stamp = model.getSymbolDef();
							if (gfxObj instanceof IGenericSchemPin) {

								// gdh 11/12/03 move access to model.getSymbolDef() to where used (cast does not work for Border)
								// now get the symboldef and ultimately the name manager for the destination symbol
								ISymbolDef sDef = (ISymbolDef) stamp;
								// add the pin to the owner since it couldn't be done until the paste
								// was completed.
								IGenericPin pin = ((IGenericSchemPin) gfxObj).getConnectivity();

								// add the pin to the symbol pin list...
								sDef.addGenericPin(pin);

								if (pin.isOverridden()) {
									// set the name of the pin
									pin.setName(pin.getName());
								}
								// start hack
								CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
								cdHelper.addCreationObject(pin);

								// now snapshot all of the pins properties
								for (IGfxObjectIterator pinIter =
										((ICompoundObject) gfxObj).getObjects(); pinIter.hasNext(); ) {
									IGfxObject subObj = pinIter.getNext();
									if (subObj instanceof IUndoableObject) {
										cdHelper.addCreationObject((IUIDObject) subObj);
									}
								}
								// end hack
							}
							if (gfxObj instanceof ISchemInternalLink) {
								ISymbolDef sDef = (ISymbolDef) stamp;
								IInternalLink link = ((ISchemInternalLink) gfxObj).getConnectivity();

								// add the pin to the symbol pin list...
								((IDevice) sDef.getConnectivity()).addInternalLink(link);
								//((IDevice)sDef).addInternalLink(link);
								link.setNameMgr(stamp.getNameMgr());

								// start hack
								CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
								cdHelper.addCreationObject(link);

								// now snapshot all of the link properties
								for (IGfxObjectIterator linkIter =
										((ICompoundObject) gfxObj).getObjects(); linkIter.hasNext(); ) {
									IGfxObject subObj = linkIter.getNext();
									if (subObj instanceof IUndoableObject) {
										cdHelper.addCreationObject((IUIDObject) subObj);
									}
									if ((subObj instanceof IInternalLinkPolyline)) {
										List<ILocation> locs = ((IPolyline) subObj).getPoints();
										for (ILocation loc : locs) {
											offsetObject(loc, delta, offsetSet);
										}
										subObj.setLocation(subObj.getLocation());
									}
								}
								// end hack
							}
							else if (gfxObj instanceof IPropText) {
								IPropText pText = (IPropText) gfxObj;
								String propName = pText.getPropertyName();
								IPropertiedObject owner = pText.getPropertyProvider();
								if (owner == null) {
									owner = stamp.getPropertyHolder();
								}
								// see if the property name already exists on the symbol. If it does, forget
								// about it. If it doesn't add it.
								if (owner.findPropertyByName(propName) != null) {
									// we need to remove the text from the UIDMgr since we are not going to paste it
									forgetPropText(pText);
									continue;
								}
								// OK, add the property to the owner then pass on and do the rest for the propText
								IProperty prop = findProperty(pText);
								if (prop == null) {
									forgetPropText(pText);
									continue;
								}
								owner.addProperty(prop);
							}
							else if (gfxObj instanceof IDatumRepresentation) {
								IDatumRepresentation newDatumRep = (IDatumRepresentation) gfxObj;
								IBaseDatum baseDatum = newDatumRep.getDatum();
								if (baseDatum instanceof IDatum) {
									IDatum datum = (IDatum) baseDatum;
									IDatum parentDatum = m_relatedDatumParentMap.get(datum);

									if (parentDatum != null) {
										// Check whether the parent is still alive, if not kill it.
										IUIDObject obj = UIDMgr.getNonDeletedObject(parentDatum.getUID());
										if (obj == null) {
											parentDatum = null;
										}
									}

									// There is no general datum as of now. So considering only as relatedEntityDatum.
									IRelatedEntityType relateEntityType = m_relatedEntityMap.get(datum);

									// If the pasted in other symbol then the parentDatum may not be present in the
									// destination symbol.
									Collection<IDatum> allREDDatums = stamp.getAllREDDatums();
									if (allREDDatums != null &&
											allREDDatums.contains(parentDatum)) {
										stamp.addDatum(relateEntityType, datum, parentDatum, -1);
									}
									else {
										stamp.addDatum(relateEntityType, datum, null, -1);
									}
								}
								else if (baseDatum instanceof IEngineeringDatum) {
									stamp.addDatum((IEngineeringDatum) newDatumRep.getDatum());
								}
								else if (baseDatum instanceof IAttributeDatum) {
									IAttributeDatum datum = (IAttributeDatum) baseDatum;
									IDatum parentDatum = m_attributeDatumParentMap.get(datum);

									if (parentDatum != null) {
										// Check whether the parent is still alive, if not kill it.
										IUIDObject obj = UIDMgr.getNonDeletedObject((parentDatum).getUID());
										if (obj == null) {
											parentDatum = null;
										}
									}

									stamp.addDatum((IAttributeDatum) newDatumRep.getDatum());
									if (parentDatum != null) {
										stamp.addAttributeDatumAssociation(parentDatum, datum);
									}
								}

								CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
								cdHelper.addCreationObject(newDatumRep);

								// now snapshot all of the pins properties
								for (IGfxObjectIterator datumRepIter =
										newDatumRep.getObjects(); datumRepIter.hasNext(); ) {
									IGfxObject subObj = datumRepIter.getNext();
									if (subObj instanceof IUndoableObject) {
										cdHelper.addCreationObject((IUIDObject) subObj);
									}
								}
							}
							else if (gfxObj instanceof IAttributeText) {
								// always set the attribute text provider to be the target symbol
								// this gets here for text copied in isolation
								// when text is copied along with it's attribute provider (e.g. a pin + its text)
								// the text does not pass through this route.
								IAttributeText attrText = (IAttributeText) gfxObj;
								ISymbolDef sDef = (ISymbolDef) stamp;
								attrText.setAttributeProvider(sDef.getConnectivity());
							}
							if (!(gfxObj instanceof ISchemInternalLink)) {
								ILocation loc = gfxObj.getLocation();
								offsetObject(loc, delta, offsetSet);
								gfxObj.setLocation(loc);
							}
						}
						gview.getSheet().addObject((IGfxObject) uidObj);
					}
					pastedSelectSet.add(new Selection(uidObj),
							false); // don't notify of selection change till all are selected
				}
				resetRepeatedCopyFlag();
				//sset.notifySelectionChanged();
				controller.getSelectMgr().getPreSelections().clear();
				controller.getSelectMgr().getPreSelections().add(pastedSelectSet);

				manageUndo(false);
				//After the SchemInternalLink is added to pinList, connect it to pin nodes
				if (!isTargetCommentSymbol()) {
					connectorInternalLinks(model, bufferCopy);
				}
			}
			ICapletView view = CAFUtils.getInstance().getActiveCapletView();
			if (view != null) {
				view.invalidate(IViewInvalidationEnum.eFull);
			}
			return true;
		}

		else {
			return false;
		}
	}

	private void connectorInternalLinks(Model model, List<IUIDObject> objects)
	{
		for (IUIDObject uidObj : objects) {
			if (uidObj instanceof ISchemInternalLink) {
				((IInternalConnectivityPinList) model.getSymbolDef().getGfx())
						.connectInternalLink((ISchemInternalLink) uidObj);
			}
		}
	}

	private boolean isTargetCommentSymbol()
	{
		ISymbolDef symbolDef = getCurrentActiveSymbol();
		return symbolDef != null && SymbolUtils.isCommentSymbol(symbolDef);
	}

	private boolean isTargetBorder()
	{
		Model model = getModel(CAFUtils.getInstance().getActiveCapletView());
		return model != null && model.getModelRoot() instanceof Border;
	}

	//dts0100565433 : we need to remove the text from the UIDMgr since we are not going to paste it
	private void forgetPropText(IPropText pText)
	{
		UIDMgr.removeObject(pText.getUID());
		CreationDeletionHelper cdHelper = CreationDeletionHelper.getTheCreationHelper();
		cdHelper.removeCreationObject(pText);
		m_creationObjects.remove(pText);
	}

	/**
	 * Clear our internal paste buffer
	 */
	public void clearPasteBuffer()
	{
		super.clearPasteBuffer();
		// now clean up the objects on the pin list
		m_pinHolder.removeAllProperties();
		m_pinHolder.removeAllProperties();

		m_relatedDatumParentMap.clear();
		m_relatedEntityMap.clear();
		m_attributeDatumParentMap.clear();
		m_properties.clear();
	}

	/**
	 * Adds an object to the selection set. A wrapper for a bit of bookkeeping.
	 *
	 * @param newObject newObject
	 * @param origObject origObject
	 * @param newObjs newObjs
	 */
	protected void addNewSelection(IUIDObject newObject, IUIDObject origObject, Collection<IUIDObject> newObjs)
	{
		super.addNewSelection(newObject, origObject, newObjs);
		// now see if there is something special to do for properties
		if (newObject instanceof IPropText) {
			IPropText propText = (IPropText) newObject;
			// get the owner of the text and see if it is a pin list. If it is, it must be added to the
			// pin list held by this class so that it can be pasted later
			IDiagramObject parent = propText.getParent();
			// if there is no diagram object owning this property, it must be a property on the symbol
			// take care of putting the property into the list of creation objects
			if (parent == null) {
				// we'll need to add the copied property to the pin list
				IProperty prop = findPropFromSourceModel(propText);
				if (prop != null) {
					rememberProp(prop);
				}
			}
		}
	}

	private void rememberProp(IProperty prop)
	{
		m_properties.put(prop.getName(), prop);
	}

	private IProperty findProperty(IPropText pText)
	{
		return m_properties.get(pText.getPropertyName());
	}

	@Nullable private IProperty findPropFromSourceModel(IPropText propText)
	{
		final IStamp stamp = ((ISymbolModel) m_sourceModel).getSymbolDef();
		return findPropFromStamp(propText, stamp);
	}

	@Nullable private IProperty findPropFromStamp(IPropText propText, IStamp stamp)
	{
		return stamp.getPropertyHolder().findPropertyByName(propText.getPropertyName());
	}

	private List<IDatumRepresentation> getDatumsToCopy(SelectSet selSet, Class<? extends IBaseDatum> T)
	{
		List<IDatumRepresentation> listDatumRep = new LinkedList<IDatumRepresentation>();
		SelectedUIDObjectIterator objIter = selSet.getSelectedUIDObjects();
		while (objIter.hasNext()) {
			IUIDObject uidObj = objIter.getNext();
			if (uidObj instanceof IDatumRepresentation) {
				IDatumRepresentation datumRep = (IDatumRepresentation) uidObj;
				IBaseDatum datum = datumRep.getDatum();
				if (T.isAssignableFrom(datum.getClass()) && !isParentInSelectionSet(datum, selSet)) {
					listDatumRep.add(datumRep);
				}
			}
		}
		return listDatumRep;
	}

	private boolean isParentInSelectionSet(IBaseDatum datum, SelectSet selSet)
	{
		Model model = ((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel());
		IBaseDiagram baseDiagram = model.getDiagram();
		IStamp symbolDef = model.getSymbolDef();
		IDatum parentDatum = null;
		if (datum instanceof IDatum) {
			parentDatum = symbolDef.getParentDatum((IDatum) datum);
		}
		else if (datum instanceof IAttributeDatum) {
			parentDatum = symbolDef.getAssociatedDatum((IAttributeDatum) datum);
		}

		boolean parentInSelList = false;
		while (parentDatum != null) {
			IDatumRepresentation parentDatumRep = (IDatumRepresentation) baseDiagram.getRepresentation(parentDatum);

			if (selSet.contains(parentDatumRep.getUID())) {
				parentInSelList = true;
				break;
			}
			parentDatum = symbolDef.getParentDatum(parentDatum);
		}
		return parentInSelList;
	}

	/**
	 * Replicates a group of objects in a selection set.  Returns the objects that have been replicated.
	 *
	 * @param selSet The selected items to be replicated
	 *
	 * @return The copies of all the selected items.
	 */
	public Collection<IUIDObject> replicatedSet(IDataTransferReplicator replicator, SelectSet selSet)
	{
		assert replicator instanceof Replicator;
		Replicator aReplicator = (Replicator) replicator;
		aReplicator.setInternalToSymbolApp(true);
		Collection<IUIDObject> newObjs = new ArrayList<IUIDObject>();
		SelectedUIDObjectIterator objIter = selSet.getSelectedUIDObjects();

		while (objIter.hasNext()) {
			IUIDObject origObject = objIter.getNext();

			// If it is not copyable skip it. Only top level objects should be copied.
			if ((origObject instanceof IDiagramObject) && (isObjectCopyable((IDiagramObject) origObject))) {
				// IDatumRepresentation objects are handled separately
				if (!(origObject instanceof IDatumRepresentation)) {
					IUIDObject newObject = copiedObj(aReplicator, origObject, selSet);

					if (newObject != null) {
						addNewSelection(newObject, origObject, newObjs);
					}
				}
			}
		}

		// IDatumRepresentation objects are handled here
		List<IDatumRepresentation> relatedDatumReps = getDatumsToCopy(selSet, IDatum.class);
		copyDatums(aReplicator, relatedDatumReps, newObjs);

		List<IDatumRepresentation> engineeringDatums = getDatumsToCopy(selSet, IEngineeringDatum.class);
		copyEngineeringDatums(aReplicator, engineeringDatums, newObjs);

		List<IDatumRepresentation> attributeDatums = getDatumsToCopy(selSet, IAttributeDatum.class);
		copyAttributeDatums(aReplicator, attributeDatums, newObjs);

		return newObjs;
	}

	private void copyEngineeringDatums(Replicator replicator, List<IDatumRepresentation> engineeringDatums,
			Collection<IUIDObject> newObjs)
	{
		Model model = ((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel());
		IStamp stamp = model.getSymbolDef();
		for (IEngineeringDatum datumGettingCopied : stamp.getAllEngineeringDatums()) {
			IDatumRepresentation datumRep =
					(IDatumRepresentation) (model.getDiagram().getRepresentation(datumGettingCopied));
			if (engineeringDatums.contains(datumRep)) {
				List<IUIDObject> datumReps = copiedObjs(replicator, datumRep);

				for (IUIDObject uidObj : datumReps) {
					m_creationObjects.add(uidObj);
					newObjs.add(uidObj);
				}
			}
		}
	}

	private void copyAttributeDatums(Replicator replicator, List<IDatumRepresentation> attributeDatums,
			Collection<IUIDObject> newObjs)
	{
		Model model = ((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel());
		IStamp stamp = model.getSymbolDef();
		for (IAttributeDatum datumGettingCopied : stamp.getAllAttributeDatums()) {
			IDatumRepresentation datumRep =
					(IDatumRepresentation) (model.getDiagram().getRepresentation(datumGettingCopied));

			if (attributeDatums.contains(datumRep)) {
				List<IUIDObject> datumReps = copiedObjs(replicator, datumRep);

				for (IUIDObject uidObj : datumReps) {
					m_creationObjects.add(uidObj);
					newObjs.add(uidObj);
				}
			}
		}
	}

	private void copyDatums(Replicator replicator, List<IDatumRepresentation> distinctParents,
			Collection<IUIDObject> newObjs)
	{
		// Purpose: is to preserve the order of the datums in the copied buffer.
		// Steps:
		// 1. Build the tree from the selected list, which has the same order as it is the browser tree
		// 2. Copy the datums from the tree.
		Model model = ((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel());
		IStamp stamp = model.getSymbolDef();

		// Preserving the order at each level of the tree
		// Build the tree of ParentDatum and its children.
		Map<IDatum, Map<IRelatedEntityType, List<IDatum>>> datumTreeToBeCopied = new HashMap<IDatum,
				Map<IRelatedEntityType, List<IDatum>>>();

		for (IDatumRepresentation datumRep : distinctParents) {
			IDatum parentDatum = stamp.getParentDatum((IDatum) datumRep.getDatum());
			Map<IRelatedEntityType, List<IDatum>> childrenMap = datumTreeToBeCopied.get(parentDatum);

			if (childrenMap == null) {
				childrenMap = new HashMap<IRelatedEntityType, List<IDatum>>();
				datumTreeToBeCopied.put(parentDatum, childrenMap);
			}

			List<IDatum> list = childrenMap.get(stamp.getRelatedEntityType((IDatum) datumRep.getDatum()));
			if (list == null) {
				List<IDatum> newDatumlist = new LinkedList<IDatum>();
				newDatumlist.add((IDatum) datumRep.getDatum());
				childrenMap.put(stamp.getRelatedEntityType((IDatum) datumRep.getDatum()), newDatumlist);
			}
			else {
				list.add((IDatum) datumRep.getDatum());
			}
		}

		// Preserve the order in the tree.
		for (IDatum parentDatum : datumTreeToBeCopied.keySet()) {

			Map<IRelatedEntityType, List<IDatum>> childMap = datumTreeToBeCopied.get(parentDatum);

			for (IRelatedEntityType typeIter : childMap.keySet()) {

				Map<IRelatedEntityType, List<IDatum>> map = stamp.getAssociatedDatums(parentDatum);
				List<IDatum> origDatumOrder = map.get(typeIter);
				List<IDatum> toBeOrdered = childMap.get(typeIter);

				// The order is in this map - used to sort.
				Map<Integer, IDatum> tmpSortMap = new HashMap<Integer, IDatum>();
				for (IDatum datumOrdering : toBeOrdered) {
					int i = 0;
					for (IDatum datumOrdered : origDatumOrder) {
						if (datumOrdering == datumOrdered) {
							break;
						}
						i++;
					}

					tmpSortMap.put(i, datumOrdering);
				}
				Set<Integer> datumOrder = tmpSortMap.keySet();
				toBeOrdered.clear();
				List<Integer> orderLst =
						CollectionUtils.createSortedList(datumOrder.iterator(), new Comparator<Integer>()
						{
							public int compare(Integer o1, Integer o2)
							{
								return (o1.compareTo(o2));
							}
						});

				for (Integer datumPos : orderLst) {
					toBeOrdered.add(tmpSortMap.get(datumPos));
				}
				childMap.put(typeIter, toBeOrdered);
			}
		}

		// copy the datums
		for (IDatum parentDatum : datumTreeToBeCopied.keySet()) {
			Map<IRelatedEntityType, List<IDatum>> childMap = datumTreeToBeCopied.get(parentDatum);
			for (IRelatedEntityType typeIter : childMap.keySet()) {
				List<IDatum> datumsToBeCopied = childMap.get(typeIter);
				for (IDatum datumGettingCopied : datumsToBeCopied) {
					IDatumRepresentation datumRep =
							(IDatumRepresentation) (model.getDiagram().getRepresentation(datumGettingCopied));

					if (datumRep instanceof IGridDatumRepresentation) {
						continue;      // Do not replicate
					}

					List<IUIDObject> datumReps = copiedObjs(replicator, datumRep);

					for (IUIDObject uidObj : datumReps) {
						m_creationObjects.add(uidObj);
						newObjs.add(uidObj);
					}
				}
			}
		}
	}

	/**
	 * Given a UID object this will duplicate it using the replicator. This method is useful if there are more than one
	 * object replicated from a single source object. E.g: IRelatedEntityDatum: In this case, we copy the parent datum
	 * and all its decendents are also copied.
	 *
	 * @param replicator The utility object to replicate.
	 * @param origObject The original UID object
	 *
	 * @return A duplicate version of the object passed in.
	 */
	private List<IUIDObject> copiedObjs(Replicator replicator, IUIDObject origObject)
	{
		List<IUIDObject> copiedObjects = new LinkedList<IUIDObject>();

		if (origObject instanceof IDatumRepresentation) {
			Model model = ((Model) CAFUtils.getInstance().getActiveCapletController().getCapletModel());
			Map<IDatum, IDatum> localtRelationMap = new HashMap<IDatum, IDatum>();

			List<IDatumRepresentation> datumReps = replicator.replicate(model.getSymbolDef(), model.getDiagram(),
					(IDatumRepresentation) origObject, localtRelationMap, m_relatedEntityMap,
					m_attributeDatumParentMap);

			m_relatedDatumParentMap.putAll(localtRelationMap);

			for (Iterator<IDatumRepresentation> iter = datumReps.listIterator(); iter.hasNext(); ) {
				copiedObjects.add(iter.next());
			}
		}

		return copiedObjects;
	}

	/**
	 * Given a UID object this will duplicate it using the replicator.  This is really a convenience method designed to
	 * call the appropriate method on the replicator based on the given object type.
	 *
	 * @param replicator for replicating the object
	 * @param origObject The original UID object
	 * @param selections the selection set
	 *
	 * @return A duplicate version of the object passed in.
	 */
	@Nullable
	private IUIDObject copiedObj(Replicator replicator, IUIDObject origObject, SelectSet selections)
	{
		if (origObject instanceof IGfxGroupable && ((IGfxGroupable) origObject).isGrouped()) {
			// Grouped objects will be copied as members of the group so don't copy them here
			return null;
		}
		IUIDObject copiedObject = null;
		if (origObject instanceof IPropertiedGraphic) {
			copiedObject = replicator.replicatePropertiedGraphic((IPropertiedGraphic) origObject);
		}
		else if (origObject instanceof IGenericSchemPin) {
			copiedObject = replicator.replicate((IGenericSchemPin) origObject, m_pinHolder);
		}
		else if (origObject instanceof IAttributeText) {
			// if this name text belongs to a pin, don't replicate it
			IAttributeText nt = (IAttributeText) origObject;
			IAttributeProvider nobj = nt.getAttributeProvider();
			if (!(nobj instanceof IGenericPin) && !(nobj instanceof IInternalLink)) {
				copiedObject = replicator.replicate(nt);
			}
		}
		else if (origObject instanceof IXRefPlaceholder) {
			IXRefPlaceholder xrph = (IXRefPlaceholder) origObject;
			IDiagramObject dobj = xrph.getParent();
			if (!(dobj instanceof IGenericSchemPin)) {
				copiedObject = replicator.replicate(xrph);
			}
		}
		else if (origObject instanceof IPropText) {
			copiedObject = replicator.replicate((IPropText) origObject);
		}
		else if (origObject instanceof ISchemInternalLink) {
			copiedObject = replicator.replicate((ISchemInternalLink) origObject, true);
		}

		return copiedObject;
	}

	public boolean isObjectCopyable(IDiagramObject obj)
	{

		if (obj.getParent() instanceof IUserDefinedZone) {
			return false;
		}

		if (obj instanceof IDatumRepresentation) {
			return !(obj instanceof IGridDatumRepresentation);
		}

		IDiagramObject parentObj = obj.getParent();
		if (parentObj instanceof IPinList) {
			IPinList pl = (IPinList) parentObj;
			if (pl.isBlock()) {
				return false;
			}
		}
		if (obj instanceof IGenericSchemPin) {
			return true;
		}
		if (obj instanceof ISchemInternalLink) {
			return true;
		}
		// see if this is a border thingy that can't be copied
		else if (obj.isDerivedObject()) {
			return false;
		}
		else if (PrintRegionHelper.isPrintRegionRelatedObj(obj)) {
			return false;
		}
		else if (obj instanceof IInternalLinkPolyline && obj.getParent() instanceof ISchemInternalLink) {
			return false;
		}
		else if (obj instanceof IPropertiedGraphic && !(obj.getParent() instanceof IDatumRepresentation)) {
			return true;
		}
		else if (obj instanceof IAttributeText) {

			IAttributeText text = (IAttributeText) obj;
			IDiagramObject parent = text.getParent();
			return !(parent instanceof IDatumRepresentation);
		}
		else if (obj instanceof IXRefPlaceholder) {
			return true;
		}
		else if (obj instanceof IPropText) {
			// if the property belongs to a pin, it not copyabgle
			IPropText text = (IPropText) obj;
			IDiagramObject parent = text.getParent();
			return !(parent instanceof IGenericSchemPin);
		}

		return false;
	}

	private class NonPastableItemCollector implements INonPastableItemCollector
	{

		private Set<IUIDObject> m_objects = new LinkedHashSet<>();

		@Override public void collect(IUIDObject obj)
		{
			m_objects.add(obj);
		}

		private void processCollectedObjects()
		{
			for (IUIDObject uidObj : m_objects) {
				// Object is not pastable, so skip it!
				removeObject(uidObj);
			}
		}

		private boolean hasCollectedObjects()
		{
			return !m_objects.isEmpty();
		}
	}

	/**
	 * isObjectPastable - is the object pastable?
	 *
	 * @param obj - object being pasted
	 *
	 * @return - true if pastable, flase otherwise
	 */
	public boolean isObjectPastable(IDiagramObject obj)
	{
		NonPastableItemCollector nonPastableItemCollector = new NonPastableItemCollector();
		filterNonPastableItems(Collections.singletonList(obj), nonPastableItemCollector);
		return !nonPastableItemCollector.hasCollectedObjects();
	}

	private interface INonPastableItemCollector
	{

		void collect(IUIDObject obj);
	}

	private void filterNonPastableItems(List<IUIDObject> bufferCopy, INonPastableItemCollector collector)
	{
		filterGenericSchemPin(CollectionUtils.filterByClass(bufferCopy, IGenericSchemPin.class), collector);
		filterSchemInternalLink(CollectionUtils.filterByClass(bufferCopy, ISchemInternalLink.class), collector);
		filterDatumRepresentation(CollectionUtils.filterByClass(bufferCopy, IDatumRepresentation.class), collector);
		filterEmbeddedImages(CollectionUtils.filterByClass(bufferCopy, IPropertiedImage.class), collector);
		filterGroupsContainingEmbeddedImages(CollectionUtils.filterByClass(bufferCopy, IPropertiedGfxGroup.class),
				collector);
	}

	private void filterGroupsContainingEmbeddedImages(Collection<IPropertiedGfxGroup> gfxGroups,
			INonPastableItemCollector collector)
	{
		if (!isTargetCommentSymbol() && !isTargetBorder()) {
			for (IPropertiedGfxGroup group : gfxGroups) {
				if (gfxGroupContainsEmbeddedImage(group)) {
					collector.collect(group);
				}
			}
		}
	}

	private boolean gfxGroupContainsEmbeddedImage(IGfxGroup group)
	{
		Collection<IGfxObject> gfxObjects = group.getGfxObjects();
		for (IGfxObject gfxObject : gfxObjects) {
			if (gfxObject instanceof IGfxGroup) {
				if (gfxGroupContainsEmbeddedImage((IGfxGroup) gfxObject)) {
					return true;
				}
			}
			else if (gfxObject instanceof IPropertiedImage && isImageEmbedded((IPropertiedImage) gfxObject)) {
				return true;
			}
		}

		return false;
	}

	private void filterEmbeddedImages(Collection<IPropertiedImage> propertiedImages,
			INonPastableItemCollector collector)
	{
		if (!isTargetCommentSymbol() && !isTargetBorder()) {
			for (IPropertiedImage image : propertiedImages) {
				if (isImageEmbedded(image)) {
					collector.collect(image);
				}
			}
		}
	}

	private boolean isImageEmbedded(IPropertiedImage image)
	{
		IImageDataSource dataSource = image.getDataSource();
		return dataSource != null && dataSource.getType() != IImageDataSource.Type.LINK;
	}

	private void filterDatumRepresentation(Collection<IDatumRepresentation> iDatumRepresentations,
			INonPastableItemCollector collector)
	{
		for (IDatumRepresentation iDatumRepresentation : iDatumRepresentations) {
			ICapletView capView = CAFUtils.getInstance().getActiveCapletView();
			if (capView != null) {
				ICapletModel cm = capView.getCapletModel();
				if (cm instanceof Model) {
					Model m = (Model) cm;
					IStamp stamp = m.getSymbolDef();
					IBaseDatum datum = iDatumRepresentation.getDatum();
					if (datum != null && datum instanceof IEngineeringDatum && datum.getName() != null &&
							stamp instanceof ISymbolDef &&
							SymbolUtils.isCommentSymbol((ISymbolDef) stamp)) {
						String engineeringDatumType = datum.getName();
						Collection<IEngineeringDatum> iDatums = stamp.getAllEngineeringDatums();
						for (IEngineeringDatum idatum : iDatums) {
							if (idatum.getName() != null && engineeringDatumType.equalsIgnoreCase(
									idatum.getName())) {
								collector.collect(iDatumRepresentation);
							}
						}
					}
					boolean status = (stamp instanceof ISymbolDef && SymbolUtils.isCommentSymbol((ISymbolDef) stamp));
					if (!status) {
						collector.collect(iDatumRepresentation);
					}
				}
			}
		}
	}

	private void filterSchemInternalLink(Collection<ISchemInternalLink> iSchemInternalLinks,
			INonPastableItemCollector collector)
	{
		ISymbolDef symbolDef = getCurrentActiveSymbol();
		//internal link is only supported in Device symbols.
		if (!(symbolDef != null && SymbolUtils.isDeviceSymbol(symbolDef))) {
			for (ISchemInternalLink iSchemInternalLink : iSchemInternalLinks) {
				collector.collect(iSchemInternalLink);
			}
		}
	}

	private void filterGenericSchemPin(Collection<IGenericSchemPin> iGenericSchemPins,
			INonPastableItemCollector collector)
	{
		ISymbolDef symbolDef = getCurrentActiveSymbol();
		if (symbolDef == null) {
			for (IGenericSchemPin iGenericSchemPin : iGenericSchemPins) {
				collector.collect(iGenericSchemPin);
			}
			return;
		}

		//dts0100514065 User is able to copy pins into comment symbols
		//Comment symbols cannot have pins
		if (SymbolUtils.isCommentSymbol(symbolDef)) {
			for (IGenericSchemPin iGenericSchemPin : iGenericSchemPins) {
				collector.collect(iGenericSchemPin);
			}
			return;
		}

		if (!iGenericSchemPins.isEmpty()) {
			//Backshell symbols cannot have more than one pin
			if (SymbolUtils.isBackshellSymbol(symbolDef)) {
				Iterator<IGenericSchemPin> pinIterator = iGenericSchemPins.iterator();
				int existingNumPins = symbolDef.getNumPins();
				if (existingNumPins == 0) {
					pinIterator.next();
				}
				while (pinIterator.hasNext()) {
					collector.collect(pinIterator.next());
				}
			}
		}

		for (IGenericSchemPin pin : iGenericSchemPins) {
			//dts0100513789 Validation failure when copying a splice symbol contents to another symbol type
			//Splicepins cannot be added on devices and vice versa
			IGenericPin pinConnectivity = pin.getConnectivity();
			chs.cof.logical.cable.IPinList symbolConnectivity = symbolDef.getConnectivity();

			if (pinConnectivity instanceof ISplicePin && !(symbolConnectivity instanceof ISplice)) {
				LogHelper.debugMsg(
						ResourceMgr.getString(SymbolDataTransfer.class, "SymbolDataTransfer.Paste.SplicePin"));
				collector.collect(pin);
			}
			if ((pinConnectivity instanceof IDevicePin || pinConnectivity instanceof IInternalPin) &&
					!(symbolConnectivity instanceof IDevice)) {
				LogHelper.debugMsg(
						ResourceMgr.getString(SymbolDataTransfer.class, "SymbolDataTransfer.Paste.DevicePin"));
				collector.collect(pin);
			}
			if (pinConnectivity instanceof IFunctionPin && !(symbolConnectivity instanceof IFunction)) {
				LogHelper.debugMsg(
						ResourceMgr.getString(SymbolDataTransfer.class, "SymbolDataTransfer.Paste.FunctionPin"));
				collector.collect(pin);
			}
		}
	}

	@Nullable private ISymbolDef getCurrentActiveSymbol()
	{
		ICapletView capView = CAFUtils.getInstance().getActiveCapletView();

		Model m = getModel(capView);
		if (m != null) {
			IStamp stmp = m.getSymbolDef();

			if (stmp instanceof ISymbolDef) {
				return (ISymbolDef) stmp;
			}
		}
		return null;
	}

	@Nullable private Model getModel(ICapletView capView)
	{
		Model m = null;
		if (capView != null) {
			ICapletModel cm = capView.getCapletModel();

			if (cm instanceof Model) {
				m = (Model) cm;
			}
		}
		return m;
	}

	@Override public void resolveOwnerForAssociatedObjects(IDataTransferReplicator replicator, SelectSet curSels)
	{
	}

}
