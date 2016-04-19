/* global Symbol */

'use strict';

function WebChatRemoteParties ($rootScope, $sce, SignalingService, UserID, LocalWebcamService) {
    this.$receivedUsers = false;
    this.length = 0;
    this.$numberOfOngoingCalls = 0;
    this.$rooms = null;

    this.$init = function (rid) {
        SignalingService.init(rid);
        SignalingService.listener = this;
    };

    SignalingService.oncall = function (message) {
        var remoteParties = this.listener;
        var user = remoteParties.$userWithUID(message.callerID);
        if (user !== null) {
            user.call(true, new RTCSessionDescription(message.sdp));
        }
    };

    SignalingService.onhangup = function (message) {
        var remoteParties = this.listener;
        var user = remoteParties.$userWithUID(message.callerID);
        if (user !== null) {
            user.hangup();
        }
    };
    

    //TODO: rework this ...
    SignalingService.oninfo = function (message) {
        var users = [];
        message.users.forEach(function (user) {
            users.push(angular.fromJson(user));
        });

        this.listener.$add(users, false);
        this.listener.$remove(message);
    };

    SignalingService.onuserjoined = function (message) {
        this.listener.$add(message, true);
    };

    SignalingService.onuserleft = function (message) {
        this.listener.$remove(message, true);
    };

    //There must be a better way...
    this.$updateView = function () {
        $rootScope.$apply();
    };

    this.$_clearRooms = function () {
        this.$rooms = null;
    };

    this.$$_addOne = function (user) {
//        if (user.UID === UserID.get())
//            return;
        if (!this.$contains(user)) {
            user['videoFeed'] = null;
            //We should create a video element on the fly with its own directive...
            //user['videoElement'] = document.querySelector("#remotewebcamfeed");
            user['videoSource'] = '';
            user['peerConnection'] = null;
            //user['signaling'] = this.$signaling;
            user['updateView'] = this.$updateView;
            //user['parentCollection'] = this;
            user['callButtonText'] = "Call";


            user['callSetupEnum'] = {
                NONE: 0,
                OUTGOING_PENDING: 1,
                INCOMING_PENGING: 2,
                ONGOING: 3,
                HANGING_UP: 4
            };

            user['callSetup'] = user.callSetupEnum.NONE;

            user['call'] = function (incoming, remoteDescription) {
                this.peerConnection = new RTCPeerConnection(null);
                this.peerConnection['user'] = this;
                this['remoteDescription'] = remoteDescription;

                this.callSetup = incoming ? this.callSetupEnum.INCOMING_PENGING
                        : this.callSetupEnum.OUTGOING_PENDING;

                // An ICE Candidate was produced : send it to remote peer
                this.peerConnection.onicecandidate = function (event) {
                    var c = event['candidate'];
                    if (c !== null) {
                        console.log("event['candidate'] : ", c['candidate']);
                        var candidate = {
                            candidate: c['candidate'],
                            sdpMLineIndex: c['sdpMLineIndex'],
                            sdpMid: c['sdpMid']
                        };
                        //console.log("Get Own Properties Names : ", event.getOwnPropertyNames());
                        SignalingService.sendICECandidates(candidate, user.UID);
                    }
                };

                // Reflect the changes when the ICE connection state changes
                // NOTE: This should find its place in a View Controller
                this.peerConnection.oniceconnectionstatechange = function (event) {
                    if (this.iceConnectionState === 'completed'
                            || this.iceConnectionState === 'connected'
                            || this.iceConnectionState === 'new') {
                        this.user.callButtonText = "Hang Up";

                        console.log('Connection completed');
                        this.user.updateView();
                    }
                    else if (this.iceConnectionState === 'failed') {
                        this.user.callButtonText = "Error :(";
                        console.log('Connection Failed');
                        this.user.updateView();
                        this.user.hangup();
                    }
                    else if (this.iceConnectionState
                            === 'checking'
                            || this.iceConnectionState === 'new'
                            || this.iceConnectionState === 'disconnected') {
                        this.user.callButtonText = "...";
                        console.log('Connecting');
                        this.user.updateView();
                    }
                    else if (this.iceConnectionState
                            === 'closed') {
                        this.user.callButtonText = "Call";
                        console.log('Disconnected');
                        this.user.updateView();
                        //this.user.hangup();
                    }
                    $rootScope.$apply();

                };

                //TODO: Add support for mesh communication
                //NOTE: This should find its place in a View Controller
                this.peerConnection.onaddstream = function (event) {
                    //document.querySelector("#remotewebcamfeed").src = window.URL.createObjectURL(event.stream);

                    this.user.videoFeed = event.stream;
                    this.user.videoSource = $sce.trustAsResourceUrl(window.URL.createObjectURL(
                            event.stream));

                    if (typeof attachMediaStream !== 'undefined') {
                        var videoElement = document.querySelector("#video_" + this.user.UID);
                        attachMediaStream(videoElement, this.user.videoFeed);
                        LocalWebcamService.gotLocalVideo();
                    }

                    WebChatRemoteParties.$numberOfOngoingCalls++;
                    console.log("New call ongoing : ",
                            WebChatRemoteParties.$numberOfOngoingCalls);
                };

                // We have local Webcam
                this.peerConnection.addStream($rootScope.stream);

                var user = this;
                user.peerConnection.setRemoteDescription(user.remoteDescription,
                        function () {
                            user.peerConnection.createAnswer(function (answer) {
                                user.peerConnection.setLocalDescription(answer,
                                        function () {
                                            var desc = {
                                                type: answer['type'],
                                                sdp: answer['sdp']
                                            };
                                            SignalingService.sendAnswer(desc,
                                                    user.UID);
                                        }, window.logError);
                            }, window.logError);
                        }, window.logError);


            };

            user['hangup'] = function () {
                console.log("hangup : ", this.UID);

                //NOTE: This and the following if statement should find their place in a View Controller
                //document.querySelector("#remotewebcamfeed").pause();

                if (this.videoFeed !== null) {
                    var videoTracks = this.videoFeed.getVideoTracks();
                    for (var i in videoTracks) {
                        var track = videoTracks[i];
                        console.log('removing video track : ', track.id);
                        track.stop();
                        this.videoFeed.removeTrack(track);
                    }

                    var audioTracks = this.videoFeed.getAudioTracks();
                    for (var i in audioTracks) {
                        var track = audioTracks[i];
                        console.log('removing audio track : ', track.id);
                        track.stop();
                        this.videoFeed.removeTrack(track);
                    }
                }

                this.videoFeed = null;
                this.videoSource = '';
                //this.videoElement.src = "";

                //This shouldn't be moved
                if (this.peerConnection !== null) {
                    if (this.peerConnection.iceConnectionState !== 'closed'
                            || this.peerConnection.iceConnectionState !== 'disconnected') {
                        console.log('closin peerConnection for user :', user.UID);
                        this.peerConnection.close();
                        this.peerConnection = null;

                        SignalingService.sendHangUp({}, this.UID);
                    }
                }

                WebChatRemoteParties.$numberOfOngoingCalls--;

                if (WebChatRemoteParties.$numberOfOngoingCalls <= 0) {
                    LocalWebcamService.stop();
                }


            };


            user['action'] = function () {
                if (this.peerConnection !== null) {
                    this.hangup();
                }
                else {
                    this.call(false, null);
                }
            };

            this[this.length++] = user;
        }
    };

    //Collection management Functions
    this.$$_addMany = function (users) {
        for (var i = users.length - 1; i >= 0; i--) {
            this.$$_addOne(users[i]);
        }
    };

    this.$add = function (that, callApply) {
        if (typeof that.length === 'undefined') {
            this.$$_addOne(that);
        }
        else {
            this.$$_addMany(that);
        }

        if (callApply === true)
            this.$updateView();
    };

    this.$$_removeOne = function (that) {
        delete this[this.$indexOf(that)];
        this.$$_updateIndices();
    };

    this.$$_removeMany = function (that) {
        for (var i = that.length - 1; i >= 0; i++) {
            this.$_removeOne(that[i]);
        }
        this.$$_updateIndices();
    };

    this.$$_updateIndices = function () {
        var newLength = this.length;
        var indexCorrection = 0;
        for (var i = 0; i < this.length; i++) {
            if (typeof this[i] === 'undefined') {
                indexCorrection++;
                newLength--;
            }
            else if (indexCorrection
                    > 0) {
                this[i - indexCorrection] = this[i];
            }
        }

        for (var i = newLength; i < this.length; i++) {
            delete this[i];
        }

        this.length = newLength;
    };

    this.$remove = function (that, callApply) {
        if (typeof that.length === 'undefined') {
            this.$$_removeOne(that);
        }
        else {
            this.$$_removeMany(that);
        }
        if (callApply === true)
            this.$updateView();
    };

    this[Symbol.iterator] = function () {
        var index = 0;
        return {
            next: function () {
                var value = this[index];
                var done = index >= this.length;
                index++;
                return {value: value, done: done};
            }
        };
    };

    this.$userWithSID = function (sid) {
        for (var i = this.length - 1; i >= 0; i--) {
            var candidate = this[i];
            if (candidate.sid === sid)
                return candidate;
        }
        return null;
    };

    this.$indexOf = function (remoteParty) {
        for (var i = this.length - 1; i >= 0; i--) {
            if (remoteParty.sid === this[i].sid) {
                return i;
            }
        }
        return -1;
    };

    this.$contains = function (remoteParty) {
        if (this.$indexOf(remoteParty) < 0) {
            return false;
        }
        else {
            return true;
        }
    };

}