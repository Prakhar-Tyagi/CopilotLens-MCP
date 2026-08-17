/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

describe("popoutHandlerTest", function ()
{
    var contentType = mentor.publisher.contentType;
    it("should be able to construct global reportURL", function ()
    {
        var reportURL = mentor.publisher.popoutHandler.createURL({
            type: "capitalreport",
            mainText: "reportTitle",
            path: "reportPath",
            projectId: "testProjectId"
        });
        expect(reportURL).toBe("popout.html#/globalReport/reportTitle/testProjectId/reportPath");
    });
    it("should be able to construct location view URL", function ()
    {
        var reportURL = mentor.publisher.popoutHandler.createURL({
            type: contentType.LOCATION_VIEWS,
            mainText: "Top View",
            path: "1/1.svg",
            objectId: "objectId",
            projectId: "projectId"
        });
        expect(reportURL).toBe("popout.html#/showLocation/Top%20View/projectId/objectId");
    });

    it("should be able to construct system diagram URL", function ()
    {
        var reportURL = mentor.publisher.popoutHandler.createURL({
            type: contentType.SYSTEM_SVG,
            mainText: "system1",
            path: "1/1.svg",
            objectId: "objectId",
            diagramId: "diagramId",
            systemId: "systemId",
            projectId: "projectId"
        });
        expect(reportURL).toBe("popout.html#/system/systemId/diagramId/projectId/objectId");
    });

    it("should be able to open popout window", function () {
        spyOn(mentor.publisher.popoutHandler, "openPopout").andCallThrough();
        mentor.publisher.popoutHandler.openPopoutWindow();
        expect(mentor.publisher.popoutHandler.openPopout).toHaveBeenCalled();
    });

    it("should be able to highlight object, signal paths and close popovers", function () {
        var obj = {
            fromMainWindow: false,
        }
        mentor.publisher.popoutHandler.highlighObject("testUID", "testSourceContainerID", true, "test2DHotSpotText", "testPopoutID", obj);
        mentor.publisher.popoutHandler.highlightSignalPath([], false, "red", {popoutId: "testPopoutID"});
        mentor.publisher.popoutHandler.closePopover();
        expect(obj.fromMainWindow).toBeTruthy();
    });

});