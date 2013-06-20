/*global jQuery: true, angular: true */

'use strict';

$(document).foundation('tooltips');
var muroApp = angular.module('muroApp', []);
var undefined;

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

muroApp.filter('filterLateGuesses', function() {
  return function(items, hide) {
    if (hide) {
      var filtered = [];
      angular.forEach(items, function(guess) {
        if (guess.valid) {
          filtered.push(guess);
        }
      });
      return filtered;
    }
    return items;
  };
});

muroApp.controller("GuessCtrl", function($scope, $http) {

  var url = "/guesses";
  //var url = '/assets/mock/guesses.json';

  $http({method: 'GET', url: url}).
      success(function(guesses) {
        $scope.guesses = $.map(guesses, function (guess) {
          guess.count = guess.later.length + 1;
          guess.diff = function() {
            var correct = getCorrectGuess();
            if (correct) {
              return Math.abs(correct[0] - guess.guess.land) +
                     Math.abs(correct[1] - guess.guess.road) +
                     Math.abs(correct[2] - guess.guess.water);
            }
            return '';
          };
          guess.laterTooltip = $.map(guess.later, function(el) {
            return '<div>' + el.user + ' <small>(' + el.delay + ' myöhemmin)</small></div>';
          }).join('');
          return guess;
        });
        $scope.viewReady = true;
      }).
      error(function(data, status, headers, config) {
        alert ('OMG. VIRHE!');
      });


  $scope.sort = {
    column: 'time',
    reverse: true
  };

  $scope.changeUrl = function () {
    if (history && $.isFunction(history.replaceState)) {
      var correct = getCorrectGuess();
      if (correct) {
        try {
          history.replaceState({}, '', '/?' + correct[0] + '-' + correct[1] + '-' + correct[2]);
        } catch (e) {
          // just to be safe...
        }
      }
    }
  };

  function getCorrectGuess() {
    var correctLand = parseInt($scope.correctLand);
    var correctRoad = parseInt($scope.correctRoad);
    var correctWater = parseInt($scope.correctWater);
    if (correctLand >= 0 && correctRoad >= 0 && correctWater >= 0) {
      return [correctLand, correctRoad, correctWater];
    }
    return undefined;
  }

  initGuess();

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
      sort.reverse = (column === 'user' || column === 'diff()') ? false : true;
    }
  };

  function initGuess() {
    var matches = (/^\?(\d+)-(\d+)-(\d+)$/gi).exec(location.search);
    if (matches && matches.length === 4) {
      $scope.correctLand = matches[1];
      $scope.correctRoad = matches[2];
      $scope.correctWater = matches[3];
      $scope.sort.column = 'diff()';
      $scope.sort.reverse = false;
    }
  }

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
