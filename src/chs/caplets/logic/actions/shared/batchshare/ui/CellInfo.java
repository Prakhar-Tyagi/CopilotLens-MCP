/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.shared.batchshare.ui;

import chs.caplets.logic.actions.shared.batchshare.ShareabilityStatus;
import chs.utilities.ResourceMgr;
import com.mentor.capital.javafx.table.ColumnInformation;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Class to manage cell states based on batch share conditions
 */
public class CellInfo
{

	@Nullable private ColumnInformation<IBatchShareRow> column;
	private final IBatchShareRow rowItem;
	private StringJoiner tooltip = new StringJoiner("\n");
	private boolean isEditable = true;

	public CellInfo(@Nullable IBatchShareRow rowItem, @Nullable ColumnInformation<IBatchShareRow> column)
	{
		this.rowItem = rowItem;
		this.column = column;
		validateCellInfo();
	}

	private void validateCellInfo()
	{
		if (column == null || rowItem == null) {
			isEditable = false;
			return;
		}

		IBatchShareGroup group = rowItem.getGroup();
		ShareabilityStatus status = group.getStatus();

		if (status == ShareabilityStatus.FROZEN_TARGET_SHARED_OBJECT) {
			isEditable = false;
			tooltip.add(ResourceMgr.getString(this, "cellinfo.frozentargetobject.msg"));
		}
		else if (status == ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECTS) {
			isEditable = false;
			tooltip.add(ResourceMgr.getString(this, "cellinfo.multipletargetobj.msg"));
		}
		else if (status == ShareabilityStatus.MULTIPLE_TARGET_SHARED_OBJECT_REVISIONS) {
			isEditable = false;
			tooltip.add(ResourceMgr.getString(this, "cellinfo.multipletargetobjrevisions.msg"));
		}

		if (BatchShareColumn.ACTION.getName().equals(column.getName())) {
			if (group.getTargetSharedObjectName() != null) {
				isEditable = false;
				tooltip.add(ResourceMgr.getString(this, "cellinfo.targetexists.msg",group.getTargetSharedObjectName()));
			}
			if (rowItem.isSelected() && group.getTargetSharedObjectName() == null) {
				Set<IBatchShareRow> selectedElements =
						group.getBatchShareElements().stream().filter(element -> element.isSelected())
								.collect(
										Collectors.toSet());

				if (selectedElements.size() == 1) {
					isEditable = false;
					tooltip.add(ResourceMgr.getString(this, "cellinfo.lastobject.msg"));
				}
			}
			if(!rowItem.isSelected()){
				isEditable = false;
			}
		}
	}

	public boolean isEditable()
	{
		return isEditable;
	}

	@Nullable public String getTooltip()
	{
		return tooltip.toString();
	}
}
