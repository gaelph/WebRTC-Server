/* global LocalMediaStream, attachMediaStream */

'use strict';

function lwVideo($rootScope, LocalWebcamService) {
    return {
        //require : '^?LocalWebcamController',
        transclude: true,
        link: function (scope, element, attrs) {
            $rootScope.$watch('stream', function (newVal, oldVal, scope) {
                if (typeof newVal !== 'undefined') {
                    if (typeof attachMediaStream !== 'undefined') {
                        attachMediaStream(element[0], newVal);
                        LocalWebcamService.gotLocalVideo();
                    } else {
                        element.attr('src', window.URL.createObjectURL(newVal));
                        element.prop('muted', true);
                    }
                } else {

                    element.attr('src', '');
                }
            });

            element.on('loadedmetadata', function (event) {
                LocalWebcamService.gotLocalVideo();
            });
        }
    };
}