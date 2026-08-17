/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor*/
define(["SectionCollection", "currentPackage"],
        function (BaseCollection, selectedPackage)
        {
            "use strict";
			var p = mentor.publisher;
            var GlobalReports = BaseCollection.extend({
				category: p.documentCategory.GLOBAL_GROUND_REPORT,
				getIdToFilter: function(item) {
					return item.get("nameAttr");
				},

                initialize:function ()
                {
                    selectedPackage.on("change:id", this.fetch, this);
                    selectedPackage.on("change:vin", this.fetch, this);
                    selectedPackage.on("change:config", this.fetch, this);
                    selectedPackage.on("change:language", this.fetch, this);
                },
                getData:function (selectedProject)
                {
                    return selectedProject.getReports(p.contentType.GLOBAL_GROUND_REPORT);
                }
            });
            return new GlobalReports();
        });