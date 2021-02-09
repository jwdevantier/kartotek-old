# Query Help

Search primarily works on note metadata. When searching, we consider the filename itself and any additional metadata given.
Metadata is added by creating a YAML-header in the file as shown below:

```yml
---
title: hello world
tags: [one, two]
description: a minimal C# program
---
...content here...
```

When searching, the query is split by whitespace into conditions. A note only matches if each condition is satisfied.

 To match on phrases, surround it in quotes, e.g. `hello world` is treated as two independent substrings whereas `"hello world"` is treated as a phrase to match.

Similarly, you may prefix `t:` to search among the note's tags or `r:` to search for notes linking to `<note>`.


## Examples

```
hello world
```

Show notes whose title contain the substrings "hello" and "world"

```
"hello world"
```
Show notes whose title contain the substring 'hello world'.

```
hello -world
```

Show notes whose title contain the substring `hello` but *not* `world`.

```
t:programming r:hello.md
```

Show notes related to the note `hello.md` (that is, they link to it) which are also tagged `programming`.

```
t:programming -react
```

Show all notes tagged programming whose title do not contain the substring `react`.