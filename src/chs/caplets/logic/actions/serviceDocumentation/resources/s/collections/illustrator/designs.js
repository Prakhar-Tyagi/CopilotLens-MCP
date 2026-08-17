/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["harnessLayouts"],
    function (harnessLayouts)
    {
        "use strict";
        harnessLayouts.afterLoad = function (harnesses)
        {
            var designType = "old";
            _.each(harnesses, function (harness)
            {
                harness.type = designType + harness.type;
                designType = "new";
            })
        }
    }
);