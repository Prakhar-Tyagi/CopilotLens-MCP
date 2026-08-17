/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("informationTemplateTest", function ()
{
    it("Should be able to render template correctly", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<div class="listPanel information auto-list"> ' +
                '<div class="titlebar auto-list-header"> ' +
                '<span class="headingCount" style="display: block;"> ' +
                '<span class="headingCountNumber auto-item-count-label">2</span></span> ' +
                '<span class="headerText auto-title-label">informations</span></div> ' +
                '<div class="listItem auto-item" data-id="item1" > ' +
                '<span class="mainText auto-title-label">item1</span> ' +
                '<span class="collapseAll auto-collapse-list" title="clicktocollapse">[-]</span>' +
                '<span class="popUp"></span> <br></div> <div class="listItem auto-item" data-id="item2" > ' +
                '<span class="mainText auto-title-label">item2</span> ' +
                '<span class="collapseAll auto-collapse-list" title="clicktocollapse">[-]</span>' +
                '<span class="popUp"></span> <br></div></div>';
        $.ajax("/base/s/templates/informationTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({mainText: "item1"});

                    var item2 = new Model();
                    item2.set({mainText: "item2"});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        items: items,
                        title: "informations",
                        expand: true
                    });
                    compiledTemplate = compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/\s\s+|\t/gm, " ");
                    expect(compiledTemplate).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

