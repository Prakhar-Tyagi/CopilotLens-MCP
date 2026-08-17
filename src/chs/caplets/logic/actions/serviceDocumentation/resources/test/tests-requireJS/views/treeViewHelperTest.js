/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
require(["underscore", "treeViewHelper", "systemsAsTreeView"], function (_, treeViewHelper, systems)
{
    describe('TreeViewHelperTest', function ()
    {
        "use strict";
        function getTestableModel(obj)
        {
            var thisModel = _.extend({}, {
                idAttribute: obj.idAttribute,
                mainText: obj.mainText,
                subText: obj.subText,
                getRevision: function() {
                    return obj.revision;
                },
                getFolders: function() {
                    return obj.folders;
                }
            });
            return new Backbone.Model(thisModel);
        }

        var origTranslate;
        var origDiagAsSystems;

        beforeEach(function() {
            origTranslate = Utils.translate;
            Utils.translate = function(str) {
                return str;
            }

            origDiagAsSystems = getWindowObj().diagramAsSystemsObjectFactoryImpl;
            getWindowObj().diagramAsSystemsObjectFactoryImpl = false;
        });

        afterEach(function() {
           Utils.translate = origTranslate;
           getWindowObj().diagramAsSystemsObjectFactoryImpl = origDiagAsSystems;
        });

        it("Logic designs - should create tree html", function ()
        {
            var models = systems.getData().getModels();
            var treeHtml = treeViewHelper.createTreeView(models, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: systems.getFileLabel,
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders
            });
            var orderedText = $(treeHtml).text();
            expect(orderedText).toBe('alphabravocharliesystemNamesystemName::systemSubTitle');
        });

        it("Tree Views are collapsed by default", function ()
        {
            var models = systems.getData().getModels();
            var treeHtml = treeViewHelper.createTreeView(models, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: systems.getFileLabel,
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders
            });
            var defaultTreeStyle = $(treeHtml).attr('style');
            expect(defaultTreeStyle).toBe('display: none');
        });

        it("Logic designs - should sort files alphaNumerically ", function ()
        {
            var model1 = getTestableModel({
                idAttribute: "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram2",
                subText: "short desc dig2",
                revision: "1",
                folders: "root\\first-level"
            });
            var model2 = getTestableModel({
                idAttribute: "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllLoopBundlesAsFlex",
                revision: "1b",
                folders: "root\\first-level"
            });
            var model3 = getTestableModel({
                idAttribute: "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram1",
                revision: "2",
                folders: "root\\first-level"
            });
            var model4 = getTestableModel({
                idAttribute: "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "FormboardWithHiddenContent",
                revision: "REV1",
                folders: "root\\first-level"
            });
            var model5 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllBundlesAsFlex",
                revision: "OBS1",
                folders: "root\\first-level",
                subText: "design short desc"
            });
            var model6 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ack-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "encode",
                revision: "encode",
                folders: "root\\%23%20spl%20folder%40%20logic%20logical",
                subText: "design short desc encode"
            });

            var files = {
                "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef": model1,
                "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef": model2,
                "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef": model3,
                "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef": model4,
                "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef": model5,
                "UID8c781e-171c1262ack-57610ab04cd955cf66b1f5eb8e64a8ef": model6
            };

            var SortedtreeView = treeViewHelper.createTreeView(files, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: systems.getFileLabel,
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders
            });
            var goldenSortedArr = ["root",
                    "# spl folder@ logic logicalencode:encode:design short desc encode",
                "first-level",
                "AllBundlesAsFlex:OBS1:design short desc",
                "AllLoopBundlesAsFlex:1b",
                "Diagram1:2",
                "Diagram2:1:short desc dig2",
                "FormboardWithHiddenContent:REV1"
            ];
            expect($(SortedtreeView).text()).toBe(goldenSortedArr.join(""));
        });

        it("Logic designs - should sort folder alphaNumerically ", function ()
        {
            var model1 = getTestableModel({
                idAttribute: "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram2",
                subText: "short desc dig2",
                revision: "1",
                folders: "root\\first-level"
            });
            var model2 = getTestableModel({
                idAttribute: "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllLoopBundlesAsFlex",
                revision: "1b",
                folders: "root\\second-level"
            });
            var model3 = getTestableModel({
                idAttribute: "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram1",
                revision: "2",
                folders: "root\\1"

            });
            var model4 = getTestableModel({
                idAttribute: "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "FormboardWithHiddenContent",
                revision: "REV1",
                folders: "root\\11"
            });
            var model5 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllBundlesAsFlex",
                revision: "OBS1",
                folders: "root\\2",
                subText: "design short desc"
            });
            var model6 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ack-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "encode",
                revision: "encode",
                folders: "root\\%23%20spl%20folder%40%20logic%20logical",
                subText: "design short desc encode"
            });
            var files = {
                "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef": model1,
                "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef": model2,
                "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef": model3,
                "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef": model4,
                "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef": model5,
                "UID8c781e-171c1262ack-57610ab04cd955cf66b1f5eb8e64a8ef": model6
            };

            var SortedtreeView = treeViewHelper.createTreeView(files, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: systems.getFileLabel,
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders
            });
            var goldenSortedArr = ["root",
                    "# spl folder@ logic logical",
                    "encode:encode:design short desc encode",
                "1",
                "Diagram1:2",
                "2",
                "AllBundlesAsFlex:OBS1:design short desc",
                "11",
                "FormboardWithHiddenContent:REV1",
                "first-level",
                "Diagram2:1:short desc dig2",
                "second-level",
                "AllLoopBundlesAsFlex:1b"
            ];
            expect($(SortedtreeView).text()).toBe(goldenSortedArr.join(""));
        });

        /*
        * DOCS-9798: File separator in path data is dependent on OS on which publisher is running.
        * Code should be able to handle forward slash and treat them as separator.
        * */
        it("Folder path with forward slash should work", function ()
        {
            var model1 = getTestableModel({
                idAttribute: "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram2",
                subText: "short desc dig2",
                revision: "1",
                folders: "root/level1"
            });
            var model2 = getTestableModel({
                idAttribute: "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllLoopBundlesAsFlex",
                revision: "1b",
                folders: "root/level1/level11/~test%253s"
            });
            var model3 = getTestableModel({
                idAttribute: "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "Diagram1",
                revision: "2",
                folders: "root/level1/level12"
            });
            var model4 = getTestableModel({
                idAttribute: "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "FormboardWithHiddenContent",
                revision: "REV1",
                folders: "root/11"
            });
            var model5 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "AllBundlesAsFlex",
                revision: "OBS1",
                folders: "root/2",
                subText: "design short desc"
            });
            var files = {
                "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef": model1,
                "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef": model2,
                "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef": model3,
                "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef": model4,
                "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef": model5
            };

            var SortedtreeView = treeViewHelper.createTreeView(files, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: systems.getFileLabel,
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders
            });
            var goldenSortedArr = ["root",
                "2",
                "AllBundlesAsFlex:OBS1:design short desc",
                "11",
                "FormboardWithHiddenContent:REV1",
                "level1",
                "Diagram2:1:short desc dig2",
                "level11",
                    "~test%3s",
                "AllLoopBundlesAsFlex:1b",
                "level12",
                "Diagram1:2"
            ];
            expect($(SortedtreeView).text()).toBe(goldenSortedArr.join(""));
        });

        it("Folder path with custom delimiter should work - e.g. diagnostic", function ()
        {
            var model1 = getTestableModel({
                idAttribute: "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "DTC101",
                subText: "short desc dig2",
                folders: "root/model1#level1"
            });
            var model2 = getTestableModel({
                idAttribute: "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "DTC102",
                folders: "root/model1#level1#level11"
            });
            var model3 = getTestableModel({
                idAttribute: "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "DTC401",
                folders: "root/model2#level1"
            });
            var model4 = getTestableModel({
                idAttribute: "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "DTC205",
                folders: "root/model2#level1"
            });
            var model5 = getTestableModel({
                idAttribute: "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef",
                mainText: "DTC900",
                folders: "root/model2#level1",
                subText: "DTC short desc"
            });
            var files = {
                "UID8c781e-171c125ec34-57610ab04cd955cf66b1f5eb8e64a8ef": model1,
                "UID8c781e-171c12617f7-57610ab04cd955cf66b1f5eb8e64a8ef": model2,
                "UID8c781e-171c125fd84-57610ab04cd955cf66b1f5eb8e64a8ef": model3,
                "UID8c781e-171c1262128-57610ab04cd955cf66b1f5eb8e64a8ef": model4,
                "UID8c781e-171c1262ace-57610ab04cd955cf66b1f5eb8e64a8ef": model5
            };

            var SortedtreeView = treeViewHelper.createTreeView(files, {
                idString: systems.getModelIdString(),
                fileLabelSupplierFn: function (diagnostic) {
                    return diagnostic.get("mainText");
                },
                folderLabelSupplierFn: _.identity,
                desginFolderProviderFn: systems.getDesingfolders,
                folderSeparatorFn: function() {
                    return "#";
                }
            });
            var goldenSortedArr = ["root/model1",
                "level1",
                "DTC101",
                "level11",
                "DTC102",
                "root/model2",
                "level1",
                "DTC205",
                "DTC401",
                "DTC900"
            ];
            expect($(SortedtreeView).text()).toBe(goldenSortedArr.join(""));
        });
    }, function (err)
    {
        describe("TreeViewHelperTest - module load Error", function ()
        {
            it("Module load failed", function ()
            {
                console.log(err.message + "::\n" + err.stack);
                expect(false).toBeTruthy();
            });
        });
    });
});
