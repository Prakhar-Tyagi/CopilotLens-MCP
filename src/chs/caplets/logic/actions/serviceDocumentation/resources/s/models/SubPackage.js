/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
        [
            "backbone",
            "filehandlers/effectivitySetter"
        ],
        function (Backbone, effectivitySetter) {

            return Backbone.Model.extend({
                defaults: {
                    name: "",
                    description: ""
                },

                idAttribute: "idAttribute",

                getThumbnailPath: function () {
                    var url = this.get('id').replace(/\\/g, '/') + '/thumbnail.png?packageId=' +
                            this.get('parentPackageId').replace(/data\\/g, '') +
                            '&effectivity=' + this.get('id').replace(/data\\/g, '');
                     return effectivitySetter.identifyIfContentIsZipped(url, this.get('id'));
                }
            });
        }
);