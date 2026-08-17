/*
 * Copyright 2018 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.border.actions;

import chs.caf.AbstractContextAction;
import chs.caf.ActionContainer;
import chs.caf.ActionEntry;
import chs.caf.CAFUtils;
import chs.caf.IApplicationSpecificationAction;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.action.IActionEnum;
import chs.caf.caplet.helpers.creation.BasicDrawingAction;
import chs.caf.caplet.selection.SelectSet;
import chs.cof.draw.IColor;
import chs.cof.draw.IDrawFactory;
import chs.cof.draw.IFillPattern;
import chs.cof.draw.IGfxAttribute;
import chs.cof.draw.IWritableGfxAttribute;
import chs.cof.draw.LogicalGraphicSize;
import chs.cof.symbol.AbstractUserDefinedZoneFactory;
import chs.cof.symbol.IBorder;
import chs.cof.symbol.IGridDisplayHandler;
import chs.cof.symbol.IUserDefinedZone;
import chs.cof.symbol.IUserDefinedZoneFactory;
import chs.cof.symbol.IZoneArea;
import chs.cof.symbol.IZoneAreaObject;
import chs.cof.symbol.IZoneExtent;
import chs.cof.symbol.IZoneExtentStore;
import chs.cof.symbol.ZoneEditUnit;
import chs.common.IExtent;
import chs.services.dynamicgfx.DynamicGfxFactoryHelper;
import chs.services.dynamicgfx.IDynamicGfx;
import chs.services.dynamicgfx.IDynamicGfxFactory;
import chs.services.gfx.GfxView;
import chs.system.FactoryMgr;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import chs.utility.GfxUtils;
import chs.utility.gfx.IViewInvalidationEnum;
import chs.utility.logic.ISymbolModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Action to create a rectangular formboard region datum
 */
public class EditUserDefinedZonesAction extends BasicDrawingAction
{

	private static final int TOOLTIP_HEIGHT = 100;
	private static final int RGB_COMPONENT_MAX_VAL = 255;
	@NotNull private final Map<ZoneEditStyle, Cursor> m_cursors = new EnumMap<>(ZoneEditStyle.class);
	@NotNull private ZoneEditStyle m_editStyle = ZoneEditStyle.NEW;
	@NotNull private Map<Integer, Consumer<Integer>> m_keyHandlers = new HashMap<>(8);
	@NotNull private final Stack<ZoneEditUnit> m_edits = new Stack<>();
	@Nullable private Point m_startPoint = null;
	@Nullable private Point m_currPoint = null;
	@Nullable private Point m_endPoint = null;
	@Nullable private IDynamicGfx m_selectRect = null;
	@Nullable private IDynamicGfx m_zoneAreaTiles = null;
	@Nullable private IGfxAttribute m_zoneAreaTilesFillAttr = null;
	@NotNull private ZoneExtentStoreCopier m_zoneExtentStoreCopier = new ZoneExtentStoreCopier();

	public EditUserDefinedZonesAction(ICapletController controller)
	{
		super(controller);
	}

	private enum ZoneEditStyle
	{
		EDIT {
			@Override IZoneArea.ZoneEdit getStyle()
			{
				return IZoneArea.ZoneEdit.EDIT;
			}

			@Override public String getResourceKey()
			{
				return "EditUserDefinedZonesAction.mode.redraw.name";
			}

			@Override public String getCursor()
			{
				return "chs/images/general/cur_user_zone_edit.png";
			}
		},
		NEW {
			@Override IZoneArea.ZoneEdit getStyle()
			{
				return IZoneArea.ZoneEdit.NEW;
			}

			@Override public String getResourceKey()
			{
				return "EditUserDefinedZonesAction.mode.new.name";
			}

			@Override public String getCursor()
			{
				return "chs/images/general/cur_user_zone_new.png";
			}

			@Override public String getStartHintKey()
			{
				return "EditUserDefinedZonesAction.start.new.hint";
			}
		},
		DRAG {
			@Override IZoneArea.ZoneEdit getStyle()
			{
				return IZoneArea.ZoneEdit.DRAG;
			}

			@Override public String getResourceKey()
			{
				return "EditUserDefinedZonesAction.mode.drag.name";
			}

			@Override public String getCursor()
			{
				return "chs/images/general/cur_user_zone_copy.png";
			}
		},
		COPY {
			@Override IZoneArea.ZoneEdit getStyle()
			{
				return IZoneArea.ZoneEdit.COPY;
			}

			@Override public String getResourceKey()
			{
				return "EditUserDefinedZonesAction.mode.copy.name";
			}

			@Override public String getCursor()
			{
				return "chs/images/general/cur_user_zone_copy.png";
			}
		};

		abstract IZoneArea.ZoneEdit getStyle();

		abstract String getResourceKey();

		String getStartHintKey()
		{
			return "EditUserDefinedZonesAction.start.others.hint";
		}

		abstract String getCursor();
	}

	public String getActionUIClass()
	{
		return EditUserDefinedZonesActionUI.class.getName();
	}

	@Override protected Class<?> snappingSource()
	{
		return Object.class;
	}

	public String getStatusbarText()
	{
		return ResourceMgr.getString(MarkDrawableAction.class, "EditUserDefinedZonesAction.Statusbar.Text");
	}

	@Override public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() > 1) {
			terminateAction();
		}
		else {
			if (m_startPoint != null) {
				m_endPoint = deviceToWorld(e);
				m_edits.push(new ZoneEditUnit(m_editStyle.getStyle(), m_startPoint, m_endPoint));
				m_startPoint = null;
				m_endPoint = null;
			}
			else {
				m_startPoint = deviceToWorld(e);
			}
			regenerateTransientZoneArea(m_currPoint);
		}
	}

	private Point deviceToWorld(MouseEvent e)
	{
		Point mousePt = CAFUtils.getInstance().getWorldPoint(e.getPoint(), e.getSource());
		return adustToEditStyle(mousePt);
	}

	@NotNull private Point adustToEditStyle(@NotNull Point mousePt)
	{
		if (m_editStyle.getStyle().isOrthogonal() && m_startPoint != null) {
			//we will make it a orthogonal line only.
			if (Math.abs(mousePt.x - m_startPoint.x) < Math.abs(mousePt.y - m_startPoint.y)) {
				return new Point(m_startPoint.x, mousePt.y);
			}
			else {
				return new Point(mousePt.x, m_startPoint.y);
			}
		}
		return mousePt;
	}

	private void updateTransientView()
	{
		getSnapHelper().clearSnapTransientGraphics();
		updateTooltip();
		invalidateTransientView();
	}

	private void invalidateTransientView()
	{
		ICapletView view = CAFUtils.getInstance().getActiveCapletView();
		if (view != null) {
			view.invalidate(IViewInvalidationEnum.eTransient);
		}
	}

	private void appendOptionTooltip(@NotNull StringBuilder tooltipText, @NotNull ZoneEditStyle candidate,
			@NotNull String resourceKey)
	{
		String optionText = "<br>" + ResourceMgr.getString(EditUserDefinedZonesAction.class, resourceKey);
		boolean current = m_editStyle.equals(candidate);
		tooltipText.append(current ? optionText + "&#10004;" : optionText);
	}

	private void appendOptionsTooltip(@NotNull StringBuilder tooltipText, boolean main, boolean backup)
	{
		if (main) {
			appendOptionTooltip(tooltipText, ZoneEditStyle.NEW, "EditUserDefinedZonesAction.options.text.N");
			appendOptionTooltip(tooltipText, ZoneEditStyle.EDIT, "EditUserDefinedZonesAction.options.text.E");
			appendOptionTooltip(tooltipText, ZoneEditStyle.COPY, "EditUserDefinedZonesAction.options.text.C");
			appendOptionTooltip(tooltipText, ZoneEditStyle.DRAG, "EditUserDefinedZonesAction.options.text.D");
		}
		if (backup) {
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(EditUserDefinedZonesAction.class,
					"EditUserDefinedZonesAction.backup.hint"));
		}
	}

	private void updateTooltip()
	{
		GfxView view = (GfxView) CAFUtils.getInstance().getActiveCapletView();
		if (view == null) {
			return;
		}
		view.clearPopupTooltip();

		IBorder border = CommonUtils.cast(((ISymbolModel) getModel()).getSymbolDef(), IBorder.class);
		if (border == null) {
			assert false;
			return;
		}

		StringBuilder tooltipText = new StringBuilder("<html>");
		if (m_startPoint != null) {
			String modeText = ResourceMgr.getString(EditUserDefinedZonesAction.class, m_editStyle.getResourceKey());
			tooltipText.append("<b>").append(modeText).append("</b>");
			tooltipText.append("<br>");
			tooltipText.append(ResourceMgr.getString(EditUserDefinedZonesAction.class,
					"EditUserDefinedZonesAction.end.hint"));
			appendOptionsTooltip(tooltipText, false, true);
		}
		else {
			String hintText = ResourceMgr.getString(EditUserDefinedZonesAction.class, m_editStyle.getStartHintKey());
			tooltipText.append(hintText);
			appendOptionsTooltip(tooltipText, true, !m_edits.isEmpty());
		}
		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.background.hint"));
		tooltipText.append("<br>");
		tooltipText.append(ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.commit.hint"));
		tooltipText.append("</html>");

		IExtent usableArea = border.getUsableArea();
		int tooltipShift = GfxUtils.TOOLTIP_SHIFT;
		Rectangle visibleArea = view.getDrawingComponent().getVisibleDiagramArea();
		int visibleAreaLeftX = CommonUtils.toInteger(visibleArea.getX());
		int visibleTop = CommonUtils.toInteger(visibleArea.getY() + visibleArea.getHeight());
		int boundY = Math.min(usableArea.getTop(), visibleTop);
		Point devicePoint = view.worldToDevice(new Point(visibleAreaLeftX, boundY));
		int x = tooltipShift + devicePoint.x;
		int y = tooltipShift + devicePoint.y + TOOLTIP_HEIGHT;
		view.showTooltipAtLocation(tooltipText.toString(), new Point(x, y));
	}

	private void discardSelectArea()
	{
		if (m_selectRect != null) {
			getDynamicGfxService().removeTransientGfx(m_selectRect);
			m_selectRect = null;
			invalidateTransientView();
		}
	}

	private void discardZoneAreaTilesArea()
	{
		if (m_zoneAreaTiles != null) {
			getDynamicGfxService().removeTransientGfx(m_zoneAreaTiles);
			m_zoneAreaTiles = null;
			invalidateTransientView();
		}
	}

	@Override public void mouseDragged(MouseEvent e)
	{
		m_currPoint = deviceToWorld(e);
		regenerateTransientZoneArea(m_currPoint);
	}

	@Override public void mouseMoved(MouseEvent e)
	{
		m_currPoint = deviceToWorld(e);
		regenerateTransientZoneArea(m_currPoint);
	}

	private void regenerateTransientZoneArea(@Nullable Point currPoint)
	{
		discardSelectArea();
		IUserDefinedZoneFactory userDefinedZoneFactory = constructUserDefinedZoneFactory();
		IZoneExtentStore storeCopy =
				m_zoneExtentStoreCopier.constructZoneExtentStoreCopy(getZoneArea(), userDefinedZoneFactory);
		if (storeCopy != null && (!m_edits.isEmpty() || currPoint != null)) {
			IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IDynamicGfxFactory transFactory = new DynamicGfxFactoryHelper(drawFactory);

			m_selectRect = transFactory.constructCompound(FactoryMgr.getCommonFactory().constructLocation(0, 0));
			assert m_selectRect != null;
			IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
			gfxAttr.setColor(drawFactory.constructColorRGB(0, 0, RGB_COMPONENT_MAX_VAL));
			gfxAttr.setThickness(new LogicalGraphicSize(IZoneArea.GRID_THICKNESS));
			m_selectRect.setAttribute(gfxAttr);

			if (m_startPoint != null && currPoint != null) {
				IDynamicGfx selectedArea = m_editStyle.getStyle().isLinear() ?
						transFactory.constructLine(m_startPoint, currPoint, new Point(0, 0), false) :
						transFactory.constructRectangle(m_startPoint, currPoint, new Point(0, 0), false);
				//this should not inherit the color from parent.
				IWritableGfxAttribute selectAreaColor = drawFactory.createGfxAttribute();
				selectAreaColor.setColor(drawFactory.constructColor("transient"));
				selectedArea.setAttribute(selectAreaColor);
				m_selectRect.addObject(selectedArea);
			}
			else if (currPoint != null) {
				IZoneArea zoneArea = getZoneArea();
				if (zoneArea != null) {
					IZoneExtent zoneExtent = zoneArea.findZoneExtent(currPoint);
					if (zoneExtent != null) {
						Point leftBottom = new Point(zoneExtent.getLeft(), zoneExtent.getBottom());
						Point rightTop = new Point(zoneExtent.getRight(), zoneExtent.getTop());
						IDynamicGfx selectedArea =
								transFactory.constructRectangle(leftBottom, rightTop, new Point(0, 0), false);
						IWritableGfxAttribute gfxAttribute = drawFactory.createGfxAttribute();
						gfxAttribute.setThickness(new LogicalGraphicSize(2));
						selectedArea.setAttribute(gfxAttribute);
						m_selectRect.addObject(selectedArea);
					}
				}
			}

			for (ZoneEditUnit edit : m_edits) {
				storeCopy.regenerateUserDefinedZones(edit.getStartPoint(), edit.getEndPoint(), edit.getEditStyle(),
						userDefinedZoneFactory);
			}

			if (m_startPoint != null && currPoint != null) {
				storeCopy.regenerateUserDefinedZones(m_startPoint, currPoint, m_editStyle.getStyle(),
						userDefinedZoneFactory);
			}

			IGridDisplayHandler displayHandler = constructGridDisplayHandler(transFactory, m_selectRect);

			storeCopy.processUserDefinedZonesGfx(displayHandler);

			getDynamicGfxService().addTransientGfx(m_selectRect);
		}
		m_zoneExtentStoreCopier.dispose(storeCopy, userDefinedZoneFactory);
		updateTransientView();
	}

	@NotNull private IGridDisplayHandler constructGridDisplayHandler(@NotNull IDynamicGfxFactory transFactory,
			@NotNull IDynamicGfx container)
	{
		return new IGridDisplayHandler()
		{
			@Override public void rectangle(int left, int bottom, int right, int top)
			{
				IDynamicGfx rect =
						transFactory.constructRectangle(new Point(left, bottom), new Point(right, top),
								new Point(0, 0), false);
				rect.hideMarkers();
				container.addObject(rect);
			}

			@Override public void line(int x1, int y1, int x2, int y2)
			{
				IDynamicGfx line = transFactory.constructLine(new Point(x1, y1), new Point(x2, y2),
						new Point(0, 0), false);
				line.hideMarkers();
				container.addObject(line);
			}
		};
	}

	@NotNull private IUserDefinedZoneFactory constructUserDefinedZoneFactory()
	{
		return new AbstractUserDefinedZoneFactory()
		{
			@NotNull @Override public IUserDefinedZone createUserDefinedZone(@NotNull String name)
			{
				IUserDefinedZone userDefinedZone =
						FactoryMgr.getSymbolFactory().constructUserDefinedZone(FactoryMgr.createUID());
				userDefinedZone.setName(name);
				register(userDefinedZone);
				return userDefinedZone;
			}
		};
	}

	private static class ZoneExtentStoreCopier
	{

		public void reset()
		{
		}

		@Nullable public IZoneExtentStore constructZoneExtentStoreCopy(@Nullable IZoneArea zoneArea,
				@NotNull IUserDefinedZoneFactory userDefinedZoneFactory)
		{
			Function<IUserDefinedZone, IUserDefinedZone> userZoneCopy =
					z -> userDefinedZoneFactory.createUserDefinedZone(StringUtils.nonNull(z.getName()));
			return zoneArea != null ? zoneArea.getZoneExtentStoreCopy(userZoneCopy) : null;
		}

		private void collectUserDefinedZones(@Nullable IZoneExtentStore store,
				@NotNull Set<IUserDefinedZone> userDefinedZones)
		{
			if (store != null) {
				Collection<IZoneExtent> allZoneExtents = store.getAllZoneExtents();
				for (IZoneExtent zoneExtent : allZoneExtents) {
					IUserDefinedZone userDefined = zoneExtent.getUserDefined();
					if (userDefined != null) {
						userDefinedZones.add(userDefined);
					}
				}
			}
		}

		public void dispose(@Nullable IZoneExtentStore copy, @NotNull IUserDefinedZoneFactory userDefinedZoneFactory)
		{
			//LOGIC2017-679: must delete user defined zones because they are being held by CDH. and would cause OutOfMemory issue.
			userDefinedZoneFactory.dispose(copy != null ? copy.getAllZoneExtents() : Collections.emptyList());
			Set<IUserDefinedZone> zonesToPurge = new HashSet<>();
			collectUserDefinedZones(copy, zonesToPurge);
			for (IUserDefinedZone userDefinedZone : zonesToPurge) {
				userDefinedZone.delete();
			}
			if (copy != null) {
				for (IZoneExtent zoneExtent : copy.getAllZoneExtents()) {
					zoneExtent.setUserDefined(null);
				}
			}
		}
	}

	public void populateCtxMenu(ActionContainer container, @Nullable SelectSet selections)
	{
		Action actionUI = getActionUI();
		if (actionUI != null && areZoneExtentsPresent()) {
			container.add(new ActionEntry(actionUI));
		}
	}

	@Override public boolean isEnabled()
	{
		return areZoneExtentsPresent() && super.isEnabled();
	}

	protected boolean areZoneExtentsPresent()
	{
		IZoneArea zoneArea = getZoneArea();
		return zoneArea != null && !zoneArea.getZoneExtents().isEmpty();
	}

	public void populateActiveCtxMenu(ActionContainer container)
	{
		// Put an entry on the menu to increment the radius by 10
		AbstractAction act = new BackupAction(this);

		String backupActionName = ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.backup.action.name");
		String backupActionDesc = ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.backup.action.description");

		act.putValue(Action.NAME, backupActionName);
		act.putValue(Action.SHORT_DESCRIPTION, backupActionDesc);
		act.putValue(Action.LONG_DESCRIPTION, backupActionDesc);

		//putValue(SMALL_ICON, icon);
		KeyStroke accel = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));

		// Put an entry on the menu to increment the radius by 10
		act = new CommitAction(this);
		String commitActionName = ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.commit.action.name");
		String commitActionDesc = ResourceMgr.getString(EditUserDefinedZonesAction.class,
				"EditUserDefinedZonesAction.commit.action.description");

		act.putValue(Action.NAME, commitActionName);
		act.putValue(Action.SHORT_DESCRIPTION, commitActionDesc);
		act.putValue(Action.LONG_DESCRIPTION, commitActionDesc);

		//putValue(SMALL_ICON, icon);
		accel = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		act.putValue(Action.ACCELERATOR_KEY, accel);
		container.add(new ActionEntry(act));
	}

	private class BackupAction extends AbstractContextAction
	{

		protected BackupAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(EditUserDefinedZonesAction.class,
					"EditUserDefinedZonesAction.backup.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			backup();
		}

		public boolean isEnabled()
		{
			return (m_startPoint != null || !m_edits.isEmpty());
		}
	}

	private class CommitAction extends AbstractContextAction
	{

		protected CommitAction(IApplicationSpecificationAction parent)
		{
			super(parent, ResourceMgr.getString(EditUserDefinedZonesAction.class,
					"EditUserDefinedZonesAction.commit.action.name"));
		}

		public void actionPerformed(ActionEvent e)
		{
			completeAction();
		}

		public boolean isEnabled()
		{
			return EditUserDefinedZonesAction.this.isEnabled();
		}
	}

	private void terminateAction()
	{
		getController().getActionMgr().terminateActiveAction(true);
	}

	private void completeAction()
	{
		if (m_startPoint != null && m_currPoint != null) {
			m_endPoint = m_currPoint;
			m_edits.push(new ZoneEditUnit(m_editStyle.getStyle(), m_startPoint, m_endPoint));
			m_startPoint = null;
			m_endPoint = null;
		}
		terminateAction();
	}

	public Cursor getCursor()
	{
		return m_cursors.getOrDefault(m_editStyle, super.getCursor());
	}

	private void loadCursors()
	{
		for (ZoneEditStyle editStyle : ZoneEditStyle.values()) {
			String imagePath = editStyle.getCursor();
			Cursor cursor = CAFUtils.getInstance().loadCursorDirect(getClass(), imagePath, new Point(7, 7));
			m_cursors.put(editStyle, cursor);
		}
	}

	@Override public IActionEnum onActivate(ActionEvent e)
	{
		resetTransientInformation();
		setupKeyHandlers();
		loadCursors();
		setCursor(getCursor());

		IUserDefinedZoneFactory userDefinedZoneFactory = constructUserDefinedZoneFactory();
		IZoneExtentStore storeCopy =
				m_zoneExtentStoreCopier.constructZoneExtentStoreCopy(getZoneArea(), userDefinedZoneFactory);
		if (storeCopy != null) {
			IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
			IDynamicGfxFactory transFactory = new DynamicGfxFactoryHelper(drawFactory);
			m_zoneAreaTiles = transFactory.constructCompound(FactoryMgr.getCommonFactory().constructLocation(0, 0));
			assert m_zoneAreaTiles != null;
			m_zoneAreaTilesFillAttr = createBackgroundGfxHideAttribute();
			IGridDisplayHandler displayHandler = constructGridDisplayHandler(transFactory, m_zoneAreaTiles);
			storeCopy.processAllZoneExtentsGridGfx(displayHandler);
			getDynamicGfxService().addTransientGfx(m_zoneAreaTiles);
		}
		m_zoneExtentStoreCopier.dispose(storeCopy, userDefinedZoneFactory);
		return super.onActivate(e);
	}

	@NotNull private IWritableGfxAttribute createBackgroundGfxHideAttribute()
	{
		IDrawFactory drawFactory = FactoryMgr.getDrawFactory();
		IWritableGfxAttribute gfxAttr = drawFactory.createGfxAttribute();
		IColor fillColor = drawFactory.constructColor("pageborder");
		gfxAttr.setFillForegroundColor(fillColor);
		gfxAttr.setFillBackgroundColor(fillColor);
		gfxAttr.setFillPattern(IFillPattern.PATTERN_SOLID);
		return gfxAttr;
	}

	private void setupKeyHandlers()
	{
		m_keyHandlers.put(KeyEvent.VK_E, (t) -> {
			setupNewEditMode(ZoneEditStyle.EDIT);
		});

		m_keyHandlers.put(KeyEvent.VK_N, (t) -> {
			setupNewEditMode(ZoneEditStyle.NEW);
		});

		m_keyHandlers.put(KeyEvent.VK_C, (t) -> {
			setupNewEditMode(ZoneEditStyle.COPY);
		});

		m_keyHandlers.put(KeyEvent.VK_D, (t) -> {
			setupNewEditMode(ZoneEditStyle.DRAG);
		});

		m_keyHandlers.put(KeyEvent.VK_BACK_SPACE, (t) -> {
			backup();
		});

		m_keyHandlers.put(KeyEvent.VK_ENTER, (t) -> {
			completeAction();
		});

		m_keyHandlers.put(KeyEvent.VK_G, (t) -> {
			if (m_zoneAreaTiles != null && m_zoneAreaTilesFillAttr != null) {
				if (m_zoneAreaTilesFillAttr.equals(m_zoneAreaTiles.getAttribute())) {
					m_zoneAreaTiles.setAttribute(FactoryMgr.getDrawFactory().createGfxAttribute());
				}
				else {
					m_zoneAreaTiles.setAttribute(m_zoneAreaTilesFillAttr);
				}
				regenerateTransientZoneArea(m_currPoint);
			}
		});
	}

	private void setupNewEditMode(ZoneEditStyle redraw)
	{
		m_editStyle = redraw;
		if (m_currPoint != null) {
			m_currPoint = adustToEditStyle(m_currPoint);
		}
		regenerateTransientZoneArea(m_currPoint);
		setCursor(getCursor());
	}

	@Override public boolean onTerminate(boolean successful)
	{
		if (successful) {
			IZoneAreaObject border = getZoneAreaObject();
			//edit model only when param is successful. otherwise undo/redo will be messed-up.
			IZoneArea zoneArea = border.getZoneArea();
			if (zoneArea != null) {
				zoneArea.regenerateUserDefinedZones(m_edits, border);
			}
		}
		resetTransientInformation();
		return successful;
	}

	@Nullable private IZoneArea getZoneArea()
	{
		return getZoneAreaObject().getZoneArea();
	}

	@NotNull private IZoneAreaObject getZoneAreaObject()
	{
		IZoneAreaObject border = CommonUtils.cast(((ISymbolModel) getModel()).getSymbolDef(), IZoneAreaObject.class);
		assert border != null;
		return border;
	}

	public void keyPressed(KeyEvent e)
	{
		int keyCode = e.getKeyCode();
		Consumer<Integer> keyHandler = m_keyHandlers.get(keyCode);
		if (keyHandler != null) {
			keyHandler.accept(keyCode);
		}
		else {
			super.keyPressed(e);
		}
	}

	private void backup()
	{
		if (m_startPoint != null) {
			m_startPoint = null;
		}
		else if (!m_edits.isEmpty()) {
			ZoneEditUnit editUnit = m_edits.pop();
			m_startPoint = editUnit.getStartPoint();
			for (ZoneEditStyle value : ZoneEditStyle.values()) {
				if (value.getStyle().equals(editUnit.getEditStyle())) {
					m_editStyle = value;
					break;
				}
			}
		}
		regenerateTransientZoneArea(m_currPoint);
	}

	private void resetTransientInformation()
	{
		discardSelectArea();
		discardZoneAreaTilesArea();
		m_zoneExtentStoreCopier.reset();
		m_zoneAreaTilesFillAttr = null;
		m_editStyle = ZoneEditStyle.NEW;
		m_keyHandlers.clear();
		m_startPoint = null;
		m_currPoint = null;
		m_endPoint = null;
		m_edits.clear();
		m_cursors.clear();
	}
}