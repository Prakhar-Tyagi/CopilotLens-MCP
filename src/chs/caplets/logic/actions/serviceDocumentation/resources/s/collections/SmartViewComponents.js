/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(
    [
        "jquery",
        "underscore",
        "backbone",
        "SmartViewComponent"
    ],
    function ($, _, Backbone, SmartViewComponent) {
        "use strict";

        var Collection = Backbone.Collection.extend({
            model: SmartViewComponent,
            loadedComponentCount: 0,

            initialize: function () {
                var that = this;

                this.on('add', function (component) {
                    component.on('change:view', function() {
                        that.loadedComponentCount = that.loadedComponentCount + 1;
                        if (that.length === that.loadedComponentCount) {
                            setTimeout(() => {
                                that.trigger("loaded");
                            }, 200);
                        }
                    });
                    component.load();
                });
            },

            fetch: function(options) {
                _.extend(options, {
                    dataType : "xml"
                });

                return Backbone.Collection.prototype.fetch.call(this, options);
            },

            parse: function (data) {
                var componentElements = $('component', data);

                return _.map(componentElements, function (element) {
                    var templateLocation = "text!" +  $(element).attr('template');
                    var componentName = $(element).attr('name');

                    if (componentName === 'harness-layouts' && templateLocation.indexOf("treeView.html") > -1) {
                        var p = mentor && mentor.publisher;
                        p && (p.harnessTreeViewEnabled = true);
                    }

                    return {
                        id: componentName,
                        viewLocation: $(element).attr('view'),
                        templateLocation: templateLocation,
                        container: $(element).attr('container')
                    };
                });
            },

            onceLoaded: function (complete) {
                this.once("loaded", function () {
                    complete && complete();
                });

                if (this.loadedComponentCount === this.length) {
                    this.trigger("loaded");
                }
            }
        });

        return Collection;
    }
);