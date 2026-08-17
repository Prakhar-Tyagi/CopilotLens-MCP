/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
/*global define*/
//todo delete this class
define(["backbone", "underscore"],
    function (Backbone, underscore, template) {
        "use strict";
        var Toolbar = Backbone.View.extend({
            render : function (content) {
                content = content || {};
                if (this.container && content.title) {
                    this.setElement(this.container);
                    var template = underscore.template(this.templateHTML)(content);
                    this.$el.append(template);
                    return this;
                }
            }
        });

        return new Toolbar();
    });

