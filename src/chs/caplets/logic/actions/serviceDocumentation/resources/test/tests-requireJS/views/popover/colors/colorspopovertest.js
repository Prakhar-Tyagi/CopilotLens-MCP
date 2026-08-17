/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */

(function ()
{
    'use strict';

    var BasicPopoverView,
        context,
        preferences,
        recentColors,
        stubs;

    BasicPopoverView = Backbone.View.extend({
        getCoordinates: function (x, y) {
            return {
                x: x,
                y: y
            }
        }
    })

    preferences = new Backbone.Model();
    recentColors = [];
    stubs = {
        'jquery': $,
        'underscore': _,
        'backbone': Backbone,
        'preferences': preferences,
        'recentColors': recentColors,
        'BasicPopoverView': BasicPopoverView
    };
    context = createContext(stubs);

    context(
        ['views/p/colors/colorspopover'],
        function (ColorsPopover) {
            var TestableView = ColorsPopover.extend({
                renderHistoryPanel: function () {
                    this.$('.iesdPopup').append('<div id="history-panel"></div>');
                },
                renderPalettePanel: function () {
                    this.$('.iesdPopup').append('<div id="palette-panel"></div>');
                }
            });
            describe('ColorsPopover', function () {
                beforeEach(function () {
                    $('body').html('');
                    preferences.clear();
                    recentColors.length = 0;

                    ColorsPopover.container = 'body';
                    ColorsPopover.templateHTML = '<div class="iesdPopup" style="visibility: hidden;"><div class="closeBtn"></div></div>';
                });

                it("shouldn't render the history panel when recent colors is empty.", function () {
                    var view = new TestableView();
                    view.render();

                    expect($('#history-panel').length).toBe(0);
                });

                it("should render the history panel when recent colors isn't empty.", function () {
                    recentColors.push("#444");

                    var view = new TestableView();
                    view.render();

                    expect($('#history-panel').length).toBe(1);
                });

                it("should reset the background color preference when close button is clicked", function () {
                    preferences.set('background-color', 'red');

                    var view = new TestableView();
                    view.render();

                    preferences.set('background-color', 'white');

                    view.$('.closeBtn').trigger("click");

                    expect(preferences.get('background-color')).toBe('red');
                });

                it('should set the current item container based on background color preference', function () {
                    preferences.set('background-color', 'red');

                    var view = new TestableView();
                    view.renderPalettePanel = function () {
                        this.$('.iesdPopup').append('<div id="palette-panel"><div><div class="cp-color-item" style="background-color: red"></div></div><div><div class="cp-color-item" style="background-color: blue"></div></div></div>');
                    };
                    view.render();

                    var $colorItems = view.$(".cp-color-item");
                    var actual = $colorItems
                        .filter(function () {
                            return this.style.backgroundColor === 'red';
                        })
                        .parent()
                        .hasClass('cp-current-item-container');
                    expect(actual).toBeTruthy();
                });

                it('should update the current item container when the background color preference changes', function () {
                    preferences.set('background-color', 'red');

                    var view = new TestableView();
                    view.renderPalettePanel = function () {
                        this.$('.iesdPopup').append('<div id="palette-panel"><div><div class="cp-color-item" style="background-color: red"></div></div><div><div class="cp-color-item" style="background-color: blue"></div></div></div>');
                    };
                    view.render();

                    preferences.set('background-color', 'blue');

                    var $colorItems = view.$(".cp-color-item");
                    var actual = $colorItems
                        .filter(function () {
                            return this.style.backgroundColor === 'blue';
                        })
                        .parent()
                        .hasClass('cp-current-item-container');
                    expect(actual).toBeTruthy();
                });
            });
        },
        function (err) {
            describe('ColorsPopover', function ()
            {
                it('failed to load', function ()
                {
                    console.log(err.message + "::\n" + err.stack);
                    expect(err).toBeUndefined();
                });
            });
        }
    );
})();