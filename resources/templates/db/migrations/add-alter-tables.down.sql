<% for alter-table in alter-tables %>ALTER TABLE <<alter-table.table>>
DROP <<alter-table.drop-constraint>> <<alter-table.fk-name>>;

<% endfor %>
