# Lazo

A clojure program to catch changes in a monorepo.


If you are working in a monorepo, but you want to open-source parts of your codebase, without splitting up your code 
base in several repositories, **Lazo** is what you need.

You configure Lazo with the following information:

```clojure
 {:organization        "test-org-integration"
  :main-repo           "main-repo"
  :main-branch         "master"
  :main-module-folder "my-module2"
  :module-repo         "module2"
  :module-branch       "master"}
``` 

And, after setting up a webhook in your monorepo, Lazo will take care of detecting changes to the module in the folder
that you specify, and push them upstream in your module repository.

## LICENSE

EPL 2.0, see [LICENSE](LICENSE.md)

## Acknowledgement

This code is a reintepretation of my previous work, [flow-bot](https://github.com/WorksHub/flow-bot) which served a
similar (but not the same) purpose.

