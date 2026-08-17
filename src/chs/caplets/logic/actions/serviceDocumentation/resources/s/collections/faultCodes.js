/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/* global define, Backbone, xmlDataLoader, objectFactoryImpl, mentor*/
define("FaultCodes", ["SectionCollection", "currentPackage"],
    function (BaseCollection, currentPackage)
    {
        "use strict";
        var p = mentor.publisher;
        var FaultCodeCollection = BaseCollection.extend({
            category: p.documentCategory.FAULT_CODE,
            initialize: function ()
            {
                currentPackage.on("change:id", this.fetch, this);
                currentPackage.on("change:language", this.fetch, this);
            },
            getData: function (selectedProject)
            {
                return (selectedProject && selectedProject.getByType('faultcode')) || [];
            }
        });
        return new FaultCodeCollection();
    });
