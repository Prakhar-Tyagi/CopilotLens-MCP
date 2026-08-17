/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("BaseConfigurationsModel",
        ["backbone", "underscore", "ConfigurationsCollection", "OptionsCollection", "LocalConfigurations"],
        function (Backbone, _, configurationsCollection, optionsCollection, localConfigurations) {
            "use strict";

            function getConfigurationModel(modelRef) {
                return modelRef.getConfigurationsModel()
            }

            function getVehicleConfigObject(modelRef) {
                return getConfigurationModel(modelRef).vehicleConfigObj;
            }

            function getPredefineConfigurations(modelRef) {
                return getVehicleConfigObject(modelRef).getPredefinedConfigurations();
            }

            function getMatchedConfigurations(modelRef) {
                return getVehicleConfigObject(modelRef).getMatchedConfigurations();
            }

            var ConfigurationsModel = Backbone.Model.extend({
                initialize: function () {
                },

                fetch: function (event, onModelLoad) {
                    var xLoc = event.clientX, yLoc = event.clientY;
                    if (xLoc) {
                        this.set("x", xLoc);
                        this.set("y", yLoc);
                        this.set("show", true);
                    }

                    function afterModelLoadCallback(dataModel)
                    {
                        var configs = dataModel.getConfigurationsModel();
                        var options = dataModel.getOptionsModel();
                        if ((configs && configs.length > 0) || (options && options.length > 0)) {
                            this.set("loadSkeleton", event);
                            configurationsCollection.fetch(configs);
                            optionsCollection.fetch(options);
                        }
                        onModelLoad && onModelLoad.call(null);
                    }

                    //all the UI from the below initialized classes has been taken out into view/templates
                    //the below class only initializes the data, there is lots of logic inside these classes which can be kept as it is
                    //as it would take lot of time/effort to change all that into a proper model
                    modelReference = this.createModel(event);
                    modelReference.createPanelGUI(afterModelLoadCallback.bind(this));
                },

                createModel: function (event) {
                },

                getExistingConfigForSelectedOptions: function () {
                    var selectedOptions = [];
                    var configName;
                    getVehicleConfigObject(modelReference).currentSelectedOptions.forEach(function (item) {
                        selectedOptions.push(item.value);
                    });

                    getMatchedConfigurations(modelReference).forEach(function (item) {
                        var configOptionsArray = item.value.split(",").map(function (item) {
                            return item.trim();
                        });
                        var arrDifference = _.difference(selectedOptions, configOptionsArray);
                        if (arrDifference.length === 0 && selectedOptions.length === configOptionsArray.length) {
                            configName = item.name;
                        }
                    });
                    return configName;
                },

                checkIfNameAlreadyExist: function (enteredConfigName) {
                    var predefinedConfigurations = getPredefineConfigurations(modelReference);
                    return this.isNewConfigurationPreExist(enteredConfigName, predefinedConfigurations);
                },

                generateConfigName: function () {
                    var index = 0;
                    var defaultConfigName = mentor.publisher.languageTranslator.localize('CustomConfiguration');
                    return this.doGenerateConfigName(index, defaultConfigName);
                },

                doGenerateConfigName: function (ind, generatedConfigName) {
                    var isNamealreadyexist = false;
                    var randomConfigName = mentor.publisher.languageTranslator.localize('CustomConfiguration');
                    getPredefineConfigurations(modelReference).forEach(function (item) {
                        if (item.name === generatedConfigName) {
                            isNamealreadyexist = true;
                        }
                    });
                    if (isNamealreadyexist === true) {
                        ind++;
                        generatedConfigName = this.doGenerateConfigName(ind, randomConfigName + " " + ind);
                    }
                    return generatedConfigName;

                },

                updateModel: function (event, selected) {
                    var dataModel = modelReference.getConfigurationsModel();
                    if (selected == 'option') {
                        dataModel.optionSelected(event);
                    }
                    else {
                        dataModel.configurationSelected(event);
                    }
                    configurationsCollection.fetch(dataModel.getConfigurationsModel());
                    optionsCollection.fetch(dataModel.getOptionsModel());
                },

                rollbackToLastAppliedOptions: function() {
                    var lastAppliedOptions = [];
                    var configurationsManager = mentor.publisher.configurationsManager;
                    var selectedOptions = configurationsManager.getSelectedOptions();
                    if (typeof selectedOptions === 'string') {
                        selectedOptions.split(',').map(function(option) {
                            lastAppliedOptions.push(option.trim());
                        });

                        var allOptionObjects = getVehicleConfigObject(modelReference).getOptionsUsedInPredefinedConfiguration();
                        var lastSelectedOptions = allOptionObjects.filter(function(optionObj) {
                            return lastAppliedOptions.indexOf(optionObj.value.trim()) > -1;
                        });
                        getVehicleConfigObject(modelReference).setCurrentSelectedOptions(lastSelectedOptions);
                    }
                },

                createNewModel: function (configuration) {
                    var predefinedConfigurations = getPredefineConfigurations(modelReference);
                    var matchedCofigurations = getMatchedConfigurations(modelReference);
                    var configurationName = configuration.name.trim()
                    if (!this.isNewConfigurationPreExist(configurationName, predefinedConfigurations)) {
                        predefinedConfigurations.push(configuration);
                        matchedCofigurations.splice(0, 0, configuration);
                        localConfigurations.saveConfig(configuration);
                        configurationsCollection.fetch(matchedCofigurations);
                        return true;
                    }
                    return false;
                },

                delete: function (configurationName) {
                    function nameMatchFn (config){
                        return config.name === configurationName;
                    };

                    var isDeleted = localConfigurations.deleteConfig(configurationName);
                    if (isDeleted) {
                        var predefinedConfigurations = getPredefineConfigurations(modelReference);
                        var matchedConfigurations = getMatchedConfigurations(modelReference);
                        var vehicleConfigObj = getVehicleConfigObject(modelReference);

                        var updatedPredefinedConfigs = _.reject(predefinedConfigurations, nameMatchFn);
                        var updatedMatchedConfigs = _.reject(matchedConfigurations, nameMatchFn);

                        vehicleConfigObj.setPredefinedConfigurations(updatedPredefinedConfigs);
                        vehicleConfigObj.setMatchedConfigurations(updatedMatchedConfigs);
                        configurationsCollection.fetch(updatedMatchedConfigs);
                    } else {
                        // TODO: what if config cannot be deleted ???
                    }
                    return isDeleted;
                },

                isNewConfigurationPreExist: function (configurationName, predefinedConfigurations) {
                    return _.find(predefinedConfigurations, function (config) {
                        return config.name.toLowerCase() === configurationName.trim().toLowerCase();
                    });
                },

                reset: function () {
                    var vehicleConfigObj = getVehicleConfigObject(modelReference);
                    var predefinedConfigurations = getPredefineConfigurations(modelReference);
                    vehicleConfigObj.setCurrentSelectedOptions([]);
                    // var matchedCofigurations = dataModel.vehicleConfigObj.getMatchedConfigurations();
                    configurationsCollection.fetch(predefinedConfigurations);
                },

                getConfigurationsCount: function () {
                    var predefineConfigurations = getPredefineConfigurations(modelReference);
                    return predefineConfigurations.length;
                },

                close: function () {
                }
            }), modelReference;
            return ConfigurationsModel;
        });

