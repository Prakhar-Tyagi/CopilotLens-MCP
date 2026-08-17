/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("XRefBuilderModel", ["BaseConfigurationsModel"],
    function (BaseConfigurationsModel) {
        "use strict";
        var XrefBuilderModel = BaseConfigurationsModel.extend({

            createModel : function (event) {
                var clonedVehicleConfigObject, windowObj = window;
                if (window.opener && window.opener.mentor) {
                    windowObj = window.opener;
                }
                var clonedVehicleConfigObject = windowObj.mentor.publisher.configurationsManager.getVehicleConfigObject().deepClone();
                return new XRefFilterConfigurationFilterPopup(event, mentor.publisher.constants.InteractiveButtonFromXref,
                    clonedVehicleConfigObject);
            },

            close : function () {
                var windowObj = window;
                if (window.opener && window.opener.mentor) {
                    windowObj = window.opener;
                }
                windowObj.interactiveBuilderXButtonClicked();
            },

            shouldShowConfigSavePanel: function() {
                return false;
            }
        }), xrefBuilderModel;
        xrefBuilderModel = new XrefBuilderModel();
        return _.extend(xrefBuilderModel, Backbone.Events);
    });

