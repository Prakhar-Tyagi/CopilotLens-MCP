/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/**
 * @fileoverview This is a class for creating the split panels
 * In this class we are creating the one panel, and two tool bars
 * For bottom toolbar we appending one slider depending upon the contenttype.
 * For the toptool bar we are appending all the buttons (reports,diagrams,popout,maximise,minimise etc...)
 * Here we are creating one method called 'changeButtonsState' This method change the buttons visibility
 * depending upon the visible views.
 * We are handling the buttons report ,diagram and print from PopupButtonHandler class.
 * For minimise,maximise and close buttons are handling from Layoutmanager
 * Layout manager tells to panel every time when ever new panel open or ant panel close
 * Depending upon that data we are handling the buttons states and also position of the slider for panel
 */

// Create an advanced viewer in the browser
// @param containerId the id of the container which will contains the viewer object
// @param viewerId the id of the viewer to be created
// @param documentUrl the url of the document to load in the viewer
XrefFilterConfigurationFilterPanel.prototype = new BaseInteractiveBuilder();
XrefFilterConfigurationFilterPanel.prototype.constructor = XrefFilterConfigurationFilterPanel;
function XrefFilterConfigurationFilterPanel(packageModelInstance, clonedVehicleObject) {
    this.setThisObject(this);
    this.packageModelInstance = packageModelInstance;
    this.vehicleConfigObj = clonedVehicleObject;

    this.processOptionPanelItemClick = function () {
        //Refresh New Window and Main window if Cofnig Filter is opened at two place...(NEw window and Separate Window)
        updateXrefInPopup(this.vehicleConfigObj);
    }

}

function updateXrefInPopup(vehicleObject) {
    var matchedConfiguration = vehicleObject.getMatchedConfigurations(), config = {};
    config.activeconfigs = matchedConfiguration;
    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.APPLY_CONFIGURATION_FILTER_ON_POPOVER,
        {config : config, filter : mentor.publisher.configurationsBasedXRefFilter});
}
;

