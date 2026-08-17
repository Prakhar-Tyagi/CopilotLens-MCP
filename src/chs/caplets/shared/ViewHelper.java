/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2004-2023 Siemens
 */
package chs.caplets.shared;

import chs.caf.CAFUtils;
import chs.caf.ICAFWindow;
import chs.caf.caplet.CapletViewIterator;
import chs.caf.caplet.ICapletModel;
import chs.caf.caplet.ICapletView;
import chs.caf.caplet.ICapletWindow;
import chs.caplets.symbol.Model;
import chs.cof.logical.IDesign;
import chs.cof.symbol.IStamp;
import chs.utility.logic.ILogicModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA. User: lstamper Date: Jun 21, 2004 Time: 9:33:58 AM
 */
public class ViewHelper
{

	private ViewHelper()
	{
	}

	public static Iterator<ICapletView> getAllActiveDesignViews()
	{
		Collection<ICapletView> views = new ArrayList<ICapletView>();
		ICapletView capletView = CAFUtils.getInstance().getActiveCapletView();
		if (capletView != null) {
			IDesign design = ((ILogicModel) capletView.getCapletModel()).getDesign();
			for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
				if (cafWin instanceof ICapletWindow && cafWin.isDisplayed()) {
					CapletViewIterator cvIt = ((ICapletWindow) cafWin).getViews();
					while (cvIt.hasNext()) {
						ICapletView view = cvIt.getNext();
						ICapletModel capletModel = view.getCapletModel();   // check it's a Logic caplet model
						if (capletModel instanceof chs.caplets.logic.Model &&
								((ILogicModel) capletModel).getDesign() == design) {
							views.add(view);
						}
					}
				}
			}
		}
		return views.iterator();
	}

	public static Iterator<ICapletView> getAllActiveSymbolViews()
	{
		Collection<ICapletView> views = new ArrayList<ICapletView>();
		ICapletView capletView = CAFUtils.getInstance().getActiveCapletView();
		if (capletView != null) {
			IStamp symDef = ((Model) capletView.getCapletModel()).getSymbolDef();
			for (ICAFWindow cafWin : CAFUtils.getInstance().getWindowMgr().getWindows()) {
				if (cafWin instanceof ICapletWindow && cafWin.isDisplayed()) {
					CapletViewIterator cvIt = ((ICapletWindow) cafWin).getViews();
					while (cvIt.hasNext()) {
						ICapletView view = cvIt.getNext();
						if (((Model) view.getCapletModel()).getSymbolDef() == symDef) {
							views.add(view);
						}
					}
				}
			}
		}
		return views.iterator();
	}
}
