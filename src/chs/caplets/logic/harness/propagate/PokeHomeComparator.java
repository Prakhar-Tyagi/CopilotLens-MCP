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

import chs.cof.COFTypeEnum;
import chs.cof.logical.cable.ILogicObject;

import java.util.Comparator;

/**
 * Comparator for objects in poke home message
 */
public class PokeHomeComparator implements Comparator<ILogicObject>
{

	@Override public int compare(ILogicObject o1, ILogicObject o2)
	{
		boolean shared1 = o1.isShared();
		boolean shared2 = o2.isShared();
		if (shared1 && !shared2) {
			return -1;
		}
		if (!shared1 && shared2) {
			return 1;
		}

		String objectType1 = COFTypeEnum.getDisplayableTypeName(o1);
		String objectType2 = COFTypeEnum.getDisplayableTypeName(o2);

		if (!objectType1.equals(objectType2)) {
			return objectType1.compareTo(objectType2);
		}

		String object1 = o1.getName();
		String object2 = o2.getName();

		return object1.compareTo(object2);
	}
}
