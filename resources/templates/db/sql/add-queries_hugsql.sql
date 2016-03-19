<% for r in queries %><% if not any r.assoc r.dissoc r.dissoc-all %>-- :name get-<<r.ent>>-<<r.prop>> :? <% if r.sel-multi %>:*<% else %>:1<% endif %>
-- :doc returns <<r.prop>> associated with <<r.ent>>
SELECT * FROM <<r.f-tbl>>
WHERE <<r.f-id>> <<r.sign>> <% if r.no-nest %>:<<r.ent>><% else %>(SELECT <<r.s-id>> FROM <<r.s-tbl>> WHERE <<r.t-id>> = :<<r.ent>>)<% endif %>

<% endif %><% if r.assoc %>-- :name assoc-<<r.ent>>-<<r.prop>>! :!
-- :doc associates <<r.ent>> with corresponding <<r.prop>>
<% if r.update %>UPDATE <<r.f-tbl>> SET <<r.f-id>> = :<<r.f-id-val>>
WHERE <<r.s-id>> <<r.update-where>><% else %>INSERT INTO <<r.f-tbl>> (<<r.f-id>>, <<r.s-id>>)
VALUES <<r.insert-values>><% endif %>

<% endif %><% if r.dissoc %>-- :name dissoc-<<r.ent>>-<<r.prop>>! :!
-- :doc dissociates <<r.ent>> from corresponding <<r.prop>>
<% if r.update %>UPDATE <<r.f-tbl>>
SET <<r.f-id>> = NULL
WHERE <<r.s-id>> <<r.update-where>><% else %>DELETE FROM <<r.f-tbl>>
WHERE <<r.f-id>> = :<<r.ent>> AND <<r.s-id>> <% if r.and-single %>= :<<r.prop>><% else %>IN :tuple:<<r.prop>><% endif %><% endif %>

<% endif %><% if r.dissoc-all %>-- :name dissoc-all-<<r.ent>>-<<r.prop>>! :!
-- :doc dissociates all <<r.ent>> from corresponding <<r.prop>>
<% if r.update %>UPDATE <<r.f-tbl>>
SET <<r.f-id>> = NULL
WHERE <<r.s-id>> <<r.update-where>><% else %>DELETE FROM <<r.f-tbl>>
WHERE <<r.f-id>> = :<<r.ent>>

<% endif %><% endif %><% endfor %>
