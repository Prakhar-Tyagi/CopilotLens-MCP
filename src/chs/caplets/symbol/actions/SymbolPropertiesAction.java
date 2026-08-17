/*
 * Copyright 2010 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol.actions;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.IPropertiesClient;
import chs.caf.caplet.helpers.PropertiesAction;
import chs.cof.symbol.IAbstractLibrary;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.SymbolScaleTypeEnum;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.PROPERTY_CREATION_STRATEGY;
import chs.common.attr.IAttributeProvider;
import chs.ctf.caf.ui.TextAttributesEditor;
import chs.ctf.caf.utils.SymbolLibraryLockRefreshHelper;
import chs.system.FactoryMgr;
import chs.utility.logic.ISymbolModel;
import chs.utility.symbol.LibraryLockRefreshStatus;
import chs.utility.ui.SymbolErrorDialog;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;


public class SymbolPropertiesAction extends PropertiesAction
{

	private ISymbolModel m_symModel;

	public SymbolPropertiesAction(ICapletController controller, IPropertiesClient client)
	{
		super(controller, client);
		m_symModel = (ISymbolModel) controller.getCapletModel();
	}

	public SymbolPropertiesAction(ICapletController controller, IPropertiesClient client,
			TextAttributesEditor textAttrEditor)
	{
		super(controller, client, textAttrEditor);
		m_symModel = (ISymbolModel) controller.getCapletModel();
	}

	public String getActionUIClass()
	{
		return SymbolPropertiesActionUI.class.getName();
	}

	public IActionEnum onActivate(ActionEvent e)
	{
		FactoryMgr.getCommonFactory().setPropertyStrategy(PROPERTY_CREATION_STRATEGY.NonOTIStrategy);
		IStamp symbol = m_symModel.getSymbolDef();
		if (symbol == null) {
			return IActionEnum.eCanceled;
		}

		IAbstractLibrary library = symbol.getContainerLibrary();
		final LibraryLockRefreshStatus refreshStatus =
				SymbolLibraryLockRefreshHelper.refreshAndCheckExistence(library);
		if (refreshStatus.isSuccessful()) {
			return super.onActivate(e);
		}
		else {
			SymbolErrorDialog.showErrorDialog(library, library.getType(),
					SymbolErrorDialog.UserAction.PropertiesAction, refreshStatus);
			return IActionEnum.eCanceled;
		}
	}

	public boolean showAllUnitsInUnitsCombo()
	{
		//for any "logical" symbol and border, show all physical units in the combo.
		IStamp stamp = m_symModel.getSymbolDef();
		//return is(border || "logical" symbol)
		if (stamp instanceof IBorder) {
			return true;
		}
		else {
			SymbolScaleTypeEnum symbolScaleType = ((ISymbolDef) stamp).getSymbolScaleType();
			return symbolScaleType == null || symbolScaleType == SymbolScaleTypeEnum.PinGridScale;
		}
	}

	private boolean areAllCorrectAttributeProviders(Set<IAttributeProvider> attributeProviders)
	{
		return attributeProviders.stream().noneMatch(attributeProvider -> attributeProvider instanceof IBlock &&
				((ISymbolDef) attributeProvider).getSymbolType().equals(SymbolTypeEnum.FUNCTION));
	}

	// Create all the components on the General tab of the Properties UI
	@Nullable protected JPanel createGeneralTab(List<PanelWrapper> panels, JTabbedPane tabbedPane)
	{
		Set<IAttributeProvider> attributeProviders =
				m_client.getPropertiedSet().getFilteredObjects(IAttributeProvider.class);
		if (!areAllCorrectAttributeProviders(attributeProviders)) {
			return null;
		}

		return super.createGeneralTab(panels, tabbedPane);
	}
	@Override public boolean onTerminate(boolean successful)
	{
		boolean terminated = super.onTerminate(successful);
		FactoryMgr.getCommonFactory().setPropertyStrategy(PROPERTY_CREATION_STRATEGY.Default);
		return terminated;
	}
}
