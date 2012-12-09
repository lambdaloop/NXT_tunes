(ns abc-parser
  (:require [clojure.contrib
	     [string :as s]
	     [math :as math]])
  (:use [clojure.contrib.pprint :only [pprint cl-format]]))

(defn cons-last
  [x l]
  (concat l (list x)))

;(def filename
;     "/home/pierre/Desktop/Jupiter/programs/robotics/sound/abcs/link.abc")

(defn concat-re
  [& res]
  (re-pattern (apply str res)))

(defn remove-comments
  [text]
  (s/replace-re #"\s*%.*\n" "\n"
		(s/replace-re #"\n\s*%.*" "" text)))

;(def abctext
;     (remove-comments (slurp filename)))

(defn get-header
  [text]
  (take-while #(re-matches #"\p{Alpha}:.*" %)
	      (s/split #"\n" text) ))


(defn note-length
  [note]
  (if-let [numbers (re-find #"\d+" note)]
    (if (re-find #"/" note)
      (/ 1 (read-string numbers))
      (read-string numbers))
    1))

(defn note-octave
  [note]
  (cond (re-find #"[xz]" note)
	-1 ;rest
	(re-find #"\p{Upper}" note)
	(- 4 (count (re-find #",+" note))) ;going down
	:else
	(+ 5 (count (re-find #"'+" note))))) ;going up

(defn note-accident
  [note]
  (let [accident (re-find #"^[\^_]?" note)]
    (case accident
	  "^" '+
	  "_" '- 
	  nil)))

(defn note-name
  [note]
  (s/upper-case (re-find #"\p{Alpha}" note)))

(defn note-slur?
  [note]
  (re-find #"-" note))

(defn note-info
  [note]
  {:length (note-length note)
   :octave (note-octave note)
   :accident (note-accident note)
   :name (note-name note)
   :slur (note-slur? note)})


(defn get-notes
  [text]
  (s/replace-str "&" ""
	     (s/replace-re #"[ |\\\n\(\)]" ""
	     (apply str
		    (drop-while #(re-matches #"\D:.*" %)
				(s/split #"\n" text))))))

(def *note-pattern*
      #"[\^_=]?\p{Alpha},*'*/?\d*-?")

(defn clean-up-notes
  [notes]
  (s/replace-re (concat-re "\\[(" *note-pattern* ").*?\\]") "$1" notes))

(defn find-notes
  [note-text]
  (re-seq *note-pattern* note-text))

(defn split-notes-by-voice
  [notes]
  (s/split #"V:\d+" notes))

(defn parse-notes
  [notes]
  (map note-info notes))

(defn same-note?
  "is this the same note?
   (up to length)"
  ([n1] true)
  ([n1 n2]
     (= (dissoc n1 :length :slur)
	(dissoc n2 :length :slur)))
  ([n1 n2 & more]
     (every? #(same-note? n1 %)
	     (cons n2 more))))

(defn combine-notes
  "combines notes, assuming
they represent the same note, up to length"
  ([note] note)
  ([n1 n2]
     (assert (same-note? n1 n2))
     (assoc n1 :length (+ (:length n1)
				(:length n2))))
  ([n1 n2 & more] (reduce combine-notes
			  (combine-notes n1 n2)
			  more)))

(defn merge-notes
  [notes]
  (loop [prev-note (first notes)
	 notes (rest notes)
	 result nil]
    (let [n (first notes)]
      (cond (empty? notes)
	    (reverse (cons prev-note result))
	    (and (same-note? n prev-note) (:slur prev-note))
	    (recur (combine-notes n prev-note)
		   (rest notes) result)
	    :else
	    (recur n (rest notes) (cons prev-note result))))))


(def *flat-names* {"B" "A"
		    "A" "G"
		    "G" "F"
		    "E" "D"
		    "D" "C"})

(defn flat-to-sharp
  [name octave]
  (case name
	"F" ["E" octave nil]
	"C" ["B" (dec octave) nil]
	[(*flat-names* name) octave '+]))

(def *distance-to-C0-by-name*
     (zipmap ["C" "C+" "D" "D+" "E" "F" "F+"
	      "G" "G+" "A" "A+" "B"]
	     (iterate inc 0)))
			

(defn distance-to-C0
  [name octave accident]
  (+ (*distance-to-C0-by-name* (str name accident))
     (* octave 12))) ;12 notes per octave (w/ sharps)
  

(def *my-ratio* 1/4) ;my format uses 1/4 for normal length
(def *robotc-ratio* (* 60 100))
(def *bash-ratio* (* 60 1000))

(defn rest? [name]
  (re-matches #"[XZ]" (s/upper-case name)))

(defn note-freq
  ([dist-C0]
     (let [ratio (math/expt 2 1/12)
	   start-freq (* 27.5 (math/expt ratio -9))] ;we are going down from A0 to C0
       (* start-freq (math/expt ratio dist-C0))))
  ([name octave accident]
     (cond (rest? name)
	   0 ;just a rest
	   (= accident '-)
	   (apply note-freq (flat-to-sharp name octave)) ;only sharps in table
	   :else
	   (note-freq (distance-to-C0 name octave accident)))))


(defn convert-note-pierre
  "converts an abc note into note of Pierre format.
e.g. (with length=1/8)
      A4 -> A4 2
     ^G,4 -> G+3 2
      e/2 -> E5 .25
      f -> F5 .5
      z4 -> R 2
      x2 -> R 1"
  [note header]
  (let [length (read-string (header :length))
	ratio (/ length *my-ratio*)
	note-length (double (* (note :length) ratio))]
    (if (rest? (note :name))
      (str "R " note-length) ;rest
      (str (note :name) (note :accident)
	   (note :octave) " " note-length))))


(defn find-ms-length
  [abc-speed]
  (let [[_ length beats] (re-matches #"(.*)=(.*)" abc-speed)
	[length beats] (map read-string [length beats])]
    (/ 1 beats length)))

(defn convert-note-robotc
  [note header]
  (let [ms-length (find-ms-length (header :speed))
	length (read-string (header :length))
	ratio (* length ms-length *robotc-ratio*)
	freq (note-freq (note :name) (note :octave)
			(note :accident))
	real-length (double (* (note :length) ratio))]
    (if (rest? (note :name))
      (cl-format nil "\twait10Msec(~a);" real-length)
      (cl-format nil "\tPlayTone(~a, ~a); wait10Msec(~:*~a);"
		 freq real-length))))

(defn convert-note-bash
  [note header]
  (let [ms-length (find-ms-length (header :speed))
	length (read-string (header :length))
	ratio (* length ms-length *bash-ratio*)
	freq (note-freq (note :name) (note :octave)
			(note :accident))
	real-length (double (* (note :length) ratio))]
    (if (rest? (note :name))
      (str "beep -f 1 -l 0 -D " real-length)
      (str "beep -f " freq " -l " real-length))))
	

(defn convert-notes
  [notes length converter]
  (map #(converter % length) notes))


(def vars
     '{T :title
       L :length
       Q :speed})

(defn parse-header
  [headers]
  (loop [result {}
	 headers headers]
    (if (empty? headers)
      result
      (let [[_ var value] (re-matches #"(\p{Alpha}):\W*(.*)" (first headers))
	    var (symbol var)]
	(if (vars var)
	  (recur (assoc result (vars var) value)
		 (rest headers))
	  (recur result (rest headers)))))))

(defn find-pierre-speed
  [abc-speed]
  (let [[_ length beats] (re-matches #"(.*)=(.*)" abc-speed)]
    (* (/ (read-string beats) (read-string length))
       (/ 1 4))))


(defn notes-and-header
  [file converter]
  (let [text (remove-comments (slurp file))
	header (-> text get-header parse-header)
	note-text  (-> text get-notes clean-up-notes split-notes-by-voice)
	notes  (map #(-> % find-notes parse-notes merge-notes) note-text)
	voice-notes (map #(convert-notes % header converter)
			 notes)
	printed-notes (map #(s/join "\n" %) voice-notes)]
    [printed-notes header]))

(defn file->pierre
  [file]
  (let [[voice-notes header]
	(notes-and-header file convert-note-pierre)]
    (for [notes voice-notes]
      (str "#config\n"
	 "T: " (header :title) "\n"
	 "Q: " (double (find-pierre-speed (header :speed))) "\n"
	 "#end\n"
	 "\n\n"
	 notes))))

(defn file->robotc
  [file]
  (let [[voice-notes header]
	(notes-and-header file convert-note-robotc)]
     (for [notes voice-notes]
      (str "task main\n"
	 "{\n"
	 notes
	 "\n}"))))

(defn file->bash
  [file]
  (let [[voice-notes header]
	(notes-and-header file convert-note-bash)]
     (for [notes voice-notes]
      (str "#!/bin/bash\n" 
	   notes))))


(defn abc->x
  [file-converter extension abc-file output-file]
  (let [voices (file-converter abc-file)]
    (dorun (map #(do (spit (str output-file "_" %2 extension) %1)
		     (println (str output-file "_" %2 extension)))
		voices (iterate inc 1))))
  'done)

(def abc->pierre (partial abc->x file->pierre ".txt"))
(def abc->robotc (partial abc->x file->robotc ".c"))
(def abc->bash   (partial abc->x file->bash ".sh"))



(def sound-path "C:\\Users\\Pierre\\Dropbox\\programs\\projects\\robotics\\sound")
(def abcs-path (str sound-path "\\abcs"))