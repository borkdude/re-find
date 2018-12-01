# spec-search

Find functions that have a matching `:args` and/or `:ret` spec on given
examples.

## Usage

This utility comes with a programmatic and CLI namespace. For the programmatic
interface see the docstring of `spec-search.core/search`.

### CLI

CLI options:

``` clojure
(def cli-options
  [["-a" "--args ARGUMENTS" "Arguments"]
   ["-r" "--ret RETVAL" "Return value"]
   ["-e" "--exact-ret-match" "Return value must match on value"]
   ["-v" "--print-ret-vals" "Filter and print on succesful return values"]
   ["-h" "--help"]])
```

These options are best explained with examples.

Search functions that accepts `8 2` as arguments, returns exactly the number `4`
and print the return value of the function calls:

``` shell
$ clj -Aspeculative --args '8 2' --ret '4' -e -v

|       function | arguments | return value |
|----------------+-----------+--------------|
| clojure.core// |       8 2 |            4 |

```

Without the `-e` option, the `:ret` match only has to be valid for the given
example. Since `4` is `any?`, the `some?` function also matches here:

``` shell
$ clj -Aspeculative --args '8' --ret '4' -v

|           function | arguments | return value |
|--------------------+-----------+--------------|
|     clojure.core// |         8 |          1/8 |
| clojure.core/some? |         8 |         true |
```

Search functions that accept `8 2` as arguments and show whatever they return:

``` shell
$ clj -Aspeculative --args '8 2'  -v

|           function | arguments | return value |
|--------------------+-----------+--------------|
|     clojure.core// |       8 2 |            4 |
|   clojure.core/get |       8 2 |          nil |
|     clojure.core/= |       8 2 |        false |
| clojure.core/range |       8 2 |           () |
|   clojure.core/str |       8 2 |         "82" |
```

Another example:

``` shell
$ clj -Aspeculative --args '{:a 1} :a' -v

|             function | arguments |                                                                             return value |
|----------------------+-----------+------------------------------------------------------------------------------------------|
|     clojure.core/get | {:a 1} :a |                                                                                        1 |
|       clojure.core/= | {:a 1} :a |                                                                                    false |
|     clojure.core/str | {:a 1} :a |                                                                               "{:a 1}:a" |
| clojure.core/partial | {:a 1} :a | #object[clojure.core$partial$fn__5824 0x88a8218 "clojure.core$partial$fn__5824@88a8218"] |
|    clojure.core/juxt | {:a 1} :a |     #object[clojure.core$juxt$fn__5807 0x4163f1cd "clojure.core$juxt$fn__5807@4163f1cd"] |
|    clojure.core/fnil | {:a 1} :a |       #object[clojure.core$fnil$fn__6883 0x23aae55 "clojure.core$fnil$fn__6883@23aae55"] |
```

Without the `-v` option, only a list of symbols of matching functions is returned:

``` shell
$ clj -Aspeculative --args '{:a 1} :a'
(clojure.core/get
 clojure.core/=
 clojure.core/str
 clojure.core/partial
 clojure.core/juxt
 clojure.core/fnil)
```

Only providing a `--ret` value is also supported:

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
$ clj -Aspeculative --args '"foo" 1' --ret '"foo"'
(clojure.core/get clojure.core/subs clojure.core/str)
```

## License

Copyright © 2018 Michiel Borkent

Distributed under the MIT License. See LICENSE.
