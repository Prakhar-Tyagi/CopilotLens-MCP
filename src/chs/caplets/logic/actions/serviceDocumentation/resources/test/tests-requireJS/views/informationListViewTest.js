/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

/*global require, ListViewTest, expect*/
/*global require, describe, it, expect, beforeEach, afterEach, mentor*/
(function () {
    "use strict";
    var context, spy = mentor.publisher.popoutHandler, stubs, Model = Backbone.Model.extend(), Collection = Backbone.Collection.extend(), fileDisplayHandler = {
        display : function (content) {
            this.content = content;
        },
        addFileHandler : function() {
            console.log('no-op dummy handler');
        }
    }, View = function (collection) {
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
    var informationModel = new Model({
        id : "CustomFIle",
        path : "CustomFIle.html"
    });

    stubs.currentPackage.set({id : "packageId"});
    context = createContext(stubs);
    var preGet;

    context(['views/informations', "collections/informations"],
        function (informationsView, informationCollection) {

            describe("informationsViewTest", function () {
                beforeEach(function () {
                    $('body').append($("<div id='inforList' data-id='CustomFIle'><div data-id='CustomFIle' id='inforSubList'></div></div>"));
                    preGet = informationCollection.get;
                    informationCollection.get = function () {
                        return informationModel;
                    };
                    mentor.publisher.popoutHandler = {
                        openPopout : function (URL) {
                            this.url = URL;
                        }
                    }
                });

                afterEach(function () {
                    $('#inforList').remove();
                    informationCollection.get = preGet;
                    mentor.publisher.popoutHandler = spy;
                    // mentor.publisher.popoutHandler.openPopout.restore();

                });
                it("should be able to load informationsView Module", function () {
                    expect(informationsView).toBeDefined();
                });

                it("should be able to open information in popout when popout btn is clicked", function () {
                    $("#inforSubList").on("click", function (event) {
                        informationsView.popOut(event);
                    });

                    $("#inforSubList").trigger("click");
                    expect(mentor.publisher.popoutHandler.url).toBe('popout.html#/information/CustomFIle/packageId');

                });

                it("should be able to open report  when item  is clicked", function () {

                    $("#inforSubList").on("click", function (event) {
                        informationsView.clicked(event);
                    });
                    $("#inforSubList").trigger("click");
                    expect(JSON.stringify(fileDisplayHandler.content)).toBe(JSON.stringify({"id" : "CustomFIle", "reset" : true, "type" : "customView"}));

                });

            });

        });
})();


