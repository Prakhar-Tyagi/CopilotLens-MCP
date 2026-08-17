/**
 * Created by kayyagar on 24-01-2016.
 */
define("PersistenceModelFactory", [
    'LocalStoragePersistenceModel', 'CookieBasedePersistenceModel', 'RemoteStoragePersistenceModel'
], function (LocalStoragePersistenceModel, CookieBasedePersistenceModel, RemoteStoragePersistenceModel)
{
    "use strict";
    var supportsLocalStorage;
    supportsLocalStorage = function ()
    {
        try {
            //local storage is an alias for window['localStorage']
            return ('localStorage' in window && window['localStorage']);
        }
        catch (e) {
            //console.log(e.stack);
        }
        return false;
    }

    return {
        createCompatibleModel: function ()
        {
            if (isHTTPProtocol()) {
                return CookieBasedePersistenceModel;
            }
            else if (supportsLocalStorage()) {
                return LocalStoragePersistenceModel;
            }
            return CookieBasedePersistenceModel;
        },
        createCookieBasedModel: function ()
        {
            return CookieBasedePersistenceModel;
        }
    }
});