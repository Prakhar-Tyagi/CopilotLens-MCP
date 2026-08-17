/*global require, describe, it, expect, beforeEach, afterEach, mentor*/
(function ()
{
    "use strict";
    var context, spy = mentor.publisher.popoutHandler, firstDiagram = {
        type: "svg",
        id: "diagramId",
        diagramId: "diagram1",
        systemId: "system1",
        getOptionExpression: function ()
        {
            return "op1 || op2";
        }
    }, secondDiagram = {
        type: "svg",
        id: "diagramId2",
        diagramId: "diagram2",
        systemId: "system1",
        getOptionExpression: function ()
        {
            return "op1 || op3";
        }
    }, diagrams = [firstDiagram,
        secondDiagram], stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), fileDisplayHandler = {
        display: function (content)
        {
            this.content = content;
        }
    }, View = function (collection)
    {
        return Backbone.View.extend();
    };

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        currentPackage: new Model(),
        systems: new Collection(),
        fileDisplayHandler: fileDisplayHandler,
        ListView: View
    };
    var systemsData = {
        idAttribute: "systemId",
        id: "systemId",
        diagramId: "diagramId",

        getFirstDiagram: function (diagram)
        {
            return firstDiagram;
        },
        getDiagrams: function ()
        {
            return diagrams;
        },
        getReports: function ()
        {
            return [{
                id: "Wire List",
                path: "path.html",
                systemId: "system1"
            }];
        }
    };
    var systemModel = new Model(systemsData);
    stubs.systems.add(systemModel);

    stubs.currentPackage.set({id: "packageId"});
    context = createContext(stubs);
    var createEmtyDiv = function ()
    {

    };
    var filter;
    context(['views/systems'], function (systems)
    {
        var URL, oldgetObjectById;

        describe("systemsViewTest", function ()
        {
            beforeEach(function ()
            {
                filter = mentor.publisher.filter;
                mentor.publisher.filter = {vinOptions: "op1"};
                oldgetObjectById = mentor.publisher.project.getObjectById;

                mentor.publisher.project.getObjectById = function ()
                {
                    return systemsData;
                }

                systems.getDiagrams = function ()
                {
                    return [firstDiagram];
                }
                // $('body').html('');
                $('body').append(
                        $("<div id='systemList' data-id='systemId'><div data-id='systemId' id='subList'></div></div>"));
                mentor.publisher.popoutHandler = {
                    openPopout: function (url)
                    {
                        URL = url;
                    },
                    createURL: function (object)
                    {
                        return spy.createURL(object);
                    }
                }
            });

            afterEach(function ()
            {
                $('#systemList').remove();
                mentor.publisher.popoutHandler = spy;
                mentor.publisher.project.getObjectById = oldgetObjectById;
                mentor.publisher.filter = filter;
                // mentor.publisher.popoutHandler.openPopout.restore();

            });
            it("should be able to load systems view Module", function ()
            {
                expect(systems).toBeDefined();
            });

            it("should be able to open popout when popout btn is clicked", function ()
            {
                $("#subList").on("click", function (event)
                {
                    systems.popOut(event);
                });

                $("#subList").trigger("click");
                expect(URL).toBe('popout.html#/system/system1/diagram1/packageId/__objectId__');

            });

            it("should be able to open second diagram when first diagram is filtered on popout", function ()
            {
                mentor.publisher.filter.vinOptions = "op3";
                $("#subList").on("click", function (event)
                {
                    systems.popOut(event);
                });

                $("#subList").trigger("click");
                expect(URL).toBe('popout.html#/system/system1/diagram2/packageId/__objectId__');

            });

            it("should be able to open first report popout when popout btn is clicked", function ()
            {
                diagrams = [];
                $("#subList").on("click", function (event)
                {
                    systems.popOut(event);
                });

                $("#subList").trigger("click");
                expect(URL).toBe('popout.html#/report/system1/Wire List/packageId');

            });

            it("should be able to open report  when item  is clicked", function ()
            {
                firstDiagram = {
                    id: "Wire List"
                }
                $("#subList").on("click", function (event)
                {
                    systems.listItemClicked(event);
                });

                $("#subList").trigger("click");
                console.log(JSON.stringify(fileDisplayHandler.content));
                expect(JSON.stringify(fileDisplayHandler.content)).toBe(JSON.stringify({
                    "id": "systemId",
                    "reset": true,
                    type: "systemReport",
                    "reportId": "Wire List",
                    "optionExpression": "",
                    "systemId": "systemId"
                }));

            });

            it("should be able to open diagram when item is clicked", function ()
            {
                firstDiagram = {
                    id: "diagramId",
                    type: "svg"

                }

                $("#subList").on("click", function (event)
                {
                    systems.listItemClicked(event);
                });

                $("#subList").trigger("click");
                console.log(JSON.stringify(fileDisplayHandler.content));
                expect(JSON.stringify(fileDisplayHandler.content)).toBe(JSON.stringify({
                    "id": "systemId",
                    "reset": true,
                    "type": "systemReport",
                    "reportId": "Wire List",
                    "optionExpression": "",
                    "systemId": "systemId"
                }));

            });
            it("should show searched report of a system when it is clicked in navigation panel", function ()
            {
                systems.docFinder = {
                    getFirstFilteredDoc: function (id, type)
                    {
                        return {
                            id: "Wire List"
                        }
                    },
                    SHOW_FIRST_SECTION_ITEM: "showFirstItem"
                };
                $("#subList").on("click", function (event)
                {
                    systems.listItemClicked(event);
                });

                $("#subList").trigger("click");
                expect(JSON.stringify(fileDisplayHandler.content)).toBe(JSON.stringify({
                    "id": "systemId",
                    "reset": true,
                    type: "systemReport",
                    "reportId": "Wire List",
                    "optionExpression": "",
                    "systemId": "systemId"
                }));
                systems.docFinder = "";
            });

            it("should show searched diagram of a system when the system is clicked in navigation panel", function ()
            {
                systems.docFinder = {
                    getFirstFilteredDoc: function (id, type)
                    {
                        return {
                            id: "diagramId2",
                            type: "svg"
                        }
                    },
                    SHOW_FIRST_SECTION_ITEM: "showFirstItem"
                };
                $("#subList").on("click", function (event)
                {
                    systems.listItemClicked(event);
                });

                $("#subList").trigger("click");
                expect(JSON.stringify(fileDisplayHandler.content)).toBe(
                        '{"id":"systemId","reset":true,"type":"systemSVG","optionExpression":"","systemId":"systemId","diagramId":"diagramId2"}');
                systems.docFinder = "";
            });

        });

    });
})();

