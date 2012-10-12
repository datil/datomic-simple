(ns datomic-simple.actions
  (:require [datomic-simple.db :as db])
  (:require clojure.string))
;;; basic CRUD fns, useful for any datomic app i.e. no namespace assumptions

(defn where [query & args]
  (->> (apply db/q query args)
    (mapv (fn [items]
      (mapv db/entity items)))))

(defn entity->map [e]
  (merge (select-keys e (keys e))
         {:db/id (:db/id e)}))

(defn find-all [query & args]
  (->> (apply where query args) flatten (map entity->map))) 

(defn find-first [query & args]
  (first (apply find-all query args)))

(defn find-by [query-map]
  (let [query-string (clojure.string/join " "
                      (concat
                        ["[:find ?e :in $"]
                        (map #(str "?field" % " ?val" %) (range (count query-map)))
                        [":where"]
                        (map #(str "[?e ?field" % " ?val" % "]") (range (count query-map)))
                        ["]"]))]
    (apply find-all query-string (flatten (vec query-map)))))

(defn num-id [id]
  (Long. id))

(defn find-id [id]
  (let [ent (db/entity (num-id id))]
    (if-not (empty? ent) (entity->map ent))))

(defn delete [& ids]
   (db/transact! (map #(vec [:db.fn/retractEntity (num-id %)]) ids)))

(defn bare-update [id attr]
  (db/transact! [(merge attr {:db/id (num-id id)})]))
