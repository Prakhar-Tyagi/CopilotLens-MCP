/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["filehandlers/systemSVGHandler"], function (systemSVGHandler) {
    "use strict";
    describe("systemSVGHandlerTest", function () {
        it("should be able to load systemSVGHandler", function () {
            expect(systemSVGHandler).toBeTruthy();
        });
        it("should highlight object if system diagram is open", function () {
            var expectedObject;
            var selectedSystem = new (Backbone.Model.extend({}))();
            selectedSystem.set("diagramId", "testDiagramId");
            selectedSystem.set("optionExpression", "testoptionExpression");
            systemSVGHandler.highlightObject = function (content) {
                expectedObject = content;
            }
            systemSVGHandler.openDiagram(selectedSystem, "testDiagramId", "testoptionExpression",
                    {objectId: "testId", systemId: "testSystemID"});
            expect(expectedObject.objectId).toBe('testId')
            expect(expectedObject.systemId).toBe('testSystemID')
        });
    });

});
