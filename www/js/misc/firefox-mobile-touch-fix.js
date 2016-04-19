var objects = [];
var scopes = [];
var callbacks = [];

function makeid() {
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < 24; i++ )
        text += possible.charAt(Math.floor(Math.random() * possible.length));

    return text;
}

function onTouch(event) {
	if (event.touches.length > 1 || (event.type === "touchend" && event.touches.length > 0))
	return;

	var newEvt = document.createEvent("MouseEvents");
	var type = null;
	var touch = null;
	switch (event.type) {
		case "touchstart": type = "mousedown"; touch = event.changedTouches[0];
		case "touchmove":  type = "mousemove"; touch = event.changedTouches[0];
		case "touchend":   type = "mouseup"; touch = event.changedTouches[0];
	}
	/*if (type === "mouseup") {
		if (event.originalTarget === $scope.loginButton) {
			callbacks[event.originalTarget.id].call(scopes[event.originalTarget.id]);
			//$scope.loggin();
		} else if (event.originalTarget === $scope.createButton) {
			$scope.createUser.call($scope);
			//$scope.createUser();
		}
	}*/

	newEvt.initMouseEvent(type, true, true, event.originalTarget.ownerDocument.defaultView, 0,
	touch.screenX, touch.screenY, touch.clientX, touch.clientY,
	event.ctrlKey, event.altKey, event.shirtKey, event.metaKey, 0, null);
	event.originalTarget.dispatchEvent(newEvt);
}

function addClickListener(scope, object, callback) {
	if (typeof object.id === 'undefined') {
		object.id = makeid;
	}
	objects[object.id] = object;
	callbacks[object.id] = callback;
	scope[object.id] = scope;

	object.addEventListener('touchend', onTouch, false);
	object.addEventListener("touchstart", onTouch, false);
	object.addEventListener("touchcancel", onTouch, false);
	object.addEventListener("touchleave", onTouch, false);
	object.addEventListener("touchmove", onTouch, false);
}