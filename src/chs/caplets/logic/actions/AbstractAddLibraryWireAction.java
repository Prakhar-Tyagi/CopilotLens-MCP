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

import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.COFTypeEnum;
import chs.cof.draw.IGfxObject;
import chs.cof.logical.ILogicDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IInterconnectSourceInfo;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.INetConductor;
import chs.cof.logical.cable.IWireConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.logical.shared.ISharedConductor;
import chs.cofUtils.logical.concurrency.LogicConcurrencyLogger;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.services.dynamicgfx.ISmartPoint;
import chs.utilities.ResourceMgr;
import chs.utility.Placement;
import chs.utility.helpers.ConductorHelper;
import chs.utility.helpers.LogicObjectLockFinder;
import chs.utility.helpers.SharedConductorHelper;
import chs.utility.logic.ILogicModel;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public abstract class AbstractAddLibraryWireAction extends CreateWireAction
{

	protected AddLibraryInnercoreActionHelper m_helper;

	private ISpecialSelection m_specialSelectMgr;

	private IMulticore m_multicore;

	protected AbstractAddLibraryWireAction(ICapletController controller, ISpecialSelection libSelectMgr,
			Class conductorClass)
	{
		super(controller);

		m_specialSelectMgr = libSelectMgr;
		m_helper = getAddLibraryInnercoireActionHelper(m_specialSelectMgr, conductorClass);
		if (IWireConductor.class.isAssignableFrom(conductorClass)) {
			super.setConductorType(CONDUCTOR_TYPE.ADD_WIRE);
		}
		else if (INetConductor.class.isAssignableFrom(conductorClass)) {
			super.setConductorType(CONDUCTOR_TYPE.ADD_NET);
		}
	}

	protected AddLibraryInnercoreActionHelper getAddLibraryInnercoireActionHelper(ISpecialSelection specialSelection,
			Class conductorClass)
	{
		return new AddLibraryInnercoreActionHelper(specialSelection, getLogicModel().getDesign(), conductorClass);
	}

	protected ILogicModel getLogicModel()
	{
		return (ILogicModel) getModel();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (m_helper.getInnercore() == null && m_helper.getLibraryWire() == null) {
			return IActionEnum.eCanceled;
		}

		IActionEnum ae = super.onActivate(e);
		m_libWire = null;
		IConductor existingCableConductor = m_helper.getExistingConductor();
		if (existingCableConductor != null && !lockObject(existingCableConductor, getLogicModel().getDesign())) {
			return IActionEnum.eCanceled;
		}
		if (existingCableConductor != null) {
			getCommand().setCableConductor(existingCableConductor);
		}
		else {
			if (m_helper.getAncestors() != null) {

				if (!lockRequiredObjects()) {

					return IActionEnum.eCanceled;
				}

				if (!m_helper.checkLibraryInnercoreAvailability(getObjectType().name())) {
					return IActionEnum.eCanceled;
				}
			}
		}
		return ae;
	}

	private boolean lockRequiredObjects()
	{
		return m_helper.lockNearestParentMulticore(getLogicModel().getDesign(), getLockErrorPrefix(),
				message -> getOutputWindow().sendApplicationMessage(message), getObjectType().name());
	}

	private boolean lockObject(IUIDObject multicoreToBeLocked, ILogicDesign logicDesign)
	{
		Set<IUID> failedObjects =
				LogicObjectLockFinder.tryEdit(logicDesign, Collections.singleton(multicoreToBeLocked));
		if (!failedObjects.isEmpty()) {
			LogicConcurrencyLogger.getInstance()
					.reportLockFailure(logicDesign, getLockErrorPrefix(), failedObjects,
							message -> getOutputWindow().sendApplicationMessage(message));
			return false;
		}
		return true;
	}

	private IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	private String getLockErrorPrefix()
	{
		return ResourceMgr
				.getString(AbstractAddLibraryWireAction.class, "LogicAction.error.unableToLock",
						getActionDisplayName());
	}

	private String getActionDisplayName()
	{
		return (String) getActionUI().getValue(Action.NAME);
	}

	abstract COFTypeEnum getObjectType();

	// ToDo - This has some functionality in common with AddLibraryInnercoreShieldAction.onTerminate(). Should
	// ToDo       consider if it's worthwhile to refactor some sections into common methods and push down into
	// ToDo       AddLibraryInnercoreActionHelper
	@Override protected IGfxObject constructDisplayObject(List<ISmartPoint> point_list)
	{
		chs.cof.logical.schem.IConductor schemCond =
				(chs.cof.logical.schem.IConductor) super.constructDisplayObject(point_list);
		IConductor cableCond = schemCond.getConnectivity();

		if (m_helper.getLibraryWire() != null) {
			m_multicore = null;
			cableCond.assignLibraryPart(m_helper.getLibraryWire());
		}
		else if (m_helper.getInnercore() != null) {
			m_multicore = m_helper.produceMulticore(getLogicModel().getDiagram());

			if (m_helper.getSharedConductor() != null) {
				cableCond.setSharedConductor(m_helper.getSharedConductor());
			}

			ISharedConductor sharedConductor = cableCond.getSharedConductor();
			if (sharedConductor != null) {
				SharedConductorHelper.assignToShared(schemCond, sharedConductor,
						getLogicModel().getDesign(), getLogicModel().getDiagram());
			}
			if (m_helper.getExistingConductor() == null) {
				m_multicore.addConductor(cableCond);
				cableCond.setLibraryRef(m_helper.getInnercore().getUID());
				MulticoreLibraryHelper.addInnerCoreProperties(cableCond, m_helper.getInnercore());
			}
			if (sharedConductor != null) {
				sharedConductor.flush();
			}

			// Make sure all the ancestor multicores have indicators. If they already have indicators, this loop adds
			// nothing (see code for Placement.placeIndicators)
			ISchemDiagram diagram = getLogicModel().getDiagram();
			Placement.populateIndicators(diagram, m_multicore);

			// A wire might now belong a to multicore with part number. We have to style it
			//dts0100959720
			schemCond.forceApplyStyle();
		}

		IInterconnectSourceInfo sourceInfo = getLogicModel().getDesign().getInterconnectSourceInfo();
		if (sourceInfo != null) {
			if (m_helper.getToDoItem() != null && m_helper.getLibraryWire() != null) { // Don't do this for innercores
				sourceInfo.addConductorDerivation(m_helper.getToDoItem(), cableCond);
				cableCond.setHarness(sourceInfo.getHarness(m_helper.getToDoItem()));
			}
			else {
				IMulticore topMC = ConductorHelper.findOutermostMulticore(cableCond);
				if (topMC != null) {
					if (sourceInfo.getToDoItem(topMC) != null) {
						cableCond.setHarness(topMC.getHarness());
					}
				}
			}
		}
		return schemCond;
	}

	public boolean isEnabled()
	{
		return super.isEnabled() && m_helper.getOperands();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}
}
