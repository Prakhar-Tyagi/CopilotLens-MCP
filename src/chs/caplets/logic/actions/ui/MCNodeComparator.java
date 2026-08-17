/*
 * Copyright 2019 Mentor Graphics Corporation
 * All Rights Reserved
 *
 * THIS WORK CONTAINS TRADE SECRET AND PROPRIETARY
 * INFORMATION WHICH IS THE PROPERTY OF MENTOR
 * GRAPHICS CORPORATION OR ITS LICENSORS AND IS
 * SUBJECT TO LICENSE TERMS.
 */

package chs.caplets.logic.actions.ui;

import chs.cof.logical.shared.ISharedConductor;
import chs.utilities.AlphaNumComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreeNode;

public class MCNodeComparator extends AlphaNumComparator<TreeNode>
{

	public int compare(TreeNode o1, TreeNode o2)
	{
		String n1 = getName(o1);
		String n2 = getName(o2);
		//
		if (n1 == null || n2 == null) {
			return 0;
		}

		int v1 = getValue(o1);
		int v2 = getValue(o2);
		if (v1 == v2) {
			// having two int values, we can't use the super method so we create a new comparator
			// for the value comparison.
			return new AlphaNumComparator<>().compare(n1, n2);
		}
		//noinspection SubtractionInCompareTo
		return (v1 - v2);
	}

	@Nullable private static String getName(TreeNode obj)
	{
		if (obj instanceof AbstractMCProxyTree) {
			return ((AbstractMCProxyTree<?>) obj).getName();
		}
		return null;
	}

	// Rank the objects how we want them to be ordered
	private static int getValue(@NotNull TreeNode obj)
	{
		if (obj instanceof AbstractMCProxyTree) {
			String role = ((AbstractMCProxyTree<?>) obj).getConductorRole();
			if (role.equals(ISharedConductor.SHIELD_TYPE)) {
				return 1;
			}
			else if (role.equals(ISharedConductor.NET_TYPE)) {
				return 2;
			}
			else if (role.equals(ISharedConductor.WIRE_TYPE)) {
				return 3;
			}
			else if (((AbstractMCProxyTree<?>) obj).isOverbraid()) {
				return 4;
			}
			else {
				return 5;
			}
		}
		return 10;
	}
}
