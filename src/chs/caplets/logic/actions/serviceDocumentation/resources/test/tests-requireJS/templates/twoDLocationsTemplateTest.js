/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
describe("twoDLocationsTemplateTest", function ()
{
    it("Should be able to render template correctly", function ()
    {
        var templateLoaded = false;
        var goldenExpectedCompiledTemplate = '<div class="listPanel twoD auto-list">	' +
                '<div class="titlebar auto-list-header">		<span class="headingCount">			' +
                '<span class="headingCountNumber auto-item-count-label">2</span>		</span>		' +
                '<span class="headerText auto-title-label">2Dviews</span>	</div>		' +
                '<div class="listItem auto-item" data-id="" >		<span class="mainText auto-title-label">item1</span>' +
                '        <span class="popUp auto-pop-out-button"></span>				' +
                '<img class="auto-thumbnail" src=\'t1\'>			</div>		<div class="listItem auto-item" data-id="" >' +
                '		<span class="mainText auto-title-label">item2</span>        <span class="popUp auto-pop-out-button"></span>' +
                '				<img class="auto-thumbnail" src=\'t2\'>			</div>	</div>';
        $.ajax("/base/s/templates/p/twoDLocationsTemplate.html", {async: false}).done(
                function (html)
                {
                    templateLoaded = true;
                    expect(html).toBeDefined();
                    var Model = Backbone.Model.extend({});
                    var item1 = new Model();
                    item1.set({mainText: "item1", subText: "subTextItem1", thumbNailPath: "t1"});

                    var item2 = new Model();
                    item2.set({mainText: "item2", subText: "subTextItem2", thumbNailPath: "t2"});

                    var items = [item1, item2];
                    var compiledTemplate = _.template(html)({
                        items: items,
                        title: "2Dviews",
                        expand: true,
                        className: "twoD",
                        showPopup: true,
                        showTitle: true
                    });
                    expect(compiledTemplate.trim().replace(/(\r\n|\n|\r)/gm, "")).toBe(goldenExpectedCompiledTemplate);
                }).fail(function ()
        {
            expect(false).toBeTruthy();
        });
        expect(templateLoaded).toBeTruthy();
    });
});

