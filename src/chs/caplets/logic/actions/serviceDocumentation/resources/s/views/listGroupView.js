/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, Utils, setInterval, clearInterval*/
define("ListGroupView", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone)
{
    "use strict";
    var ListGroup = Backbone.View.extend({

        toggleNavSection: function (section)
        {
            if ($(".list-content", section).length > 0) {
                $(".list-content", section).each(function () {
                    $(this).toggle();
                });
            }
            else {
                var i, j, objs = $(".listItem", section);
                if (objs.length <= 1000) {
                    $.each(objs, function (index)
                    {
                        $(objs[index]).toggle();
                    });
                }
                else {
                    i = 0;
                    j = setInterval(function ()
                    {
                        var obj = objs[i];
                        i = i + 1;
                        $(obj).toggle();
                        if (i === objs.length) {
                            clearInterval(j);
                        }
                    }, 1);
                }
            }
        },

        toggleSection: function (evt)
        {
            var section = $(evt.currentTarget).parent(), i = 0, objs, j;
            this.toggleNavSection(section);
            evt.stopPropagation();
        },
        render: function ()
        {
            this.setElement(this.container);
        }

    });

    return ListGroup;
});