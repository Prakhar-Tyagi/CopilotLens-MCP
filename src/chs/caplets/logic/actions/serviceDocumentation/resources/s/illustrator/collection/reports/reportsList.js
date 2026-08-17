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
            var IllustratorReports = BaseCollection.extend({
                category: "Reports",
                dataLoader: p.customGeneratorDataLoader,

                getData: function (selectedProject)
                {
                    var list = this.dataLoader.load("Reports", selectedProject.getId());
                    return list.sort(Utils.sort);
                }
            });
            return new IllustratorReports();
        });