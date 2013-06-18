/*global jQuery: true, angular: true */

'use strict';

$(document).foundation('tooltips');
var muroApp = angular.module('muroApp', []);

muroApp.directive('formattedTime', function() {
  return {
    scope: {
      formattedTime: "="
    },
    template: '<span title="{{formatted}}" data-tooltip>{{ago}}</span>',
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

$(function() {
  //$('body')

});

muroApp.controller("GuessCtrl", function($scope, $http) {

  $http({method: 'GET', url: '/guesses'}).
      success(function(guesses) {
        $scope.guesses = $.map(guesses, function (guess) {
          console.log(guess.later);
          guess.count = guess.later.length + 1;
          console.log(guess.count);
          return guess;
        });
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
      sort.reverse = (column === 'user') ? false : true;
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
