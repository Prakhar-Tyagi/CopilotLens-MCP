/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("wiresViewsTemplateTest", function ()
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
        var goldenExpectedCompiledTemplate = '<div class="titlebar auto-list-header">\t\t' +
                '<span class="headingCount">\t\t\t' +
                '<span class="headingCountNumber auto-item-count-label">2/2</span></span>    ' +
                '<span class="headerText auto-title-label">wires</span></div>' +
                '<div class="listItem false auto-item"\t data-id="" >' +
                '<span class="mainText auto-title-label">item1</span>' +
                '<span class="collapseAll auto-collapse-list-button"\t  title="clicktocollapse">[-]</span><br>' +
                '<span class="subText auto-subtitle-label">subTextItem1</span></div>' +
                '<div class="listItem false auto-item"\t data-id="" >' +
                '<span class="mainText auto-title-label">item2</span>' +
                '<span class="collapseAll auto-collapse-list-button"\t  title="clicktocollapse">[-]</span><br>' +
                '<span class="subText auto-subtitle-label">subTextItem2</span></div>' +
                '<div class="listItem next_prevous_btn next auto-next-button" >' +
                '<span class="mainText auto-title-label">nextItems</span></div>';
        $.ajax("/base/s/templates/wiresViewsTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({mainText: "item1", subText: "subTextItem1", totalObjects: 2, isActive: false});

                    var item2 = new Model();
                    item2.set({mainText: "item2", subText: "subTextItem2", totalObjects: 2, isActive: false});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        items: items,
                        title: "wires",
                        page: 1,
                        expand: true,
                        totalPages: 2,
                        totalItems: 2
                    });
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

