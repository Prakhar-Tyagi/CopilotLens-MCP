/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global mentor, $, LoadMask, setTimeout, define, require, getWindowObj*/
/**
 * mentor.publisher.contentArea keeps information of opened files in Viewer.
 *
 * @type {*}
 */
mentor.publisher.contentArea = (function (p)
{
    "use strict";
    var resolveConfigurationFilterMode,
            activeSystemId,
            activeSystemDiagram,
            contents = {},
            divIdToContentTypeMap = {},
            panelClosed,
            isContentAlreadyOpen,
            getContentIdentifier,
            contentPanelMap = {},
            replaceExistingPanelWithNew,
            activeContent,
            onActiveContentClose,
            selectedSystem,
            mainWindowLayoutManager,
            initialTitle;

    /**
     * This method removes a opened  'container' from the
     * stored containers.
     * @param container container to be removed
     */
    panelClosed = function (container)
    {
        var newContents = {}, k;
        if (contents) {
            for (k in contents) {
                if (contents.hasOwnProperty(k) && k !== container) {
                    newContents[k] = contents[k];
                }
            }
        }
        contents = newContents;
    };

    require(["currentPackage"], function (selectedPackage) {
        if (selectedPackage) {
            selectedPackage.on("change:id", function () {
                initialTitle = undefined;
            });
        }
    });

    onActiveContentClose = function ()
    {
        var selectedPanelInNavigation = activeContent || require("models/selectedSystem");
        selectedPanelInNavigation && selectedPanelInNavigation.trigger("change:clearNavigationPanelSelection");
    };

    replaceExistingPanelWithNew = function (containerId, newPanel)
    {
        var panel = contentPanelMap[containerId];
        if (panel && panel.close && panel.cid !== newPanel.cid) {
            onActiveContentClose();
            panel.close(true);
        }

        if (panel && panel.cid === newPanel.cid) {
            newPanel.$el.html("");
        }
        contentPanelMap[containerId] = newPanel;
    };

    function saveContentBySplitPanel(panelId, content)
    {
        contents[panelId] = content;
    }

    function translateTitle(title)
    {
        if (window.Utils && window.Utils.translate) {
            return Utils.translate(title);
        }
        return title;
    }

    function changeWindowTitle()
    {

        if (!initialTitle) {
            initialTitle = $(document).attr("title");
        }
        var currTitle = initialTitle;
        ["splitter1", "splitter2", "splitter3"].map(function (value) {
            return contents[value] || {};
        }).map(function (content) {
            return content.title || (content.get && content.get("title"));
        }).filter(function (title) {
            return title && currTitle.indexOf(title) < 0;
        }).forEach(function (title) {
            currTitle += " | " + translateTitle(title);
        });
        $(document).attr("title", currTitle);
    }

    function getPanelId(containerId)
    {
        var panelId, path, currPath, parentEle = $("#" + containerId).parent();
        panelId = $(parentEle).hasClass("contentArea") ? $(parentEle).attr('id') : $(parentEle).parent().attr('id');
        return panelId;
    }

    /**
     * This method checks if provided 'content' is already opened.
     *
     * the check is done based on any of the parameter such as path, diagramId
     * or its display text (mainText + subText ).
     * one of the property is taken from the order given above.
     * if path is present then path is compared otherwise diagramId or diaplyText
     *
     *
     * @param containerId container/div id where 'content' is going to be opened
     * @param content content to be open e.g a system's diagram, a report, a file etc.
     * @return {Boolean} Returns true if same content is already opened in given containerId
     */
    isContentAlreadyOpen = function (containerId, content)
    {
        var panelId = getPanelId(containerId);
        //saveContentBySplitPanel(panelId, content);
        changeWindowTitle();
        return mentor.publisher.detailLayoutManager.isPanelOpen(panelId);

    };

    return {
        setSelectedSystem: function (p_selected)
        {
            selectedSystem = p_selected;
        },

        setMainWindowLayoutManager: function (p_layoutManager)
        {
            mainWindowLayoutManager = p_layoutManager;
        },

        getMainWindowLayoutManager: function ()
        {
            return mainWindowLayoutManager || getWindowObj().mentor.publisher.detailLayoutManager;
        },

        getSelectedSystem: function ()
        {
            return selectedSystem || getWindowObj().mentor.publisher.selectedSystem;
        },
        layoutContentPanel: function (content, isSameContentOpen)
        {
            var isSplitPanelAlreadyOpen, type = content.type ||
                    content.get("type"), container, systemId = content.systemId;
            isSameContentOpen = isSameContentOpen || false;

            container =
                    p.detailLayoutManager.layout(type, systemId);

            var panelId = getPanelId(container);
            saveContentBySplitPanel(panelId, content);
            if (isSameContentOpen) {
                isSplitPanelAlreadyOpen = true;
            }
            else {
                isSplitPanelAlreadyOpen = isContentAlreadyOpen(container, content);
            }

            //if same content type is open no need to relayout content panel and toolBar can be shown as it is
            if (!isSameContentOpen) {
                //make visible
                $("#" + container).show();
                //if some other content was open in the current split panel then do not relayout content panel
                if (!isSplitPanelAlreadyOpen) {
                    mentor.publisher.detailLayoutManager.relayout(type);
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.RESIZE_SVG, {});
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.REPOSITION_SVG_SLIDER, {});
                }
                else {
                    //here since content was already open we just need to adjust/resize the panel
                    mentor.publisher.detailLayoutManager.resizePanel(
                            mentor.publisher.detailLayoutManager.getPanelId(type));
                }

                // for new content enable buttons
                mentor.publisher.detailLayoutManager.enableMaximizeAndCloseBtns(isSplitPanelAlreadyOpen);
                p.detailLayoutManager.refreshContentToolbars();

            }
            else {
                //here since content was already open we just need to adjust/resize the panel
                mentor.publisher.detailLayoutManager.resizePanel(mentor.publisher.detailLayoutManager.getPanelId(type));
            }
            if (content.type !== mentor.publisher.contentType.JT_3D) {
                LoadMask.removeLoadMask();
            }
            else {
                LoadMask.addLoadMask(container);
            }
            return container;

        },

        closeExistingPanel: function (content, contentPanelObj)
        {
            var type = content.type || content.get("type"), container, systemId = content.systemId;
            container =
                    p.detailLayoutManager.getPanelId(type);
            LoadMask.addLoadMask(container);
            replaceExistingPanelWithNew(container, contentPanelObj);
        },
        reset: function ()
        {
            contents = {};
        },

        clearContent: function (panels)
        {
            var index, length;
            panels = panels || [];
            length = panels.length;
            for (index = 0; index < length; index = index + 1) {
                panelClosed(panels[index]);
            }
        },
        getAllOpenContentDetails: function ()
        {
            var openpanels = p.detailLayoutManager.getOpenPanels(), k = 0, container, updatedContents = {};
            for (k = 0; k < openpanels.length; k = k + 1) {
                container = p.detailLayoutManager.getContainerForSplitter(openpanels[k]);
                updatedContents[container] = contents[openpanels[k]];
            }
            return updatedContents;
        },

        getNoOfOpenPanels: function ()
        {
            return mentor.publisher.detailLayoutManager.getNoOfOpenPanels();
        },

        getContentTypeOpenInSplitter: function (splitterId)
        {
            var contentDetail = contents[splitterId];
            if (contentDetail) {
                return contentDetail.type || (contentDetail.get && contentDetail.get("type"));
            }
            else {
                return p.detailLayoutManager.getContentTypeBySplitterId(splitterId);
            }
        },
        setActiveContent: function (p_activeContent)
        {
            activeContent = p_activeContent;
        },
        closeAllPanels: function ()
        {
            this.getMainWindowLayoutManager().reset();
        },
        notifyOfSystemChange: function ()
        {
            if (this.getSelectedSystem() && this.getSelectedSystem().trigger) {
                this.getSelectedSystem().trigger("change:systemId");
            }

        }, closeAllSplitPanelsIfNewSystemIsOpened: function (content)
        {
            var currentSystemId = this.getSelectedSystem().get("systemId");
            if (content.systemId &&
                    currentSystemId &&
                    content.systemId !== currentSystemId &&
                    (getWindowObj().mentor.publisher.detailLayoutManager.isContentActive(
                            p.contentType.SYSTEM_REPORT))) {
                this.notifyOfSystemChange();
                getWindowObj().mentor.publisher.detailLayoutManager.resetContentPanel();
                this.closeAllPanels();
            }
        }
    };

}(mentor.publisher));
