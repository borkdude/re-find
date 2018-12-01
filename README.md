# spec-search

Find functions that have a matching `:args` and/or `:ret` spec on given
examples or predicates.

## Usage

This utility comes with a programmatic and CLI interface. For the programmatic
interface see the docstring of `spec-search.core/search`.

### CLI

CLI options:

``` clojure
(def cli-options
  [["-a" "--args ARGUMENTS" "Arguments"]
   ["-r" "--ret RETVAL" "Return value"]
   ["-e" "--exact-ret-match" "Return value must match on value"]
   ["-v" "--print-ret-vals" "Filter and print on succesful return values"]])
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

|         function |   arguments | return value |
|------------------+-------------+--------------|
| clojure.core/map | inc [1 2 3] |      (2 3 4) |
```

Of course, that's `map` (a spec for `mapv` isn't currently in speculative).

Without the `-e` option the return value doesn't only has to satisfy the `:ret` spec and is checked independent from the arguments. In the following example,
since `4` matches `any?`, both `/` and `some?` match:

``` shell
$ clj -Aspeculative --args '8' --ret '4' -v

|           function | arguments | return value |
|--------------------+-----------+--------------|
|     clojure.core// |         8 |          1/8 |
| clojure.core/some? |         8 |         true |
```

In addition to a value the `--ret` option accepts a predicate:

``` shell
$ clj -Aspeculative --args '8' --ret 'number?' -v

|       function | arguments | return value |
|----------------+-----------+--------------|
| clojure.core// |         8 |          1/8 |
```

A search for functions that accept `{:a 1} :b 1` as arguments and returns a
`map?`:

``` shell
$ clj -Aspeculative --args '{:a 1} :b 1' --ret 'map?' -v

|           function |   arguments | return value |
|--------------------+-------------+--------------|
| clojure.core/assoc | {:a 1} :b 1 | {:a 1, :b 1} |
```

Without the `-v` option, only a list of symbols of matching functions is returned:

``` shell
$ clj -Aspeculative --args '{:a 1}' --ret 'map?'
(clojure.core/merge clojure.core/partial)
```

Only providing a `--ret` value is also supported but this is slightly less
useful:

``` shell
$ clj -Aspeculative --ret '"foo"'
(clojure.core/remove
 clojure.core/get
 clojure.core/re-groups
 clojure.core/reduce
 clojure.core/first
 clojure.core/subs
 clojure.core/range
 clojure.core/some
 clojure.core/some?
 clojure.core/str
 clojure.core/apply
 clojure.core/filter
 clojure.core/re-matches
 clojure.core/re-find
 clojure.core/reset!
 clojure.core/map
 clojure.core/swap!)
```

If called with the right arguments, these functions could in theory return a
string. Function `range` won't ever return a string. The `:ret` of its spec is
`seqable?`, and since a string is `seqable?` it matches. More information means
better results. This makes more sense:

``` shell
$ clj -Aspeculative --args '"foo" 1' --ret 'string?'
(clojure.core/subs clojure.core/str)
```

What functions called with `nil` return `nil`?
``` shell
$ clj -Aspeculative --args 'nil' --ret 'nil' -e
(clojure.core/first clojure.core/merge)
```

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

Distributed under the MIT License. See LICENSE.
