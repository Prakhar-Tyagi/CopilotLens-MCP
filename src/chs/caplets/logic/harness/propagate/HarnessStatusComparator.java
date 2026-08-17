/*
 * Copyright 2021 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.harness.propagate;

import chs.ctf.ui.utility.statusmessage.HarnessPropagateStatus;
import chs.ctf.ui.utility.statusmessage.IStatus;

import java.util.Comparator;

/**
 * Comparator for IHArnessUpdateStatusMessage
 */
class HarnessStatusComparator implements Comparator<IHarnessPropagateStatusMessage>
{

	@Override public int compare(IHarnessPropagateStatusMessage o1, IHarnessPropagateStatusMessage o2)
	{
		boolean editable1 = o1.isEditable();
		boolean editable2 = o2.isEditable();

		if (editable1 && !editable2) {
			return -1;
		}
		if (!editable1 && editable2) {
			return 1;
		}

		IStatus status1 = o1.getStatus();
		IStatus status2 = o2.getStatus();

		if (status1.equals(HarnessPropagateStatus.Warning) && !status2.equals(HarnessPropagateStatus.Warning)) {
			return -1;
		}

		if (!status1.equals(HarnessPropagateStatus.Warning) && status2.equals(HarnessPropagateStatus.Warning)) {
			return 1;
		}

		String design1 = o1.getDesignName();
		String design2 = o2.getDesignName();

		if (!design1.equals(design2)) {
			return design1.compareTo(design2);
		}

		String objectType1 = o1.getObjectType();
		String objectType2 = o2.getObjectType();

		if (!objectType1.equals(objectType2)) {
			return objectType1.compareTo(objectType2);
		}

		String object1 = o1.getObjectDetailText();
		String object2 = o2.getObjectDetailText();

		return object1.compareTo(object2);
	}
}
