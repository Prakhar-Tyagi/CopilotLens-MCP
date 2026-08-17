/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, renderer*/
define(["backbone", "underscore", "jquery", "currentPackage", "views/appNameAndLogo/appNameAndLogoView"],
        function (Backbone, underscore, $, currentPackage, appNameAndLogoView) {
            "use strict";
            var ToolBarButtons, p = mentor.publisher, layoutManager = p.detailLayoutManager;
            ToolBarButtons = Backbone.View.extend({
                el: "<div></div>",

                events: {
                    "click .languageBtn": "showLanguages",
                    "click .regenerateBtn": "regenerateSignal",
                    "click .printBtn": "showPrint",
                    "click .backgroundColorBtn": "showColors",
                    "click .privacyPolicyBtn":"openPrivacyLink",
                    "mouseover .component-button": "showToolTip",
                    "mousemove .component-button": "showToolTip",
                    "mouseleave .component-button": "removeToolTip"
                },

                showToolTip: function (event)
                {
                    mentor.publisher.toolTip.showToolTipFromEvent(event);
                },

                removeToolTip: function (event)
                {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REMOVE_TOOL_TIP, event);
                },

                showPrint: function (event)
                {
                    mentor.publisher.printer.printButtonClickHandler(event);
                },

                showLanguages: function (event)
                {
                    mentor.publisher.languageTranslator.clickHandler(event);
                },

                openPrivacyLink: function (event) {
                    var privacyPolicyUrl = mentor.publisher.serverConfig['privacy-policy'];
                    if (privacyPolicyUrl) {
                        if (!privacyPolicyUrl.startsWith('http://') && !privacyPolicyUrl.startsWith('https://')) {
                            privacyPolicyUrl = 'https://' + privacyPolicyUrl;
                        }
                        window.open(privacyPolicyUrl, '_blank');
                    }
                },

                showColors: function (event) {
                    var currentTarget = event.currentTarget;
                    var clientY = currentTarget ? (currentTarget.offsetTop + currentTarget.offsetHeight) : event.clientY;
                    var options = {
                        preferredX: event.clientX,
                        preferredY: clientY
                    };

                    var Popover = require("views/p/colors/colorspopover");
                    var popover = new Popover();
                    popover.render(options);

                    p.stopEventFlow(event);
                },

                regenerateSignal: function (event) {
                    renderer.regenerateSVG();
                    p.stopEventFlow(event);
                },

                render: function (options) {
                    var template,
                            that = this, allowPrinting, showPrivacyBtn;
                    options = options || {title: "No Title to display"};
                    options.language = Utils.readCookie("language") || "EN";
                    allowPrinting = options.allowsPrinting;
                    options.allowsPrinting = mentor.publisher.features.allowsPrinting;
                    if (typeof allowPrinting !== 'undefined') {
                        options.allowsPrinting = allowPrinting && mentor.publisher.features.allowsPrinting;
                    }
                    var privacyPolicyUrl = mentor.publisher.serverConfig['privacy-policy'];
                    showPrivacyBtn = privacyPolicyUrl && privacyPolicyUrl.trim().length > 0;
                    options.showPrivacyBtn = showPrivacyBtn;
                    template = underscore.template(ToolBarButtons.templateHTML)(options);
                    this.$el.append(template);
                    this.delegateEvents();
                    appNameAndLogoView.updateApplicationNameAndLogo(this);
                    return this;
                }
            });

            return ToolBarButtons;
        });
