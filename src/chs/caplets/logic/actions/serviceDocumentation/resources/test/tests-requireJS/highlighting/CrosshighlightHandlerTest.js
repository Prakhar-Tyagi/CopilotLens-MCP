describe("crossHighlightHandlerTest", function () {
    var origGetID, origLoad;
    beforeEach(function () {
        origGetID = mentor.publisher.project.getId;
        origLoad = mentor.publisher.objectDataLoader.load;
        mentor.publisher.project.getId = function () {
            return 'project';
        };
        mentor.publisher.objectDataLoader.load = function () {
            return {
                getCrossReferences: function () {
                    return {
                        xref: [{objectId: "id1"}]
                    }
                },
                getHarnessLayouts: function () {
                    return [{
                        objectids: ["id2"]

                    }]
                },
                getRelatedObjects: function () {
                    return [{
                        objectids: ["id3"]

                    }];
                },
                getShieldBodyUIDs: function () {
                    var shieldBodyDataArray = [], shieldBodyObject = {};
                    shieldBodyObject.id = 'sbid1';
                    shieldBodyDataArray.push(shieldBodyObject);
                    return shieldBodyDataArray;
                }
            }
        };
    });
    afterEach(function () {
        mentor.publisher.project.getId = origGetID;
        mentor.publisher.objectDataLoader.load = origLoad;
    });
    it("should also highlight harness object", function () {
        var ids = crossHighlightHandler.getAllObjectIdsToHighlight({
            getCrossReferences: function () {
                return {
                    xref: [{objectId: "id1"}]
                }
            },
            getHarnessLayouts: function () {
                return [{
                    objectids: ["id2"]

                }];
            },
            getRelatedObjects: function () {
                return [{
                    objectids: ["id3"]

                }];
            }
        });
        expect(ids.length).toBe(3);
        expect(ids.includes("id1"));
        expect(ids.includes("id2"));
        expect(ids.includes("id3"));
    });

    it("should cross highlight when not resetting the previuos highlighted uids", function () {
        var highlightedInChildWindow = false, callToInitiateCrossHighlight = false,
                testableCrossHighlightHandler = new CrossHighlightHandler();
        testableCrossHighlightHandler.twoDHotSpotText = 'test2dhp';
        testableCrossHighlightHandler.initiateHighlightInChildWindow =
                function (uid, sourceContainerId, notToResetFlag, twoDHotSpotText, popoutId, data) {
                    expect(uid).toBe("uid1");
                    expect(sourceContainerId).toBe("scid1");
                    expect(notToResetFlag).toBe(false);
                    expect(twoDHotSpotText).toBe("test2dhp");
                    expect(popoutId).toBe("popoutid1");
                    expect(data.objectId).toBe("uid1");
                    expect(data.systemId).toBe("sfid1");
                    expect(data.fromMainWindow).toBe(false);
                };
        testableCrossHighlightHandler.initiateCrossHighlight =
                function (uid, sourceContainerId, fromParentWindow, notToResetFlag, popoutId, data) {
                    expect(uid).toBe("uid1");
                    expect(sourceContainerId).toBe("scid1");
                    expect(notToResetFlag).toBe(false);
                    expect(popoutId).toBe("popoutid1");
                    expect(data.objectId).toBe("uid1");
                    expect(data.systemId).toBe("sfid1");
                    expect(data.fromMainWindow).toBe(false);
                };
        testableCrossHighlightHandler.highlightObjects =
                function (objects, uidArray, sourceContainerId, popoutId, systemFolderId) {
                    expect(objects.length).toBe(3);
                    expect(uidArray.length).toBe(1);
                    expect(sourceContainerId).toBe("scid1");
                    expect(popoutId).toBe("popoutid1");
                    expect(systemFolderId).toBe("sfid1");
                };
        testableCrossHighlightHandler.highlightShieldBodyIds =
                function (shieldBodyUIDs, uid, sourceContainerId, uidArray, popoutId, systemFolderId) {
                    expect(uid).toBe("uid1");
                    expect(shieldBodyUIDs.length).toBe(1);
                    expect(uidArray.length).toBe(1);
                    expect(sourceContainerId).toBe("scid1");
                    expect(popoutId).toBe("popoutid1");
                    expect(systemFolderId).toBe("sfid1");
                };
        testableCrossHighlightHandler.crossHighLightAcrossWindows('uid1', 'sfid1', 'scid1', 'project', 'popoutid1',
                false, false);
    });

    it("should cross highlight when resetting the previuos highlighted uids", function () {
        var highlightedInChildWindow = false, callToInitiateCrossHighlight = false,
                testableCrossHighlightHandler = new CrossHighlightHandler();
        testableCrossHighlightHandler.twoDHotSpotText = 'test2dhp';
        testableCrossHighlightHandler.initiateHighlightInChildWindow =
                function (uid, sourceContainerId, notToResetFlag, twoDHotSpotText, popoutId, data) {
                    expect(uid).toBe("uid1");
                    expect(sourceContainerId).toBe("scid1");
                    expect(notToResetFlag).toBe(true);
                    expect(twoDHotSpotText).toBe("test2dhp");
                    expect(popoutId).toBe("popoutid1");
                    expect(data.objectId).toBe("uid1");
                    expect(data.systemId).toBe("sfid1");
                    expect(data.fromMainWindow).toBe(false);
                };
        testableCrossHighlightHandler.initiateCrossHighlight =
                function (uid, sourceContainerId, fromParentWindow, notToResetFlag, popoutId, data) {
                    expect(uid).toBe("uid1");
                    expect(sourceContainerId).toBe("scid1");
                    expect(notToResetFlag).toBe(true);
                    expect(popoutId).toBe("popoutid1");
                    expect(data.objectId).toBe("uid1");
                    expect(data.systemId).toBe("sfid1");
                    expect(data.fromMainWindow).toBe(false);
                };
        testableCrossHighlightHandler.highlightObjects =
                function (objects, uidArray, sourceContainerId, popoutId, systemFolderId) {
                    expect(objects.length).toBe(3);
                    expect(uidArray.length).toBe(1);
                    expect(sourceContainerId).toBe("scid1");
                    expect(popoutId).toBe("popoutid1");
                    expect(systemFolderId).toBe("sfid1");
                };
        testableCrossHighlightHandler.highlightShieldBodyIds =
                function (shieldBodyUIDs, uid, sourceContainerId, uidArray, popoutId, systemFolderId) {
                    expect(uid).toBe("uid1");
                    expect(shieldBodyUIDs.length).toBe(1);
                    expect(uidArray.length).toBe(1);
                    expect(sourceContainerId).toBe("scid1");
                    expect(popoutId).toBe("popoutid1");
                    expect(systemFolderId).toBe("sfid1");
                };
        testableCrossHighlightHandler.crossHighLightAcrossWindows('uid1', 'sfid1', 'scid1', 'project', 'popoutid1',
                false, true);
    });
    it("should find connectionId to highlight when single diagram linked", function () {
        objDataArray = [{
            diagramId: "diaUID1",
            connUID: "Id1"
        }];
        testableCrossHighlightHandler = new CrossHighlightHandler();
        expect(testableCrossHighlightHandler.getUidToHighlight("diaUID1","", objDataArray)).toBe("Id1");
    });

    it("should find connectionId to highlight when multiple diagrams linked", function () {
        objDataArray = [
            {
                diagramId: "diaUID1",
                connUID: "Id1"
            },
            {
                diagramId: "diaUID2",
                connUID: "Id2"
            }
        ];
        testableCrossHighlightHandler = new CrossHighlightHandler();
        expect(testableCrossHighlightHandler.getUidToHighlight("diaUID2","", objDataArray)).toBe("Id2");
        expect(testableCrossHighlightHandler.getUidToHighlight("diaUID1","", objDataArray)).toBe("Id1");
    });

    it("should be able to trigger highlighting from 2dsvg", function () {
        objDataArray = [
            {
                diagramId: "diaUID1",
                connUID: "Id1"
            },
            {
                diagramId: "diaUID2",
                connUID: "Id2"
            }
        ];
        testableCrossHighlightHandler = new CrossHighlightHandler();

        spyOn(testableCrossHighlightHandler, "highLightInWindow");
        testableCrossHighlightHandler.triggerHighLightingfrom2DSVG(window, objDataArray, "testSourceContainerID");
        expect(testableCrossHighlightHandler.highLightInWindow).toHaveBeenCalled();
    });

    it("should be able to zoom into views", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        var origGet=mentor.publisher.selectedSystem.get;
        mentor.publisher.selectedSystem.get=function () {return true;};
        //testableCrossHighlightHandler.zoomViews();
        mentor.publisher.selectedSystem.get=origGet;
    });

    it("should be able to highlight in window", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        spyOn(window.crossHighlightHandler, "highElementsInSVG");
        testableCrossHighlightHandler.highLightInWindow(window, objDataArray, "testSourceContainerID", null);
        expect(window.crossHighlightHandler.highElementsInSVG).toHaveBeenCalled();
    });

    it("should be able to highlight element in report", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        spyOn(testableCrossHighlightHandler, "crossHighlightReport");
        testableCrossHighlightHandler.highlightElementInReport();
        expect(testableCrossHighlightHandler.crossHighlightReport).toHaveBeenCalled();
    });

    it("should be able to cross highlight report", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        testableCrossHighlightHandler.crossHighlightReport();
        expect(window.isSVGClick).toBeFalsy();
    });

    it("should be able to highlight objects", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        var uidArray=[];
        testableCrossHighlightHandler.highlightObjects([{
            objectId: "uid1",
        }], uidArray, "testSourceContainerID", "testPopoutID", "TestSysFolderID");
        expect(uidArray).toEqual(["uid1"]);
    });

    it("should be able to highlight shield body ids", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        var uidArray=[];
        testableCrossHighlightHandler.highlightShieldBodyIds([{
            id: "uid1",
        }], 'testUid', "testSourceContainerID", uidArray, "testPopoutID", "TestSysFolderID");
        expect(uidArray).toEqual(["uid1"]);
    });

    it("should be able to initiate cross highlight", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        spyOn(testableCrossHighlightHandler, "highlightElementInReport").andCallThrough();
        testableCrossHighlightHandler.initiateCrossHighlight();
        expect(testableCrossHighlightHandler.highlightElementInReport).toHaveBeenCalled();
    });

    it("should be able to zoom object in3dxml", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        var objectPathIn3dXML = {};
        testableCrossHighlightHandler.zoomObjectIn3DXML(objectPathIn3dXML);
        expect(packageModel.get('partNumber')).toEqual(objectPathIn3dXML);
    });

    it("should be able to initiate highlight in child window", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        spyOn(mentor.publisher.popoutHandler, "highlighObject");
        testableCrossHighlightHandler.initiateHighlightInChildWindow();
        expect(mentor.publisher.popoutHandler.highlighObject).toHaveBeenCalled();
    });

    it("should be able to initiate highlight in child window", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        spyOn(testableCrossHighlightHandler, "crossHighlightInAJTView");
        testableCrossHighlightHandler.crossHighlighJTViews();
        expect(testableCrossHighlightHandler.crossHighlightInAJTView).toHaveBeenCalled();
    });

    it("should be able to cross highlight in ajtview", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        var doc = {
            jtViewerManager: {
                getSelectedParts: function () {return ["testID1", "testID2", "testID3"]},
                setSelectionByPsId: function () {},
            },
            psidVSZ: function () {},
        }, jtIDs = ["testID4", "testID5"];
        spyOn(doc.jtViewerManager, "setSelectionByPsId");
        testableCrossHighlightHandler.crossHighlightInAJTView(doc, jtIDs);
        expect(doc.jtViewerManager.setSelectionByPsId).toHaveBeenCalled();
    });

    xit("should be able to init cross highlight", function () {
        testableCrossHighlightHandler = new CrossHighlightHandler();
        testableCrossHighlightHandler.initCrossHighlight("uid1", "scid1", "project", "did1", "pid1", true);
        expect(selectedSystem.get("objectId")).toBe("uid1");
    });

});
