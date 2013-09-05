;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html

(ns code-maat.parsers.mercurial
  (:require [instaparse.core :as insta]
            [incanter.core :as incanter]))

;;; This module is responsible for parsing a Mercurial log file.
;;;
;;; Input: a log file generated with the following command:
;;;         
;;;    hg log --template "rev: {rev} author: {author} date: {date|shortdate} files:\n{files %'{file}\n'}\n"
;;;
;;; The command above uses Mercurial's templating system to format an
;;; output with each file in the changeset separated by newlines.
;;;
;;; Ouput: An incanter dataset with the following columns:
;;;   :entity :date :author :rev
;;; where
;;;  :entity -> the changed entity as a string
;;;  :date -> commit date as a string
;;;  :author -> as a string
;;;  :rev -> revision from Mercurial

(def ^:const grammar
  "Here's the instaparse grammar for a Mercurial log-file."
   "
    <S>       =   entries
    <entries> =  (entry <nl*>)* | entry
    entry     =  rev <ws> author <ws> date <ws> changes
    rev       =  <'rev: '> #'\\d+'
    author    =  <'author: '> #'.+(?=\\sdate:\\s\\d{4}-)' (* match until the date field *)
    date      =  <'date: '> #'\\d{4}-\\d{2}-\\d{2}'
    changes   =  <'files:'> <nl> (file <nl?>)*
    file      =  #'.+'
    ws        =  #'\\s'
    nl        =  '\\n'
    ")

(def mercurial-log-parser
  (insta/parser grammar))

(defn- raise-parse-failure
  [f]
  (let [reason (with-out-str (print f))]
    (throw (IllegalArgumentException. reason))))

(defn as-grammar-map
  "The actual invokation of the parser.
   Returns a Hiccup parse tree upon success,
   otherwise an informative exception is thrown."
  [input]
  (let [result (insta/parse mercurial-log-parser input)]
    (if (insta/failure? result)
      (raise-parse-failure (insta/get-failure result))
      result)))