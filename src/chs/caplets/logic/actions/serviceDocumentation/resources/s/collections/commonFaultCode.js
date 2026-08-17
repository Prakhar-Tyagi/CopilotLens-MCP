/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

define(["underscore", "currentPackage", "SectionCollection", "collections/faults"],
        function (_, currentPackage, BaseCollection, faults) {
            "use strict";
            var p = mentor.publisher;
            var commonFaultCode = BaseCollection.extend({
                model: Backbone.Model.extend({idAttribute: "id"}),
                category: p.documentCategory.COMMON_FAULT_CODE,
                getData: function (project) {
                    var commonFaultCode = (project && project.getByType('commonFaultCodes')) || [];
                    return faults.getModels() && faults.getModels().length > 0 ? commonFaultCode : [];
                },

            });
            return new commonFaultCode();
        });