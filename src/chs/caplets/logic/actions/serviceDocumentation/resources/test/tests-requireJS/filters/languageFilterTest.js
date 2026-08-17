/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2023 Siemens
 */

describe("LanguageFilterTest", function () {
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
        },
        getFirstSection: function () {
            return {
                name: "section1",
                listItems: function () {return []},
            };
        },
        get: function () {
            return {
                name: "project1",
            };
        },
        getByType: function () {
            return [
                {
                    name: "project1",
                },
            ];
        },
        getCustomData: function () {
            return {
                name: "customData1",
            };
        }
    };
    beforeEach(function ()
    {
        mentor.publisher.LanguageFilteredProject.setProject(project);
    });

    it("should be able to get custom data", function () {
        spyOn(project, "getCustomData").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getCustomData();
        expect(project.getCustomData).toHaveBeenCalled();
    });

    it("should be able to get by type", function () {
        spyOn(project, "getByType").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getByType("testType");
        expect(project.getByType).toHaveBeenCalled();
    });

    it("should be able to get by name", function () {
        spyOn(project, "get").andCallThrough();
        mentor.publisher.LanguageFilteredProject.get("testName");
        expect(project.get).toHaveBeenCalled();
    });

    it("should be able to get first section", function () {
        spyOn(project, "getFirstSection").andCallThrough();
        var res = mentor.publisher.LanguageFilteredProject.getFirstSection();
        res.listItems();
        expect(project.getFirstSection).toHaveBeenCalled();
    });

    it("should be able to get data", function (){
        spyOn(project, "getData").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getData();
        expect(project.getData).toHaveBeenCalled();
    });

    it("should be able to create list groups", function (){
        spyOn(project, "createListGroups").andCallThrough();
        mentor.publisher.LanguageFilteredProject.createListGroups();
        expect(project.createListGroups).toHaveBeenCalled();
    });

    it("should be able to get information", function (){
        spyOn(project, "getReports").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getInformation();
        expect(project.getReports).toHaveBeenCalledWith('introduction-page');
    });

    it("should be able to filtered reports", function (){
        spyOn(project, "getReports").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getReports();
        expect(project.getReports).toHaveBeenCalled();
    });

    it("should be able to get filtered objects", function (){
        spyOn(project, "getObjects").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getObjects();
        expect(project.getObjects).toHaveBeenCalled();
    });

    it("should be able to get Id", function (){
        spyOn(project, "getId").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getId();
        expect(project.getId).toHaveBeenCalled();
    });

    it("should be able to load object data", function (){
        spyOn(project, "loadObjectData").andCallThrough();
        mentor.publisher.LanguageFilteredProject.loadObjectData();
        expect(project.loadObjectData).toHaveBeenCalled();
    });

    it("should be able to get object by Id", function (){
        expect(
            JSON.stringify(mentor.publisher.LanguageFilteredProject.getObjectById("objId1"))
        ).toEqual(JSON.stringify({ withoutTranslation : {} }));
    });

    it("should be able to get systems", function (){
        spyOn(project, "getSystems").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getSystems();
        expect(project.getSystems).toHaveBeenCalled();
    });

    it("should be able to get project", function (){
        expect(
                mentor.publisher.LanguageFilteredProject.getProject()
        ).toEqual(project);
    });

    it("should be able to get filtered diagrams", function (){
        spyOn(project, "getObjects").andCallThrough();
        mentor.publisher.LanguageFilteredProject.getDiagrams();
    });

    it("should be able to apply filter", function (){
        spyOn(mentor.publisher.LanguageFilteredProject, "applyFilter");
        mentor.publisher.LanguageFilteredProject.applyFilter();
        expect(mentor.publisher.LanguageFilteredProject.applyFilter).toHaveBeenCalled();
    });

    it("should be able to get current language", function () {
        mentor.publisher.LanguageFilteredProject.setCurrentLanguage("en");
        expect(mentor.publisher.LanguageFilteredProject.getCurrentLanguage("testSystemID", "testObjectUID")).toBe("en");
    });

    it("should be able to get localized name", function () {
        expect(mentor.publisher.LanguageFilteredProject.getLocalizedName("testName")).toBe("testName");
        expect(mentor.publisher.LanguageFilteredProject.getLocalizedName("_en$testNamei")).toBe("_en$testNamei");
    });

    it("should be able to translate quick code", function () {
        expect(mentor.publisher.LanguageFilteredProject.translateQuickCode("testQuickCode")).toBe("testQuickCode");
    });

    afterEach(function () {
    });
});