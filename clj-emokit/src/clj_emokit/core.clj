(ns clj-emokit.core
  (:import [com.codeminders.hidapi HIDDeviceInfo HIDManager HIDDeviceNotFoundException ClassPathLibraryLoader])

  )



(defn load-hid-natives []
   (ClassPathLibraryLoader/loadNativeHIDLibrary))


(def E_EMOKIT_DRIVER_ERROR -1)
(def E_EMOKIT_NOT_INITED -2)
(def E_EMOKIT_NOT_OPENED -3)


(def EMOKIT_VID 0x21a1)
(def EMOKIT_VID 0x0001)

; Out endpoint for all emotiv devices
(def EMOKIT_OUT_ENDPT  0x02)
; In endpoint for all emotiv devices
(def EMOKIT_IN_ENDPT   0x82)

