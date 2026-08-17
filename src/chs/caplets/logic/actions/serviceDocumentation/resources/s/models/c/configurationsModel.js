/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("ConfigurationsModel", ["BaseConfigurationsModel"],
    function (BaseConfigurationsModel) {
        "use strict";
        var ConfigurationsModel = BaseConfigurationsModel.extend({

            createModel : function (event) {
                return new ConfigurationFilterPopup(event, mentor.publisher.constants.InteractiveButtonFromVIN);
            },

            close : function () {
                interactiveBuilderXButtonClicked();
            },

            shouldShowConfigSavePanel: function() {
                return true;
            }
        }), configurationsModel;
        configurationsModel = new ConfigurationsModel();
        return _.extend(configurationsModel, Backbone.Events);
    });