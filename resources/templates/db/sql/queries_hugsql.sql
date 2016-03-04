-- :name create-<<entity>>! :i!
-- :doc creates a new <<entity>> record
INSERT INTO <<entity-plural>>
<<properties>>
VALUES <<values-properties>>

-- :name update-<<entity>>! :! :n
-- :doc update an existing <<entity>> record
UPDATE <<entity-plural>>
SET <<set-properties>>
WHERE id = :id

-- :name get-<<entity>> :? :1
-- :doc retrieve a <<entity>> given the id.
SELECT * FROM <<entity-plural>>
WHERE id = :id

-- :name delete-<<entity>>! :! :n
-- :doc delete a <<entity>> given the id
DELETE FROM <<entity-plural>>
WHERE id = :id
