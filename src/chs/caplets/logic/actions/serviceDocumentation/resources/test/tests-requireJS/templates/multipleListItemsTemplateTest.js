/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("multipleListItemsTemplateTest", function ()
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
        var goldenExpectedCompiledTemplate = '<div class="listPanel multipleListItemsTemplate auto-list">	' +
                '<div class="titlebar auto-list-header">		<span class="headingCount">			' +
                '<span class="headingCountNumber auto-item-count-label">2</span>		</span>		' +
                '<span class="headerText auto-title-label">multipleListItemsTemplate</span>	</div>		' +
                '<div class="listItem auto-item" data-id="item1" 	>	' +
                '<span class="mainText auto-title-label">item1</span>		' +
                '<span class="popUp auto-pop-out-button"></span>	</div>	' +
                '<div class="listItem auto-item" data-id="item2" 	>	' +
                '<span class="mainText auto-title-label">item2</span>		<span class="popUp auto-pop-out-button">' +
                '</span>	</div></div>';
        $.ajax("/base/s/templates/p/multipleListItemsTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item2 = {mainText: "item2"};
                    var item1 = {mainText: "item1"};

                    var panelitem = new Model();
                    panelitem.set({
                        listItems: [item1, item2],
                        expand: true,
                        title: "multipleListItemsTemplate"
                    });
                    var compiledTemplate = _.template(html)({
                        listItems: [panelitem],
                        expand: true,
                        className: "multipleListItemsTemplate",
                        showPopup: true
                    });
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/list-id="[a-z][0-9]+"/g, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

