
define('routers/customPluginRouter', ["fileDisplayHandler", "componentRouter"], function (fileDisplayHandler, componentRouter) {
    return extend(componentRouter, {
        openComponent: function (options) {
            options.componentType = options.componentType && options.componentType.toLowerCase();
            this.getObjectLevelPluginData(options);
        },
        getObjectLevelPluginData: function(options) {
            componentRouter.getComponentByNameAndType(options.componentName, options.componentType, "", function(data) {
                const p = mentor.publisher;
                const systems = p.project.getSystems();
                if (systems) {
                    if (data && data.items && data.items.length) {
                        const currentSystem = systems.filter(system => system.getName() === Utils.getUrlParameter('system'));
                        const currentSystemId = currentSystem && currentSystem[0] ? currentSystem[0].systemId : "";
                        let selectedSystemItem = {};
                        for(let i in data.items) {
                            const item = data.items[i];
                            if (item.name === options.componentName && item.systemUid === currentSystemId) {
                                selectedSystemItem = item;
                                break;
                            }
                        }
                        if (selectedSystemItem && selectedSystemItem.objectId) {
                            const componentObject = p.objectDataLoader.load(selectedSystemItem.systemUid, selectedSystemItem.objectId, p.project.getId())
                            if (componentObject) {
                                const content = componentObject.getCustomData();
                                if (content) {
                                    // need to confirm if there will be multiple custom plugin how the data will be
                                    const contentToShow = content[0] && content[0].listItems && content[0].listItems.filter(cont => cont.mainText === Utils.getUrlParameter('viewName'))[0];
                                    if (contentToShow) {
                                        contentToShow.type = mentor.publisher.contentType.CUSTOM_VIEW;
                                        mentor.publisher.fileDisplayHandler.display(contentToShow);
                                    } else {
                                        alert(mentor.publisher.languageTranslator.localize("AlertNotLoadReportByName").format(Utils.getUrlParameter('viewName')));
                                    }
                                }
                            }
                        } else {
                            alert(mentor.publisher.languageTranslator.localize("AlertObjTypeWithSearchItemNotFound").format("System", Utils.getUrlParameter('system')));
                        }
                    } else {
                        alert(mentor.publisher.languageTranslator.localize("AlertNotLoadReportByName").format(Utils.getUrlParameter('viewName')));
                    }
                } else {
                    alert(mentor.publisher.languageTranslator.localize("AlertNotLoadReportByName").format(Utils.getUrlParameter('viewName')));
                }
            });
        }
    });
});