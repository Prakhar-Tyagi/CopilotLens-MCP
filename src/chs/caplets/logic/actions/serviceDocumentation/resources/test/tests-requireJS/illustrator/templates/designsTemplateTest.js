/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("IllustratorDesignsTemplateTest", function ()
{
    var golden = '';
    beforeEach(function ()
    {
        previousTraslated = Utils.translate;
        Utils.translate = function (value)
        {
            return value + "_traslated";
        }
    });
    afterEach(function ()
    {
        Utils.translate = previousTraslated;
    });
    it("should not render design for empty designs", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/designs.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        items: []

                    });
                    expect(compiledTemplate.trim()).toBeFalsy();
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });

    it("should  render designs for non-empty items", function ()
    {
        var templateLoaded = false;
        $.ajax("/base/s/illustrator/templates/designs.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var compiledTemplate = _.template(html, {
                        expand: true,
                        totalPages: 1,
                        items: [{
                            get: function ()
                            {
                                return "value"
                            },

                            id: "id",
                            getNameWithPartNumberAndRevision: function ()
                            {
                                return "name";
                            },
                            getTooltipByName: function ()
                            {
                                return "tooltip"
                            }
                        }],
                        title: "Designs",
                        page: 1,
                        totalItems: 1,

                    });
                    expect(compiledTemplate.indexOf('<div class="titlebar auto-list-header">') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf(
                                    '<span class="headingCountNumber auto-item-count-label">1/1</span></span>') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf(
                                    '<span class="headerText auto-title-label">Designs_traslated</span></div>') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<div class="listItem auto-item" data-id="id"') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('base-id="value" >') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<span class="mainText auto-title-label">name_traslated</span>') >=
                            0).toBeTruthy();
                    expect(compiledTemplate.indexOf('<span class="popUp"></span>') >= 0).toBeTruthy();
                    expect(compiledTemplate.indexOf(
                                    '<span class="subText auto-title-label">tooltip_traslated</span>') >=
                            0).toBeTruthy();
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();

    });
});