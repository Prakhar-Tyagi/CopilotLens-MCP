/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, $*/
define(["PopoverItemView", "PrintOptionsCollection"],
        function (PopoverItemView, printOptions)
        {
            "use strict";
            var PackagesPopoverItem = PopoverItemView.extend({
                shouldShowNoOfPagesDropDown: function (containerId)
                {
                    var selectedViews, noOFViewsOpen, openViews = mentor.publisher.contentArea.getAllOpenContentDetails()
                    selectedViews = this.getSelectedViews(containerId);
                    noOFViewsOpen = this.getNoOfOpenViews();
                    var isIEAndNumberOFPageIsOne = Utils.is_msie() && mentor.publisher.config &&
                            mentor.publisher.config['print-method'] === 'html' && openViews && noOFViewsOpen === 1;
                    if (isIEAndNumberOFPageIsOne) {
                        var isSingleSVG = openViews[selectedViews[0]].type ===
                                mentor.publisher.contentType.SYSTEM_SVG;
                        if (!isSingleSVG) {
                            isSingleSVG =
                                    ((openViews[selectedViews[0]].get && openViews[selectedViews[0]].get("path") &&
                                    openViews[selectedViews[0]].get("path").indexOf(".svg") > 0) ||
                                    (openViews[selectedViews[0]].path &&
                                    openViews[selectedViews[0]].path.indexOf(".svg") > 0));
                        }
                    }
                    return isSingleSVG;
                },

                getContainerIdToPrint: function ()
                {
                    return this.getData() && this.getData().length > 0 &&
                            $(this.getData().at(0).get("container")).attr('id');
                },
                getData: function ()
                {
                    if (printOptions && printOptions.length > 0) {
                        this.containerToPrint = $(printOptions.at(0).get("container")).attr('id');
                    }

                    if (this.shouldShowNoOfPagesDropDown(this.containerToPrint)) {
                        printOptions.remove(printOptions.at(1));
                    }
                    return printOptions;
                },
                getTitle: function ()
                {
                    return "";
                },
                getClassName: function ()
                {
                    if (this.shouldShowNoOfPagesDropDown(this.getContainerIdToPrint())) {
                        return "printWithNoOfPagesDropDown"
                    }
                    return "printOptions";
                },
                isExpanded: function ()
                {
                    return true;
                },
                events: {
                    "click .printOptions>.listItem": "performPrintAction",
                    "click .printWithNoOfPagesDropDown>.listItem": "performPrintAction"
                },
                shouldShowPopup: function ()
                {
                    return false;
                },

                closePrintPopover: function ()
                {
                    mentor.publisher.eventDispatcher.dispatchEvent(mentor.publisher.events.CLOSE_POPOVER, {});
                },

                getNoOfOpenViews: function ()
                {
                    var openViews = mentor.publisher.contentArea.getAllOpenContentDetails(), k, noOfViews = 0;
                    if (openViews) {
                        for (k in openViews) {
                            if (openViews.hasOwnProperty(k)) {
                                noOfViews++;
                            }
                        }
                    }
                    return noOfViews;
                },

                getSelectedViews: function (containerId)
                {
                    var /*openViews = mentor.publisher.contentArea.getAllOpenContentDetails(), k,*/ selectedViews = [];
                    /*   if (openViews) {
                     for (k in openViews) {
                     if (openViews.hasOwnProperty(k)) {
                     selectedViews.push(k);
                     break;
                     }
                     }
                     }*/

                    selectedViews.push($("#" + containerId + " .panel_content").attr('id'));
                    return selectedViews;
                },

                printFirstContent: function (noOfPages, containerId)
                {
                    mentor.publisher.printer.initiatePrinting(this.getSelectedViews(containerId), noOfPages);
                },

                showPrintSelectionPopover: function (event)
                {
                    mentor.publisher.printer.printSelectionClickHandler(event);
                    event.stopPropagation();
                },

                getDataId: function (event)
                {
                    return $(event.currentTarget).attr('data-id');
                },

                performPrintAction: function (event)
                {
                    var cid = this.getDataId(event), content, noOfPages = $("#noOfPagesToPrint").val();
                    content = printOptions.get(cid);
                    if (content) {
                        this.closePrintPopover();
                        if (content.get('id') === mentor.publisher.constants.print) {
                            this.printFirstContent(noOfPages, $(content.get('container')).attr('id'));
                        }
                        else {
                            this.showPrintSelectionPopover(event);
                        }
                    }
                    event.stopPropagation();
                }
            });
            return new PackagesPopoverItem();
        });
