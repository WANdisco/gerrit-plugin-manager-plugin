// Copyright (C) 2015 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

var app = angular.module('PluginManager', []).controller(
    'LoadInstalledPlugins',
    function($scope, $http) {
      var plugins = this;

      plugins.list = {};

      plugins.available = {};

      $scope.refreshInstalled = function() {
        $http.get('/plugins/?all', plugins.httpConfig).then(
            function successCallback(response) {

              angular.forEach(response.data, function(plugin) {
                plugins.list[plugin.id] = {
                    id: plugin.id,
                    index_url: plugin.index_url,
                    version: plugin.version,
                    sha1: '',
                    url: plugin.url,
                    update_version: ''
                }
              });

              $scope.refreshAvailable();
            }, function errorCallback(response) {
            });
      }

      $scope.refreshAvailable = function() {
        $http.get('/plugins/plugin-manager/available', plugins.httpConfig)
            .then(function successCallback(response) {

              angular.forEach(response.data, function(plugin) {
                var currPlugin = plugins.list[plugin.id];

                if(currPlugin === undefined) {
                  currPlugin = {
                      id: plugin.id,
                      index_url: '',
                      version: ''
                  }
                }

                if(plugin.version != currPlugin.version) {
                  currPlugin.update_version = plugin.version;
                }
                currPlugin.sha1 = plugin.sha1;
                currPlugin.url = plugin.url;

                plugins.list[plugin.id] = currPlugin;
              });
              plugins.available = response.data;
            }, function errorCallback(response) {
            });
      }

      $scope.install = function(id, url) {
        var pluginInstallData = { "url" : url };
        $("button#" + id).addClass("hidden");
        $("span#installing-" + id).removeClass("hidden");
        $http.put('/a/plugins/' + id + ".jar", pluginInstallData).then(
            function successCallback(response) {
              $("span#installing-" + id).addClass("hidden");
              $("span#installed-" + id).removeClass("hidden");
              $scope.refreshInstalled();
            }, function errorCallback(response) {
              $("span#installing-" + id).addClass("hidden");
              $("span#failed-" + id).removeClass("hidden");
            });
      }

      $scope.refreshInstalled();
    });

app.config(function($httpProvider) {
  $httpProvider.defaults.headers.common = {
    'X-Gerrit-Auth' : '@X-Gerrit-Auth'
  };
});