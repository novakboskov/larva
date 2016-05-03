<% for alter-table in alter-tables %>ALTER TABLE <<alter-table.table>>
ADD CONSTRAINT <<alter-table.fk-name>> FOREIGN KEY (<<alter-table.on>>)
REFERENCES <<alter-table.to-table>>(id);

<% endfor %>
