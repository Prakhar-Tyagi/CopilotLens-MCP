/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor*/
define("Harnesses", ["SectionCollection", "currentPackage"],
        function (BaseCollection, selectedPackage) {
            "use strict";
            if (Utils.isPopoutWindow()) {
                mentor.publisher.harnessReportsData = getWindowObj().mentor.publisher.harnessReportsData;
            }
            else {
                var p = mentor.publisher;
                var HarnessesCollection = BaseCollection.extend({
                    category: p.documentCategory.HARNESS,
                    initialize: function () {
                        selectedPackage.on("change:id", this.fetch, this);
                        selectedPackage.on("change:vin", this.fetch, this);
                        selectedPackage.on("change:config", this.fetch, this);
                        selectedPackage.on("change:language", this.fetch, this);
                    },
                    getData: function (selectedProject) {
                            return (selectedProject && selectedProject.getByType('harness')) || [];
                    }
                });
                mentor.publisher.harnessReportsData = new HarnessesCollection();
            }
            return mentor.publisher.harnessReportsData;
        });