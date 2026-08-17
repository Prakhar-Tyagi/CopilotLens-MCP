/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/**
 * @Fileoverview This calss containd the Utility methods , that are used
 * for common purpose.
 */
jQuery.extend({
    InteractiveBuilderUtility : function () {
        var packageModel = mentor.publisher.configurationsManager;

        this.displayConfigBuilderButton = function () {
            if (typeof(vinfilter) != "undefined" && Utils.notNull(vinfilter) &&
                packageModel.getProjectFilteringType() !== mentor.publisher.constants.TypeVINFilter) {
                packageModel.loadVehicleConfiguration(packageModel.get('currentActiveProject'),
                    function (vehicleConfigDetails) {
                        if (vehicleConfigDetails.textValue != 'failure') {
                            vinfilter.showInterativeBuilderButton(true);
                        }
                        else {
                            vinfilter.showInterativeBuilderButton(false);
                        }
                        resizeVinFilterBox();
                    });
            }
        };
    }
});

InteractiveBuilderUtility = new $.InteractiveBuilderUtility();