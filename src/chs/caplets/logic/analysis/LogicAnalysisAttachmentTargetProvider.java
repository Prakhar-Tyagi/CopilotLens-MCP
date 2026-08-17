/*
 * Copyright 2004-2008 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.analysis;

import chs.analysis.AnalysisServices;
import chs.analysis.IAnalysisAttachmentTargetProvider;
import chs.caf.CAFUtils;
import chs.caf.caplet.selection.ISelectMgr;
import chs.caf.caplet.selection.SelectSet;
import chs.caf.caplet.selection.SelectedUIDObjectIterator;
import chs.caf.caplet.selection.SelectionFilter;
import chs.caplets.logic.Model;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnector;
import chs.cof.logical.cable.IDevice;
import chs.cof.logical.cable.ILogicObject;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IPinList;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.cable.ISplice;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.project.IProject;
import chs.common.IUIDObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rharring
 */
public class LogicAnalysisAttachmentTargetProvider implements IAnalysisAttachmentTargetProvider
{

	/**
	 * The model
	 */
	protected Model m_model;

	/**
	 * Creates a new instance of LogicAnalysisAttachmentTargetProvider
	 */
	public LogicAnalysisAttachmentTargetProvider(Model model)
	{
		m_model = model;
	}

	/**
	 * This method gets a collection containing the objects ( which should be IAnalysable ) which this attachment
	 * operation targets, typically the selection.
	 * <p>
	 * These targets may be multiple instances of the same component / device or a single instance. Mixtures of
	 * instances should NOT be provided.
	 *
	 * @return Collection, the objects selected.
	 */
	public Collection getTargets()
	{
		ArrayList v = new ArrayList();
		Map backmap = new HashMap();
		// Get the pre selections to see if there is exactly on to work with
		ISelectMgr selectMgr = CAFUtils.getInstance().getActiveSelectMgr();
		if (selectMgr == null) {
			return v;
		}

		/*
		 dts0100596138 : filter the selection to to consider only Rpresented Object type
		 				 selections. (leaving apart the other selection objects )
		 */
		SelectSet unfilteredSelections = selectMgr.getPreSelections();
		SelectSet selections = new SelectSet();
		SelectionFilter filteredSelections = new SelectionFilter(IRepresentedObject.class);
		selections.setSelectionFilter(filteredSelections);
		selections.setSelections(unfilteredSelections);

		if (selections.getSelectCount() == 0) {
			return v;
		}
		else if (selections.getSelectCount() == 1) {
			// If we have exactly one object selected, then see if it is one
			// we care about and if so make sure it already has an analysis
			// model associated with it.
			IUIDObject uidObj = selections.getSelectedUIDObjects().getNext();

			if (uidObj instanceof IRepresentedObject) {
				IRepresentedObject repObj = (IRepresentedObject) uidObj;

				IUIDObject connObj = repObj.getRawConnectivity();
				if (connObj instanceof ILogicObject) {
					ILogicObject logicObj = (ILogicObject) connObj;

					if (logicObj instanceof IDevice ||
							(logicObj instanceof IConnector) ||
							(logicObj instanceof ISplice) ||
							(logicObj instanceof IWireConductor) ||
							(logicObj instanceof IShieldConductor)
							) {
						v.add(logicObj);
						return v;
					}
				}
			}
		}
		else {
			// There are multiple objects selected.  See if they are all either conductors
			// or pin lists with the same symbol reference.  The first element into the selected
			// objects is what all other objects will compare against.
			for (SelectedUIDObjectIterator sit = selections.getSelectedUIDObjects(); sit.hasNext(); ) {
				IUIDObject obj = sit.getNext();
				if (obj instanceof chs.cof.logical.schem.IConductor) {
					chs.cof.logical.schem.IConductor schemConductor = (chs.cof.logical.schem.IConductor) obj;
					ILogicObject logicObject = (ILogicObject) schemConductor.getRawConnectivity();

					// We don't allow model attachment to Nets, so make sure we
					// don't have a net.
					if (logicObject instanceof INetConductor) {
						v.clear();
						return v;
					}

					if (v.isEmpty() || v.get(0) instanceof IConductor) {
						// This is the first one in so just add it
						v.add(logicObject);
					}
					else {
						// Not a match and there is already something in, so get out
						// since there is a mix of objects
						v.clear();
						return v;
					}
				}
				else if (obj instanceof chs.cof.logical.schem.IPinList) {
					chs.cof.logical.schem.IPinList schemPinList = (chs.cof.logical.schem.IPinList) obj;
					IPinList pinList = (IPinList) schemPinList.getRawConnectivity();
					if (v.isEmpty()) {
						// If this pinList has a symbol then it has potential, but if not then
						// we don't support multiple attachment.
						if (schemPinList.getSymbolRef() != null) {
							// This is the first one in so just add it
							v.add(pinList);
							backmap.put(pinList, schemPinList);
						}
						else {
							v.clear();
							return v;
						}
					}
					else if (v.get(0) instanceof IPinList) {
						// See if the pin lists have the same symbol
						IPinList firstPL = (IPinList) v.get(0);
						chs.cof.logical.schem.IPinList firstSchemPinList =
								(chs.cof.logical.schem.IPinList) backmap.get(firstPL);
						if (schemPinList.getSymbolRef() != null && (schemPinList.getSymbolRef().getSymbolUID() ==
								firstSchemPinList.getSymbolRef().getSymbolUID())) {
							// We have a match, so put it in
							v.add(pinList);
							backmap.put(pinList, schemPinList);
						}
						else {
							// Symbols don't match, so get out
							v.clear();
							return v;
						}
					}
					else {
						// Not a match and there is already something in, so get out
						// since there is a mix of objects
						v.clear();
						return v;
					}
				}
			}
		}

		return v;
	}

	/**
	 * This method gets a string representation of the design's uid
	 *
	 * @return String, the design's uid.
	 */
	public String getUID()
	{
		return m_model.getDesign().getUID().toString();
	}

	/**
	 * This method returns the id attached to the design's project.
	 *
	 * @return int, the subsystemid attached to the project
	 */
	public int getProjectSubsystemId()
	{
		return AnalysisServices.getSubsystemProjectId(getProject());
	}

	/**
	 * This method gets the symbol definition of the given targets. This should return null to disable symbol
	 * attachment.
	 *
	 * @param v - the selected targets
	 *
	 * @return IUIDObject, a fullyLoaded ISymbolDef or null to indicate no symbol attachment possible
	 */
	public IUIDObject getSymbolTarget(Collection v)
	{
		// We never want logic to be able to attach to a symbol...
		return null;
	}

	@Override public boolean isReadOnlyTarget(IUIDObject object)
	{
		if (object instanceof ILogicObject) {
			ILogicObject logicObject = (ILogicObject) object;
			if (logicObject.getSharedObject() != null || !logicObject.getDesignContainer().isEditable()) {
				return true;
			}
		}
		return false;
	}

	@Override public boolean isPromotionTabEnabled()
	{
		return true;
	}

	@Nullable @Override public IProject getProject()
	{
		return m_model.getDesign() != null ? m_model.getDesign().getProject() : null;
	}

	public boolean doesContainerSupportActionInMUMode()
	{

		ILogicDesign design = m_model.getDesign();
		if (design != null) {
			return !design.isUnderConcurrentEdit();
		}
		return true;
	}

	@Nullable @Override public String getDisabledTooltip()
	{
		return null;
	}
}
