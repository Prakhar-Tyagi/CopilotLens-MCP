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
var ConfigurationFilterPopup = function (event, sourceButton) {

    var myThis = this;
    var packageModel = mentor.publisher.configurationsManager;
    var position = {left: $(event)[0].clientX, top: $(event)[0].clientY};
    this.clikedFromButton = sourceButton;
    var configModel;

    this.createPanelGUI = function (afterModelSetupCallback) {
        ConfigurationFilterPanel.prototype = new BaseInteractiveBuilder();
        configModel = new ConfigurationFilterPanel(packageModel);
        window.configFilterInMain = configModel;
        configModel.initializeData(afterModelSetupCallback);
        packageModel.setLastVehicleConfigObject(jQuery.extend(true, {}, packageModel.getVehicleConfigObject()));
    };

    this.getConfigurationsModel = function () {
        return configModel;
    };

    this.hidePopup = function () {
        this.popupDIV.hidePopup();
        if (!(Utils.getUrlParameter('configpopup') == 'yes') && isSelectedOptionsSetChanged()) {
            packageModel.setVehicleConfigObject(packageModel.getLastVehicleConfigObject());
        }
        window.configFilterInMain = null;
        configurationPopup = null;
    };

    this.clickedFromWhere = function () {
        return this.clikedFromButton;
    };
};

function interactiveBuilderXButtonClicked()
{
    //$('#configPopup').remove();
    window.configFilterInMain = null;
    configurationPopup = null;
    //Start Dynamic Filtering

    if (isSelectedOptionsSetChanged()) {
        var configManager = mentor.publisher.configurationsManager;
        var translator = mentor.publisher.languageTranslator;
        var vehicleConfigObject = configManager.getVehicleConfigObject();
        if (Utils.getUrlParameter('configpopup') === 'yes') {
            configManager.setLastVehicleConfigObject(vehicleConfigObject);
        }
        else {
            configManager.setLastVehicleConfigObject(jQuery.extend(true, {}, vehicleConfigObject));
        }
        if (vehicleConfigObject.getCurrentSelectedOptions().length == 0) {
            resetVINFilter();
            resetConfigurationFilter();
        }
        else if (isMatchingConfigurationChanged() && vehicleConfigObject.getCurrentSelectedOptions().length != 0) {
            configManager.setProjectFilteringType(mentor.publisher.constants.TypeConfigurationFilter);
            configManager.setSelectedOptions(vehicleConfigObject.getMatchedConfigurations());

            //Reload the project with absolute Filtering
            //if the options selected lead to dynamic mode, do configurations filtering
            //if the options lead to a final configuration, do vinfiltering
            if (configManager.isDynamicNavigationActive()) {
                applyConfigurationFilter(configManager.getFilterOptionsForContentArea());
            }
            else {
                applyVINFilter(configManager.getFilterOptionsForContentArea(), true);
            }
        }
        else if (vehicleConfigObject.getMatchedConfigurations().length != 1) {
            if (Utils.getUrlParameter('configpopup') === 'yes') {
                window.opener.mentor.publisher.optionFilterPanel.setTextExternally(
                        vehicleConfigObject.getCurrentSelectedOptionsAsString());
            }
            else {
                mentor.publisher.optionFilterPanel.setTextExternally(
                        vehicleConfigObject.getCurrentSelectedOptionsAsString());
            }
        }
    }
}

function filterByOptions()
{
    window.configFilterInMain = null;
    configurationPopup = null;
    if(isSelectedOptionsSetChanged()) {
        var configManager = mentor.publisher.configurationsManager;
        var translator = mentor.publisher.languageTranslator;
        var vehicleConfigObject = configManager.getVehicleConfigObject();
        configManager.setLastVehicleConfigObject(jQuery.extend(true, {}, vehicleConfigObject));

        if (vehicleConfigObject.getCurrentSelectedOptions().length == 0) {
            resetVINFilter();
            resetConfigurationFilter();
        }
        else {
            configManager.setProjectFilteringType(mentor.publisher.constants.TypeConfigurationFilter);
            var selectedOptions = vehicleConfigObject.getCurrentSelectedOptions();
            configManager.setSelectedOptions([{
                name: translator.localize("customFilter"),
                value: selectedOptions.map(function (item) {
                    return item.value;
                }).join(", ")
            }]);
            applyVINFilter(configManager.getFilterOptionsForContentArea(), true);
        }
    }
}

function isSelectedOptionsSetChanged()
{
    var oldVehicleObj, newVehicleObj;
    if (!mentor.publisher.configurationsManager.getLastVehicleConfigObject()) {
        return false;
    }
    var isChanged = false;
    oldVehicleObj = mentor.publisher.configurationsManager.getLastVehicleConfigObject();
    newVehicleObj = mentor.publisher.configurationsManager.getVehicleConfigObject();
    var oldState = oldVehicleObj.getCurrentSelectedOptions();
    var newState = newVehicleObj.getCurrentSelectedOptions();//This is always array
    newVehicleObj.isDynamic = (newVehicleObj.getMatchedConfigurations() || []).length > 1;
    if (oldVehicleObj.isDynamic !== newVehicleObj.isDynamic) {
        return true;
    }

    if (oldState.length !== newState.length) {
        isChanged = true;
    }
    else {
        isChanged = !Utils.compareArrayContents(oldState, newState);
    }
    return isChanged;
}

/*
 * This function is useful to check if 'X' button was clciked and there was any change from laststate to now new state
 */
function isMatchingConfigurationChanged()
{
    var isChanged = false;
    var configManager = mentor.publisher.configurationsManager;
    var currentState = configManager.getSelectedOptions();
    var newState = configManager.getVehicleConfigObject().getMatchedConfigurations();//This is always array
    //Basically neither CurrentState is there nor user selected something
    //He might have just open the configurtaion filter panel and closed it
    if (currentState === "" && configManager.getVehicleConfigObject().getCurrentSelectedOptions().length === 0) {
        return isChanged;
    }
    if (currentState instanceof Array) {
        if (currentState.length !== newState.length) {//it will become Unique config reached state
            isChanged = true;
        }
        else {
            isChanged = !(Utils.compareArrayContents(currentState, newState));
        }
    }
    else {
        if (newState.length === 1) {
            if (currentState !== newState[0].value) {
                isChanged = true;
            }
        }
        else if (newState.length > 0 &&
                configManager.getVehicleConfigObject().getCurrentSelectedOptions().length !==
                0) {
            isChanged = true; //The currentState is either VIN/No Filter/Or Static Config Case and the new state is matching array so we need to reload.
        }
    }
    return isChanged;
}