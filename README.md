# project-catalog

A Hello-world-like application built using clojure, pedestal and monger... all just to try some clojure code :)


## Getting Started

1. Start a local mongodb instance
2. Start the application: `lein run-dev` \*
3. Use (GET http://localhost:5000/projects ) to check all the registered projects
4. Use (POST http://localhost:5000/projects) to register new projects
4. Read your app's source code at src/project_catalog/service.clj. Explore the docs of functions
   that define routes and responses.
5. Run your app's tests with `lein test`. Read the tests at test/project_catalog/service_test.clj.
6. Learn more! See the [Links section below](#links).

\* `lein run-dev` automatically detects code changes. Alternatively, you can run in production mode
with `lein run`.

## Configuration

To configure logging see config/logback.xml. By default, the app logs to stdout and logs/.
To learn more about configuring Logback, read its [documentation](http://logback.qos.ch/documentation.html).

## Links
* [Other examples](https://github.com/pedestal/samples)

