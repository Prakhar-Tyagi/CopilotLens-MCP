/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("flatListTemplateTest", function ()
{
    beforeEach(function ()
    {
        Utils.translate_old = Utils.translate;
        Utils.translate = function (text)
        {
            return text;
        };
    });
    afterEach(function ()
    {
        Utils.translate = Utils.translate_old;
    });
    it("Should be able to render template correctly", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<div class="titlebar auto-list-header"> ' +
                '<span class="headingCount" style="display: block; "> ' +
                '<span class="headingCountNumber auto-item-count-label">2</span> </span> ' +
                '<span class="headerText auto-title-label">systems</span></div>' +
                '<div class="list-content" ><div class="listItem auto-item" data-id="" base-id="sys1"> ' +
                '<span class="mainText auto-title-label" style="white-space: pre">item1</span> ' +
                '<span class="collapseAll auto-collapse-list-button" title="clicktocollapse">[-]</span>' +
                '<span class="popUp"></span> <br> <img class="auto-thumbnail" src=\'t1\'> ' +
                '<span class="subText auto-subtitle-label">subTextItem1</span> </div>' +
                '<div class="listItem auto-item" data-id="" base-id="sys2"> ' +
                '<span class="mainText auto-title-label" style="white-space: pre">item2</span> ' +
                '<span class="collapseAll auto-collapse-list-button" title="clicktocollapse">[-]</span>' +
                '<span class="popUp"></span> <br> <img class="auto-thumbnail" src=\'t2\'> ' +
                '<span class="subText auto-subtitle-label">subTextItem2</span> </div></div>';

        $.ajax("/base/s/templates/flatListTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({mainText: "item1", subText: "subTextItem1", thumbNailPath: "t1", systemId: "sys1",});

                    var item2 = new Model();
                    item2.set({mainText: "item2", subText: "subTextItem2", thumbNailPath: "t2", systemId: "sys2"});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        items: items,
                        title: "systems",
                        page: 1,
                        expand: true,
                        totalPages: 2
                    });
                    compiledTemplate = compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/\s\s+|\t+/gm, " ");
                    expect(compiledTemplate).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

