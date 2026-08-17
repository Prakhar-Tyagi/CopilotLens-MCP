/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, describe, it, expect, Backbone, beforeEach, afterEach, mentor*/
(function ()
{
    var context, stubs, View = Backbone.View.extend(), Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), testCavityTable, systemButtonsEnabled;

    testCavityTable = '<div>CavityTable goes here</div>>';
    var faceview = new Model(), viewBtnEnabled;

    stubs = {
        jquery: $,
        underscore: _,
        backbone: Backbone,
        currentPackage: new Model(),
        DiagramsPopoverModel: new Model(),
        RelatedDataPopoverModel: new Model(),
        ReportsPopoverModel: new Model(),
        SignalTracerModel: new Model(),
        "models/selectedSystem": new Model(),
        "views/navigationPanelView": new View(),
        "PopoverItem": new Collection(),
        TranslationUtils: {
            translateHTMLContent: function (text)
            {
                expect(JSON.stringify(text)).toBeTruthy();
            }
        },
        "ListGroupView": new View()
    };
    context = createContext(stubs);

    function setViews()
    {
        faceview.set('faceviews',
                [{mainText: "top", id: "top", path: "path1"}, {mainText: "front", id: "front", path: "path2"}]);
    }

    function createFaceViewsData()
    {
        var faceviews = new Collection();
        var faceview1 = new Model();
        var faceview2 = new Model();
        faceview1.set("mainText", "fv1");
        faceview1.set("multiple-faceview-support", true);
        faceview1.set("id", "fv1");

        faceview2.set("mainText", "fv1");
        faceview2.set("id", "fv1");
        faceview2.set("multiple-faceview-support", true);

        faceviews.add(faceview1);
        faceviews.add(faceview2);
        setViews();
    }

    context(["views/contentpanel/faceViewPanel", "models/selectedSystem"],
            function (faceviewpanel, selectedSystem)
            {
                "use strict";
                var faceviewButtonHTML = '<div class="component-button face-view-button auto-faceview-data-button" style="float: left;"></div>';
                var p_sectionName;
                var p_sectionData;
                var p_popoverFilterModel;
                var p_config;
                var m_x;
                var m_y;
                var m_enableFilter;
                var m_popoverFilterModel;
                var origFilter;
                describe("faceViewContentPanelTest", function ()
                {
                    function loadTemplate(template)
                    {
                        var htmlText;
                        $('body').append('<div id="faceview"></div>');
                        $.ajax(template, {async: false}).done(function (html)
                        {
                            htmlText = html;
                            expect(html.indexOf("face-view-button")).toBeGreaterThan(-1);
                        }).fail(function ()
                        {
                            expect(false).toBeTruthy();
                        });
                        return htmlText;
                    }

                    function mockMethods(faceviewpanel2)
                    {
                        faceviewpanel2 = faceviewpanel2 || faceviewpanel;
                        faceviewpanel2.container = '#faceview';
                        faceviewpanel2.getAMDLoader = function ()
                        {
                            return function (deps, callback)
                            {
                                expect(deps[0]).toBe('text!../faceviewSymbolTable.html?packageId=12da');
                                callback(testCavityTable);
                            };
                        };

                        faceviewpanel2.getTitle = function (symbolName)
                        {
                            return symbolName;
                        };
                        faceviewpanel2.enableSystemBtns = function ()
                        {
                            systemButtonsEnabled = true;
                        };
                        faceviewpanel2.compileHTMLTemplate = function ()
                        {
                            return {
                                template: function (templateName, data)
                                {
                                    expect(templateName).toBe();
                                    expect(JSON.stringify(data)).toBe('{"cavityTableHTML":"<div>CavityTable goes here</div>>","path":"faceviewSymbolPath.svg?packageId=12da","symbolExists":"faceviewSymbolPath.svg"}');
                                    return testCavityTable;
                                }
                            };
                        };

                        faceviewpanel2.renderToolbar = function ()
                        {
                            return $(loadTemplate("/base/s/templates/cp/toolbar/systemToolBar.html"));
                        };
                        faceviewpanel2.enableViewButton = function ()
                        {
                            viewBtnEnabled = true;
                        };
                        faceviewpanel2.getPopOverObject = function ()
                        {
                            return {
                                addSection: function (sectionName, sectionData, popoverFilterModel, config)
                                {
                                    p_sectionName = sectionName;
                                    p_sectionData = sectionData;
                                    p_popoverFilterModel = popoverFilterModel;
                                    p_config = config;
                                },
                                showPopover: function (title, x, y, enableFilter, popoverFilterModel)
                                {
                                    m_x = x;
                                    m_y = y;
                                    m_enableFilter = enableFilter;
                                    m_popoverFilterModel = popoverFilterModel;
                                }
                            }
                        };
                    }

                    beforeEach(function ()
                    {
                        viewBtnEnabled = false;

                        systemButtonsEnabled = false;
                        origFilter = mentor.publisher.filter;
                        mentor.publisher.filter = {};
                        faceview.set("symbol", "faceviewSymbol");
                        faceview.set("path", "faceviewSymbolPath.svg");
                        faceview.set("cavityTable", "faceviewSymbolTable.html");
                        faceview.set("symbol", "faceviewSymbol");
                        faceview.set("mainText", "connector-part-number");
                        faceview.set("multiple-faceview-support", true);
                        mockMethods();
                    });

                    it("should be able to load faceViewPanel module", function ()
                    {
                        expect(faceviewpanel).toBeDefined();
                    });

                    it("should be able to render face view symbol and table", function ()
                    {
                        /*faceview.set('faceviews', ['fv1', 'fv2']);*/
                        var isWaiting = true;
                        setViews();
                        selectedSystem.set("faceview", faceview);

                        runs(function() {
                            faceviewpanel.render();
                            setTimeout(function() {
                                isWaiting = false;
                            }, 100);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            expect(systemButtonsEnabled).toBeTruthy();
                            expect($('#faceview').html().indexOf("CavityTable goes here")).toBeGreaterThan(-1);
                        });
                    });

                    it("system toolbar should have view button", function ()
                    {
                        loadTemplate("/base/s/templates/cp/toolbar/systemToolBar.html");
                        loadTemplate("/base/s/templates/popup/systemToolBar.html");
                    });

                    it("should show 'view' button for connectors with multiple symbols", function ()
                    {
                        setViews();
                        selectedSystem.set("faceview", faceview);
                        faceviewpanel.render();
                        expect(viewBtnEnabled).toBeTruthy();
                    });

                    it("should not show 'view' button for connectors with one symbol", function ()
                    {
                        faceview.set('faceviews', ['fv1']);
                        selectedSystem.set("faceview", faceview);
                        faceviewpanel.render();
                        expect(viewBtnEnabled).toBeFalsy();
                        setViews();
                    });

                    it("should allow navigation to different symbols", function ()
                    {
                        context(["views/contentpanel/faceViewPanel"], function (fvPanel)
                        {
                            mockMethods(fvPanel);
                            var faceviewSelected = new Model();
                            faceviewSelected.set("id", "top");
                            createFaceViewsData();
                            selectedSystem.set("faceview", faceview);
                            var symbolChanged, faceviewRerendered;
                            fvPanel.changeSymbol = function (faceviewsymbol)
                            {
                                expect(faceviewsymbol.id).toBe("top");
                                faceviewRerendered = true;
                            };

                            fvPanel.showSymbolByView({}, faceviewSelected);
                            expect(faceviewRerendered).toBeTruthy();
                        }, function(err){
                            expect(err).toBeUndefined();
                        });

                    });

                    it("should should show popover when view button is clicked", function ()
                    {
                        var isWaiting = true;
                        createFaceViewsData();
                        selectedSystem.set("faceview", faceview);

                        runs(function() {
                            faceviewpanel.showFaceViews({
                                pageX: 200,
                                pageY: 200
                            });
                            setTimeout(function() {
                                isWaiting = false;
                            }, 100);
                        });

                        waitsFor(function() {
                            return !isWaiting;
                        }, 2000);

                        runs(function() {
                            expect(p_sectionName).toBe('FaceViewButtonTitle');
                            expect(JSON.stringify(p_sectionData)).toBe(
                                    '[{"mainText":"top","id":"top","path":"path1"},{"mainText":"front","id":"front","path":"path2"}]');
                            expect(JSON.stringify(p_config)).toBe('{"expand":true,"showPopoutBtn":true,"async":true}');

                            expect(m_x).toBe(200);
                            expect(m_y).toBe(200);
                        });
                    });

                    it("should should be able to open face view in pop out from view popover", function ()
                    {
                        var faceviewSelected = new Model();
                        faceviewSelected.set("id", "top");
                        createFaceViewsData();
                        selectedSystem.set("faceview", faceview);
                        faceview.set("objectId", "connectorId");
                        var popoutCalled;
                        faceviewpanel.popout = function (data)
                        {
                            popoutCalled = true;
                            expect(data.get("id")).toBe('top');
                            expect(data.get("objectId")).toBe('connectorId');
                        };

                        faceviewpanel.onPopout({}, faceviewSelected);
                        expect(popoutCalled).toBeTruthy();

                    });

                    afterEach(function ()
                    {
                        $("#faceview").remove();
                        mentor.publisher.filter = origFilter;
                    });

                });

            }, function (err)
            {
                describe("faceviewpanelTestFailed", function ()
                {
                    it("should load the test and dependencies", function ()
                    {
                        expect(err).toBeUndefined();
                    });
                });
            });
})();
