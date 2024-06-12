We export a number of environment variables during each build, which you may find
useful for more complex testing or deployment.

### Basics

Ideally, you won't have code which behaves differently in CI. But for the cases
when it's necessary, we set two environment variables which you can test:

<dl>
  <dt>
    `CIRCLECI`
  </dt>
  <dd>
    true
  </dd>
  <dt>
    `CI`
  </dt>
  <dd>
    true
  </dd>
</dl>

### Build Details

We publish the details of the currently running build in these variables:

<dl>
  <dt>
    `CIRCLE_PROJECT_USERNAME`
  </dt>
  <dd>
    The username or organization name of the project being tested, i.e. "foo" in circleci.com/gh/foo/bar/123
  </dd>
  <dt>
    `CIRCLE_PROJECT_REPONAME`
  </dt>
  <dd>
    The repository name of the project being tested, i.e. "bar" in circleci.com/gh/foo/bar/123
  </dd>
  <dt>
    `CIRCLE_BRANCH`
  </dt>
  <dd>
    The name of the branch being tested, e.g. 'master'.
  </dd>
  <dt>
    `CIRCLE_SHA1`
  </dt>
  <dd>
    The SHA1 of the commit being tested.
  </dd>
  <dt>
    `CIRCLE_COMPARE_URL`
  </dt>
  <dd>
    A link to GitHub's comparison view for this push. Not present for builds that are triggered by GitHub pushes.
  </dd>
  <dt>
    `CIRCLE_BUILD_NUM`
  </dt>
  <dd>
    The build number, same as in circleci.com/gh/foo/bar/123
  </dd>
  <dt>
    `CIRCLE_ARTIFACTS`
  </dt>
  <dd>
    The directory whose contents are automatically saved as [build artifacts](/docs/build-artifacts).
  </dd>
</dl>

### Parallelism

These variables are available for [manually setting up parallelism](/docs/parallel-manual-setup):

<dl>
  <dt>
    `CIRCLE_NODE_TOTAL`
  </dt>
  <dd>
    The total number of nodes across which the current test is running.
  </dd>
  <dt>
    `CIRCLE_NODE_INDEX`
  </dt>
  <dd>
    The index (0-based) of the current node.
  </dd>
</dl>

### Other

There are quite a few other environment variables set. Here are some of
the ones you might be looking for:

<dl>
  <dt>
    `HOME`
  </dt>
  <dd>
    /home/ubuntu
  </dd>
  <dt>
    `DISPLAY`
  </dt>
  <dd>
    :99
  </dd>
  <dt>
    `LANG`
  </dt>
  <dd>
    en_US.UTF-8
  </dd>
  <dt>
    `PATH`
  </dt>
  <dd>
    Includes /home/ubuntu/bin
  </dd>
</dl>

### Set your own!

You can of course set your own environment variables, too!
The only gotcha is that each command runs in its own shell, so just adding an
`export FOO=bar` command won't work.

#### Setting environment variables for all commands using circle.yml

You can set environment variables in your `circle.yml` file, that
[will be set for every command](/docs/configuration#environment).

#### Setting environment variables for all commands without adding them to git

Occasionally, you'll need to add an API key or some other secret as
an environment variable.  You might not want to add the value to your
git history.  Instead, you can add environment variables using the
**Project settings &gt; Environment Variables** page of your project.

All commands and data on CircleCI's VMs can be accessed by any of your colleagues&mdash;we run your arbitrary code, so it is not possible to secure.
Take this into account before adding important credentials that some colleagues do not have access to.

#### Per-command environment variables

You can set environment variables per-command as well.
You can use standard bash syntax in your commands:

```
RAILS_ENV=test bundle exec rake test
```

You can also use [the environment modifier](/docs/configuration#modifiers) in your
`circle.yml` file.
