/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer*/
define(["backbone", "underscore", "jquery", "views/contentpanel/toolbar/systemToolBar",
    "views/contentpanel/toolbar/generalButtons"],
        function (Backbone, underscore, $, SystemButtons, GeneralButtons)
        {
            "use strict";
            var Toolbar, p = mentor.publisher;
            Toolbar = Backbone.View.extend({
                el: "<div class='toolbar background'></div>",
                LayoutButtons: SystemButtons,

                initialize: function ()
                {
                    var that = this;
                },

                showPrint: function (event)
                {
                    mentor.publisher.printer.printButtonClickHandler(event);
                },

                showLanguages: function (event)
                {
                    mentor.publisher.languageTranslator.clickHandler(event);
                },

                regenerateSignal: function (event)
                {
                    renderer.regenerateSVG();
                    p.stopEventFlow(event);
                },

                isDocumentTypeActive: function ()
                {
                    return this.layoutButtons.isDocumentTypeActive(this.options);
                },

                isReportsBtnActive: function ()
                {
                    return this.layoutButtons.isReportsBtnActive(this.options);
                },

				enableFaceViewsNavigation: function (config)
				{
					if (this.layoutButtons) {
						this.layoutButtons.enableFaceViewsNavigation(config);
					}
				},

                render: function (options)
                {
                    var generalBtns, crossAndMaximizeBtnHandler = mentor.publisher.ToolBar(options.type);

                    options = options || {};
                    options.title = options.title || "No title to display";
                    this.options = options;
                    this.$el.html('');

					this.layoutButtons = new this.LayoutButtons({el: this.el});
					this.layoutButtons.render(options);



                    generalBtns = new GeneralButtons({el: this.el});
                    generalBtns.render(options);
                    setTimeout(function ()
                    {
                        crossAndMaximizeBtnHandler.addEventListernerForCloseAndMaximizeButtons(options.title);
                    }, 10);
                    return this;
                }
            });

            return Toolbar;
        });

mentor.publisher.ToolBar = function (contentType)
{
    "use strict";
    var contentPanelContainer = 'systemSVGLoadArea', onMaximize, systemId, onClose, /*getToolBar,*/ regenerateBtn, hideRegenerateBtn,
            enableReferences, showPrintAndLanguageBtn, addEventListener, enableDiagrams, enableReports,
            setTitle, contentPanelId, p, isItFirstPanel, enableCloseAndMaximizeBtns, panelWidth, panelHeight, onPopoutBtnClicked, data, eventToGenerate, enablePopoutBtn;
    p = p || mentor.publisher;
    contentPanelId = p.detailLayoutManager.getPanelId(contentType);

    addEventListener = function ()
    {
        onClose();
        onMaximize();
    };

    onClose = function ()
    {
        var otherPanel, topPanelId;
        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.closeBtn).off('click');
        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.closeBtn).on("click", function (event)
        {
            p.detailLayoutManager.close(contentType);
            mentor.publisher.contentArea.clearContent([contentPanelId]);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.RESIZE_SVG, {});
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REPOSITION_SVG_SLIDER, {});
        });
    };

    onMaximize = function ()
    {
        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.maximizeBtn).off('click');
        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.maximizeBtn).on("click", function (event)
        {

            panelWidth = $("#" + contentPanelId).width();
            panelHeight = $("#" + contentPanelId).height();

            $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.restoreBtn).show();
            $(this).hide();
            p.detailLayoutManager.maximizePanel(contentPanelId);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.RESIZE_SVG, {});
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REPOSITION_SVG_SLIDER, {});
        });

        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.restoreBtn).off('click');
        $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.restoreBtn).on("click", function (event)
        {
            $("#" + contentPanelId + " " + p.toolBarElementCSSSelectors.maximizeBtn).show();
            $(this).hide();
            p.detailLayoutManager.restorePanel(contentPanelId, panelWidth, panelHeight);
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.RESIZE_SVG, {});
            mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REPOSITION_SVG_SLIDER, {});
        });

    };

    return {
        addEventListernerForCloseAndMaximizeButtons: function (name)
        {
            addEventListener();
            if (window.opener && window.opener.mentor) {
                window.document.title = name;
            }
        }
    };
};
