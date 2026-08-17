define(["backbone"], function (Backbone) {
    return {
        addEffectivitAndZipLocationInURLAsParameters: function (url) {
            var urlToModify = url;
            if (getWindowObj().mentor.publisher.packectInfo) {
                var effectivity = getWindowObj().mentor.publisher.packectInfo.effectivity;
                var id = getWindowObj().mentor.publisher.packectInfo.packageId;
                var prependChar = "&";
                if (urlToModify.indexOf("?") < 0) {
                    prependChar = "?";
                }
                if (id && !effectivity) {
                    urlToModify =
                            urlToModify + prependChar + "packageId=" + id.replace("data/", "").replace("data\\", "");
                }
                else {
                    urlToModify = urlToModify + prependChar + "effectivity=" +
                            effectivity.replace("data/", "").replace("data\\", "")
                            + "&packageId=" + id.replace("data/", "").replace("data\\", "");
                }
            }
            return urlToModify;
        },
        urlPrefix: "zipped/",
        checkContentType: true,
        identifyIfContentIsZipped: function (url, subDir) {
            if (this.checkContentType) {
                this.checkContentType = false;
                if (subDir) {
                    var urlExists = this.urlExists(subDir + "/index.xml");
                    if (urlExists) {
                        this.urlPrefix = "";
                    }
                }
            }
            if (url && (url.indexOf('data/') === 0 || url.indexOf('data\\') === 0)) {
                return this.urlPrefix + url;
            }
            return url;
        },
        distinguishZippedContent: function (url) {
            var packageInfo = getWindowObj().mentor.publisher.packectInfo;
            if (url && packageInfo) {
                return this.identifyIfContentIsZipped(url, "data/" + packageInfo.packageId);
            }
            return url;
        },

        urlExists: function (url) {
            var http = new XMLHttpRequest();
            http.open('HEAD', url, false);
            http.send();
            return http.status !== 404;
        },
        isPopoutWindow: function () {
            return (window.opener && window.opener.mentor);
        }, getAllPackages: function () {
            var Packages = require("Packages");
            var allPackages = new Packages();
            var resp = allPackages.fetch({
                async: false
            });

            if (allPackages.models.length === 0) {
                var translator = mentor.publisher.languageTranslator;
                var clientType = resp.getResponseHeader("client-type");
                var errorMessage = updateClientType(translator.localize("NoValidPacketAvailableMsg"), clientType);
                showError(errorMessage);
                throw new Error(errorMessage);
            }

            return allPackages;
        },
        addPackageInformation: function (URL, selectPackage) {
            var projectId = selectPackage.get("projectId");
            var packageId = selectPackage.get("id").replace("\\", "/");
            var name = selectPackage.get("name");
            if (projectId && packageId && name) {
                URL += "&projId=" + projectId;
                URL += "&packageId=" + packageId;
                URL += "&package=" + name;
            }
            if (selectPackage && selectPackage.get("effectivityRange")) {
                URL += "&effRange=" + selectPackage.get("effectivityRange");
            }
            return URL;
        }, addEffAndProjectIdInURLs: function (URL) {
            var UserSession = require("UserSession");
            var selectPackage = UserSession.getActiveSession().get(UserSession.kSelectedPackageProperty);
            return this.addPackageInformation(URL, selectPackage);
        },
        initializeEffectivity: function (params) {
            if (!this.isPopoutWindow()) {
                var allPackages = this.getAllPackages();
                var packageId = params.projectId.replace("/", "\\");
                var firstEffectivity;
                if (params.range) {
                    var findPackageById = allPackages.findPackageById(packageId,
                            params.range,
                            params.projId);
                    firstEffectivity = findPackageById.id;
                }
                else {
                    var packageByName = allPackages.findPackageByName(packageId);
                    if (!packageByName) {
                        Backbone.history.navigate("");
                        return false;
                    }
                    firstEffectivity = packageByName.get("id");
                }

                var p = mentor.publisher;
                p.packectInfo = {};
                var packectInfo = p.packectInfo;
                packectInfo.packageId = packageId.replace("data/", "").replace("data\\", "");
                if (firstEffectivity) {
                    packectInfo.effectivity = firstEffectivity.replace("data/", "").replace("data\\", "");
                }
                else {
                    packectInfo.effectivity = undefined;
                }
            }
            return true;
        },

        setEffectivityInCookies: function () {
            var packageId = getWindowObj().mentor.publisher.packectInfo.packageId;
            Utils.createCookie("packageId", packageId.replace("data/", ""), Utils.getCookiesDuration());
            //var effValue = getWindowObj().mentor.publisher.packectInfo.effectivity;
            //if (effValue) {
               // Utils.createCookie("effectivity", effValue.replace("data/", ""),duration);
            //}
        },

        resetEffectivityCookies: function () {
            Utils.createCookie("packageId", "", -1);
            Utils.createCookie("effectivity", "", -1);
        }
    }
});