/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("basicListItemTemplateTest", function ()
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
        var goldenExpectedCompiledTemplate = '<div class="listPanel testClassName auto-list">		' +
                '<div class="titlebar auto-list-header">		' +
                '<span class="headingCount">			' +
                '<span class="headingCountNumber auto-item-count-label">2</span>		</span>		' +
                '<span class="headerText auto-title-label">basicTemplate</span>	</div>			' +
                '<div class="listItem  auto-item"  >		' +
                '<span class="mainText auto-title-label">item1</span>				' +
                '<span class="popUp auto-pop-out-button"></span>				' +
                '<span class="subText auto-subtitle-label" style="white-space: pre">subTextItem1</span>	</div>		' +
                '<div class="listItem  auto-item"  >		' +
                '<span class="mainText auto-title-label">item2</span>				' +
                '<span class="popUp auto-pop-out-button"></span>				' +
                '<span class="subText auto-subtitle-label" style="white-space: pre">subTextItem2</span>	</div>	</div>';
        $.ajax("/base/s/templates/p/basicListItemTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({mainText: "item1", subText: "subTextItem1"});

                    var item2 = new Model();
                    item2.set({mainText: "item2", subText: "subTextItem2"});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        items: items,
                        title: "basicTemplate",
                        expand: true,
                        className: "testClassName",
                        showPopup: true,
                        showTitle: true
                    });
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "").replace(/data-id="[a-z][0-9]+"/g, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

