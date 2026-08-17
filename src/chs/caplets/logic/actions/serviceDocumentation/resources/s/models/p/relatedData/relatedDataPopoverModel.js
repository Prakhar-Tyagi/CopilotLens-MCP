/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor, _*/
define("RelatedDataPopoverModel",
        ["PopoverModel"],
        function (PopoverModel)
        {
            "use strict";
            var RelatedDataPopoverModel = PopoverModel.extend({
                initialize: function ()
                {
                    var model = this;
                    mentor.publisher.eventDispatcher.attachEventListener(mentor.publisher.events.CLOSE_POPOVER,
                            function (evt)
                            {
                                model.trigger("closed");
                            });
                },
                loadCollections: function (model)
                {
                    this.trigger("load");
                }
            }), relatedDataPopoverModel;
            relatedDataPopoverModel = new RelatedDataPopoverModel();
            return _.extend(relatedDataPopoverModel, Backbone.Events);
        });

