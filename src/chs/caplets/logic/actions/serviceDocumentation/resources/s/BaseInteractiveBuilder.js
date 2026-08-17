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
 * @author Siva
 */

function BaseInteractiveBuilder()
{
    "use strict";
    var myThis = this;
    var optionsModel = [];
    var configurationsModel = [];
    this.packageModelInstance = null;
    this.vehicleConfigObj = null;
    this.currentAvailableListOptions = [];

    function onConfigDataLoad(afterModelSetupCallback, vehicleConfigDetails)
    {
        if (vehicleConfigDetails.textValue != 'failure') {
            var predefinedConfiguration = new Array();
            var optionsArrayForPredefined = new Array();
            var vehicleConfigObj = myThis.vehicleConfigObj;
            if (vehicleConfigDetails.dataArray.configuration
                && vehicleConfigDetails.dataArray.configuration.length > 0) {
                predefinedConfiguration = vehicleConfigDetails.dataArray.configuration;
                var sortedConfigs = myThis.sortConfigurations(predefinedConfiguration);
                vehicleConfigObj.setPredefinedConfigurations(sortedConfigs);
                vehicleConfigObj.setMatchedConfigurations(sortedConfigs.slice());
                configurationsModel = sortedConfigs;
            }

            if (vehicleConfigDetails.dataArray.option && vehicleConfigDetails.dataArray.option.length > 0) {
                vehicleConfigObj.setAllOptions(vehicleConfigDetails.dataArray.option);
                optionsArrayForPredefined = vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
                optionsModel = optionsArrayForPredefined;
            }
            vehicleConfigObj.setEmptyFlag(false);
            afterModelSetupCallback(myThis);
        }
    }

    this.getOptionsModel = function () {
        return optionsModel;
    };

    this.getConfigurationsModel = function () {
        return configurationsModel;
    };

    this.optionSelected = function (event) {
        var jCurrTargetEle = $(event.currentTarget), isChecked = false, optionCode = "";

        if (jCurrTargetEle.is("input[type='checkbox']")) {
            isChecked = jCurrTargetEle.is(":checked");
            optionCode = jCurrTargetEle.data("value");
        }
        else {
            var children = jCurrTargetEle.children("input[type='checkbox']");
            isChecked = children.first().is(":checked");
            optionCode = children.first().data("value");
        }

        var currentSelectedOptions = myThis.vehicleConfigObj.getCurrentSelectedOptions();
        if (isChecked) {
            var globalArray = myThis.vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();

            var dataObj = _.findWhere(globalArray, {value: optionCode});
            currentSelectedOptions.push(dataObj);

            var nestedInclusiveOptions = myThis.getNestedInclusiveOptions(dataObj, globalArray);
            nestedInclusiveOptions.forEach(function (nestedInclusiveOption) {
                var exists = _.findWhere(currentSelectedOptions, {value: nestedInclusiveOption.value});
                if (!exists) {
                    currentSelectedOptions.push(nestedInclusiveOption);
                }
            });
        }
        else {
            //Removing from current selected list
            var optionObj = _.findWhere(currentSelectedOptions, {value: optionCode});
            var indexToDelete = _.indexOf(currentSelectedOptions, optionObj);
            if (indexToDelete > -1) {
                currentSelectedOptions.splice(indexToDelete, 1);
            }
        }
        myThis.doProcessing();
        myThis.processOptionPanelItemClick();
    };

    this.getNestedInclusiveOptions = function (option, globalOptionArr) {
        var nestedOptions = [];

        function optionStringToOptionObject(optionName)
        {
            var indexOfObject = Utils.findIndexOfObject(globalOptionArr, optionName);
            return globalOptionArr[indexOfObject];
        }

        if (option.inclusiveOptions) {
            var includedOpts = option.inclusiveOptions
                .split(',')
                .map(function (optionName) {
                    return optionName.trim();
                })
                .map(optionStringToOptionObject);

            var nested = includedOpts
                .map(function (thisOption) {
                    return this.getNestedInclusiveOptions(thisOption, globalOptionArr);
                }, this)
                .reduce(function (acc, items) {
                    return acc.concat(items);
                }, []);

            return includedOpts.concat(nested);
        }
        return [];
    };

    this.configurationSelected = function (item) {
        var previousSelectedConfig, windowObj = window;
        if (window.opener && window.opener.mentor) {
            windowObj = window.opener;
        }

        /*if (Utils.getUrlParameter('configpopup') === 'yes') {
         window.setConfigPanelItemClicked = true;//This flag is used for Dynamic Loading from Popout Browser
         }*/
        myThis.packageModelInstance.getVINFilterInstance().setTextExternally &&
        myThis.packageModelInstance.getVINFilterInstance().setTextExternally('');
        //Set the currentOptions to the the selected list
        var currentOptions = item.currentTarget.getAttribute('data-value');
        myThis.packageModelInstance.getVehicleConfigObject().updateCurrentSelectedOptionsInVehicleObject(
            currentOptions);
        myThis.packageModelInstance.getVehicleConfigObject().setMatchedConfigurations(new Array());

        //Set Match Configuration to clicked configurtaion
        var matchConfigIndex = Utils.findIndexOfObject(
            myThis.packageModelInstance.getVehicleConfigObject().getPredefinedConfigurations(),
            currentOptions);
        if (matchConfigIndex !== '') {
            myThis.packageModelInstance.getVehicleConfigObject().getMatchedConfigurations().push(
                myThis.packageModelInstance.getVehicleConfigObject().getPredefinedConfigurations()[matchConfigIndex]);
        }

        //Set the Filter Type and Options set on which to filter the project
        myThis.packageModelInstance.setProjectFilteringType(mentor.publisher.constants.TypeConfigurationFilter);
        previousSelectedConfig =
            myThis.packageModelInstance.getVehicleConfigObject().getMatchedConfigurations();
        myThis.packageModelInstance.setSelectedOptions(previousSelectedConfig);

        myThis.packageModelInstance.setLastVehicleConfigObject(myThis.packageModelInstance.getVehicleConfigObject());

        myThis.packageModelInstance.hidePopup();
        windowObj.resetConfigurationFilter();

        myThis.packageModelInstance.getVehicleConfigObject().updateCurrentSelectedOptionsInVehicleObject(
            currentOptions);
        myThis.packageModelInstance.setTextInStaticMode(previousSelectedConfig);

        windowObj.applyVINFilter(currentOptions, true);
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_CONFIG_POPOVER);
    };

    this.setThisObject = function (value) {
        myThis = value;
    }

    this.processOptionPanelItemClick = function () {

    }
    this.resetPanel = function () {
        myThis.vehicleConfigObj.setMatchedConfigurations(new Array());
        myThis.vehicleConfigObj.currentAvailableListOptions = [];
    }
    this.initializeData = function (afterModelSetupCallback) {
        if (myThis.vehicleConfigObj.isVehicleConfigEmpty()) {
            myThis.packageModelInstance.loadVehicleConfiguration(mentor.publisher.project.getId(),
                onConfigDataLoad.bind(null, afterModelSetupCallback));
        }
        else {
            myThis.doProcessing();
            afterModelSetupCallback(myThis);
        }
        //myThis.registerToolTipEvents();
    };

    this.sortConfigurations = function (matchedConfigurations) {
        var sortedConfigs = _(matchedConfigurations)
            .chain()
            .sortBy(function (config) {
                return config.name.toLowerCase();
            })
            .sortBy(function (config) {
                return config.isLocal;
            })
            .toArray()
            .value();
        return sortedConfigs;
    };

    this.populateMatchedConfigurtaionDiv = function () {
        var matchedConfigurations = myThis.vehicleConfigObj.getMatchedConfigurations();
        var sortedConfigs = this.sortConfigurations(matchedConfigurations);
        myThis.vehicleConfigObj.setMatchedConfigurations(sortedConfigs);
    };

    this.populateOptionsListForAvailablePanel = function () {
        myThis.currentAvailableListOptions = new Array();
        var optionsArray = new Array();
        var vehicleConfigObj = myThis.vehicleConfigObj;
        var currentMatchedConfigs = vehicleConfigObj.getMatchedConfigurations();
        for (var i = 0; i < currentMatchedConfigs.length; i++) {
            var options = currentMatchedConfigs[i].value;

            var array = options.split(',');
            for (var j = 0; j < array.length; j++) {
                var index = Utils.findIndexOfObject(optionsArray, array[j]);
                if (index === "") {
                    optionsArray.push(array[j]);
                }
            }
        }

        if (optionsArray.length === 0) {
            myThis.currentAvailableListOptions = vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
        }
        else {
            for (var i = 0; i < optionsArray.length; i++) {
                var globalArray = vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
                var indexOfOptionsInGlobalStore = Utils.findIndexOfObject(globalArray, optionsArray[i]);
                if (indexOfOptionsInGlobalStore !== "") {
                    var dataObj = globalArray[indexOfOptionsInGlobalStore];
                    myThis.currentAvailableListOptions.push(dataObj);
                }
            }
        }
    };

    this.doProcessing = function () {
        myThis.vehicleConfigObj.findSuperSetConfigNames();
        myThis.populateMatchedConfigurtaionDiv();
        configurationsModel = myThis.vehicleConfigObj.getMatchedConfigurations();
        myThis.populateOptionsListForAvailablePanel();
        optionsModel = myThis.vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
        myThis.updateOptionsList(myThis.currentAvailableListOptions);
        myThis.disableExclusiveOptions();
        myThis.checkInclusiveOptions();
    };

    this.updateOptionsList = function (currentActiveItems) {
        var optionsList = myThis.vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
        for (var j = 0; j < optionsList.length; j++) {
            var element = optionsList[j];
            var itemContents = element.value;
            var index = Utils.findIndexOfObject(currentActiveItems, itemContents);

            var currentSelectedOptions = myThis.vehicleConfigObj.getCurrentSelectedOptions();
            var indexSelected = Utils.findIndexOfObject(currentSelectedOptions, itemContents);
            element.checked = false;
            element.disabled = false;
            // if (index === '' && currentSelectedOptions.length > 0) {
            //     element.checked = false;
            //     element.disabled = true;
            // }
            if (indexSelected !== '' && index !== '') {//Active and Selected both
                element.disabled = false;
                element.checked = true;
            }
        }
    };

    function toUniqueArray(accumulator, optionNameArr)
    {
        optionNameArr.forEach(function (item) {
            if (item) {
                if (accumulator.indexOf(item) === -1) {
                    accumulator.push(item);
                }
            }
        });
        return accumulator;
    };

    function createValueExtractor(attributeName)
    {
        return function valueExtractor(thisObj) {
            if (thisObj[attributeName]) {
                return thisObj[attributeName].split(',').map(function (str) {
                    return str.trim();
                });
            }
            return [];
        };
    }

    this.disableExclusiveOptions = function () {
        var currentSelectedOptions = myThis.vehicleConfigObj.getCurrentSelectedOptions();
        var optionsUsedInPredefinedConfiguration = myThis.vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
        var valueExtractor = createValueExtractor("exclusiveOptions");
        var exclusions = currentSelectedOptions
            .map(valueExtractor)
            .reduce(toUniqueArray, []);

        optionsUsedInPredefinedConfiguration.forEach(function (option) {
            if (exclusions.indexOf(option.value) > -1) {
                option.disabled = true;
                option.checked = false;
            }
        });
    }

    this.checkInclusiveOptions = function () {
        var currentSelectedOptions = myThis.vehicleConfigObj.getCurrentSelectedOptions();
        var optionsUsedInPredefinedConfiguration = myThis.vehicleConfigObj.getOptionsUsedInPredefinedConfiguration();
        var valueExtractor = createValueExtractor("inclusiveOptions");
        // We do not need to recursively find the inclusive options here.
        // currentSelectedOptions is already updated with all nested inclusions in 'optionSelected' method.
        var inclusions = currentSelectedOptions
            .map(valueExtractor)
            .reduce(toUniqueArray, []);

        optionsUsedInPredefinedConfiguration.forEach(function (option) {
            if (inclusions.indexOf(option.value) > -1) {
                option.checked = true;
                option.disabled = true;
            }
        });
    }
}
