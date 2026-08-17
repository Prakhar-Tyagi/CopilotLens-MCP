/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/**
 * This is the class that handles the related data button of the Splitter panel.
 * Based on the given data it creates the those panel and appends it to Popup.
 * For this popup Filter also appending at the end .
 * Listerners for those items in panels also hanling in this class only.
 * Based on the item click it higlight in the open views.
 * @param event
 * @param itemDetails
 * @author Siva.
 */
var XRefFilterConfigurationFilterPopup = function (event, sourceButton, clonedVehicleConfigObject) {

    var myThis = this, windowObj = window;
    if (window.opener && window.opener.mentor) {
        windowObj = window.opener;
    }
    var packageModel = windowObj.mentor.publisher.configurationsManager;
    var position = {left : $(event)[0].clientX, top : $(event)[0].clientY };
    var configModel;
    this.clonedVehicleConfigObject = clonedVehicleConfigObject;
    this.clikedFromButton = sourceButton;

    this.getConfigurationsModel = function () {
        return configModel;
    };

    this.createPanelGUI = function (afterModelSetupCallback) {
        XrefFilterConfigurationFilterPanel.prototype = new BaseInteractiveBuilder();
        configModel = new XrefFilterConfigurationFilterPanel(packageModel, this.clonedVehicleConfigObject);

        function setIsDynamicFilteringFlow(item, index, arr) {
            arr[index].isDynamicFilteringFlow = true;
        };
        configModel.vehicleConfigObj.predefinedConfigurtaions.forEach(setIsDynamicFilteringFlow);
        configModel.vehicleConfigObj.matchedConfigurtaions.forEach(setIsDynamicFilteringFlow);

        window.configFilterInMain = configModel;
        configFilterInMain.initializeData(afterModelSetupCallback);
    };
};

