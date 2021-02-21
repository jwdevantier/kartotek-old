# Kartotek

*This application is quite bare-bones and still under trial and development.*

Kartotek is a small, self-contained application which I use to search among and discover relationships between my notes.

The application is inspired by the Zettelkasten method and thus favors a style of taking notes where each note is short, covering a single idea and where a lot of emphasis is placed on linking notes with other, related concepts.

This application focuses exclusively on searching through your notes. You can search by title, tags and relationships. That is `r:hello.md` will list all notes which link to `hello.md`.

For more information on the search/query language, see [search help](resources/search-help.md).

## Building the application

To build the application, you only need

1. A functioning Java VM
2. [Clojure](https://clojure.org/guides/getting_started)
3. The `make` program

 a functioning Java VM and  installed on your machine.

From there, you can build a self-contained jar like so:
```
make uberjar
```

## Running the application

having built the jar (see above), you may run it like so:
```
java -jar target/kartotek.jar
```

## Configuration

You can change the port used by the program's webserver as well as specify where your notes directory is found by writing a configuration file.

Note the program will look for `config.edn` in the current working directory. By default, the configuration would be the following:

```clojure
{:web {:port 8081}
 :db {:note-dir "notes"}}
```

## Tweaking look & feel
You can redefine the underwhelming styling in two ways:

1. Update the files in `resources/assets` and rebuild the JAR

2. Copy the file you wish to change from `resources/assets` into an `assets` folder in the directory from which you start the program and make your changes.

This way, you can download a new theme for [highlight.js](https://highlightjs.org/) and place it in `assets/highlight.theme.css` or make tweaks to `assets/style.css` as you desire.