angular.module('museWebInterfaceApp').config(['$locationProvider', '$routeProvider',
    function config($locationProvider, $routeProvider) {
        $locationProvider.hashPrefix('!');

        $routeProvider.when('/about', {
            template: '<about></about>'
        }).when('/choose-music', {
            template: '<choose-music></choose-music>'
        }).when('/login', {
            template: '<login></login>'
        }).otherwise('/login');


        // use the HTML5 History API
        $locationProvider.html5Mode(true);
    }

]);