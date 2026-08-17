/*
 * Copyright 2004-2012 Mentor Graphics Corporation
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
import chs.caf.ICtxMenuProvider;
import chs.caf.IOutputWindow;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ISpecialSelection;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.logic.Model;
import chs.cof.COFTypeEnum;
import chs.cof.drawplus.IDiagramObject;
import chs.cof.drawplus.IDiagramObjectIterator;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IConductor;
import chs.cof.logical.cable.IConnectivity;
import chs.cof.logical.cable.IInterconnectToDoItem;
import chs.cof.logical.cable.IMulticore;
import chs.cof.logical.cable.IShieldBody;
import chs.cof.logical.cable.IShieldConductor;
import chs.cof.logical.schem.ISchemDiagram;
import chs.cof.parts.ILibraryInnerCore;
import chs.cofUtils.parameterized.Generator;
import chs.cofUtils.parameterized.GeneratorParameters;
import chs.cofUtils.parameterized.IndicatorHelper;
import chs.common.IReadOnlyNamedObject;
import chs.system.FactoryMgr;
import chs.utilities.ResourceMgr;
import chs.utilities.ui.messaging.Message;
import chs.utilities.ui.messaging.PromptSeverity;
import chs.utilities.ui.messaging.ResourceBasedMessageContent;
import chs.utility.Placement;
import chs.utility.helpers.ConductorHelper;
import chs.utility.logic.LogicObjectUtils;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Mar 4, 2004 Time: 4:32:36 PM To change this template use Options |
 * File Templates.
 */
public class AddLibraryInnercoreShieldAction extends ControllerActionRT implements ICtxMenuProvider
{

	private ISpecialSelection m_libSelectMgr;
	private AddLibraryInnercoreActionHelper m_helper;
	private IDesign m_design = null;
	private IConnectivity m_connectivity = null;
	private Generator m_generator = null;

	public AddLibraryInnercoreShieldAction(ICapletController controller, ISpecialSelection libSelectMgr)
	{
		super(controller);

		m_design = getModel().getDesign();
		m_connectivity = m_design.getConnectivity();
		m_libSelectMgr = libSelectMgr;
		m_generator = Generator.getGenerator();
		m_helper = new AddLibraryInnercoreActionHelper(m_libSelectMgr, getModel().getDesign(), IShieldConductor.class);
	}

	protected ISpecialSelection getSelection()
	{
		return m_libSelectMgr;
	}

	protected Model getModel()
	{
		return (Model) getController().getCapletModel();
	}

	public void convertToShield(ILibraryInnerCore wire, List<IMulticore> ancestors)
	{
		m_helper.prepareForInnercoreWire(wire, ancestors);
		doAdd();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		if (m_helper.getInnercore() == null) {
			return IActionEnum.eCanceled;
		}
		if (!lockRequiredObjects()) {
			return IActionEnum.eCanceled;
		}

		if (isShieldAlreadyAdded()) {
			return IActionEnum.eCanceled;
		}

		if (!m_helper.checkLibraryInnercoreAvailability(getActionDisplayName())) {
			return IActionEnum.eCanceled;
		}
		return IActionEnum.eCompleted;
	}

	private boolean lockRequiredObjects()
	{
		return m_helper.lockNearestParentMulticore(getModel().getDesign(), getLockErrorPrefix(),
				message -> getOutputWindow().sendApplicationMessage(message), COFTypeEnum.Shield.name());
	}

	private boolean isShieldAlreadyAdded()
	{
		Object ancestor = m_helper.getAncestors().get(0);
		if (ancestor instanceof IMulticore) {
			IMulticore multicore = (IMulticore) ancestor;
			if (multicore.getShield() != null) {
				displayMessageForAlreadyPlacedByOtherUser(multicore.getShield());
				return true;
			}
		}
		return false;
	}

	private void displayMessageForAlreadyPlacedByOtherUser(IConductor conductor)
	{
		ResourceBasedMessageContent content =
				new ResourceBasedMessageContent(AddLibraryInnercoreShieldAction.class,
						"AddLibraryInnercoreShieldAction.shieldAlreadyAdded");
		String objectType = COFTypeEnum.Shield.name();
		content.setContextParameters(objectType);
		content.setMessageParameters(objectType.toLowerCase(), getMulticoreName());
		content.setImplicationsParameters(conductor.getName(), getMulticoreName());
		content.setGuidanceParameters(conductor.getName());
		showWarningDialog(content);
	}

	protected void showWarningDialog(ResourceBasedMessageContent content)
	{
		Message.show(PromptSeverity.WARNING, content);
	}

	private String getMulticoreName()
	{
		Object ancestor = m_helper.getAncestors().get(0);
		if (ancestor instanceof IReadOnlyNamedObject) {
			return ((IReadOnlyNamedObject) ancestor).getName();
		}
		return "";
	}

	private String getLockErrorPrefix()
	{
		return ResourceMgr.getString(AddLibraryInnercoreShieldAction.class,
				"LogicAction.error.unableToLock", getActionDisplayName());
	}

	private IOutputWindow getOutputWindow()
	{
		return CAFUtils.getInstance().getOutputWindow();
	}

	private String getActionDisplayName()
	{
		return (String) getActionUI().getValue(Action.NAME);
	}

	// ToDo - This has some functionality in common with AbstractAddLibraryWireAction.constructDisplayObject(). Should
	// ToDo       consider if it's worthwhile to refactor some sections into common methods and push down into
	// ToDo       AddLibraryInnercoreActionHelper
	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			return doAdd();
		}
		return successful;
	}

	private boolean doAdd()
	{
		//dts0100698884 - Shield does not get hooked to multicore indicator in second instance
		ISchemDiagram diagram = getModel().getDiagram();

		IMulticore parentMC = m_helper.produceMulticore(diagram);
		if (parentMC.getShield() != null) {
			return false;
		}
		// Make a shield.
		IShieldConductor shield = FactoryMgr.getCablePropertiedFactory().createShieldConductor(FactoryMgr.createUID());
		if (m_helper.getLibraryWire() != null) {
			shield.assignLibraryPart(m_helper.getLibraryWire());
		}
		else if (m_helper.getInnercore() != null) {
			shield.setLibraryRef(m_helper.getInnercore().getUID());
			MulticoreLibraryHelper.addInnerCoreProperties(shield, m_helper.getInnercore());
		}
		if (m_helper.getToDoItem() != null && m_helper.getLibraryWire() != null) { // Don't do this for innercores
			m_design.getInterconnectSourceInfo().addConductorDerivation(m_helper.getToDoItem(), shield);
		}
		else {
			IMulticore topMC = ConductorHelper.findOutermostMulticore(parentMC);
			if (topMC != null) {
				IInterconnectToDoItem item = m_design.getInterconnectSourceInfo().getToDoItem(topMC);
				if (item != null) {
					shield.setHarness(topMC.getHarness());
				}
			}
		}
		if (parentMC.getSharedMulticore() != null) {
			shield.setSharedConductor(parentMC.getSharedMulticore().getShield());
		}

		// If the parent multicore doesn't already have a shield indicator, make one.
		// ToDo - Is this necessary? Has m_helper.produceMulticore() taken care of this already?
		IShieldBody shieldBody = parentMC.getShieldBody();
		boolean newIndicator = false;
		if (shieldBody == null) {
			shieldBody = LogicObjectUtils.createCableShieldBody(false, parentMC);
			newIndicator = true;
		}
		if (!IndicatorHelper.isShieldIndicator(parentMC.getIndicatorType())) {
			shieldBody.setType(IndicatorHelper.getDefaultShieldIndicatorType());
		}

		// Associate the shield and oval indicator.
		m_connectivity.addShieldConductor(shield);
		parentMC.setShield(shield);

		// If there are any wires drawn for this multicore, we need to deal with schem
		if (AddLibraryInnercoreActionHelper.thresholdedPlacedWireCount(parentMC, getModel().getDiagram(), 1) == 1) {
			GeneratorParameters parameters =
					createGeneratorParameters(getModel().getDiagram().getGrid().getGridSpacing());
			if (newIndicator) {
				// We just created the shield indicator. Make schem indicators and place them.
				Generator gen = Generator.getGenerator();
				Placement.placeIndicators(gen, diagram, parentMC, shieldBody, parameters, true, true);
			}
			else {
				// We associated the new shield with an existing shield indicator. Add hookups to any existing
				// schem indicators and redraw.
				for (IDiagramObjectIterator soitr = diagram.getRepresentations(shieldBody.getUID());
						soitr.hasNext(); ) {
					IDiagramObject so = soitr.getNext();
					if (so instanceof chs.cof.logical.schem.IShieldBody) {
						chs.cof.logical.schem.IShieldBody sb = (chs.cof.logical.schem.IShieldBody) so;
						// NOTE: createShieldBodyHookups only creates them IF THEY DON'T ALREADY EXIST!
						sb.createShieldBodyHookups(diagram);
						m_generator.generateShieldBody(sb, parameters);
					}
				}
			}
		}
		boolean successful = true;
		return successful;
	}

	protected GeneratorParameters createGeneratorParameters(int gridSpacing)
	{
		return new GeneratorParameters(gridSpacing);
	}

	public boolean isEnabled()
	{
		return getController().getCapletModel().isEditable() && m_helper.getOperands() && super.isEnabled();
	}

	public void populateCtxMenu(ActionContainer container, SelectSet selections)
	{
		if (isEnabled()) {
			container.add(new ActionEntry(getActionUI()));
		}
	}

	public String getActionUIClass()
	{
		return AddLibraryInnercoreShieldActionUI.class.getName();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
	}
}
