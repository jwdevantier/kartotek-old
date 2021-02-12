(ns app.utils)


(defn atexit
  "schedule function to run when the runtime is shutting down."
  [f]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. f)))

(defn deep-merge
  "recursively merge maps."
  [& maps]
  (letfn [(m [& xs]
           (if (some #(and (map? %) (not (record? %))) xs)
             (apply merge-with m xs)
             (last xs)))]
    (reduce m maps)))
