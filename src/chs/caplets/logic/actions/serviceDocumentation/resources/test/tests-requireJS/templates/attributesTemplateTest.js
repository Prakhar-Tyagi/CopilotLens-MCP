/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("attributesTemplateTest", function ()
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
        var goldenExpectedCompiledTemplate = '<div class="listPanel classNameAttr auto-list">	' +
                '<div class="titlebar auto-list-header">		<span class="headingCount">			' +
                '<span class="headingCountNumber auto-item-count-label">2</span>		' +
                '</span>		<span class="headerText auto-title-label">attributePanel</span>	</div>		' +
                '<div class="listItem auto-item"  >		<span class="mainText">			' +
                '<span class="attributeName auto-name-label">attr1</span>			' +
                '<span class="attributeValue auto-value-label"></span>		</span>	</div>		' +
                '<div class="listItem auto-item"  >		<span class="mainText">			' +
                '<span class="attributeName auto-name-label">attr1</span>			' +
                '<span class="attributeValue auto-value-label"></span>		</span>	</div>	</div>';

        $.ajax("/base/s/templates/p/attributesTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({name: "attr1", subText: "val1"});
                    var item2 = new Model();

                    item2.set({name: "attr1", subText: "val1"});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        className: "classNameAttr",
                        items: items,
                        title: "attributePanel",
                        expand: true
                    });
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/data-id="[a-z][0-9]+"/g, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

