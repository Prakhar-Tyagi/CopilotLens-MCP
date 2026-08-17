var plainTextSearch = new PlainSearch();
var indexBasedTextSearch = new SearchUsingIndex();
var textSearch = new Search()();
textSearch.setAttrBasedSearch(plainTextSearch);
textSearch.setIndexBasedSearch(indexBasedTextSearch);
function createDataLoader()
{
    return {
        objects: [],
        loaded: false,
        ajax: ajax,
        textSearch: textSearch,
        onDataLoad: function (loadedObjects)
        {
            logMessage("data loaded for " + this.url);
            try {
                this.objects = JSON.parse(loadedObjects);
            }
            catch (e) {
                this.objects = [];
            }
            logMessage("data loaded  " + this.objects);
            this.loaded = true;
        },
        init: function (data)
        {
            logMessage("Initializing");
            var that = this;
            if (that.url !== data[1]) {
                that.url = data[1];
                this.ajax(that.url, that.onDataLoad.bind(this), true);
            }
        },
        reset: function ()
        {
            this.objects = [];
            this.loaded = false;
        },
        getSize: function ()
        {
            logMessage("get Size method ");
            if (this.loaded) {
                //return obj
            }
            else {
                logMessage("wait for data to be loaded get Size");
                setTimeout(this.getSize.bind(this), 1000);
            }

        },
        findFirstMatchingElement: function (items, attrToMatch)
        {
            var element;
            attrToMatch = attrToMatch || {};
            if (attrToMatch.name && attrToMatch.value) {
                for (var i in items) {
                    if (items.hasOwnProperty(i)) {
                        var item = items[i];
                        if (item && item[attrToMatch.name] === attrToMatch.value) {
                            element = item;
                            break;
                        }
                    }
                }
            }
            return element;
        }, getObjectByAttribute: function (message, options, callback)
        {
            if (options.attributes) {
                var attrToMatch = options.attributes;
                var vinOptions = options.options;
                var filter = this.textSearch.getFilter(false);
                if (this.loaded) {
                    var data = this.objects;
                    var items = filter.filter(data, "", vinOptions);

                    var matchedItems = this.findFirstMatchingElement(items, attrToMatch);
                    options.items = matchedItems;
                    callback(["getObjectByAttribute", options]);
                }
                else {
                    logMessage("wait for data to be loaded getItems");
                    var that = this;
                    setTimeout(function ()
                    {
                        that.getObjectByAttribute(message, options, callback);
                    }, 1000);
                    //callback([message, {status: "error", reason: "Dataloader is not initialized"}]);
                }

            }

        },
        findMatchingElements: function (items, attrsToMatch)
        {
            var element = [], flag;
            attrsToMatch = attrsToMatch || {};
            for (var i in items) {
                if (items.hasOwnProperty(i)) {
                    let item = items[i];
                    flag = this.filterItemByAttributes(item, attrsToMatch);
                    if (flag) {
                        element.push(item);
                    }
                }
            }
            return element;
        }, filterItemByAttributes: function(item, attrsToMatch)
        {
            let flag = true;
            for(var i in attrsToMatch) {
                if (attrsToMatch.hasOwnProperty(i)) {
                    const attrToMatch = attrsToMatch[i];
                    if (item && item[attrToMatch.name] !== attrToMatch.value) {
                        flag = false;
                        break;
                    }
                }
            }
            return flag;
        },
        getObjectsByAttribute: function (message, options, callback)
        {
            if (options.attributes) {
                var attrToMatch = options.attributes;
                var vinOptions = options.options;
                var filter = this.textSearch.getFilter(false);
                if (this.loaded) {
                    var data = this.objects;
                    var items = filter.filter(data, "", vinOptions);

                    var matchedItems = this.findMatchingElements(items, attrToMatch);
                    options.items = matchedItems;
                    callback(["getObjectsByAttribute", options]);
                }
                else {
                    logMessage("wait for data to be loaded getItems");
                    var that = this;
                    setTimeout(function ()
                    {
                        that.getObjectsByAttribute(message, options, callback);
                    }, 1000);
                }
            }
        },
        addTotalNumberOfObjects: function (items, start, end)
        {
            var itemsToShow = items.slice(start, end) || [];
            for (var i in itemsToShow) {
                if (itemsToShow.hasOwnProperty(i)) {
                    var item = itemsToShow[i];
                    if (item) {
                        item.totalObjects = items.length;
                    }
                }
                return itemsToShow;
            }
        }, getItems: function (message, options, callback)
        {
            if (this.url) {
                var start = options.start;
                var end = options.end;
                var searchText = options.searchText;
                var vinOptions = options.options;
                var isIndexEnabled = options.indexes;
                var indexes = options.indexes;

                logMessage("getItems ");
                logMessage("getItems " + options);
                if (this.loaded) {
                    var data = this.objects;
                    var filter = this.textSearch.getFilter(isIndexEnabled);
                    var items = data;
                    if (indexes) {

                        items = filter.filter(data, indexes, vinOptions);
                    }
                    else if (searchText) {
                        items = filter.filter(data, searchText, vinOptions);
                    }
                    else if (vinOptions) {
                        items = filter.filter(data, "", vinOptions);
                    }
                    if (end > items.length - 1) {
                        end = items.length;
                    }
                    options.size = items.length;
                    options.items = this.addTotalNumberOfObjects(items, start, end);
                    callback(["getItems", options]);
                }
                else {
                    logMessage("wait for data to be loaded getItems");
                    var that = this;
                    setTimeout(function ()
                    {
                        that.getItems(message, options, callback);
                    }, 1000);
                }
            }
            else {
                callback([]);
            }

        }
    };
}
var handlers = createDataLoader();

function getDataLoader(methodName)
{
    return handlers[methodName].bind(handlers);
}
function logMessage(message)
{
    // if(window.console) {
    //     window.console.log(message); 
    // }
}

