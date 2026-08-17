/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/* global define*/
define("LocationViews", ["SectionCollection", "currentPackage"],
        function (BaseCollection, currentPackage) {
            "use strict";
            if (Utils.isPopoutWindow()) {
                mentor.publisher.locationViewsData = getWindowObj().mentor.publisher.locationViewsData;
            }
            else {
                var LocationsCollection, p = mentor.publisher;
                LocationsCollection = BaseCollection.extend({
                    category: p.documentCategory.LOCATION_VIEWS,
                    initialize: function () {
                        currentPackage.on("change:id", this.fetch, this);
                        currentPackage.on("change:language", this.fetch, this);
                    },
                    getData: function (selectedProject) {
                        var array = (selectedProject && selectedProject.getByType('LocationViews')) || [];
                        return array.sort(function (a, b) {
                            return Utils.sort(a, b, "mainText");
                        });
                    }
                });
                mentor.publisher.locationViewsData = new LocationsCollection();
            }
            return mentor.publisher.locationViewsData;
        });