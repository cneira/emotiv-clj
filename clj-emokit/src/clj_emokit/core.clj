(ns clj-emokit.core
  (:import [com.codeminders.hidapi HIDDeviceInfo HIDManager HIDDeviceNotFoundException ClassPathLibraryLoader])
  )




(defn load-hid-natives []
   (ClassPathLibraryLoader/loadNativeHIDLibrary))


(defn get-devices []
  (do
    (def m (HIDManager/getInstance))
    (.listDevices m)
    )
  )


(defn find-devices

  ([vendor  product ]
   (let [ from-device (list-devices)]
     (filter #(and (== vendor (:vendor_id %)) (== product (:product_id %)))   from-device )
     ))

  ([vendor  product interface]
   (let [ from-device (list-devices)]
     (filter #(and (== vendor (:vendor_id %)) (== product (:product_id %))  (== interface (:interface_number %)))   from-device )
     ))  )




;this will return a coll for ex
;  {:path "\\?\hid#vid_05e1&pid_202a&mi_03#7&2c76e901&0&0000#{4d1e55b2-f16f-11cf-88cb-001111000030}" :vendor_id 1505 :product_id 8234 :serial_number 0000000001 :release_number 256 :manufacturer_string "Generic" :product_string "USB Ear-Microphone" :usage_page 12 :usage 1 :interface_number 3

(defn list-devices []
  (for [x  (get-devices)]
    {:path (.getPath  x)  :vendor_id (.getVendor_id x)  :product_id  (.getProduct_id x) :serial_number (.getSerial_number x)
     :release_number (.getRelease_number x) :manufacturer_string  (.getManufacturer_string x) :product_string  (.getProduct_string x) :usage (.getUsage x)
     :usage_page (.getUsage_page x) :interface_number (.getInterface_number x ) :object x}

    )
  )



(defn open-device
  ([ vendor_id product_id ]
   (.open (:object (find-devices vendor_id  product_id  ))))

  ([ vendor_id product_id interface]
   (.open (:object (find-devices vendor_id  product_id interface ))))


  )


;; emotiv related stuff


(def E_EMOKIT_DRIVER_ERROR -1)
(def E_EMOKIT_NOT_INITED -2)
(def E_EMOKIT_NOT_OPENED -3)


(def EMOKIT_VID 0x21a1)
(def EMOKIT_VID 0x0001)

; Out endpoint for all emotiv devices
(def EMOKIT_OUT_ENDPT  0x02)
; In endpoint for all emotiv devices
(def EMOKIT_IN_ENDPT   0x82)
(def EMOKIT_KEYSIZE   16 )  ; 128 bits == 16 bytes
(def EMOKIT_RESEARCH  1  )

; ID of the feature to report we need to identify the device as consumer/research
(def EMOKIT_REPORT_ID 0  )
(def EMOKIT_REPORT_SIZE 9 )

(def F3_MASK  (byte-array [ (byte 10) (byte 11) (byte 12) (byte 13)  (byte 14) (byte 15)
                            (byte 0) (byte 1) (byte 2) (byte 3)  (byte 4) (byte 5)
                            (byte 6) (byte 7)
                            ]
                            )
  )

(def emokit-epoch-id  4660)
(def product_id 60674)

( get (find-devices  emokit-epoch-id product_id 1 ) :vendor_id)
(map? (find-devices  emokit-epoch-id product_id 1 ))



 ;https://groups.google.com/forum/#!topic/clojure/nScP6pOrgjo
 ; no unsigned char available yet ...
 (defn ubyte [b]
   (if (>= b 128)
     (byte (- b 256))
     (byte b)))



 (defn  find-emotiv-epoch []
   (let [ consumer-epoch (map ubyte [0x00 0xa0 0xff 0x1f 0xff 0x00 0x00 0x00 0x00])  buf (byte-array 9)  dev  (open-device emokit-epoch-id product_id 0 )]
     (if (nil? dev) nil
       (do  (let [ bytes-read (.getFeatureReport dev buf) ]
              (if (== bytes-read 0) nil  (if (== consumer-epoch  buf) true ))
              )
         )
       )))

 (find-emotiv-epoch)



















