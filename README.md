# Query String Framework

### Inventory ###
http://localhost:8080/query?queryString=
select:id,item.descriptionItem,quantity,quantityCommitted;
from:Inventory


### Campo Calculado ###
http://localhost:8080/query?queryString=
select:id,item.descriptionItem,quantity,quantityCommitted,quantity-quantityCommitted;
from:Inventory


### Order ###
http://localhost:8080/query?queryString=
select:id,item.descriptionItem,quantity,quantityCommitted,quantity*quantityCommitted;
from:Inventory;
order:desc(id)


## Filter ##
http://localhost:8080/query?queryString=
select:id,item.descriptionItem,quantity,quantityCommitted,quantity*quantityCommitted;
from:Inventory;
filter:quantity=quantityCommitted;
order:desc(id)


### Group By ###
http://localhost:8080/query?queryString=
select:item.descriptionItem,sum(quantity),sum(quantityCommitted);
from:Inventory
group:item.descriptionItem


### JOINs (Nada haver) ###
http://localhost:8080/query?queryString=
select:i.item.descriptionItem,i.quantity,i.quantityCommitted,u.login;
from:Inventory,i;
join:left(UserAccess,u,i.id,u.id)