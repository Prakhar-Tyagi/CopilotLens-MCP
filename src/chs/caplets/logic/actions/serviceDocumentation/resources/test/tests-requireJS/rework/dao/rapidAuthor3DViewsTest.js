/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

(function () {
    var context, stubs, Model = Backbone.Model.extend(), View = Backbone.View.extend();

    var p = mentor.publisher;

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        currentPackage: new Model(),
        "DiagramsPopoverModel": new Model(),
        "RelatedDataPopoverModel": new Model(),
        "ReportsPopoverModel": new Model(),
        SignalTracerModel: new Model(),
        ListGroupView: new View(),
        "TranslationUtils": {
            translateHTMLContent: function () {
            }
        },
        "views/appNameAndLogo/appNameAndLogoView": {
            updateApplicationNameAndLogo: function () {
            }
        },
        "internalLinkHandler": {
            addMouseEventListener: function () {
            }
        },
        "views/navigationPanelView": new View(),
        // fileDisplayHandler: fakeFileDisplayHandler,
    };

    context = createContext(stubs);

    context(["views/contentpanel/rapidAuthorCatalogPanel", "views/contentpanel/toolbar/systemToolBar",
            "views/contentpanel/toolbar/generalButtons", "ra3DModel"],
        function (rapidAuthorCatalogPanel, systemToolBar, generalButtons, ra3DModel) {

            "use strict";

            describe("3DViews visibility",
                function () {

                    var testData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><object><diagrams><diagram><name>New 3D</name><maintext>New 3D Test</maintext><type>RA</type></type></diagram></diagrams></object></object>";

                    beforeEach(function () {
                        p.rapidAuthorCatalogPanel = rapidAuthorCatalogPanel;
                        var Content = Backbone.Model.extend();
                        var content = new Content({
                            path: "",
                            type: "RA",
                            mainText: "Test",
                            objectId: "123",
                            id: "abc",
                            selectedItems: [],
                            modelName: "",
                            modelPath: "",
                            modelUrl: ""
                        });
                        rapidAuthorCatalogPanel.model = content;

                        Utils.is_msie = function () {
                            return false;
                        };
                    });

                    it("Rapid Author Catalog Link should be available if view not visible", function () {
                        p.rapidAuthorCatalogPanel.isVisible = function () {
                            return false;
                        };
                        var loader = mentor.publisher.objectDataParser;
                        var result = loader(testData, "Dummy", "Dummy").get3DViews();
                        expect(result.listItems.length).toEqual(1);
                        expect(result.listItems[0].type).toBe("RA");
                        expect(result.listItems[0].mainText).toBe("New 3D Test")
                    });

                    it("Rapid Author Catalog link should not be available if view is visible", function () {
                        p.rapidAuthorCatalogPanel.isVisible = function () {
                            return true;
                        };
                        p.rapidAuthorCatalogPanel.model.set("mainText", "New 3D Test")
                        var loader = mentor.publisher.objectDataParser;
                        var result = loader(testData, "Dummy", "Dummy").get3DViews();
                        expect(result.listItems.length).toEqual(0);
                    });

                    it("Rapid Author Catalog link should be available if a different view is visible", function () {
                        p.rapidAuthorCatalogPanel.isVisible = function () {
                            return true;
                        };
                        p.rapidAuthorCatalogPanel.model.set("mainText", "Old 3D Test")
                        var loader = mentor.publisher.objectDataParser;
                        var result = loader(testData, "Dummy", "Dummy").get3DViews();
                        expect(result.listItems.length).toEqual(1);
                        expect(result.listItems[0].type).toBe("RA");
                        expect(result.listItems[0].mainText).toBe("New 3D Test")
                    });
                });
        }
        , function (err) {
            describe("3D Views Test Failed", function () {
                it("should load the test and dependencies", function () {
                    console.log(err.message + "::\n" + err.stack);
                    console.dir(err);
                    expect(err).toBeUndefined();
                });
            });
        }
    );
})();