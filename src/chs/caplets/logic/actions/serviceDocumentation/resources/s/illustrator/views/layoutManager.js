/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, define, $*/
define([], function ()
{
    "use strict";
    var p = mentor.publisher,
            panels = {
                topleft: "splitter1",
                topright: "splitter2",
                bottom: "splitter3"
            };

    var splitter = {
        domquerylib: $,
        contentPanel: mentor.publisher.contentPanel,
        setContentPanel: function (contentPanel)
        {
            this.contentPanel = contentPanel;
        },
        clearUponNewSelection: false,
        openThirdDetailPanel: function (contentType)
        {
            var totalSize = this.getTotalSizeAvailable();
            var verticalSplitterBar = this.contentPanel.showVerticalBar();

            if(totalSize) {
                var splitPaneDimension = this.getSplitPaneDimension(totalSize);
                this.setSizeForTopLeftSection(totalSize, splitPaneDimension.topSectionHeight);
                this.setSizeForTopRightPanel(totalSize, splitPaneDimension.topSectionHeight);
                this.setSizeForBottomSection(totalSize, splitPaneDimension.bottomSectionHeight);
                verticalSplitterBar.height(splitPaneDimension.topSectionHeight);
            }

            this.getHorizontalBar().show();
            return {panelId: panels.topright, panelToSplit: panels.topleft};
        },
        openSecondDetailPanel: function (contentType, openContainers)
        {
            var sizeAfterSplit = {};
            if (this.isHorizontalSplitRequired(contentType, openContainers)) {
                sizeAfterSplit = this.splitHorizontallyIntoTwo();
            }
            else {
                sizeAfterSplit = this.splitVerticallyIntoTwo();
            }
            return sizeAfterSplit;

        },
        isHorizontalSplitRequired: function (contentType, openContainers)
        {
            return openContainers[this.getScreenSectionMap().customView] ||
                    this.getScreenSectionMap()[contentType] === panels.bottom;
        },

        splitHorizontallyIntoTwo: function ()
        {
            var separatorWidth = this.contentPanel.showVerticalBar().width();
            this.getVerticalBar().hide();
            this.getHorizontalBar().show();
            if (this.contentPanel.getDimensions()) {
                var totalheight = this.contentPanel.getDimensions().height - separatorWidth;
                var halfHeight = (totalheight) / 2;
                var rounded = this.getRoundedLength(halfHeight);
                return {"splitter1": {height: rounded, width: "100%"}, "splitter2": {height: rounded, width: "100%"},"splitter3": {height: rounded, width: "100%"}};
            }
        },
        splitVerticallyIntoTwo: function ()
        {
            var separatorWidth = this.contentPanel.showVerticalBar().height("100%").width();
            var totalWidth = this.contentPanel.getDimensions().width - separatorWidth;
            var halfWidth = (totalWidth) / 2;
            var rounded = this.getRoundedLength(halfWidth);
            return {"splitter1": {height: "100%", width: rounded}, "splitter2": {height: "100%", width: rounded}};
        },

        setPanelSizeForLaterUse: function (h, w, panelId) {
            if(panelId && window.heavySVGs && false){
                p.panelSize = p.panelSize || {};
                p.panelSize[panelId] = {height: h, width: w};
            }
        }, setSizeForTopLeftSection: function (totalSize, height)
        {
            var w = (totalSize.width) / 2;
            var roundedHeight = this.getRoundedLength(height);
            var roundedWidth = this.getRoundedLength(w);
            this.setPanelSizeForLaterUse(roundedHeight, roundedWidth, "newDesignRevision");
            this.domquerylib("#" + panels.topleft).height(roundedHeight).width(roundedWidth).show();
        },
        setSizeForTopRightPanel: function (totalSize, height)
        {
            var remainingWidth = totalSize.width - (totalSize.width / 2);
            var roundedHeight = this.getRoundedLength(height);
            var roundedWidth = this.getRoundedLength(remainingWidth);
            this.setPanelSizeForLaterUse(roundedHeight, roundedWidth, "oldDesignRevision");
            this.domquerylib("#" + panels.topright).height(roundedHeight).width(roundedWidth).show();
        },
        getRoundedLength: function (l) {
            // return Math.round((l + Number.EPSILON) * 10) / 10;
            return Math.floor(l);
        },
        getContentPanel: function ()
        {
            return this.domquerylib(this.contentPanel.containerSelector)[0];
        }, getTotalSizeAvailable: function ()
        {
            var separatorWidth = this.contentPanel.showVerticalBar().width();
            if (this.getContentPanel()) {
                var totalHeight = this.getContentPanel().getBoundingClientRect().height;
                var totalWidth = this.getContentPanel().getBoundingClientRect().width;
                totalHeight = totalHeight - separatorWidth;
                totalWidth = totalWidth - separatorWidth;
                return {height: totalHeight, width: totalWidth};
            }

        },
        getBottonPanel: function ()
        {
            return this.domquerylib("#" + panels.bottom);
        }, setSizeForBottomSection: function (totalSize, height)
        {
            var roundedHeight = this.getRoundedLength(height);
            var w = "100%";
            this.getBottonPanel().height(roundedHeight).width(w).show();
        },
        getSplitPaneDimension: function (totalSize) {
            var h1, h2, heightRatios;
            var configuredHeightRatio = p.config['splitpane-height-ratio'] || "1:1";
            try {
                heightRatios = configuredHeightRatio.split(":");
                h1 = parseInt(heightRatios[0].trim());
                h2 = parseInt(heightRatios[1].trim());
            }
            catch (e) {
                h1 = 1;
                h2 = 1;
            }
            var topSectionHeight = (totalSize.height / (h1 + h2)) * h1;
            var bottomSectionHeight = totalSize.height - topSectionHeight;
            return {"topSectionHeight": topSectionHeight, "bottomSectionHeight": bottomSectionHeight};
        },
        getScreenSectionMap: function ()
        {
            var sectionMap = {};
            sectionMap.systemReport = panels.bottom;
            sectionMap.customView = panels.bottom;
            sectionMap.projectReport = panels.bottom;
            sectionMap.connectorFaceView = panels.bottom;
            sectionMap[p.contentType.OLD_DESIGN_REVISION] = panels.topleft;
            sectionMap[p.contentType.NEW_DESIGN_REVISION] = panels.topright;
            sectionMap[p.contentType.HARNESS_LAYOUT_REPORT] = panels.bottom;
            return sectionMap;
        },
        getVerticalBar: function ()
        {
            return this.domquerylib(this.contentPanel.verticalSeparator);
        }, getHorizontalBar: function ()
        {
            return this.domquerylib(this.contentPanel.horizontalSeparator);
        }, showSeparatorForTwoPanels: function (panels, verticalBarHeight)
        {
            if (panels[this.contentPanel.topleft] && panels[this.contentPanel.topright]) {
                this.contentPanel.showVerticalBar();
                this.getVerticalBar().height(verticalBarHeight);
            }
            else {
                this.getHorizontalBar().show();
            }
        }

    };
    return splitter;
});
