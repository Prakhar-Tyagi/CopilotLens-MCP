/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function ()
{
    "use strict";
    var dummyModel = new (Backbone.Model.extend())(),
        context, stubs, customViewRendered = false,
        View = function (collection)
        {
            return Backbone.View.extend();
        },
        Model = Backbone.Model.extend(),
        fileDisplayHandler = {
            display: function () {},
        }
    ;

    stubs = {
        currentPackage: dummyModel,
        jquery: $,
        underscore: _,
        backbone: Backbone,
        ComponentLoader: {
            getComponentViewByName: function (panel)
            {
                if (panel === "custom_plugin") {
                    return {
                        render: function ()
                        {
                            customViewRendered = true;
                        }
                    }
                }
            }
        },
        fileDisplayHandler: fileDisplayHandler,
        ListView: View,
        SectionCollection: Backbone.Collection,
    };

    context = createContext(stubs);

    context(['views/customProjectDataListView'], function (customProjectDataListView)
    {
        describe("customProjectDataListViewTest", function ()
        {

            beforeEach(function ()
            {
            });
            it("should be able to load customProjectDataListView Module", function ()
            {
                expect(customProjectDataListView).toBeDefined();
            });

            it("should be able to load custom view for a project level plugin", function ()
            {
                customViewRendered = false;
                customProjectDataListView.loadViewForCustomData("custom_plugin", {});
                expect(customViewRendered).toBeTruthy();
            });

            it("should be able to load custom view by default view if custom view is not present", function ()
            {
                var useDefaultView = false,
                    origUseDefaultView=customProjectDataListView.useDefaultView;
                customViewRendered = false;
                customProjectDataListView.useDefaultView = function () {
                    useDefaultView = true;
                };

                customProjectDataListView.loadViewForCustomData("no_custom_view_for_the_plugin", {});
                expect(customViewRendered).toBeFalsy();
                expect(useDefaultView).toBeTruthy();
                customProjectDataListView.useDefaultView=origUseDefaultView;
            });

            it("should be able to get data and get panel data", function () {
                var origGetCustomData = mentor.publisher.project.getCustomData,
                    origGetData = mentor.publisher.project.getData;

                mentor.publisher.project.getCustomData = function () {};
                mentor.publisher.project.getData = function () {};

                spyOn(mentor.publisher.project, "getCustomData");
                spyOn(mentor.publisher.project, "getData");
                customProjectDataListView.getData();
                customProjectDataListView.getPanelData("samplePanelType");
                expect(mentor.publisher.project.getCustomData).toHaveBeenCalled();
                expect(mentor.publisher.project.getData).toHaveBeenCalled();

                mentor.publisher.project.getCustomData = origGetCustomData;
                mentor.publisher.project.getData = origGetData;
            });

            it("should be able to use default view", function () {
                var customView=customProjectDataListView.useDefaultView("samplePanel", {}),
                    evt = {
                        stopPropagation: function () {},
                        target: 'target',
                        currentTarget: 'target',
                    };
                customView.beforeViewRender();
                var content=new (Backbone.Model.extend({
                    id: function () {},
                    mainText: "testMainText",
                    path: "testPath",
                    type: "testType"
                }))();

                customView.getData=function () {
                    return {
                        get: function (id) {
                            return content;
                        }
                    }
                }
                spyOn(fileDisplayHandler, "display");
                customView.clicked(evt);
                expect(fileDisplayHandler.display).toHaveBeenCalled();

                spyOn(evt, "stopPropagation");
                customView.popOut(evt);
                expect(evt.stopPropagation).toHaveBeenCalled();
            });

            it("should be able to create custom panel", function () {
                spyOn(customProjectDataListView, "loadViewForCustomData");
                customProjectDataListView.createCustomPanel();
                expect(customProjectDataListView.loadViewForCustomData).toHaveBeenCalled();
            });

            it("should be able to panel already created", function () {
                expect(customProjectDataListView.panelAlreadyCreated('sampleText g')).toBeFalsy();
            });

            it("should be able to render", function () {
                customProjectDataListView.getData=function () {
                    return [
                        {
                            name: "panel1",
                            type: "panelType"
                        },
                        {
                            name: "panel2",
                            type: "panelType"
                        }
                    ];
                };
                customProjectDataListView.createCustomPanel=function () {};
                spyOn(customProjectDataListView, "createCustomPanel");
                customProjectDataListView.render();
                expect(customProjectDataListView.createCustomPanel).toHaveBeenCalled();
            });

        });
    });
})();

