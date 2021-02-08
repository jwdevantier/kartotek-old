(ns app.utils)


(defn atexit
  "schedule function to run when the runtime is shutting down."
  [f]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. f)))
