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
import java.util.ArrayList;
import org.json.JSONObject;

/**
 *
 * @author reinaldo.locatelli
 */
@Named
public class QueryStringParse {

    private QueryString queryString;
    private String alias;
    private List list;
    private LinkedHashMap<String, String> fieldsAlias;
    private HashMap<String, String> tableAlias;

    public QueryStringParse() {
        tableAlias = new HashMap<>();
    }

    public String execute(QueryString queryString, EntityManager entityManager) {
        return execute(queryString, entityManager, null, false);
    }

    public String execute(QueryString queryString, EntityManager entityManager, List<String> allowedTables, boolean datatable) {

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

        String queryJpql = createQuery(allowedTables);

        Integer count = null;
        List countResult = null;

        Query query = null;
        try {
            query = entityManager.createQuery(queryJpql);
            if (datatable) {
                countResult = entityManager.createQuery(createCountQuery(allowedTables)).getResultList();
                if (countResult != null) {
                    if (countResult.size() > 1) {
                        count = countResult.size();
                    } else {

                        if (countResult.get(0) instanceof Long) {
                            count = ((Long) countResult.get(0)).intValue();
                        } else {
                            count = 0;
                            for (Object object : (Object[]) countResult.get(0)) {
                                if (count < ((Long) object).intValue()) {
                                    count = ((Long) object).intValue();
                                }
                            }
                        }
                    }
                }
            }
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
        return getJson(count);
    }

    public String createCountQuery(List<String> allowedtables) {
        if (allowedtables != null && allowedtables.size() == 0) {
            allowedtables = null;
        }
        String from = from(allowedtables);
        String join = join(allowedtables);

        StringBuilder stringBuilder = new StringBuilder("SELECT ");

        boolean dot = false;
        String[] fields = queryString.getSelect().split(",");

        String count = " ";
        if (join.isEmpty()) {
            for (String field : fields) {
                if (field.contains(".")) {
                    dot = true;
                }
                count += " COUNT(" + field + "),";
            }
        }

        if (!dot) {
            stringBuilder.append("COUNT(" + alias + ") ");
        } else {
            stringBuilder.append(count.substring(0, count.length() - 1));
        }

        stringBuilder.append(from);
        stringBuilder.append(join);

        stringBuilder.append(FilterParse.getStringFilter(queryString, alias, tableAlias));
        stringBuilder.append(groupBy());

        return stringBuilder.toString();
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
        stringBuilder.append(groupBy());
        stringBuilder.append(order());

        return stringBuilder.toString();
    }

    public String select() {
        String allColumns = queryString.getSelect();
        if (allColumns == null) {
            return " " + alias;
        }

        String columns = "";
        List<String> fields = new ArrayList<>();// Arrays.asList(allColumns.split(","));

        int startPosition = 0;
        boolean ignoreProcess = false;
        for (int i = 0; i < allColumns.length(); i++) {
            if (allColumns.substring(i, i + 1).equals("{") || allColumns.substring(i, i + 1).equals("}")) {
                ignoreProcess = !ignoreProcess;
            }
            if (ignoreProcess) {
                continue;
            }

            if (allColumns.substring(i, i + 1).equals(",") || i == allColumns.length() - 1) {
                if (i == allColumns.length() - 1) {
                    fields.add(allColumns.substring(startPosition, i + 1).replaceAll("[{|}]", ""));
                } else {
                    fields.add(allColumns.substring(startPosition, i).replaceAll("[{|}]", ""));
                }
                startPosition = i + 1;
            }
        }

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
        if (queryString.getJoin() != null && !queryString.getJoin().isEmpty()) {
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

    public String groupBy() {
        String retorno = "";
        if (queryString.getGroup() != null) {
            String[] fieldGroups = queryString.getGroup().split(",");

            for (String field : fieldGroups) {
                field = field.trim();

                if (retorno.isEmpty()) {
                    retorno += " GROUP BY " + field;
                } else {
                    retorno += ", " + field + " ";
                }
            }
        }

        return retorno;
    }

    public String order() {
        String retorno = "";
        if (queryString.getOrder() != null) {
            List<String> orders = new ArrayList<>();

            int startPosition = 0;
            boolean ignoreProcess = false;
            for (int i = 0; i < queryString.getOrder().length(); i++) {
                if (queryString.getOrder().substring(i, i + 1).equals("{") || queryString.getOrder().substring(i, i + 1).equals("}")) {
                    ignoreProcess = !ignoreProcess;
                    continue;
                }
                if (ignoreProcess) {
                    continue;
                }

                if (queryString.getOrder().substring(i, i + 1).equals(";") || i == queryString.getOrder().length() - 1) {
                    if (queryString.getOrder().substring(i, i + 1).equals(";")) {
                        orders.add(queryString.getOrder().substring(startPosition, i));
                    } else {
                        orders.add(queryString.getOrder().substring(startPosition, i + 1));
                    }
                    startPosition = i + 1;
                    i++;
                }
            }

            for (String order : orders) {
                order = order.trim();
                List<String> fields = new ArrayList<>();
                String typeOrder = "";
                String allFields = "";
                if (order.subSequence(0, 3).equals("asc")) {
                    allFields = order.substring(4, order.length() - 1);
                    typeOrder = " ASC ";
                } else if (order.subSequence(0, 4).equals("desc")) {
                    allFields = order.substring(5, order.length() - 1);
                    typeOrder = " DESC ";
                }

                startPosition = 0;
                ignoreProcess = false;
                for (int i = 0; i < allFields.length(); i++) {
                    if (allFields.substring(i, i + 1).equals("{") || allFields.substring(i, i + 1).equals("}")) {
                        ignoreProcess = !ignoreProcess;
                    }
                    if (ignoreProcess) {
                        continue;
                    }

                    if (allFields.substring(i, i + 1).equals(",") || i == allFields.length() - 1) {
                        fields.add(allFields.substring(startPosition, i + 1).replaceAll("[{|}]", ""));
                        startPosition = i + 1;
                        i++;
                    }
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

    public String getJson(Integer count) {

        String retorno = "";

        if (count != null) {
            retorno += "{\"count\":" + count + ", \"data\":";
        }

        retorno += "[";
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

        retorno += "]";
        if (count != null) {
            retorno += "}";
        }

        return retorno;
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
