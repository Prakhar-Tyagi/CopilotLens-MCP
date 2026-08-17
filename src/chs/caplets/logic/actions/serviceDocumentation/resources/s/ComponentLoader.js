/**
 * This material contains trade secrets or otherwise confidential information owned by
 * Siemens Industry Software Inc.or its affiliates (collectively, “SISW”), or its licensors.
 * Access to and use of this information is strictly limited as set forth in the Customer’s
 * applicable agreements with SISW.
 *
 * Copyright 2021 Siemens
 */
define(["jquery", "underscore", "SmartViewComponents"],
    function ($, _, SmartViewComponents)
    {
        "use strict";

        return {
            loadedComponents: {},

            getComponentViewByName: function (name)
            {
                var prop, comps = this.loadedComponents;
                for (prop in comps) {
                    if (comps.hasOwnProperty(prop) && comps[prop].get(name)) {
                        return comps[prop].get(name).get("view");
                    }
                }
            },

            loadComponents: function (url, options)
            {
                function noop()
                {
                };

                options = options || {};
                options.preRender = options.preRender || noop;
                options.postRender = options.postRender || noop;
                options.beforeComponentsStartLoading = options.beforeComponentsStartLoading || noop;
                var components;
                options.beforeComponentsStartLoading();
                if (!_.has(this.loadedComponents, url)) {
                    components = new SmartViewComponents([], {
                        url: url
                    });
                    components.fetch({
                        async: false
                    });
                    this.loadedComponents[url] = components;
                }

                components = this.loadedComponents[url];
                components.onceLoaded(function ()
                {
                    options.preRender();
                    components.forEach(function (component)
                    {
                        var view = component.get('view');
                        if (!view.doNotLoadOnStart && view.render) {
                            view.render();
                        }
                    });
                    options.postRender();
                });
            }
        };
    }
);