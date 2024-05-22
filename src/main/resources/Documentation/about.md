This plugin adds support for discovering and installing other plugins
to Gerrit.

The list of plugins are taken from the following sources:

- Internal core plugins contained in the gerrit.war
- Plugins built on the [Gerrit CI][1] or another configurable location
  for the stable branch that Gerrit is built

Replication Notes:

- Plugins that have been modified for Cirata replication will not be
  updated or modified by plugin-manager. This will be made visible
  in the user interface.

**NOTE**: Management of plugins is restricted to Gerrit Administrators.

[1]: https://gerrit-ci.gerritforge.com
