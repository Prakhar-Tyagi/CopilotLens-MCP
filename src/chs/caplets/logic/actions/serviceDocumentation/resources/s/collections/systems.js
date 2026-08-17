/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Backbone, mentor*/
define("systems", ["SectionCollection"],
        function (BaseCollection)
        {
            "use strict";
            var p = mentor.publisher;
            var SystemsCollection = BaseCollection.extend({
                model: Backbone.Model.extend({idAttribute: "idAttribute"}),
                category: p.documentCategory.SYSTEMS,
                getData: function (selectedProject)
                {
                    var sortedArray = (selectedProject && selectedProject.getSystems())|| [];
                    sortedArray = sortedArray.sort(Utils.sort);
                    return sortedArray;
                }
            });
            return new SystemsCollection();
        });