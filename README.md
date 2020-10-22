# Lazo
<img src="lasso.svg" alt="Catch them changes!" width="200px" align="right">

A clojure program to catch changes in a repository 

If you want to open source parts of your codebase (e.g. a folder) without splitting your code across several repositories, **Lazo** is what you need.


## Use cases

- Monorepo: You have several folders with projects and you want to open source some of them, but you like working in a monorepo because it's easier to reason about API interfaces.
- Ops: You have an application and some ops/infrastructure code, but you would like to open source just the app.

## How it works

Lazo has two concepts: a **Main** repo (e.g. a monorepo) and a **Module** repo.   A prerequisite is that the module repo will contain the code that is in a folder (any folder) in the main repo.

Lazo stores locally copies of your main repo(s) and module repo(s) and, whenever a `push` event from a GitHub webhook is detected, it does all the boring bookkeeping of copying the changes over to the appropriate module.

It retains commit authorship and commit messages (which are bundled up, in case the push is for more than one commit).

## Running Lazo

You can run Lazo either as an uberjar, or with the [official docker image](https://hub.docker.com/repository/docker/nextjournal/lazo).

In order for Lazo to know about your repositories and modules, you need to provide a EDN configuration file:

- Either you supply a `LAZO_CONFIG_FILE` environment variable with the path of the config file
- Otherwise it will assume that it's a `config.edn` at the same location of where you're running the jar, or in the root folder of the docker image.

### Example config

See [config.edn.template](config.edn.template) or:

```clojure
{:user      "Foo"
 :email     "foo@example.com"
 :token     "TOKEN"
 :local-dir ".lazo-repos"
 :repos     [{:organization       "test-org-integration"
              :main-repo          "main-repo"
              :main-branch        "master"
              :main-module-folder "my-module"
              :module-repo        "module-repo"
              :module-branch      "master"}
              {:organization       "test-org-integration"
               :main-repo          "main-repo"
               :main-branch        "master"
               :main-module-folder "my-module2"
               :module-repo        "module2"
               :module-branch      "main"}]}
```

- `:user` is the Github user you want to use for the commit
- `:email` is the github user email that you want to use for the commit
- `:token` is the personal developer token that you use for authentication
- `:local-dir` is the local folder you want Lazo to use for the local copies of the repositories

And then, for each pair main/module, a configuration block that contains:
- `:organization` the github organization
- `:main-repo` the name of the main repo under the org
- `:main-branch` the name of the main branch that you use in main repo. Lazo will only care about pushes to this branch, allowing you to use GitFlow or whatever branching strategy you prefer
- `:main-module-folder` this is the folder that you want to sync with the module repo
- `:module-repo` the name of the module repo
- `:module-branch` the name of the main branch of the module repo. Lazo will push changes to this branch.

## LICENSE

EPL 2.0, see [LICENSE](LICENSE.md)

## Acknowledgement

- This code is a reintepretation of my previous work, [flow-bot](https://github.com/WorksHub/flow-bot) which served a similar (but not the same) purpose.
- Icons made by [Freepik](https://www.flaticon.com/authors/freepik) from [Flaticon](https://www.flaticon.com/)
