/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2024 Siemens
 */

package chs.caplets.logic.actions;

import chs.caf.CAFUtils;
import chs.caf.caplet.ICapletController;
import chs.caf.caplet.helpers.ConductorRouteAction;
import chs.caf.helpers.ui.std.DesignAbstractionHelper;
import chs.cof.logical.cable.IGeneralHighway;
import chs.cofUtils.cmd.CreateSchemGeneralHighwayCmd;
import chs.common.DesignAbstractionType;

import java.awt.Cursor;
import java.awt.Point;

/**
 * A create tool to make a general highway.
 *
 * @created May 23, 2024
 */
public class CreateGeneralHighwayAction extends CreateHighwayAction
{
    private static Cursor m_highwayCursor = null;

    public CreateGeneralHighwayAction(ICapletController controller)
    {
        super(controller);

        if (m_highwayCursor == null) {
            m_highwayCursor = CAFUtils.getInstance()
                    .loadCursor(controller.getCaplet(), "chs/images/app/cur_highway.png", new Point(7, 7));
        }

        m_cmd = new CreateSchemGeneralHighwayCmd(ConductorRouteAction.getInstance());
    }

    /**
     * Return the cursor for this action
     */
    public Cursor getCursor()
    {
        return m_highwayCursor;
    }

    protected Class snappingSource()
    {
        return IGeneralHighway.class;
    }

    public String getActionUIClass()
    {
        return CreateGeneralHighwayActionUI.class.getName();
    }

    /*
	Highway are not supported in Smart Flows
	 */
    @Override public boolean isEnabled()
    {
        if (!super.isEnabled()) {
            return false;
        }
        DesignAbstractionType designAbstraction = DesignAbstractionHelper.getTypeOfDesignAbstraction();
        return designAbstraction == null || (designAbstraction != DesignAbstractionType.SMART_FLOWS);
    }

}
