(ns shtrom.t-data)

(def test-dir "/tmp/shtrom-test")

(def test1-key "1")
(def test2-key "2")
(def test-ref "test")
(def test-bin-size 64)

(def test-hist-length 4)
(def test-hist-body '(0 256 (51406115 305419793 2311186 304231527)))

(def test-reduce-hist-length 2)
(def test-reduce-hist-body '(0 256 (356825908 306542713)))

(def test-content-length (+ 16 (* test-hist-length 4)))
(def test-reduce-content-length (+ 16 (* test-reduce-hist-length 4)))

(def test-resources-dir "test-resources")
