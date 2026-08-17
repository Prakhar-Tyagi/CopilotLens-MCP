function SearchUsingIndex()
{
    "use strict";
    return {
        filter: function (objects, itemsOfType, options)
        {
            this.indexes = itemsOfType;
            var that = this;
            if (objects && this.indexes) {
                var callback = this.filterUsingIndexedData.bind(this, objects, options);
                return objects.filter(callback);
            }
            else if (options) {
                return objects.filter(function (item)
                {
                    return isObjectActive(item.optionExpression, options);
                });
            }
            return objects;
        },
        getObjectId: function (objects, item)
        {
            var objectId = item.id;
            return objectId;
        },
        filterItems: function (id, item, element)
        {
            return element.id === id;
        },
        filterUsingIndexedData: function (objects, options, item)
        {
            var that = this;
            if (this.indexes && this.indexes.length > 0) {
                var id = this.getObjectId(objects, item);
                if (id) {
                    var callback = this.filterItems.bind(this, id, item);
                    return (isObjectActive(item.optionExpression, options)) && this.indexes.some(callback);
                }
            }
            return false;
        }
    };
}