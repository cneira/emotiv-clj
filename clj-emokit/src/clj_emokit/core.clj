(ns clj-emokit.core
  (:import [com.codeminders.hidapi HIDDeviceInfo HIDManager HIDDeviceNotFoundException ClassPathLibraryLoader])
  (:import [javax.crypto.spec.SecretKeySpec ])
  )


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
                          ))

(def emokit-epoch-id  4660)
(def product_id 60674)



(defn load-hid-natives []
  (memoize #(ClassPathLibraryLoader/loadNativeHIDLibrary))
)


(defn get-devices []
  (do
    (load-hid-natives)
    (def m (HIDManager/getInstance))
    (.listDevices m)
    )
  )


;this will return a coll for ex
;  {:path "\\?\hid#vid_05e1&pid_202a&mi_03#7&2c76e901&0&0000#{4d1e55b2-f16f-11cf-88cb-001111000030}" :vendor_id 1505 :product_id 8234 :serial_number 0000000001 :release_number 256 :manufacturer_string "Generic" :product_string "USB Ear-Microphone" :usage_page 12 :usage 1 :interface_number 3

(defn list-devices []
  (for [x  (get-devices)]
    {:path (.getPath  x)  :vendor_id (.getVendor_id x)  :product_id  (.getProduct_id x) :serial_number (.getSerial_number x)
     :release_number (.getRelease_number x) :manufacturer_string  (.getManufacturer_string x) :product_string  (.getProduct_string x) :usage (.getUsage x)
     :usage_page (.getUsage_page x) :interface_number (.getInterface_number x ) :object x}

    )
  )

;This will return a hash-map representing first device found using the keys (vendor_id, product_id)/ (vendor_id, product_id,interface)
(defn find-devices
  ([vendor  product ]
   (let [ from-device (list-devices)]
     (first (filter #(and (== vendor (:vendor_id %)) (== product (:product_id %)))   from-device ))
     ))

  ([vendor  product interface]
   (let [ from-device (list-devices)]
    (first (filter #(and (== vendor (:vendor_id %)) (== product (:product_id %))  (== interface (:interface_number %)))   from-device ))
     ))  )

(defn open-device
  ([ vendor_id product_id ]
   (.open (:object (find-devices vendor_id  product_id  ))))

  ([ vendor_id product_id interface]
   (.open (:object (find-devices vendor_id  product_id interface ))))
  )


 (defn close-device
   ([ vendor_id product_id ]
   (.close (:object (find-devices vendor_id  product_id  ))))
   ([ vendor_id product_id interface]
   (.close (:object (find-devices vendor_id  product_id interface ))))
  )



;; emotiv related stuff



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
             (if (== bytes-read 0) nil  dev ))
             )
        )
      ))


(defn get-type-of-device []
  (let [ consumer-epoch (map ubyte [0x00 0xa0 0xff 0x1f 0xff 0x00 0x00 0x00 0x00]) buf (byte-array 9)  dev  (open-device emokit-epoch-id product_id 0 )]
    (if (nil? dev) nil
      (do  (let [ bytes-read (.getFeatureReport dev buf) ]
             (close-device emokit-epoch-id product_id  0 )
             (if (== bytes-read 0) nil  (if (== consumer-epoch buf) "consumer" "research" ))
             )
        )
  )))

(defn research? []
  (if (== (get-type-of-device) "research") true false )
  )




(defn get-serial [ emotiv-device ]
  (let  [ serial (:serial_number  emotiv-device)]
    (if  (and  (= (count serial) 16 )  (boolean (re-find #"SN" serial ))) serial  nil )))




(defn getkey-from-emotiv-device   [emotiv-device]
  (let [serial  (get-serial (find-devices emokit-epoch-id product_id )) ]
   (let [ raw  (.getBytes  serial)  b (byte-array 16)  ]
     (do
         (aset-byte b 0  (aget raw 15))
         (aset-byte b 1 0)
         (aset-byte b 2 (aget raw 14))
         (aset-byte b 3  (if (true? research?) (byte \H)  (byte \T)  ))
         (aset-byte b 4  (if (true? research?) (aget raw 15)  (aget raw  13)  ))
         (aset-byte b 5  (if (true? research?) (byte 0)  (byte  16 )  ))
         (aset-byte b 6  (if (true? research?) (aget raw 14)  (aget raw  12)  ))
         (aset-byte b 7  (if (true? research?)  (byte \T) (byte \B) ))
         (aset-byte b 8  (if (true? research?) (aget raw 13)  (aget raw  15)  ))
         (aset-byte b 9  (if (true? research?) (byte 16) (byte 0) ))
         (aset-byte b 10  (if (true? research?) (aget raw 12)  (aget raw  14)  ))
         (aset-byte b 11  (if (true? research?) (byte \B)  (byte \H)  ))
         (aset-byte b 12 (aget raw 13))
         (aset-byte b 14 (aget raw 12))
         (aset-byte b 15  \P )
         (new SecretKeySpec  b "AES")

       )

     ))
  )


(defn









