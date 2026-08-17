//©  2019 SIEMENS PRODUCT LIFECYCLE MANAGEMENT SOFTWARE INC
import {
	isBoolean,
	isFunction,
	isObject,
	notNullOrUndefined,
} from "../utilities/TypeChecking";
import { Friend as _ViewerFriend } from "../Viewer";
import { ClassFactory } from "../ClassFactory";
import { StringTable } from "../StringTable";

import { LogLevel, createLog } from "../Logger";
var debug = createLog( "SGOManager", LogLevel.DEBUG );
var error = createLog( "SGOManager", LogLevel.ERROR );
//var warning = createLog( "SGOManager", LogLevel.WARNING );
//var verbose = createLog( "SGOManager", LogLevel.VERBOSE );

const Friend = Object.freeze( {

	_ignoreSGO: Symbol(),
	_renderSGOs: Symbol(),
	_unionBoundingBox: Symbol()
} );

/**
 * @class SGOManager
 * @classdesc Class used to expose SGO functionality.<br><br>
 * NOTE:  Use [Viewer.addExtension]{@link PLMVisWeb.Viewer#addExtension} to instantiate.  Do not construct directly!
 * @memberof PLMVisWeb
 */
var SGOManager = function ( params ) { //NOSONAR

	// SGO
	let _sceneGraphObjects = [],
		_preSceneSGOs = [],
		_inSceneSGOs = [],
		_postSceneSGOs = [],
		_ignoreSGO = false;

	var _viewerReady = false;
	var _boundResizeHandler;

	function dispose () {
		console.log( "disposing of sgo's" );

		var disposeOf = function ( sgo ) {
			if ( isFunction( sgo.removeEvents ) ) {
				sgo.removeEvents();
			}
			sgo.viewer = null;
			delete sgo.original;
		};

		_sceneGraphObjects.forEach( disposeOf );
		_sceneGraphObjects = [];
		_preSceneSGOs = [];
		_postSceneSGOs = [];
		_inSceneSGOs = [];

		if ( notNullOrUndefined( _boundResizeHandler ) ) {
			this._viewer.removeEventListener( "resize", _boundResizeHandler );
		}
		this._viewer[ _ViewerFriend._render ]();
	};


	function _SGOManager ( params ) { // NOSONAR
		ClassFactory.makeDisposable( this );
		this.dispose = dispose.bind( this );
		this._viewer = params._parent;
		this._renderer = params._parent._renderer;

		// Watch for viewer's resize event indicating valid viewer size at initialization.
		_boundResizeHandler = resizeHandler.bind( this );
		this._viewer.addEventListener( "resize", _boundResizeHandler );
		// TODO:  At some point, there may be a better way to indicate the viewer is ready - a non-0 width/height.
		// For those SGOs that want their bounding area to be included, a valid size is required.
	}

	_SGOManager.prototype = {
		constructor: _SGOManager
	};

	_SGOManager.prototype[ Friend._renderSGOs ] = function ( sceneDepth ) {
		if ( !_ignoreSGO ) {
			var camInfo = this._viewer.getCameraInfo( false );

			if ( sceneDepth === SGOManager.RenderTime.PreScene ) {
				// PRE-Scene SGOs
				for ( var i = 0, l = _preSceneSGOs.length; i < l; i++ ) {
					_preSceneSGOs[ i ].scene.updateMatrixWorld( true );
					_preSceneSGOs[ i ].render( this._renderer, camInfo );
				}
			} else {
				if ( sceneDepth === SGOManager.RenderTime.InScene ) {

					// Render IN-scene SGOs
					for ( var i = 0, l = _inSceneSGOs.length; i < l; i++ ) {
						_inSceneSGOs[ i ].scene.updateMatrixWorld( true );
						_inSceneSGOs[ i ].render( this._renderer, camInfo );
					}
				} else {
					for ( var i = 0, l = _postSceneSGOs.length; i < l; i++ ) {
						_postSceneSGOs[ i ].scene.updateMatrixWorld( true );
						_postSceneSGOs[ i ].render( this._renderer, camInfo );
					}
				}
			}
		}
	};


	_SGOManager.prototype[ Friend._unionBoundingBox ] = function ( inputBBox ) {
		if ( _viewerReady ) {	// viewer must have a valid viewer size to union

			var getBBox = function ( object ) {
				if ( isObject( object.sgoBBox ) && object.sgoBBox.min && object.sgoBBox.max ) {
					return object.sgoBBox;
				}
				else if ( ( isBoolean( object.updateBBox ) && object.updateBBox ) && !object.front && !object.back ) { // updateBBox will be removed after the 1.7.0 release
					if ( object.boundingBox ) {
						return object.boundingBox;
					}
					else if ( object.original && object.original.geometry ) {// try to calculate a bounding box
						if ( object.original.geometry.boundingBox ) {
							return object.original.geometry.boundingBox;
						}
						else {
							object.original.geometry.computeBoundingBox();
							if ( object.original.geometry.boundingBox ) {
								return object.original.geometry.boundingBox;
							}
							else {
								debug( StringTable.stringsByLabel.SGOMANAGER_LOGGER_noBBox );
								return null;
							}
						}
					}
				}
				else {
					return null;
				}
			};

			if ( _preSceneSGOs.length > 0 ) {
				for ( var index = 0; index < _preSceneSGOs.length; index++ ) {
					var bbox = getBBox( _preSceneSGOs[ index ] );
					if ( bbox ) {
						inputBBox.union( bbox );
					}
				}
			}
			if ( _inSceneSGOs.length > 0 ) {
				for ( var index = 0; index < _inSceneSGOs.length; index++ ) {
					var bbox = getBBox( _inSceneSGOs[ index ] );
					if ( bbox ) {
						inputBBox.union( bbox );
					}
				}
			}
			if ( _postSceneSGOs.length > 0 ) {
				for ( var index = 0; index < _postSceneSGOs.length; index++ ) {
					var bbox = getBBox( _postSceneSGOs[ index ] );
					if ( bbox ) {
						inputBBox.union( bbox );
					}
				}
			}
		}
	};

	/**
	 * Adds a scene graph object to the view
	 *
	 * @function addSGO
	 * @memberof PLMVisWeb.SGOManager.prototype
	 *
	 * @param {Object} object - Three.js-constructed Mesh, Group, or Object3D
	 * @throws {Error}
	 */
	_SGOManager.prototype.addSGO = function ( object ) {
		// parameter checks:
		isObject( object, true );

		object.viewer = this._viewer;
		_sceneGraphObjects.push( object );

		if ( object.front ) {
			_postSceneSGOs.push( object );
		}
		else if ( object.back ) {
			_preSceneSGOs.push( object );
		}
		else {

			_inSceneSGOs.push( object );
			let psi = this._viewer.getProductStructureInfo();
			if ( !psi.ERROR && psi.children && psi.children.length !== 0 ) {
				this._viewer[ _ViewerFriend._updateModelBoundingBox ]();
			}
			else if ( ( isObject( object.sgoBBox ) && object.sgoBBox.min && object.sgoBBox.max ) ||
				( isBoolean( object.updateBBox ) && object.updateBBox ) ) { // updateBBox will be removed after the 1.7.0 release
				this._viewer.fitToVisible();
			}
		}

		this._viewer[ _ViewerFriend._render ]();
	};


	/**
	 * Removes a previously added scene graph object from the view
	 *
	 * @function removeSGO
	 * @memberof PLMVisWeb.SGOManager.prototype
	 *
	 * @param {Object} object - Three.js-constructed Mesh, Group, or Object3D previously added via the 'addSGO' API
	 * @throws {Error}
	 */
	_SGOManager.prototype.removeSGO = function ( object ) {
		// parameter checks:
		isObject( object, true );

		var index = _sceneGraphObjects.indexOf( object );
		if ( index !== -1 ) {
			_sceneGraphObjects.splice( index, 1 );
		}

		if ( ( index = _postSceneSGOs.indexOf( object ) ) !== -1 ) {
			_postSceneSGOs.splice( index, 1 );
		}

		else if ( ( index = _preSceneSGOs.indexOf( object ) ) !== -1 ) {
			_preSceneSGOs.splice( index, 1 );
		}

		else if ( ( index = _inSceneSGOs.indexOf( object ) ) !== -1 ) {
			_inSceneSGOs.splice( index, 1 );
		}

		if ( isFunction( object.removeEvents ) ) {
			object.removeEvents();
		}

		object.viewer = null;
		delete object.original;

		this._viewer[ _ViewerFriend._render ]();
	};


	/**
	 * Get all SGOs currently in the viewer.
	 *
	 * @function getSGOs
	 * @memberof PLMVisWeb.SGOManager.prototype
	 *
	 * @returns {Object[]} - An array of the SGO objects currenlty in the viewer.
	 */
	_SGOManager.prototype.getSGOs = function () {
		//	debug( "getSGOs", StringTable.stringsByLabel.VIEWER_LOGGER_getSGOs );

		var SGOs = [];

		for ( var index = 0; index < _sceneGraphObjects.length; index++ ) {
			SGOs.push( _sceneGraphObjects[ index ] );
		}

		return SGOs;
	};


	/**
	 * Reorders the SGO rendering in the viewer between top, behind or in the scene geometry.  This method is not exposed since
	 * the SGO maintains the render position.  The SGO informs the viewer of a change in the order.
	 *
	 * @ignore
	 * @function renderOrderSGO
	 * @memberof PLMVisWeb.SGOManager.prototype
	 *
	 * @param {Object} object - Three.js-constructed Mesh, Group, or Object3D
	 * @throws {Error}
	 */
	_SGOManager.prototype.renderOrderSGO = function ( object ) {
		// parameter checks:
		isObject( object, true );

		//	debug( StringTable.stringsByLabel.VIEWER_LOGGER_renderOrderSGO );

		var index;
		if ( ( index = _postSceneSGOs.indexOf( object ) ) !== -1 ) {
			_postSceneSGOs.splice( index, 1 );
		}
		else if ( ( index = _preSceneSGOs.indexOf( object ) ) !== -1 ) {
			_preSceneSGOs.splice( index, 1 );
		}
		else if ( ( index = _inSceneSGOs.indexOf( object ) ) !== -1 ) {
			_inSceneSGOs.splice( index, 1 );
		}

		if ( object.front ) {
			_postSceneSGOs.push( object );
		}
		else if ( object.back) {
			_preSceneSGOs.push( object );
		}
		else {
			_inSceneSGOs.push( object );
		}

		this._viewer[ _ViewerFriend._render ]();
	};


	Object.defineProperties( _SGOManager.prototype, {
		[ Friend._ignoreSGO ]: {
			get: function () {
				return _ignoreSGO;
			},
			set: function ( ignoreSGO ) {
				if ( ignoreSGO !== undefined && ignoreSGO !== _ignoreSGO ) {
					_ignoreSGO = ignoreSGO;
				}
				_ignoreSGO = ignoreSGO;
			}
		}
	} );

	function resizeHandler ( resizeEvent ) {
		if ( !_viewerReady && resizeEvent.width !== 0 && resizeEvent.height !== 0 ) {
			_viewerReady = true;
			this._viewer.fitToVisible();
		}
	}

	return new _SGOManager( params );
};

SGOManager.prototype = {
	constructor: SGOManager,
	_name: "SGOManager"
};

/**
 * Enum for SGO render times
 * @readonly
 * @memberof PLMVisWeb.SGOManager
 * @enum {String}
 */
SGOManager.RenderTime = {

	PreScene: "pre",

	InScene: "in",

	PostScene: "post"
};
export { SGOManager, Friend };
