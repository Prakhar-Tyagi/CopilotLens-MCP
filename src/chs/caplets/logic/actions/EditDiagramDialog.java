/*
 * Copyright 2005-2012 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */
package chs.caplets.logic.actions;

import chs.cof.logical.schem.ISchemDiagram;
import chs.common.IProperty;
import chs.common.attr.IAttributeTypes;
import chs.utilities.StringUtils;
import chs.utility.audit.AuditableEventType;
import chs.utility.audit.DiagramAuditTrialHelper;
import chs.utility.helpers.SystemPreferencesHelper;
import org.jetbrains.annotations.NotNull;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class EditDiagramDialog extends EditDiagramPropertiesDialog
{


	public EditDiagramDialog(Frame frame, String title, @NotNull ISchemDiagram diagram, boolean editable)
	{
		super(frame, title, diagram.getDesign(), diagram, editable);
	}

	protected Collection<IProperty> getInitialProperties()
	{
		final ISchemDiagram diagram = getDiagram();
		List<IProperty> props = new ArrayList<IProperty>(diagram.getNumProperties());
		for (IProperty prop : diagram.getProperties()) {
			props.add(prop);
		}
		return props;
	}

	@NotNull
	public static String getDescriptionAsCommaSeparatedText(@NotNull StringBuilder modifiedUserAccountDescription, @NotNull String description) {
		if (modifiedUserAccountDescription.length() > 0) {
			return ", " + description;
		}
		return description;
	}

	/**
	 * Save the changes to diagram object
	 *
	 * @param deletedPropSet => return any deleted properties
	 *
	 * @return true if there is a change
	 */
	public boolean setChangedInfo(Set<IProperty> deletedPropSet)
	{
		final ISchemDiagram diagram = getDiagram();
		StringBuilder description = new StringBuilder();
		boolean changed = false;
		if (isNameChanged()) {
			diagram.setName(getEditedDiagramName());
			description.append(getDescriptionAsCommaSeparatedText(description, "Name updated"));
			changed = true;
		}

		if(SystemPreferencesHelper.areSectorsSupported()) {
			if (isDiagramLocationChanged()) {
				String oldLocationValue = getDiagram().getIECLocation();
				String newLocationValue = getDiagramLocation();
				diagram.setIECLocation(newLocationValue);
				if (!StringUtils.equals(StringUtils.ensureNotNull(oldLocationValue), newLocationValue)) {
					description.append(getDescriptionAsCommaSeparatedText(description, "Location updated"));
				}
				diagram.propagateAttributeChange(IAttributeTypes.IEC_LOCATION,
						StringUtils.ensureNotNull(oldLocationValue), StringUtils.ensureNotNull(newLocationValue));

				changed = true;
			}
			if (isDiagramFunctionChanged()) {
				String oldFunctionValue = getDiagram().getIECFunction();
				String newFunctionValue = getDiagramFunction();
				diagram.setIECFunction(newFunctionValue);
				if (!StringUtils.equals(StringUtils.ensureNotNull(oldFunctionValue), newFunctionValue)) {
					description.append(getDescriptionAsCommaSeparatedText(description, "Function updated"));
				}
				diagram.propagateAttributeChange(IAttributeTypes.IEC_FUNCTION,
						StringUtils.ensureNotNull(oldFunctionValue), StringUtils.ensureNotNull(newFunctionValue));
				changed = true;
			}
		}

		if (isStyleSetChanged()) {
			changed = true;
			description.append(getDescriptionAsCommaSeparatedText(description, "Style set updated"));
			diagram.setPreferenceSetName(getEditedDiagramStyleSet().getName());
		}

		if (editProperties(diagram, deletedPropSet)) {
			description.append(getDescriptionAsCommaSeparatedText(description, "Properties updated"));
			changed = true;
		}

		if (changed) {
			DiagramAuditTrialHelper.getInstance()
					.storeDiagramAuditTrail(diagram, AuditableEventType.DIAGRAM_ATTRIBUTES_CHANGED,
							description.toString());
		}

		return changed;
	}

	protected Collection<String> getNamesToAvoid()
	{
		final ISchemDiagram theDiagram = getDiagram();
		List<String> existingNames = new ArrayList<String>(getDesign().getNumDiagrams());
		for (ISchemDiagram diagram : getDesign().getDiagrams()) {
			if (diagram == theDiagram) {
				continue;
			}
			existingNames.add(diagram.getName());
		}
		return existingNames;
	}
}
