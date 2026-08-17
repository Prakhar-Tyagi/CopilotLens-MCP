/**
 * Created by kayyagar on 24-01-2016.
 */
define("CookieBasedePersistenceModel", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone)
{
    "use strict";
    var CookieBasedePersistenceModel;

    CookieBasedePersistenceModel = Backbone.Model.extend({
        defaults: {
            id: '',
            value: ''
        },
        fetch: function (options)
        {
            var value = (Utils.readCookie(this.get('id')));
            this.set(JSON.parse(value));
            options.success();
        },

        save: function (attributes)
        {
            Utils.createCookie(this.get("id"), JSON.stringify(attributes), Utils.getCookiesDuration());
        },

        destroy: function (options)
        {
        }
    });
    return CookieBasedePersistenceModel;
});
