The CircleCI environment provides the libraries, languages, and databases needed for most development work
(check out the [comprehensive list)](/docs/environment).
However, if you need to install a particular version of software&mdash;to match your production systems or to test backward compatibility, for example&mdash;or add custom code, CircleCI makes it easy to set up your environment to meet your testing needs.

## Root commands and packages

[Contact us](mailto:sayhi@circleci.com) to add any packages we don't currently have.
We'll add them to your project, and you'll be able to start using them immediately.
We'll add common packages to our VM image too.

We can run any arbitrary command for you in the same way, not just package management.
This means we can tweak our VM in any way you need.
Let us know what commands you need when you [contact us](mailto:sayhi@circleci.com).

In case you're curious, the reason we require this is that our virtualization does not support providing `sudo` to arbitrary users.
We use LXC (the same technology used by Heroku, Docker, and other PaaSes).
LXC is one of the many technologies we use to make your builds run so fast, especially compared to alternatives such as VirtualBox or OpenVZ.
The tradeoff is you need to [contact us](mailto:sayhi@circleci.com) for `sudo` commands. We plan to address this in the future.

## Installing via the circle.yml

If you don't need root to install your custom software, you can do it manually.
You do this by adding extra commands within the dependencies section in your
`circle.yml` file.

For example, if you need to use a specific version of Redis, you would download and compile the needed version as part of your build.

```
dependencies:
  pre:
    - wget http://redis.googlecode.com/files/redis-2.4.18.tar.gz
    - tar xvzf redis-2.4.18.tar.gz
    - cd redis-2.4.18 && make
```

### Caching

Natually, downloading and compiling this custom software can take time, making your build longer.
To reduce the time spent installing dependencies, CircleCI will cache them between builds.
You can add arbitrary directories to this cache, allowing you to avoid the overhead of building your custom software during the build.

Tell CircleCI to save a cached copy using the
[`cache_directories` setting, in your `circle.yml` file](/docs/configuration#cache-directories).

```
dependencies:
  cache_directories:
    - redis-2.4.18   # relative to the build directory
```

### Avoid recompiles

To take advantage of this, you'll need to test for the presence of an existing cached install of your software.
If the software is already present, no need to build it.
You should test that your cache is there by including a
`test` command in the dependencies section in your `circle.yml` file.

```
dependencies:
  cache_directories:
    - redis-2.4.18
  pre:
    - if [[ ! -e redis-2.4.18/src/redis-server ]]; then wget http://redis.googlecode.com/files/redis-2.4.18.tar.gz && tar xzf redis-2.4.18.tar.gz && cd redis-2.4.18 && make; fi
```

Or, a little more cleanly:

```
dependencies:
  pre:
    - bash ./install-redis-2.4.18.sh
```

where `install-redis-2.4.18.sh` is a file checked into your repository, like the following:

```
set -x
set -e
if [ ! -e redis-2.4.18/src/redis-server ]; then
  wget http://redis.googlecode.com/files/redis-2.4.18.tar.gz
  tar xzf redis-2.4.18.tar.gz
  cd redis-2.4.18
  make;
fi
```
