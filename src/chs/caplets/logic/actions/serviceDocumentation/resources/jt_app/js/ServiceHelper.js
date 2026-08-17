// COPYRIGHT 2016 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC.

var serverBaseURL = "http://sci6w052:8080/ConverterService_v170";
var showDebug = false;

function uploader(file, callback) {
	var xhr = new XMLHttpRequest();  // REST API for upload
	xhr.open('POST', serverBaseURL+'/converter/upload');
	xhr.onload = function () {
		callback(xhr);
	};

	if(showDebug) {
		console.log("ServiceHelper.uploader  REQUEST:");
		console.log(file);
	}
	var formData = new FormData();
	formData.append("file", file);
	formData.append("name", file.name); // needed by IE
	xhr.send(formData);
}

function exporter(ticket, callback) {
	var exportRequest = {
		input : ticket
	};

	var xhr = new XMLHttpRequest();  // REST API for export
	var exportPath = serverBaseURL+'/converter/export';
	xhr.open('POST', exportPath);
	xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
	xhr.onload = function () {
		callback(xhr);
	};

	if(showDebug) {
		console.log("ServiceHelper.exporter  REQUEST:");
		console.log(exportRequest);
	}
	xhr.send(JSON.stringify(exportRequest));
}

function exporterSVG(ticket, callback) {
	var exportRequest = {
		input : ticket,
		exportMonochrome : true // change this to false if you want full color!
	};

	var xhr = new XMLHttpRequest();  // REST API for exportPages
	var exportPath = serverBaseURL+'/converter/exportPages';
	xhr.open('POST', exportPath);
	xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
	xhr.onload = function () {
		callback(xhr);
	};

	if(showDebug) {
		console.log("ServiceHelper.exporterSVG  REQUEST:");
		console.log(exportRequest);
	}
	xhr.send(JSON.stringify(exportRequest));
}

function converter(inputJt, outputDir, callback, lod, log, useXT, usePMI, usePool, useNodenames, useBase64, lateLoad, deleteJt) {
	lodLvl  = (typeof lod === "number") ? lod : 0; // default to highest LOD
	logLvl  = (typeof log === "number") ? log : 1; // default to info
	useXT   = (useXT==null||undefined) ? false : useXT;
	usePMI  = (usePMI==null||undefined) ? true : usePMI;
	usePool = (usePool==null||undefined) ? true : usePool;
	useNodenames = (useNodenames==null||undefined) ? false : useNodenames;
	useBase64 = (useBase64==null||undefined) ? false : useBase64;
	lateLoad = (lateLoad==null||undefined) ? false : lateLoad;
	var convertRequest = {
		inputFile : inputJt,  // relative to input prefix
		outputFolder : outputDir,  // relative to output prefix
		lodLevel: lodLvl,
		logLevel: logLvl,
		useXT: useXT,
		usePMI: usePMI,
		usePooling: usePool,
		useNodeNames : useNodenames,
		useBase64 : useBase64
	};
	var xhr = new XMLHttpRequest();  // REST API for convert or covertLateLoad
	var converterPath = lateLoad
		? (deleteJt ? serverBaseURL+'/converter/convertLateLoad' : serverBaseURL+'/converter/convertLateLoadNoDelete')
		: (deleteJt ? serverBaseURL+'/converter/convert' : serverBaseURL+'/converter/convertNoDelete');
	xhr.open('POST', converterPath);
	xhr.setRequestHeader("Content-Type", "application/json;charset=UTF-8");
	xhr.onload = function () {
		callback(xhr);
	};

	if(showDebug) {
		console.log("ServiceHelper.converter  REQUEST:");
		console.log(convertRequest);
	}
	xhr.send(JSON.stringify(convertRequest));
}
