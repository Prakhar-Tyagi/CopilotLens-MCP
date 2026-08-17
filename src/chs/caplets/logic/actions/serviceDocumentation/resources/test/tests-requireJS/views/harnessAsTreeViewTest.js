/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

require(["harnessAsTreeView", "harnessLayouts"], function (systemUnderTest, harnessCollection)
{
    describe('harnessAsTreeViewTest', function ()
    {
        "use strict";

        function getTestableCollection()
        {
            return [
                {
                    "id": "harness-uid1",
                    "mainText": "Harness-design1",
                    "subText": "random title",
                    "folders": ["folder1\\folder11\\design1"],
                    "tooltips": [{
                        name: "Revision",
                        value: "rev1"
                    }, {
                        name: "Part Number",
                        value: "01-002-001"
                    }]
                },
                {
                    "id": "harness-uid2",
                    "mainText": "Design2",
                    "subText": "WYSIWYG",
                    "folders": ["folder1\\folder12\\design2"],
                    "tooltips": [{
                        name: "Revision",
                        value: "B2B"
                    }, {
                        name: "Part Number",
                        value: "01-003-001"
                    }]
                },
                {
                    "id": "harness-uid3",
                    "mainText": "Design1",
                    "subText": "Subtitle for design3",
                    "folders": ["folder1\\folder12\\Derivative\\design-derivative"],
                    "tooltips": [{
                        name: "Revision",
                        value: "Revision_1"
                    }, {
                        name: "Part Number",
                        value: "01-001-001"
                    }]
                }
            ];
        };

        beforeEach(function ()
        {
            var testableCollection = getTestableCollection();
            harnessCollection.reset(testableCollection, {silent: true});
        });

        afterEach(function ()
        {
            harnessCollection.reset(null, {silent: true});
        });

        it("should load the module", function ()
        {
            expect(systemUnderTest).toBeDefined();
            expect(systemUnderTest.title).toBe("HarnessLayouts");
            expect(systemUnderTest.cssClass).toBe("harness-layouts");
        });

        it("should return unique model key for harness view", function ()
        {
            expect(systemUnderTest.getModelIdString()).toBe("id");
        });

        it("should contain models which are set to collection", function ()
        {
            // no default harness models in global context
            var models = systemUnderTest.getData().getModels();
            var modesIdsAsString = models.map(model => model.get("id")).join("||");
            expect(modesIdsAsString).toBe('harness-uid1||harness-uid2||harness-uid3');
        });

        it("created tree html should contain all folder nodes and design name", function ()
        {
            var models = systemUnderTest.getData().getModels();
            var folderData = systemUnderTest.createTreeDataFromDesigns(models);
            var treeHtml = systemUnderTest.renderFolder(folderData.folders.folder1);
            var expectedFolderNodes = [
                "folder1",
                "folder11",
                "design1",
                "Harness-design1:01-002-001:rev1",
                "folder12",
                "Derivative",
                "design-derivative",
                "Design1:01-001-001:Revision_1",
                "design2",
                "Design2:01-003-001:B2B"
            ];
            expect($(treeHtml).text()).toBe(expectedFolderNodes.join(""));
        });

        it("should return translated value for specific strings", function ()
        {
            var strArr = ["Derivatives", "Functional Modules", "Production Modules", "any Other String",
                "something-else"];

            var translatedStrArr = strArr.map(k => systemUnderTest.getFolderDisplayLabel(k));
            expect(JSON.stringify(translatedStrArr)).toBe(
                '["derivatives.label","fm.modules.label","pm.modules.label","any Other String","something-else"]');
        });
    });
}, function (err)
{
    describe("harnessAsTreeViewTest - module load Error", function ()
    {
        it("Module load failed", function ()
        {
            console.log(err.message + "::\n" + err.stack);
            expect(false).toBeTruthy();
        });
    });
});