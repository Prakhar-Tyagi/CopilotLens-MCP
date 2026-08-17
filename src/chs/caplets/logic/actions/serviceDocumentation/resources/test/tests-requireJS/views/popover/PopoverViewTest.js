/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
(function () {
    "use strict";
    var dummyModel = new (Backbone.Model.extend())(),
        template = "<%=items.length%><%=className%><%=showTitle%><%=title%><%=JSON.stringify(items)%>",
        context, stubs,
        createDummyView = function (noData) {
            return {
                container: ".popoverItem",
                templateHTML: template,
                getClassName: function () {
                    return "testClass";
                },
                getData: function () {
                    var Model = Backbone.Model.extend({
                        getModels: function () {
                            if (noData) {
                                return [];
                            }
                            return [{
                                    get: function () {
                                        return function () {
                                            return 'diagram1';
                                        };
                                    }}, {
                                    get: function () {
                                        return function () {
                                            return 'diagram2';
                                        };
                                    }
                                }];
                        }});
                    return new Model();
                }
            };
        },
        xrefBuilderModel = new (Backbone.Model.extend())();

    stubs = {
        currentPackage: dummyModel,
        jquery: $,
        DragController: jQuery.noop,
        underscore: _,
        backbone: Backbone,
        BasicPopoverView: Backbone.View.extend({}),
        XRefBuilderModel: xrefBuilderModel
    };
    context = createContext(stubs);

    context(['PopoverView'], function (PopoverView) {
        describe("PopoverViewTest", function () {

            it("should be able to load PopoverView Module", function () {
                expect(PopoverView).toBeDefined();
            });

            it("should be able get object popover title from object specific template", function () {
                var actualTemplate, goldenTemplate = "Wire.html", underscoreTemplate;
                var popoverView = new PopoverView();
                popoverView.loadTitleTemplateForObject = function (template) {
                    actualTemplate = template;
                    return {}
                };
                popoverView.loadObjectTitleTemplate = function (designObject, templateFile, callback) {
                    underscoreTemplate = templateFile;
                };
                popoverView.getObjectTitleAndRenderPopover({
                    getType: function() {
                        return "Wire";
                    }
                }, function(){
                });
                expect(underscoreTemplate).toBe("text!templates/p/Wire.html");
            });

            it("should load default template for title if object specific template is not available", function () {
                var actualTemplate, goldenTemplate = "Wire.html", underscoreTemplate;
                var popoverView = new PopoverView();
                popoverView.loadTitleTemplateForObject = function (template) {
                    actualTemplate = template;
                    return {
                        error: true
                    }
                };
                popoverView.loadObjectTitleTemplate = function (designObject, templateFile, callback) {
                    underscoreTemplate = templateFile;
                };
                popoverView.getObjectTitleAndRenderPopover({
                    getType: function() {
                        return "Wire";
                    }
                }, function(){
                });
                expect(underscoreTemplate).toBe("text!templates/p/objectTitle.html");
            });

            it("should be able to get title of object popover", function () {
                var popoverView = new PopoverView(), title;
                title = popoverView.getObjectType({
                    getType: function (){
                        return "Device";
                    },
                    getName: function (){
                        return "battery";
                    }
                }, {
                    localize: function (text){
                        return text + "_localized";
                    }
                });
                expect(title).toBe("Device_localized battery_localized");
            });

            it("should be able to get title of object popover with configured title for object", function () {
                var popoverView = new PopoverView(),
                    origGetObjectPropertyToUseForTitle = mentor.publisher.dataLoader.getObjectPropertyToUseForTitle,
                    title;
                mentor.publisher.dataLoader.getObjectPropertyToUseForTitle=function (type) {return "testConfiguredTitle"};
                title = popoverView.getObjectType({
                    getType: function (){
                        return "Device";
                    },
                    getName: function (){
                        return "battery";
                    }
                }, {
                    localize: function (text){
                        return text + "_localized";
                    }
                });

                expect(title).toBe("testConfiguredTitle");

                mentor.publisher.dataLoader.getObjectPropertyToUseForTitle=origGetObjectPropertyToUseForTitle;
            });

            it("should be able to handle start, end touches and handle dragging", function () {
                var popoverView = new PopoverView(),
                    e={
                        touches: {
                            length: 1,
                        }
                    },
                    origDraggable=popoverView.draggable;
                popoverView.draggable={
                    startDragging: function (e) {},
                    endDragging: function (e) {},
                };
                spyOn(popoverView.draggable, "startDragging");
                spyOn(popoverView.draggable, "endDragging");

                popoverView.touchStartHandler(e);
                expect(popoverView.draggable.startDragging).toHaveBeenCalled();
                popoverView.touchEndHandler(e);
                expect(popoverView.draggable.endDragging).toHaveBeenCalled();
                popoverView.startDragging(e);
                expect(popoverView.draggable.startDragging).toHaveBeenCalled();
                popoverView.endDragging(e);
                expect(popoverView.draggable.endDragging).toHaveBeenCalled();

                popoverView.draggable=origDraggable;
            });

            it("should be able to fetch the builder model when the button is clicked", function () {
                var popoverView = new PopoverView(),
                    evt={
                        stopPropagation: function () {},
                    };
                spyOn(xrefBuilderModel, "fetch");
                popoverView.xrefConfigurationBuilderButtonClicked(evt);
                expect(xrefBuilderModel.fetch).toHaveBeenCalled();
            });

            it("should be able to return when rendered without proper params", function () {
                var popoverView = new PopoverView(),
                    popOverModel = new (Backbone.Model.extend())();
                expect(popoverView.render()).toBeUndefined();
                expect(popoverView.render(popOverModel)).toBeUndefined();
            });

            it("should be able to render", function () {
                var popoverView = new PopoverView(),
                    popOverModel = new (Backbone.Model.extend())(),
                    designObj={
                        getSignalTraceFiles: function () {return {fullInstanceFile: {}, signalTraceFile: {}}},
                    },
                origGetObjectTitleAndRenderPopover=popoverView.getObjectTitleAndRenderPopover;
                popOverModel.set("showFilter", true);
                popOverModel.set("showXrefBuilderButton", true);
                popOverModel.set("x", 100);
                popOverModel.set("y", 100);
                popOverModel.set("popoverModel", designObj);

                popoverView.templateHTML = "<%=title%>";

                popoverView.getCoordinates=function (paramX, paramY) {return {x: 100, y:100}};
                popoverView.getObjectTitleAndRenderPopover=function (designObject, callback) {callback()};


                popoverView.render(popOverModel);

                popoverView.getObjectTitleAndRenderPopover=origGetObjectTitleAndRenderPopover;
            });

            it("should be able to load title template, trace signals, and get render connectivity button tool tip", function () {
                var popoverView = new PopoverView(),
                    origLoadXMLByAjax=mentor.publisher.xmlLoader.loadXMLByAjax,
                    origLocalize=mentor.publisher.languageTranslator.localize
                ;

                mentor.publisher.xmlLoader.loadXMLByAjax=function () {};
                spyOn(mentor.publisher.xmlLoader, "loadXMLByAjax");
                popoverView.loadTitleTemplateForObject({});
                expect(mentor.publisher.xmlLoader.loadXMLByAjax).toHaveBeenCalled();
                mentor.publisher.xmlLoader.loadXMLByAjax=origLoadXMLByAjax;


                mentor.publisher.languageTranslator.localize=function () {};
                spyOn(mentor.publisher.languageTranslator, "localize");
                popoverView.getRenderConnectivityBtnToolTip();
                expect(mentor.publisher.languageTranslator.localize).toHaveBeenCalled();
                mentor.publisher.languageTranslator.localize=origLocalize;

            });
        });
    }, function (err){
        describe("popoverLoadFails", function () {
            it("should be able to load popoverView", function () {
                expect(err).toBeUndefined();
            });
        })
    });
})();

