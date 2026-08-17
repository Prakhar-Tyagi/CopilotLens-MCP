/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, window, $, mentor, Msg*/
define(["jquery", "underscore", "PopoverItemView", "CustomDataCollection", "fileDisplayHandler", "currentPackage",
            "models/selectedSystem"],
        function ($, underscore, PopoverItemView, customDataCollection, fileDisplayHandler, currentPackage,
                selectedSystem) {
            "use strict";
            var CustomDataItem = PopoverItemView.extend({
                getData: function () {
                    return customDataCollection;
                },
                getTitle: function () {
                    //todo needs to be changed, will be the name of the generator
                    return "Custom Data";
                },
                getClassName: function () {
                    return "customData";
                },
                events: {
                    "click .customData>.listItem": "popoverItemClicked",
                    "click .customData>.listItem>.popUp": "popOut"
                },

                createCutomReportURL: function (content) {
                    return "popout.html#/report/" +
                            content.mainText + "/" +
                            selectedSystem.get('systemId') + "/" +
                            currentPackage.get("id").replace("\\", "/") + "/" +
                            content.path.replace(/\\/g, "/");
                },

                createCustomFileURL: function (content) {
                    return "popout.html#/customFile/" +
                            encodeURIComponent(content.mainText) + "/" +
                            currentPackage.get("id").replace("\\", "/") + "/" +
                            content.path.replace(/\\/g, "/");
                },

                extractContent: function (content) {
                    if (content.objectReport === "report") {
                        return {
                            path: content.path,
                            systemId: selectedSystem.get('systemId'),
                            reset: false,
                            type: mentor.publisher.contentType.SYSTEM_REPORT,
                            title: content.mainText
                        };
                    }
                    else {
                        return content;
                    }
                },

                createURL: function (content) {
                    if (content.objectReport === 'report') {
                        return this.createCutomReportURL(content);
                    }
                    else {
                        return this.createCustomFileURL(content);
                    }
                },

                getDataId: function (event) {
                    return $(event.currentTarget).parent().attr('list-id');
                },

                popOut: function (event) {
                    var cid = this.getDataId(event), url, dataId = $(event.currentTarget).parent().attr('data-id'),
                            listContent, dataContent, k = 0, content;
                    listContent = customDataCollection.get(cid);
                    if (listContent) {
                        dataContent = customDataCollection.findDataContent(listContent, dataId);
                        var contentToDisplay = this.extractContent(dataContent);
                        url = this.createURL(dataContent);
                        if(contentToDisplay.path.indexOf("\\") > 0){
                            this.openPopout(url);
                        }
                        else if((contentToDisplay.path.indexOf("http:") > 0) || (contentToDisplay.path.indexOf("https:") > 0)
                                && (contentToDisplay.path.indexOf("www.") > 0)){
                            this.openPopout(contentToDisplay.path);
                        }
                        else if (dataContent && dataContent.path.indexOf("./") == 0 || dataContent.systemId === undefined) {
                            this.openPopout(dataContent.path);
                        }
                        else if (url && url.indexOf("www.") < 0) {
                            this.openPopout(url);
                        }
                        else {
                            this.displayContent(contentToDisplay);
                        }
                        event.stopPropagation();
                        return;
                        /* dataContent = listContent.get('listItems');
                        if (dataContent && dataContent.length > 0) {
                            for (k = 0; k < dataContent.length; k = k + 1) {
                                if (dataContent[k].mainText === dataId) {
                                    var contentToDisplay = this.extractContent(dataContent[k]);
                                    url = this.createURL(dataContent[k]);
                                    if(contentToDisplay.path.indexOf("\\") > 0){
                                        this.openPopout(url);
                                    }
                                    else if((contentToDisplay.path.indexOf("http:") > 0) || (contentToDisplay.path.indexOf("https:") > 0)
                                            && (contentToDisplay.path.indexOf("www.") > 0)){
                                        this.openPopout(contentToDisplay.path);
                                    }
                                    else if (dataContent[k] && dataContent[k].path.indexOf("./") == 0 || dataContent[k].systemId === undefined) {
                                        this.openPopout(dataContent[k].path);
                                    }
                                    else if (url && url.indexOf("www.") < 0) {
                                        this.openPopout(url);
                                    }
                                    else {
                                        this.displayContent(contentToDisplay);
                                    }
                                    event.stopPropagation();
                                    return;
                                }
                            }
                        } */
                    }
                },

                popoverItemClicked: function (event) {

                    var cid = $(event.currentTarget).attr('list-id'), dataId = $(event.currentTarget).attr('data-id'),
                            listContent, dataContent, k = 0, content;
                    listContent = customDataCollection.get(cid);

                    dataContent = customDataCollection.findDataContent(listContent, dataId);
                    var contentToDisplay = this.extractContent(dataContent);
                    getWindowObj().require(["internalLinkHandler"], function (internalLinkHandler) {
                        if (internalLinkHandler.isItaRelativeURL(contentToDisplay.path)) {
                            internalLinkHandler.displayHref(contentToDisplay.path);
                        }
                        else if (contentToDisplay.path && contentToDisplay.path.indexOf("/") === 0) {
                            this.openPopout(contentToDisplay.path);
                        }
                        else {
                            this.displayContent(contentToDisplay);
                        }
                    }.bind(this));
                    /* if (listContent) {
                        dataContent = listContent.get('listItems');
                        if (dataContent && dataContent.length > 0) {
                            for (k = 0; k < dataContent.length; k = k + 1) {
                                if (dataContent[k].mainText === dataId) {
                                    var contentToDisplay = this.extractContent(dataContent[k]);
                                    getWindowObj().require(["internalLinkHandler"], function (internalLinkHandler) {
                                        if (internalLinkHandler.isItaRelativeURL(contentToDisplay.path)) {
                                            internalLinkHandler.displayHref(contentToDisplay.path);
                                        }
                                        else if (contentToDisplay.path && contentToDisplay.path.indexOf("/") === 0) {
                                            this.openPopout(contentToDisplay.path);
                                        }
                                        else {
                                            this.displayContent(contentToDisplay);
                                        }
                                    }.bind(this))

                                    break;
                                }
                            }
                        }
                    } */
                },
                getModel: function (models) {
                    return {
                        title: mentor.publisher.languageTranslator.localize(this.getTitle()),
                        showTitle: this.getTitle() !== '',
                        listItems: this.filter(this.getData().models),
                        className: this.getClassName(),
                        showPopup: this.shouldShowPopup(),
                        expand: this.isExpanded()
                    };
                }
            });
            return new CustomDataItem();
        });
