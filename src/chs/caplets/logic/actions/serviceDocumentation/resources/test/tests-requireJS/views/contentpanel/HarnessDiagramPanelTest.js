/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/*
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, "SISW"), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer's
 * applicable agreements with SISW.
 *
 * Copyright 2022 Siemens
 */

/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc. or its affiliates (collectively, ?SISW?), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer?s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function () {
    var stubs = {
        jquery: $,
        underscore: _,
        harnessLayoutBarHandler: Backbone.Model.extend(),
        backbone: Backbone
    };
    var context = createContext(stubs);

    context(["views/contentpanel/harnessDiagramPanel", "models/selectedSystem"], function (harnessDiagramPanel, selectedSystem) {
        "use strict";
        describe("HarnessDiagramPanelTest", function () {
            beforeEach(function () {
                var Model = Backbone.Model.extend({});
                harnessDiagramPanel.currentPackage = new Model();

            });
            it("should be able to load harnessDiagramPanel", function () {
                expect(harnessDiagramPanel).toBeDefined();
            });

            it("Test the getDataId, getContentType, getDocumentType in harnessDiagramPanel", function () {
                selectedSystem.set("harnessLayoutId", "123")
                expect(harnessDiagramPanel.getContentType()).toBe("harnessLayoutDiagram");
                expect(harnessDiagramPanel.getDataId()).toBe("123");
                expect(harnessDiagramPanel.getDocumentType()).toBe("diagrams");
            });
        });

    }, function (err) {
        describe("harnessDiagramPanel loading failed", function () {
            it("should load harness diagram panel", function () {
                expect(err).toBeTruthy();
            });
        });
    });
})();