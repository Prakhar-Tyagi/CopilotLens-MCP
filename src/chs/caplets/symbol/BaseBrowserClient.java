/*
 * Copyright 2003-2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.symbol;

import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.browser.BrowserClientHelper;
import chs.caf.caplet.helpers.browser.BrowserFolder;
import chs.cof.COFTypeEnum;
import chs.cof.draw.ICompoundObject;
import chs.cof.draw.IGfxObject;
import chs.cof.draw.IGfxObjectIterator;
import chs.cof.draw.IGfxPrimitive;
import chs.cof.draw.IText;
import chs.cof.drawplus.IDatumRepresentation;
import chs.cof.drawplus.IGridDatumRepresentation;
import chs.cof.drawplus.IPropertiedCommentSymbol;
import chs.cof.drawplus.IRepresentedObject;
import chs.cof.logical.cable.IAbstractPin;
import chs.cof.logical.cable.IFunctionPin;
import chs.cof.logical.schem.IConnectivityRef;
import chs.cof.logical.schem.IInternalSchemPin;
import chs.cof.logical.schem.IPin;
import chs.cof.logical.schem.ISchemInternalLink;
import chs.cof.symbol.IBlock;
import chs.cof.symbol.IBlockIterator;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IStamp;
import chs.cof.symbol.ISymbolDef;
import chs.cof.symbol.ISymbolLibraryMgr;
import chs.cof.symbol.ISymbolRef;
import chs.cof.symbol.ISymboledObject;
import chs.cof.symbol.IUserDefinedZone;
import chs.cof.symbol.IZoneArea;
import chs.cof.symbol.IZoneAreaObject;
import chs.cof.symbol.IZoneExtent;
import chs.cof.symbol.SymbolTypeEnum;
import chs.common.IAnalysable;
import chs.common.IAttributeDatum;
import chs.common.IBaseDatum;
import chs.common.IDatum;
import chs.common.IDrillPointDatum;
import chs.common.IEngineeringDatum;
import chs.common.IFixturePlacementDatum;
import chs.common.IFormboardRegionDatum;
import chs.common.IFormboardRegionDatumHolder;
import chs.common.IGenericDatum;
import chs.common.IObjectFilter;
import chs.common.IReadOnlyNamedObject;
import chs.common.IUID;
import chs.common.IUIDObject;
import chs.common.UIDUtils;
import chs.common.reln.IRelatedEntityType;
import chs.common.reln.Relation;
import chs.images.CHSImageLoader;
import chs.images.CHSImages;
import chs.system.UIDMgr;
import chs.utilities.BuildInfo;
import chs.utilities.Pair;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utilities.suite.ApplicationSuiteInfo;
import chs.utility.SymbolUtils;
import chs.utility.helpers.NamedObjectComparator;
import chs.utility.helpers.UtilsHelper;
import chs.utility.reln.RelatedEntityUtils;
import chs.utility.ui.IconUtils;
import chs.utility.ui.UIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class BaseBrowserClient extends BrowserClientHelper
{

	private Model m_model;
	private Icon m_backshell_Icon;
	private Icon m_comment_icon;
	private Icon m_device_icon;
	private Icon m_ground_icon;
	private Icon m_splice_icon;
	private Icon m_pin_icon;
	private Icon m_functionPin_icon;
	private Icon m_border_icon;
	private Icon m_transparent_icon;
	private Icon m_additionalCompDatum_icon;
	private Icon m_connector_datum_icon;
	private Icon m_cavity_datum_icon;
	private Icon m_cavity_wire_datum_icon;
	private Icon m_grid_datum_icon;
	private Icon m_user_zone_icon;
	protected Icon m_analysisModel_decoration;
	private Icon m_attributeDatumIcon;
	private Icon m_engineeringDatumIcon;
	private Icon m_drillPointDatumIcon;
	private Icon m_fixturePlacementDatumIcon;
	private Icon m_genericDatumIcon;
	private Icon m_internalPin_icon;
	private Icon m_resistanceLink_icon;
	private Icon m_diodeLink_icon;
	private Icon m_fusingLink_icon;
	private IUIDObject m_pins = null;
	private IUIDObject m_blocks = null;
	private IUIDObject m_functionPins = null;
	private IUIDObject m_text = null;
	private IUIDObject m_gfx = null;
	private IUIDObject m_internalPins = null;
	private IUIDObject m_internalLinks = null;
	private BrowserFolder m_datumsFolder = null;
	//   private BrowserFolder m_generalDatumFolder = null;

	private BrowserFolder m_designDatumFolder = null;
	private BrowserFolder m_designConnectorDatumFolder = null;
	private BrowserFolder m_designSpliceDatumFolder = null;

	private BrowserFolder m_connectorDatumFolder = null;
	private BrowserFolder m_connectorCavityDatumFolder = null;
	private BrowserFolder m_connectorWireDatumFolder = null;
	private BrowserFolder m_connectorCavityWireDatumFolder = null;
	private BrowserFolder m_connectorPositionDatumFolder = null;
	private BrowserFolder m_connPosAddCompDatumFolder = null;
	private BrowserFolder m_connUnposAddCompDatumFolder = null;
	private BrowserFolder m_connAddCompDatumFolder = null;

	private BrowserFolder m_spliceDatumFolder = null;
	private BrowserFolder m_spliceCavityDatumFolder = null;
	private BrowserFolder m_spliceCenStripWireDatumFolder = null;
	private BrowserFolder m_spliceWireDatumFolder = null;
	private BrowserFolder m_spliceCavityWireDatumFolder = null;
	private BrowserFolder m_splicePositionDatumFolder = null;
	private BrowserFolder m_splicePosAddCompDatumFolder = null;
	private BrowserFolder m_spliceUnposAddCompDatumFolder = null;
	private BrowserFolder m_spliceAddCompDatumFolder = null;

	private BrowserFolder m_clipDatumFolder = null;
	private BrowserFolder m_clipPositionDatumFolder = null;
	private BrowserFolder m_clipPosAddCompDatumFolder = null;
	private BrowserFolder m_clipUnposAddCompDatumFolder = null;
	private BrowserFolder m_clipAddCompDatumFolder = null;

	private BrowserFolder m_grommetDatumFolder = null;
	private BrowserFolder m_grommetPositionDatumFolder = null;
	private BrowserFolder m_grommetPosAddCompDatumFolder = null;
	private BrowserFolder m_grommetUnposAddCompDatumFolder = null;
	private BrowserFolder m_grommetAddCompDatumFolder = null;

	private BrowserFolder m_nodeMLCDatumFolder = null;
	private BrowserFolder m_nodeMLCPositionDatumFolder = null;
	private BrowserFolder m_nodeMLCPosAddCompDatumFolder = null;
	private BrowserFolder m_nodeMLCUnposAddCompDatumFolder = null;
	private BrowserFolder m_nodeMLCAddCompDatumFolder = null;

	private BrowserFolder m_othCompDatumFolder = null;
	private BrowserFolder m_othCompPosDatumFolder = null;
	private BrowserFolder m_othCompPosAddCompDatumFolder = null;
	private BrowserFolder m_othCompUnposAddCompDatumFolder = null;
	private BrowserFolder m_otherCompAddCompDatumFolder = null;

	private BrowserFolder m_positionDatumFolder = null;

	private BrowserFolder m_gridDatumFolder = null;
	private BrowserFolder m_engineeringDatumFolder = null;
	private BrowserFolder m_attributeDatumFolder = null;
	protected BrowserFolder m_drillPointDatumFolder = null;
	private BrowserFolder m_fixturePlacementDatumFolder = null;
	private BrowserFolder m_genericDatumFolder = null;
	private BrowserFolder m_formboardRegionFolder = null;
	private BrowserFolder m_userDefinedZonesFolder = null;
	private BrowserFolder m_commentBlocksFolder = null;
	private Map<BrowserFolder, Pair<COFTypeEnum, Relation>> m_folderVsRedType =
			new LinkedHashMap<BrowserFolder, Pair<COFTypeEnum, Relation>>();
	private Map<BrowserFolder, List<IUID>> m_folderVsSubFolders =
			new LinkedHashMap<BrowserFolder, List<IUID>>();
	private List<IUID> m_children = null;
	private TypeComparator m_pinFilter = new PinTypeComparator();
	private TypeComparator m_functionPinFilter = new FunctionPinComparator();
	private TypeComparator m_internalPinFilter = new InternalPinTypeComparator();
	private TypeComparator m_internalLinkFilter = new InternalLinkTypeComparator();
	private TypeComparator m_gfxFilter = new GfxTypeComparator();
	private TypeComparator m_txtFilter = new TxtTypeComparator();

	private Comparator<IUID> m_comp = null;
	private static final int ELEVEN = 11;
	private static final String MissingReferenceSymbol =
			ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.CommentSymRefMissing.Name");
	private Icon m_function_Icon;

	/**
	 * Implementation of the BrowserClient to supply information to the browser tree
	 *
	 * @param cont controller
	 */
	protected BaseBrowserClient(ICapletController cont)
	{
		super(cont);
		m_controller = cont;
		m_model = (Model) cont.getCapletModel();

		// Create the icons for the browser
		createIcons();

		// create the top-level folders for pins, gfx...
		IStamp rootObj = m_model.getSymbolDef();
		setRootObject(rootObj);

		createFolders();

		m_children = new ArrayList<IUID>(2);
		populateRootNode(rootObj, m_children);

		m_comp = new SortedNamedObjectComparator();
	}

	private void createIcons()
	{
		m_backshell_Icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_backshell_active.gif");
		m_comment_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_comment_active.gif");
		m_device_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_device_active.gif");
		m_function_Icon = IconUtils.getSymbolIcon(SymbolTypeEnum.FUNCTION);
		m_ground_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_ground_active.gif");
		m_splice_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_splice_active.gif");
		m_folderIcon = CHSImageLoader.loadImageIcon("chs/images/general/ico_folder.gif");
		m_pin_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_pin_active.gif");
		m_functionPin_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_port_active.gif");
		m_border_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_border_active.gif");
		m_analysisModel_decoration = CHSImageLoader.loadImageIcon("chs/images/app/dec_analysismodel.gif");
		m_transparent_icon = CHSImageLoader.loadImageIcon(CHSImages.TRANSPARENT_ICON);
		m_grid_datum_icon = CHSImageLoader.loadImageIcon("chs/images/general/ico_drawrectangle_active.gif");
		m_user_zone_icon = CHSImageLoader.loadImageIcon("chs/images/javafx_ui/zone-small.png");
		m_attributeDatumIcon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
		m_engineeringDatumIcon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
		m_drillPointDatumIcon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
		m_fixturePlacementDatumIcon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");
		m_genericDatumIcon = CHSImageLoader.loadImageIcon("chs/images/app/ico_datum_generic.gif");

		m_connector_datum_icon = IconUtils.getIcon(COFTypeEnum.Connector.value(), IconUtils.ACTIVE);
		if (m_connector_datum_icon == null) {
			m_connector_datum_icon = m_transparent_icon;
		}
		m_cavity_datum_icon = IconUtils.getIcon(COFTypeEnum.ConnectorPin.value(), IconUtils.ACTIVE);
		if (m_cavity_datum_icon == null) {
			m_cavity_datum_icon = m_transparent_icon;
		}
		m_cavity_wire_datum_icon = IconUtils.getIcon(COFTypeEnum.Wire.value(), IconUtils.ACTIVE);
		if (m_cavity_wire_datum_icon == null) {
			m_cavity_wire_datum_icon = m_transparent_icon;
		}
		m_additionalCompDatum_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_component_datum.gif");
		m_internalPin_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_internalpin.png");
		m_resistanceLink_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_resistance_link.gif");
		m_diodeLink_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_diode_link.gif");
		m_fusingLink_icon = CHSImageLoader.loadImageIcon("chs/images/app/ico_fuse_link.gif");
	}

	protected void populateREDDatumFolders()
	{
		m_folderVsRedType.put(m_designConnectorDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.HarnessDesign, Relation.Connector));
		m_folderVsRedType.put(m_designSpliceDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.HarnessDesign, Relation.Splice));

		m_folderVsRedType.put(m_connectorCavityDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.ConnectorPin));
		m_folderVsRedType.put(m_connectorWireDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.Wire));
		m_folderVsRedType.put(m_connectorCavityWireDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.ConnectorPin, Relation.Wire));
		m_folderVsRedType.put(m_connectorPositionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.Position));
		m_folderVsRedType.put(m_connPosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_connUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_connAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Connector, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_spliceCavityDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.SplicePin));
		m_folderVsRedType.put(m_spliceCenStripWireDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.CenterStripWire));
		m_folderVsRedType.put(m_spliceWireDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.Wire));
		m_folderVsRedType.put(m_spliceCavityWireDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.SplicePin, Relation.Wire));
		m_folderVsRedType.put(m_splicePositionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.Position));
		m_folderVsRedType.put(m_splicePosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_spliceUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_spliceAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Splice, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_clipPositionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Clip, Relation.Position));
		m_folderVsRedType.put(m_clipPosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Clip, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_clipUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Clip, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_clipAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Clip, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_grommetPositionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Grommet, Relation.Position));
		m_folderVsRedType.put(m_grommetPosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Grommet, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_grommetUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Grommet, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_grommetAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Grommet, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_nodeMLCPositionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.MLC, Relation.Position));
		m_folderVsRedType.put(m_nodeMLCPosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.MLC, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_nodeMLCUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.MLC, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_nodeMLCAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.MLC, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_othCompPosDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.OtherComponent, Relation.Position));
		m_folderVsRedType.put(m_othCompPosAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.OtherComponent, Relation.PositionedAdditionalComponent));
		m_folderVsRedType.put(m_othCompUnposAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.OtherComponent, Relation.UnpositionedAdditionalComponent));
		m_folderVsRedType.put(m_otherCompAddCompDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.OtherComponent, Relation.NodeAdditionalComponent));

		m_folderVsRedType.put(m_positionDatumFolder,
				new Pair<COFTypeEnum, Relation>(COFTypeEnum.Position, Relation.AdditionalComponent));

		List<IUID> subfolder1 = new ArrayList<IUID>(ELEVEN);
		subfolder1.add(m_designDatumFolder.getUID());
		subfolder1.add(m_connectorDatumFolder.getUID());
		subfolder1.add(m_spliceDatumFolder.getUID());
		subfolder1.add(m_clipDatumFolder.getUID());
		subfolder1.add(m_grommetDatumFolder.getUID());
		subfolder1.add(m_nodeMLCDatumFolder.getUID());
		subfolder1.add(m_othCompDatumFolder.getUID());
		subfolder1.add(m_positionDatumFolder.getUID());
		subfolder1.add(m_gridDatumFolder.getUID());
		m_folderVsSubFolders.put(m_datumsFolder, subfolder1);

		List<IUID> subfolder2 = new ArrayList<IUID>(2);
		subfolder2.add(m_designConnectorDatumFolder.getUID());
		subfolder2.add(m_designSpliceDatumFolder.getUID());
		m_folderVsSubFolders.put(m_designDatumFolder, subfolder2);

		List<IUID> subfolder3 = new ArrayList<IUID>(7);
		subfolder3.add(m_connectorCavityDatumFolder.getUID());
		subfolder3.add(m_connectorWireDatumFolder.getUID());
		subfolder3.add(m_connectorCavityWireDatumFolder.getUID());
		subfolder3.add(m_connectorPositionDatumFolder.getUID());
		subfolder3.add(m_connPosAddCompDatumFolder.getUID());
		subfolder3.add(m_connUnposAddCompDatumFolder.getUID());
		subfolder3.add(m_connAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_connectorDatumFolder, subfolder3);

		List<IUID> subfolder4 = new ArrayList<IUID>(8);
		subfolder4.add(m_spliceCavityDatumFolder.getUID());
		subfolder4.add(m_spliceCenStripWireDatumFolder.getUID());
		subfolder4.add(m_spliceWireDatumFolder.getUID());
		subfolder4.add(m_spliceCavityWireDatumFolder.getUID());
		subfolder4.add(m_splicePositionDatumFolder.getUID());
		subfolder4.add(m_splicePosAddCompDatumFolder.getUID());
		subfolder4.add(m_spliceUnposAddCompDatumFolder.getUID());
		subfolder4.add(m_spliceAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_spliceDatumFolder, subfolder4);

		List<IUID> subfolder5 = new ArrayList<IUID>(4);
		subfolder5.add(m_clipPositionDatumFolder.getUID());
		subfolder5.add(m_clipPosAddCompDatumFolder.getUID());
		subfolder5.add(m_clipUnposAddCompDatumFolder.getUID());
		subfolder5.add(m_clipAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_clipDatumFolder, subfolder5);

		List<IUID> subfolder6 = new ArrayList<IUID>(4);
		subfolder6.add(m_grommetPositionDatumFolder.getUID());
		subfolder6.add(m_grommetPosAddCompDatumFolder.getUID());
		subfolder6.add(m_grommetUnposAddCompDatumFolder.getUID());
		subfolder6.add(m_grommetAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_grommetDatumFolder, subfolder6);

		List<IUID> subfolder7 = new ArrayList<IUID>(4);
		subfolder7.add(m_nodeMLCPositionDatumFolder.getUID());
		subfolder7.add(m_nodeMLCPosAddCompDatumFolder.getUID());
		subfolder7.add(m_nodeMLCUnposAddCompDatumFolder.getUID());
		subfolder7.add(m_nodeMLCAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_nodeMLCDatumFolder, subfolder7);

		List<IUID> subfolder8 = new ArrayList<IUID>(4);
		subfolder8.add(m_othCompPosDatumFolder.getUID());
		subfolder8.add(m_othCompPosAddCompDatumFolder.getUID());
		subfolder8.add(m_othCompUnposAddCompDatumFolder.getUID());
		subfolder8.add(m_otherCompAddCompDatumFolder.getUID());
		m_folderVsSubFolders.put(m_othCompDatumFolder, subfolder8);
	}

	/**
	 * String to be shown in the browser
	 */
	@Override @Nullable public String doGetPresentationName(IUID uid)
	{
		Object obj = m_uidMgr.getObject(uid);

		if (obj instanceof IReadOnlyNamedObject) {
			String name = ((IReadOnlyNamedObject) obj).getName();
			if (name != null && !name.isEmpty()) {
				return name;
			}
		}
		else if (obj instanceof IPin || obj instanceof IInternalSchemPin) {
			return ((IConnectivityRef) obj).getConnectivity().getName();
		}
		else if (obj instanceof ISchemInternalLink) {
			return ((ISchemInternalLink) obj).getConnectivity().getName();
		}
		else if (obj instanceof IDatumRepresentation) {
			return super.doGetPresentationName(uid);
		}
		else if (obj instanceof IPropertiedCommentSymbol) {
			ISymbolRef blkSymRef = ((ISymboledObject) obj).getSymbolRef();
			ISymbolLibraryMgr symLibMgr = UtilsHelper.getCHSSystem().getSymbolLibraryMgr();
			IStamp blkSymDef = symLibMgr.getReferencedSymbol(blkSymRef);
			return blkSymDef != null ? blkSymDef.getName() : MissingReferenceSymbol;
		}

		if (obj != null) {
			return obj.toString();
		}
		else {
			return null;
		}
	}

	public String getToolTipText(IUID uid, IUID parentUID)
	{
		return null;
	}

	/**
	 * Icon based on type
	 */
	@Override @Nullable public Icon getIcon(@NotNull IUID uid)
	{
		IUIDObject nodeObj = m_uidMgr.getObject(uid);
		Icon icon = null;
		// is it the root diagram?
		if (uid == getRoot()) {
			icon = m_device_icon;
		}

		if (nodeObj instanceof IPin) {
			IAbstractPin cablePin = ((IPin) nodeObj).getConnectivity();
			if (cablePin instanceof IFunctionPin) {
				icon = m_functionPin_icon;
			}
			else {
				icon = m_pin_icon;
			}
		}

		if (nodeObj instanceof IInternalSchemPin) {
			icon = m_internalPin_icon;
		}

		if (nodeObj instanceof ISchemInternalLink) {
			ISchemInternalLink link = (ISchemInternalLink) nodeObj;
			String linkType = link.getConnectivity().getLinkType();
			if ("Resistance".equalsIgnoreCase(linkType)) {
				icon = m_resistanceLink_icon;
			}
			else if ("Fusing".equalsIgnoreCase(linkType)) {
				icon = m_fusingLink_icon;
			}
			else if ("Diode".equalsIgnoreCase(linkType)) {
				icon = m_diodeLink_icon;
			}
		}
		if (nodeObj instanceof IDatumRepresentation) {
			IBaseDatum baseDatum = ((IDatumRepresentation) nodeObj).getDatum();
			if (baseDatum instanceof IDatum) {
				IDatum datum = (IDatum) baseDatum;
				if (isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.HarnessDesign, Relation.Connector))) {
					icon = m_connector_datum_icon;
				}
				else if (isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.HarnessDesign, Relation.Splice))) {
					icon = m_splice_icon;
				}
				else if (isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector, Relation.ConnectorPin)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice, Relation.SplicePin))) {
					icon = m_cavity_datum_icon;
				}
				else if (isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.ConnectorPin, Relation.Wire)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.SplicePin, Relation.Wire)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice, Relation.Wire)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector, Relation.Wire)) ||
						isDatumOfType(datum, RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice,
								Relation.CenterStripWire))) {
					icon = m_cavity_wire_datum_icon;
				}
				else if (isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Position, Relation.Position)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector, Relation.Position)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice, Relation.Position)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Clip, Relation.Position)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Grommet, Relation.Position)) ||
						isDatumOfType(datum,
								RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.MLC, Relation.Position)) ||
						isDatumOfType(datum, RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.OtherComponent,
								Relation.Position))) {
					icon = IconUtils.getFilledPositionIcon(IconUtils.NEITHER);
				}
				else if (isAdditionalComponentDatum(datum)) {
					icon = m_additionalCompDatum_icon;
				}
				else {
					icon = m_transparent_icon;
				}
			}
			else if (baseDatum instanceof IAttributeDatum) {
				icon = m_attributeDatumIcon;
			}
			else if (baseDatum instanceof IEngineeringDatum) {
				icon = m_engineeringDatumIcon;
			}
			else if (baseDatum instanceof IDrillPointDatum) {
				icon = m_drillPointDatumIcon;
			}
			else if (baseDatum instanceof IFixturePlacementDatum) {
				icon = m_fixturePlacementDatumIcon;
			}
			else if (baseDatum instanceof IGenericDatum) {
				icon = m_genericDatumIcon;
			}
		}

		if (nodeObj instanceof IGridDatumRepresentation) {
			icon = m_grid_datum_icon;
		}
		else if (nodeObj instanceof IUserDefinedZone) {
			icon = m_user_zone_icon;
		}
		else if (nodeObj instanceof IPropertiedCommentSymbol) {
			icon = m_comment_icon;
			icon = IconUtils.getSymbolledObjectIconDecorated(icon, (ISymboledObject) nodeObj);
		}
		else if (nodeObj instanceof ISymbolDef) {
			if (SymbolUtils.isGroundSymbol((ISymbolDef) nodeObj)) {
				icon = m_ground_icon;
			}
			else if (SymbolUtils.isBackshellSymbol((ISymbolDef) nodeObj)) {
				icon = m_backshell_Icon;
			}
			else if (SymbolUtils.isSpliceSymbol((ISymbolDef) nodeObj)) {
				icon = m_splice_icon;
			}
			else if (SymbolUtils.isCommentSymbol((ISymbolDef) nodeObj)) {
				icon = ((ISymbolDef) nodeObj).getSymbolSubType().getIcon();
			}
			else if (SymbolUtils.isFunctionSymbol((ISymbolDef) nodeObj)) {
				icon = m_function_Icon;
			}
			else {
				icon = m_device_icon;
			}
			icon = IconUtils.getSymbolDefIconDecorated(icon, (IStamp) nodeObj);
		}
		else if (nodeObj instanceof IBorder) {
			icon = m_border_icon;
			icon = IconUtils.getSymbolDefIconDecorated(icon, (IStamp) nodeObj);
		}

		// check for one of our folders
		if (nodeObj instanceof BrowserFolder) {
			icon = m_folderIcon;
		}

		if (icon != null && nodeObj != null) {
			if (nodeObj instanceof IAnalysable && ((IAnalysable) nodeObj).hasAnalysisModel()) {
				icon = CHSImageLoader
						.getDecoratedImage(icon, m_analysisModel_decoration, CHSImageLoader.ANALYSIS_CORNER);
			}
			return icon;
		}

		return null;
	}

	private boolean isAdditionalComponentDatum(IDatum datum)
	{
		return isDatumOfType(datum,
				RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Position, Relation.AdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Connector,
								Relation.NodeAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Splice,
								Relation.NodeAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Clip,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Clip,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Clip, Relation.NodeAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Grommet,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Grommet,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.Grommet,
								Relation.NodeAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.OtherComponent,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.OtherComponent,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.OtherComponent,
								Relation.NodeAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.MLC,
								Relation.PositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.MLC,
								Relation.UnpositionedAdditionalComponent)) ||
				isDatumOfType(datum,
						RelatedEntityUtils.getRelatedEntityType(COFTypeEnum.MLC, Relation.NodeAdditionalComponent));
	}

	private Collection<IUID> getGraphics()
	{
		return getGraphics(m_model.getSymbolDef().getGfx());
	}

	private Collection<IUID> getGraphics(ICompoundObject pl)
	{
		return getObjects(pl, m_gfxFilter);
	}

	private Collection<IUID> getText()
	{
		return getText(m_model.getSymbolDef().getGfx());
	}

	private Collection<IUID> getText(ICompoundObject pl)
	{
		return getObjects(pl, m_txtFilter);
	}

	private List<IUID> getBlocks()
	{
		List<IUID> res = new ArrayList<IUID>();
		IStamp stmp = m_model.getSymbolDef();
		if (stmp instanceof ISymbolDef) {
			ISymbolDef sd = (ISymbolDef) stmp;
			for (IBlockIterator sditr = sd.getBlocks(); sditr.hasNext(); ) {
				res.add(sditr.getNext().getUID());
			}
		}
		return res;
	}

	private List<IUID> getComments()
	{
		List<IUID> res = new ArrayList<IUID>();
		IStamp stmp = m_model.getSymbolDef();
		if (stmp instanceof ISymbolDef) {
			ISymbolDef sd = (ISymbolDef) stmp;
			ICompoundObject gfx = sd.getGfx();
			if (gfx != null) {
				for (IPropertiedCommentSymbol comment : gfx.getObjects(IPropertiedCommentSymbol.class)) {
					res.add(comment.getUID());
				}
			}
		}
		return res;
	}

	private List<IUID> getPins(IBlock blk)
	{
		return getPins(blk.getGfx());
	}

	private List<IUID> getPins()
	{
		return getPins(m_model.getSymbolDef().getGfx());
	}

	private List<IUID> getPins(ICompoundObject pl)
	{
		return getObjects(pl, m_pinFilter);
	}

	private List<IUID> getInternalPins(IBlock blk)
	{
		return getInternalPins(blk.getGfx());
	}

	private List<IUID> getInternalPins()
	{
		return getInternalPins(m_model.getSymbolDef().getGfx());
	}

	private List<IUID> getInternalPins(ICompoundObject pl)
	{
		return getObjects(pl, m_internalPinFilter);
	}

	private List<IUID> getInternalLinks(IBlock blk)
	{
		return getInternalLinks(blk.getGfx());
	}

	private List<IUID> getInternalLinks()
	{
		return getInternalLinks(m_model.getSymbolDef().getGfx());
	}

	private List<IUID> getInternalLinks(ICompoundObject pl)
	{
		return getObjects(pl, m_internalLinkFilter);
	}

	private boolean symbolHasDatums()
	{
		if (m_model.getSymbolDef().getGfx() == null) {
			return false;
		}
		Collection<IDatumRepresentation> datumReps =
				m_model.getSymbolDef().getGfx().getObjects(IDatumRepresentation.class);
		return datumReps != null && !datumReps.isEmpty();
	}

	private List<IUID> getDatumsRepresentation(IRelatedEntityType relatedEntityType)
	{

		Map<IRelatedEntityType, List<IDatum>> result = m_model.getSymbolDef().getTopLevelDatums();

		if (result.containsKey(relatedEntityType) && !(relatedEntityType.equals(IRelatedEntityType.Unknown))) {
			List<IBaseDatum> datums = new ArrayList<IBaseDatum>();
			for (IDatum datum : result.get(relatedEntityType)) {
				datums.add(datum);
			}
			return getDatumRepresentationUIDs(
					SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), datums));
		}
		else if (result.containsKey(relatedEntityType) && relatedEntityType.equals(IRelatedEntityType.Unknown)) {

			List<IDatum> gridDatum = result.get(IRelatedEntityType.Unknown);
			List<IBaseDatum> datums = new ArrayList<IBaseDatum>();
			for (IDatum datum : gridDatum) {
				datums.add(datum);
			}
			return getDatumRepresentationUIDs(
					SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), datums));
		}
		else {
			return Collections.emptyList();
		}
	}

	private List<IUID> getEngineeringDatumsRepresentation()
	{

		Collection<IEngineeringDatum> result = m_model.getSymbolDef().getAllEngineeringDatums();
		List<IEngineeringDatum> engineeringDatums = new ArrayList<IEngineeringDatum>();
		for (IEngineeringDatum datum : result) {
			if (datum.getName() != null) {
				engineeringDatums.add(datum);
			}
		}
		return getDatumRepresentationUIDs(
				SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), engineeringDatums));
	}

	private List<IUID> getAttributeDatumsRepresentation()
	{

		Collection<IAttributeDatum> result = m_model.getSymbolDef().getAllAttributeDatums();
		List<IAttributeDatum> attributeDatums = new ArrayList<IAttributeDatum>();
		for (IAttributeDatum datum : result) {
			if (datum.getName() != null && m_model.getSymbolDef().getAssociatedDatum(datum) == null) {
				attributeDatums.add(datum);
			}
		}
		return getDatumRepresentationUIDs(
				SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), attributeDatums));
	}

	private List<IUID> getDrillPointDatumsRepresentation()
	{

		Collection<IDrillPointDatum> result = m_model.getSymbolDef().getAllDrillPointDatums();
		List<IDrillPointDatum> dpDatums = new ArrayList<IDrillPointDatum>();
		for (IDrillPointDatum datum : result) {
			if (datum.getName() != null) {
				dpDatums.add(datum);
			}
		}
		return getDatumRepresentationUIDs(
				SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), dpDatums));
	}

	private List<IUID> getFixturePlacementDatumsRepresentation()
	{

		Collection<IFixturePlacementDatum> result = m_model.getSymbolDef().getAllFixturePlacementDatums();
		List<IFixturePlacementDatum> fpDatums = new ArrayList<IFixturePlacementDatum>();
		for (IFixturePlacementDatum datum : result) {
			if (datum.getName() != null) {
				fpDatums.add(datum);
			}
		}
		return getDatumRepresentationUIDs(
				SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), fpDatums));
	}

	private List<IUID> getGenericDatumsRepresentation()
	{

		Collection<IGenericDatum> result = m_model.getSymbolDef().getAllGenericDatums();
		List<IGenericDatum> genDatums = new ArrayList<IGenericDatum>();
		for (IGenericDatum datum : result) {
			if (datum.getName() != null) {
				genDatums.add(datum);
			}
		}
		return getDatumRepresentationUIDs(
				SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), genDatums));
	}

	private List<IUID> getFormboardRegionDatumsRepresentation()
	{

		IStamp symbol = m_model.getSymbolDef();
		if (symbol instanceof IFormboardRegionDatumHolder) {
			Collection<IFormboardRegionDatum> result =
					((IFormboardRegionDatumHolder) symbol).getAllFormboardRegionDatums();
			List<IFormboardRegionDatum> frDatums = new ArrayList<IFormboardRegionDatum>();
			for (IFormboardRegionDatum datum : result) {
				if (datum.getName() != null) {
					frDatums.add(datum);
				}
			}
			return getDatumRepresentationUIDs(SymbolUtils.getDatumRepresentation(symbol, frDatums));
		}
		return Collections.emptyList();
	}

	private List<IUID> getUserDefinedZones()
	{
		IStamp symbol = m_model.getSymbolDef();
		if (symbol instanceof IZoneAreaObject) {
			IZoneAreaObject zoneAreaObject = (IZoneAreaObject) symbol;
			IZoneArea zoneArea = zoneAreaObject.getZoneArea();
			if (zoneArea != null) {
				Set<IUserDefinedZone> userDefinedZones = new HashSet<>();
				for (IZoneExtent zoneExtent : zoneArea.getZoneExtents()) {
					IUserDefinedZone userDefined = zoneExtent.getUserDefined();
					if (userDefined != null) {
						userDefinedZones.add(userDefined);
					}
				}
				//if name is same then sort by UID.
				List<IUserDefinedZone> sortedUDZs = new ArrayList<>(userDefinedZones);
				Collections.sort(sortedUDZs, (z1, z2) -> z1.getUID().compareTo(z2.getUID()));
				Collections.sort(sortedUDZs, (z1, z2) -> String.CASE_INSENSITIVE_ORDER
						.compare(StringUtils.nonNull(z1.getName()), StringUtils.nonNull(z2.getName())));
				return UIDUtils.convertToUID(sortedUDZs);
			}
		}
		return Collections.emptyList();
	}

	private boolean isDatumOfType(IDatum datum, IRelatedEntityType relatedEntityType)
	{
		IRelatedEntityType symbolRelType = m_model.getSymbolDef().getRelatedEntityType(datum);
		return symbolRelType != null && symbolRelType.equals(relatedEntityType);
	}

	private List<IUID> getObjects(ICompoundObject pl, TypeComparator tc)
	{
		List<IUID> vec = new ArrayList<IUID>();
		if (pl != null) {
			for (IGfxObjectIterator git = pl.getObjects(); git.hasNext(); ) {
				IGfxObject gobj = git.getNext();
				if (tc.isWantedType(gobj) && gobj instanceof IUIDObject) {
					IUIDObject uidObj = (IUIDObject) gobj;
					vec.add(uidObj.getUID());
				}
			}
		}
		return vec;
	}

	/**
	 * is this object selectable in the browser tree?
	 */
	public boolean isSelectable(IUID uid)
	{
		IUIDObject obj = m_uidMgr.getObject(uid);
		return !(obj instanceof BrowserFolder);
	}

	private static class SortedNamedObjectComparator implements Comparator<IUID>
	{

		public int compare(IUID o1, IUID o2)
		{
			// get the objects from their uid
			IUIDObject obj1 = UIDMgr.getObject(o1);
			IUIDObject obj2 = UIDMgr.getObject(o2);

			// if this is not a represented object, we don't know what to do
			// with is so return no change
			if (!(obj1 instanceof IRepresentedObject) || !(obj2 instanceof IRepresentedObject)) {
				return 0;
			}
			IUIDObject connectivity1 = ((IRepresentedObject) obj1).getRawConnectivity();
			IUIDObject connectivity2 = ((IRepresentedObject) obj2).getRawConnectivity();
			// now make sure they're named objects
			if (!(connectivity1 instanceof IReadOnlyNamedObject) || !(connectivity2 instanceof IReadOnlyNamedObject)) {
				return 0;
			}
			String n1 = ((IReadOnlyNamedObject) connectivity1).getName();
			String n2 = ((IReadOnlyNamedObject) connectivity2).getName();
			// use the system wide comparator
			return UIUtils.compareAlphaNumStrings(n1, n2);
		}
	}

	public List<IUID> getChildren(IUID uid)
	{
		if (uid == getRoot()) {
			return m_children;
		}

		IUIDObject obj = m_uidMgr.getObject(uid);
		List<IUID> vecChildren = Collections.emptyList();
		if (obj == m_pins) {
			vecChildren = getPins();
			Collections.sort(vecChildren, m_comp);
		}
		else if (obj == m_internalPins) {
			vecChildren = getInternalPins();
			Collections.sort(vecChildren, m_comp);
		}
		else if (obj == m_internalLinks) {
			vecChildren = getInternalLinks();
			Collections.sort(vecChildren, m_comp);
		}
		else if (obj == m_blocks) {
			vecChildren = getBlocks();
		}
		else if (obj == m_functionPins) {
			vecChildren = getFunctionPins();
			Collections.sort(vecChildren, m_comp);
		}
		else if (obj instanceof IBlock) {
			List<IUID> children = new ArrayList<IUID>();
			children.addAll(getPins((IBlock) obj));
			children.addAll(getInternalPins((IBlock) obj));
			children.addAll(getInternalLinks((IBlock) obj));
			vecChildren = children;
		}
		else if (m_folderVsSubFolders.containsKey(obj)) {
			List<IUID> subfolders = m_folderVsSubFolders.get(obj);
			Collections.sort(subfolders, NamedObjectComparator.caseInsensitiveComparator());
			return subfolders;
		}
		else if (m_folderVsRedType.containsKey(obj)) {
			Pair<COFTypeEnum, Relation> pair = m_folderVsRedType.get(obj);
			COFTypeEnum CofType = pair.getFirst();
			Relation relation = pair.getSecond();
			vecChildren = getDatumsRepresentation(RelatedEntityUtils.getRelatedEntityType(CofType, relation));
		}
		else if (obj == m_gridDatumFolder) {
			vecChildren = getDatumsRepresentation(IRelatedEntityType.Unknown);
		}
		else if (obj == m_engineeringDatumFolder) {
			vecChildren = getEngineeringDatumsRepresentation();
		}
		else if (obj == m_attributeDatumFolder) {
			vecChildren = getAttributeDatumsRepresentation();
		}
		else if (obj == m_drillPointDatumFolder) {
			vecChildren = getDrillPointDatumsRepresentation();
		}
		else if (obj == m_fixturePlacementDatumFolder) {
			vecChildren = getFixturePlacementDatumsRepresentation();
		}
		else if (obj == m_genericDatumFolder) {
			vecChildren = getGenericDatumsRepresentation();
		}
		else if (obj == m_formboardRegionFolder) {
			vecChildren = getFormboardRegionDatumsRepresentation();
		}
		else if (obj == m_userDefinedZonesFolder) {
			vecChildren = getUserDefinedZones();
		}
		else if (obj == m_commentBlocksFolder) {
			vecChildren = getComments();
		}

		else if (obj instanceof IGridDatumRepresentation) {
			return vecChildren;
		}

		else if (obj instanceof IDatumRepresentation) {
			IBaseDatum baseDatum = ((IDatumRepresentation) obj).getDatum();
			if (baseDatum instanceof IDatum) {
				IDatum datum = (IDatum) baseDatum;
				List<IUID> children = new ArrayList<IUID>();
				IRelatedEntityType iRelatedEntityType = m_model.getSymbolDef().getRelatedEntityType(datum);
				if (iRelatedEntityType != null) {
					COFTypeEnum anEnum = iRelatedEntityType.getTargetEntityType();
					List<IRelatedEntityType> entityTypes = RelatedEntityUtils.getRelatedEntities(anEnum.value(), new IObjectFilter<IRelatedEntityType>()
					{
						public boolean accept(IRelatedEntityType obj)
						{
							return true;
						}
					});
					for (IRelatedEntityType relType : entityTypes) {
						List<IUID> datumReps =
								getDatumRepresentationUIDs(SymbolUtils.getChildrenDatumsOfType(m_model.getSymbolDef(),
										datum, relType));

						children.addAll(datumReps);
					}
				}
				Collection<IAttributeDatum> associatedAttributeDatums =
						m_model.getSymbolDef().getAssociatedAttributesDatums(datum);
				List<IDatumRepresentation> datumReps =
						SymbolUtils.getDatumRepresentation(m_model.getSymbolDef(), associatedAttributeDatums);
				children.addAll(getDatumRepresentationUIDs(datumReps));
				vecChildren = children;
			}
		}
		if (vecChildren.isEmpty()) {
			return null;
		}

		return vecChildren;
	}

	private List<IUID> getFunctionPins()
	{
		return getObjects(m_model.getSymbolDef().getGfx(), m_functionPinFilter);
	}

	private static List<IUID> getDatumRepresentationUIDs(List<IDatumRepresentation> datumReps)
	{
		List<IUID> datumRepIds = new ArrayList<IUID>();

		for (IDatumRepresentation dr : datumReps) {
			datumRepIds.add(dr.getUID());
		}

		return datumRepIds;
	}

	public boolean hasChildren(IUID uid, IUID parentUID)
	{
		// the root node always has children
		if (uid == getRoot()) {
			return true;
		}

		//return false;
		Object nodeObj = m_uidMgr.getObject(uid);

		/* first check for one of our folders*/
		if (nodeObj instanceof BrowserFolder) {
			// now find out which type it is
			if (nodeObj == m_pins) {
				return !(getPins().isEmpty());
			}
			else if (nodeObj == m_functionPins) {
				return !(getFunctionPins().isEmpty());
			}
			else if (nodeObj == m_internalPins) {
				return !(getInternalPins().isEmpty());
			}
			else if (nodeObj == m_internalLinks) {
				return !(getInternalLinks().isEmpty());
			}
			else if (nodeObj == m_blocks) {
				return !(getBlocks().isEmpty());
			}
			else if (nodeObj == m_gfx) {
				return !(getGraphics().isEmpty());
			}
			else if (nodeObj == m_text) {
				return !(getText().isEmpty());
			}
			else if (nodeObj == m_datumsFolder) {
				return (symbolHasDatums());
			}
			else if (nodeObj == m_designDatumFolder || nodeObj == m_connectorDatumFolder ||
					nodeObj == m_spliceDatumFolder || nodeObj == m_clipDatumFolder || nodeObj == m_grommetDatumFolder ||
					nodeObj == m_nodeMLCDatumFolder || nodeObj == m_othCompDatumFolder) {
				return true;
			}
			else if (m_folderVsRedType.containsKey(nodeObj)) {
				Pair<COFTypeEnum, Relation> pair = m_folderVsRedType.get(nodeObj);
				COFTypeEnum CofType = pair.getFirst();
				Relation relation = pair.getSecond();
				return !(getDatumsRepresentation(
						RelatedEntityUtils.getRelatedEntityType(CofType, relation)).isEmpty());
			}
			else if (nodeObj == m_gridDatumFolder) {
				return !(getDatumsRepresentation(IRelatedEntityType.Unknown).isEmpty());
			}
			else if (nodeObj == m_engineeringDatumFolder) {
				return !(getEngineeringDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_attributeDatumFolder) {
				return !(getAttributeDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_drillPointDatumFolder) {
				return !(getDrillPointDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_fixturePlacementDatumFolder) {
				return !(getFixturePlacementDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_genericDatumFolder) {
				return !(getGenericDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_formboardRegionFolder) {
				return !(getFormboardRegionDatumsRepresentation().isEmpty());
			}
			else if (nodeObj == m_userDefinedZonesFolder) {
				return !(getUserDefinedZones().isEmpty());
			}
			else if (nodeObj == m_commentBlocksFolder) {
				return !(getComments().isEmpty());
			}
		}
		else if (nodeObj instanceof IBlock) {
			return !(getPins((IBlock) nodeObj).isEmpty())
					|| !(getInternalPins((IBlock) nodeObj).isEmpty())
					|| !(getInternalLinks((IBlock) nodeObj).isEmpty());
		}
		else if (nodeObj instanceof IGridDatumRepresentation) {
			return false;
		}
		else if (nodeObj instanceof IDatumRepresentation) {
			IBaseDatum baseDatum = ((IDatumRepresentation) nodeObj).getDatum();
			if (baseDatum instanceof IDatum) {
				return !SymbolUtils.getAllRelatedDatums(m_model.getSymbolDef(), baseDatum).isEmpty();
			}
			else {
				return false;
			}
		}
		return false;
	}

	private void createFolders()
	{
		createSymbolOnlyFolders();
		if (isCapitalSuite()) {
			createGenericDatumFolder();
			createFormboardRegionFolder();
		}
		createUserDefinedZoneFolder();
		createCommentBlocksFolder();
	}

	protected void createSymbolOnlyFolders()
	{
		m_pins = createFolder(ResourceMgr.getString(BaseBrowserClient.class, "BrowserClient.Folder.Pins.Label"));
		m_internalPins =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "BrowserClient.Folder.InternalPins.Label"));
		m_internalLinks =
				createFolder(
						ResourceMgr.getString(BaseBrowserClient.class, "BrowserClient.Folder.InternalLinks.Label"));
		m_blocks = createFolder(ResourceMgr.getString(BaseBrowserClient.class, "BrowserClient.Folder.Blocks.Label"));
		m_datumsFolder = createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.Datum.Title"));

		m_designDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.DesignDatum.Title"));
		m_designConnectorDatumFolder =
				createFolder(
						ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.DesignConnectorDatum.Title"));
		m_designSpliceDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.DesignSpliceDatum.Title"));

		m_connectorDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.ConnectorDatum.Title"));
		m_connectorCavityDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.Cavity.Title"));
		m_connectorWireDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.Wire.Title"));
		m_connectorCavityWireDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.CavityWire.Title"));
		m_connectorPositionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_connPosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_connUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_connAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_spliceDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.SpliceDatum.Title"));
		m_spliceCavityDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.Cavity.Title"));
		m_spliceWireDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.Wire.Title"));
		m_spliceCavityWireDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.CavityWire.Title"));
		m_spliceCenStripWireDatumFolder =
				createFolder(
						ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.CenterStripWireDatum.Title"));
		m_splicePositionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_splicePosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_spliceUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_spliceAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_clipDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.ClipDatum.Title"));
		m_clipPositionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_clipPosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_clipUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_clipAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_grommetDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.GrommetDatum.Title"));
		m_grommetPositionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_grommetPosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_grommetUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_grommetAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_nodeMLCDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.MLCDatum.Title"));
		m_nodeMLCPositionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_nodeMLCPosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_nodeMLCUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_nodeMLCAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_othCompDatumFolder =
				createFolder(
						ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.OtherComponentDatum.Title"));
		m_positionDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_othCompPosDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionDatum.Title"));
		m_othCompPosAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.PositionedAddCompDatum.Title"));
		m_othCompUnposAddCompDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UnpositionedAddCompDatum.Title"));
		m_otherCompAddCompDatumFolder =
				createFolder(ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AddCompDatum.Title"));

		m_gridDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.GridDatum.Title"));
		m_engineeringDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.EngineeringDatum.Title"));
		m_attributeDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.AttributeDatum.Title"));
		m_functionPins = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.FunctionPorts.Title"));
		if (isCapitalSuite()) {
			m_drillPointDatumFolder = createFolder(
					ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.DrillPointDatum.Title"));
			m_fixturePlacementDatumFolder = createFolder(
					ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.FixturePlacementDatum.Title"));
		}
		populateREDDatumFolders();
	}

	protected static boolean isCapitalSuite()
	{
		return ApplicationSuiteInfo.getInstance().getCurrentApplicationSuite().getAppSuite() ==
				ApplicationSuiteInfo.AppSuite.Capital;
	}

	protected void createGenericDatumFolder()
	{
		m_genericDatumFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.GenericDatum.Title"));
	}

	protected void createFormboardRegionFolder()
	{
		m_formboardRegionFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.FormboardRegion.Title"));
	}

	protected void createUserDefinedZoneFolder()
	{
		m_userDefinedZonesFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.UserDefinedZone.Title"));
	}

	protected void createCommentBlocksFolder()
	{
		if (!BuildInfo.getBuildInfo().areDeveloperExtensionsEnabled()) {
			return;
		}
		m_commentBlocksFolder = createFolder(
				ResourceMgr.getString(BaseBrowserClient.class, "Resouce.Browser.CommentBlock.Title"));
	}

	private void populateRootNode(IStamp root, List<IUID> children)
	{
		addSymbolFolders(root, children);
		addBorderFolders(children);
		addCommentBlocksFolder(children);
	}

	protected void addSymbolFolders(IStamp root, List<IUID> children)
	{
		if (root instanceof ISymbolDef) {
			ISymbolDef symDef = (ISymbolDef) root;
			// Backshell, Comment and Splice symbols can't have blocks
			if (!(SymbolUtils.isBackshellSymbol(symDef)
					|| SymbolUtils.isSpliceSymbol(symDef)
					|| SymbolUtils.isCommentSymbol(symDef))) {
				children.add(m_blocks.getUID());
			}
			// Comment symbols don't have pins
			if (!SymbolUtils.isCommentSymbol(symDef)) {
				children.add(SymbolUtils.isFunctionSymbol(symDef) ? m_functionPins.getUID() : m_pins.getUID());
				if (SymbolUtils.isDeviceSymbol(symDef)) {
					children.add(m_internalPins.getUID());
					children.add(m_internalLinks.getUID());
				}
			}
			/** Only comment symbols can have datum
			 *
			 */
			if (SymbolUtils.isCommentSymbol(symDef)) {
				children.add(m_attributeDatumFolder.getUID());
				children.add(m_engineeringDatumFolder.getUID());
				children.add(m_datumsFolder.getUID());
				if (m_drillPointDatumFolder != null) {
					children.add(m_drillPointDatumFolder.getUID());
				}
				if (m_fixturePlacementDatumFolder != null) {
					children.add(m_fixturePlacementDatumFolder.getUID());
				}
				addGenericDatumFolder(children);
			}
		}
		else {
			children.add(m_pins.getUID());
			children.add(m_blocks.getUID());
			children.add(m_internalPins.getUID());
			children.add(m_internalLinks.getUID());
		}
	}

	protected void addBorderFolders(List<IUID> children)
	{
		addGenericDatumFolder(children);
		addFormboardRegionFolder(children);
		addUserDefinedZonesFolder(children);
	}

	protected void addGenericDatumFolder(List<IUID> children)
	{
		if (m_genericDatumFolder != null) {
			children.add(m_genericDatumFolder.getUID());
		}
	}

	protected void addFormboardRegionFolder(List<IUID> children)
	{
		if (m_formboardRegionFolder != null) {
			children.add(m_formboardRegionFolder.getUID());
		}
	}

	protected void addUserDefinedZonesFolder(List<IUID> children)
	{
		if (m_userDefinedZonesFolder != null) {
			children.add(m_userDefinedZonesFolder.getUID());
		}
	}

	protected void addCommentBlocksFolder(List<IUID> children)
	{
		if (m_commentBlocksFolder != null) {
			children.add(m_commentBlocksFolder.getUID());
		}
	}

	private interface TypeComparator
	{

		boolean isWantedType(Object o);
	}

	private static class PinTypeComparator implements TypeComparator
	{

		public boolean isWantedType(Object o)
		{
			return o instanceof IPin;
		}
	}

	private static class FunctionPinComparator implements TypeComparator
	{

		@Override public boolean isWantedType(Object o)
		{
			return o instanceof IPin && ((IPin) o).getConnectivity() instanceof IFunctionPin;
		}
	}

	private static class InternalPinTypeComparator implements TypeComparator
	{

		public boolean isWantedType(Object o)
		{
			return o instanceof IInternalSchemPin;
		}
	}

	private static class InternalLinkTypeComparator implements TypeComparator
	{

		public boolean isWantedType(Object o)
		{
			return o instanceof ISchemInternalLink;
		}
	}

	private static class GfxTypeComparator implements TypeComparator
	{

		public boolean isWantedType(Object o)
		{
			return o instanceof IGfxPrimitive && !(o instanceof IText);
		}
	}

	private static class TxtTypeComparator implements TypeComparator
	{

		public boolean isWantedType(Object o)
		{
			return o instanceof IText;
		}
	}
}

