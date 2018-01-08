package com.querystringframework.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import com.querystringframework.exception.QueryException;
import org.json.JSONObject;

/**
 *
 * @author reinaldo.locatelli
 */
@Named
public class QueryStringParse {

    private QueryString queryString;
    private String queryJpql;
    private String alias;
    private List list;
    private LinkedHashMap<String, String> fieldsAlias;
    private HashMap<String, String> tableAlias;

    public QueryStringParse() {
        tableAlias = new HashMap<>();
    }

    public String execute(QueryString queryString, EntityManager entityManager) {
        return execute(queryString, entityManager, null);
    }

    public String execute(QueryString queryString, EntityManager entityManager, List<String> allowedtables) {

        if (entityManager == null) {
            throw new QueryException("EntityManager não pode ser nulo");
        }

        this.queryString = queryString;

        if (queryString.getSelect() == null || queryString.getSelect().isEmpty()) {
            throw new QueryException("atributo 'select' não encontrado");
        }
        if (queryString.getFrom() == null || queryString.getFrom().isEmpty()) {
            throw new QueryException("atributo 'from' não encontrado");
        }

        queryJpql = createQuery(allowedtables);

        Query query = null;
        try {
            query = entityManager.createQuery(queryJpql);
        } catch (Exception e) {
            throw new QueryException(e.getMessage());
        }

        if (queryString.getOffset() != null) {
            query.setFirstResult(Integer.valueOf(queryString.getOffset()));
        }

        if (queryString.getLimit() != null) {
            query.setMaxResults(Integer.valueOf(queryString.getLimit()));
        }

        this.list = query.getResultList();
        return getJson();
    }

    public String createQuery(List<String> allowedtables) {
        if (allowedtables != null && allowedtables.size() == 0) {
            allowedtables = null;
        }
        StringBuilder stringBuilder = new StringBuilder("SELECT ");

        String from = from(allowedtables);
        String join = join(allowedtables);

        stringBuilder.append(select());
        stringBuilder.append(from);
        stringBuilder.append(join);
        stringBuilder.append(FilterParse.getStringFilter(queryString, alias, tableAlias));
        stringBuilder.append(order());

        return stringBuilder.toString();
    }

    public String select() {
        String allColumns = queryString.getSelect();
        if (allColumns == null) {
            return " " + alias;
        }

        String columns = "";
        List<String> fields = Arrays.asList(allColumns.split(","));
        fieldsAlias = new LinkedHashMap<>();

        Pattern aliasPattern = Pattern.compile("(\\@.+)");

        for (String field : fields) {
            Matcher m = aliasPattern.matcher(field);
            if (m.find()) {
                field = field.substring(0, field.indexOf("@"));
                fieldsAlias.put(field, m.group(1).replaceAll("@", ""));
            } else {
                fieldsAlias.put(field, getAliasField(field));
            }
        }

        for (String key : fieldsAlias.keySet()) {
            columns += " " + key + ",";
        }

        columns = columns.substring(0, columns.length() - 1);

        return columns;
    }

    public String getAliasField(String field) {
        String aliasField = field;
        if (field.contains(".")) {
            if (field.substring(0, field.indexOf(".")).equals(alias)) {
                aliasField = field.substring(field.indexOf(".") + 1);
            } else {
                String table = tableAlias.get(field.substring(0, field.indexOf(".")));
                if (table != null && !table.isEmpty()) {
                    table = table.substring(0, 1).toLowerCase() + table.substring(1);
                    aliasField = table + field.substring(field.indexOf("."));
                }
            }
        }

        return aliasField;
    }

    public String from(List<String> allowedtables) {
        String table = queryString.getFrom();
        String from = " FROM ";
        String tableName = null;
        if (table.contains(",")) {
            String[] fromArray = table.split(",");
            alias = fromArray[1];
            tableName = fromArray[0];
            from += tableName;
        } else {
            from += table;
            alias = "tableOrigin";
        }

        if (allowedtables != null && !allowedtables.contains(tableName)) {
            throw new QueryException("Sem permissão para executar consultas na tabela '" + from + "'");
        }

        tableAlias.put(alias, tableName);
        from += " " + alias;
        return from;
    }

    public String join(List<String> allowedtables) {
        String retorno = "";
        if (queryString.getJoin() != null) {
            String[] joins = queryString.getJoin().split(";");

            for (String join : joins) {
                join = join.trim();
                String[] values = null;
                String typeJoin = "";
                if (join.subSequence(0, 4).equals("left")) {
                    values = join.substring(5, join.length() - 1).split(",");
                    typeJoin = " LEFT ";
                } else if (join.subSequence(0, 5).equals("inner")) {
                    values = join.substring(6, join.length() - 1).split(",");
                    typeJoin = " INNER ";
                }

                if (values.length != 4) {
                    throw new QueryException("join mal formado: '" + join + "'. O join deve obedecer a estrutura: tipoJoin(Tabela,alias,atributoTabela,atributoFrom)");

                }

                if (allowedtables != null && !allowedtables.contains(values[0])) {
                    throw new QueryException("Sem permissão para executar consultas na tabela '" + values[0] + "'");
                }

                tableAlias.put(values[1], values[0]);
                retorno += String.format(" %s JOIN %s %s ON %s = %s ", typeJoin, values[0], values[1], values[2], values[3]);
            }
        }

        return retorno;
    }

    public String order() {
        String retorno = "";
        if (queryString.getOrder() != null) {
            String[] orders = queryString.getOrder().split(";");

            for (String order : orders) {
                order = order.trim();
                String[] fields = null;
                String typeOrder = "";
                if (order.subSequence(0, 3).equals("asc")) {
                    fields = order.substring(4, order.length() - 1).split(",");
                    typeOrder = " ASC ";
                } else if (order.subSequence(0, 4).equals("desc")) {
                    fields = order.substring(5, order.length() - 1).split(",");
                    typeOrder = " DESC ";
                }

                if (fields != null) {
                    for (String field : fields) {
                        if (retorno.isEmpty()) {
                            retorno += " ORDER BY " + field + typeOrder;
                        } else {
                            retorno += ", " + field + typeOrder;
                        }
                    }
                }
            }
        }

        return retorno;
    }

    public String getJson() {
        String retorno = "[";
        if (fieldsAlias != null && list != null) {

            for (Object object : list) {
                Object[] objectRow = null;
                if (object instanceof Object[]) {
                    objectRow = (Object[]) object;
                } else {
                    objectRow = new Object[1];
                    objectRow[0] = object;
                }

                retorno += createJsonBody(null, null, objectRow, 0);

                retorno += ",";
            }
            if (retorno.substring(retorno.length() - 1).equals(",")) {
                retorno = retorno.substring(0, retorno.length() - 1);
            }

        }

        return retorno + "]";
    }

    private JSONObject createJsonBody(JSONObject jsonObject, String alias, Object[] objectRow, int index) {
        if (jsonObject == null) {
            jsonObject = new JSONObject();
        }
        if (alias == null) {
            int i = 0;
            for (String key : fieldsAlias.keySet()) {
                alias = fieldsAlias.get(key);
                createJsonBody(jsonObject, alias, objectRow, i);
                i++;
            }
        } else {

            if (!alias.contains(".")) {
                jsonObject.put(alias, objectRow[index]);
            } else {
                String object = alias.substring(0, alias.indexOf("."));
                String field = alias.substring(alias.indexOf(".") + 1, alias.length());

                JSONObject jsonObject2 = null;

                if (!jsonObject.isNull(object)) {
                    jsonObject2 = (JSONObject) jsonObject.get(object);
                }
                JSONObject subObject = createJsonBody(jsonObject2, field, objectRow, index);
                if (subObject != null && subObject.length() > 0) {
                    jsonObject.put(object, subObject);
                }

            }
        }

        return jsonObject;
    }

}
