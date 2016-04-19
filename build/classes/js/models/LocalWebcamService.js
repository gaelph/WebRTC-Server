/* global logError */

'use strict';

function LocalWebcamService($rootScope) {
    $rootScope.stream;

    this.constraints = {
        video: {
            facingMode: 'user',
            optional: [
                {minFramerate: 25}, {maxFramerate: 30},
                {minWidth: 320}, {maxWidth: 1280},
                {minHeigth: 240}, {maxHeight: 800}
            ]},
        audio: true
    };

    this.callbacks = [];
    this.events = {
        STOP: 'stop',
        START: 'start',
        LOADEDMETADATA: 'loadedmetadata',
        STREAMCHANGED: 'streamchanged'
    };

    this.on = function (label, object, callback) {
        if (typeof this.callbacks[label] === 'undefined') {
            this.callbacks[label] = [];
        }
        this.callbacks[label].push({
            object: object,
            callback: callback
        });
    };

    this.do = function (label) {
        if (typeof this.callbacks[label] !== 'undefined') {
            for (var i = this.callbacks[label].length - 1; i >= 0; i--) {
                var object = this.callbacks[label][i].object;
                var callback = this.callbacks[label][i].callback;

                callback.call(object);
            }
        }
    };

    this.removeTracksFromStream = function (tracks) {
        for (var i in tracks) {
            var track = tracks[i];
            track.stop();
            $rootScope.stream.removeTrack(track);
        }
    };

    this.stop = function () {
        console.log('Stoping Webcam');

        if ($rootScope.stream !== null) {
            this.removeTracksFromStream($rootScope.stream.getVideoTracks());
            this.removeTracksFromStream($rootScope.stream.getAudioTracks());

            $rootScope.stream = null;
        }
    };

    this.start = function () {
        /*if (navigator.getUserMedia === navigator.mediaDevices.getUserMedia) {
            navigator.mediaDevices.getUserMedia(this.constraints).
                    then(this.setupLocalWebcam).
                    catch(logError);
        } else {*/
            navigator.getUserMedia(this.constraints, this.setupLocalWebcam, logError);
        //}
    };

    this.setupLocalWebcam = function (stream) {
        $rootScope.stream = stream;
        $rootScope.$apply();
    };

    this.gotLocalVideo = function () {
        this.do(this.events.LOADEDMETADATA);
    };

}