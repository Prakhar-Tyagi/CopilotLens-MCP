/*
 * Copyright 2020 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.actionreport;

import chs.caf.CAFUtils;
import chs.caf.IOutputWindow;
import chs.common.IAttributePropertyProvider;
import chs.common.attr.IAttributeTypes;
import chs.utilities.ResourceMgr;
import com.mentor.chs.api.IXAttributes;
import com.mentor.iesd.reports.utils.IAttributeNames;
import org.jetbrains.annotations.NotNull;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UI Components for merge changes display
 */
public class MergeActionChangeDisplayHelper extends MergeActionChangeReporter
{

	public static final String NOT_APPLICABLE = "N/A";
	public static final String HYPERLINK_SEPERATOR = "&";
	public static final String COLON_SEPERATOR = ":";
	public final String target_value_prevail =
			ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.table.targetvalues");

	public static final String[] Excluded = {
			IAttributeTypes.PART_ASSIGNED,
			IAttributeTypes.NAME_OVERRIDDEN,
			IAttributeTypes.GENERATED_NAME,
			IAttributeTypes.DEFAULT_NAME,
			IAttributeTypes.SHARED_OBJECT_REVISION,
			IAttributeTypes.DESIGN,
			IAttributeTypes.DESIGN_ABSTRACTION,
			IAttributeTypes.UNIT_OF_MEASURE,
			IAttributeTypes.GROUP_NAME,
			IXAttributes.DerivedFootprintType,
			IAttributeTypes.BOM_ID,
			IAttributeTypes.GENERATED_FM_CODE,
			IAttributeTypes.USER_PM_CODE,
			IAttributeTypes.GENERATED_PM_CODE,
			IAttributeTypes.NUM_PINS
	};

	public static final String[] Warning = {
			IAttributeNames.AnalysisModelAtt,
			AttrPropExtractor.ANALYSABLE_SYMBOL,
			AttrPropExtractor.ANALYSIS_INTERFACE,
			AttrPropExtractor.ANALYSIS_FAILURES,
			AttrPropExtractor.ANALYSIS_PIN_MAPPING,
			IAttributeTypes.ASSEMBLY
	};
	public static final Set<String> EXCLUDED_SET = new HashSet<>(Arrays.asList(Excluded));
	public static final Set<String> WARNING_SET = new HashSet<>(Arrays.asList(Warning));

	@Override public void reportChanges()
	{
		IOutputWindow ow = CAFUtils.getInstance().getOutputWindow();
		if (ow == null || mChangeComparisonObjects.isEmpty()) {
			return;
		}

		IMergeComparison<IMergeActionChange, IAttributePropertyProvider> mainChange = mChangeComparisonObjects.get(0);
		ICachedObject primarySourceObject = mainChange.getSourceObject();
		ICachedObject primaryMergeObject = mainChange.getMergedObject();
		ICachedObject primaryTargetObject = mainChange.getTargetObject();

		if (primaryMergeObject == null || primarySourceObject == null || primaryTargetObject == null) {
			return;
		}
		Component displayPanel = constructChangesPanel(primarySourceObject, primaryMergeObject, primaryTargetObject);
		String title = ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.tab.title");
		ow.removePane(title);
		ow.addComponentPane(title, displayPanel, false);
		ow.setActivePaneForced(title);
	}

	@NotNull protected Component constructChangesPanel(@NotNull ICachedObject sourceObject, @NotNull ICachedObject mergedObject,
			@NotNull ICachedObject targetObject)
	{
		String hyperLink = mergedObject.getDesignUID() + HYPERLINK_SEPERATOR + mergedObject.getObjectInfo().getUID();
		Collection<IMergeReportTableDataRow> tableData = getTableData();
		StringBuilder name = new StringBuilder(mergedObject.getAttributes().get(IXAttributes.Name));
		if (targetObject.isSharedObject()) {
			String revision = targetObject.getAttributes().get(IAttributeTypes.SHARED_OBJECT_REVISION);
			if (revision != null) {
				name.append(COLON_SEPERATOR).append(revision);
			}
			return new MergeChangesReportTab(sourceObject.getAttributes().get(IXAttributes.Name) + " " +
					ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.header.share"),
					hyperLink,
					name.toString(),
					ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.header.attrchange.share"),
					tableData);
		}
		else {
			return new MergeChangesReportTab(sourceObject.getAttributes().get(IXAttributes.Name) + " " +
					ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.header.merge"),
					hyperLink,
					name.toString(),
					ResourceMgr.getString(this, "MergeActionChangeDisplayHelper.header.attrchange.merge"),
					tableData);
		}
	}
	@NotNull protected Collection<IMergeReportTableDataRow> getTableData()
	{
		List<IMergeReportTableDataRow> data = new ArrayList<>();
		for (IMergeComparison<IMergeActionChange, IAttributePropertyProvider> objs : mChangeComparisonObjects) {
			for (IMergeActionChange ch : objs.computeChanges()) {

				String objectType = ch.sourceObjectType();
				StringBuilder objectName = new StringBuilder();
				objectName.append(objectType).append(":").append(" ").append("[").append(ch.getSourceObjectName())
						.append("]");
				String key = ch.getKey();
				String initialValue = ch.getInitialValue();
				String initialTargetValue = ch.getInitialTargetValue();
				String transformedValue = ch.getTransformedValue();
				String details = ch.getDetails();
				IActionChange.ComparisonField keyType = ch.getKeyType();
				String displayName = AttrPropExtractor.attributeDisplayNameMap.get(key);
				String displayKeyValue = key;
				if (displayName != null) {
					displayKeyValue = displayName;
				}

				if (IActionChange.ComparisonField.Property.equals(keyType)) {
					data.add(new MergeReportTableDataRow(key, initialValue, initialTargetValue,
							transformedValue,
							IReportTableDataRow.DisplayInformationType.Information, objectName.toString(), details));
					continue;
				}
				if (WARNING_SET.contains(key)) {
					if (AttrPropExtractor.ANALYSIS_INTERFACE.equals(key)) {
						data.add(new MergeReportTableDataRow(ResourceMgr
								.getString(this, "MergeActionChangeDisplayHelper.attribute.analysisinterface"),
								NOT_APPLICABLE, NOT_APPLICABLE, target_value_prevail,
								IReportTableDataRow.DisplayInformationType.Warning, objectName.toString(), details));
						continue;
					}
					if (AttrPropExtractor.ANALYSIS_PIN_MAPPING.equals(key)) {
						data.add(new MergeReportTableDataRow(ResourceMgr
								.getString(this, "MergeActionChangeDisplayHelper.attribute.analysispinmapping"),
								NOT_APPLICABLE, NOT_APPLICABLE, target_value_prevail,
								IReportTableDataRow.DisplayInformationType.Warning, objectName.toString(), details));
						continue;
					}
					if (AttrPropExtractor.ANALYSIS_FAILURES.equals(key)) {
						data.add(new MergeReportTableDataRow(ResourceMgr
								.getString(this, "MergeActionChangeDisplayHelper.attribute.analysisfailures"),
								NOT_APPLICABLE, NOT_APPLICABLE,
								target_value_prevail,
								IReportTableDataRow.DisplayInformationType.Warning, objectName.toString(), details));
						continue;
					}
					if (AttrPropExtractor.ANALYSABLE_SYMBOL.equals(key)) {
						displayKeyValue = ResourceMgr
								.getString(this, "MergeActionChangeDisplayHelper.attribute.analysispinmapping");
					}
					data.add(new MergeReportTableDataRow(displayKeyValue, initialValue, initialTargetValue,
							transformedValue, IReportTableDataRow.DisplayInformationType.Warning,
							objectName.toString(), details));
					continue;
				}
				if (!EXCLUDED_SET.contains(key)) {
					data.add(new MergeReportTableDataRow(displayKeyValue,
							initialValue,
							initialTargetValue,
							transformedValue,
							IReportTableDataRow.DisplayInformationType.Information, objectName.toString(), details));
				}
			}
		}

		return data;
	}
}

