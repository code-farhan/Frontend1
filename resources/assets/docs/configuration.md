CircleCI automatically infers your settings from your code, so CircleCI's normal processing works just fine in most circumstances.
When it doesn't, the `circle.yml` file makes it easy to tell CircleCI what you need.
This is a simple YAML file where you spell out any tweaks required for your web app.
You place the file in your git repo's root directory and CircleCI reads the file each time it runs a build.

If you want a quick look at how to set up your `circle.yml`
file, check out our [sample file](/docs/config-sample).

Should you have a test failure, our [troubleshooting section](/docs/troubleshooting)
can likely tell you the best way to solve the problem.
If you find yourself repeateadly consulting this guide, please
[contact us](mailto:sayhi@circleci.com) and let us know what you're working on.
We'll try to make it easier for you.


## File structure and content

The `circle.yml` file is made up of six primary sections.
Each section represents a _phase_ of running your tests:

*   **machine**: adjusting the VM to your preferences and requirements
*   **checkout**: checking out and cloning your git repo
*   **dependencies**: setting up your project's language-specific dependencies
*   **database**: preparing the databases for your tests
*   **test**: running your tests
*   **deployment**: deploying your code to your web servers

The `circle.yml`
file contains another **general** section for general build-related configurations
that are not related to a specific phase.

**Remember**: most projects won't need to specify anything for many of the phases.

The sections contain lists of bash commands.  If you don't specify
commands, CircleCI infers them from your code.  Commands are run in
the order they appear in the file; all test commands are run to
completion, but a non-zero exit code during setup will cause the
build to fail early.  You can modify which&mdash;and
when&mdash;commands are run by adding `override`,
`pre` and/or `post` to adjust CircleCI's
inferred commands.  Here's how it works:

*   **pre**: commands run before CircleCI's inferred commands
*   **override**: commands run instead of CircleCI's inferred commands
*   **post**:  commands run after CircleCI's inferred commands

Each command is run in a separate shell.
As such, they do not share an environment with their predecessors, so be aware that
`export foo=bar` in particular does not work.
If you'd like to set an environment variable globally, you can specify them in the
[Machine configuration](#machine) section, described below.

#### Modifiers

You can tweak individual commands by adding a modifier.
Allowed modifiers are:

*   **timeout**: if a command runs this many seconds without output, kill it (default:180s)
*   **pwd**: run commands using this value as the current working directory (default: the checkout directory named for your project, except in the `machine` and `checkout/pre` sections, where it defaults to `$HOME`.)
*   **environment**: a hash creating a list of environment variables set for this command
    (see [Machine configuration](#machine) for this modifier's properties when used in the `machine` section of the file)
*   **parallel**: (only used with commands in the `test` section)
    if you have [ manually set up parallelism](/docs/parallel-manual-setup), set this to true to run a command across all VMs
*   **files**:
    The files identified by the file list (or globs) will be appended to the
    command arguments. The files will be distributed across all containers
    running the build. Check
    [manual parallelism setup document](/docs/parallel-manual-setup#auto-splitting) for more details.
*   **background**: when "true", runs a command in the background.  It is similar to ending a shell command with '&amp;', but works correctly over ssh.  Useful for starting servers, which your tests will connect to.

Note that YAML is very strict about indentation each time you add a new property.
For that reason, modifiers must be indented one level from their command.
In the following example, we treat the `bundle install`
command as a key, with `timeout`, `environment`, and `pwd` as the command's hash values.

```
dependencies:
  override:
    - bundle install:
        timeout: 240
        environment:
          foo: bar
          foo2: bar2
        pwd:
          test_dir
```

## Machine configuration

The `machine` section enables you to configure the virtual machine that runs your tests.

Here's an illustration of the types of things you might typically set in the
`machine` section of the file.

```
machine:
  timezone:
    America/Los_Angeles
  ruby:
    version: 1.9.3-p0-falcon

test:
  post:
    - bundle exec rake custom:test:suite
```

This example sets the [time zone](#timezone),
chooses a [Ruby version](#ruby-version)
and patchset, and adds a custom test command
to run after the rest of your commands.

Although `pre` and `post` are supported in the `machine`
section, `override` is not.
Here's how you might adjust the `circle.yml` file using
`pre` to install a different version of `phantomjs` than the version CircleCI has installed.

```
machine:
  pre:
    - curl -k -L -o phantomjs.tar.bz2 http://phantomjs.googlecode.com/files/phantomjs-1.8.2-linux-x86_64.tar.bz2
    - tar -jxf phantomjs.tar.bz2
```

### Environment

You set environment variables for **all commands** in the build by adding
`environment` to the `machine` section.
Remember that CircleCI uses a new shell for every command; as previously mentioned
`export foo=bar` won't work. Instead, you must include something like this.

```
machine:
  environment:
    foo: bar
    baz: 123
```

If you don't want to use this method, there are
[a number of other options](/docs/environment-variables).

### Timezone

The machine's time zone is UTC by default.
You use `timezone`
to adjust to the same time zone as your _production_ server.
Changing the time to your _development_ machine's time zone is **asking for trouble**.

This modifier tells CircleCI to
overwrite `/etc/timezone`
and then restart all databases and services that rely on it.
This modifier supports any time zone listed in the IANA time zone database.
You can find this by looking in `/usr/share/zoneinfo/`
on your Unix machine or in the **TZ** column in
[Wikipedia's list of TZ database time zones](http://en.wikipedia.org/wiki/List_of_tz_database_time_zones).

Be aware that some developers, especially those that collaborate across different time zones, do use UTC on their production servers.
This alternative can avoid horrific Daylight Saving Time (DST) bugs.

### Hosts

Sometimes you might need to add one or more entries to the
`/etc/hosts` file to assign various domain names to an IP address.
This example points to the development subdomain at the circleci hostname and IP address.

```
machine:
  hosts:
    dev.circleci.com: 127.0.0.1
    foobar: 1.2.3.4
```

### Ruby version

CircleCI uses [RVM](https://rvm.io/) to manage Ruby versions.
We use the Ruby version you specify in your `.rvmrc`, your
`.ruby-version` file, or your Gemfile.
If you don't have one of these files, we'll use Ruby `{{ versions.default_ruby }}`
or `{{ versions.old_ruby }}`, whichever we think is better.
If you use a different Ruby version let CircleCI know by including that information in the
`machine` section. Here's an example of how you do that.

```
machine:
  ruby:
    version: 1.9.3-p0-falcon
```

The complete list of supported Ruby versions is found [here](/docs/environment#ruby).

### Node.js version

CircleCI uses [NVM](https://github.com/creationix/nvm)
to manage Node versions. See
[supported Node versions](/docs/environment#nodejs)
for a complete list. If you do not specify a version, CircleCI uses
`{{ versions.default_node }}`.
Note that recent versions of NVM support selecting versions through
package.json.
If your version of NVM supports this, we recommend you use it.

Here's an example of how to set the version of Node.js to be used for your tests.

```
machine:
  node:
    version: 0.6.18
```

### Java version

Here's an example of how to set the version of Java to be used for your tests.

```
machine:
  java:
    version: openjdk7
```

The default version of Java is `oraclejdk7`.
See [supported Java versions](/docs/environment#java)
for a complete list.

### PHP version

CircleCI uses [php-build](https://github.com/CHH/php-build)
and [phpenv](https://github.com/CHH/phpenv)
to manage PHP versions.
Here's an example of how to set the version of PHP used for your tests.

```
machine:
  php:
    version: 5.4.5
```

See [supported PHP versions](/docs/environment#php) for a complete list.

### Python version

CircleCI uses [pyenv](https://github.com/yyuu/pyenv)
to manage Python versions.
Here's an example of how to set the version of Python used for your tests.

```
machine:
  python:
    version: 2.7.5
```

See [supported Python versions](/docs/environment#python)
for a complete list.

### GHC version

You can choose from a
[number of available GHC versions](/docs/configuration#Haskell)
in your `circle.yml`:

```
machine:
  ghc:
    version: 7.8.3
```

### Other languages

Our [test environment](/docs/environment) document has more configuration information about
[other languages](/docs/environment#other) including [Python](/docs/environment#python),
[Clojure](/docs/environment#clojure), [C/C++](/docs/environment#other),
[Golang](/docs/environment#other) and [Erlang](/docs/environment#other).

### Databases and other services

CircleCI supports a large number of [databases and other services](/docs/environment#databases).
Most popular ones are running by default on our build machines (bound to localhost), including Postgres, MySQL, Redis and MongoDB.

You can enable other databases and services from the `services` section:

```
machine:
  services:
    - cassandra
    - elasticsearch
    - rabbitmq-server
    - riak
    - beanstalkd
    - couchbase-server
    - neo4j
    - sphinxsearch

```

## Code checkout from GitHub

The `checkout` section is usually pretty vanilla, but we include examples of common things you might need to put in the section.
You can modify commands by including `override`, `pre`, and/or `post`.


####  Example: using git submodules

```
checkout:
  post:
    - git submodule sync
    - git submodule update --init
```

####  Example: overwriting configuration files on CircleCI


```
checkout:
  post:
    - mv config/.app.yml config/app.yml
```

## Project-specific dependencies

Most web programming languages and frameworks, including Ruby's bundler, npm for Node.js, and Python's pip, have some form of dependency specification;
CircleCI automatically runs commands to fetch such dependencies.

You can use `override`, `pre`, and/or `post` to modify `dependencies` commands.
Here are examples of common tweaks you might make in the `dependencies` section.

####  Example: using npm and Node.js

```
dependencies:
  override:
    - npm install --dev
```

####  Example: using a specific version of bundler

```
dependencies:
  pre:
    - gem uninstall bundler
    - gem install bundler --pre
```

### Bundler flags

If your project includes bundler (the dependency management program for Ruby), you can include
`without` to list dependency groups to be excluded from bundle install.
Here's an example of what that would look like.

```
dependencies:
  bundler:
    without: [production, osx]
```

### Custom Cache Directories

CircleCI caches dependencies between builds.
To include any custom directories in our caching, you can use
`cache_directories` to list any additional directories you'd like cached between builds.
Here's an example of how you could cache two custom directories.

```
dependencies:
  cache_directories:
    - "assets/cache"    # relative to the build directory
    - "~/assets/output" # relative to the user's home directory
```

Caches are private, and are not shared with other projects.

## Database setup

Your web framework typically includes commands to create your database, install your schema, and run your migrations.
You can use `override`, `pre`, and/or `post` to modify `database` commands.
See [Setting up your test database](/docs/manually#databases) for more information.

If our inferred `database.yml` isn't working for you, you may need to `override` our setup commands (as shown in the following example).
If that is the case, please [contact us](mailto:sayhi@circleci.com)
and let Circle know so that we can improve our inference.

```
database:
  override:
    - mv config/database.ci.yml config/database.yml
    - bundle exec rake db:create db:schema:load --trace
```

FYI, you have the option of pointing to the location of your stored database config file using the `environment` modifier in the
`machine` section.

```
machine:
  environment:
    DATABASE_URL: postgres://ubuntu:@127.0.0.1:5432/circle_test
```

## Running your tests

The most important part of testing is actually running the tests!

CircleCI supports the use of `override`, `pre`, and/or `post` in the `test` section.
However, this section has one minor difference: all test commands will run, even if one fails.
This allows our test output to tell you about all the tests that fail, not just the first error.

####  Example: running spinach after RSpec


```
test:
  post:
    - bundle exec rake spinach:
        environment:
          RAILS_ENV: test
```

####  Example: running phpunit on a special directory


```
test:
  override:
    - phpunit my/special/subdirectory/tests
```

CircleCI also supports the use of `minitest_globs`
(a list of file globs, using [Ruby's Dir.glob syntax](http://ruby-doc.org/core-2.0/Dir.html#glob-method))
that can list the file globs to be used during testing.

By default, when testing in parallel, CircleCI runs all tests in the test/unit, test/integration, and
test/functional directories. You can add `minitest_globs` to replace the
standard directories with your own.
This is needed only when you have additional or non-standard
test directories and you are testing in parallel with MiniTest.

####  Example: minitest_globs


```
test:
  minitest_globs:
    - test/integration/**/*.rb
    - test/extra-dir/**/*.rb
```

## Deployment

The `deployment`
section is optional. You can run commands to deploy to staging or production.
These commands are triggered only after a successful (green) build.

```
deployment:
  production:
    branch: production
    commands:
      - ./deploy_prod.sh
  staging:
    branch: master
    commands:
      - ./deploy_staging.sh
```

The `deployment`
section consists of multiple subsections. In the example shown above, there
are two&mdash;one named _production_ and one named _staging_.
Subsection names must be unique.
Each subsection can list multiple branches, but at least one of these fields must be
named _branch_. In instances of multiple branches, the first one that matches
the branch being built is the one that is run.
In the following example, if a developer pushes to any of the three branches listed, the script
`merge_to_master.sh` is run.

```
deployment:
  automerge:
    branch: [dev_alice, dev_bob, dev_carol]
    commands:
      - ./merge_to_master.sh
```

The _branch_ field can also specify regular expressions, surrounded with
`/` (e.g. `/feature_.*/`):

```
deployment:
  feature:
    branch: /feature_.*/
    commands:
      - ./deploy_feature.sh
```

You can also optionally specify a repository _owner_ in any deployment subsection.
This can be useful if you have multiple forks of the project, but only one should be
deployed. For example, a deployment subsection like this will only deploy if the project
belongs to "circleci", and other users can push to the master branch of their fork without
triggering a deployment:

```
deployment:
  master:
    branch: master
    owner: circleci
    commands:
      - ./deploy_master.sh
```

### SSH Keys

If deploying to your servers requires SSH access, you'll need to
upload the keys to CircleCI.
CircleCI's UI enables you to do this on your project's **Project Settings > SSH keys** page.
Add and then submit the one or more SSH keys needed
for deploying to your machines. If you leave the **Hostname** field blank,
the public key will be used for all hosts.

### Heroku

CircleCI also has first-class support for deploying to Heroku.
Specify the app you'd like to
`git push` to under `appname`.
Upon a successful build, we'll automatically deploy to the app in the section that matches the push, if there is one.

```
deployment:
  staging:
    branch: master
    heroku:
      appname: foo-bar-123
```

Setting up our deployment to Heroku requires one extra step.
Due to Heroku's architecture and security model, we need to deploy as a particular user.
A member of your project, possibly you, will need to register as that user.
CircleCI's UI enables you to do this on your project's **Project Settings > Heroku settings** page.

### Heroku with pre or post-deployment steps

If you want to deploy to Heroku and also run commands before or after the deploy, you must use the 'normal' deployment syntax.

```
deployment:
    production:
      branch: production
      commands:
        - git push git@heroku.com:foo-bar-123.git $CIRCLE_SHA1:master
        - heroku run rake db:migrate --app foo-bar-123
```

## Notifications

CircleCI sends personalized notifications by email.

In addition to these per-user emails, CircleCI sends notifications on a per-project basis.
CircleCI supports sending webhooks when your build completes.
CircleCI also supports HipChat, Campfire, Flowdock and IRC notifications; you configure these notifications from your project's
**Project Settings > Notifications ** page.

This example will POST a JSON packet to the specified URL.

```
notify:
  webhooks:
    # A list of hook hashes, containing the url field
    - url: https://example.com/hooks/circle
```

The JSON packet is identical to the result of the
[Build API](/docs/api#build)
call for the same build, except that it is wrapped in a "payload" key:

```
{
  "payload": {
    "vcs_url" : "https://github.com/circleci/mongofinil",
    "build_url" : "https://circleci.com/gh/circleci/mongofinil/22",
    "build_num" : 22,
    "branch" : "master",
    ...
  }
}

```

## Specifying branches to build

CircleCI by default tests every push to _any_ branch in the repository.
Testing all branches maintains quality in all branches and adds
confidence when the branches are to be merged with default branch.

You may, however, blacklist branches from being built in CircleCI.  This example
excludes `gh-pages` from being built in circle:

```
general:
  branches:
    ignore:
      - gh-pages # list of branches to ignore
      - /release\/.*/ # or ignore regexes
```

You may also whitelist branches, so only whitelisted branches will trigger a build.
This example limit builds in circle to `master` and `feature-.*` branches:

```
general:
  branches:
    only:
      - master # list of branches to build
      - /feature-.*/ # or regexes
```

We discourage branch whitelisting, it means work-in-progress
code can go a long time without being integrated and tested and we've found
it leads to problems when that untested code gets merged.

`circle.yml` is per-branch configuration file, and the branch ignore list in one branch will
only affect that branch and no other one.

## Specifying build directory

Circle runs all commands on the repository root, by default.  However, if
you store your application code in a subdirectory instead of the root, you
can specify the build directory in circle.yml.  For example, to set the build
directory to `api` sub-directory, you can use the following configuration:

```
general:
  build_dir: api
```

Circle will run its inference as well as all build commands from that directory.

## Specifying custom artifacts directories and files

You can specify extra directories and files to be
[saved as artifacts](/docs/build_artifacts):

```
general:
  artifacts:
    - "selenium/screenshots" # relative to the build directory
    - "~/simplecov" # relative to the user's home directory
    - "test.txt" # a single file, relative to the build directory
```

## Need anything else?

We are adding support for configuring every part of your build.
If you need to tweak something that isn't currently supported, please
[contact us](mailto:sayhi@circleci.com)
and we'll figure out how to make it happen.
