/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 *
 */

describe("ConfigurationFilterTest", function () {
    var project = {
        getSystems : function () {
            return [
                {
                    name: "system1",
                    idAttribute: "objId1",
                },
                {
                    name: "system2",
                    idAttribute: "objId2",
                },
            ];
        },
        createListGroups: function () {
            return [
                {
                    name: "group1",
                },
                {
                    name: "group2",
                },
            ];
        },
        getData: function () {
            return [
                {
                    name: "data1",
                },
                {
                    name: "data2",
                },
            ];
        },
        getObjects: function () {
            return [
                {
                    name: "object1",
                },
                {
                    name: "object2",
                },
            ];
        },
        getReports: function () {
            return [
                {
                    name: "report1",
                },
                {
                    name: "report2",
                },
            ];
        },
        getDiagrams: function () {
            return [
                {
                    name: "diagram1",
                },
                {
                    name: "diagram2",
                },
            ];
        },
        getId: function () {
            return "projectID";
        },
        loadObjectData: function () {
            return {
                name: "object1",
            }
        },
        getObjectById: function (id) {
            return {
                name: "object1",
            };
        }
    };
    beforeEach(function ()
    {
        mentor.publisher.ConfigurationFilteredProject.setProject(project);
    });

    it("should be able to check if the config filter is applied", function (){
        expect(mentor.publisher.ConfigurationFilteredProject.isConfigFilter()).toBeTruthy();
    });

    it("should be able to create list groups", function (){
        spyOn(project, "createListGroups").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.createListGroups();
        expect(project.createListGroups).toHaveBeenCalled();
    });

    it("should be able to get data", function (){
        spyOn(project, "getData").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getData();
        expect(project.getData).toHaveBeenCalled();
    });

    it("should be able to get filtered objects", function (){
        spyOn(project, "getObjects").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getObjects();
        expect(project.getObjects).toHaveBeenCalled();
    });

    it("should be able to get filtered diagrams", function (){
        spyOn(project, "getObjects").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getDiagrams();
    });

    it("should be able to filtered reports", function (){
        spyOn(project, "getReports").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getReports();
        expect(project.getReports).toHaveBeenCalled();
    });

    it("should be able to get information", function (){
        spyOn(project, "getReports").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getInformation();
        expect(project.getReports).toHaveBeenCalledWith('introduction-page');
    });

    it("should be able to get Id", function (){
        spyOn(project, "getId").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getId();
        expect(project.getId).toHaveBeenCalled();
    });

    it("should be able to load object data", function (){
        spyOn(project, "loadObjectData").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.loadObjectData();
        expect(project.loadObjectData).toHaveBeenCalled();
    });

    it("should be able to get object by Id", function (){
        expect(mentor.publisher.ConfigurationFilteredProject.getObjectById("objId1")).toEqual({name: 'system1', idAttribute: 'objId1'});
    });

    it("should be able to get systems", function (){
        spyOn(project, "getSystems").andCallThrough();
        mentor.publisher.ConfigurationFilteredProject.getSystems();
        expect(project.getSystems).toHaveBeenCalled();
    });

    it("should be able to get project", function (){
        expect(
            mentor.publisher.ConfigurationFilteredProject.getProject()
        ).toEqual(project);
    });

    it("should be able to apply filter", function (){
        spyOn(mentor.publisher.configurationsBasedFilter, "applyFilter");
        mentor.publisher.ConfigurationFilteredProject.applyFilter();
        expect(mentor.publisher.configurationsBasedFilter.applyFilter).toHaveBeenCalled();
    });

    xit("should be able to apply configuration filter", function (){
        var vinOptions = {
            "vin": {
                "vin1": {
                    "name": "vin1",
                    "description": "vin1",
                    "id": "vin1",
                }
            }
        };
        // configManager.getVINFilterInstance(...).setTextExternally is not a function
        // the correct function is configManager.getVINFilterInstance(...).VINFilterView.setTextExternally
        applyConfigurationFilter(vinOptions);
    });

    it("should be able to reset configuration filter", function (){
        var origGetVehicleConfigObject = mentor.publisher.configurationsManager.getVehicleConfigObject,
            origProject= mentor.publisher.project,
            origGetProject = mentor.publisher.project.getProject
        ;

        mentor.publisher.configurationsManager.getVehicleConfigObject = function () {
            return {
                resetSelection: function () {}
            }
        };
        mentor.publisher.project = mentor.publisher.ConfigurationFilteredProject;

        spyOn(mentor.publisher.ConfigurationFilteredProject, "getProject").andCallThrough();
        resetConfigurationFilter();
        expect(mentor.publisher.ConfigurationFilteredProject.getProject).toHaveBeenCalled();

        mentor.publisher.project = origProject;

        mentor.publisher.project.getProject = function () {
            return mentor.publisher.ConfigurationFilteredProject;
        };
        mentor.publisher.project.setProject = function () {};

        spyOn(mentor.publisher.ConfigurationFilteredProject, "setProject").andCallThrough();
        resetConfigurationFilter();
        expect(mentor.publisher.ConfigurationFilteredProject.setProject).toHaveBeenCalled();

        mentor.publisher.configurationsManager.getVehicleConfigObject = origGetVehicleConfigObject;
        mentor.publisher.project = origProject;
    });

    it("should be able to resolve dynamic configuration", function (){
        var data={}, config={},
            origIsDynamicNavigationActive = mentor.publisher.configurationsManager.isDynamicNavigationActive,
            origGetVehicleConfigObject = mentor.publisher.configurationsManager.getVehicleConfigObject,
            origApplyConfigurationFilter  = applyConfigurationFilter
        ;
        expect(resolveDynamicConfigurationMode(data, config)).toBeFalsy();

        mentor.publisher.configurationsManager.isDynamicNavigationActive = function () {return true;};
        mentor.publisher.configurationsManager.getVehicleConfigObject =  function () {
            return {
                updateCurrentSelectedOptionsInVehicleObject: function () {},
                findSuperSetConfigNames: function () {},
                getMatchedConfigurations: function () {},
            }
        };
        applyConfigurationFilter = function () {};
        expect(resolveDynamicConfigurationMode(data, config)).toBeTruthy();

        mentor.publisher.configurationsManager.isDynamicNavigationActive = origIsDynamicNavigationActive;
        mentor.publisher.configurationsManager.getVehicleConfigObject = origGetVehicleConfigObject;
        applyConfigurationFilter = origApplyConfigurationFilter;
    });
});
describe("ConfigurationsBasedFilterTest", function () {
    var p = mentor.publisher,
        origIsDynamicNavigationActive = p.configurationsManager.isDynamicNavigationActive;
    beforeEach(function () {
        p.configurationsManager.isDynamicNavigationActive = function () {return true;};
    });
    it("should be able to apply filter", function () {
        spyOn(mentor.publisher.configurationsBasedFilter, "applyFilter").andCallThrough();
        mentor.publisher.configurationsBasedXRefFilter.applyFilter([], {activeconfigs: {}});
        expect(mentor.publisher.configurationsBasedFilter.applyFilter).toHaveBeenCalled();

        p.configurationsManager.isDynamicNavigationActive = function () {return false;};
        mentor.publisher.configurationsBasedXRefFilter.applyFilter([], {activeconfigs: {}});
    });

    it("should be able to check if the config filter is applied", function () {
        expect(mentor.publisher.configurationsBasedOtherFilter.applyFilter({})).toEqual([]);
        p.configurationsManager.isDynamicNavigationActive = function () {return false;}
        expect(mentor.publisher.configurationsBasedOtherFilter.applyFilter({})).toEqual({});
    });

    afterEach(function () {
        p.configurationsManager.isDynamicNavigationActive = origIsDynamicNavigationActive;
    })
});
