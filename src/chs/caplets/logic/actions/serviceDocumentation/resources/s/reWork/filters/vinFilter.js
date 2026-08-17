/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global Utils, mentor, extend, OptionExpressionFilter, callFunction*/
mentor.publisher.VINFilteredProject = extend(mentor.publisher.textFilteredProject, (function () {
    "use strict";
    var vinOptions, project, filterSystems, filterSystem;
    filterSystem = function (system, options) {
        var reports, diagrams;
        system = system || {};
        options = options || vinOptions;
        if (typeof system.getDiagrams === "function") {
            diagrams = system.getDiagrams();
            system.getDiagrams = function () {
                return mentor.publisher.vinOptionExpressionFilter.applyFilter(diagrams, options);
            };
        }

        if (typeof system.getReports === "function") {
            reports = system.getReports();
            system.getReports = function () {
                return mentor.publisher.vinOptionExpressionFilter.applyFilter(reports, options);
            };

        }

        return system;
    };
    filterSystems = function () {
        var systems = project.getSystems(), filteredSystems, k;
        filteredSystems =
            mentor.publisher.vinOptionExpressionFilter.applyFilter(mentor.publisher.object(systems), vinOptions);
        for (k = 0; k < filteredSystems.length; k = k + 1) {
            filterSystem(filteredSystems[k]);
        }
        return filteredSystems;
    };
    return {
        setVINOptions : function (options) {
            vinOptions = options;
        },
        getVINOptions : function () {
            return vinOptions;
        },
        applyFilter : function (items) {
            return mentor.publisher.vinOptionExpressionFilter.applyFilter(items, this.getVINOptions());
        },
        getDiagrams : function (type) {
            var diagrams = project.getDiagrams(type);
            return this.applyFilter(diagrams);
        },
        setProject : function (projectData) {
            project = projectData;
        },
        getProject : function () {
            return project;
        },
        getSystems : function () {
            /*            var systems = project.getSystems();
             return this.applyFilter(systems);*/
            return filterSystems();
        },
        getObjects : function (type, loadAllObjects) {
            var objects = project.getObjects(type, loadAllObjects);
            return this.applyFilter(objects);
        },
        getObjectById : function (objectId) {
            return filterSystem(project.getObjectById(objectId));
        },
        loadObjectData : function (systemId, objectUid) {
            return project.loadObjectData(systemId, objectUid);
        },
        getId : function () {
            return project.getId();
        },
        getInformation : function () {
            return project.getReports('introduction-page');
        },
        getReports : function (type) {
            var reports = project.getReports(type);
            return this.applyFilter(reports);
        },
        getData : function (type, systemId, diagramId) {
            return project.getData(type, systemId, diagramId);
        },
        filterSystem : function (system, options) {
            return filterSystem(system, options);
        },
        getFirstSection : function () {
            var firstSection = this.getProject().getFirstSection(), translatedListItems = [
            ], temp = mentor.publisher.object(firstSection);
            translatedListItems = this.applyFilter(firstSection.listItems());
            temp.listItems = function () {
                return translatedListItems;
            };
            return temp;
        },
        get : function (name) {
            return this.getObjectById(name);
        },
        getCustomData : function () {
            return project.getCustomData();
        },
        getByType : function (type) {
            var objects = [], originalObject, translatedObject;
            originalObject = this.getProject().getByType(type);
            if (type === 'systems') {
                if (originalObject && originalObject.length > 0) {
                    objects = originalObject;
                    translatedObject = this.applyFilter(objects);
                } else if (originalObject) {
                    objects.push(originalObject);
                    translatedObject = this.applyFilter(objects)[0];
                }
                return translatedObject;
            } else {
                return mentor.publisher.vinOptionExpressionFilter.applyFilter(originalObject, this.getVINOptions());
            }
        }
        //todo global objects, apply deep filtering
        //todo need a way to see if an object is garbage collected
    };
}()));

function applyVINFilter(vinOptions, fromConfigurationFilter, vinNumber) {
    "use strict";
    var fromConfigurationBuilderFlow = fromConfigurationFilter || false;
    /**
     * if the unique configuration is selected after dynamic config then the project object should get reset to original state
     */
    if (fromConfigurationBuilderFlow && callFunction(mentor.publisher.project.isConfigFilter)) {
        mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject.getProject();
    }
    if (typeof mentor.publisher.VINFilteredProject.getProject() === "undefined") {
        mentor.publisher.VINFilteredProject.setProject(mentor.publisher.project);
        mentor.publisher.project = mentor.publisher.VINFilteredProject;
    }
    mentor.publisher.VINFilteredProject.setVINOptions(vinOptions);
    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.VIN_FILTER_APPLIED,
        {vinOptions : vinOptions, fromConfigurationBuilderFlow : fromConfigurationBuilderFlow, vinNumber: vinNumber});
    mentor.publisher.popoutHandler.closePopoutWindows();
}

function resetVINFilter() {
    "use strict";
    mentor.publisher.filter.reset();
    if (mentor.publisher.VINFilteredProject === mentor.publisher.project) {
        mentor.publisher.project = mentor.publisher.VINFilteredProject.getProject();
        mentor.publisher.VINFilteredProject.setProject(undefined);
    } else if (typeof mentor.publisher.project.getProject !== "undefined") {
        if (mentor.publisher.project.getProject() === mentor.publisher.VINFilteredProject) {
            mentor.publisher.project.setProject(mentor.publisher.VINFilteredProject.getProject());
            mentor.publisher.VINFilteredProject.setProject(undefined);
            mentor.publisher.VINFilteredProject.setVINOptions("");
        }
    }

    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.VIN_FILTER_APPLIED,
        {vinOptions : ""});
}

mentor.publisher.vinOptionExpressionFilter = (function () {
    "use strict";
    var applyOptionFilter;
    applyOptionFilter = function (items, vinOptions) {

        var filteredObjects = [], i, anItem, objectOptionExpression, optionExpressionFilter;
        vinOptions = vinOptions || "";
        items = items || [];
        if (vinOptions) {

            optionExpressionFilter = new OptionExpressionFilter();
            for (i = 0; i < items.length; i = i + 1) {
                anItem = items[i];
                if (typeof anItem.getOptionExpression === "function" || anItem.optionExpression) {
                    if (typeof anItem.getOptionExpression !== "function") {
                        objectOptionExpression = anItem.optionExpression || "";
                    } else {
                        objectOptionExpression = anItem.getOptionExpression() || "";
                    }

                    if (optionExpressionFilter.evaluteOptionsAgainstOptionExpressions(objectOptionExpression,
                        vinOptions)) {
                        filteredObjects.push(anItem);
                    }
                } else {
                    filteredObjects.push(anItem);
                }
            }
        } else {
            filteredObjects = items;
        }
        return filteredObjects;
    };

    return {
        applyFilter : function (objects, vinOptions) {
            return applyOptionFilter(objects, vinOptions);
        }
    };
}());

