/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global require, describe, it, expect, beforeEach, afterEach, mentor, runs, waitsFor, $*/
(function () {
    "use strict";

    var context,
        spy = mentor.publisher.popoutHandler,
        stubs,
        Model = Backbone.Model.extend(),
        Collection = Backbone.Collection.extend(),
        fileDisplayHandler = {
            display : function (content) {
                this.content = content;
            },
            addFileHandler : function() {
                console.log('no-op dummy handler');
            }
        },
        View = function (collection) {
            return Backbone.View.extend();
        };

    stubs = {
        jquery : $,
        underscore : _,
        backbone : Backbone,
        currentPackage : new Model(),
        fileDisplayHandler : fileDisplayHandler,
        SectionCollection : Backbone.Model.extend(),
        ListView : View
    };

    stubs.currentPackage.set({id : "packageId"});
    context = createContext(stubs);
    var preGet;

    context(['DesignObjectsView', "models/selectedSystem", "currentPackage"],
        function (DesignObjectsView, selectedSystem, currentPackage) {
            var wires = new (Backbone.Collection.extend())(), wiresView;

            describe("DesignObjectsViewTest", function () {

                beforeEach(function () {
                    $('body').html('');
                    $('body').append($("<div id='wires'></div>"))
                    wires.getModels = function () {
                        return wires.models;
                    };
                    wires.reset([
                        {mainText : "wire1", id : "wire1"},
                        {mainText : "wire2", id : "wire2"}
                    ]);

                    var WireView = DesignObjectsView(wires);
                    wiresView = new WireView();
                    wiresView.templateHTML =
                        '<%if(items.length){ %><% if(page > 1){ %><div class="listItem next_prevous_btn previous"></div><%}%><% _.each(items, function(item) {%><div data-id="<%=item.id%>"><%=item.get("mainText")%></div><% });%><% if( totalPages > page){ %><div class="listItem next_prevous_btn next"></div><%}%><%}%>';
                    wiresView.container = "#wires"
                });

                afterEach(function () {
                    $("#wires").remove();
                });

                it("should be able to load DesignObjectsView Module", function () {
                    expect(DesignObjectsView).toBeDefined();
                });

                it("should be able to render DesignObjectsView Module", function () {
                    wiresView.render();
                    expect($('body').html()).toBe('<div id="wires"><div data-id="wire1">wire1</div><div data-id="wire2">wire2</div></div>');
                });

                it("should be able to provide pagination", function () {
                    wiresView.itemsPerPage = 1;
                    var bodyEle = $('body');
                    runs(function() {
                        wiresView.render();
                        var htmlFirstPage = $(bodyEle).html();
                        expect(htmlFirstPage).toBe('<div id="wires"><div data-id="wire1">wire1</div><div class="listItem next_prevous_btn next"></div></div>');
                        wiresView.showNextPage({
                            stopPropagation: function(){}
                        });
                    });

                    waitsFor(function() {
                        return $('#wires').html().indexOf('wire2') != -1;
                    },1000);

					runs(function() {
                        expect($(bodyEle).html()).toBe('<div id="wires"><div class="listItem next_prevous_btn previous"></div><div data-id="wire2">wire2</div></div>');
                    });
                });

				it("should be able filter the wires after pagination", function () {
					wiresView.itemsPerPage = 1;
					wiresView.render();
					wiresView.showNextPage({
						stopPropagation: function(){}
					});
					wiresView.showObjectsThatMatchesSearchText();
					expect(wiresView.page).toBe(1);
				});

                it("should be able filter objects based on open diagram", function () {
                    var presentDiagram  ={
                        get : function () {
                            return function(){return "testDId";};
                        }
                    };
                    selectedSystem.set("diagramId", "testbvcDId", {silent : true});
                    wiresView.itemsPerPage = 1;
                    wiresView.render();

                    var filteredItems = wiresView.filterForDiagram([
                        presentDiagram,
                        {
                            get : function () {
                                return function(){return "33t4estDId2";};
                            }
                        }
                    ]);
                    expect(presentDiagram.isActive).toBe('panelitem_hide');
                });

                it("should be able to collapse all", function () {
                    var evt = {
                        target: 'target',
                        stopPropagation: function () {},
                    };
                    spyOn(evt, 'stopPropagation');
                    wiresView.collapseAll(evt);
                    expect(evt.stopPropagation).toHaveBeenCalled();
                });

                it("should be able to expand all", function () {
                    wiresView.isExpanded = true;
                    wiresView.expandAll();
                    expect(wiresView.isExpanded).toBe(true);
                });

                it("should be able to highlight previous selection", function () {
                    selectedSystem.set("selectedElement", "selectedElement");
                    spyOn(selectedSystem, "get").andCallThrough();
                    wiresView.highlightPreviousSelection();
                    expect(selectedSystem.get).toHaveBeenCalledWith("selectedElement");
                });

                it("should be able to collapse all", function () {
                    var evt = {
                        stopPropagation: function () {},
                    };
                    spyOn(evt, 'stopPropagation');
                    wiresView.popOut(evt);
                    expect(evt.stopPropagation).toHaveBeenCalled();
                });

                it("should be able to highlight on click", function () {
                    var evt = {
                        currentTarget: 'target'
                    };
                    expect(wiresView.highlightOnClick(evt)).toBeFalsy();
                });

                it("should be able to click on list item", function () {
                    var evt = {
                            currentTarget: 'target',
                            stopPropagation: function () {}
                        },
                        origHighlightOnClick=wiresView.highlightOnClick,
                        origIsContentTypeOpen=wiresView.isContentTypeOpen
                        ;
                    wiresView.highlightOnClick=function () {return true};
                    wiresView.isContentTypeOpen=function () {return true};

                    spyOn(wiresView, "highlightOnClick").andCallThrough();
                    wiresView.listItemClicked(evt);
                    expect(wiresView.highlightOnClick).toHaveBeenCalled();

                    wiresView.highlightOnClick=origHighlightOnClick;
                    wiresView.isContentTypeOpen=origIsContentTypeOpen;
                });

                it("should be able to get content type and get isContentTypeOpen", function () {
                    expect(wiresView.getContentType()).toBe("");
                    var origIsContentActive=mentor.publisher.detailLayoutManager.isContentActive;
                    mentor.publisher.detailLayoutManager.isContentActive=function () {return true};

                    expect(wiresView.isContentTypeOpen()).toBeTruthy();

                    mentor.publisher.detailLayoutManager.isContentActive=origIsContentActive;
                });

                it("should be able to call appropriate functions on mouse out and mouse over", function () {
                    var evt = {
                        currentTarget: 'target',
                        stopPropagation: function () {}
                    };
                    spyOn(wiresView, 'hideCollapseAll').andCallThrough();
                    wiresView.mouseout(evt);
                    expect(wiresView.hideCollapseAll).toHaveBeenCalled();

                    spyOn(wiresView, 'showCollapseAll').andCallThrough();
                    wiresView.mouseover(evt);
                    expect(wiresView.showCollapseAll).toHaveBeenCalled();
                });

                it("should be able to highlight objects", function () {
                    var evt = {
                            currentTarget: 'target',
                            stopPropagation: function () {}
                        },
                        collection={
                            get: function () {return {attributes: []}}
                        };
                    spyOn(mentor.publisher.eventDispatcher, "dispatchEvent");
                    wiresView.highlightObject(evt, collection);
                    expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
                });

                it("should be able to call appropriate functions when header is clicked", function () {
                    wiresView.expanded=true;
                    wiresView.headerClicked({});
                    expect(wiresView.expanded).toBeFalsy();
                });

                it("should be able to show previous page", function () {
                    var evt = {
                            stopPropagation: function () {}
                        };
                    spyOn(evt, 'stopPropagation');
                    wiresView.showPreviousPage(evt);
                    expect(evt.stopPropagation).toHaveBeenCalled();
                });

                it("should be able to call appropriate functions on clicked", function () {
                    var evt = {
                                currentTarget: 'target',
                                stopPropagation: function () {}
                            },
                            origGetData=wiresView.getData,
                            designObject=new (Backbone.Model.extend({systemId: "testSystemId"}))()
                    ;
                    wiresView.getData=function () {return {get: function () {return designObject}}};
                    spyOn(mentor.publisher.eventDispatcher, "dispatchEvent");
                    wiresView.clicked(evt);
                    expect(mentor.publisher.eventDispatcher.dispatchEvent).toHaveBeenCalled();
                    wiresView.getData=origGetData;
                });
            });
        });
})();

