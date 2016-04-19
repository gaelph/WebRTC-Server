'use strict';

function UserIDService($cookies) {

    this.set = function (newValue) {

        var expireDate = new Date();
        expireDate.setFullYear(10 + expireDate.getFullYear());


        $cookies.put('token', newValue, {
            path: '/',
            domain: 'localhost',
            expires: expireDate
        });
        $cookies.put('token', newValue, {
            path: '/',
            domain: '192.168.1.29',
            expires: expireDate
        });
        $cookies.put('token', newValue, {
            path: '/',
            domain: 'gaelph.bounceme.net',
            expires: expireDate
        });
        $cookies.put('token', newValue, {
            path: '/',
            domain: '176.189.105.192',
            expires: expireDate
        });
    };

    this.get = function () {
        var value = $cookies.get('token');

        if (typeof value !== 'undefined')
            return value;
        else
            return '';
    };


}