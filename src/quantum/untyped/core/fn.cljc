(ns quantum.untyped.core.fn)

#?(:clj (defmacro fn1 [f & args] `(fn fn1# [arg#] (~f arg# ~@args)))) ; analogous to ->
#?(:clj (defmacro fnl [f & args] `(fn fnl# [arg#] (~f ~@args arg#)))) ; analogous to ->>
