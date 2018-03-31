package com.querystringframework.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author reinaldo.locatelli
 */
public class FilterParse {

    public static String getStringFilter(QueryString queryString, String aliasTableFrom, HashMap<String, String> tableAlias) {
        String retorno = "";

        //FilterAnd
        if (queryString.getFilterAnd() != null && !queryString.getFilterAnd().isEmpty()) {
            String filterAnd = "";
            String[] filter = queryString.getFilterAnd().split(";");
            for (String f : filter) {
                if (!filterAnd.isEmpty()) {
                    filterAnd += " and ";
                }
                filterAnd += processFilter(f, aliasTableFrom, tableAlias);
            }
            retorno += " where (" + filterAnd + ")";
        }

        //FilterOr
        if (queryString.getFilterOr() != null && !queryString.getFilterOr().isEmpty()) {
            String filterOr = "";
            String[] filter = queryString.getFilterOr().split(";");
            for (String f : filter) {
                if (!filterOr.isEmpty()) {
                    filterOr += " or ";
                }
                filterOr += processFilter(f, aliasTableFrom, tableAlias);
            }
            if (retorno.isEmpty()) {
                retorno += " where " + filterOr;
            } else {
                retorno += " and (" + filterOr + ")";
            }
        }

        //Filter
        String filter = queryString.getFilter();
        if (filter != null && !filter.isEmpty()) {
            if (retorno.isEmpty()) {
                retorno += " where " + filter;
            } else {
                retorno += " and " + filter;
            }
        }

        return retorno;
    }

    private static String processFilter(String filter, String aliasTableFrom, HashMap<String, String> tableAlias) {
        String filterType = filter.substring(0, 2);
        String field = "";
        List<String> values = new ArrayList<>();

        String retorno = " ";

        int startPosition = 3;
        boolean ignoreProcess = false;

        aliasTableFrom = aliasTableFrom + ".";

        for (int i = 2; i < filter.length(); i++) {
            if (filter.substring(i, i + 1).equals("\"")
                    || (field.isEmpty() && (filter.substring(i, i + 1).equals("{") || filter.substring(i, i + 1).equals("}")))) {
                ignoreProcess = !ignoreProcess;
                if (field.isEmpty()) {
                    aliasTableFrom = "";
                }
            }
            if (ignoreProcess) {
                continue;
            }

            if (filter.substring(i, i + 1).equals(",") || filter.substring(i, i + 1).equals(")")) {
                if (field.isEmpty()) {
                    field = filter.substring(startPosition, i).replaceAll("[{|}]", "");
                } else {
                    values.add(filter.substring(startPosition, i));
                }
                startPosition = i + 1;
            }
        }

        //tratar values and query
        if (field.contains(".") && tableAlias.containsKey(field.substring(0, field.indexOf(".")))) {
            aliasTableFrom = "";
        }

        if (field.startsWith("\"") && field.endsWith("\"")) {
            aliasTableFrom = "";
            field = field.substring(1, field.length() - 1);
        }

        for (int i = 0; i <= values.size() - 1; i++) {
            if (values.get(i).startsWith("\"") && values.get(i).endsWith("\"")) {
                values.set(i, values.get(i).substring(1, values.get(i).length() - 1));
            }
        }

        String valueArray = "";
        for (int i = 0; i <= values.size() - 1; i++) {
            if (i > 0) {
                valueArray += ",";
            }
            valueArray += values.get(i);
        }

        int loop = 1;
        boolean or = false;

        if (values.size() > 1 && (filterType.equals("eq") || filterType.equals("ct") || filterType.equals("ew") || filterType.equals("sw"))) {
            or = true;
        }

        if (or) {
            loop = values.size();
        }

        for (int i = 0; i < loop; i++) {

            if (filterType.equals("eq")) {
                retorno += " lower(cast(" + aliasTableFrom + field + " as string))=" + getValue(values.get(i));
            } else if (filterType.equals("in")) {
                retorno += aliasTableFrom + field + " in (" + valueArray + ")";
            } else if (filterType.equals("bt")) {
                retorno += aliasTableFrom + field + " between " + values.get(0) + " and " + values.get(1);
            } else if (filterType.equals("ct")) {
                retorno += " lower(cast(" + aliasTableFrom + field + " as string)) like " + getValue("%" + values.get(i) + "%").toLowerCase();
            } else if (filterType.equals("ew")) {
                retorno += " lower(cast(" + aliasTableFrom + field + " as string)) like " + getValue("%" + values.get(i)).toLowerCase();
            } else if (filterType.equals("sw")) {
                retorno += " lower(cast(" + aliasTableFrom + field + " as string)) like " + getValue(values.get(i) + "%").toLowerCase();
            } else if (filterType.equals("ge")) {
                retorno += aliasTableFrom + field + " >= " + getValue(values.get(0));
            } else if (filterType.equals("gt")) {
                retorno += aliasTableFrom + field + " > " + getValue(values.get(0));
            } else if (filterType.equals("le")) {
                retorno += aliasTableFrom + field + " <= " + getValue(values.get(0));
            } else if (filterType.equals("lt")) {
                retorno += aliasTableFrom + field + " < " + getValue(values.get(0));
            }

            if (or && i < loop - 1) {
                retorno += " or ";
            }
        }

        return retorno;

    }

    private static String getValue(String value) {
        if (value.toLowerCase().equals("null")) {
            return value;
        } else {
            return String.format("'%s'", value);
        }
    }

}
