
define('routers/powerToGroundRouter', ["fileDisplayHandler", "componentRouter"], function (fileDisplayHandler, componentRouter) {
    return extend(componentRouter, {
        openComponent: function (options) {
            options.componentType = options.componentType && options.componentType.toLowerCase();
            const systems = mentor.publisher.project.getSystems();
            if (systems) {
                const that = this;
                componentRouter.getComponentByNameAndType(options.componentName, options.componentType, "", function(data) {
                    if (data && data.items && data.items.length) {
                        let connectivityDisplayed = false;
                        const currentSystem = systems.filter(system => system.getName() === Utils.getUrlParameter('system'));
                        const currentSystemId = currentSystem && currentSystem[0] ? currentSystem[0].systemId : "";
                        for(let i in data.items) {
                            const item = data.items[i];
                            if (item.name === options.componentName && item.systemUid === currentSystemId) {
                                that.displayConnectivity(item);
                                connectivityDisplayed = true;
                                break;
                            }
                        }
                        // shared object
                        if (!connectivityDisplayed) {
                            for(let i in data.items) {
                                const item = data.items[i];
                                const objectData = mentor.publisher.objectDataLoader.load(item.systemUid, item.objectId, mentor.publisher.project.getId());
                                const crossRefData = objectData.getCrossReferences();
                                for(let j in crossRefData.xrefs) {
                                    const xref = crossRefData.xrefs[j];
                                    const mainText = xref.mainText.split(":")[0];
                                    if(mainText === Utils.getUrlParameter('system')) {
                                        that.displayConnectivity(xref);
                                        connectivityDisplayed = true;
                                        break;
                                    }
                                }
                                if(connectivityDisplayed) break;
                            }
                        }
                    }
                });
            }
        },
        displayConnectivity: function(item) {
            const p = mentor.publisher;
            const componentObject = p.objectDataLoader.load(item.systemUid, item.objectId, p.project.getId());
            if (componentObject && componentObject.getSignalTraceFiles) {
                const content = componentObject.getSignalTraceFiles();
                if (content) {
                    const connectivityFile = content && content.listItems && content.listItems[0] && content.listItems[0].id;
                    displayConnectivity(connectivityFile, false, true, 'Build');
                    crossHighlightHandler.initCrossHighlight(item.objectId);
                }
            }
        }
    });
});