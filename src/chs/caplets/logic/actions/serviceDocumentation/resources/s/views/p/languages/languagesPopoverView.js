/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(['jquery', 'underscore', 'backbone', "BasicPopoverView", "LanguagesPopoverModel"],
    function ($, underscore, Backbone, BasicPopoverView, languagesModel) {
        "use strict";
        var LanguagesPopoverView = BasicPopoverView.extend({
            initialize : function () {
                LanguagesPopoverView.__super__.initialize();
                languagesModel.on("change:loadSkeleton", this.render, this);
                //console.log("Initializing view of languages popover");
            },
            render : function () {
                if (!languagesModel.get('popoverModel')) {
                    return;
                }
                $('#detailPopup', this.$el).remove();
                this.setElement(this.container);
                var x = languagesModel.get('x');
                var y = languagesModel.get('y');
                var showFilter = languagesModel.get('showFilter') || false;
                var coordinates = this.getCoordinates(x, y);
                var template = underscore.template(this.templateHTML)({
                    title: mentor.publisher.languageTranslator.localize("ChooseLanguageMenuTitle") || "Choose Language",
                    show: true,
                    x: coordinates.x,
                    y: coordinates.y,
                    height: showFilter ? mentor.publisher.constants.popOverHeightWithFilter :
                            mentor.publisher.constants.popOverHeightWithoutFilter,
                    showFilter: showFilter,
                    showRenderConnectivityBtn: false
                });
                this.$el.append(template);
                return this;
            }
        });
        return new LanguagesPopoverView();
    });
