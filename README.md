# re-find
[![CircleCI](https://circleci.com/gh/borkdude/re-find/tree/master.svg?style=svg)](https://circleci.com/gh/borkdude/re-find/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/re-find.svg)](https://clojars.org/re-find)

Find functions that have a matching `:args` and/or `:ret` spec on given
examples.

## Usage

This utility comes with a programmatic and CLI interface. For the programmatic
interface see the docstring of `re-find.core/match`.
The web version is hosted [here](https://borkdude.github.io/re-find.web/).

### CLI

CLI options:

``` clojure
(def cli-options
  [["-a" "--args ARGUMENTS" "arguments"]
   ["-r" "--ret RETVAL" "return value"]
   ["-e" "--exact-ret-match" "return value must match on value"]
   ["-s" "--safe" "safe: no evaluation of functions on given arguments"]
   ["-v" "--verbose" "prints table with return values"]
   ["-p" "--permutations" "try with permutations of args"]
   ["-f" "--finitize" "prevent evaluation of infinite collections"]])
```

These options are best explained with examples.

The following examples are possible because of the specs in
[speculative](https://github.com/slipset/speculative/). They are preloaded using
the alias. Speculative is not part of this utility. This utility could be used
with arbitrary other specs that you load in your own code.

So let's search some Clojure core functions.

Which functions accept `inc [1 2 3]` as arguments and return exactly `[2 3 4]`?

``` shell
$ clj -Aspeculative --args 'inc [1 2 3]' -r '[2 3 4]' -e -v

|          function |   arguments | return value |
|-------------------+-------------+--------------|
| clojure.core/keep | inc [1 2 3] |      (2 3 4) |
|  clojure.core/map | inc [1 2 3] |      (2 3 4) |
```

Of course, `map` and `keep`!

What if we got the order of the arguments wrong? This is what the
`--permutations` option is for:

``` shell
$ clj -Aspeculative --args '[1 2 3] inc' -r '[2 3 4]' -e -v -p

|          function |   arguments | return value |
|-------------------+-------------+--------------|
| clojure.core/keep | [1 2 3] inc |      (2 3 4) |
|  clojure.core/map | [1 2 3] inc |      (2 3 4) |
```

Without the `-e` option the return value doesn't only has to satisfy the `:ret`
spec and is checked independent from the arguments. In the following example,
since `4` matches `any?`, both `/` and `some?` match:

``` shell
$ clj -Aspeculative --args '8' --ret '4' -v

|           function | arguments | return value |
|--------------------+-----------+--------------|
|     clojure.core// |         8 |          1/8 |
| clojure.core/some? |         8 |         true |
```

In addition to a value, the `--ret` option accepts a predicate:

``` shell
$ clj -Aspeculative --args '8' --ret 'number?' -v

|       function | arguments | return value |
|----------------+-----------+--------------|
| clojure.core// |         8 |          1/8 |
```

A search for functions that accept two `sets` and return a `set`:

``` shell
$ clj -Aspeculative --args '#{1 2} #{2 3}' --ret 'set?' -v

|                 function |     arguments | return value |
|--------------------------+---------------+--------------|
| clojure.set/intersection | #{1 2} #{2 3} |         #{2} |
|   clojure.set/difference | #{1 2} #{2 3} |         #{1} |
|        clojure.set/union | #{1 2} #{2 3} |     #{1 3 2} |
|       clojure.set/select | #{1 2} #{2 3} |         #{2} |
```

Without the `-v` option, only a list of symbols of matching functions is returned:

``` shell
$ clj -Aspeculative --args '#{1 2} #{2 3}' --ret 'set?'
(clojure.set/intersection
 clojure.set/difference
 clojure.set/union
 clojure.set/select)
```

What functions called with `nil` return exactly `nil`?
``` shell
$ clj -Aspeculative --args 'nil' --ret 'nil' -e
(clojure.set/intersection
 clojure.core/first
 clojure.core/merge
 clojure.set/difference
 clojure.set/union)
```

With what options can we find the beautiful function named `re-find`?

``` shell
$ clj -A:speculative --args '#"b" "abc"' --ret '"b"' -e -v

|             function |  arguments | return value |
|----------------------+------------+--------------|
| clojure.core/re-find | #"b" "abc" |          "b" |
```

For safety, there is a `--safe` option that will prevent found functions to
evaluate with the given arguments.

``` shell
$ clj -Aspeculative --args 'nil' --ret 'nil' -e --safe
Assert failed: exact-ret-match? is true or ret is fn? but safe? is set to true
```

To prevent evaluation of infinite collections, use the `--finitize` option:

``` shell
$ clj -Aspeculative --args '' -r '#(every? number? %)' -v -p -f

|           function | arguments |              return value |
|--------------------+-----------+---------------------------|
|  clojure.core/list |           |                        () |
| clojure.core/range |           | (0 1 2 3 4 5 6 7 8 9 ...) |
|  clojure.core/into |           |                        [] |
|   clojure.core/str |           |                        "" |
|  clojure.core/conj |           |                        [] |
|  clojure.set/union |           |                       #{} |
```

Without the `-p` option, the above example would never terminate, because
`(range)` returns an infinite collection and the return predicate runs over it
with `every?`.

## Name

Often you know there's a function for it, but you forgot the name. `re-find` can
help you re-find it. The name for this library was inspired by the awesome
`re-find` function in Clojure.

## Credits

Inspiration came from [findfn](https://github.com/Raynes/findfn) which was a
cool library in the early days of Clojure. Its strategy was brute force and
just tried to call all core functions.

The idea to use specs to find functions was triggered by an episode of [The
REPL](https://www.therepl.net/) with [Martin
Klepsch](https://twitter.com/martinklepsch). They were discussing
[Hoogle](https://hoogle.haskell.org/) which is a search engine for Haskell that
finds functions by type signatures. Clojure has specs, so why not use those.

## License

Copyright © 2018 Michiel Borkent

Distributed under the EPL License, same as Clojure. See LICENSE.
