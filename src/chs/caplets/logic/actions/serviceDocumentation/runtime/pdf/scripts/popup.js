function getObjectDataForId(id, match) {
    var objects = app.objectData[id];
    var menuObjectsToShow = [];
    var menuObjects = createMenuObjects(objects, match);
    for (var k = 0; k < menuObjects.length; k++) {
        if (menuObjects[k].oSubMenu) {
            menuObjectsToShow.push(menuObjects[k]);
        }
    }
    return menuObjectsToShow;
}

function createMenuObject(menuObj, match) {
    var obj;
    var children = [];
    if (isValid(menuObj, match)) {
        obj = {};
        obj.cName = menuObj.cName;
        obj.cReturn = menuObj.cReturn;
        children = createMenuObjects(menuObj.oSubMenu, match);
        if (children && children.length > 0) {
            obj.oSubMenu = children;
        }
    }
    return obj;
}

function createMenuObjects(menuObjects, match) {
    var objects = [];
    var obj;
    if (menuObjects) {
        for (var k = 0; k < menuObjects.length; k++) {
            obj = createMenuObject(menuObjects[k], match);
            if (obj) {
                objects.push(obj);
            }
        }
    }
    return objects;
}

function isValid(menuObj, match) {
    if (!match) {
        return true;
    }
    if (!menuObj.key) {
        return true;
    }
    return match !== menuObj.key;
}