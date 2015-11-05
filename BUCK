include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'plugin-manager',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: plugin-manager',
    'Gerrit-ApiType: plugin',
    'Gerrit-HttpModule: com.googlesource.gerrit.plugins.manager.WebModule',
    'Implementation-Title: Plugin manager',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/plugin-manager',
  ],
  provided_deps = [
    '//lib:gson',
    '//lib/log:log4j'
  ],
)

