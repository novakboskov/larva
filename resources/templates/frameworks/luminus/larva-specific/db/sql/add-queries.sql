-- name: create-<<entity>><!
-- creates a new <<entity>> record
INSERT INTO <<entity-plural>>
<<properties>>
VALUES <<values-properties>>

-- name: update-<<entity>>!
-- update an existing <<entity>> record
UPDATE <<entity-plural>>
SET <<set-properties>>
WHERE id = :id

-- name: get-<<entity>>
-- retrieve a <<entity>> given the id.
SELECT * FROM <<entity-plural>>
WHERE id = :id

-- name: delete-<<entity>>!
-- delete a <<entity>> given the id
DELETE FROM <<entity-plural>>
WHERE id = :id
