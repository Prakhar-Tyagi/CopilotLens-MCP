/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global Utils, mentor, extend, OptionExpressionFilter, callFunction, applyVINFilter, jQuery, isObjectActive, getActiveConfigurationsForSystemWithOptions*/
Utils.namespace('mentor.publisher.ConfigurationFilteredProject');

mentor.publisher.ConfigurationFilteredProject = extend(mentor.publisher.VINFilteredProject, (function () {
    "use strict";
    var project, filterXRefData;

    filterXRefData = function (obj) {
        var xrefs, filteredListItems;
        if ((typeof obj.getCrossReferences === "function") && obj.getCrossReferences()) {
            xrefs = obj.getCrossReferences();
            filteredListItems = mentor.publisher.configurationsBasedFilter.applyFilter(xrefs.xrefs,
                mentor.publisher.configurationsManager.getSelectedOptions());
            xrefs.listItems = filteredListItems;
            obj.getCrossReferences = function () {
                return xrefs;
            };
        }
        return obj;
    };
    return {
        applyFilter : function (systems) {
            //this is the logic where the systems are duplicated
            return mentor.publisher.configurationsBasedFilter.applyFilter(systems,
                mentor.publisher.configurationsManager.getSelectedOptions());
        },
        setProject : function (projectData) {
            project = projectData;
        },
        getProject : function () {
            return project;
        },
        getSystems : function () {
            var systems = project.getSystems();
            return this.applyFilter(systems);
        },
        getObjectById : function (objectId) {
            //object by id should still return the original object
            var i, filteredSystems = this.getSystems();
            for (i = 0; i < filteredSystems.length; i = i + 1) {
                if (filteredSystems[i] && filteredSystems[i].idAttribute === objectId) {
                    return filteredSystems[i];
                }
            }
            return project.getObjectById(objectId);
        },
        loadObjectData : function (systemId, objectUid) {
            var objects = [], originalObject, filteredObject;
            originalObject = project.loadObjectData(systemId, objectUid);
            filteredObject = filterXRefData(originalObject);
            return filteredObject;
        },
        getId : function () {
            return project.getId();
        },
        getInformation : function () {
            return project.getReports('introduction-page');
        },
        getReports : function (type) {
            var reports = project.getReports(type);
            return mentor.publisher.vinOptionExpressionFilter.applyFilter(reports, this.getVINOptions());
        },
        getDiagrams : function (type) {
            var diagrams = project.getDiagrams(type);
            return mentor.publisher.vinOptionExpressionFilter.applyFilter(diagrams, this.getVINOptions());
        },
        getObjects : function (type, loadAllObjects) {
            var objects = project.getObjects(type, loadAllObjects);
            return mentor.publisher.vinOptionExpressionFilter.applyFilter(objects, this.getVINOptions());
        },
        getData : function (type, systemId, diagramId) {
            return project.getData(type, systemId, diagramId);
        },
        createListGroups : function () {
            return project.createListGroups();
        },
        isConfigFilter : function () {
            return true;
        }
    };
}()));

function applyConfigurationFilter(vinOptions) {
    "use strict";
    //console.log("apply configuration filter");
    var configManager = mentor.publisher.configurationsManager;
    configManager.getVINFilterInstance().setTextExternally(configManager.getVehicleConfigObject().getCurrentSelectedOptionsAsString());
    mentor.publisher.ConfigurationFilteredProject.setVINOptions(vinOptions);

    //set the project to original backup project
    //however 1st time, there is no backup project.
    if (typeof mentor.publisher.ConfigurationFilteredProject.getProject() === "undefined") {
        /*mentor.publisher.ConfigurationFilteredProject.setProject(mentor.publisher.project);
         mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject;*/
    } else {
        mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject.getProject();
    }
    //set the backup project to project
    //and the set the project to filtered project
    mentor.publisher.ConfigurationFilteredProject.setProject(mentor.publisher.project);
    mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject;
    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CONFIGURATION_FILTER_APPLIED,
        {data : configManager.getVehicleConfigObject().getCurrentSelectedOptionsAsString()});
}

function resetConfigurationFilter() {
    "use strict";
    mentor.publisher.filter.reset();
    mentor.publisher.configurationsManager.setDynamicNavigationActive(false);
    mentor.publisher.configurationsManager.getVehicleConfigObject().resetSelection();
    if (mentor.publisher.ConfigurationFilteredProject === mentor.publisher.project) {
        mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject.getProject();
        mentor.publisher.ConfigurationFilteredProject.setProject(undefined);
    } else if (typeof mentor.publisher.project.getProject !== "undefined") {
        if (mentor.publisher.project.getProject() === mentor.publisher.ConfigurationFilteredProject) {
            mentor.publisher.project.setProject(mentor.publisher.ConfigurationFilteredProject.getProject());
            mentor.publisher.ConfigurationFilteredProject.setProject(undefined);
            mentor.publisher.ConfigurationFilteredProject.setVINOptions("");
        }
    }
    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CONFIGURATION_FILTER_APPLIED,
        {data : ""});
}

function resolveDynamicConfigurationMode(data, config) {
    "use strict";
    var systemConfig, activeConfig = "", previousConfig;
    var configManager = mentor.publisher.configurationsManager;
    if (config) {
        activeConfig = config;
    } else if (data.detail) {
        activeConfig = callFunction(data.detail.getActiveConfiguration) || (
            typeof  data.detail.getActiveConfiguration === "function" ? data.detail.getActiveConfiguration() :
                "");
    } else {
        activeConfig = callFunction(data.getActiveConfiguration) || (
            typeof  data.getActiveConfiguration === "function" ? data.getActiveConfiguration() :
                "");
    }
    //if the current state is not in dynamic mode, we need not proceed any further
    if (!configManager.isDynamicNavigationActive()) {
        return false;
    }

    //if the current state is dynamic mode, then some computation is done, after which it is checked whether the state is still dynamic
    //if the state is found to be dynamic, then configuration filter is applied for that activeconfig
    //if not VIN filter is applied
    if (activeConfig) {
        //mentor.publisher.filter.vinOptions = activeConfig;
        mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.ITEM_CLICKED_IN_DYNAMIC_MODE,
            {vinOptions : activeConfig});
        systemConfig = activeConfig;
        var vehicleConfigObject = configManager.getVehicleConfigObject();
        vehicleConfigObject.updateCurrentSelectedOptionsInVehicleObject(systemConfig);
        vehicleConfigObject.findSuperSetConfigNames();
        //Set the Options set on which to filter the project
        previousConfig = vehicleConfigObject.getMatchedConfigurations();
        configManager.setSelectedOptions(previousConfig);
        if (!configManager.isDynamicNavigationActive()) {
            //resetting the VIN text box as it again gets set when the VIN filter is applied
            //mentor.publisher.configurationsManager.getVINFilterInstance().setTextExternally('');
            resetConfigurationFilter();
            //mentor.publisher.configurationsManager.setSelectedOptions(previousConfig);
            //mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject.getProject();
            vehicleConfigObject.updateCurrentSelectedOptionsInVehicleObject(systemConfig);
            configManager.setTextInStaticMode(previousConfig);
            applyVINFilter(activeConfig, true);
        } else {
            applyConfigurationFilter(activeConfig);
        }
    }
    return true;
}

mentor.publisher.configurationsBasedFilter = (function () {
    "use strict";
    var filterSystemsForDynamicMode, createActiveConfigurationProperty;
    /*
     This is a generic method which will duplicate the systems/xref items.
     this is called to duplicate the system/xref items in the configuration filtering dynamic mode
     */
    filterSystemsForDynamicMode = function (system, activeConfiguration) {
        var k, id, systemObject = jQuery.extend(true, {}, system), diagramOptionExpression,
            tooltips = [], tooltip, activeDiagrams = [], duplicateArray = [];
        if (typeof systemObject.getId === "function") {
            id = systemObject.getId();
        } else {
            id = systemObject.id;
        }
        //we change the idToHighlight value, the id would essentially be the same
        //systemObject.idToHighlight = id + activeConfiguration.value;
        systemObject.idToHighlight = id;
        createActiveConfigurationProperty(systemObject, activeConfiguration);
        //determine the Active diagram for default opening from diagram List
        //todo is this needed? will it be enough to filter the diagrams
        /* if (typeof systemObject.getDiagrams === "function") {
         for (k = 0; k < systemObject.getDiagrams().length; k = k + 1) {
         //In case of Dynamic VIN navigation just store all the diagram in 'diagramList' at the time
         //of duplicate creation will check the activeness and set accordingly
         diagramOptionExpression = systemObject.getDiagrams()[k].getOptionExpression();
         if (isObjectActive(diagramOptionExpression, systemObject.getActiveConfiguration())) {
         activeDiagrams.push(systemObject.getDiagrams()[k]);
         }
         }

         //update the active diagrams for the system
         systemObject.getDiagrams = function () {
         return activeDiagrams;
         };
         }*/

        mentor.publisher.VINFilteredProject.filterSystem(systemObject, systemObject.getActiveConfiguration());

        //updating tooltips
        if (typeof systemObject.getToolTips === "function") {
            for (k = 0; k < systemObject.getToolTips().length; k = k + 1) {
                //todo this is a hack, we need to get rid of this
                if (systemObject.getToolTips()[k].getName() !== 'Configuration') {
                    tooltips.push(systemObject.getToolTips()[k]);
                }
            }
            //tooltips = systemObject.getToolTips();
        }
        tooltip = {
            getName : function () {
                return 'Configuration';
            },
            getValue : function () {
                return activeConfiguration.name + '->' +
                    systemObject.getActiveConfiguration();
            }
        };
        tooltips.push(tooltip);
        systemObject.getToolTips = function () {
            return tooltips;
        };
        return systemObject;
    };
    createActiveConfigurationProperty = function (item, actvieConfig) {
        item.getActiveConfiguration = function () {
            return actvieConfig.value;
        };
        item.idAttribute = item.id + actvieConfig.name + actvieConfig.value;
    };
    return {
        applyFilter : function (systems, activeconfigs) {
            //todo is this correct? we should not call filter when dynamice navigation is not active
            //todo itt is not the responsible of the filter to check this
            /*if (!mentor.publisher.configurationsManager.isDynamicNavigationActive()) {
             return;
             }*/
            //this is the logic where the systems are duplicated
            var duplicateArray = [
            ], itr, activeConfigArray, configItr, index;
            for (itr = 0; itr < systems.length; itr = itr + 1) {
                //todo the code below is commented out
                //todo check if this is right
                //if (mentor.publisher.configurationsManager.isDynamicNavigationActive()) {
                if (!systems[itr].getOptionExpression) {
                    duplicateArray.push(systems[itr]);
                } else {
                    activeConfigArray =
                        getActiveConfigurationsForSystemWithOptions(systems[itr].getOptionExpression(), activeconfigs);
                    for (configItr = 0; Utils.notNull(activeConfigArray) && configItr < activeConfigArray.length;
                        configItr = configItr + 1) {
                        duplicateArray.push(filterSystemsForDynamicMode(systems[itr],
                            activeConfigArray[configItr]));
                    }
                }
//                } else {
//                    duplicateArray = systems;
//                    for (index = 0; index < duplicateArray.length; index = index + 1) {
//                        createActiveConfigurationProperty(duplicateArray[index],
//                            mentor.publisher.configurationsManager.getFilterOptionsForContentArea());
//                    }
//                }
            }
            return duplicateArray;
        }
    };
}());

/*
 The problem with using the same old configBasedFilter for xrefs also is that
 the xref's should not be filtered for the current view,
 they should be filtered based on original view or the un-filtered view, to elobarate this,
 Let us say OP1 is currently applied and the original Xrefs->XRef(original) are currently duplicated based on OP1 ->XRef(OP1),
 Now when some options are selected in XRefConfigBuilder lets say OP2, its XRef(original) which should be filtered and not XRef(OP1)
 So this filter is sort of a wrapper which gives the original XRefs to the COnfigBasedFilter.
 */
mentor.publisher.configurationsBasedXRefFilter = (function (p) {
    "use strict";
    return {
        applyFilter : function (xrefs, config) {
            //todo is this correct?
            var activeconfigs = config.activeconfigs, filteredListItems;
            if (!p.configurationsManager.isDynamicNavigationActive()) {
                return;
            }
            //this is the logic where the systems are duplicated
            /*xrefs =
             p.ConfigurationFilteredProject.getProject().loadObjectData(objectData.systemId,
             objectData.objectId).getCrossReferences();*/
            //xrefs = model ? model.xrefs : [];
            filteredListItems = p.configurationsBasedFilter.applyFilter(xrefs, activeconfigs);
            return filteredListItems;
        }
    };
}(mentor.publisher));

mentor.publisher.configurationsBasedOtherFilter = (function (p) {
    "use strict";
    var duplicateFaultCodeXREFs, createActiveConfigurationProperty;
    duplicateFaultCodeXREFs = function (system, activeConfiguration) {
        var k, id, systemObject = jQuery.extend(true, {}, system), diagramOptionExpression,
            tooltips = [], tooltip, activeDiagrams = [], duplicateArray = [];
        createActiveConfigurationProperty(systemObject, activeConfiguration);
        tooltip = {
            getName : function () {
                return 'Configuration';
            },
            getValue : function () {
                return activeConfiguration.name + '->' +
                    systemObject.getActiveConfiguration();
            }
        };
        tooltips.push(tooltip);
        systemObject.getToolTips = function () {
            return tooltips;
        };
        return systemObject;
    };
    createActiveConfigurationProperty = function (item, activeConfig) {
        item.getActiveConfiguration = function () {
            return activeConfig.value;
        };
        item.idAttribute = item.id + activeConfig.name + activeConfig.value;
    };
    return {
        applyFilter : function (model) {
            var activeconfigs = mentor.publisher.configurationsManager.getSelectedOptions(), xrefs = model, filteredListItems =
                [], activeConfigArray, k, configItr;
            if (!p.configurationsManager.isDynamicNavigationActive()) {
                return model;
            }
            for (k = 0; xrefs && k < xrefs.length; k = k + 1) {
                activeConfigArray =
                    getActiveConfigurationsForSystemWithOptions(xrefs[k].optionExpression, activeconfigs);
                for (configItr = 0; activeConfigArray && configItr < activeConfigArray.length;
                    configItr = configItr + 1) {
                    filteredListItems.push(duplicateFaultCodeXREFs(xrefs[k],
                        activeConfigArray[configItr]));
                }
            }
            return filteredListItems;
        }
    };
}(mentor.publisher));


