<% for r in queries %>--name: get-<<r.ent>>-<<r.prop>><!
-- returns <<r.prop>> associated with <<r.ent>>
SELECT * FROM <<r.f-tbl>> WHERE <<r.f-id>> <<r.sign>> (SELECT <<r.s-id>> FROM <<r.t-tbl>> WHERE <<r.t-id>> = :<<r.ent>>)

<% endfor %>
