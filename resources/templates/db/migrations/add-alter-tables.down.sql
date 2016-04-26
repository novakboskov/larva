<% for alter-table in alter-tables %>ALTER TABLE <<alter-table.table>>
DROP FOREIGN KEY <<alter-table.fk-name>>;

<% endfor %>
