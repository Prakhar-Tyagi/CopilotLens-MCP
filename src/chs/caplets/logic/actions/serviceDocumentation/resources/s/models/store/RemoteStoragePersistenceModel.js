/**
 * Created by kayyagar on 24-01-2016.
 */
define("RemoteStoragePersistenceModel", [
    'jquery',
    'underscore',
    'backbone'
], function ($, underscore, Backbone)
{
    "use strict";
    var RemoteStoragePersistenceModel, ajaxReg;

    ajaxReg = function (model, url, method, options, doSave)
    {
        $.ajax({
            async: true, url: url, type: method, success: function (data, textStatus, XMLHttpRequest)
            {
                var value = XMLHttpRequest.responseText;
                if (doSave) {
                    model.set(JSON.parse(value));
                }
                (options && options.success) ? options.success() : '';
                //console.log(value);
            }, error: function (data, textStatus, XMLHttpRequest)
            {
                (options && options.error) ? options.error() : '';
                //console.log(textStatus);
            }, dataType: (Utils.is_msie()) ? "text" : "html"
        });
    }, RemoteStoragePersistenceModel = Backbone.Model.extend({
        defaults: {
            id: ''
        },
        fetch: function (options)
        {
            var queryUrl = this.url + '?id=' + this.get('id');
            ajaxReg(this, queryUrl, 'GET', options, true);
        },

        save: function (attributes)
        {
            var attrParams = $.param(attributes), saveUrl;
            saveUrl = this.url + '?' + attrParams;
            ajaxReg(this, saveUrl, 'POST');
        },

        destroy: function (options)
        {
        }
    });
    return RemoteStoragePersistenceModel;
});
