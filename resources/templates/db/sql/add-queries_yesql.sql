<% for r in queries %><% if not any r.assoc r.dissoc %>-- name: get-<<r.ent>>-<<r.prop>><!
-- returns <<r.prop>> associated with <<r.ent>>
SELECT * FROM <<r.f-tbl>> WHERE <<r.f-id>> <<r.sign>> <% if r.no-nest %>:<<r.ent>><% else %>(SELECT <<r.s-id>> FROM <<r.t-tbl>> WHERE <<r.t-id>> = :<<r.ent>>)<% endif %><% endif%>

<% endfor %>
