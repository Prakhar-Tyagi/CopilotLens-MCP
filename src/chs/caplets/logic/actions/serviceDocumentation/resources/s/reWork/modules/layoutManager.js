/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, �SISW�), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer�s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, $, _, window, resizeVinFilterBox*/
(function (p)
{
    "use strict";
    p.contentPanel = {
        containerSelector: "#detail",
        horizontalSeparator: "#horizontalResizebar",
        verticalSeparator: "#verticalResizebar",
        topleft: "splitter1",
        topright: "splitter2",
        bottom: "splitter3",

        showVerticalBar: function ()
        {
            $(this.verticalSeparator).attr("style", "");
            $(this.verticalSeparator).removeClass("horizontalResizebar");
            return $(this.verticalSeparator).show();
        },
        getDimensions: function (containerDom)
        {
            containerDom = containerDom || $(this.containerSelector);
            return containerDom[0] && containerDom[0].getBoundingClientRect();
        }

    };

    p.detailLayoutManager = (function (p)
    {
        var currentSystemId = '',
                isSameSystem;
        var splitter = {
            contentPanel: mentor.publisher.contentPanel,
            setContentPanel: function (contentPanel)
            {
                this.contentPanel = contentPanel;
            },
            clearUponNewSelection: true,
            getScreenSectionMap: function ()
            {
                var sectionMap = {};
                sectionMap.systemSVG = "splitter1";
                sectionMap.systemReport = "splitter3";
                sectionMap.customView = "splitter2";
                sectionMap[mentor.publisher.contentType.THREE_D_XML] = "splitter2";
                sectionMap[mentor.publisher.contentType.JT_3D] = "splitter2";
                sectionMap[mentor.publisher.contentType.RA_3D] = "splitter2";
                sectionMap[mentor.publisher.contentType.LOCATION_VIEWS] = "splitter2";
                sectionMap.faultcode = "splitter3";
                sectionMap.diagnostic = "splitter3";
                sectionMap.projectReport = "splitter3";
                sectionMap.connectorFaceView = "splitter3";
                sectionMap.locationviews = "splitter2";
                sectionMap.RENDERED_SVG = "splitter2";
                sectionMap.harnessLayoutDiagram = "splitter2";
                sectionMap[mentor.publisher.contentType.HARNESS_LAYOUT_REPORT] = "splitter3";
                sectionMap[mentor.publisher.contentType.TROUBLESHOOT] = "splitter3";
                return sectionMap;
            },
            openSecondDetailPanel: function (contentType, openContainers)
            {
                var h1, h2, heightRatios;
                var mapOfTypes = this.getScreenSectionMap();

                if (openContainers[mapOfTypes.systemReport]) {
                    $(this.contentPanel.horizontalSeparator).show();
                }
                else {
                    this.contentPanel.showVerticalBar().addClass("horizontalResizebar");
                }
                var totalHeight = $(this.contentPanel.containerSelector).height() - $(this.contentPanel.horizontalSeparator).height();
                var configuredHeightRatio = p.config['splitpane-height-ratio'] || "1:1";
                try {
                    heightRatios = configuredHeightRatio.split(":");
                    h1 = parseInt(heightRatios[0].trim());
                    h2 = parseInt(heightRatios[1].trim());
                } catch (e) {
                    h1 = 1;
                    h2 = 1;
                }

                var t1 = (totalHeight/(h1+h2))*h1;
                var t2 = totalHeight - t1;
                return {
                    "splitter1" : {height: t1, width: "100%"},
                    "splitter2" : {height: t2, width: "100%"},
                    "splitter3" : {height: t2, width: "100%"}
                };
            },
            openThirdDetailPanel: function (panelId, panelSharing)
            {
                var
                        panelToSplit,
                        width,
                        height,
                        firstPanel,
                        topPanelHeight,
                        topPanelWidth;

                panelToSplit = panelSharing[panelId];
                height = $("#" + panelToSplit).height();
                width = $("#" + panelToSplit).width();
                this.contentPanel.showVerticalBar();
                var verticalSepWidth = $(this.contentPanel.verticalSeparator).width();
                if ("splitter3" === panelId) {
                    firstPanel = panelSharing[panelToSplit];
                    topPanelHeight = $("#" + firstPanel).height();
                    topPanelWidth = ($("#" + firstPanel).width() - verticalSepWidth) / 2;

                    $(this.contentPanel.verticalSeparator).height(topPanelHeight);
                    $("#" + panelToSplit).width(Math.floor(topPanelWidth)).height(topPanelHeight);

                    $("#" + firstPanel).width(
                            Math.ceil(topPanelWidth - verticalSepWidth / 2));
                    $("#" + panelId).width(width).height(height);
                }
                else {
                    topPanelHeight = $("#" + panelToSplit).height();
                    topPanelWidth =
                            ($("#" + panelToSplit).width() - verticalSepWidth) / 2;
                    $("#" + panelToSplit).width(
                            Math.floor(topPanelWidth - verticalSepWidth / 2));
                    $("#" + panelId).width(Math.ceil(topPanelWidth)).height(topPanelHeight);
                    $(this.contentPanel.verticalSeparator).height(topPanelHeight);
                }
                return {panelId: panelId, panelToSplit: panelToSplit};
            },
            showSeparatorForTwoPanels: function (panels, verticalBarHeight)
            {
                $(this.contentPanel.horizontalSeparator).show();
            }
        };
        var layoutS = (function ()
        {
            var noOfOpenTypes = 0,
                    openContainers = {},
                    openContentTypes = {},
                    detailPanels = [],
                    maximizedPanel,
                    verticalBarHeight,
                    panelSharing = {},
                    mapOfTypes = {},
                    restorableDimensions = {};

            var layoutS = {
                setSpiltter: function (newSplitter)
                {
                    splitter = newSplitter;
                    mapOfTypes = splitter.getScreenSectionMap();
                },
                update: function (fileType)
                {
                    openContainers[mapOfTypes[fileType]] = true;
                    openContentTypes[fileType] = true;
                    detailPanels.push(mapOfTypes[fileType]);
                    noOfOpenTypes = noOfOpenTypes + 1;
                },
                openFirstDetailPanel: function (fileType)
                {
                    $(splitter.contentPanel.verticalSeparator).hide();
                    $(splitter.contentPanel.horizontalSeparator).hide();
                    this.enableDetailPanels("100%", "100%");
                },


                setSize: function (panelContainer, height, width)
                {
                    $("#" + panelContainer).height(height);
                    $("#" + panelContainer).width(width);
                    this.adjustToolBarHeight(panelContainer);

                },
                adjustToolBarHeight: function (panelContainer)
                {
                    var totalHeight = $("#" + panelContainer).height();
                    var height = (totalHeight - $("#" + panelContainer + ">.toolbar").height()) * 100 / totalHeight;
                    $("#" + panelContainer + ">.detailContent").height(height + "%");
                },
                relayout: function (contentType)
                {
                    if (noOfOpenTypes === 0) {
                        if (contentType) {
                            this.update(contentType);
                        }
                        this.openFirstDetailPanel(contentType);
                    }
                    else if (noOfOpenTypes === 1) {
                        if (contentType) {
                            this.update(contentType);
                        }
                        this.openSecondDetailPanel(contentType);
                    }
                    else if (noOfOpenTypes === 2) {
                        if (contentType) {
                            this.update(contentType);
                        }
                        this.openThirdDetailPanel(contentType);
                    }
                },
                openSecondDetailPanel: function (contentType)
                {
                    var dimensions = splitter.openSecondDetailPanel(contentType, openContainers);
                    this.resizeSections(dimensions);
                },

                resizeSections: function (dimensions)
                {
                    var toBeHidden, that = this;
                    toBeHidden = _.difference(["splitter1", "splitter2", "splitter3"], detailPanels);

                    _.each(detailPanels, function (panel) {
                        $("#" + panel).show();
                        if (dimensions && dimensions[panel]) {
                            that.setSize(panel, dimensions[panel].height, dimensions[panel].width);
                        }
                    });

                    _.each(toBeHidden, function (panel) {
                        $("#" + panel).hide();
                    });
                },

                enableDetailPanels: function (height, width)
                {
                    var dimensions = {};
                    dimensions["splitter1"] = {height: height, width: width};
                    dimensions["splitter2"] = {height: height, width: width};
                    dimensions["splitter3"] = {height: height, width: width};
                    this.resizeSections(dimensions);
                },

                openThirdDetailPanel: function (fileType)
                {
                    var panelId = mapOfTypes[fileType];
                    var modifiedPanels = splitter.openThirdDetailPanel(panelId, panelSharing);
                    this.adjustToolBarHeight(modifiedPanels.panelToSplit);
                    this.adjustToolBarHeight(modifiedPanels.panelId);
                    this.resizeSections();
                },
                reLayoutOnWindowResize: function ()
                {
                    if (!maximizedPanel) {
                        if (noOfOpenTypes === 1) {
                            this.openFirstDetailPanel();
                        }
                        else if (noOfOpenTypes === 2) {
                            this.openSecondDetailPanel();
                        }
                        else if (noOfOpenTypes === 3) {
                            this.enableDetailPanels("50%", "100%");
                            this.openThirdDetailPanel(p.contentType.SYSTEM_REPORT);
                        }
                    }
                    else {
                        this.getRestoreBtn(maximizedPanel).trigger("click");
                    }

                },
                getRestoreBtn: function (divId)
                {
                    return $("#" + divId + " .restoreBtn");
                },
                getScreenSectionMap: function ()
                {
                    return splitter.getScreenSectionMap();
                },
                getSectionPairing: function ()
                {
                    var pairing = {};
                    pairing.splitter1 = "splitter2";
                    pairing.splitter2 = "splitter1";
                    pairing.splitter3 = "splitter2";
                    return pairing;
                },

                isOpen: function (fileType)
                {
                    return openContainers[mapOfTypes[fileType]];
                },

                reset: function (force)
                {
                    if (splitter.clearUponNewSelection || force) {

                        openContainers = {};
                        openContentTypes = {};
                        noOfOpenTypes = 0;
                        detailPanels = [];
                        this.relayout();
                    }
                },
                getCloseBtn: function (divId)
                {
                    return $("#" + divId + " .closeBtn");
                },

                getMaximizeBtn: function (divId)
                {
                    return $("#" + divId + " .maximizeBtn");
                },

                enableMaximizeAndCloseBtns: function ()
                {
                    var i;
                    //show maximise and close button only when there are more that one panel
                    if (noOfOpenTypes > 1) {
                        for (i = 0; i < detailPanels.length; i = i + 1) {
                            if (detailPanels[i]) {
                                this.getCloseBtn(detailPanels[i]).show();
                                this.getMaximizeBtn(detailPanels[i]).show();
                                this.getRestoreBtn(detailPanels[i]).hide();
                            }
                        }

                    }
                },

                getContainer: function (type)
                {
                    var div = mapOfTypes[type];
                    return this.getContainerForSplitter(div);
                },

                getContainerForSplitter: function (div)
                {
                    return $("#" + div + " .panel_content").attr('id');
                },

                closePanel: function (panel)
                {
                    $("#" + panel + " .panel_content>*").remove();
                    if ($("#" + panel + " .toolbar")[0]) {
                        $($("#" + panel + " .toolbar")[0]).children().each(function ()
                        {
                            $(this).hide();
                        });
                    }
                },

                relayoutOnClose: function (panel)
                {
                    $("#" + panel).hide();
                    if (noOfOpenTypes === 2) {
                        $(splitter.contentPanel.verticalSeparator).hide();
                        this.openSecondDetailPanel();
                    }
                    else if (noOfOpenTypes === 1) {
                        $(splitter.contentPanel.horizontalSeparator).hide();
                        $(splitter.contentPanel.verticalSeparator).hide();
                        this.openFirstDetailPanel();
                    }
                },

                closeOtherPanels: function (panelId)
                {
                    var toBeHidden = _.difference(["splitter1", "splitter2", "splitter3"], [panelId]);
                    _.each(toBeHidden, function (panel) {
                        $("#" + panel).hide();
                    });
                    if ($(splitter.contentPanel.horizontalSeparator).is(":visible") == true) {
                        $(splitter.contentPanel.horizontalSeparator).hide();
                    }
                    if ($(splitter.contentPanel.verticalSeparator).is(":visible") == true) {
                        $(splitter.contentPanel.verticalSeparator).hide();
                    }
                    //$(splitter.contentPanel.verticalSeparator).hide();
                },

                showSeparators: function ()
                {
                    var panelContainerId, i, panels = {};
                    panels[splitter.contentPanel.bottom] = false;
                    panels[splitter.contentPanel.topleft] = false;
                    panels[splitter.contentPanel.topright] = false;
                    _.each(detailPanels, function (panel) {
                        var panel$ = $('#' + panel);
                        if (panel$.is(':hidden')) {
                            panel$.show();
                        }
                        panels[panel] = true;
                    });
                    if (noOfOpenTypes == 2) {
                        splitter.showSeparatorForTwoPanels(panels, verticalBarHeight);
                    }

                    if (noOfOpenTypes == 3) {
                        $(splitter.contentPanel.horizontalSeparator).show();
                        splitter.contentPanel.showVerticalBar();
                        $(splitter.contentPanel.verticalSeparator).height(verticalBarHeight);
                    }
                },
                resetContentPanel: function ()
                {
                    if (maximizedPanel) {
                        this.getRestoreBtn(maximizedPanel).trigger("click");
                    }
                },
                isPanelOpen: function (panelId)
                {
                    var index;
                    if (maximizedPanel) {
                        this.resetContentPanel();
                    }
                    for (index in detailPanels) {
                        if (detailPanels.hasOwnProperty(index) && detailPanels[index] === panelId) {
                            return true;
                        }
                    }
                    return false;
                },
                getPanelId: function (contentType)
                {
                    return mapOfTypes[contentType];
                },
                close: function (contentType)
                {
                    var panel = this.getPanelId(contentType), i, remainingPanels = [];
                    this.closePanel(panel);
                    if (noOfOpenTypes > 1) {
                        openContainers[mapOfTypes[contentType]] = false;
                        openContentTypes[contentType] = false;
                        for (i = 0; i < detailPanels.length; i = i + 1) {
                            if (detailPanels[i] !== panel) {
                                remainingPanels.push(detailPanels[i]);
                            }
                        }
                        detailPanels = remainingPanels;
                        noOfOpenTypes = noOfOpenTypes - 1;
                        this.relayoutOnClose(panel);
                    }
                    if (noOfOpenTypes === 1) {
                        $("[id^='splitter'] .closeBtn").each(function ()
                        {
                            $(this).hide();
                        });
                        $(".maximizeBtn").each(function ()
                        {
                            $(this).hide();
                        });
                    }
                },
                showPrintAndLanguageBtn: function ()
                {
                    var isFirst = true;
                    $(p.toolBarElementCSSSelectors.languageBtn).each(function ()
                    {
                        var splitterDiv = $(this).parent().parent();
                        if (isFirst && $(splitterDiv).css("display") !== 'none') {
                            $(this).show();
                            isFirst = false;
                        }
                        else {
                            $(this).hide();
                        }
                    });
                    isFirst = true;
                    $(p.toolBarElementCSSSelectors.expandCollapseNavPanel).each(function ()
                    {
                        var splitterDiv = $(this).parent().parent();
                        if (isFirst && $(splitterDiv).css("display") !== 'none') {
                            $(this).show();
                            isFirst = false;
                        }
                        else {
                            $(this).hide();
                        }
                    });
                },
                maximizePanel: function (panelId)
                {
                    var dimensions = {};
                    _.each(["splitter1", "splitter2", "splitter3"], function (panel) {
                        restorableDimensions[panel] = {height: $('#' + panel).height() +'px', width: $('#' + panel).width() + 'px'};

                        if (panel === panelId) {
                            dimensions[panel] = {height: "100%", width: "100%"};
                        }
                        else {
                            dimensions[panel] = {height: "0", width: "0"};
                        }
                    });
                    this.resizeSections(dimensions);
                    verticalBarHeight = $(splitter.contentPanel.verticalSeparator).height();
                    $(splitter.contentPanel.verticalSeparator).hide();
                    $(splitter.contentPanel.horizontalSeparator).hide();
                    this.adjustToolBarHeight(panelId);
                    this.refreshContentToolbars();
                    maximizedPanel = panelId;
                },
                restorePanel: function (paneld, previousWidth, previousHeight)
                {
                    maximizedPanel = undefined;
                    this.showSeparators();
                    this.resizeSections(restorableDimensions || {});
                    this.refreshContentToolbars();
                },
                isContentActive: function (contentType)
                {
                    return openContentTypes[contentType];
                },
                getNoOfOpenPanels: function ()
                {
                    return noOfOpenTypes;
                },
                getOpenPanels: function ()
                {
                    return detailPanels;
                },
                getTopPanelId: function ()
                {
                    var topPanelId;
                    if (this.getNoOfOpenPanels() === 3) {
                        return mapOfTypes[mentor.publisher.contentType.CUSTOM_VIEW];
                    }
                    else if (this.getNoOfOpenPanels() === 2) {
                        if (this.isContentActive(mentor.publisher.contentType.SYSTEM_SVG)) {
                            return mapOfTypes[mentor.publisher.contentType.SYSTEM_SVG];
                        }
                        else {
                            return mapOfTypes[mentor.publisher.contentType.SYSTEM_SVG];
                        }

                    }
                    else {
                        return this.getOpenPanels()[0];
                    }
                    return topPanelId;
                },
                getContentTypeBySplitterId: function (splitterId)
                {
                    for (var splitter in mapOfTypes) {
                        if (mapOfTypes.hasOwnProperty(splitter) && mapOfTypes[splitter] &&
                                mapOfTypes[splitter] === splitterId) {
                            return splitter;
                        }
                    }
                },
                refreshContentToolbars: function () {
                    this.showPrintAndLanguageBtn();
                    this.showBackgroundColorBtnIfNeeded();
                    this.updateSiemensLogoAndPrivacyVisibility();
                    var contentAreas = $(".contentArea:visible");
                    contentAreas.find(".primary-toolbar-button").addClass("hidden-toolbar-button");
                    contentAreas.first().find(".primary-toolbar-button").removeClass("hidden-toolbar-button");

                },
                updateSiemensLogoAndPrivacyVisibility: function () {
                    var noOfOpenPanels = mentor.publisher.detailLayoutManager.getNoOfOpenPanels();

                    if (noOfOpenPanels === 2) {
                        $('#splitter1 .toolbar .SiemensWhiteLogo, #splitter1 .toolbar .privacyPolicyBtn').css("display", "block");
                        $('#splitter2 .toolbar .SiemensWhiteLogo, #splitter2 .toolbar .privacyPolicyBtn').css("display", "none");
                        $('#splitter3 .toolbar .SiemensWhiteLogo, #splitter3 .toolbar .privacyPolicyBtn').css("display", "none");
                    }
                    else if (noOfOpenPanels === 3) {
                        $('#splitter1 .toolbar .SiemensWhiteLogo, #splitter1 .toolbar .privacyPolicyBtn').css("display", "none");
                        $('#splitter2 .toolbar .SiemensWhiteLogo, #splitter2 .toolbar .privacyPolicyBtn').css("display", "block");
                        $('#splitter3 .toolbar .SiemensWhiteLogo, #splitter3 .toolbar .privacyPolicyBtn').css("display", "none");
                    }
                    else {
                        $('#splitter1 .toolbar .SiemensWhiteLogo, #splitter1 .toolbar .privacyPolicyBtn').css("display", "block");
                        $('#splitter2 .toolbar .SiemensWhiteLogo, #splitter2 .toolbar .privacyPolicyBtn').css("display", "block");
                        $('#splitter3 .toolbar .SiemensWhiteLogo, #splitter3 .toolbar .privacyPolicyBtn').css("display", "block");
                    }
                },

                showBackgroundColorBtnIfNeeded: function () {
                    var contentAreas = $(".contentArea:visible");
                    contentAreas.find(".backgroundColorBtn").hide();

                    var shouldShowButton = _.chain(p.contentArea.getAllOpenContentDetails())
                            .values()
                            .filter(function (value) {
                                        return value && (hasSVGPath(value) || hasSVGType(value));
                                    })
                                    .size()
                                    .value() > 0;
                    if (shouldShowButton) {
                        contentAreas.first().find(".backgroundColorBtn").show();
                    }
                }
            };

            mapOfTypes = layoutS.getScreenSectionMap();
            panelSharing = layoutS.getSectionPairing();
            return layoutS;

        }());

        $(window).on("resize", function ()
        {

            mentor.publisher.detailLayoutManager.reLayoutOnWindowResize();

        });

        isSameSystem = function (systemId)
        {
            if (currentSystemId && systemId && currentSystemId !== systemId &&
                    !p.detailLayoutManager.isContentActive(p.contentType.FAULT_CODE) &&
                    !p.detailLayoutManager.isContentActive(p.contentType.GLOBAL_REPORT)) {
                mentor.publisher.popoutHandler.closePopoutWindows();
                layoutS.reset();
            }
            currentSystemId = systemId;
        };

        return {
            // getContentTypeOpenInSplitter: function (splitterId)
            // {
            //     mentor.publisher.contentArea.getAllOpenContentDetails();
            // },
            resetContentPanel: function ()
            {
                return layoutS.resetContentPanel();
            },
            enableMaximizeAndCloseBtns: function (isPanelAlreadyOpen)
            {
                return layoutS.enableMaximizeAndCloseBtns(isPanelAlreadyOpen);
            },
            isPanelOpen: function (panelId)
            {
                return layoutS.isPanelOpen(panelId);
            },
            layout: function (contentType, systemId)
            {
                return layoutS.getContainer(contentType);
            },
            getPanelId: function (contentType)
            {
                return layoutS.getPanelId(contentType);
            },
            close: function (contentType)
            {
                return layoutS.close(contentType);
            },
            getNoOfOpenPanels: function ()
            {
                return layoutS.getNoOfOpenPanels();
            },
            getOpenPanels: function ()
            {
                return layoutS.getOpenPanels();
            },
            getTopPanelId: function ()
            {
                return layoutS.getTopPanelId();
            },
            refreshContentToolbars: function ()
            {
                return layoutS.refreshContentToolbars();
            },
            showBackgroundColorBtnIfNeeded: function ()
            {
                return layoutS.showBackgroundColorBtnIfNeeded();
            },
            showPrintAndLanguageBtn: function ()
            {
                return layoutS.showPrintAndLanguageBtn();
            },
            maximizePanel: function (panelId)
            {
                return layoutS.maximizePanel(panelId);
            },
            restorePanel: function (paneld, previousWidth, previousHeight)
            {
                return layoutS.restorePanel(paneld, previousWidth, previousHeight);
            },
            isContentActive: function (contentType)
            {
                return layoutS.isContentActive(contentType);
            },
            getContainerForSplitter: function (div)
            {
                return layoutS.getContainerForSplitter(div);
            },
            relayout: function (contentType, systemId)
            {
                this.resetContentPanel();
                isSameSystem(systemId);
                layoutS.relayout(contentType);
            },
            resizePanel: function (panelId)
            {
                return layoutS.adjustToolBarHeight(panelId);
            },
            reset: function (force)
            {
                return layoutS.reset(force);
            },
            reLayoutOnWindowResize: function ()
            {
                return layoutS.reLayoutOnWindowResize();
            },
            getContentTypeBySplitterId: function (splitterId)
            {
                return layoutS.getContentTypeBySplitterId(splitterId);
            },
            setLayoutSplitter: function (newSplitter)
            {
                layoutS.setSpiltter(newSplitter);
            }
        };
    }(mentor.publisher));
}(mentor.publisher));

/**
 * Handling touchevents for resizing the navigation bar, horizontal and vertical resize bars.
 * */
const H_RESIZE_ICON = "#horizontalResizebar > .resize-icon";
const V_RESIZE_ICON = "#verticalResizebar > .resize-icon";
const NAV_RESIZE_ICON = ".iesdResizeBar > .resize-icon";
const TOUCH_DRAG_COLOR = "#D2E6F5";

const TouchUtil = (function() {
    function navResizeOnTouchStart(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        const touch = evt.touches[0];
        setDragPosition(touch.pageX, touch.pageY);
        registerTouchHandlers(NAV_RESIZE_ICON, navResizeOnTouchmove, navResizeOnTouchend, navResizeOnTouchend);
    }

    function navResizeOnTouchmove(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        setDragColor(NAV_RESIZE_ICON, TOUCH_DRAG_COLOR);
        resizeOnNavigationBarMove(evt.touches[0]);
    }

    function navResizeOnTouchend(evt) {
        unregisterTouchHandlers(NAV_RESIZE_ICON, navResizeOnTouchmove, navResizeOnTouchend, navResizeOnTouchend);
        resizeVinFilterBox();
        resizeTextFilterBox();
    }

    function hResizeOnTouchStart(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        const touch = evt.touches[0];
        setDragPosition(touch.pageX, touch.pageY);
        registerTouchHandlers(H_RESIZE_ICON, hResizeOnTouchmove, hResizeOnTouchend, hResizeOnTouchend);
    }

    function hResizeOnTouchmove(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        setDragColor(H_RESIZE_ICON, TOUCH_DRAG_COLOR);
        hResizeBarMouseDrag(evt.touches[0]);
    }

    function hResizeOnTouchend(evt) {
        unregisterTouchHandlers(H_RESIZE_ICON, hResizeOnTouchmove, hResizeOnTouchend, hResizeOnTouchend);
        resizeVinFilterBox();
        resizeTextFilterBox();
    }

    function vResizeOnTouchStart(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        const touch = evt.touches[0];
        setDragPosition(touch.pageX, touch.pageY);
        registerTouchHandlers(V_RESIZE_ICON, vResizeOnTouchmove, vResizeOnTouchend, vResizeOnTouchend);
    }

    function vResizeOnTouchmove(evt) {
        if (!isValidTouchEvent(evt)) {
            return;
        }
        setDragColor(V_RESIZE_ICON, TOUCH_DRAG_COLOR);
        vResizeBarMouseDrag(evt.touches[0]);
    }

    function vResizeOnTouchend(evt) {
        unregisterTouchHandlers(V_RESIZE_ICON, vResizeOnTouchmove, vResizeOnTouchend, vResizeOnTouchend);
        resizeVinFilterBox();
        resizeTextFilterBox();
    }

    function isValidTouchEvent(evt) {
        return evt && evt.touches && evt.touches.length === 1;
    }

    function setDragPosition(startX, startY) {
        dragX = startX;
        dragY = startY;
    }

    function setDragColor(iconSelector, color) {
        const $resizeIcon = $(iconSelector);
        $resizeIcon.css("background-color", color);
        $resizeIcon.parent().css("background-color", color);
    }

    function resetDragColor(iconSelector) {
        const $resizeIcon = $(iconSelector);
        $resizeIcon.css("background-color", "#DEDEDE");
        $resizeIcon.parent().css("background-color", "#A8A8A8");
    }

    function registerTouchHandlers(iconSelector, touchMoveHandler, touchEndHandler, touchCancelHandler) {
        const $resizeIcon = $(iconSelector);
        $resizeIcon.on('touchmove', touchMoveHandler);
        $resizeIcon.one('touchend', touchEndHandler);
        $resizeIcon.one('touchcancel', touchCancelHandler);
    }

    function unregisterTouchHandlers(iconSelector, touchMoveHandler, touchEndHandler, touchCancelHandler) {
        const $resizeIcon = $(iconSelector);
        $resizeIcon.off('touchmove', touchMoveHandler);
        $resizeIcon.off('touchend', touchEndHandler);
        $resizeIcon.off('touchcancel', touchCancelHandler);
        resetDragColor(iconSelector);
        setDragPosition(null, null);
    }

    return {
        navResizeOnTouchStart: navResizeOnTouchStart,
        hResizeOnTouchStart: hResizeOnTouchStart,
        vResizeOnTouchStart: vResizeOnTouchStart
    };
})();

function initResizebars()
{
    "use strict";
    $('#horizontalResizebar').on("mousedown", hResizeBarMouseDown);
    $('#verticalResizebar').on("mousedown", vResizeBarMouseDown);
    $('.iesdResizeBar').on("mousedown", resizeBarMouseDown);
    $(H_RESIZE_ICON).on("touchstart", TouchUtil.hResizeOnTouchStart);
    $(V_RESIZE_ICON).on("touchstart", TouchUtil.vResizeOnTouchStart);
    $(NAV_RESIZE_ICON).on("touchstart", TouchUtil.navResizeOnTouchStart);
}
var dragX, dragY;
function hResizeBarMouseDown(event)
{
    "use strict";
    dragX = event.pageX;
    dragY = event.pageY;
    if (navigator.userAgent.match(/Firefox/i)) {
        $('#detail').on("mousemove", hResizeBarMouseDrag);

    }
    else {
        var divForResize = $('<div id="divForReisze" class="divForResize">');
        $('body').append(divForResize);
        $('#divForReisze').on("mousemove", hResizeBarMouseDrag);
        $('#divForReisze').on("mouseup", hResizeBarMouseUp);
    }
    $('#detail').on("mouseup", hResizeBarMouseUp);

}

function hResizeBarMouseDrag(event)
{
    "use strict";
    var deltaX = event.pageX - dragX;
    var deltaY = event.pageY - dragY;

    dragX = event.pageX;
    dragY = event.pageY;

    if (deltaY > 0) {

        if ($('#splitter3').css('display') !== 'none' && $('#splitter3').height() - deltaY < 70) {
            return;
        }
        else if ($('#splitter2').css('display') !== 'none' && $('#splitter2').height() - deltaY < 70) {
            if ($('#splitter3').css('display') === 'none') {
                return;
            }
        }

        $('#splitter3').height($('#splitter3').height() - deltaY);
        adjustToolBarHeight(["splitter3"]);
        $('#splitter1 .toolbar .SiemensWhiteLogo').css("display", "block");
        $('#splitter2 .toolbar .SiemensWhiteLogo').css("display", "none");
        $('#splitter3 .toolbar .SiemensWhiteLogo').css("display", "none");
        if (mentor.publisher.detailLayoutManager.getNoOfOpenPanels() === 3) {
            $('#verticalResizebar').height($('#verticalResizebar').height() + deltaY);
            $('#splitter2').height($('#splitter2').height() + deltaY);
            $('#splitter1').height($('#splitter1').height() + deltaY);
            adjustToolBarHeight(["splitter2", "splitter1"]);
            $('#splitter1 .toolbar .SiemensWhiteLogo').css("display", "none");
            $('#splitter2 .toolbar .SiemensWhiteLogo').css("display", "block");
        }
        else if ($('#splitter2').css('display') !== 'none') {
            $('#splitter2').height($('#splitter2').height() + deltaY);
            adjustToolBarHeight(["splitter2"]);
        }
        else {
            $('#splitter1').height($('#splitter1').height() + deltaY);
            adjustToolBarHeight(["splitter1"]);
        }
    }
    else {

        if ($('#splitter1').css('display') !== 'none' && $('#splitter1').height() + deltaY < 70) {
            return;
        }
        else if ($('#splitter2').css('display') !== 'none' && $('#splitter2').height() + deltaY < 70) {
            if ($('#splitter1').css('display') === 'none') {
                return;
            }
        }
        $('#splitter1 .toolbar .SiemensWhiteLogo').css("display", "block");
        $('#splitter2 .toolbar .SiemensWhiteLogo').css("display", "none");
        $('#splitter3 .toolbar .SiemensWhiteLogo').css("display", "none");
        if (mentor.publisher.detailLayoutManager.getNoOfOpenPanels() === 3) {
            $('#verticalResizebar').height($('#verticalResizebar').height() + deltaY);
            $('#splitter1').height($('#splitter1').height() + deltaY);
            $('#splitter2').height($('#splitter2').height() + deltaY);
            adjustToolBarHeight(["splitter1", "splitter2"]);
            $('#splitter1 .toolbar .SiemensWhiteLogo').css("display", "none");
            $('#splitter2 .toolbar .SiemensWhiteLogo').css("display", "block");
        }
        else if ($('#splitter2').css('display') === 'none') {
            $('#splitter1').height($('#splitter1').height() + deltaY);
            adjustToolBarHeight(["splitter1"]);
        }
        else {
            $('#splitter2').height($('#splitter2').height() + deltaY);
            adjustToolBarHeight(["splitter2"]);
        }
        $('#splitter3').height($('#splitter3').height() - deltaY);
        adjustToolBarHeight(["splitter3"]);
    }
}

function adjustToolBarHeight(panelsWithToolBar)
{
    "use strict";
    var index, length;
    panelsWithToolBar = panelsWithToolBar || [];
    length = panelsWithToolBar.length;
    for (index = 0; index < length; index = index + 1) {
        mentor.publisher.detailLayoutManager.resizePanel(panelsWithToolBar[index]);
    }
}

function hResizeBarMouseUp(event)
{
    "use strict";
    if (navigator.userAgent.match(/Firefox/i)) {
        $('#detail').off('mousemove', hResizeBarMouseDrag);

    }
    else {
        $('#divForReisze').off('mousemove', hResizeBarMouseDrag);
        $('#divForReisze').off('mouseup', hResizeBarMouseUp);
        $('#divForReisze').remove();
    }
    $('#detail').off('mouseup', hResizeBarMouseUp);

}

function vResizeBarMouseDown(event)
{
    "use strict";
    dragX = event.pageX;
    dragY = event.pageY;
    if (navigator.userAgent.match(/Firefox/i)) {
        $('#detail').on("mousemove", vResizeBarMouseDrag);
        $('#detail').on("mouseup", vResizeBarMouseUp);
    }
    else {
        var divForResize = $('<div id="divForReisze" class="divForResize">');
        $('body').append(divForResize);
        $('#divForReisze').on("mousemove", vResizeBarMouseDrag);
        $('#divForReisze').on("mouseup", vResizeBarMouseUp);

    }

}

function vResizeBarMouseDrag(event)
{
    "use strict";
    var deltaX = event.pageX - dragX;
    var deltaY = event.pageY - dragY;

    dragX = event.pageX;
    dragY = event.pageY;
    if ($('#verticalResizebar').width() < $('#verticalResizebar').height()) {
        if (deltaX > 0) {
            if ($('#splitter2').width() - deltaX < 70) {
                return;
            }
            var availableWidth = $("#detail").width() - $('#verticalResizebar').width() -
                    ($('#splitter2').width() + deltaX);
            $('#splitter2').width($('#splitter2').width() - deltaX);
            $('#splitter1').width(availableWidth);
            adjustToolBarHeight(["splitter2", "splitter1"]);
        }
        else {
            if ($('#splitter1').width() + deltaX < 70) {
                return;
            }
            var w = $("#detail").width() - $('#verticalResizebar').width() -
                    ($('#splitter1').width() - deltaX);
            $('#splitter1').width($('#splitter1').width() + deltaX);
            $('#splitter2').width(w);
            adjustToolBarHeight(["splitter2", "splitter1"]);
        }
    }
    else {
        if (deltaY > 0) {
            if ($('#splitter2').height() - deltaY < 70) {
                return;
            }
            $('#splitter2').height($('#splitter2').height() - deltaY);
            $('#splitter1').height($('#splitter1').height() + deltaY);
            adjustToolBarHeight(["splitter2", "splitter1"]);
        }
        else {
            if ($('#splitter1').height() + deltaY < 70) {
                return;
            }
            $('#splitter1').height($('#splitter1').height() + deltaY);
            $('#splitter2').height($('#splitter2').height() - deltaY);
            adjustToolBarHeight(["splitter2", "splitter1"]);
        }
    }
}

function vResizeBarMouseUp(event)
{
    "use strict";
    if (navigator.userAgent.match(/Firefox/i)) {
        $('#detail').off('mousemove', vResizeBarMouseDrag);
        $('#detail').off('mouseup', vResizeBarMouseUp);
    }
    else {
        $('#divForReisze').off('mousemove', vResizeBarMouseDrag);
        $('#divForReisze').off('mouseup', vResizeBarMouseUp);
        $('#divForReisze').remove();
    }
}

var actionType;

function resizeBarMouseDown(event)
{
    "use strict";
    if (actionType || typeof(actionType) === 'undefined') {
        actionType = true;
    }
    dragX = event.pageX;
    dragY = event.pageY;

    if (navigator.userAgent.match(/Firefox/i)) {
        $('.iesdApplication').on("mousemove", resizeBarMouseDrag);
        $('.iesdApplication').on("mouseup", resizeBarMouseUp);
    }
    else {
        var divForResize = $('<div id="divForReisze" class="divForResize">');
        $('body').append(divForResize);
        $('#divForReisze').on("mousemove", resizeBarMouseDrag);
        $('#divForReisze').on("mouseup", resizeBarMouseUp);
    }
    $('.iesdResizeBar').on("mouseup", resizeBarMouseUp);
    $('.iesdApplication').on("mouseup", resizeBarMouseUp);
}

function resizeBarMouseDrag(event)
{

    resizeOnNavigationBarMove(event);
}

function resizeOnNavigationBarMove(event)
{
    "use strict";
    var deltaX = event.pageX - dragX;
    var deltaY = event.pageY - dragY;

    var detailArea = $('#detail');
    var resizeBar = $('.iesdResizeBar');

    var resizeBarPos = $(resizeBar).position();

    dragX = event.pageX;
    dragY = event.pageY;
    var newPos = resizeBarPos.left + deltaX;

    $('#navigation').width(newPos);
    detailArea.css('left', newPos);
    detailArea.width(detailArea.width() - deltaX);
    $(resizeBar).css('left', newPos);
    relayout();

}

function relayout(event)
{
    "use strict";
    var navWidth = ($('#navigation').css('display') === 'none' ) ? 0 : $('#navigation').width();
    $('#detail').width($(document).width() - navWidth);
    mentor.publisher.detailLayoutManager.reLayoutOnWindowResize();

}

function resizeViewer(event)
{
    "use strict";

    relayout(event);
    resizeVinFilterBox(event);
    resizeTextFilterBox(event);
    resizeNavContentBar();

}

function resizeNavContentBar()
{
    var totalHeight = $("#navigation").height();
    var navFooterHeight = $("#navigationBottomToolbar").height();
    var vinPanelHeight = $("#vinSearchToolbar").height();
    var navHeaderHeight = $("#platformToolbar").height();
    var navContentHeight = totalHeight - navFooterHeight - navHeaderHeight;
    var marginTop = $("#platform-grouped-list").css("margin-top");
    if (vinPanelHeight) {
        $("#platform-grouped-list").height(navContentHeight - vinPanelHeight);
    }
}

function resizeBarMouseUp()
{
    "use strict";
    if (navigator.userAgent.match(/Firefox/i)) {
        $('.iesdApplication').off('mousemove', resizeBarMouseDrag);
        $('.iesdApplication').off('mouseup', resizeBarMouseUp);
    }
    else {
        $('#divForReisze').off('mousemove', resizeBarMouseDrag);
        $('#divForReisze').off('mouseup', resizeBarMouseUp);
        $('#divForReisze').remove();
    }
    resizeVinFilterBox();
    resizeTextFilterBox();
    $('.iesdResizeBar').off('mouseup', resizeBarMouseUp);
    $('.iesdApplication').off('mouseup', resizeBarMouseUp);

}

function resizeTextFilterBox()
{
    "use strict";
    var isConfigEnabled = true, configCss, crossBtnWidth, radiusWidth, containerWidth, textBoxWidth, crossButtonWidth;
    crossBtnWidth = 0;
    radiusWidth = 3;
    if (isConfigEnabled) {
        crossBtnWidth = $("#resetFilter").width();
        radiusWidth = 3;
    }
    containerWidth = $("#filterBox").width();
    textBoxWidth = containerWidth - crossBtnWidth;
    $('#filterTextHolder').width(textBoxWidth - radiusWidth);
    $('#filterText').width(textBoxWidth - radiusWidth);
}

function hasSVGType(value)
{
    'use strict';
    if (!(value.hasOwnProperty('type') || (value.get && value.get('type')))) {
        return false;
    }
    const switchSVGType = function (value) {
        switch (value) {
            case 'systemSVG':
            case 'connectorFaceView':
            case 'locationviews':
            case 'harnessLayoutDiagram':
            case 'RENDERED_SVG':
                return true;
            default:
                return false;
        }
    }
    return switchSVGType(value.type) || (value.get && switchSVGType(value.get('type')));
}

function hasSVGPath(value)
{
    'use strict';
    var path = (value.get && value.get('path')) || value.path;
    if (typeof path === "string" || path instanceof String) {
        return path.toLowerCase().match(/.*\.svg$/);
    }
    return false;
}
