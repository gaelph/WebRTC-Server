/* global this */

function SignalingService(UserID) {

    this.socket;

    this.rid;

    this.init = function (rid) {

        this.rid = rid;

        //Initialize the socket
        this.socket = new WebSocket("ws://localhost:3200/ws/" + rid);
        this.socket['listener'] = this;

        this.socket['callbacks'] = {};

        this.socket['on'] = function (label, callback) {
            this['callbacks'][label] = callback;
        };

        this.socket['emit'] = function (label, message) {
            this.send(label + '##' +JSON.stringify(message));
        };

        this.socket.onopen = function () {
        };

        this.socket.onerror = function (error) {
            console.log("error in socket : ", error.currentTarget.url);
        };

        this.socket.onclose = function (event) {
            console.log("Socket closed : ", event.code, " ", event.reason, " ", event.wasClean);
        };

        this.socket.onmessage = function (message) {
            var label = message.data.substring(0, message.data.indexOf('##'));
            message = message.data.substring(message.data.indexOf('##') + 2);
            message = angular.fromJson(message);

            if (this.callbacks[label] !== 'undefined')
                this.callbacks[label].call(this, message);
        };

        // Icoming Communication
        this.socket.on('call', function (message) {
            message = angular.fromJson(message);
            this.listener.oncall(message);
        });

        this.socket.on('hangup', function (message) {
            message = angular.fromJson(message);
            this.listener.onhangup(message);
        });

        this.socket.on('answer', function (message) {
            message = angular.fromJson(message);
            this.listener.onanswer(message);
        });

        this.socket.on('icecandidate', function (message) {
            message = angular.fromJson(message);
            this.listener.onicecandidate(message);
        });

        this.socket.on('info', function (message) {
            message = angular.fromJson(message);
            this.listener.oninfo(message);
        });

        this.socket.on('user_joined', function (message) {
            message = angular.fromJson(message);
            this.listener.onuserjoined(message);
        });

        this.socket.on('user_left', function (message) {
            message = angular.fromJson(message);
            this.listener.onuserleft(message);
        });
    };

    //Out communication
    this.send = function (label, message, to) {
        this.socket.emit(label, JSON.stringify({
            sender: UserID.get(),
            target: this.rid,
            body: message
        }));
    };
    this.sendOffer = function (message, to) {
        this.send('offer', message, to);
    };
    this.sendHangUp = function (message, to) {
        this.send('hangup', message, to);
    };
    this.sendAnswer = function (message, to) {
        this.send('answer', message, to);
    };
    this.sendICECandidates = function (message, to) {
        this.send('icecandidate', message, to);
    };
    this.sendDisconnect = function () {
        this.socket.close();
        //this.send('disconnect', null, null);
    };

    //Interface functions
    this.oncall = function (message) {};
    this.onhangup = function (message) {};
    this.onanswer = function (message) {};
    this.onicecandidate = function (message) {};
    this.oninfo = function (message) {};
    this.onuserjoined = function (message) {};
    this.onuserleft = function (message) {};
}
