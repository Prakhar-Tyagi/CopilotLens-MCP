/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

var VehicleConfigObject = function () {
    this.isEmpty = true;
    this.predefinedConfigurtaions = new Array();
    this.allOptions = new Array();
    this.matchedConfigurtaions = new Array();
    this.currentSelectedOptions = new Array();
    this.predefinedConfigurtaionsOptions = new Array();
    var packageModel = mentor.publisher.configurationsManager;

    this.setPredefinedConfigurations = function (value) {
        this.predefinedConfigurtaions = value;
    };
    this.getPredefinedConfigurations = function () {
        return this.predefinedConfigurtaions;
    };

    this.setAllOptions = function (value) {
        this.allOptions = value;
        this.populateOptionsForPredefinedConfiguration();
    };

    this.populateOptionsForPredefinedConfiguration = function () {
        // TODO: Do I still need to maintain config specific optons list ??
        // var configArray = this.predefinedConfigurtaions;
        //
        // for (var index = 0; index < configArray.length; index++) {
        //     var options = configArray[index].value;
        //
        //     var array = options.split(',');
        //     for (var i = 0; i < array.length; i++) {
        //         var added = Utils.findIndexOfObject(this.predefinedConfigurtaionsOptions, array[i]);
        //         if (added === '') {
        //             var found = Utils.findIndexOfObject(this.allOptions, array[i]);
        //             if (found !== '') {
        //                 this.predefinedConfigurtaionsOptions.push(this.allOptions[found]);
        //             }
        //         }
        //     }
        // }
        this.predefinedConfigurtaionsOptions = this.sortBasedOnRequiredOrder(this.allOptions);

    };

    this.sortBasedOnRequiredOrder = function (optionsArray)//variant and exclusive together
    {
        var finalArray = new Array();
        var variantArray = new Array();
        var nonVariantArray = new Array();
        if (Utils.notNull(optionsArray) && optionsArray.length > 0) {
            for (var index = 0; index < optionsArray.length; index++) {
                if (optionsArray[index].isVariant === 'true') {
                    var alreadyPresent = Utils.findIndexOfObject(variantArray, optionsArray[index]);
                    if (alreadyPresent === '') {
                        variantArray.push(optionsArray[index]);
                    }
                }
                else {
                    var alreadyPresent = Utils.findIndexOfObject(nonVariantArray, optionsArray[index]);
                    if (alreadyPresent === '') {
                        nonVariantArray.push(optionsArray[index]);
                    }
                }
            }
        }
        Utils.sortArrayData(variantArray);
        Utils.sortArrayData(nonVariantArray)
        finalArray = this.addExclusiveTogether(finalArray, variantArray);
        finalArray = this.addExclusiveTogether(finalArray, nonVariantArray);

        return finalArray;
    };

    this.addExclusiveTogether = function (finalArray, variantArray) {
        for (var index = 0; index < variantArray.length; index++) {
            if (Utils.findIndexOfObject(finalArray, variantArray[index]) === '') {
                finalArray.push(variantArray[index]);
                var options = variantArray[index].exclusiveOptions;

                var array = options.split(',');
                for (var i = 0; i < array.length; i++) {
                    var getOption = Utils.findIndexOfObject(this.predefinedConfigurtaionsOptions, array[i]);
                    if (getOption !== "") {
                        var obj = this.predefinedConfigurtaionsOptions[getOption];
                        if (Utils.findIndexOfObject(finalArray, obj) === '') {
                            finalArray.push(obj);
                        }
                    }
                }
            }
        }
        return finalArray;
    };

    this.getAllOptions = function () {
        return this.allOptions;
    };

    this.setMatchedConfigurations = function (value) {
        this.matchedConfigurtaions = value;
    };
    this.getMatchedConfigurations = function () {
        return this.matchedConfigurtaions;
    };

    this.setCurrentSelectedOptions = function (value) {
        this.currentSelectedOptions = value;
    };

    this.getCurrentSelectedOptions = function () {
        return this.currentSelectedOptions;
    };

    this.getCurrentSelectedOptionsAsString = function () {
        var selectedOptionsAsString = '';
        for (var index = 0; index < this.currentSelectedOptions.length; index++) {
            selectedOptionsAsString = selectedOptionsAsString + this.currentSelectedOptions[index].value.trim() + ',';
        }
        selectedOptionsAsString = selectedOptionsAsString.substr(0, selectedOptionsAsString.lastIndexOf(','));
        return selectedOptionsAsString;
    };

    this.getOptionsUsedInPredefinedConfiguration = function () {
        return this.predefinedConfigurtaionsOptions; // now equals to all available options
    };

    this.isVehicleConfigEmpty = function () {
        return this.isEmpty;
    };
    this.setEmptyFlag = function (value) {
        this.isEmpty = value;
    };

    this.resetSelection = function () {
        this.matchedConfigurtaions = [];
        this.currentSelectedOptions = [];
        packageModel.setProjectFilteringType(mentor.publisher.constants.TypeNOFilter);
        packageModel.setSelectedOptions('');
        //packageModel.setSelectedOptions('');
        // vinfilter.showInterativeBuilderButton(true && !this.isVehicleConfigEmpty());
        InteractiveBuilderUtility.displayConfigBuilderButton();
        packageModel.setLastVehicleConfigObject(null);
        //packageModel.setFinalOptionsSetReached(false);
    };

    // TODO: delete this, there's no usage
    // this.deleteVehicleConfigurationData = function () {
    //     packageModel.setVehicleConfigObject(null);
    //     packageModel.setLastVehicleConfigObject(null);
    //     packageModel.setProjectFilteringType(mentor.publisher.constants.TypeNOFilter);
    //     packageModel.setSelectedOptions('');
    // };

    this.updateCurrentSelectedOptionsInVehicleObject = function (options) {
        var optionCodesArray = options.split(',');
        this.setCurrentSelectedOptions(new Array());
        for (var i = 0; i < optionCodesArray.length; i++) {
            var indexOfOptionsInGlobalStore = Utils.findIndexOfObject(this.getOptionsUsedInPredefinedConfiguration(),
                optionCodesArray[i]);
            var dataObj = this.getOptionsUsedInPredefinedConfiguration()[indexOfOptionsInGlobalStore];
            this.getCurrentSelectedOptions().push(dataObj);
        }
    };

    this.findSuperSetConfigNames = function () {
        this.setMatchedConfigurations(new Array());
        var predefinedConfigArr = this.getPredefinedConfigurations();
        var currentSelectedOptions = this.getCurrentSelectedOptions();
        if (currentSelectedOptions.length === 0) {
            this.setMatchedConfigurations(predefinedConfigArr);
        }
        else {
            var matchedConfigs = _.filter(predefinedConfigArr, function (thisConfig) {
                var optionStr = thisConfig.value;
                var configOptions = optionStr.split(",").map(function (option) {
                    return option.trim();
                });

                var selectedOptions = currentSelectedOptions
                    .map(function (optionObj) {
                        return optionObj.value && optionObj.value.trim();
                    });

                return _.every(selectedOptions, function (selectedOption) {
                    return configOptions.indexOf(selectedOption.trim()) > -1;
                });
            });

            this.setMatchedConfigurations(matchedConfigs);
        }
    };

    this.deepClone = function () {
        var clonedVehicleObject = new VehicleConfigObject();
        clonedVehicleObject.allOptions = jQuery.extend(true, new Array(), this.allOptions);
        clonedVehicleObject.currentSelectedOptions = jQuery.extend(true, new Array(), this.currentSelectedOptions);
        clonedVehicleObject.isEmpty = new Boolean(this.isEmpty).valueOf();
        clonedVehicleObject.matchedConfigurtaions = jQuery.extend(true, new Array(), this.matchedConfigurtaions);
        clonedVehicleObject.predefinedConfigurtaions = jQuery.extend(true, new Array(), this.predefinedConfigurtaions);
        clonedVehicleObject.predefinedConfigurtaionsOptions =
            jQuery.extend(true, new Array(), this.predefinedConfigurtaionsOptions);
        return clonedVehicleObject;

    }
}