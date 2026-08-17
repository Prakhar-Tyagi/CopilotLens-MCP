/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

define("PanelDataCollection", ["SectionCollection", "currentPackage"],
        function (BaseCollection, currentPackage) {
            "use strict";
            const PanelDataCollection = BaseCollection.extend({
                getData: function (panelType) {
                    return mentor.publisher.project.getData(panelType);
                },
                fetch: function(obj) {
                    const view = Utils.getUrlParameter('view') || "";
                    if (view) {
                        var models, items = this.getData(view), model;
                        models = this.createModelObj(items);
                        this.reset(models);
                        return this.models;
                    }
                    return;
                }

            });
            return new PanelDataCollection();// this.loadViewForCustomData(panel, panelDataCollection);
        }
);