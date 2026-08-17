/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
define(["PopoverItemView", "PrintContentCollection", "SelectedPrintContentModel"],
    function (PopoverItemView, printContentCollection, printContentModel) {
        "use strict";
        var PackagesPopoverItem = PopoverItemView.extend({
            /*var contentArray = [];*/
            getData : function () {
                return printContentCollection;
            },
            getTitle : function () {
                return "";
            },
            getClassName : function () {
                return "printContent";
            },
            events : {
                "click .printContent>.listItem" : "updateContentsToPrint"
            },

            updateContentsToPrint : function (event) {
                var currentTarget$ = $(event.currentTarget);
                if (event.target.className.indexOf('configPanelCheckBox') == -1) {
                    $('.configPanelCheckBox', currentTarget$).prop('checked', !$('.configPanelCheckBox', currentTarget$).prop('checked'));
                }

                var cid = currentTarget$.attr('data-id'), content;
                content = printContentCollection.get(cid);
                if (content) {
                    printContentModel.updateModel(content.get('type'), $('.configPanelCheckBox', currentTarget$).prop('checked'));
                }
                event.stopImmediatePropagation();
            }
        });
        return new PackagesPopoverItem();
    });
