# 'make' will list all documented targets, see https://marmelab.com/blog/2016/02/29/auto-documented-makefile.html
.DEFAULT_GOAL := help
.PHONY: help
help:
	@echo "\033[33mAvailable targets, for more information, see \033[36mREADME.md\033[0m"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'


.PHONY: clean
clean:  ## clean build files
	rm -rf target || 0

.PHONY: targets
targets: ## list available tools.edn (deps.edn) commands you can run
	clojure -M -e '(do (println "Targets: ") (doseq [target (-> "deps.edn" slurp read-string :aliases keys)] (println (str"* clojure -M:" (name target)))))'

.PHONY: repl-clj
repl-clj:  ## start a clojure/cider REPL
	clojure -Sdeps '{:deps {nrepl/nrepl {:mvn/version "0.8.3"} refactor-nrepl {:mvn/version "2.5.0"} cider/cider-nrepl {:mvn/version "0.25.5"}}}' -A:dev -m nrepl.cmdline --middleware '["refactor-nrepl.middleware/wrap-refactor","cider.nrepl/cider-middleware"]'

.PHONY: repl-cljs
repl-cljs:  ## start shadowcljs (note: need clojure server to see)
	-echo "starting shadowcljs - will recompile on changes."
	-echo ""
	-echo "NOTE: you will need to start the Clojure server to visit the pages."
	npx shadow-cljs watch :app

.PHONY: uberjar
uberjar:  ## produce self-contained executable jar in targets/ folder
	-echo "precompiling class definition"
	clojure -M -e "(compile 'app.core)"
	-echo "Creating uberjar..."
	clojure -M:uberjar


.PHONY: test
test:  ## run tests
	clojure -M:test
