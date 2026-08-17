/*
 * Copyright 2004-2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.IActionable;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.ControllerActionRT;
import chs.caf.caplet.helpers.MulticoreLibraryHelper;
import chs.caf.caplet.helpers.browser.IBrowserTreeContainer;
import chs.caf.caplet.helpers.browser.LogicBrowserTree;
import chs.caf.caplet.helpers.browser.PartBrowserActionHelper;
import chs.cof.logical.IDesign;
import chs.cof.logical.cable.IMulticore;
import chs.cof.parts.ILibraryMulticore;
import chs.cof.parts.Library;
import chs.cof.parts.LibraryCriteriaHelper;
import chs.cof.parts.configure.ConfigurationTypeEnum;
import chs.cof.parts.partselector.ILibraryPartSelection;
import chs.cof.parts.partselector.ILibraryPartSelector;
import chs.cof.parts.partselector.PartSelectionContext;
import chs.common.criteria.ICriteria;
import chs.utility.logic.ILogicModel;

import javax.swing.JComponent;
import java.awt.event.ActionEvent;

/**
 */
public class AddLibraryMulticoreAction extends ControllerActionRT
{

	protected ILibraryPartSelection m_libMulticore;
	private IDesign m_design;
	private IMulticore m_multicore;

	public AddLibraryMulticoreAction(ICapletController controller)
	{
		super(controller);
		m_design = ((ILogicModel) getController().getCapletModel()).getDesign();
	}

	protected IActionEnum onActivate(ActionEvent e)
	{
		m_libMulticore = null;
		m_libMulticore = PartBrowserActionHelper.getSelectedBrowserPart();
		if (m_libMulticore == null) {
			ICriteria<ILibraryMulticore> criteria = LibraryCriteriaHelper.createCriteria(ILibraryMulticore.class);
			ILibraryPartSelector partSelector = Library.getInstance()
					.getLibraryPartSelector(CAFUtils.getInstance().getWindowMgr().getDialogFrame());
			PartSelectionContext partSelectionContext = new PartSelectionContext();
			partSelectionContext.setSelectionFilter(LibraryCriteriaHelper.getSelectionFilterForNoSymbols(
					null, null, LibraryCriteriaHelper
							.getCustomerDetailsFromScopes(CAFUtils.getInstance().getActiveDesignContainer(),
									CAFUtils.getInstance().getCurrentProject())
			));

			//@todo used library configuration context directly, needs confirmation - kjuthi
			m_libMulticore = partSelector.selectPart(criteria, CAFUtils.getInstance().getCurrentProject(),
					partSelectionContext, ConfigurationTypeEnum.LOGICAL,
					CAFUtils.getInstance().getActiveDesignContainer());
		}

		return (m_libMulticore == null ? IActionEnum.eCanceled : IActionEnum.eCompleted);
	}

	protected boolean onTerminate(boolean successful)
	{
		if (successful) {
			enableDesignTreeExpansion(true);
			m_multicore = MulticoreLibraryHelper.createLibrariedMulticore(m_libMulticore, m_design);
		}
		return successful;
	}

	public boolean onPostTerminate(boolean onTerminateResult)
	{
		if (m_multicore != null) {
			IActionable logicBrowserTree = getController().getActionableBrowser("Diagram");
			if (logicBrowserTree instanceof LogicBrowserTree) {
				((LogicBrowserTree) logicBrowserTree).setObjectSelected(m_multicore.getUID());
			}
			enableDesignTreeExpansion(false);
		}
		return true;
	}

	public String getActionUIClass()
	{
		return AddLibraryMulticoreActionUI.class.getName();
	}

	private void enableDesignTreeExpansion(boolean enabled)
	{
		JComponent browser = getController().getBrowser();
		if (browser instanceof IBrowserTreeContainer) {
			((IBrowserTreeContainer) browser).setHomeTreeExpansionEnabled(enabled);
		}
	}
}
