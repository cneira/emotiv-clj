(ns clj-emokit.core-test
  (:require [clojure.test :refer :all]
            [clj-emokit.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(list-devices)



(find-devices  4660  60674 1)
(find-emotiv-epoch)

(def k (getkey-from-emotiv-device  (find-devices  4660  60674 1)))



(research?)


(def e (map ubyte [0x00 0xa0 0xff 0x1f 0xff 0x00 0x00 0x00 0x00]) )


( def d (open-device emokit-epoch-id product_id))

(def buf (byte-array 9))
(def bytes-read (.getFeatureReport d buf))
