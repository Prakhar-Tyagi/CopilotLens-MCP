/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2023 Siemens
 */
package chs.caplets.border.properties;

import chs.caf.CAFUtils;
import chs.caf.caplet.IPropertiedSet;
import chs.caf.caplet.IPropertiesClientComponent;
import chs.caf.caplet.selection.SelectSet;
import chs.caplets.symbol.Model;
import chs.caplets.symbol.properties.PropertiesClient;
import chs.cof.draw.IGfxAttribute;
import chs.cof.drawplus.IBorderHolder;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.shared.ISharedObject;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IUserDefinedZone;
import chs.cof.symbol.IZoneArea;
import chs.cof.symbol.IZoneAreaObject;
import chs.cof.symbol.IZoneExtent;
import chs.common.IExtent;
import chs.common.IFormboardRegionDatum;
import chs.common.IGenericDatum;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.IUnit;
import chs.common.UnitTypeEnum;
import chs.ctf.editui.IAttributesClient;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.system.UIDMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.helpers.UnitHelper;
import chs.utility.logic.ISymbolModel;
import chs.utility.ui.IValidityListener;
import chs.utility.ui.PaperSizeChooserPanel;
import chs.utility.ui.UsableAreaChooserPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Point;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class BorderPropertiesClient extends PropertiesClient
{

	public BorderPropertiesClient(Model model)
	{
		super(model);
		m_clientComponents.add(new BorderInfoControls());
	}

	@Override protected void initDefaultPropertiedSet()
	{
		IUserDefinedZone userDefinedZone = findUserDefinedZone();
		if (userDefinedZone != null) {
			doInitPropertiedSet(userDefinedZone);
		}
		else {
			super.initDefaultPropertiedSet();
		}
	}

	@Nullable private IUserDefinedZone findUserDefinedZone()
	{
		GfxView gfxView = CommonUtils.cast(CAFUtils.getInstance().getActiveCapletView(), GfxView.class);
		Point location = gfxView != null ? gfxView.getCurrentMouseLocation() : null;
		ISymbolModel model = CommonUtils.cast(getModel(), ISymbolModel.class);
		IZoneAreaObject border = CommonUtils.cast(model != null ? model.getSymbolDef() : null, IZoneAreaObject.class);
		IZoneArea zoneArea = border != null ? border.getZoneArea() : null;
		IZoneExtent zoneExtent = (zoneArea != null && location != null) ? zoneArea.findZoneExtent(location) : null;
		return zoneExtent != null ? zoneExtent.getUserDefined() : null;
	}

	public void stopEditingProperties(boolean successful)
	{
		super.stopEditingProperties(successful);
		CAFUtils.getInstance().getActiveCapletView().invalidate(IViewInvalidationEnum.eFull);
	}

	@Override protected boolean isNameChangeAllowed(@NotNull SelectSet selections)
	{
		IUIDObject singleSelectedUIDObject = selections.getSingleSelectedUIDObject();
		return singleSelectedUIDObject instanceof IDatumRepresentation ||
				singleSelectedUIDObject instanceof IUserDefinedZone;
	}

	@Override
	@Nullable public IAttributesClient getAttributesClient()
	{
		if (disableForBorderDatums(m_propertiedSet.getNamedObjects(), m_propertiedSet.getNamedObjOwners())) {
			return null;
		}
		return super.getAttributesClient();
	}

	private boolean disableForBorderDatums(Set<? extends IReadOnlyNamedObject> namedObjects,
			Set<IRepresentedObject> representedObjects)
	{
		boolean disabled = namedObjects.size() != 1 || representedObjects.size() != 1;
		if (!disabled) {
			IRepresentedObject representedObject = representedObjects.iterator().next();
			IReadOnlyNamedObject namedObject = namedObjects.iterator().next();
			disabled = namedObject instanceof IFormboardRegionDatum || namedObject instanceof IGenericDatum
					|| representedObject instanceof IUserDefinedZone;
		}
		return disabled;
	}

	@Override public UnitTypeEnum getDistanceUnit()
	{
		IGfxAttribute gfxAttribute = getGfxAttribute();
		assert gfxAttribute != null;
		return gfxAttribute.getThickness().getUnit();
	}

	private static class BorderInfoControls implements IPropertiesClientComponent
	{

		private static final String TAB_LABEL =
				ResourceMgr.getString(BorderPropertiesClient.class, "PropertiesClient.BorderSize.Tab.Label");

		private IBorder m_border = null;
		private PaperSizeChooserPanel m_paperSizeChooser;
		private UsableAreaChooserPanel m_usableAreaChooser;

		private BorderInfoControls()
		{
		}

		public String getTabName(IPropertiedSet propset)
		{
			return TAB_LABEL;
		}

		/**
		 * Given a particular "PropertiedSet" object, checks to see if it can operate on the set.
		 */
		public boolean acceptsSet(IPropertiedSet propset)
		{
			boolean accept = true;
			for (Iterator<IUID> iter = propset.iterator(); iter.hasNext(); ) {
				IUID uid = iter.next();
				IUIDObject obj = UIDMgr.getObject(uid);
				if (obj instanceof IBorderHolder) {
					m_border = ((IBorderHolder) obj).getOwner();
				}
				else {
					accept = false;
				}
			}
			return accept;
		}

		public boolean modifiesSet(IPropertiedSet propset)
		{
			return acceptsSet(propset);
		}

		/**
		 * Creates a panel for displaying the information
		 */
		public JPanel getWidget(IPropertiedSet propset)
		{
			JPanel main = new JPanel();
			main.setLayout(new FlowLayout());

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			m_paperSizeChooser = new PaperSizeChooserPanel(m_border);
			panel.add(m_paperSizeChooser);

			m_usableAreaChooser = new UsableAreaChooserPanel(m_border);
			panel.add(m_usableAreaChooser);

			main.add(panel);

			return main;
		}

		/**
		 * Returns true - this component should have it's own tab
		 */
		public boolean isPropPage()
		{
			return true;
		}

		/**
		 * Make necessary data changes
		 */
		public void edit(IPropertiedSet propset)
		{
			IUnit page = m_border.getGrid().getRealMapping();
			IUnit w = m_paperSizeChooser.getWidthField().getUnit(FactoryMgr.getCommonFactory().createUnit());
			IUnit h = m_paperSizeChooser.getHeightField().getUnit(FactoryMgr.getCommonFactory().createUnit());

			int width = CommonUtils.toInteger(Math.round(w.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters()));
			int height = CommonUtils.toInteger(Math.round(h.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters()));

			IExtent ext = m_border.getPaperSize();
			assert ext != null;
			ext.setX(-width / 2);
			ext.setY(-height / 2);
			ext.setWidth(width);
			ext.setHeight(height);
			m_border.setPaperSize(ext);

			// Next, The Usable Area.
			IUnit uax = m_usableAreaChooser.getXField().getUnit(FactoryMgr.getCommonFactory().createUnit());
			IUnit uay = m_usableAreaChooser.getYField().getUnit(FactoryMgr.getCommonFactory().createUnit());
			IUnit uaw = m_usableAreaChooser.getWidthField().getUnit(FactoryMgr.getCommonFactory().createUnit());
			IUnit uah = m_usableAreaChooser.getHeightField().getUnit(FactoryMgr.getCommonFactory().createUnit());

			int xscale = CommonUtils.toInteger(uax.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
			int yscale = CommonUtils.toInteger(uay.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
			int wscale = CommonUtils.toInteger(uaw.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());
			int hscale = CommonUtils.toInteger(uah.getInMeters() * GfxUtils.SCALE_CONST / page.getInMeters());

			if (validUsableArea(wscale, hscale)) {
				ext = m_border.getUsableArea();
				ext.setX(xscale);
				ext.setY(yscale);
				ext.setWidth(wscale);
				ext.setHeight(hscale);
				m_border.setUsableArea(ext);
				m_border.setUnits(UnitHelper.enumToString(m_paperSizeChooser.getUnitType()));
			}
		}

		private boolean validUsableArea(int wscale, int hscale)
		{
			return wscale > 0 && hscale > 0;
		}

		/**
		 * @see IPropertiesClientComponent#stopEditing(IPropertiedSet)
		 */
		public void stopEditing(IPropertiedSet propset)
		{
		}

		/**
		 * @see IPropertiesClientComponent#destroy()
		 */
		public void destroy()
		{
		}

		/**
		 * @see IPropertiesClientComponent#isValid()
		 */
		public boolean isValid()
		{
			return (m_paperSizeChooser == null || m_paperSizeChooser.isPanelValid())
			    &&	(m_usableAreaChooser == null || m_usableAreaChooser.isPanelValid());
		}

		/**
		 * @see IPropertiesClientComponent#addValidityListener(IValidityListener)
		 */
		public void addValidityListener(IValidityListener listener)
		{
			if(m_paperSizeChooser != null) {
				m_paperSizeChooser.addValidityListener(listener);
			}
			if (m_usableAreaChooser != null){
				m_usableAreaChooser.addValidityListener(listener);
			}
		}

		/**
		 * @see IPropertiesClientComponent#removeValidityListener(IValidityListener)
		 */
		public void removeValidityListener(IValidityListener listener)
		{
		}

		@Override public void addPropertyChangeListener(@NotNull PropertyChangeListener propertyChangeListener)
		{
		}

		public Set<ISharedObject> getEditedSharedObjects()
		{
			return Collections.emptySet();
		}

		public Set<ISharedObject> getSharedObjects()
		{
			return Collections.emptySet();
		}
	}
}
