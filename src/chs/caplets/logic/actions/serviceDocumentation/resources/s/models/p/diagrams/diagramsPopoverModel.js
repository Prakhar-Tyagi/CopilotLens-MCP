/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s 
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define, mentor*/
define("DiagramsPopoverModel", ["PopoverModel", "DiagramsCollection"],
    function (PopoverModel, diagrams) {
        "use strict";
        var DiagramsPopoverModel = PopoverModel.extend({
            loadCollections : function (model) {
                diagrams.fetch(model);
            }
        }), diagramsPopoverModel;
        diagramsPopoverModel = new DiagramsPopoverModel();
        return _.extend(diagramsPopoverModel, Backbone.Events);
    });

