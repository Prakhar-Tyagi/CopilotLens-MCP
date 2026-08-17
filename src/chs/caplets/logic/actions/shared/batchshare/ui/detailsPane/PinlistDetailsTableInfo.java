/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui.detailsPane;

import chs.cof.drawplus.IDiagramObject;
import chs.cof.logical.schem.IPinList;
import chs.cof.logical.schem.ISchemDiagram;
import chs.utilities.CommonUtils;
import chs.utilities.ResourceMgr;
import chs.utilities.StringUtils;
import com.mentor.capital.javafx.table.ColumnInformation;
import com.mentor.capital.javafx.table.cell.ITableCell;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * @author rmahato
 */
public class PinlistDetailsTableInfo extends InstanceDetailsTableInfo
{

	private static final String DIAGRAM_COLOUM_NAME =
			ResourceMgr.getString(PinlistDetailsTableInfo.class, "PinlistDetailsTableInfo.diagramColoumnName.text");
	private static final String PINS_COLOUM_NAME =
			ResourceMgr.getString(PinlistDetailsTableInfo.class, "PinlistDetailsTableInfo.pinsColoumnName.text");
	public static final int MAX_TOOLTIP_LENGTH = 200;
	public static final int MIN_TOOLTIP_LENGTH = 30;
	public static final int TOOL_TIP_WRAP_LENGTH = 400;
	@Nullable protected String pinsColumnToolTip = null;

	public PinlistDetailsTableInfo()
	{
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, "");
		coloumValueMap.put(PINS_COLOUM_NAME, "");
	}

	public PinlistDetailsTableInfo(@NotNull ISchemDiagram diagram, @NotNull IDiagramObject diagramObject)
	{
		instanceDiagram = diagram;
		m_diagramObject = diagramObject;
		coloumValueMap = new LinkedHashMap<>();
		coloumValueMap.put(DIAGRAM_COLOUM_NAME, diagram.getName());
		coloumValueMap.put(PINS_COLOUM_NAME, getAllPinNames(diagramObject));
	}

	@NotNull @Override
	protected DetailsTableInfo createInstanceDetailObject(@NotNull ISchemDiagram diagram,
			@NotNull IDiagramObject diagramObject)
	{
		return new PinlistDetailsTableInfo(diagram, diagramObject);
	}

	@NotNull protected String getAllPinNames(@NotNull IDiagramObject diagramObject)
	{
		IPinList pinList = (IPinList) diagramObject;
		String allPinNames = pinList.getPins().stream().map(pin -> pin.getConnectivity().getName()).distinct().
				collect(Collectors.joining(", "));
		if (allPinNames.length() > MIN_TOOLTIP_LENGTH) {
			pinsColumnToolTip = StringUtils.truncateString(allPinNames, MAX_TOOLTIP_LENGTH);
		}
		return allPinNames;
	}

	@Override public void addToolTip(ITableCell<DetailsTableInfo> tableCell)
	{
		ColumnInformation<DetailsTableInfo> column = tableCell.getColumn();
		if (column != null && tableCell.getColumn().getName().equalsIgnoreCase(PINS_COLOUM_NAME)) {
			Labeled cell = CommonUtils.cast(tableCell, Labeled.class);
			if (cell != null && !StringUtils.isEmpty(pinsColumnToolTip)) {
				Tooltip tooltip = new Tooltip(pinsColumnToolTip);
				tooltip.setPrefWidth(TOOL_TIP_WRAP_LENGTH);
				tooltip.setWrapText(true);
				cell.tooltipProperty().set(tooltip);
			}
		}
	}
}
