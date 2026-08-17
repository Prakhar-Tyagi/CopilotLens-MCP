/*global require, describe, it, expect, mentor, beforeEach, Backbone*/
require(["routers/multipleDocumentRouter"], function (multipleDocumentRouter)
{
    "use strict";
    describe("multipleDocumentRouterTest", function ()
    {

        var actualContent = [];
        var p = mentor.publisher;
        var system = new (Backbone.Model.extend({}))();
        var getData;

        beforeEach(function ()
        {
            actualContent = [];
            multipleDocumentRouter.setDocumentDisplayHandler({
                display: function (content)
                {
                    actualContent.push(content);
                }
            });
            multipleDocumentRouter.setSelectedSystem(system);
            multipleDocumentRouter.history = Backbone.history;

            multipleDocumentRouter.handleSingleDocument = multipleDocumentRouter['old_handleSingleDocument'];

            multipleDocumentRouter.extractDocData = multipleDocumentRouter['old_extractDocData'];

            multipleDocumentRouter.createURL = multipleDocumentRouter['old_createURL'];
           
            getData = mentor.publisher.project.getData;
            mentor.publisher.project.getData = function ()
            {
                return [{mainText: "report1", path: "path1"}, {mainText: "report2", path: "path2"}];
            }
        });
        afterEach(function ()
        {
            mentor.publisher.project.getData = getData;
        });

        it("should be able to render one document page", function ()
        {
            var golden = getInformationPage();
            var success = multipleDocumentRouter.render("", "", JSON.stringify(golden));
            expect(success).toBeTruthy();
            expect(actualContent.length).toBe(1);
            expect(actualContent[0].doNotSaveAsHistory).toBeTruthy();
            expect(actualContent[0].reset).toBeTruthy();
            golden.reset = true;
            golden.doNotSaveAsHistory = true;
            expect(JSON.stringify(actualContent[0])).toBe(JSON.stringify(golden));
        });

        it("should be able to highlight object using object Id", function ()
        {

            system.set("objectId", "obj1", {silent: true});
            var sys = getSystemSVG();
            var objectIdActual;
            multipleDocumentRouter.setObjectHighlighter({
                initCrossHighlight: function (objectId)
                {
                    objectIdActual = objectId;
                }
            });
            multipleDocumentRouter.render("testObjectId", "", JSON.stringify(sys));
            expect(system.get("objectId")).toBeFalsy();
            expect(objectIdActual).toBe("testObjectId");

        });

        it("should be able to highlight selected document", function ()
        {

            system.set("id", "", {silent: true});
            var sys = getSystemSVG();
            var objectIdActual;
            multipleDocumentRouter.render("", "selectedDoc", JSON.stringify(sys));
            expect(system.get("id")).toBe("selectedDoc");

        });

        it("should be able to render two documents pages", function ()
        {
            var golden1 = getInformationPage();
            var golden2 = getSystemSVG();
            var success = multipleDocumentRouter.render("", "", JSON.stringify(golden1), JSON.stringify(golden2));
            expect(success).toBeTruthy();
            expect(actualContent.length).toBe(2);
        });

        it("should be able to render three documents pages", function ()
        {
            var golden1 = getInformationPage();
            var golden2 = getInformationPage();
            var golden3 = getLocationView();
            var success = multipleDocumentRouter.render("", "", JSON.stringify(golden1), JSON.stringify(golden2),
                    JSON.stringify(golden3));
            expect(success).toBeTruthy();
            expect(actualContent.length).toBe(3);
        });

        it("should be able to save  content panel state when two documents are open", function ()
        {
            var URLSaved, options;
            var golden1 = getInformationPage();
            var golden3 = getLocationView();
            setContentArea([golden1, golden3]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url, op)
                {
                    URLSaved = url;
                    options = op;
                }
            });

            multipleDocumentRouter.save(true);
            expect(URLSaved).toBe(
                    "document_views/id1/effRange/projId/objectId/curDoc/%7B%22id%22%3A%22Information-page%22%2C%22path%22%3A%22information-page.pdf%22%2C%22type%22%3A%22customView%22%7D/%7B%22id%22%3A%22location-view" +
                    "-diagram%22%2C%22path%22%3A%22location-view.SVG%22%2C%22type%22%3A%22locationviews%22%7D");
            expect(options.trigger).toBeFalsy();
        });

        it("should be able to save  content panel state when two documents are open", function ()
        {
            var URLSaved, options;
            var golden1 = getInformationPage();
            var golden3 = getLocationView();
            setContentArea([golden1, golden3]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url, op)
                {
                    URLSaved = url;
                    options = op;
                }
            });

            multipleDocumentRouter.save(true);
            expect(URLSaved.indexOf('Information-page')).toBeTruthy();
            expect(options.trigger).toBeFalsy();
        });

        it("should be able to save  content panel state when three documents are open", function ()
        {
            var URLSaved, options;
            var golden1 = getInformationPage();
            var golden2 = getSystemSVG();
            var golden3 = getLocationView();
            setContentArea([golden1, golden2, golden3]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url, op)
                {
                    URLSaved = url;
                    options = op;
                }
            });

            multipleDocumentRouter.save(true);
            expect(URLSaved).toBe(
                    "document_views/id1/effRange/projId/objectId/curDoc/%7B%22id%22%3A%22Information-page%22%2C%22path%22%3A%22information-page.pdf%22%2C%22type%22%3A%22customView%22%7D/%7B%22id%22%3A%22sytem-diagram%22%2C%22path%22%3A%22sytem-diagram." +
                    "SVG%22%2C%22type%22%3A%22systemSVG%22%7D/%7B%22id%22%3A%22location-view-diagram%22%2C%22path%22%3A%22location-view.SVG%22%2C%22type%22%3A%22locationviews%22%7D");
            expect(options.trigger).toBeFalsy();
        });
        it("should be able to save highlighted object Id", function ()
        {
            var URLSaved;
            setContentArea([getSystemSVG()]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url)
                {
                    URLSaved = url;
                }
            });
            multipleDocumentRouter.save(true, "testObjectId");
            expect(URLSaved).toBe("document_views/id1/effRange/projId/objectIdtestObjectId/curDoc" +
                    "/%7B%22id%22%3A%22sytem-diagram%22%2C%22path%22%3A%22sytem-diagram.SVG%22%2C%22type%22%3A%22systemSVG%22%7D");
        });

        it("should not save URL when  doNotHash flag is false", function ()
        {
            var URLSaved;
            setContentArea([getSystemSVG()]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url)
                {
                    URLSaved = url;
                }
            });
            multipleDocumentRouter.save(false, "testObjectId");
            expect(URLSaved).toBeUndefined();
        });

        it("back navigation should preserve navigation panel highlight", function ()
        {
            var URLSaved;
            setContentArea([getSystemSVG()]);
            multipleDocumentRouter.setHistoryTracker({
                navigate: function (url)
                {
                    URLSaved = url;
                }
            });
            multipleDocumentRouter.save(true, "testObjectId", "selectedDocumentObjectId");
            expect(URLSaved).toBe("document_views/id1/effRange/projId/objectIdtestObjectId/curDocselectedDocumentObjectId" +
                    "/%7B%22id%22%3A%22sytem-diagram%22%2C%22path%22%3A%22sytem-diagram.SVG%22%2C%22type%22%3A%22systemSVG%22%7D");
        });

        it("should close any popover during history navigation", function ()
        {
            var popoverClosed = false;
            var golden1 = getInformationPage();
            p.eventDispatcher.attachEventListener(p.events.CLOSE_POPOVER, function ()
            {
                popoverClosed = true;
            });
            var success = multipleDocumentRouter.render("", "", JSON.stringify(golden1));
            expect(success).toBeTruthy();
            expect(popoverClosed).toBeTruthy();
        });

        function getSystemSVG()
        {
            var golden2 = {
                id: "sytem-diagram",
                path: "sytem-diagram.SVG",
                type: mentor.publisher.contentType.SYSTEM_SVG
            };
            return golden2;
        }

        function getInformationPage()
        {
            var golden1 = {
                id: "Information-page",
                path: "information-page.pdf",
                type: mentor.publisher.contentType.CUSTOM_VIEW
            };
            return golden1;
        }

        function setContentArea(contentArr)
        {
            multipleDocumentRouter.setContentArea({
                getAllOpenContentDetails: function ()
                {
                    return contentArr;
                },
                getNoOfOpenPanels: function ()
                {
                    return contentArr.length;
                }
            });
        }

        function getLocationView()
        {
            var golden3 = {
                id: "location-view-diagram",
                path: "location-view.SVG",
                type: mentor.publisher.contentType.LOCATION_VIEWS
            };
            return golden3;
        }
    });
});
