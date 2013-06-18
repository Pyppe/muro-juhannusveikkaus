/*global jQuery: true, angular: true */

'use strict';


var muroApp = angular.module('muroApp', []);

muroApp.directive('formattedTime', function() {
  return {
    scope: {
      formattedTime: "="
    },
    template: '<span title="{{formatted}}">{{ago}}</span>',
    link: function (scope, el, attrs) {
      var timeStr = scope.formattedTime
      var t = makeTime(timeStr);
      scope.ago = t.ago;
      scope.formatted = t.formatted;
      scope.timestamp = t.timestamp;
      scope.time = timeStr;
    }
  };
});

muroApp.controller("GuessCtrl", function($scope, $http) {

  $http({method: 'GET', url: '/guesses'}).
      success(function(guesses) {
        $scope.guesses = guesses;
      }).
      error(function(data, status, headers, config) {
        alert ('OMG. VIRHE!');
      });


  $scope.sort = {
    column: 'time',
    reverse: true
  };

  $scope.selectedClass = function(column) {
    var cls = "table-sort"
    if (column == $scope.sort.column) {
      cls += $scope.sort.reverse ? '-up' : '-down';
    }
    return cls;
  };

  $scope.changeSorting = function(column) {
    var sort = $scope.sort;
    if (sort.column == column) {
      sort.reverse = !sort.reverse;
    } else {
      sort.column = column;
      sort.reverse = (column === 'time') ? true : false;
    }
  };

});

function makeTime(time) {
  var timeMoment = moment(time, "YYYY-MM-DD'T'HH:mmZ");
  return {
    ago: timeMoment.fromNow(),
    formatted: timeMoment.format("ddd [klo] HH:mm, MMMM Do"),
    timestamp: timeMoment.format("X"),
    time: time
  };
}







/*.
    config(['$routeProvider', function($routeProvider) {
      $routeProvider.
          when('/phones', {templateUrl: 'partials/phone-list.html',   controller: PhoneListCtrl}).
          when('/phones/:phoneId', {templateUrl: 'partials/phone-detail.html', controller: PhoneDetailCtrl}).
          otherwise({redirectTo: '/phones'});
    }]);*/