function PlainSearch()
{
    "use strict";
    var searchText, vinoptions;
    return {
        filter: function (objects, inputText, options)
        {
            searchText = inputText;
            vinoptions = options;
            if (objects) {
                return objects.filter(this.filterUsingAttributes.bind(this));
            }
            return objects;
        },
        filterExact: function (objects, inputText, options)
        {
            searchText = inputText;
            vinoptions = options;
            if (objects) {
                return objects.filter(this.filterUsingAttribute.bind(this));
            }
            return objects;
        },
        matchText: function (text, searchText)
        {
            return text && text.toLowerCase().indexOf(searchText.trim().toLowerCase()) >= 0;
        },
        getTooltips: function (item)
        {
            return item.tooltips || (item.getToolTips ? item.getToolTips() : []);
        }, getTooltipValue: function (toolTips, key)
        {
            return toolTips[key].value || toolTips[key].getValue && toolTips[key].getValue() || "";
        }, filterUsingAttributes: function (item)
        {
            var key, toolTip;
            item = item.attributes ? item.attributes : item;
            if (vinoptions && !isObjectActive(item.optionExpression, vinoptions)) {
                return false;
            }

            if ((this.matchText(item.mainText, searchText)) ||
                    (this.matchText(item.subText, searchText)) ||
                    (this.matchText(item.nameAttr, searchText)) ||
                    (this.matchText(item.folder, searchText))) {
                return true;
            }

            var toolTips = this.getTooltips(item);
            var matched = false;
            for (key in toolTips) {
                if (toolTips.hasOwnProperty(key)) {
                    toolTip = this.getTooltipValue(toolTips, key);
                    matched = this.matchText(toolTip, searchText);
                    if (matched) {
                        return matched;
                    }
                }
            }

            if (item.folders) {
                var foldersData = item.folders;
                var pathMatched = false;
                if (foldersData instanceof Array) {
                    pathMatched = foldersData.some(function (path)
                    {
                        return this.matchText(path, searchText);
                    }.bind(this));
                }
                else {
                    pathMatched = this.matchText(foldersData, searchText);
                }
                return pathMatched;
            }

            return false;
        }, filterUsingAttribute: function (item)
        {
            var key, toolTip;
            item = item.attributes ? item.attributes : item;
            if (vinoptions && !isObjectActive(item.optionExpression, vinoptions)) {
                return false;
            }

            if (item.mainText === searchText || item.nameAttr === decodeURIComponent(searchText)) {
                return true;
            }
        }
    };
}