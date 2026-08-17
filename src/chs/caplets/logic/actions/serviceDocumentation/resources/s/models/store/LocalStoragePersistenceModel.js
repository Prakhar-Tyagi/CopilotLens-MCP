/**
 * Created by kayyagar on 24-01-2016.
 */
define("LocalStoragePersistenceModel", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone)
{
    "use strict";
    var LocalStoragePersistenceModel;

    LocalStoragePersistenceModel = Backbone.Model.extend({
        defaults: {
            id: ''
        },
        fetch: function (options)
        {
            var value = (localStorage.getItem(this.get('id')));
            this.set(JSON.parse(value));
            options.success();
        },

        save: function (attributes)
        {
            localStorage.setItem(this.get("id"), JSON.stringify(attributes));
        },

        destroy: function (options)
        {
            localStorage.removeItem(this.get("id"));
        },
    });
    return LocalStoragePersistenceModel;
});
